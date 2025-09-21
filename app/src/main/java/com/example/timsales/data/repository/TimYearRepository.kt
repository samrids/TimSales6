package com.example.timsales.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.timsales.config.AppConfig
import com.example.timsales.data.api.GristApiService
import com.example.timsales.data.model.FlexibleTimestampAdapter
import com.example.timsales.data.model.*
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import java.util.concurrent.TimeUnit

class TimYearRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "TimYearRepository"
    }
    
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("app_config", Context.MODE_PRIVATE)
    }
    
    private val apiService: GristApiService by lazy {
        try {
            Log.d(TAG, "Creating API service...")
            createApiService()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating API service", e)
            throw e
        }
    }
    
    private fun createApiService(): GristApiService {
        try {
            Log.d(TAG, "Setting up HTTP client...")
            
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
            
            Log.d(TAG, "HTTP client configured")
            
            // Create custom Gson with flexible timestamp handling
            val gson = GsonBuilder()
                .registerTypeAdapter(String::class.java, FlexibleTimestampAdapter())
                .create()
            
            val retrofit = Retrofit.Builder()
                .baseUrl(AppConfig.GRIST_BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
            
            Log.d(TAG, "Retrofit configured with base URL: ${AppConfig.GRIST_BASE_URL}")
            
            return retrofit.create(GristApiService::class.java)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in createApiService", e)
            throw e
        }
    }
    
    /**
     * ตรวจสอบการตั้งค่า API
     */
    private fun validateConfig(): Triple<String, String, String>? {
        // Prefer values from AppConfig (static config) but allow overrides from SharedPreferences
        val apiKeyFromPrefs = preferences.getString("api_key", null)
        val docIdFromPrefs = preferences.getString("doc_id", null)

        val apiKey = when {
            !AppConfig.GRIST_API_KEY.isNullOrEmpty() -> AppConfig.GRIST_API_KEY
            !apiKeyFromPrefs.isNullOrEmpty() -> apiKeyFromPrefs
            else -> null
        }

        val docId = when {
            !AppConfig.GRIST_DOC_ID.isNullOrEmpty() -> AppConfig.GRIST_DOC_ID
            !docIdFromPrefs.isNullOrEmpty() -> docIdFromPrefs
            else -> null
        }

        if (apiKey.isNullOrEmpty() || docId.isNullOrEmpty()) {
            Log.w(TAG, "API configuration not complete - API Key or Doc ID missing. AppConfig API Key length=${AppConfig.GRIST_API_KEY.length}, DocId='${AppConfig.GRIST_DOC_ID}'")
            return null
        }

        val authHeader = "Bearer $apiKey"
        Log.d(TAG, "Using Doc ID: $docId, Auth header length: ${apiKey.length}")
        return Triple(apiKey, docId, authHeader)
    }
    
    /**
     * ดึงข้อมูลปีขายทั้งหมด
     */
    suspend fun getAllTimYears(): Result<List<TimYear>> {
        return try {
            val config = validateConfig() ?: return Result.failure(Exception("API configuration not set"))
            val (_, docId, authHeader) = config
            
            Log.d(TAG, "Fetching all TimYear records")
            
            val response = apiService.getAllTimYears(
                docId = docId,
                tableId = AppConfig.TIMYEAR_TABLE_ID,
                authorization = authHeader
            )
            
            Log.d(TAG, "TimYear GET response code: ${response.code()}, message: ${response.message()}")
            val respBody = try { response.body() } catch (e: Exception) { null }
            Log.d(TAG, "TimYear GET response raw body: $respBody")

            if (response.isSuccessful) {
                val timYears = response.body()?.records?.mapNotNull { record ->
                    try {
                        val fields = record.fields
                        if (fields == null) {
                            Log.w(TAG, "Skipping record with null fields (id=${record.id})")
                            null
                        } else {
                            fields.copy(id = record.id)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error mapping record id=${record.id}", e)
                        null
                    }
                } ?: emptyList()
                
                Log.d(TAG, "Successfully fetched ${timYears.size} TimYear records")
                
                // เรียงตาม saleYear จากใหม่ไปเก่า
                val sortedTimYears = timYears.sortedByDescending { it.saleYear }
                
                Result.success(sortedTimYears)
            } else {
                val errorBody = try { response.errorBody()?.string() } catch (e: Exception) { "<error reading body>" }
                val errorMsg = "Failed to fetch TimYear records: ${response.code()} - ${response.message()} - Body: $errorBody"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching TimYear records", e)
            Result.failure(e)
        }
    }
    
    /**
     * ดึงปีล่าสุดที่มีในระบบ
     */
    suspend fun getLatestYear(): Result<Int?> {
        return try {
            val result = getAllTimYears()
            if (result.isSuccess) {
                val timYears = result.getOrNull() ?: emptyList()
                val latestYear = timYears.maxByOrNull { it.saleYear }?.saleYear
                Log.d(TAG, "Latest year found: $latestYear")
                Result.success(latestYear)
            } else {
                result.exceptionOrNull()?.let { exception ->
                    Log.e(TAG, "Error getting latest year", exception)
                    Result.failure(exception)
                } ?: Result.failure(Exception("Unknown error getting latest year"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting latest year", e)
            Result.failure(e)
        }
    }
    
    /**
     * เพิ่มปีขายใหม่
     */
    suspend fun createTimYear(saleYear: Int): Result<TimYear> {
        return try {
            val config = validateConfig() ?: return Result.failure(Exception("API configuration not set"))
            val (_, docId, authHeader) = config
            
            // ตรวจสอบว่าปีนี้มีอยู่แล้วหรือไม่
            val existingYears = getAllTimYears()
            if (existingYears.isSuccess) {
                val years = existingYears.getOrNull() ?: emptyList()
                if (years.any { it.saleYear == saleYear }) {
                    Log.w(TAG, "Year $saleYear already exists")
                    return Result.failure(Exception("ปี $saleYear มีอยู่ในระบบแล้ว"))
                }
            }
            
            Log.d(TAG, "Creating new TimYear with saleYear: $saleYear")
            
            val request = GristTimYearRecordsRequest(
                records = listOf(
                    GristTimYearRecordRequest(
                        fields = TimYearRequest(saleYear = saleYear)
                    )
                )
            )
            
            val response = apiService.createTimYear(
                docId = docId,
                tableId = AppConfig.TIMYEAR_TABLE_ID,
                authorization = authHeader,
                request = request
            )
            
            if (response.isSuccessful) {
                val createdRecord = response.body()?.records?.firstOrNull()
                if (createdRecord != null) {
                    val createdFields = createdRecord.fields
                    val timYear = if (createdFields != null) {
                        createdFields.copy(id = createdRecord.id)
                    } else {
                        // Fallback: construct from request data
                        TimYear(id = createdRecord.id, saleYear = saleYear)
                    }
                    Log.d(TAG, "Successfully created TimYear: $timYear")
                    Result.success(timYear)
                } else {
                    val errorMsg = "No record returned from create TimYear API"
                    Log.e(TAG, errorMsg)
                    Result.failure(Exception(errorMsg))
                }
            } else {
                val errorMsg = "Failed to create TimYear: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating TimYear", e)
            Result.failure(e)
        }
    }
    
    /**
     * แก้ไขปีขาย
     */
    suspend fun updateTimYear(timYear: TimYear): Result<Unit> {
        return try {
            val config = validateConfig() ?: return Result.failure(Exception("API configuration not set"))
            val (_, docId, authHeader) = config
            
            Log.d(TAG, "Updating TimYear with ID: ${timYear.id}")
            
            val request = GristTimYearUpdateRecordsRequest(
                records = listOf(
                    GristTimYearUpdateRecord(
                        id = timYear.id,
                        fields = TimYearRequest(saleYear = timYear.saleYear)
                    )
                )
            )
            
            val response = apiService.updateTimYear(
                docId = docId,
                tableId = AppConfig.TIMYEAR_TABLE_ID,
                authorization = authHeader,
                request = request
            )
            
            if (response.isSuccessful) {
                Log.d(TAG, "Successfully updated TimYear with ID: ${timYear.id}")
                Result.success(Unit)
            } else {
                val errorMsg = "Failed to update TimYear: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating TimYear", e)
            Result.failure(e)
        }
    }
    
    /**
     * ลบปีขาย
     */
    suspend fun deleteTimYear(id: Int): Result<Unit> {
        return try {
            val config = validateConfig() ?: return Result.failure(Exception("API configuration not set"))
            val (_, docId, authHeader) = config
            
            Log.d(TAG, "Deleting TimYear with ID: $id")
            
            val response = apiService.deleteTimYear(
                docId = docId,
                tableId = AppConfig.TIMYEAR_TABLE_ID,
                authorization = authHeader,
                request = listOf(id)
            )
            
            if (response.isSuccessful) {
                Log.d(TAG, "Successfully deleted TimYear with ID: $id")
                Result.success(Unit)
            } else {
                val errorMsg = "Failed to delete TimYear: ${response.code()} - ${response.message()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting TimYear", e)
            Result.failure(e)
        }
    }
    
    /**
     * ตรวจสอบว่ามีข้อมูลปีขายอยู่หรือไม่
     */
    suspend fun hasAnyTimYear(): Result<Boolean> {
        return try {
            val result = getAllTimYears()
            if (result.isSuccess) {
                val timYears = result.getOrNull() ?: emptyList()
                val hasData = timYears.isNotEmpty()
                Log.d(TAG, "Has TimYear data: $hasData")
                Result.success(hasData)
            } else {
                result.exceptionOrNull()?.let { exception ->
                    Log.e(TAG, "Error checking TimYear data", exception)
                    Result.failure(exception)
                } ?: Result.failure(Exception("Unknown error checking TimYear data"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking TimYear data", e)
            Result.failure(e)
        }
    }
    
    /**
     * เพิ่มปีปัจจุบันอัตโนมัติหากยังไม่มีข้อมูล
     */
    suspend fun initializeCurrentYearIfNeeded(): Result<Int> {
        return try {
            val hasData = hasAnyTimYear()
            if (hasData.isSuccess && hasData.getOrNull() == false) {
                // ไม่มีข้อมูลปีขาย สร้างปีปัจจุบัน
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                Log.d(TAG, "No TimYear data found. Creating current year: $currentYear")
                
                val createResult = createTimYear(currentYear)
                if (createResult.isSuccess) {
                    Log.d(TAG, "Successfully initialized current year: $currentYear")
                    Result.success(currentYear)
                } else {
                    createResult.exceptionOrNull()?.let { exception ->
                        Log.e(TAG, "Error initializing current year", exception)
                        Result.failure(exception)
                    } ?: Result.failure(Exception("Unknown error initializing current year"))
                }
            } else {
                // มีข้อมูลอยู่แล้ว ดึงปีล่าสุด
                val latestYearResult = getLatestYear()
                if (latestYearResult.isSuccess) {
                    val latestYear = latestYearResult.getOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
                    Log.d(TAG, "Using existing latest year: $latestYear")
                    Result.success(latestYear)
                } else {
                    latestYearResult.exceptionOrNull()?.let { exception ->
                        Log.e(TAG, "Error getting latest year", exception)
                        Result.failure(exception)
                    } ?: Result.failure(Exception("Unknown error getting latest year"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TimYear", e)
            Result.failure(e)
        }
    }
}