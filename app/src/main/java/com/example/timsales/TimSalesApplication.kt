package com.example.timsales

import android.app.Application
import com.example.timsales.utils.DebugUtils

/**
 * Application class สำหรับ Global Configuration
 */
class TimSalesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        
        
        // Set up global exception handler
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            DebugUtils.logException("UncaughtException", "Thread: ${thread.name}", Exception(exception))
            
            // Log the full stack trace
            exception.printStackTrace()
            
            // You can also save crash logs to file or send to crash reporting service
            handleCrash(exception)
        }
        
        
        
        // Log app configuration
        logAppConfiguration()
    }
    
    private fun handleCrash(exception: Throwable) {
        try {
            // Log crash details
            DebugUtils.logError("=== APP CRASHED ===")
            DebugUtils.logError("Exception: ${exception.javaClass.simpleName}")
            DebugUtils.logError("Message: ${exception.message}")
            DebugUtils.logError("Cause: ${exception.cause}")
            DebugUtils.logError("Stack trace:")
            exception.stackTrace.forEach { element ->
                DebugUtils.logError("  at $element")
            }
            DebugUtils.logError("=== END CRASH LOG ===")
            
        } catch (e: Exception) {
            // If logging fails, at least try to print to system out
            System.err.println("Failed to log crash: ${e.message}")
            exception.printStackTrace()
        }
    }
    
    private fun logAppConfiguration() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            
            // Use longVersionCode for API 28+ (Android 9.0) to avoid deprecation warning
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
        } catch (e: Exception) {
            DebugUtils.logError("Failed to log app configuration: ${e.message}")
        }
    }
}