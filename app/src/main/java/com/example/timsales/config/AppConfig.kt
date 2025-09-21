package com.example.timsales.config

import com.example.timsales.BuildConfig

/**
 * Configuration class สำหรับเก็บค่าคงที่ต่างๆ
 * ค่าสำคัญจะถูกโหลดจากไฟล์ .env ผ่าน BuildConfig เพื่อความปลอดภัย
 */
object AppConfig {
    
    /**
     * Grist API Configuration
     * ค่าเหล่านี้จะถูกโหลดจากไฟล์ .env ผ่าน BuildConfig
     */
    
    // Base URL ของ Grist API (จาก .env)
    val GRIST_BASE_URL: String = BuildConfig.GRIST_BASE_URL
    
    // Document ID จาก URL ของ Grist document (จาก .env)
    val GRIST_DOC_ID: String = BuildConfig.GRIST_DOC_ID
    
    // Table ID ของตารางที่เก็บข้อมูลยอดขาย (จาก .env)
    val GRIST_TABLE_ID: String = BuildConfig.GRIST_TABLE_ID
    
    // Table ID สำหรับ TimYear table (จาก .env)
    val TIMYEAR_TABLE_ID: String = BuildConfig.TIMYEAR_TABLE_ID
    
    // API Key สำหรับ Authentication (จาก .env)
    val GRIST_API_KEY: String = BuildConfig.GRIST_API_KEY
    
    /**
     * Network Configuration
     */
    const val NETWORK_TIMEOUT_SECONDS = 30L
    const val NETWORK_READ_TIMEOUT_SECONDS = 30L
    const val NETWORK_WRITE_TIMEOUT_SECONDS = 30L
    
    /**
     * Date Format Configuration
     */
    const val DATE_FORMAT_API = "yyyy-MM-dd"
    const val DATE_FORMAT_DISPLAY = "dd/MM/yyyy"
    
    /**
     * Number Format Configuration  
     */
    const val DECIMAL_PLACES = 2
    const val CURRENCY_SYMBOL = "บาท"
    const val WEIGHT_UNIT = "กก."
    const val PRICE_UNIT = "บาท/กก."
    
    /**
     * UI Configuration
     */
    const val ANIMATION_DURATION_MS = 300L
    const val LOADING_DELAY_MS = 500L
    
    /**
     * Validation Configuration
     */
    const val MIN_WEIGHT = 0.01 // กิโลกรัม
    const val MAX_WEIGHT = 10000.0 // กิโลกรัม  
    const val MIN_PRICE = 0.01 // บาทต่อกิโลกรัม
    const val MAX_PRICE = 100000.0 // บาทต่อกิโลกรัม
}

/**
 * คำแนะนำในการตั้งค่า:
 * 
 * 1. สร้างไฟล์ .env ที่ root ของโปรเจ็กต์
 * 2. เพิ่มค่า configuration ดังนี้:
 *    GRIST_BASE_URL=https://docs.getgrist.com/api/
 *    GRIST_DOC_ID=your_document_id
 *    GRIST_API_KEY=your_api_key
 *    GRIST_TABLE_ID=TimSales
 *    TIMYEAR_TABLE_ID=TimYear
 * 
 * 3. สร้าง Document ใหม่ใน Grist
 * 4. สร้างตาราง "TimSales" ด้วย columns:
 *    - saleDate (Date)
 *    - kg (Numeric) 
 *    - unitPrice (Numeric)
 *    - amount (Numeric, Formula: $kg * $unitPrice)
 * 5. ไปที่ Profile Settings > API Keys และสร้าง API Key ใหม่
 * 6. คัดลอก Document ID จาก URL
 * 7. อัปเดตค่าในไฟล์ .env ให้ถูกต้อง
 * 
 * หมายเหตุ: ไฟล์ .env จะไม่ถูก commit เข้า git เพื่อความปลอดภัย
 */