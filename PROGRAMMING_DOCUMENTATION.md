# TimSales6 - Agricultural Sales Management System
## Programming Documentation

### Project Overview

**TimSales6** เป็นแอปพลิเคชัน Android ที่พัฒนาด้วย Kotlin สำหรับการจัดการข้อมูลยอดขายสินค้าเกษตร โดยใช้ Grist Database เป็นระบบจัดเก็บข้อมูลและ REST API สำหรับการเชื่อมต่อ

### Technical Specifications

- **Language**: Kotlin
- **Platform**: Android API Level 24+ (Android 7.0+)
- **Architecture**: MVVM with Repository Pattern
- **Database**: Grist (Cloud-based Spreadsheet Database)
- **Network**: Retrofit 2 + OkHttp
- **UI Framework**: Traditional Android Views (not Compose)
- **Build System**: Gradle with Kotlin DSL

---

## Project Structure

```
app/
├── src/main/
│   ├── java/com/example/timsales/
│   │   ├── MainActivity.kt                      # Dashboard หน้าแรก
│   │   ├── AddSalesActivity.kt                  # หน้าเพิ่มข้อมูลขาย
│   │   ├── SalesListActivity.kt                 # หน้ารายการยอดขาย
│   │   ├── EditSalesActivity.kt                 # หน้าแก้ไขข้อมูล
│   │   ├── TimSalesApplication.kt               # Application Class
│   │   ├── adapter/
│   │   │   └── SalesRecordAdapter.kt            # RecyclerView Adapter
│   │   ├── config/
│   │   │   └── AppConfig.kt                     # Configuration Constants
│   │   ├── data/
│   │   │   ├── api/
│   │   │   │   └── GristApiService.kt           # Retrofit API Interface
│   │   │   ├── model/
│   │   │   │   ├── SalesRecord.kt               # Data Models
│   │   │   │   └── SalesStatistics.kt           # Statistics Model
│   │   │   └── repository/
│   │   │       └── SalesRepository.kt           # Data Repository
│   │   ├── debug/
│   │   │   └── ConfigurationChecker.kt          # Debug Utilities
│   │   └── utils/
│   │       └── DebugUtils.kt                    # Logging Utilities
│   └── res/
│       ├── layout/                              # XML Layout Files
│       ├── values/                              # Colors, Strings, Themes
│       └── drawable/                            # Icons and Graphics
└── build.gradle.kts                            # App Build Configuration
```

---

## Core Components

### 1. Data Models

#### SalesRecord.kt
```kotlin
data class SalesRecord(
    val id: Int = 0,                            // Record ID from Grist
    val saleDate: String,                       // Sale date (YYYY-MM-DD)
    val kg: Double,                             // Weight in kilograms
    val unitPrice: Double,                      // Price per kg
    val amount: Double = 0.0,                   // Total amount (calculated)
    val createdAt: String? = null,              // Creation timestamp
    val updatedAt: String? = null               // Update timestamp
)
```

**Key Features:**
- Flexible timestamp parsing (Unix timestamp และ ISO format)
- Custom TypeAdapter สำหรับ Gson serialization
- Format methods สำหรับการแสดงผล UI

#### SalesStatistics.kt
```kotlin
data class SalesStatistics(
    val totalAmount: Double = 0.0,              // ยอดขายรวม
    val totalKg: Double = 0.0,                  // น้ำหนักรวม
    val averageUnitPrice: Double = 0.0,         // ราคาเฉลี่ย
    val recordCount: Int = 0                    // จำนวนรายการ
)
```

### 2. API Layer

#### GristApiService.kt
Retrofit interface สำหรับเชื่อมต่อ Grist API:

```kotlin
interface GristApiService {
    @GET("docs/{docId}/tables/{tableId}/records")
    suspend fun getAllSalesRecords(...)
    
    @POST("docs/{docId}/tables/{tableId}/records")
    suspend fun createSalesRecord(...)
    
    @PATCH("docs/{docId}/tables/{tableId}/records")
    suspend fun updateSalesRecord(...)
    
    @POST("docs/{docId}/tables/{tableId}/data/delete")
    suspend fun deleteSalesRecord(...)
}
```

**API Endpoints:**
- **GET** `/docs/{docId}/tables/{tableId}/records` - ดึงข้อมูลทั้งหมด
- **POST** `/docs/{docId}/tables/{tableId}/records` - เพิ่มข้อมูลใหม่
- **PATCH** `/docs/{docId}/tables/{tableId}/records` - แก้ไขข้อมูล
- **POST** `/docs/{docId}/tables/{tableId}/data/delete` - ลบข้อมูล

### 3. Repository Pattern

#### SalesRepository.kt
จัดการการเรียก API และ data transformation:

