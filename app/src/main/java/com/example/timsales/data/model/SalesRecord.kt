package com.example.timsales.data.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.google.gson.stream.JsonToken
import java.lang.reflect.Type

/**
 * Custom TypeAdapter สำหรับ timestamp ที่อาจเป็นทั้ง String หรือ Number
 */
class FlexibleTimestampAdapter : TypeAdapter<String?>() {
    override fun write(out: JsonWriter?, value: String?) {
        if (value == null) {
            out?.nullValue()
        } else {
            out?.value(value)
        }
    }

    override fun read(`in`: JsonReader?): String? {
        if (`in` == null) return null
        
        return when (`in`.peek()) {
            JsonToken.NULL -> {
                `in`.nextNull()
                null
            }
            JsonToken.STRING -> `in`.nextString()
            JsonToken.NUMBER -> {
                // Convert Unix timestamp (seconds) to readable format
                val timestamp = `in`.nextDouble().toLong()
                val date = Date(timestamp * 1000) // Convert to milliseconds
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
            }
            else -> `in`.nextString() // Fallback
        }
    }
}

/**
 * Data model สำหรับข้อมูลยอดขายสินค้าเกษตร
 * ตาม Schema ใน Grist: saleDate, kg, unitPrice, amount (calculated), created_at, updated_at
 */
