package com.example.timsales.data.repository

import com.example.timsales.config.AppConfig
import com.example.timsales.data.api.*
import com.example.timsales.data.model.*
import com.example.timsales.utils.DebugUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.google.gson.GsonBuilder

/**
 * Repository class สำหรับจัดการข้อมูลยอดขาย
 * ทำหน้าที่เป็นตัวกลางระหว่าง API และ UI
 */
class SalesRepository {

    private val apiService: GristApiService by lazy {
        try {
            createApiService()
        } catch (e: Exception) {
            DebugUtils.logException("SalesRepository", "Error creating API service", e)
            throw e
        }
    }

    private fun createApiService(): GristApiService {
        try {
            
            
            // Create logging interceptor for debugging
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Create OkHttp client with timeout and logging
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(AppConfig.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(AppConfig.NETWORK_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(AppConfig.NETWORK_WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
            
            
            
            // Create custom Gson with flexible timestamp handling
            val gson = GsonBuilder()
                .registerTypeAdapter(String::class.java, FlexibleTimestampAdapter())
                .create()
            
            val retrofit = Retrofit.Builder()
                .baseUrl(AppConfig.GRIST_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            
            
            
            return retrofit.create(GristApiService::class.java)
            
        } catch (e: Exception) {
            DebugUtils.logException("SalesRepository", "Error in createApiService", e)
            throw e
        }
    }

    private fun getAuthHeader(): String {
        val header = "Bearer ${AppConfig.GRIST_API_KEY}"
        DebugUtils.logApi("Auth header created (length: ${AppConfig.GRIST_API_KEY.length})")
        return header
    }

    /**
     * ดึงข้อมูลยอดขายทั้งหมด
     */
    suspend fun getAllSalesRecords(): ApiResult<List<SalesRecord>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getAllSalesRecords(
                    docId = AppConfig.GRIST_DOC_ID,
                    tableId = AppConfig.GRIST_TABLE_ID,
                    authorization = getAuthHeader()
                )
                
                
                if (response.isSuccessful) {
                    val gristResponse = response.body()
                    
                    val records = gristResponse?.records?.mapNotNull { gristRecord ->
                        try {
                            
                            // สร้าง SalesRecord พร้อมกับ ID ที่ถูกต้อง
                            val salesRecord = gristRecord.fields.copy(id = gristRecord.id)
                                                        
                            salesRecord
                        } catch (e: Exception) {                            
                            null // Skip this record if there's an error
                        }
                    } ?: emptyList()
                    
                    // records.forEach { record ->
                    //     DebugUtils.logApi("Final record: ID=${record.id}, Date=${record.saleDate}, Amount=${record.amount}")
                    // }
                    
                    ApiResult.Success(records)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val error = "API Error: ${response.code()} ${response.message()} - $errorBody"
                    ApiResult.Error(Exception(error))
                }
            } catch (e: java.net.UnknownHostException) {
                val error = "Network Error: Unable to connect to Grist server. Check internet connection and URL."
                ApiResult.Error(Exception(error))
            } catch (e: java.net.ConnectException) {
                val error = "Connection Error: Unable to connect to ${AppConfig.GRIST_BASE_URL}"
                ApiResult.Error(Exception(error))
            } catch (e: Exception) {
                ApiResult.Error(e)
            }
        }
    }
    
    /**
     * Alias สำหรับ getAllSalesRecords
     */
    suspend fun getAllSales(): ApiResult<List<SalesRecord>> {
        return getAllSalesRecords()
    }

    /**
     * เพิ่มข้อมูลยอดขายใหม่
     */
    suspend fun createSalesRecord(salesRecord: SalesRecordRequest): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = GristRecordsRequest(
                    records = listOf(
                        GristRecordRequest(fields = salesRecord)
                    )
                )
                
                val response = apiService.createSalesRecord(
                    docId = AppConfig.GRIST_DOC_ID,
                    tableId = AppConfig.GRIST_TABLE_ID,
                    authorization = getAuthHeader(),
                    request = request
                )
                
                if (response.isSuccessful) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(Exception("API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                ApiResult.Error(e)
            }
        }
    }

