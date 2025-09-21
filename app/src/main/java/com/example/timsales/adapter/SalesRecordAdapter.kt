package com.example.timsales.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timsales.R
import com.example.timsales.data.model.SalesRecord

/**
 * Adapter สำหรับแสดงรายการยอดขายใน RecyclerView
 */
class SalesRecordAdapter(
    private var salesRecords: List<SalesRecord>,
    private val onEditClick: (SalesRecord) -> Unit,
    private val onDeleteClick: (SalesRecord) -> Unit
) : RecyclerView.Adapter<SalesRecordAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvItemNumber: TextView = view.findViewById(R.id.tvItemNumber)
        val tvSaleDate: TextView = view.findViewById(R.id.tvSaleDate)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvWeight: TextView = view.findViewById(R.id.tvWeight)
        val tvUnitPrice: TextView = view.findViewById(R.id.tvUnitPrice)
        val tvCreatedDate: TextView = view.findViewById(R.id.tvCreatedDate)
        val tvUpdatedDate: TextView = view.findViewById(R.id.tvUpdatedDate)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sales_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = salesRecords[position]
        
        // Bind item number (1-based index)
        val itemNumber = position + 1
        holder.tvItemNumber.text = itemNumber.toString()
        
        // Set background color based on position for variety
        val backgroundRes = when (position % 4) {
            0 -> R.drawable.circle_number_bg_blue
            1 -> R.drawable.circle_number_bg_green
            2 -> R.drawable.circle_number_bg_orange
            3 -> R.drawable.circle_number_bg_purple
            else -> R.drawable.circle_number_bg_blue
        }
        holder.tvItemNumber.setBackgroundResource(backgroundRes)

        // Bind main data to views
        holder.tvSaleDate.text = record.getFormattedDate()
        holder.tvAmount.text = record.getFormattedAmount()
        holder.tvWeight.text = record.getFormattedWeight()
        holder.tvUnitPrice.text = record.getFormattedUnitPrice()
        
        // Bind timestamp data with proper formatting
        holder.tvCreatedDate.text = "สร้าง: ${record.getFormattedCreatedDate()}"
        holder.tvUpdatedDate.text = "แก้ไข: ${record.getFormattedUpdatedDate()}"

        // Set click listeners
        holder.btnEdit.setOnClickListener {
            onEditClick(record)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(record)
        }

        // Optional: Click on item to edit
        holder.itemView.setOnClickListener {
            onEditClick(record)
        }
    }

    override fun getItemCount(): Int = salesRecords.size

    /**
     * Update data in adapter
     */
    fun updateData(newRecords: List<SalesRecord>) {
        salesRecords = newRecords
        notifyDataSetChanged()
    }

    /**
     * Remove item from adapter
     */
    fun removeItem(record: SalesRecord) {
        val index = salesRecords.indexOf(record)
        if (index != -1) {
            val mutableList = salesRecords.toMutableList()
            mutableList.removeAt(index)
            salesRecords = mutableList
            notifyItemRemoved(index)
            // Update item numbers for all remaining items after this position
            notifyItemRangeChanged(index, salesRecords.size - index)
        }
    }
}