data class SalesRecord(
    val id: Int = 0,
    @SerializedName("saleDate") val saleDate: String, // วันที่ขาย (YYYY-MM-DD format)
    @SerializedName("kg") val kg: Double, // จำนวนกิโลกรัม
    @SerializedName("unitPrice") val unitPrice: Double, // ราคาต่อหน่วย
    @SerializedName("amount") val amount: Double = 0.0, // ยอดรวม (calculated field from Grist)
    @SerializedName("seasonCode") val seasonCode: String? = null, // รอบการขาย (formula column from Grist เช่น "2026/27")
    @SerializedName("created_at") val createdAt: String? = null, // Flexible timestamp from server
    @SerializedName("updated_at") val updatedAt: String? = null // Flexible timestamp from server
) {
    /**
     * ดึงรหัสเดือนของรายการขายในรูปแบบ YYYY-MM สำหรับจัดกลุ่มในกราฟ
     * คืนค่า null หากแยกวันที่ไม่ได้
     */
    fun getYearMonth(): String? {
        return try {
            // saleDate อาจอยู่ในรูป 2026-02-15, 2026-02-15T.., 2026-02-15 ..
            val datePart = saleDate.split("T")[0].split(" ")[0]
            if (datePart.length >= 7) datePart.substring(0, 7) else null
        } catch (e: Exception) {
            null
        }
    }
    /**
     * Format วันที่สำหรับแสดงผล
     */
    fun getFormattedDate(): String {
        return try {
            // Try to parse different date formats
            when {
                saleDate.contains("T") || saleDate.contains("+") -> {
                    // ISO format: 2025-09-06T23:22:09 or 2025-09-06 23:22:09.697746+07:00
                    val cleanDate = saleDate.split("T")[0].split(" ")[0]
                    formatDateString(cleanDate)
                }
                saleDate.contains("-") && saleDate.length >= 10 -> {
                    // Simple date format: 2025-09-06
                    val dateOnly = saleDate.substring(0, 10)
                    formatDateString(dateOnly)
                }
                else -> saleDate
            }
        } catch (e: Exception) {
            saleDate // Return original if parsing fails
        }
    }
    
    private fun formatDateString(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                // แปลงจาก YYYY-MM-DD เป็น DD/MM/YY (ปี 2 หลัก เพื่อไม่ให้ตกบรรทัด)
                val yy = if (parts[0].length == 4) parts[0].substring(2) else parts[0]
                "${parts[2]}/${parts[1]}/$yy"
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * Format จำนวนเงินสำหรับแสดงผล (มีเครื่องหมายจุลภาคเมื่อครบหลักพัน เช่น 17,754.00 บาท)
     */
    fun getFormattedAmount(): String {
        return String.format("%,.2f บาท", amount)
    }

    /**
     * Format น้ำหนักสำหรับแสดงผล
     */
    fun getFormattedWeight(): String {
        return String.format("%,.2f กก.", kg)
    }

    /**
     * Format ราคาต่อหน่วยสำหรับแสดงผล
     */
    fun getFormattedUnitPrice(): String {
        return String.format("%,.2f บาท/กก.", unitPrice)
    }
    
    /**
     * Get created date in readable format
     */
    fun getFormattedCreatedDate(): String {
        return formatTimestamp(createdAt)
    }
    
    /**
     * Get updated date in readable format
     */
    fun getFormattedUpdatedDate(): String {
        return formatTimestamp(updatedAt)
    }
    
    private fun formatTimestamp(timestamp: String?): String {
        if (timestamp.isNullOrEmpty()) return "ไม่ระบุ"
        
        return try {
            // Case 1: Already formatted timestamp (yyyy-MM-dd HH:mm:ss)
            if (timestamp.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"))) {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(timestamp)
                date?.let { outputFormat.format(it) } ?: timestamp
            }
            // Case 2: ISO timestamp with T
            else if (timestamp.contains("T")) {
                val cleanTimestamp = timestamp.split("+")[0].replace("T", " ")
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val date = inputFormat.parse(cleanTimestamp)
                date?.let { outputFormat.format(it) } ?: timestamp
            }
            // Case 3: Unix timestamp as string
            else if (timestamp.matches(Regex("\\d+(\\.\\d+)?"))) {
                val unixTimestamp = timestamp.toDouble().toLong()
                val date = Date(unixTimestamp * 1000) // Convert to milliseconds
                val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                outputFormat.format(date)
            }
            // Case 4: Date only format
            else if (timestamp.contains("-") && timestamp.length >= 10) {
                val datePart = timestamp.substring(0, 10)
                formatDateString(datePart)
            }
            // Default case
            else {
                timestamp
            }
        } catch (e: Exception) {
            // Fallback: try to extract just the date part
            try {
                if (timestamp.contains("T")) {
                    val datePart = timestamp.split("T")[0]
                    formatDateString(datePart)
                } else if (timestamp.contains(" ")) {
                    val datePart = timestamp.split(" ")[0]
                    formatDateString(datePart)
                } else {
                    timestamp
                }
            } catch (e2: Exception) {
                timestamp
            }
        }
    }
}

/**
 * Model สำหรับ API Request เมื่อสร้างหรือแก้ไขข้อมูล
 * ไม่รวม ID และ calculated fields
 */
data class SalesRecordRequest(
    @SerializedName("saleDate") val saleDate: String,
    @SerializedName("kg") val kg: Double,
    @SerializedName("unitPrice") val unitPrice: Double
)

/**
 * Model สำหรับ Grist API Response (Records format)
 */
data class GristRecordsResponse(
    val records: List<GristRecord>
)

data class GristRecord(
    val id: Int,
    val fields: SalesRecord
)

/**
 * Model สำหรับ Grist API Request (Records format)
 */
data class GristRecordsRequest(
    val records: List<GristRecordRequest>
)

data class GristRecordRequest(
    val fields: SalesRecordRequest
)

/**
 * Model สำหรับสถิติยอดขาย (สำหรับหน้าจอหลัก)
 */
//data class SalesStatistics(
//    val totalAmount: Double = 0.0, // ยอดขายรวม
//    val totalKg: Double = 0.0, // น้ำหนักรวม
//    val averageUnitPrice: Double = 0.0, // ราคาเฉลี่ย
//    val recordCount: Int = 0 // จำนวนรายการ
//) {
//    fun getFormattedTotalAmount(): String {
//        return String.format("%.2f บาท", totalAmount)
//    }
//
//    fun getFormattedTotalKg(): String {
//        return String.format("%.2f กก.", totalKg)
//    }
//
//    fun getFormattedAveragePrice(): String {
//        return String.format("%.2f บาท/กก.", averageUnitPrice)
//    }
//}

/**
 * API Response wrapper สำหรับ error handling
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Throwable) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}