    /**
     * แก้ไขข้อมูลยอดขาย
     */
    suspend fun updateSalesRecord(id: Int, salesRecord: SalesRecordRequest): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = GristUpdateRecordsRequest(
                    records = listOf(
                        GristUpdateRecord(id = id, fields = salesRecord)
                    )
                )
                
                val response = apiService.updateSalesRecord(
                    docId = AppConfig.GRIST_DOC_ID,
                    tableId = AppConfig.GRIST_TABLE_ID,
                    authorization = getAuthHeader(),
                    request = request
                )
                
                if (response.isSuccessful) {
                    ApiResult.Success(Unit)
                } else {
                    ApiResult.Error(Exception("API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                ApiResult.Error(e)
            }
        }
    }

    /**
     * ลบข้อมูลยอดขาย
     */
    suspend fun deleteSalesRecord(id: Int): ApiResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                
                // ส่ง array ของ IDs ที่ต้องการลบ
                val idsToDelete = listOf(id)
                
                val response = apiService.deleteSalesRecord(
                    docId = AppConfig.GRIST_DOC_ID,
                    tableId = AppConfig.GRIST_TABLE_ID,
                    authorization = getAuthHeader(),
                    request = idsToDelete
                )
                               
                if (response.isSuccessful) {
                    ApiResult.Success(Unit)
                } else {
                    val errorBody = response.errorBody()?.string()
                    
                    val error = "Delete failed: ${response.code()} ${response.message()}"
                    ApiResult.Error(Exception(error))
                }
            } catch (e: java.net.UnknownHostException) {
                val error = "Network Error: Unable to connect to Grist server"
                ApiResult.Error(Exception(error))
            } catch (e: Exception) {
                ApiResult.Error(e)
            }
        }
    }

    /**
     * คำนวณ dashboard ของรอบการขายที่เลือก (สถิติรวม + ยอดรายเดือนตลอดรอบ)
     *
     * การจับคู่รายการขายเข้ากับรอบ:
     *  - ถ้า Grist ตั้งคอลัมน์ formula `seasonCode` ไว้ → จับคู่ด้วย seasonCode (แม่นยำที่สุด)
     *  - ถ้ายังไม่ได้ตั้ง → fallback ไปจับคู่ด้วยช่วงวันที่ของรอบ (startDate..endDate)
     */
    suspend fun getSeasonDashboard(season: TimYear): ApiResult<SeasonDashboard> {
        return when (val result = getAllSalesRecords()) {
            is ApiResult.Success -> {
                val seasonSales = filterBySeason(result.data, season)
                val statistics = calculateStatistics(seasonSales)
                val monthly = buildMonthlyPoints(season, seasonSales)
                ApiResult.Success(
                    SeasonDashboard(
                        seasonCode = season.resolveSeasonCode(),
                        statistics = statistics,
                        monthly = monthly
                    )
                )
            }
            is ApiResult.Error -> ApiResult.Error(result.exception)
            else -> ApiResult.Error(Exception("Unknown error"))
        }
    }

    /**
     * ดึงเฉพาะรายการขายที่อยู่ในรอบที่เลือก (สำหรับหน้ารายการยอดขาย)
     */
    suspend fun getSalesRecordsForSeason(season: TimYear): ApiResult<List<SalesRecord>> {
        return when (val result = getAllSalesRecords()) {
            is ApiResult.Success -> ApiResult.Success(filterBySeason(result.data, season))
            is ApiResult.Error -> ApiResult.Error(result.exception)
            else -> ApiResult.Error(Exception("Unknown error"))
        }
    }

    /**
     * (เก็บไว้เพื่อ backward compatibility) คำนวณเฉพาะสถิติรวมของรอบ
     */
    suspend fun getSalesStatistics(season: TimYear): ApiResult<SalesStatistics> {
        return when (val dashboard = getSeasonDashboard(season)) {
            is ApiResult.Success -> ApiResult.Success(dashboard.data.statistics)
            is ApiResult.Error -> ApiResult.Error(dashboard.exception)
            else -> ApiResult.Error(Exception("Unknown error"))
        }
    }

    /**
     * กรองเฉพาะรายการขายที่อยู่ในรอบที่ระบุ
     */
    private fun filterBySeason(records: List<SalesRecord>, season: TimYear): List<SalesRecord> {
        val code = season.resolveSeasonCode()
        return records.filter { record ->
            try {
                val recordCode = record.seasonCode
                if (!recordCode.isNullOrEmpty()) {
                    recordCode == code
                } else {
                    // Fallback: ใช้ช่วงวันที่ของรอบ
                    season.containsDate(record.saleDate)
                }
            } catch (e: Exception) {
                DebugUtils.logException("SalesRepository", "Error matching season for ${record.saleDate}", e)
                false
            }
        }
    }

    /**
     * สร้างจุดข้อมูลรายเดือนต่อเนื่องตลอดรอบ (เดือนที่ไม่มียอดจะเป็น 0)
     */
    private fun buildMonthlyPoints(season: TimYear, records: List<SalesRecord>): List<MonthlyPoint> {
        val months = enumerateMonths(season.getEffectiveStartDate(), season.getEffectiveEndDate())
        val byMonth = records.groupBy { it.getYearMonth() }
        return months.map { ym ->
            val recs = byMonth[ym] ?: emptyList()
            MonthlyPoint(
                yearMonth = ym,
                label = monthLabel(ym),
                revenue = recs.sumOf { it.amount },
                kg = recs.sumOf { it.kg },
                count = recs.size
            )
        }
    }

    /**
     * ไล่รายชื่อเดือนในรูป YYYY-MM ตั้งแต่เดือนเริ่มถึงเดือนสิ้นสุด (inclusive)
     */
    private fun enumerateMonths(start: String, end: String): List<String> {
        return try {
            var y = start.substring(0, 4).toInt()
            var m = start.substring(5, 7).toInt()
            val endY = end.substring(0, 4).toInt()
            val endM = end.substring(5, 7).toInt()
            val out = mutableListOf<String>()
            var guard = 0
            while ((y < endY || (y == endY && m <= endM)) && guard < 60) {
                out.add(String.format("%04d-%02d", y, m))
                m++
                if (m > 12) { m = 1; y++ }
                guard++
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * แปลง YYYY-MM เป็นป้ายแกน X แบบเดือนอังกฤษ + ปี ค.ศ. เช่น "2026-02" -> "Feb 26"
     * (ใช้ตัวอักษรอังกฤษเพื่อไม่ให้สระภาษาไทยโดนตัดบนแกนกราฟ)
     */
    private fun monthLabel(yearMonth: String): String {
        return try {
            val yy = yearMonth.substring(2, 4) // 2 หลักท้ายของปี ค.ศ.
            val m = yearMonth.substring(5, 7).toInt()
            val names = arrayOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            if (m in 1..12) "${names[m - 1]} $yy" else yearMonth
        } catch (e: Exception) {
            yearMonth
        }
    }

    /**
     * คำนวณสถิติจากรายการยอดขาย
     */
    private fun calculateStatistics(records: List<SalesRecord>): SalesStatistics {
        if (records.isEmpty()) {
            return SalesStatistics()
        }

        val totalAmount = records.sumOf { it.amount }
        val totalKg = records.sumOf { it.kg }
        // ราคาเฉลี่ยต่อกก. = ยอดขายรวม / น้ำหนักรวม (weighted average)
        val averageUnitPrice = if (totalKg > 0) totalAmount / totalKg else 0.0
        
        
        return SalesStatistics(
            totalAmount = totalAmount,
            totalKg = totalKg,
            averageUnitPrice = averageUnitPrice,
            recordCount = records.size
        )
    }
}