```kotlin
class SalesRepository {
    private val apiService: GristApiService
    
    suspend fun getAllSalesRecords(): ApiResult<List<SalesRecord>>
    suspend fun createSalesRecord(record: SalesRecordRequest): ApiResult<Unit>
    suspend fun updateSalesRecord(id: Int, record: SalesRecordRequest): ApiResult<Unit>
    suspend fun deleteSalesRecord(id: Int): ApiResult<Unit>
    suspend fun getSalesStatistics(): ApiResult<SalesStatistics>
}
```

**Key Features:**
- Coroutines support สำหรับ asynchronous operations
- Custom Gson configuration สำหรับ timestamp parsing
- Comprehensive error handling และ logging
- Year 2025 filtering สำหรับสถิติ

### 4. UI Components

#### MainActivity.kt - Dashboard
- แสดงสถิติยอดขายประจำปี 2025
- Navigation ไปยังหน้าต่างๆ
- Real-time data loading พร้อม loading indicators

#### AddSalesActivity.kt - Add New Sale
- Form validation
- Date picker integration  
- Real-time amount calculation (kg × unitPrice)
- Error handling และ user feedback

#### SalesListActivity.kt - Sales List
- RecyclerView พร้อม custom adapter
- Pull-to-refresh functionality
- Delete confirmation dialog
- Edit navigation

#### EditSalesActivity.kt - Edit Sale
- Pre-populate form with existing data
- Update validation
- Optimistic UI updates

---

## Configuration Management

### AppConfig.kt
```kotlin
object AppConfig {
    const val GRIST_BASE_URL = "https://docs.getgrist.com/api/"
    const val GRIST_DOC_ID = "your-document-id"
    const val GRIST_TABLE_ID = "TimSales"
    const val GRIST_API_KEY = "your-api-key"
    
    // Network timeouts
    const val NETWORK_TIMEOUT_SECONDS = 30L
    const val NETWORK_READ_TIMEOUT_SECONDS = 30L
    const val NETWORK_WRITE_TIMEOUT_SECONDS = 30L
}
```

---

## Database Schema (Grist)

### TimSales Table Structure
```
Column Name    | Type      | Description              | Required
---------------|-----------|--------------------------|----------
id             | Integer   | Auto-increment primary   | Yes
saleDate       | Date      | วันที่ขาย                | Yes  
kg             | Numeric   | น้ำหนัก (กิโลกรัม)        | Yes
unitPrice      | Numeric   | ราคาต่อหน่วย (บาท/กก.)    | Yes
amount         | Formula   | ยอดรวม ($kg * $unitPrice)| Auto
created_at     | DateTime  | วันที่สร้างระเบียน        | Auto
updated_at     | DateTime  | วันที่แก้ไขล่าสุด         | Auto
```

---

## Error Handling Strategy

### 1. Network Errors
```kotlin
catch (e: UnknownHostException) {
    ApiResult.Error(Exception("Unable to connect to server"))
}
catch (e: ConnectException) {
    ApiResult.Error(Exception("Connection timeout"))
}
```

### 2. API Response Errors
```kotlin
if (response.isSuccessful) {
    // Success handling
} else {
    val errorBody = response.errorBody()?.string()
    ApiResult.Error(Exception("API Error: ${response.code()} - $errorBody"))
}
```

### 3. Data Validation
```kotlin
fun validateInput(): ValidationResult {
    when {
        kg <= 0 -> ValidationResult.Error("น้ำหนักต้องมากกว่า 0")
        unitPrice <= 0 -> ValidationResult.Error("ราคาต้องมากกว่า 0")
        saleDate.isBlank() -> ValidationResult.Error("กรุณาเลือกวันที่")
        else -> ValidationResult.Success
    }
}
```

---

## Debugging and Logging

### DebugUtils.kt
Comprehensive logging system:

```kotlin
object DebugUtils {
    fun log(message: String) { /* Standard logging */ }
    fun logApi(message: String) { /* API-specific logging */ }
    fun logError(message: String, throwable: Throwable? = null) { /* Error logging */ }
    fun logLifecycle(activity: String, event: String) { /* Activity lifecycle */ }
    fun logException(tag: String, message: String, exception: Exception) { /* Exception logging */ }
}
```

### Debug Features
- Request/Response logging
- Activity lifecycle tracking
- Exception stack traces
- Performance monitoring
- API call timing

---

## Security Considerations

### 1. API Key Management
```kotlin
// Store in AppConfig.kt (should be moved to BuildConfig in production)
const val GRIST_API_KEY = "your-secure-api-key"

// Usage with Authorization header
@Header("Authorization") authorization: String = "Bearer $API_KEY"
```

