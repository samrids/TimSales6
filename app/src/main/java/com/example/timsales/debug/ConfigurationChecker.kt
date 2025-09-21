package com.example.timsales.debug

import com.example.timsales.config.AppConfig
import com.example.timsales.utils.DebugUtils

/**
 * Debug helper สำหรับตรวจสอบการตั้งค่าพื้นฐาน
 */
object ConfigurationChecker {
    
    fun checkConfiguration(): Boolean {
        var isValid = true
        
    // Configuration check started
        
        // Check Grist configuration
        if (AppConfig.GRIST_DOC_ID == "YOUR_DOC_ID_HERE") {
            DebugUtils.logError("GRIST_DOC_ID not configured!")
            isValid = false
        } else {
            // GRIST_DOC_ID is set
        }
        
        if (AppConfig.GRIST_API_KEY == "YOUR_API_KEY_HERE") {
            DebugUtils.logError("GRIST_API_KEY not configured!")
            isValid = false
        } else {
            // GRIST_API_KEY is set
        }

        // Check network timeouts
        
        if (!isValid) {
            DebugUtils.logError("❌ Configuration has errors - app may not work properly")
        }
        
        return isValid
    }
    
    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== DEBUG INFO ===")
            appendLine("Base URL: ${AppConfig.GRIST_BASE_URL}")
            appendLine("Doc ID: ${AppConfig.GRIST_DOC_ID}")
            appendLine("Table ID: ${AppConfig.GRIST_TABLE_ID}")
            appendLine("API Key Length: ${AppConfig.GRIST_API_KEY.length}")
            appendLine("API Key Configured: ${AppConfig.GRIST_API_KEY != "YOUR_API_KEY_HERE"}")
            appendLine("=== END DEBUG INFO ===")
        }
    }
}