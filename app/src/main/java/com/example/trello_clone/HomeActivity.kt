package com.example.trello_clone

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class HomeActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var recyclerView: RecyclerView
    private lateinit var boardsList: MutableList<Board>
    private lateinit var boardAdapter: BoardAdapter
    private lateinit var modalView: View
    private lateinit var modalView1: View
    private lateinit var modalView2: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference.child("boards")

        // Set up RecyclerView
        recyclerView = findViewById(R.id.workspace_section)
        recyclerView.layoutManager = GridLayoutManager(this, 3) // 3 items per row
        boardsList = mutableListOf()
        boardAdapter = BoardAdapter(boardsList)
        recyclerView.adapter = boardAdapter

        // Fetch boards from Firebase
        fetchBoards()

        // Set up logout button
        val logoutBtn = findViewById<ImageView>(R.id.menu_icon)
        logoutBtn.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            } else {
                false
            }
        }

        val moveToCategories = findViewById<ConstraintLayout>(R.id.add_Card)
        moveToCategories.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                startActivity(Intent(this, AssetInventoryActivity::class.java))
                true
            } else {
                false
            }
        }

        val notificationMove = findViewById<ImageView>(R.id.notification_icon)
        notificationMove.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                startActivity(Intent(this, NotificationActivity::class.java))
                true
            } else {
                false
            }
        }

        // Inflate and set up modal for adding a card
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        modalView = LayoutInflater.from(this).inflate(R.layout.activity_add_card, rootView, false)
        modalView.visibility = View.GONE
        rootView.addView(modalView)

        val addCardButton = findViewById<Button>(R.id.btn_add_Card)
        addCardButton.setOnClickListener {
            setupAssetModal()
            modalView.visibility = View.VISIBLE
        }

        val closeCardModalButton = modalView.findViewById<ImageView>(R.id.topCloseButton)
        closeCardModalButton.setOnClickListener {
            modalView.visibility = View.GONE
        }
        
        // Set up send button for asset modal
        val sendAssetButton = modalView.findViewById<ImageView>(R.id.sendButton)
        sendAssetButton.setOnClickListener {
            val assetName = modalView.findViewById<EditText>(R.id.inputField).text.toString().trim()
            val assetValueStr = modalView.findViewById<EditText>(R.id.valueField).text.toString().trim()
            val assetLocation = modalView.findViewById<EditText>(R.id.locationField).text.toString().trim()
            val assetStatus = modalView.findViewById<EditText>(R.id.statusField).text.toString().trim()
            val serialNumber = modalView.findViewById<EditText>(R.id.serialNumberField).text.toString().trim()
            val manufacturer = modalView.findViewById<EditText>(R.id.manufacturerField).text.toString().trim()
            
            if (assetName.isNotEmpty() && assetValueStr.isNotEmpty()) {
                try {
                    val assetValue = assetValueStr.toDouble()
                    
                    // Get the selected category or create a new one
                    val categorySpinner = modalView.findViewById<Spinner>(R.id.categorySpinner)
                    if (categorySpinner != null && categorySpinner.selectedItem != null) {
                        val selectedCategory = categorySpinner.selectedItem.toString()
                        saveAssetToDatabase(assetName, assetValue, assetLocation, assetStatus, serialNumber, manufacturer, selectedCategory)
                    } else {
                        // No category selected, create a default one
                        saveAssetToDatabase(assetName, assetValue, assetLocation, assetStatus, serialNumber, manufacturer, "General Assets")
                    }
                    
                    modalView.visibility = View.GONE
                    
                    // Clear the fields
                    modalView.findViewById<EditText>(R.id.inputField).text.clear()
                    modalView.findViewById<EditText>(R.id.valueField).text.clear()
                    modalView.findViewById<EditText>(R.id.locationField).text.clear()
                    modalView.findViewById<EditText>(R.id.statusField).text.clear()
                    modalView.findViewById<EditText>(R.id.serialNumberField).text.clear()
                    modalView.findViewById<EditText>(R.id.manufacturerField).text.clear()
                    
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter a valid asset value", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter asset name and value", Toast.LENGTH_SHORT).show()
            }
        }

        // Inflate and set up modal for editing a location
        modalView1 = LayoutInflater.from(this).inflate(R.layout.activity_change_location, rootView, false)
        modalView1.visibility = View.GONE
        rootView.addView(modalView1)

        val editIconButton = findViewById<ImageView>(R.id.edit_icon)
        editIconButton.setOnClickListener {
            setupLocationSpinners()
            modalView1.visibility = View.VISIBLE
        }

        val closeLocationModalButton = modalView1.findViewById<ImageView>(R.id.topCloseButton)
        closeLocationModalButton.setOnClickListener {
            modalView1.visibility = View.GONE
        }

        // Setup done button for location change
        val doneButton = modalView1.findViewById<ImageView>(R.id.done)
        doneButton.setOnClickListener {
            // Get the selected values and perform the move operation
            val categorySpinner = modalView1.findViewById<Spinner>(R.id.boardMenu)
            val locationSpinner = modalView1.findViewById<Spinner>(R.id.listMenu)
            
            val selectedCategory = categorySpinner.selectedItem?.toString() ?: ""
            val selectedLocation = locationSpinner.selectedItem?.toString() ?: ""
            
            if (selectedCategory.isNotEmpty() && selectedLocation.isNotEmpty()) {
                Toast.makeText(this, "Asset moved to $selectedLocation in $selectedCategory", Toast.LENGTH_SHORT).show()
                modalView1.visibility = View.GONE
            } else {
                Toast.makeText(this, "Please select both category and location", Toast.LENGTH_SHORT).show()
            }
        }

        // Inflate and set up modal for adding a board
        modalView2 = LayoutInflater.from(this).inflate(R.layout.add_board, rootView, false)
        modalView2.visibility = View.GONE
        rootView.addView(modalView2)

        val addBoardButton = findViewById<ImageView>(R.id.add_board)
        addBoardButton.setOnClickListener {
            modalView2.visibility = View.VISIBLE
        }

        val closeBoardModalButton = modalView2.findViewById<ImageView>(R.id.topCloseButton)
        closeBoardModalButton.setOnClickListener {
            modalView2.visibility = View.GONE
        }

        val sendBoardButton = modalView2.findViewById<ImageView>(R.id.sendButton)
        val boardNameInput = modalView2.findViewById<EditText>(R.id.boardNameInput)
        sendBoardButton.setOnClickListener {
            val boardName = boardNameInput.text.toString().trim()
            if (boardName.isNotEmpty()) {
                saveBoardToDatabase(boardName)
                modalView2.visibility = View.GONE
            } else {
                Toast.makeText(this, "Please enter a board name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveBoardToDatabase(boardName: String) {
        val boardId = database.push().key
        if (boardId != null) {
            val boardData = mapOf(
                "id" to boardId,
                "name" to boardName,
                "image" to "" // Placeholder for image
            )
            database.child(boardId).setValue(boardData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Board added successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add board", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun fetchBoards() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                boardsList.clear()
                for (boardSnapshot in snapshot.children) {
                    val boardId = boardSnapshot.child("id").value.toString()
                    val boardName = boardSnapshot.child("name").value.toString()
                    val boardImage = boardSnapshot.child("image").value?.toString()
                    boardsList.add(Board(boardId, boardName, boardImage))
                }
                boardAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Failed to load boards", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupLocationSpinners() {
        val database = FirebaseDatabase.getInstance().reference

        // Get categories for the first spinner
        database.child("asset_categories").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categoryNames = mutableListOf<String>()
                
                if (snapshot.exists()) {
                    for (categorySnapshot in snapshot.children) {
                        val categoryName = categorySnapshot.child("name").getValue(String::class.java)
                        if (!categoryName.isNullOrEmpty()) {
                            categoryNames.add(categoryName)
                        }
                    }
                }
                
                if (categoryNames.isEmpty()) {
                    // Add default categories if none exist
                    categoryNames.add("IT Equipment")
                    categoryNames.add("Office Furniture")
                    categoryNames.add("Vehicles")
                }
                
                // Set up the category spinner
                val dropdownMenu = modalView1.findViewById<Spinner>(R.id.boardMenu)
                val dropdownAdapter = ArrayAdapter(this@HomeActivity, android.R.layout.simple_spinner_dropdown_item, categoryNames)
                dropdownMenu.adapter = dropdownAdapter
            }
            
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Error loading categories: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
        
        // Predefined locations for the second spinner
        val locations = listOf(
            "Main Office", 
            "Branch Office", 
            "Warehouse", 
            "Production Floor", 
            "Conference Room", 
            "Employee Desk", 
            "Storage Room"
        )
        
        val dropdownMenu1 = modalView1.findViewById<Spinner>(R.id.listMenu)
        val dropdownAdapter1 = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, locations)
        dropdownMenu1.adapter = dropdownAdapter1
    }

    private fun setupAssetModal() {
        // Add a category spinner to the asset creation modal
        val categorySpinner = modalView.findViewById<Spinner>(R.id.categorySpinner)
        if (categorySpinner != null) {
            // Fetch available categories
            val dbRef = FirebaseDatabase.getInstance().reference
            dbRef.child("asset_categories").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val categories = mutableListOf<String>()
                    
                    if (snapshot.exists()) {
                        for (categorySnapshot in snapshot.children) {
                            val categoryName = categorySnapshot.child("name").getValue(String::class.java)
                            if (!categoryName.isNullOrEmpty()) {
                                categories.add(categoryName)
                            }
                        }
                    }
                    
                    if (categories.isEmpty()) {
                        // Add default category if none exist
                        categories.add("General Assets")
                    }
                    
                    val adapter = ArrayAdapter(this@HomeActivity, 
                        android.R.layout.simple_spinner_dropdown_item, categories)
                    categorySpinner.adapter = adapter
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeActivity, 
                        "Error loading categories: ${error.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
    
    private fun saveAssetToDatabase(assetName: String, assetValue: Double, 
                                    assetLocation: String, assetStatus: String, 
                                    serialNumber: String, manufacturer: String,
                                    categoryName: String) {
        val dbRef = FirebaseDatabase.getInstance().reference
        
        // First, find or create the category
        dbRef.child("asset_categories").orderByChild("name").equalTo(categoryName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var categoryId: String? = null
                    var categoryData: Map<String, Any>? = null
                    var currentAssetCount = 0L
                    var currentTotalValue = 0.0
                    
                    if (snapshot.exists()) {
                        // Category exists, get its ID
                        for (categorySnapshot in snapshot.children) {
                            categoryId = categorySnapshot.key
                            currentAssetCount = categorySnapshot.child("assetCount").getValue(Long::class.java) ?: 0
                            currentTotalValue = categorySnapshot.child("totalValue").getValue(Double::class.java) ?: 0.0
                            break
                        }
                    } else {
                        // Create new category
                        categoryId = dbRef.child("asset_categories").push().key
                        categoryData = mapOf(
                            "name" to categoryName,
                            "image" to "",
                            "assetCount" to 0L,
                            "totalValue" to 0.0
                        )
                    }
                    
                    if (categoryId != null) {
                        // Now add the asset to this category
                        val assetId = dbRef.child("asset_categories").child(categoryId).child("assets").push().key
                        
                        if (assetId != null) {
                            val currentTime = System.currentTimeMillis()
                            val sixMonthsFromNow = currentTime + (6 * 30 * 24 * 60 * 60 * 1000)
                            
                            val assetData = mapOf(
                                "id" to assetId,
                                "name" to assetName,
                                "value" to assetValue,
                                "location" to assetLocation,
                                "status" to assetStatus,
                                "purchaseDate" to currentTime,
                                "assignedTo" to "",
                                "maintenanceStatus" to "No maintenance scheduled",
                                "lastServiceDate" to 0L,
                                "nextServiceDate" to sixMonthsFromNow,
                                "serialNumber" to serialNumber,
                                "manufacturer" to manufacturer
                            )
                            
                            // Prepare updates
                            val updates = HashMap<String, Any>()
                            
                            // If category is new, create it
                            if (categoryData != null) {
                                updates["asset_categories/$categoryId"] = categoryData
                            }
                            
                            // Add the asset and update category counters
                            updates["asset_categories/$categoryId/assets/$assetId"] = assetData
                            updates["asset_categories/$categoryId/assetCount"] = currentAssetCount + 1
                            updates["asset_categories/$categoryId/totalValue"] = currentTotalValue + assetValue
                            
                            // Perform the updates
                            dbRef.updateChildren(updates)
                                .addOnSuccessListener {
                                    Toast.makeText(this@HomeActivity, "Asset added successfully", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this@HomeActivity, "Failed to add asset", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@HomeActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}

data class Board(val id: String, val name: String, val image: String?)

class BoardAdapter(private val boards: List<Board>) : RecyclerView.Adapter<BoardAdapter.BoardViewHolder>() {
    inner class BoardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val boardId: TextView = itemView.findViewById(R.id.project_id)
        val boardName: TextView = itemView.findViewById(R.id.project_heading)
        val boardImage: ImageView = itemView.findViewById(R.id.default_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BoardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_board, parent, false)
        return BoardViewHolder(view)
    }

    override fun onBindViewHolder(holder: BoardViewHolder, position: Int) {
        val board = boards[position]
        holder.boardId.text = board.id // Bind the board ID
        holder.boardImage.setImageResource(R.drawable.default_board) // Default image
        holder.boardName.text = board.name
        holder.boardImage.setImageResource(R.drawable.default_board) // Default image
    }

    override fun getItemCount(): Int = boards.size
}
