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
     * คำนวณสถิติยอดขายจากข้อมูลที่ได้
     */
    suspend fun getSalesStatistics(): ApiResult<SalesStatistics> {
        return when (val result = getAllSalesRecords()) {
            is ApiResult.Success -> {
                val records = result.data
                // กรองเฉพาะข้อมูลปี 2025
                val records2025 = filterSalesFor2025(records)
                val statistics = calculateStatistics(records2025)
                ApiResult.Success(statistics)
            }
            is ApiResult.Error -> {
                ApiResult.Error(result.exception)
            }
            else -> {
                ApiResult.Error(Exception("Unknown error"))
            }
        }
    }
    
    /**
     * กรองเฉพาะข้อมูลยอดขายในปี 2025
     */
    private fun filterSalesFor2025(records: List<SalesRecord>): List<SalesRecord> {
        return records.filter { salesRecord ->
            try {
                when {
                    salesRecord.saleDate.contains("2025") -> true
                    salesRecord.saleDate.startsWith("2025") -> true
                    salesRecord.saleDate.length >= 4 && salesRecord.saleDate.substring(0, 4) == "2025" -> true
                    else -> false
                }
            } catch (e: Exception) {
                DebugUtils.logException("SalesRepository", "Error parsing date ${salesRecord.saleDate}", e)
                false
            }
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
        val averageUnitPrice = if (records.isNotEmpty()) records.map { it.unitPrice }.average() else 0.0
        
        
        return SalesStatistics(
            totalAmount = totalAmount,
            totalKg = totalKg,
            averageUnitPrice = averageUnitPrice,
            recordCount = records.size
        )
    }
}