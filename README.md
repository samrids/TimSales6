# TimSales6 - Agricultural Sales Management System

![Android](https://img.shields.io/badge/Android-API%2024+-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.8+-blue.svg)
![Grist](https://img.shields.io/badge/Database-Grist-orange.svg)

## Overview

**TimSales6** เป็นแอปพลิเคชัน Android สำหรับการจัดการข้อมูลยอดขายสินค้าเกษตร พัฒนาด้วย Kotlin และใช้ Grist Database เป็นระบบจัดเก็บข้อมูล

## 🚀 Quick Setup

### 1. Clone Repository
```bash
git clone [repository-url]
cd TimSales6
```

### 2. Environment Configuration
1. คัดลอกไฟล์ environment template:
```bash
cp .env.example .env
```

2. แก้ไขค่าในไฟล์ `.env`:
```env
GRIST_BASE_URL=https://docs.getgrist.com/api/
GRIST_DOC_ID=your_document_id_here
GRIST_API_KEY=your_api_key_here
GRIST_TABLE_ID=TimSales
TIMYEAR_TABLE_ID=TimYear
```

### 3. วิธีการหา Configuration Values

#### 🔑 API Key:
- ไปที่ Grist → Profile Settings → API Keys
- สร้าง API Key ใหม่และคัดลอก

#### 📄 Document ID:
- เปิด Grist document ของคุณ
- ดู URL: `https://docs.getgrist.com/doc/YOUR_DOC_ID`
- คัดลอก `YOUR_DOC_ID`

### 4. Build และ Run
1. เปิดใน Android Studio
2. Build → Make Project  
3. Run บน device/emulator

## 🔒 Security & Environment

### Environment Variables
แอปนี้ใช้ไฟล์ `.env` เพื่อเก็บข้อมูลที่ละเอียดอ่อน:

- ✅ API Keys และ sensitive data จะไม่ถูก commit เข้า git
- ✅ ใช้ BuildConfig เพื่อเข้าถึงค่า configuration อย่างปลอดภัย
- ✅ ไฟล์ `.env` ถูก ignore ใน `.gitignore` แล้ว

### ไฟล์ที่สำคัญ:
- `.env` - ข้อมูลจริง (ไม่ commit)
- `.env.example` - Template สำหรับทีม (commit ได้)
- `AppConfig.kt` - อ่านค่าจาก BuildConfig

### ⚠️ คำเตือน:
- **อย่า commit** ไฟล์ `.env` เข้า git
- **อย่าแชร์** API Keys กับผู้อื่น
- ใช้ `.env.example` เป็น template เท่านั้น

## Features

- 📊 **Dashboard**: สรุปยอดขายประจำปี 2025
- ➕ **Add Sales**: เพิ่มข้อมูลยอดขายใหม่
- 📋 **Sales List**: ดูรายการยอดขายทั้งหมด
- ✏️ **Edit Sales**: แก้ไขข้อมูลที่มีอยู่
- 🗑️ **Delete Sales**: ลบข้อมูลที่ไม่ต้องการ
- 🔄 **Real-time Sync**: ซิงค์ข้อมูลกับ Grist Database
- 📱 **Responsive UI**: ใช้งานง่ายบนมือถือ

## Screenshots

| Dashboard | Add Sales | Sales List |
|-----------|-----------|------------|
| ![Dashboard](screenshots/dashboard.png) | ![Add](screenshots/add.png) | ![List](screenshots/list.png) |

## Technical Stack

- **Language**: Kotlin
- **Platform**: Android API 24+ (Android 7.0+)
- **Architecture**: MVVM + Repository Pattern
- **Database**: Grist (Cloud Spreadsheet Database)
- **Network**: Retrofit 2.9.0 + OkHttp
- **UI**: Traditional Android Views
- **Build**: Gradle with Kotlin DSL

## Prerequisites

- Android Studio Arctic Fox (2020.3.1) or later
- Android SDK API Level 24+
- Kotlin 1.8+
- Internet connection (for Grist API)

## Quick Start

### 1. Clone Repository
```bash
git clone https://github.com/your-repo/TimSales6.git
cd TimSales6
```

### 2. Setup Grist Database

1. Go to [docs.getgrist.com](https://docs.getgrist.com)
2. Create new document
3. Create table "TimSales" with columns:
   - `saleDate` (Date)
   - `kg` (Numeric)
   - `unitPrice` (Numeric) 
   - `amount` (Formula: `$kg * $unitPrice`)

### 3. Get API Credentials

1. Go to Profile Settings → API Keys
2. Generate new API key
3. Copy document ID from URL
4. Note table name ("TimSales")

### 4. Configure Application

Edit `app/src/main/java/com/example/timsales/config/AppConfig.kt`:

```kotlin
object AppConfig {
    const val GRIST_BASE_URL = "https://docs.getgrist.com/api/"
    const val GRIST_DOC_ID = "your-document-id-here"
    const val GRIST_TABLE_ID = "TimSales"
    const val GRIST_API_KEY = "your-api-key-here"
}
```

### 5. Build and Run

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Or use Android Studio:
- Click **Run** → **Run 'app'**

## Project Structure

```
app/
├── src/main/java/com/example/timsales/
│   ├── MainActivity.kt                 # Dashboard
│   ├── AddSalesActivity.kt            # Add new sale
│   ├── SalesListActivity.kt           # List all sales
│   ├── EditSalesActivity.kt           # Edit existing sale
│   ├── adapter/SalesRecordAdapter.kt  # RecyclerView adapter
│   ├── config/AppConfig.kt            # Configuration
│   ├── data/
│   │   ├── api/GristApiService.kt     # Retrofit interface
│   │   ├── model/SalesRecord.kt       # Data models
│   │   └── repository/SalesRepository.kt # Data layer
│   └── utils/DebugUtils.kt            # Logging utilities
└── res/layout/                        # UI layouts
```

## API Integration

### Grist API Endpoints Used

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET    | `/docs/{docId}/tables/{tableId}/records` | Get all records |
| POST   | `/docs/{docId}/tables/{tableId}/records` | Create new record |
| PATCH  | `/docs/{docId}/tables/{tableId}/records` | Update record |
| POST   | `/docs/{docId}/tables/{tableId}/data/delete` | Delete record |

### Authentication
```http
Authorization: Bearer your-api-key-here
Content-Type: application/json
```

## Data Model

```kotlin
data class SalesRecord(
    val id: Int = 0,                    // Auto-generated
    val saleDate: String,               // YYYY-MM-DD format
    val kg: Double,                     // Weight in kg
    val unitPrice: Double,              // Price per kg
    val amount: Double = 0.0,           // Calculated total
    val createdAt: String? = null,      // Creation timestamp
    val updatedAt: String? = null       // Update timestamp
)
```

## Key Features

### 1. Year 2025 Statistics
Dashboard shows statistics filtered for year 2025 only:
- Total sales amount
- Total weight sold
- Average price per kg
- Number of records

### 2. Flexible Timestamp Support
Handles multiple timestamp formats from Grist:
- Unix timestamp: `1757832054.363537`
- ISO format: `2025-09-16T14:30:00+07:00`
- Simple format: `2025-09-16 14:30:00`

### 3. Comprehensive Error Handling
- Network connectivity issues
- API response errors
- Data validation errors
- User-friendly error messages

### 4. Debug Logging
Extensive logging system for troubleshooting:
- API request/response logging
- Activity lifecycle tracking
- Exception logging with stack traces

## Configuration Options

### Network Timeouts
```kotlin
const val NETWORK_TIMEOUT_SECONDS = 30L
const val NETWORK_READ_TIMEOUT_SECONDS = 30L
const val NETWORK_WRITE_TIMEOUT_SECONDS = 30L
```

### Validation Rules
```kotlin
const val MIN_WEIGHT = 0.01     // กก.
const val MAX_WEIGHT = 10000.0  // กก.
const val MIN_PRICE = 0.01      // บาท/กก.
const val MAX_PRICE = 100000.0  // บาท/กก.
```

## Troubleshooting

### Common Issues

#### 1. Network Connection Error
```
Error: Unable to resolve host "docs.getgrist.com"
```
**Solution**: Check internet connection and DNS settings

#### 2. API Authentication Error
```
Error: 401 Unauthorized
```
**Solution**: Verify API key in AppConfig.kt

#### 3. Record ID Issues
```
Error: Record ID = 0 for all records
```
**Solution**: Check ID mapping in SalesRepository.getAllSalesRecords()

#### 4. Delete Operation Fails
```
Error: 404 Not Found on delete
```
**Solution**: Verify delete endpoint uses POST method with correct path

## Development

### Running Tests
```bash
./gradlew test           # Unit tests
./gradlew connectedAndroidTest  # Integration tests
```

### Debug Logging
Enable verbose logging in debug builds:
```kotlin
// DebugUtils will automatically log to console in debug mode
DebugUtils.log("Your debug message")
DebugUtils.logApi("API call details")
DebugUtils.logError("Error occurred", exception)
```

### Building Release
```bash
./gradlew assembleRelease
```

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

For support and questions:
- Create an [Issue](https://github.com/your-repo/TimSales6/issues)
- Check [Programming Documentation](PROGRAMMING_DOCUMENTATION.md)

## Changelog

### Version 1.0.0 (2025-09-16)
- Initial release
- Basic CRUD operations
- Grist API integration
- Year 2025 statistics dashboard
- Comprehensive error handling
- Debug logging system

---

**Made with ❤️ for Agricultural Business Management** - ระบบเก็บข้อมูลยอดขายสินค้าเกษตร

แอป Android สำหรับเก็บข้อมูลยอดขายสินค้าเกษตรที่เชื่อมต่อกับ Grist Database

## ความสามารถของแอป

### หน้าจอหลัก (MainActivity)
- แสดงสถิติสรุปยอดขาย:
  - ยอดขายรวม (Sum of Amount)
  - ปริมาณรวม (Sum of kg) 
  - ราคาเฉลี่ย (Average unitPrice)
- ปุ่มเพิ่มยอดขาย
- ปุ่มดูรายการยอดขาย

### หน้าเพิ่มยอดขาย (AddSalesActivity)
- เลือกวันที่ขาย
- ใส่น้ำหนัก (กิโลกรัม)
- ใส่ราคาต่อหน่วย (บาท/กก.)
- แสดงยอดรวมแบบ realtime
- บันทึกข้อมูลไปยัง Grist

### หน้ารายการยอดขาย (SalesListActivity)
- แสดงรายการยอดขายทั้งหมด
- กดแต่ละรายการเพื่อแก้ไข
- ปุ่มลบพร้อมการยืนยัน
- ปุ่ม FAB สำหรับเพิ่มยอดขาย

### หน้าแก้ไขยอดขาย (EditSalesActivity)
- แก้ไขวันที่ขาย
- แก้ไขน้ำหนัก
- แก้ไขราคาต่อหน่วย
- อัปเดตข้อมูลใน Grist

## โครงสร้างข้อมูลใน Grist

แอปนี้ต้องการตารางใน Grist ที่มี columns ดังนี้:

| Column Name | Type | Description | Required |
|-------------|------|-------------|----------|
| saleDate | Date | วันที่ขาย | ✓ |
| kg | Numeric | น้ำหนัก (กิโลกรัม) | ✓ |
| unitPrice | Numeric | ราคาต่อหน่วย (บาท/กก.) | ✓ |
| amount | Numeric (Formula) | ยอดรวม = kg × unitPrice | ✓ |
| created_at | DateTime | วันเวลาที่สร้าง (Server) | Auto |
| updated_at | DateTime | วันเวลาที่แก้ไข (Server) | Auto |

### Formula สำหรับ amount column:
```
$kg * $unitPrice
```

## การตั้งค่า Grist

1. **สร้าง Document ใหม่ใน Grist**
   - ไปที่ [Grist](https://docs.getgrist.com)
   - สร้าง Document ใหม่
   - ตั้งชื่อ Document เป็น "TimSales" หรือชื่ือที่ต้องการ

2. **สร้างตาราง Sales**
   - เพิ่มตารางใหม่ชื่อ "Sales"
   - เพิ่ม columns ตามโครงสร้างด้านบน
   - ตั้งค่า column types ให้ถูกต้อง
   - เพิ่ม Formula สำหรับ amount column

3. **สร้าง API Key**
   - ไปที่ Profile Settings > API Keys
   - คลิก "Create API Key"
   - คัดลอก API Key ที่ได้

4. **หา Document ID**
   - ดู URL ของ Document: `https://docs.getgrist.com/doc/YOUR_DOC_ID`
   - คัดลอก YOUR_DOC_ID

## การตั้งค่าแอป

แก้ไขไฟล์ `AppConfig.kt`:

```kotlin
// แก้ไขค่าเหล่านี้ให้ตรงกับข้อมูล Grist ของคุณ
const val GRIST_BASE_URL = "https://docs.getgrist.com/api/"
const val GRIST_DOC_ID = "YOUR_DOC_ID_HERE" // ใส่ Document ID จริง
const val GRIST_TABLE_ID = "Sales" // ชื่อตาราง
const val GRIST_API_KEY = "YOUR_API_KEY_HERE" // ใส่ API Key จริง
```

## การ Build และ Run

1. **Requirements:**
   - Android Studio Giraffe หรือใหม่กว่า
   - Android SDK 26 ขึ้นไป
   - Kotlin 1.8+
   - Java 8

2. **Dependencies สำคัญ:**
   - Retrofit 2.9.0 (HTTP Client)
   - Gson (JSON Parsing) 
   - Material Design Components
   - AndroidX Libraries
   - Coroutines (Async Programming)

3. **Steps:**
   ```bash
   git clone <repository-url>
   cd TimSales6
   # แก้ไขไฟล์ AppConfig.kt
   # เปิดใน Android Studio
   # Build และ Run
   ```

## โครงสร้างโปรเจค

```
app/
├── src/main/java/com/example/timsales/
│   ├── MainActivity.kt              # หน้าจอหลัก
│   ├── AddSalesActivity.kt          # เพิ่มยอดขาย
│   ├── SalesListActivity.kt         # รายการยอดขาย
│   ├── EditSalesActivity.kt         # แก้ไขยอดขาย
│   ├── adapter/
│   │   └── SalesRecordAdapter.kt    # RecyclerView Adapter
│   ├── config/
│   │   └── AppConfig.kt             # Configuration
│   ├── data/
│   │   ├── api/
│   │   │   └── GristApiService.kt   # Retrofit API Interface
│   │   ├── model/
│   │   │   └── SalesRecord.kt       # Data Models
│   │   └── repository/
│   │       └── SalesRepository.kt   # Data Repository
│   └── ui/theme/                    # UI Themes
├── src/main/res/
│   ├── layout/                      # XML Layouts
│   ├── drawable/                    # Drawable Resources
│   ├── values/                      # Strings, Colors, etc.
│   └── xml/                         # XML Resources
└── build.gradle.kts                 # Build Configuration
```

## การใช้งาน

1. **เพิ่มยอดขาย:**
   - เปิดแอป > กด "เพิ่มยอดขาย"
   - เลือกวันที่, ใส่น้ำหนัก, ใส่ราคา
   - กด "บันทึก"

2. **ดูรายการยอดขาย:**
   - หน้าจอหลัก > กด "ดูรายการยอดขาย"
   - ดูรายการทั้งหมด
   - กดรายการเพื่อแก้ไข
   - กดปุ่มลบเพื่อลบข้อมูล

3. **แก้ไขยอดขาย:**
   - จากหน้ารายการ > กดรายการที่ต้องการแก้ไข
   - แก้ไขข้อมูล
   - กด "อัปเดต"

## การแก้ไขปัญหา

### ปัญหาเชื่อมต่อ API
- ตรวจสอบ API Key, Document ID, Table ID
- ตรวจสอบการเชื่อมต่อ Internet
- ตรวจสอบ Logs ใน Android Studio

### ปัญหา Build
- Clean Project > Rebuild Project
- ตรวจสอบ dependencies ใน build.gradle.kts
- ตรวจสอบ SDK versions

### ปัญหา Grist
- ตรวจสอบ Column Names ใน Grist ต้องตรงกับ @SerializedName
- ตรวจสอบ API Key มีสิทธิ์ในการเข้าถึง Document
- ตรวจสอบ Formula ใน amount column

## License

This project is for educational purposes.

## Author

Created for TimSales Agricultural Sales Management System