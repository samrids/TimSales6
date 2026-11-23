package com.example.timsales.data.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model สำหรับข้อมูลปีขาย
 * ตาม Schema ใน Grist: saleYear, created_at, updated_at
 */
data class TimYear(
    val id: Int = 0,
    @SerializedName("saleYear") val saleYear: Int, // ปีเริ่มต้นของรอบ (Integer เช่น 2026 = รอบ 2026/27)
    @SerializedName("startDate") val startDate: String? = null, // วันเริ่มรอบการขาย (YYYY-MM-DD)
    @SerializedName("endDate") val endDate: String? = null, // วันสิ้นสุดรอบการขาย (YYYY-MM-DD)
    @SerializedName("seasonCode") val seasonCode: String? = null, // รหัสรอบ (formula จาก Grist เช่น "2026/27")
    @SerializedName("created_at") val createdAt: String? = null, // DateTime string from server
    @SerializedName("updated_at") val updatedAt: String? = null // DateTime string from server
) {
    /**
     * รหัสรอบการขาย เช่น "2026/27"
     * ใช้ค่าจาก Grist ถ้ามี ไม่งั้น derive จาก saleYear
     */
    fun resolveSeasonCode(): String {
        if (!seasonCode.isNullOrEmpty()) return seasonCode
        val nextTwo = ((saleYear + 1) % 100)
        return String.format("%d/%02d", saleYear, nextTwo)
    }

    /**
     * Label สำหรับแสดงผล เช่น "รอบ 2026/27"
     */
    fun getSeasonLabel(): String {
        return "รอบ ${resolveSeasonCode()}"
    }

    /**
     * วันเริ่มรอบที่ใช้งานจริง (YYYY-MM-DD)
     * ใช้ค่าจาก Grist ถ้ามี ไม่งั้น default = 1 ก.พ. ของปีเริ่มรอบ
     */
    fun getEffectiveStartDate(): String {
        return normalizeDate(startDate) ?: String.format("%04d-02-01", saleYear)
    }

    /**
     * วันสิ้นสุดรอบที่ใช้งานจริง (YYYY-MM-DD)
     * ใช้ค่าจาก Grist ถ้ามี ไม่งั้น default = 31 มี.ค. ของปีถัดไป
     */
    fun getEffectiveEndDate(): String {
        return normalizeDate(endDate) ?: String.format("%04d-03-31", saleYear + 1)
    }

    /**
     * ตรวจว่าวันที่ขายอยู่ในรอบนี้หรือไม่ (ใช้เป็น fallback เมื่อ Grist ยังไม่ได้ตั้ง seasonCode)
     */
    fun containsDate(rawSaleDate: String): Boolean {
        val d = normalizeDate(rawSaleDate) ?: return false
        return d >= getEffectiveStartDate() && d <= getEffectiveEndDate()
    }

    private fun normalizeDate(raw: String?): String? {
        if (raw.isNullOrEmpty()) return null
        return try {
            val datePart = raw.split("T")[0].split(" ")[0]
            if (datePart.length >= 10 && datePart[4] == '-') datePart.substring(0, 10) else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Format ปีสำหรับแสดงผล (เก็บไว้เพื่อ backward compatibility)
     */
    fun getFormattedYear(): String {
        return getSeasonLabel()
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
    
    private fun formatDateString(dateStr: String): String {
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                "${parts[2]}/${parts[1]}/${parts[0]}" // แปลงจาก YYYY-MM-DD เป็น DD/MM/YYYY
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }
}

/**
 * Model สำหรับ API Request เมื่อสร้างหรือแก้ไขข้อมูลปี
 */
data class TimYearRequest(
    @SerializedName("saleYear") val saleYear: Int
)

/**
 * Model สำหรับ Grist API Response (TimYear Records format)
 */
data class GristTimYearRecordsResponse(
    val records: List<GristTimYearRecord>
)

data class GristTimYearRecord(
    val id: Int,
    val fields: TimYear? // nullable because API may return different shape
)

/**
 * Model สำหรับ Grist API Request (TimYear Records format)
 */
data class GristTimYearRecordsRequest(
    val records: List<GristTimYearRecordRequest>
)

data class GristTimYearRecordRequest(
    val fields: TimYearRequest
)

/**
 * Model สำหรับ Update Request
 */
data class GristTimYearUpdateRecordsRequest(
    val records: List<GristTimYearUpdateRecord>
)

data class GristTimYearUpdateRecord(
    val id: Int,
    val fields: TimYearRequest
)