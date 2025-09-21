package com.example.timsales.data.api

import com.example.timsales.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Grist API Interface สำหรับจัดการข้อมูลยอดขาย
 * อ้างอิงจาก Grist API Documentation: https://raw.githubusercontent.com/gristlabs/grist-help/master/api/grist.yml
 */
interface GristApiService {
    
    /**
     * ดึงข้อมูลยอดขายทั้งหมด
     * GET /api/docs/{docId}/tables/{tableId}/records
     */
    @GET("docs/{docId}/tables/{tableId}/records")
    suspend fun getAllSalesRecords(
        @Path("docId") docId: String,
        @Path("tableId") tableId: String,
        @Header("Authorization") authorization: String
    ): Response<GristRecordsResponse>

    /**
     * เพิ่มข้อมูลยอดขายใหม่
     * POST /docs/{docId}/tables/{tableId}/records
     */
    @POST("docs/{docId}/tables/{tableId}/records")
    @Headers("Content-Type: application/json")
    suspend fun createSalesRecord(
        @Path("docId") docId: String,
        @Path("tableId") tableId: String,
        @Header("Authorization") authorization: String,
        @Body request: GristRecordsRequest
    ): Response<GristRecordsResponse>

    /**
     * แก้ไขข้อมูลยอดขาย
     * PATCH /docs/{docId}/tables/{tableId}/records
     */
    @PATCH("docs/{docId}/tables/{tableId}/records")
    @Headers("Content-Type: application/json")
    suspend fun updateSalesRecord(
        @Path("docId") docId: String,
        @Path("tableId") tableId: String,
        @Header("Authorization") authorization: String,
        @Body request: GristUpdateRecordsRequest
    ): Response<Void>

    /**
     * ลบข้อมูลยอดขาย
     * POST /docs/{docId}/tables/{tableId}/data/delete
     * ส่ง array ของ record IDs ในรูปแบบ JSON array [1, 2, 3]
     */
    @POST("docs/{docId}/tables/{tableId}/data/delete")
    @Headers("Content-Type: application/json")
    suspend fun deleteSalesRecord(
        @Path("docId") docId: String,
        @Path("tableId") tableId: String,
        @Header("Authorization") authorization: String,
        @Body request: List<Int> // ส่ง array ของ IDs: [1, 2, 3]
    ): Response<Void>
    
    // ==================== TimYear API Endpoints ====================
    
    /**
     * ดึงข้อมูลปีขายทั้งหมด
     * GET /api/docs/{docId}/tables/TimYear/records
     */
    @GET("docs/{docId}/tables/{tableId}/records")
    suspend fun getAllTimYears(
        @Path("docId") docId: String,
        @Path("tableId") tableId: String,
        @Header("Authorization") authorization: String
    ): Response<GristTimYearRecordsResponse>

    /**
     * เพิ่มปีขายใหม่
     * POST /docs/{docId}/tables/TimYear/records
     */
    @POST("docs/{docId}/tables/{tableId}/records")
    @Headers("Content-Type: application/json")
    suspend fun createTimYear(
        @Path("docId") docId: String,
        @Path("tableId") tableId: String,
        @Header("Authorization") authorization: String,
        @Body request: GristTimYearRecordsRequest
    ): Response<GristTimYearRecordsResponse>

    /**
     * แก้ไขข้อมูลปีขาย
     * PATCH /docs/{docId}/tables/TimYear/records
     */
    @PATCH("docs/{docId}/tables/{tableId}/records")
    @Headers("Content-Type: application/json")
    suspend fun updateTimYear(
        @Path("docId") docId: String,
        @Path("tableId") tableId: String,
        @Header("Authorization") authorization: String,
        @Body request: GristTimYearUpdateRecordsRequest
    ): Response<Void>

    /**
     * ลบข้อมูลปีขาย
     * POST /docs/{docId}/tables/TimYear/data/delete
     */
    @POST("docs/{docId}/tables/{tableId}/data/delete")
    @Headers("Content-Type: application/json")
    suspend fun deleteTimYear(
        @Path("docId") docId: String,
        @Path("tableId") tableId: String,
        @Header("Authorization") authorization: String,
        @Body request: List<Int> // ส่ง array ของ IDs: [1, 2, 3]
    ): Response<Void>
}

/**
 * Model สำหรับ Update Request
 */
data class GristUpdateRecordsRequest(
    val records: List<GristUpdateRecord>
)

data class GristUpdateRecord(
    val id: Int,
    val fields: SalesRecordRequest
)