### 2. Input Validation
- Server-side validation สำหรับทุก API calls
- Client-side validation สำหรับ UX
- Sanitization สำหรับ text inputs

### 3. Network Security
- HTTPS only connections
- Certificate pinning (recommended for production)
- Request/Response encryption

---

## Performance Optimization

### 1. Network Optimization
```kotlin
// OkHttp configuration
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(loggingInterceptor)
    .build()
```

### 2. Memory Management
- Lazy initialization สำหรับ heavy objects
- Proper lifecycle management
- Image loading optimization
- RecyclerView ViewHolder pattern

### 3. Data Processing
- Background threads สำหรับ heavy operations
- Caching strategies
- Pagination (สำหรับ large datasets)

---

## Testing Strategy

### 1. Unit Tests
```kotlin
@Test
fun `test sales calculation`() {
    val record = SalesRecord(kg = 10.0, unitPrice = 50.0)
    assertEquals(500.0, record.kg * record.unitPrice, 0.01)
}
```

### 2. Integration Tests
- API endpoint testing
- Database operations
- Error scenarios

### 3. UI Tests (Recommended)
- Espresso tests สำหรับ critical user flows
- Screenshot testing
- Accessibility testing

---

## Deployment Checklist

### 1. Pre-Production
- [ ] Update API keys to production values
- [ ] Remove debug logging
- [ ] Enable ProGuard/R8 obfuscation
- [ ] Test on multiple devices
- [ ] Performance profiling

### 2. Production Configuration
```kotlin
// build.gradle.kts
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}
```

### 3. Post-Deployment
- [ ] Monitor crash reports
- [ ] API performance monitoring
- [ ] User feedback collection
- [ ] Analytics integration

---

## Troubleshooting Guide

### Common Issues

#### 1. API Connection Issues
```
Error: Unable to resolve host "docs.getgrist.com"
Solution: Check internet connection, DNS settings, or firewall
```

#### 2. Data Parsing Errors
```
Error: NumberFormatException for timestamp
Solution: Verify timestamp format, check FlexibleTimestampAdapter
```

#### 3. Record ID Issues
```
Error: Record ID = 0 for all records
Solution: Verify ID mapping in getAllSalesRecords()
```

#### 4. Delete Operation Fails
```
Error: 404 on delete endpoint
Solution: Use POST /data/delete with array of IDs
```

---

## Future Enhancements

### 1. Planned Features
- [ ] Data export (CSV, PDF)
- [ ] Advanced filtering และ search
- [ ] Offline mode พร้อม sync
- [ ] Charts และ analytics
- [ ] Multi-user support
- [ ] Push notifications

### 2. Technical Improvements
- [ ] Migration to Jetpack Compose
- [ ] Room database สำหรับ offline cache
- [ ] WorkManager สำหรับ background sync
- [ ] Dependency injection (Hilt/Koin)
- [ ] Clean Architecture implementation

### 3. Performance Enhancements
- [ ] Image caching และ optimization
- [ ] Network request batching
- [ ] Database query optimization
- [ ] Memory leak prevention

---

## API Integration Guide

### Setup Steps

1. **Create Grist Document**
   ```
   1. Go to docs.getgrist.com
   2. Create new document
   3. Create table "TimSales" with required columns
   4. Note document ID from URL
   ```

2. **Generate API Key**
   ```
   1. Go to Profile Settings
   2. Navigate to API Keys section
   3. Generate new API key
   4. Copy key for AppConfig.kt
   ```

3. **Configure Application**
   ```kotlin
   // Update AppConfig.kt
   const val GRIST_DOC_ID = "your-doc-id-here"
   const val GRIST_API_KEY = "your-api-key-here"
   const val GRIST_TABLE_ID = "TimSales"
   ```

### API Testing
```bash
# Test connection with curl
curl -H "Authorization: Bearer YOUR_API_KEY" \
     https://docs.getgrist.com/api/docs/YOUR_DOC_ID/tables/TimSales/records
```

---

## Conclusion

TimSales6 เป็นแอปพลิเคชันที่ออกแบบมาเพื่อความเรียบง่ายและประสิทธิภาพ โดยใช้ modern Android development practices และ robust error handling เพื่อให้ผู้ใช้สามารถจัดการข้อมูลยอดขายได้อย่างมีประสิทธิภาพ

การใช้ Grist เป็น backend ทำให้สามารถจัดการข้อมูลได้ง่าย พร้อมกับความยืดหยุ่นในการขยายตัวและการปรับแต่งในอนาคต

---

**Document Version**: 1.0  
**Last Updated**: September 16, 2025  
**Author**: Kotlin Expert Assistant  
**Project**: TimSales6 Agricultural Sales Management System