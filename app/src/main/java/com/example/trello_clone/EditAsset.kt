package com.example.trello_clone


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import android.text.format.DateUtils

class EditAsset : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var modalView: View
    private lateinit var modalView1: View

    private var userName: String? = null // To store the logged-in user's name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_board)

        // Get the logged-in user's name
        val firebaseUser = FirebaseAuth.getInstance().currentUser

        // Get the Asset Category ID passed from the intent
        val categoryId = intent.getStringExtra("CATEGORY_ID")
        println("CATEGORY_ID: $categoryId")
        if (categoryId != null) {
            fetchChecklistItems(categoryId)
            fetchAssetDetails(categoryId)
            fetchComments(categoryId)
        }

        var closeBtn = findViewById<ImageView>(R.id.close_icon)

        closeBtn.setOnClickListener {
            val intent = Intent(this, AssetInventoryActivity::class.java)
            startActivity(intent)
        }
        
        // Set up the Move functionality
        val moveButton = findViewById<TextView>(R.id.edit_icon)
        moveButton.setOnClickListener {
            // Show a toast message for now since move functionality is in development
            Toast.makeText(this, "Moving asset to a different category...", Toast.LENGTH_SHORT).show()
            
            // Create an intent to open ChangeLocationActivity
            try {
                val intent = Intent(this, AssetInventoryActivity::class.java)
                intent.putExtra("SHOW_MOVE_DIALOG", true)
                intent.putExtra("ASSET_ID", categoryId)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error opening move dialog: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        var goToLabels = findViewById<LinearLayout>(R.id.lableGrid)

        goToLabels.setOnClickListener {
            val intent = Intent(this, Labels::class.java)
            startActivity(intent)
        }


        // Inflating the modal layout and add it to the main content view
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        modalView = LayoutInflater.from(this).inflate(R.layout.activity_members, rootView, false)

        // Initially, modal is hidden
        modalView.visibility = View.GONE
        rootView.addView(modalView)

        // Setting up the Open Modal button click listener
        val openModalButton = findViewById<LinearLayout>(R.id.members_btn)
        openModalButton.setOnClickListener {
            modalView.visibility = View.VISIBLE
        }

        // Setting up the Close button inside the modal
        val topCloseButton: TextView = modalView.findViewById(R.id.done_button)
        topCloseButton.setOnClickListener {
            modalView.visibility = View.GONE
        }

        // Hiding the modal when clicking outside the main modal area (this will close the modal)
        modalView.setOnClickListener {
            modalView.visibility = View.GONE
        }



        // Inflating the modal layout and add it to the main content view
        val rootView1 = findViewById<ViewGroup>(android.R.id.content)
        modalView1 = LayoutInflater.from(this).inflate(R.layout.activity_attachments, rootView1, false)

        // Initially, modal is hidden
        modalView1.visibility = View.GONE
        rootView.addView(modalView1)

        // Setting up the Open Modal button click listener
        val openModalButton1 = findViewById<LinearLayout>(R.id.add_attachment_btn)
        openModalButton1.setOnClickListener {
            modalView1.visibility = View.VISIBLE
        }
        val openModalButton2 = findViewById<ImageView>(R.id.link_image)
        openModalButton2.setOnClickListener {
            modalView1.visibility = View.VISIBLE
        }

        // Hiding the modal when clicking outside the main modal area (this will close the modal)
        modalView1.setOnClickListener {
            modalView1.visibility = View.GONE
        }


        scrollView = findViewById(R.id.scrollViewer)
        var addItemEditText: EditText = findViewById(R.id.addchecklist_input)

        // Initialize the checklist button directly in the addItemEditText focus code
        // without relying on addChecklist_btn which doesn't exist in the layout
        addItemEditText.setOnEditorActionListener { v, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                val checklistItem = addItemEditText.text.toString().trim()
                if (checklistItem.isNotEmpty()) {
                    addItemEditText.text.clear() // Clear the input field
                    categoryId?.let { id -> addChecklistItemToDatabase(id, checklistItem) } // Pass categoryId
                } else {
                    Toast.makeText(this, "Please enter a checklist item", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        // Add a focus listener to scroll to the edit text when clicked
        addItemEditText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                scrollToView(addItemEditText)
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(addItemEditText, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        val sendButton: ImageView = findViewById(R.id.send_button)
        val commentInput: EditText = findViewById(R.id.footer_edit_text)

        sendButton.setOnClickListener {
            val commentText = commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                // Clear input field
                commentInput.text.clear()

                // Push comment to Firebase
                categoryId?.let { id ->
                    addCommentToDatabase(id, commentText)
                    // Add the comment to the UI immediately for better user experience
                    addCommentToUI("User", System.currentTimeMillis(), commentText)
                }
            } else {
                Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show()
            }
        }

        // Clear previous comments to avoid duplicates
        val commentsContainer = findViewById<LinearLayout>(R.id.commentsContainer)
        commentsContainer.removeAllViews()
    }

    override fun onResume() {
        super.onResume()

        val categoryId = intent.getStringExtra("CATEGORY_ID")
        if (categoryId != null) {
            fetchComments(categoryId) // Fetch comments whenever the activity is resumed
        }
    }

    private fun scrollToView(view: View) {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val y = location[1]
        scrollView.smoothScrollTo(0, y)
    }

    private fun addChecklistItemToUI(checklistItem: String) {
        val checklistLayout: LinearLayout = findViewById(R.id.checklists_layout)

        // Create a new horizontal LinearLayout for the item
        val itemLayout = LinearLayout(this)
        itemLayout.orientation = LinearLayout.HORIZONTAL
        itemLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        itemLayout.gravity = android.view.Gravity.CENTER_VERTICAL

        // Add a CheckBox
        val checkBox = CheckBox(this)
        checkBox.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        checkBox.setPadding(
            (8 * resources.displayMetrics.density).toInt(), // Left padding for the label text
            0, // Top padding
            0, // Right padding
            0  // Bottom padding
        )
        checkBox.text = checklistItem
        checkBox.setTextColor(resources.getColor(R.color.white))
        checkBox.isChecked = false

        // Add the CheckBox to the layout
        itemLayout.addView(checkBox)

        // Add the new item layout to the checklist layout
        checklistLayout.addView(itemLayout)
    }


    private fun addChecklistItemToDatabase(categoryId: String, checklistItem: String) {
        val database = FirebaseDatabase.getInstance().reference

        // Generate a unique key for the checklist item
        val checklistItemId = database.child("asset_categories").child(categoryId).child("checklists").push().key

        if (checklistItemId != null) {
            val checklistData = mapOf(
                "id" to checklistItemId,
                "name" to checklistItem
            )

            database.child("asset_categories").child(categoryId).child("checklists").child(checklistItemId)
                .setValue(checklistData)
                .addOnSuccessListener {
                    // Successfully added to the database
                    Toast.makeText(this, "Checklist item added", Toast.LENGTH_SHORT).show()
                    addChecklistItemToUI(checklistItem)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add checklist item", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun addCommentToUI(userName: String, timeInMillis: Long, commentText: String) {
        val commentsContainer: LinearLayout = findViewById(R.id.commentsContainer)
        
        // Ensure the container exists
        if (commentsContainer == null) {
            Toast.makeText(this, "Comment container not found", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Inflate the comment layout
            val inflater = LayoutInflater.from(this)
            val commentView = inflater.inflate(R.layout.comment_layout, commentsContainer, false)

            // Set user data
            val userNameView: TextView = commentView.findViewById(R.id.text_view_1)
            val timeView: TextView = commentView.findViewById(R.id.time_text)
            val commentContent: TextView = commentView.findViewById(R.id.comment_txt)

            userNameView.text = userName
            timeView.text = DateUtils.getRelativeTimeSpanString(
                timeInMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            commentContent.text = commentText

            // Add the comment view to the container
            commentsContainer.addView(commentView, 0) // Add at the top for newest comments first
            
            // Scroll to show the new comment
            scrollView.post {
                scrollView.smoothScrollTo(0, commentView.top)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error adding comment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun addCommentToDatabase(categoryId: String, commentText: String) {
        val database = FirebaseDatabase.getInstance().reference

        // Generate a unique key for the comment
        val commentId = database.child("asset_categories").child(categoryId).child("comments").push().key

        if (commentId != null) {
            val commentData = mapOf(
                "id" to commentId,
                "user" to "John Doe",
                "time" to System.currentTimeMillis().toString(),
                "text" to commentText
            )

            database.child("asset_categories").child(categoryId).child("comments").child(commentId)
                .setValue(commentData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Comment added successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add comment", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchComments(categoryId: String) {
        val database = FirebaseDatabase.getInstance().reference
        
        // Clear previous comments to avoid duplicates
        val commentsContainer = findViewById<LinearLayout>(R.id.commentsContainer)
        commentsContainer.removeAllViews()

        database.child("asset_categories").child(categoryId).child("comments")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (commentSnapshot in snapshot.children) {
                            val userName = commentSnapshot.child("user").getValue(String::class.java) ?: "Unknown"
                            val time = commentSnapshot.child("time").getValue(String::class.java)?.toLongOrNull() ?: System.currentTimeMillis()
                            val text = commentSnapshot.child("text").getValue(String::class.java) ?: "No text"

                            addCommentToUI(userName, time, text)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditAsset, "Error fetching comments: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun fetchAssetDetails(categoryId: String) {
        val database = FirebaseDatabase.getInstance().reference

        database.child("asset_categories").child(categoryId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val categoryName = snapshot.child("name").getValue(String::class.java)
                    val totalValue = snapshot.child("totalValue").getValue(Double::class.java) ?: 0.0
                    val assetCount = snapshot.child("assetCount").getValue(Long::class.java) ?: 0
                    
                    if (!categoryName.isNullOrEmpty()) {
                        val titleEditText: EditText = findViewById(R.id.title)
                        titleEditText.setText("$categoryName ($assetCount assets, $${String.format("%.2f", totalValue)})") 
                        
                        // Update the subtitle to show more category information
                        val subTitleEditText: EditText = findViewById(R.id.sub_title)
                        subTitleEditText.setText("Asset Category: $categoryName\nTotal Value: $${String.format("%.2f", totalValue)}\nTotal Assets: $assetCount")
                    } else {
                        Toast.makeText(this@EditAsset, "Category name not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@EditAsset, "Error fetching category details: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }



    private fun fetchChecklistItems(categoryId: String) {
        val database = FirebaseDatabase.getInstance().reference

        database.child("asset_categories").child(categoryId).child("checklists")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (itemSnapshot in snapshot.children) {
                            val checklistItem = itemSnapshot.child("name").getValue(String::class.java)
                            if (checklistItem != null) {
                                addChecklistItemToUI(checklistItem)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@EditAsset, "Error fetching checklist: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}

