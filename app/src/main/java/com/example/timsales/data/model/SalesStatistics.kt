package com.example.timsales.data.model

/**
 * Data class สำหรับเก็บสถิติยอดขายประจำปี 2025
 */
data class SalesStatistics(
    val totalAmount: Double = 0.0,
    val totalKg: Double = 0.0,
    val averageUnitPrice: Double = 0.0,
    val recordCount: Int = 0
) {
    /**
     * Format ยอดขายรวมสำหรับแสดงผล
     */
    fun getFormattedTotalAmount(): String {
        return if (totalAmount > 0) {
            String.format("฿%,.2f", totalAmount)
        } else {
            "฿0.00"
        }
    }

    /**
     * Format น้ำหนักรวมสำหรับแสดงผล
     */
    fun getFormattedTotalKg(): String {
        return if (totalKg > 0) {
            String.format("%,.2f กก.", totalKg)
        } else {
            "0.00 กก."
        }
    }

    /**
     * Format ราคาเฉลี่ยสำหรับแสดงผล
     */
    fun getFormattedAveragePrice(): String {
        return if (averageUnitPrice > 0) {
            String.format("฿%,.2f/กก.", averageUnitPrice)
        } else {
            "฿0.00/กก."
        }
    }

    /**
     * แสดงจำนวนรายการขาย
     */
    fun getFormattedRecordCount(): String {
        return if (recordCount > 0) {
            "$recordCount รายการ"
        } else {
            "ไม่มีข้อมูล"
        }
    }
}