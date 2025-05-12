package com.example.trello_clone

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AssetCategoryItemAdapter(
    private val categories: List<AssetCategoryItem>,
    private val onAddAssetClicked: (AssetCategoryItem) -> Unit
) : RecyclerView.Adapter<AssetCategoryItemAdapter.AssetCategoryViewHolder>() {

    inner class AssetCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: TextView = itemView.findViewById(R.id.cardName)
        val categoryImage: ImageView = itemView.findViewById(R.id.iv_card_image)
        val addAssetButton: TextView = itemView.findViewById(R.id.tv_add_card)
        val assetsRecyclerView: RecyclerView = itemView.findViewById(R.id.rv_assets)
        val assetIdView: TextView = itemView.findViewById(R.id.tv_asset_id)
        val assetValueView: TextView = itemView.findViewById(R.id.tv_asset_value)
        val locationView: TextView = itemView.findViewById(R.id.tv_location)
        val statusView: TextView = itemView.findViewById(R.id.tv_asset_status)
        val purchaseDateView: TextView = itemView.findViewById(R.id.tv_purchase_date)
        val assignedToView: TextView = itemView.findViewById(R.id.tv_assigned_to)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetCategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)
        return AssetCategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssetCategoryViewHolder, position: Int) {
        val category = categories[position]

        // Set category name with count and value
        holder.categoryName.text = "${category.name} (${category.assetCount} assets)"
        
        // Set category image
        Glide.with(holder.itemView.context)
            .load(if (category.image.isEmpty()) R.drawable.default_board else category.image)
            .placeholder(R.drawable.default_board)
            .into(holder.categoryImage)

        // Display first asset info if available
        if (category.assets.isNotEmpty()) {
            val firstAsset = category.assets[0]
            holder.assetIdView.text = "Category ID: ${category.categoryId.takeLast(6)}"
            holder.assetValueView.text = "Total Value: $${String.format("%.2f", category.totalValue)}"
            
            // Show location count instead of single location
            val locations = category.assets.map { it.location }.distinct()
            val locationText = if (locations.size == 1 && locations[0].isNotEmpty()) {
                "Location: ${locations[0]}"
            } else if (locations.size > 1) {
                "Locations: ${locations.size} different locations"
            } else {
                "Location: Not specified"
            }
            holder.locationView.text = locationText
            
            // Show status distribution
            val statuses = category.assets.groupBy { it.status }
            val statusText = StringBuilder("Status: ")
            if (statuses.size == 1) {
                statusText.append("All ${statuses.keys.first()}")
            } else {
                val inUseCount = statuses.entries.find { it.key.contains("In Use", ignoreCase = true) }?.value?.size ?: 0
                val inStorageCount = statuses.entries.find { it.key.contains("Storage", ignoreCase = true) }?.value?.size ?: 0
                val inMaintenanceCount = statuses.entries.find { it.key.contains("Maintenance", ignoreCase = true) }?.value?.size ?: 0
                val retiredCount = statuses.entries.find { 
                    it.key.contains("Retired", ignoreCase = true) || it.key.contains("Disposed", ignoreCase = true)
                }?.value?.size ?: 0
                
                if (inUseCount > 0) statusText.append("$inUseCount In Use, ")
                if (inStorageCount > 0) statusText.append("$inStorageCount In Storage, ")
                if (inMaintenanceCount > 0) statusText.append("$inMaintenanceCount In Maintenance, ")
                if (retiredCount > 0) statusText.append("$retiredCount Retired, ")
                
                // Remove trailing comma and space
                if (statusText.endsWith(", ")) {
                    statusText.setLength(statusText.length - 2)
                }
            }
            holder.statusView.text = statusText.toString()
            
            // Calculate average age of assets
            val averageAgeMillis = category.assets
                .filter { it.purchaseDate > 0 }
                .map { System.currentTimeMillis() - it.purchaseDate }
                .average()
                
            val purchaseDateStr = if (!averageAgeMillis.isNaN()) {
                val averageAgeDays = (averageAgeMillis / (24 * 60 * 60 * 1000)).toInt()
                "Avg. Age: ${if (averageAgeDays > 365) "${averageAgeDays / 365} years" else "$averageAgeDays days"}"
            } else {
                "Avg. Age: Unknown"
            }
            holder.purchaseDateView.text = purchaseDateStr
            
            // Show maintenance info
            val nextMaintenanceAsset = category.assets
                .filter { it.nextServiceDate > System.currentTimeMillis() }
                .minByOrNull { it.nextServiceDate }
                
            val maintenanceText = if (nextMaintenanceAsset != null) {
                val daysToMaintenance = ((nextMaintenanceAsset.nextServiceDate - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)).toInt()
                "Next Maintenance: $daysToMaintenance days"
            } else {
                "No scheduled maintenance"
            }
            holder.assignedToView.text = maintenanceText
            
            // Only show assets in RecyclerView if we have them
            if (category.assets.size > 1) {
                holder.assetsRecyclerView.visibility = View.VISIBLE
                
                // Only display the remaining assets (skip the first one already shown in the summary)
                val remainingAssets = category.assets.drop(1)
                val assetsAdapter = ImprovedAssetAdapter(remainingAssets)
                holder.assetsRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
                holder.assetsRecyclerView.adapter = assetsAdapter
            } else {
                holder.assetsRecyclerView.visibility = View.GONE
            }
        } else {
            // No assets, show default values
            holder.assetIdView.text = "Category ID: ${category.categoryId.takeLast(6)}"
            holder.assetValueView.text = "Total Value: $${String.format("%.2f", category.totalValue)}"
            holder.locationView.text = "Location: None"
            holder.statusView.text = "Status: No assets"
            holder.purchaseDateView.text = "Purchase Date: N/A"
            holder.assignedToView.text = "No assets in this category"
            holder.assetsRecyclerView.visibility = View.GONE
        }

        // Navigate to EditAsset on card click with proper CATEGORY_ID
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, EditAsset::class.java)
            intent.putExtra("CATEGORY_ID", category.categoryId)
            context.startActivity(intent)
        }

        // Handle Add Asset button click
        holder.addAssetButton.text = "+ Add Asset"
        holder.addAssetButton.setOnClickListener {
            onAddAssetClicked(category)
        }
    }

    override fun getItemCount() = categories.size
}

