package com.example.trello_clone

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.LinearLayout
import com.google.firebase.database.ServerValue


class AssetInventoryActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var assetCategoryAdapter: AssetCategoryItemAdapter
    private var assetCategoriesList: MutableList<AssetCategoryItem> = mutableListOf()

    private lateinit var modalView: View
    private lateinit var categoryModalView: View

    private lateinit var menuIcon: ImageView
    private lateinit var closeIcon: ImageView
    private lateinit var filterIcon: ImageView
    private lateinit var notificationIcon: ImageView
    private lateinit var optionIcon: ImageView
    private lateinit var tickIcon: ImageView

    private lateinit var totalAssetsTextView: TextView
    private lateinit var totalValueTextView: TextView
    private lateinit var assetsInUseTextView: TextView
    private lateinit var assetsInStorageTextView: TextView
    private lateinit var assetsInMaintenanceTextView: TextView
    private lateinit var assetsRetiredTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_asset_inventory)
        // Initialize tickIcon
        tickIcon = findViewById(R.id.done)
        // Initialize summary text views
        totalAssetsTextView = findViewById(R.id.total_assets)
        totalValueTextView = findViewById(R.id.total_value)
        assetsInUseTextView = findViewById(R.id.assets_in_use)
        assetsInStorageTextView = findViewById(R.id.assets_in_storage)
        assetsInMaintenanceTextView = findViewById(R.id.assets_in_maintenance)
        assetsRetiredTextView = findViewById(R.id.assets_retired)
        
        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance().reference

        // Initialize RecyclerView
        categoriesRecyclerView = findViewById(R.id.cardsRecyclerView)
        categoriesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        assetCategoryAdapter = AssetCategoryItemAdapter(assetCategoriesList) { categoryItem ->
            // Open modal for the specific asset category
            openAddAssetModal(categoryItem)
        }
        categoriesRecyclerView.adapter = assetCategoryAdapter

        // Fetch asset categories data
        fetchAssetCategories()

        // Check if we should show the move dialog
        if (intent.getBooleanExtra("SHOW_MOVE_DIALOG", false)) {
            val assetId = intent.getStringExtra("ASSET_ID")
            if (assetId != null) {
                showMoveAssetDialog(assetId)
            }
        }

        var goToHome = findViewById<ImageView>(R.id.menu_icon)
        goToHome.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }


        // Set up notification navigation button
        val btn3 = findViewById<ImageView>(R.id.notification_icon)
        btn3.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val intent = Intent(this, NotificationActivity::class.java)
                startActivity(intent)
                true
            } else {
                false
            }
        }

        // Inflating the modal layout and add it to the main content view
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        modalView = LayoutInflater.from(this).inflate(R.layout.activity_add_card, rootView, false)

        // Initially, hide the modal
        modalView.visibility = View.GONE
        rootView.addView(modalView)

        // Set up add category FAB
        val addCategoryFab = findViewById<FloatingActionButton>(R.id.fab_add_category)
        addCategoryFab.setOnClickListener {
            openAddCategoryModal()
        }

        // Inflating the add category modal and add it to the main content view
        categoryModalView = LayoutInflater.from(this).inflate(R.layout.add_board, rootView, false)
        categoryModalView.visibility = View.GONE
        rootView.addView(categoryModalView)

        // Set up the Close button for category modal
        val closeCategoryButton: ImageView = categoryModalView.findViewById(R.id.topCloseButton)
        closeCategoryButton.setOnClickListener {
            categoryModalView.visibility = View.GONE
        }

        // Set up the Send button for category modal
        val sendCategoryButton: ImageView = categoryModalView.findViewById(R.id.sendButton)
        sendCategoryButton.setOnClickListener {
            val categoryNameInput: EditText = categoryModalView.findViewById(R.id.boardNameInput)
            val categoryName = categoryNameInput.text.toString().trim()

            if (categoryName.isNotEmpty()) {
                addCategoryToDatabase(categoryName)
                categoryNameInput.text.clear()
                categoryModalView.visibility = View.GONE
            } else {
                Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up the Close button inside the modal
        val topCloseButton: ImageView = modalView.findViewById(R.id.topCloseButton)
        topCloseButton.setOnClickListener {
            modalView.visibility = View.GONE
        }

        // Hide the modal when clicking outside the main modal area (this will close the modal)
        modalView.setOnClickListener {
            modalView.visibility = View.GONE
        }

        menuIcon = findViewById(R.id.menu_icon)
        filterIcon = findViewById(R.id.filter_icon)
        notificationIcon = findViewById(R.id.notification_icon)
        optionIcon = findViewById(R.id.option_icon)

        val headingBtn = findViewById<EditText>(R.id.board_heading)
        headingBtn.hint = "Search assets..."

        headingBtn.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                OnOpenSearchBar()
            }
            else{
                OnCloseSearchBar()
            }
        }
        
        // Add text change listener for search functionality
        headingBtn.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: android.text.Editable?) {
                // Only search if text is entered and search bar is active
                if (tickIcon.visibility == View.VISIBLE) {
                    val query = s.toString().trim()
                    searchAssets(query)
                }
            }
        })

        menuIcon.setOnClickListener {
            OnCloseSearchBar()
        }
        tickIcon.setOnClickListener {
            OnCloseSearchBar()
            headingBtn.clearFocus()
        }
        
        // Set up filter button to show status filter dialog
        filterIcon.setOnClickListener {
            showStatusFilterDialog()
        }

    }

    private fun OnOpenSearchBar() {
        filterIcon.visibility = View.GONE          // Hide filterIcon
        notificationIcon.visibility = View.GONE    // Hide notificationIcon
        optionIcon.visibility = View.GONE          // Hide optionIcon
        tickIcon.visibility = View.VISIBLE         // Show tickIcon
    }

    private fun OnCloseSearchBar() {
        filterIcon.visibility = View.VISIBLE       // Show filterIcon
        notificationIcon.visibility = View.VISIBLE // Show notificationIcon
        optionIcon.visibility = View.VISIBLE       // Show optionIcon
        tickIcon.visibility = View.GONE            // Hide tickIcon
    }

    private fun fetchAssetCategories() {
        database.child("asset_categories").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                assetCategoriesList.clear()
                var totalAssets = 0L
                var totalAssetValue = 0.0
                var assetsInUse = 0
                var assetsInStorage = 0
                var assetsInMaintenance = 0
                var assetsRetired = 0
                
                if (snapshot.exists()) {
                    for (categorySnapshot in snapshot.children) {
                        val categoryId = categorySnapshot.key ?: "" // Get the category ID
                        val categoryName = categorySnapshot.child("name").getValue(String::class.java) ?: ""
                        val categoryImage = categorySnapshot.child("image").getValue(String::class.java) ?: ""
                        val assetCount = categorySnapshot.child("assetCount").getValue(Long::class.java) ?: 0
                        val totalValue = categorySnapshot.child("totalValue").getValue(Double::class.java) ?: 0.0
                        val assets = mutableListOf<AssetItem>()

                        // Update totals
                        totalAssets += assetCount
                        totalAssetValue += totalValue

                        // Fetch assets for this category
                        val assetsSnapshot = categorySnapshot.child("assets")
                        if (assetsSnapshot.exists()) {
                            for (asset in assetsSnapshot.children) {
                                val assetId = asset.key ?: ""
                                val assetName = asset.child("name").getValue(String::class.java) ?: ""
                                val assetValueObj = asset.child("value").getValue()
                                // Safely handle different possible value types
                                val assetValue = when (assetValueObj) {
                                    is Double -> assetValueObj
                                    is Long -> assetValueObj.toDouble()
                                    is Int -> assetValueObj.toDouble()
                                    is String -> assetValueObj.toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }
                                val assetLocation = asset.child("location").getValue(String::class.java) ?: ""
                                val assetStatus = asset.child("status").getValue(String::class.java) ?: "Unknown"
                                val purchaseDate = asset.child("purchaseDate").getValue(Long::class.java) ?: 0L
                                val assignedTo = asset.child("assignedTo").getValue(String::class.java) ?: ""
                                val maintenanceStatus = asset.child("maintenanceStatus").getValue(String::class.java) ?: "No maintenance scheduled"
                                val lastServiceDate = asset.child("lastServiceDate").getValue(Long::class.java) ?: 0L
                                val nextServiceDate = asset.child("nextServiceDate").getValue(Long::class.java) ?: 0L
                                val serialNumber = asset.child("serialNumber").getValue(String::class.java) ?: ""
                                val manufacturer = asset.child("manufacturer").getValue(String::class.java) ?: ""
                                
                                // Update asset status counts based on case-insensitive status text
                                when {
                                    assetStatus.contains("In Use", ignoreCase = true) -> assetsInUse++
                                    assetStatus.contains("Storage", ignoreCase = true) -> assetsInStorage++
                                    assetStatus.contains("Maintenance", ignoreCase = true) -> assetsInMaintenance++
                                    assetStatus.contains("Retired", ignoreCase = true) || 
                                    assetStatus.contains("Disposed", ignoreCase = true) -> assetsRetired++
                                }
                                
                                assets.add(AssetItem(
                                    assetId, 
                                    assetName, 
                                    assetValue, 
                                    assetLocation, 
                                    assetStatus, 
                                    purchaseDate, 
                                    assignedTo,
                                    maintenanceStatus,
                                    lastServiceDate,
                                    nextServiceDate,
                                    serialNumber,
                                    manufacturer
                                ))
                            }
                        }

                        assetCategoriesList.add(AssetCategoryItem(categoryId, categoryName, categoryImage, assetCount, totalValue, assets))
                    }
                    
                    // Update summary section
                    updateAssetSummary(totalAssets, totalAssetValue)
                    
                    // Update asset lifecycle section
                    updateAssetLifecycleView(assetsInUse, assetsInStorage, assetsInMaintenance, assetsRetired)
                    
                    assetCategoryAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(this@AssetInventoryActivity, "No asset categories found", Toast.LENGTH_SHORT).show()
                    // Clear summary with zeros
                    updateAssetSummary(0, 0.0)
                    updateAssetLifecycleView(0, 0, 0, 0)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AssetInventoryActivity, "Error fetching data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun updateAssetSummary(totalAssets: Long, totalValue: Double) {
        totalAssetsTextView.text = "Total Assets: $totalAssets"
        totalValueTextView.text = "Total Value: $${String.format("%.2f", totalValue)}"
    }
    
    private fun updateAssetLifecycleView(inUse: Int, inStorage: Int, inMaintenance: Int, retired: Int) {
        assetsInUseTextView.text = inUse.toString()
        assetsInStorageTextView.text = inStorage.toString()
        assetsInMaintenanceTextView.text = inMaintenance.toString()
        assetsRetiredTextView.text = retired.toString()
        
        // Add tooltips for the lifecycle statuses
        val inUseLayout = findViewById<LinearLayout>(R.id.inuse_layout)
        val inStorageLayout = findViewById<LinearLayout>(R.id.instorage_layout)
        val inMaintenanceLayout = findViewById<LinearLayout>(R.id.maintenance_layout)
        val retiredLayout = findViewById<LinearLayout>(R.id.retired_layout)
        
        inUseLayout?.setOnClickListener {
            Toast.makeText(this, "Assets currently in use by employees or departments", Toast.LENGTH_SHORT).show()
        }
        
        inStorageLayout?.setOnClickListener {
            Toast.makeText(this, "Assets stored in warehouse or inventory", Toast.LENGTH_SHORT).show()
        }
        
        inMaintenanceLayout?.setOnClickListener {
            Toast.makeText(this, "Assets currently undergoing repairs or maintenance", Toast.LENGTH_SHORT).show()
        }
        
        retiredLayout?.setOnClickListener {
            Toast.makeText(this, "Assets that are no longer in use, deprecated, or disposed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addAssetToDatabase(categoryItem: AssetCategoryItem, assetName: String, assetValue: Double, 
                                   assetLocation: String, assetStatus: String, serialNumber: String = "", 
                                   manufacturer: String = "") {
        val categoryId = categoryItem.categoryId

        val assetId = database.child("asset_categories").child(categoryId).child("assets").push().key
        if (assetId != null) {
            val currentTime = System.currentTimeMillis()
            
            // Calculate next service date (6 months from now)
            val sixMonthsFromNow = currentTime + (6 * 30 * 24 * 60 * 60 * 1000) // Approximate 6 months in milliseconds
            
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
            
            // Update the asset count and total value in the category
            val updates = HashMap<String, Any>()
            updates["asset_categories/$categoryId/assets/$assetId"] = assetData
            updates["asset_categories/$categoryId/assetCount"] = categoryItem.assetCount + 1
            updates["asset_categories/$categoryId/totalValue"] = categoryItem.totalValue + assetValue
            
            database.updateChildren(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Asset added successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add asset", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun openAddAssetModal(categoryItem: AssetCategoryItem) {
        modalView.visibility = View.VISIBLE

        // Set up the modal's input fields
        val nameField: EditText = modalView.findViewById(R.id.inputField)
        val valueField: EditText = modalView.findViewById(R.id.valueField)
        val locationField: EditText = modalView.findViewById(R.id.locationField)
        val statusField: EditText = modalView.findViewById(R.id.statusField)
        val serialNumberField: EditText = modalView.findViewById(R.id.serialNumberField)
        val manufacturerField: EditText = modalView.findViewById(R.id.manufacturerField)
        val sendButton: ImageView = modalView.findViewById(R.id.sendButton)
        val modalTitle: TextView = modalView.findViewById(R.id.modalTitle)

        // Update the title to show which category we're adding to
        modalTitle.text = "Add Asset to ${categoryItem.name}"

        // Clear previous input
        nameField.text.clear()
        valueField.text.clear()
        locationField.text.clear()
        statusField.text.clear()
        serialNumberField.text.clear()
        manufacturerField.text.clear()

        // Set default status if empty
        statusField.hint = "Status (In Use, In Storage, etc.)"

        // Handle Add Asset action
        sendButton.setOnClickListener {
            val assetName = nameField.text.toString().trim()
            val assetValueStr = valueField.text.toString().trim()
            val assetLocation = locationField.text.toString().trim()
            val assetStatus = statusField.text.toString().trim()
            val serialNumber = serialNumberField.text.toString().trim()
            val manufacturer = manufacturerField.text.toString().trim()
            
            if (assetName.isNotEmpty() && assetValueStr.isNotEmpty()) {
                try {
                    val assetValue = assetValueStr.toDouble()
                    val finalStatus = if (assetStatus.isEmpty()) "In Use" else assetStatus
                    addAssetToDatabase(categoryItem, assetName, assetValue, assetLocation, finalStatus, serialNumber, manufacturer)
                    modalView.visibility = View.GONE // Close modal after adding
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Please enter a valid asset value", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter asset name and value", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up the Close button inside the modal
        val closeButton: ImageView = modalView.findViewById(R.id.topCloseButton)
        closeButton.setOnClickListener {
            modalView.visibility = View.GONE
        }
    }
    
    // Calculate depreciation for an asset based on straight-line method
    private fun calculateDepreciation(purchaseValue: Double, purchaseDate: Long, usefulLifeYears: Int): Double {
        val currentTime = System.currentTimeMillis()
        val ageInMillis = currentTime - purchaseDate
        
        // Convert to years
        val ageInYears = ageInMillis / (365 * 24 * 60 * 60 * 1000.0)
        
        // If older than useful life, asset is fully depreciated
        if (ageInYears >= usefulLifeYears) {
            return 0.0
        }
        
        // Annual depreciation amount
        val depreciationPerYear = purchaseValue / usefulLifeYears
        
        // Current value
        return purchaseValue - (depreciationPerYear * ageInYears)
    }
    
    // Format date from timestamp
    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "Not set"
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    private fun openAddCategoryModal() {
        categoryModalView.visibility = View.VISIBLE
    }

    private fun addCategoryToDatabase(categoryName: String) {
        val categoryId = database.child("asset_categories").push().key

        if (categoryId != null) {
            val categoryData = mapOf(
                "name" to categoryName,
                "image" to "", // Empty image URL
                "assetCount" to 0L,
                "totalValue" to 0.0
            )
            
            database.child("asset_categories").child(categoryId).setValue(categoryData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Category added successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to add category", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Add a method to show the move asset dialog
    private fun showMoveAssetDialog(assetId: String) {
        // Create a dialog for selecting the target category
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Move Asset")
        
        // Get the category names for the selection
        val categoryNames = assetCategoriesList.map { it.name }.toTypedArray()
        
        if (categoryNames.isEmpty()) {
            Toast.makeText(this, "No categories available to move to", Toast.LENGTH_SHORT).show()
            return
        }
        
        builder.setItems(categoryNames) { dialog, which ->
            // Get the selected category
            val targetCategory = assetCategoriesList[which]
            
            // Show a confirmation toast
            Toast.makeText(this, "Moving asset to ${targetCategory.name}", Toast.LENGTH_SHORT).show()
            
            // Implement the actual asset move functionality
            moveAssetToCategory(assetId, targetCategory.categoryId)
        }
        
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        
        val dialog = builder.create()
        dialog.show()
    }
    
    private fun moveAssetToCategory(assetId: String, targetCategoryId: String) {
        // Show progress to user
        val progressDialog = android.app.ProgressDialog(this)
        progressDialog.setMessage("Moving asset to new category...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        // First, find which category contains this asset
        var sourceCategory: String? = null
        var assetData: Map<String, Any>? = null
        
        database.child("asset_categories").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    progressDialog.dismiss()
                    Toast.makeText(this@AssetInventoryActivity, "Error: Categories not found", Toast.LENGTH_SHORT).show()
                    return
                }
                
                // Find the asset in all categories
                for (categorySnapshot in snapshot.children) {
                    val categoryId = categorySnapshot.key ?: continue
                    val assetsSnapshot = categorySnapshot.child("assets")
                    
                    if (assetsSnapshot.hasChild(assetId)) {
                        sourceCategory = categoryId
                        
                        // Get complete asset data
                        val asset = assetsSnapshot.child(assetId)
                        val assetMap = mutableMapOf<String, Any>()
                        
                        // Copy all asset properties with safer null handling
                        for (property in asset.children) {
                            val key = property.key
                            val value = property.value
                            
                            if (key != null && value != null) {
                                assetMap[key] = value
                            }
                        }
                        assetData = assetMap
                        break
                    }
                }
                
                // Check if we found the asset
                if (sourceCategory == null || assetData == null) {
                    progressDialog.dismiss()
                    Toast.makeText(this@AssetInventoryActivity, "Asset not found in any category", Toast.LENGTH_SHORT).show()
                    return
                }
                
                // Check if source and target are the same
                if (sourceCategory == targetCategoryId) {
                    progressDialog.dismiss()
                    Toast.makeText(this@AssetInventoryActivity, "Asset is already in this category", Toast.LENGTH_SHORT).show()
                    return
                }
                
                // Get source category details
                val sourceCategorySnapshot = snapshot.child(sourceCategory!!)
                val sourceCategoryAssetCount = sourceCategorySnapshot.child("assetCount").getValue(Long::class.java) ?: 0
                val sourceCategoryTotalValue = sourceCategorySnapshot.child("totalValue").getValue(Double::class.java) ?: 0.0
                
                // Get target category details
                val targetCategorySnapshot = snapshot.child(targetCategoryId)
                if (!targetCategorySnapshot.exists()) {
                    progressDialog.dismiss()
                    Toast.makeText(this@AssetInventoryActivity, "Target category not found", Toast.LENGTH_SHORT).show()
                    return
                }
                
                val targetCategoryAssetCount = targetCategorySnapshot.child("assetCount").getValue(Long::class.java) ?: 0
                val targetCategoryTotalValue = targetCategorySnapshot.child("totalValue").getValue(Double::class.java) ?: 0.0
                
                // Get asset value for updates with safer null handling
                val assetValue = when (val valueObj = assetData?.get("value")) {
                    is Double -> valueObj
                    is Long -> valueObj.toDouble()
                    is Int -> valueObj.toDouble()
                    is String -> valueObj.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
                
                // Create batch update for adding to target category
                val updates = HashMap<String, Any>()
                
                // Add asset to target category
                updates["asset_categories/$targetCategoryId/assets/$assetId"] = assetData!!
                
                // Update target category stats
                updates["asset_categories/$targetCategoryId/assetCount"] = targetCategoryAssetCount + 1
                updates["asset_categories/$targetCategoryId/totalValue"] = targetCategoryTotalValue + assetValue
                
                // Update source category stats
                updates["asset_categories/$sourceCategory/assetCount"] = sourceCategoryAssetCount - 1
                updates["asset_categories/$sourceCategory/totalValue"] = sourceCategoryTotalValue - assetValue
                
                // Execute the batch update
                database.updateChildren(updates)
                    .addOnSuccessListener {
                        // Now remove the asset from source category in a separate call
                        database.child("asset_categories").child(sourceCategory!!).child("assets").child(assetId)
                            .removeValue()
                            .addOnSuccessListener {
                                progressDialog.dismiss()
                                
                                // Get the asset and category names for a more informative message with null safety
                                val assetName = when (val nameObj = assetData?.get("name")) {
                                    is String -> nameObj
                                    else -> "Asset"
                                }
                                val targetCategoryName = targetCategorySnapshot.child("name").getValue(String::class.java) ?: "new category"
                                
                                Toast.makeText(
                                    this@AssetInventoryActivity, 
                                    "Successfully moved \"$assetName\" to \"$targetCategoryName\"", 
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                // Refresh data
                                fetchAssetCategories()
                            }
                            .addOnFailureListener { e ->
                                progressDialog.dismiss()
                                Toast.makeText(
                                    this@AssetInventoryActivity, 
                                    "Error removing from original category: ${e.message}", 
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@AssetInventoryActivity, 
                            "Failed to move asset: ${e.message}", 
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            
            override fun onCancelled(error: DatabaseError) {
                progressDialog.dismiss()
                Toast.makeText(this@AssetInventoryActivity, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Implementation of search functionality
    private fun searchAssets(query: String) {
        if (query.isEmpty()) {
            // If search is empty, just reload all categories
            fetchAssetCategories()
            return
        }
        
        val lowercaseQuery = query.lowercase()
        database.child("asset_categories").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                assetCategoriesList.clear()
                var totalAssets = 0L
                var totalAssetValue = 0.0
                var assetsInUse = 0
                var assetsInStorage = 0
                var assetsInMaintenance = 0
                var assetsRetired = 0
                
                if (snapshot.exists()) {
                    for (categorySnapshot in snapshot.children) {
                        val categoryId = categorySnapshot.key ?: ""
                        val categoryName = categorySnapshot.child("name").getValue(String::class.java) ?: ""
                        val categoryImage = categorySnapshot.child("image").getValue(String::class.java) ?: ""
                        val assetCount = categorySnapshot.child("assetCount").getValue(Long::class.java) ?: 0
                        val totalValue = categorySnapshot.child("totalValue").getValue(Double::class.java) ?: 0.0
                        val filteredAssets = mutableListOf<AssetItem>()

                        // Fetch and filter assets for this category
                        val assetsSnapshot = categorySnapshot.child("assets")
                        if (assetsSnapshot.exists()) {
                            for (asset in assetsSnapshot.children) {
                                val assetId = asset.key ?: ""
                                val assetName = asset.child("name").getValue(String::class.java) ?: ""
                                val assetValueObj = asset.child("value").getValue()
                                // Safely handle different possible value types
                                val assetValue = when (assetValueObj) {
                                    is Double -> assetValueObj
                                    is Long -> assetValueObj.toDouble()
                                    is Int -> assetValueObj.toDouble()
                                    is String -> assetValueObj.toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }
                                val assetLocation = asset.child("location").getValue(String::class.java) ?: ""
                                val assetStatus = asset.child("status").getValue(String::class.java) ?: "Unknown"
                                val serialNumber = asset.child("serialNumber").getValue(String::class.java) ?: ""
                                val manufacturer = asset.child("manufacturer").getValue(String::class.java) ?: ""
                                val assignedTo = asset.child("assignedTo").getValue(String::class.java) ?: ""
                                val maintenanceStatus = asset.child("maintenanceStatus").getValue(String::class.java) ?: "No maintenance scheduled"
                                val purchaseDate = asset.child("purchaseDate").getValue(Long::class.java) ?: 0L
                                val lastServiceDate = asset.child("lastServiceDate").getValue(Long::class.java) ?: 0L
                                val nextServiceDate = asset.child("nextServiceDate").getValue(Long::class.java) ?: 0L
                                
                                // Add asset if it matches search query
                                if (assetName.lowercase().contains(lowercaseQuery) || 
                                    serialNumber.lowercase().contains(lowercaseQuery) ||
                                    manufacturer.lowercase().contains(lowercaseQuery) ||
                                    assetLocation.lowercase().contains(lowercaseQuery) ||
                                    assignedTo.lowercase().contains(lowercaseQuery)) {
                                    
                                    // Update asset status counts
                                    when {
                                        assetStatus.contains("In Use", ignoreCase = true) -> assetsInUse++
                                        assetStatus.contains("Storage", ignoreCase = true) -> assetsInStorage++
                                        assetStatus.contains("Maintenance", ignoreCase = true) -> assetsInMaintenance++
                                        assetStatus.contains("Retired", ignoreCase = true) || 
                                        assetStatus.contains("Disposed", ignoreCase = true) -> assetsRetired++
                                    }
                                    
                                    filteredAssets.add(AssetItem(
                                        assetId, 
                                        assetName, 
                                        assetValue, 
                                        assetLocation, 
                                        assetStatus, 
                                        purchaseDate, 
                                        assignedTo,
                                        maintenanceStatus,
                                        lastServiceDate,
                                        nextServiceDate,
                                        serialNumber,
                                        manufacturer
                                    ))
                                    totalAssets++
                                    totalAssetValue += assetValue
                                }
                            }
                        }

                        // Only include categories that have matching assets
                        if (filteredAssets.isNotEmpty()) {
                            assetCategoriesList.add(AssetCategoryItem(
                                categoryId, 
                                categoryName, 
                                categoryImage, 
                                filteredAssets.size.toLong(), 
                                filteredAssets.sumOf { it.value }, 
                                filteredAssets
                            ))
                        }
                    }
                    
                    // Update summary with filtered data
                    updateAssetSummary(totalAssets, totalAssetValue)
                    updateAssetLifecycleView(assetsInUse, assetsInStorage, assetsInMaintenance, assetsRetired)
                    
                    assetCategoryAdapter.notifyDataSetChanged()
                    
                    if (assetCategoriesList.isEmpty()) {
                        Toast.makeText(this@AssetInventoryActivity, "No matching assets found", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AssetInventoryActivity, "Error searching assets: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Method to handle status filtering
    private fun showStatusFilterDialog() {
        val statusOptions = arrayOf(
            "All Assets", 
            "In Use Assets", 
            "Assets In Storage", 
            "Assets In Maintenance", 
            "Retired Assets"
        )
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Filter Assets by Status")
        builder.setItems(statusOptions) { dialog, which ->
            when (which) {
                0 -> fetchAssetCategories() // Show all assets
                1 -> filterAssetsByStatus("In Use")
                2 -> filterAssetsByStatus("Storage")
                3 -> filterAssetsByStatus("Maintenance")
                4 -> filterAssetsByStatus("Retired")
            }
        }
        builder.show()
    }
    
    private fun filterAssetsByStatus(statusFilter: String) {
        database.child("asset_categories").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                assetCategoriesList.clear()
                var totalFilteredAssets = 0L
                var totalFilteredValue = 0.0
                var assetsInUse = 0
                var assetsInStorage = 0
                var assetsInMaintenance = 0
                var assetsRetired = 0
                
                if (snapshot.exists()) {
                    for (categorySnapshot in snapshot.children) {
                        val categoryId = categorySnapshot.key ?: ""
                        val categoryName = categorySnapshot.child("name").getValue(String::class.java) ?: ""
                        val categoryImage = categorySnapshot.child("image").getValue(String::class.java) ?: ""
                        val filteredAssets = mutableListOf<AssetItem>()

                        // Fetch assets for this category
                        val assetsSnapshot = categorySnapshot.child("assets")
                        if (assetsSnapshot.exists()) {
                            for (asset in assetsSnapshot.children) {
                                val assetId = asset.key ?: ""
                                val assetName = asset.child("name").getValue(String::class.java) ?: ""
                                val assetValueObj = asset.child("value").getValue()
                                // Safely handle different possible value types
                                val assetValue = when (assetValueObj) {
                                    is Double -> assetValueObj
                                    is Long -> assetValueObj.toDouble()
                                    is Int -> assetValueObj.toDouble()
                                    is String -> assetValueObj.toDoubleOrNull() ?: 0.0
                                    else -> 0.0
                                }
                                val assetLocation = asset.child("location").getValue(String::class.java) ?: ""
                                val assetStatus = asset.child("status").getValue(String::class.java) ?: "Unknown"
                                val purchaseDate = asset.child("purchaseDate").getValue(Long::class.java) ?: 0L
                                val assignedTo = asset.child("assignedTo").getValue(String::class.java) ?: ""
                                val maintenanceStatus = asset.child("maintenanceStatus").getValue(String::class.java) ?: "No maintenance scheduled"
                                val lastServiceDate = asset.child("lastServiceDate").getValue(Long::class.java) ?: 0L
                                val nextServiceDate = asset.child("nextServiceDate").getValue(Long::class.java) ?: 0L
                                val serialNumber = asset.child("serialNumber").getValue(String::class.java) ?: ""
                                val manufacturer = asset.child("manufacturer").getValue(String::class.java) ?: ""
                                
                                // Check asset status against filter
                                val statusMatches = assetStatus.contains(statusFilter, ignoreCase = true)
                                
                                // Special case for retired assets which can be "Retired" or "Disposed"
                                val isRetiredFilter = statusFilter == "Retired" && 
                                                    (assetStatus.contains("Retired", ignoreCase = true) || 
                                                     assetStatus.contains("Disposed", ignoreCase = true))
                                
                                if (statusMatches || isRetiredFilter) {
                                    // Update asset status counts
                                    when {
                                        assetStatus.contains("In Use", ignoreCase = true) -> assetsInUse++
                                        assetStatus.contains("Storage", ignoreCase = true) -> assetsInStorage++
                                        assetStatus.contains("Maintenance", ignoreCase = true) -> assetsInMaintenance++
                                        assetStatus.contains("Retired", ignoreCase = true) || 
                                        assetStatus.contains("Disposed", ignoreCase = true) -> assetsRetired++
                                    }
                                    
                                    filteredAssets.add(AssetItem(
                                        assetId, 
                                        assetName, 
                                        assetValue, 
                                        assetLocation, 
                                        assetStatus, 
                                        purchaseDate, 
                                        assignedTo,
                                        maintenanceStatus,
                                        lastServiceDate,
                                        nextServiceDate,
                                        serialNumber,
                                        manufacturer
                                    ))
                                    
                                    totalFilteredAssets++
                                    totalFilteredValue += assetValue
                                }
                            }
                        }

                        // Only include categories that have matching assets
                        if (filteredAssets.isNotEmpty()) {
                            assetCategoriesList.add(AssetCategoryItem(
                                categoryId, 
                                categoryName, 
                                categoryImage, 
                                filteredAssets.size.toLong(), 
                                filteredAssets.sumOf { it.value }, 
                                filteredAssets
                            ))
                        }
                    }
                    
                    // Update summary section with filtered data
                    updateAssetSummary(totalFilteredAssets, totalFilteredValue)
                    updateAssetLifecycleView(assetsInUse, assetsInStorage, assetsInMaintenance, assetsRetired)
                    
                    assetCategoryAdapter.notifyDataSetChanged()
                    
                    if (assetCategoriesList.isEmpty()) {
                        Toast.makeText(this@AssetInventoryActivity, "No assets found with status: $statusFilter", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@AssetInventoryActivity, "Showing $totalFilteredAssets $statusFilter assets", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AssetInventoryActivity, "Error filtering assets: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

data class AssetItem(
    val id: String,
    val name: String,
    val value: Double,
    val location: String,
    val status: String,
    val purchaseDate: Long = 0L,
    val assignedTo: String = "",
    val maintenanceStatus: String = "No maintenance scheduled",
    val lastServiceDate: Long = 0L,
    val nextServiceDate: Long = 0L,
    val serialNumber: String = "",
    val manufacturer: String = ""
)

class AssetAdapter(private val assets: List<AssetItem>) : RecyclerView.Adapter<AssetAdapter.AssetViewHolder>() {

    inner class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val assetName: TextView = itemView.findViewById(R.id.card_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card_simple, parent, false)
        return AssetViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        val asset = assets[position]
        val formattedValue = String.format("%.2f", asset.value)
        holder.assetName.text = if (asset.serialNumber.isNotEmpty()) {
            "${asset.name} - SN:${asset.serialNumber} - $${formattedValue}"
        } else {
            "${asset.name} - $${formattedValue}"
        }
    }

    override fun getItemCount() = assets.size
}

data class AssetCategoryItem(
    val categoryId: String,
    val name: String,
    val image: String,
    val assetCount: Long,
    val totalValue: Double,
    val assets: List<AssetItem> = emptyList()
) 