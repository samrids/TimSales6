package com.example.timsales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.timsales.data.model.TimYear
import com.example.timsales.utils.DebugUtils

class TimYearAdapter(
    private val timYearList: MutableList<TimYear>,
    private val onEditClick: (TimYear) -> Unit,
    private val onDeleteClick: (TimYear) -> Unit
) : RecyclerView.Adapter<TimYearAdapter.TimYearViewHolder>() {

    companion object {
        private const val TAG = "TimYearAdapter"
    }

    class TimYearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSaleYear: TextView = itemView.findViewById(R.id.tvSaleYear)
        val tvCreatedDate: TextView = itemView.findViewById(R.id.tvCreatedDate)
        val tvUpdatedDate: TextView = itemView.findViewById(R.id.tvUpdatedDate)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TimYearViewHolder {
        try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_timyear, parent, false)
            return TimYearViewHolder(view)
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in onCreateViewHolder", e)
            throw e
        }
    }

    override fun onBindViewHolder(holder: TimYearViewHolder, position: Int) {
        try {
            val timYear = timYearList[position]
            
            // แสดงปีขาย
            holder.tvSaleYear.text = timYear.getFormattedYear()
            
            // แสดงวันที่สร้าง
            holder.tvCreatedDate.text = "สร้าง: ${timYear.getFormattedCreatedDate()}"
            
            // แสดงวันที่อัปเดต
            holder.tvUpdatedDate.text = "อัปเดต: ${timYear.getFormattedUpdatedDate()}"
            
            // Set click listeners
            holder.btnEdit.setOnClickListener {
                try {
                    onEditClick(timYear)
                } catch (e: Exception) {
                    DebugUtils.logException(TAG, "Error in edit button click", e)
                }
            }
            
            holder.btnDelete.setOnClickListener {
                try {
                    onDeleteClick(timYear)
                } catch (e: Exception) {
                    DebugUtils.logException(TAG, "Error in delete button click", e)
                }
            }
            
            // Set item click listener for entire item
            holder.itemView.setOnClickListener {
                try {
                    onEditClick(timYear)
                } catch (e: Exception) {
                    DebugUtils.logException(TAG, "Error in item click", e)
                }
            }
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in onBindViewHolder at position $position", e)
        }
    }

    override fun getItemCount(): Int {
        return timYearList.size
    }

    /**
     * อัปเดตข้อมูลใน adapter
     */
    fun updateData(newTimYears: List<TimYear>) {
        try {
            
            
            timYearList.clear()
            timYearList.addAll(newTimYears)
            notifyDataSetChanged()
            
            
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error updating adapter data", e)
        }
    }

    /**
     * เพิ่ม TimYear ใหม่
     */
    fun addTimYear(timYear: TimYear) {
        try {
            
            
            timYearList.add(0, timYear) // เพิ่มที่ตำแหน่งแรก
            notifyItemInserted(0)
            
            
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error adding TimYear", e)
        }
    }

    /**
     * อัปเดต TimYear ที่มีอยู่
     */
    fun updateTimYear(updatedTimYear: TimYear) {
        try {
            
            
            val position = timYearList.indexOfFirst { it.id == updatedTimYear.id }
            if (position != -1) {
                timYearList[position] = updatedTimYear
                notifyItemChanged(position)
                
            } else {
                DebugUtils.logError("TimYear with ID ${updatedTimYear.id} not found for update")
            }
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error updating TimYear", e)
        }
    }

    /**
     * ลบ TimYear
     */
    fun removeTimYear(timYearId: Int) {
        try {
            
            
            val position = timYearList.indexOfFirst { it.id == timYearId }
            if (position != -1) {
                val timYear = timYearList[position]
                timYearList.removeAt(position)
                notifyItemRemoved(position)
                
            } else {
                DebugUtils.logError("TimYear with ID $timYearId not found for removal")
            }
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error removing TimYear", e)
        }
    }

    /**
     * ค้นหา TimYear ตาม ID
     */
    fun getTimYearById(id: Int): TimYear? {
        return try {
            timYearList.find { it.id == id }
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error finding TimYear by ID", e)
            null
        }
    }

    /**
     * เคลียร์ข้อมูลทั้งหมด
     */
    fun clearData() {
        try {
            
            
            val size = timYearList.size
            timYearList.clear()
            notifyItemRangeRemoved(0, size)
            
            
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error clearing adapter data", e)
        }
    }
}