// Improved asset adapter with status indicators and better formatting
class ImprovedAssetAdapter(private val assets: List<AssetItem>) : RecyclerView.Adapter<ImprovedAssetAdapter.AssetViewHolder>() {

    inner class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val assetName: TextView = itemView.findViewById(R.id.card_name)
        val assetDetails: TextView = itemView.findViewById(R.id.asset_details)
        val statusIndicator: View = itemView.findViewById(R.id.status_indicator)
        val assetCard: CardView = itemView.findViewById(R.id.asset_card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AssetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_asset_improved, parent, false)
        return AssetViewHolder(view)
    }

    override fun onBindViewHolder(holder: AssetViewHolder, position: Int) {
        val asset = assets[position]
        
        // Set asset name with serial number if available
        holder.assetName.text = if (asset.serialNumber.isNotEmpty()) {
            "${asset.name} (SN: ${asset.serialNumber})"
        } else {
            asset.name
        }
        
        // Format asset details
        val formattedValue = String.format("%.2f", asset.value)
        val formattedDate = formatDate(asset.purchaseDate)
        
        holder.assetDetails.text = "Value: $${formattedValue} • Location: ${asset.location} • Acquired: $formattedDate"
        
        // Set status indicator color
        when {
            asset.status.contains("In Use", ignoreCase = true) -> {
                holder.statusIndicator.setBackgroundColor(Color.parseColor("#4CAF50")) // Green
            }
            asset.status.contains("Storage", ignoreCase = true) -> {
                holder.statusIndicator.setBackgroundColor(Color.parseColor("#FFC107")) // Yellow
            }
            asset.status.contains("Maintenance", ignoreCase = true) -> {
                holder.statusIndicator.setBackgroundColor(Color.parseColor("#FF5722")) // Orange
            }
            asset.status.contains("Retired", ignoreCase = true) || 
            asset.status.contains("Disposed", ignoreCase = true) -> {
                holder.statusIndicator.setBackgroundColor(Color.parseColor("#9E9E9E")) // Gray
            }
            else -> {
                holder.statusIndicator.setBackgroundColor(Color.parseColor("#2196F3")) // Blue (default)
            }
        }
        
        // Set up click to navigate to asset detail
        holder.assetCard.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, EditAsset::class.java)
            // Safer handling of the asset ID to prevent null type errors
            val assetIdParts = asset.id.split("/")
            val categoryId = if (assetIdParts.isNotEmpty() && assetIdParts.size > 1) {
                assetIdParts[0]
            } else {
                asset.id // Use the full ID as a fallback
            }
            intent.putExtra("CATEGORY_ID", categoryId)
            intent.putExtra("ASSET_ID", asset.id)
            context.startActivity(intent)
        }
    }
    
    private fun formatDate(timestamp: Long): String {
        if (timestamp == 0L) return "N/A"
        val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    override fun getItemCount() = assets.size
} 