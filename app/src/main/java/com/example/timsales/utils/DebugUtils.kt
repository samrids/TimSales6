package com.example.timsales.utils

import android.util.Log

/**
 * Debug Utility Class สำหรับการ debug แอป
 */
object DebugUtils {
    
    private const val TAG = "TimSales_Debug"
    private const val ERROR_TAG = "TimSales_Error"
    private const val API_TAG = "TimSales_API"
    
    /**
     * Log debug message
     */
    fun log(message: String, tag: String = TAG) {
        Log.d(tag, message)
    }
    
    /**
     * Log error message
     */
    fun logError(message: String, throwable: Throwable? = null) {
        Log.e(ERROR_TAG, message, throwable)
    }
    
    /**
     * Log API calls
     */
    fun logApi(message: String) {
        Log.d(API_TAG, message)
    }
    
    /**
     * Log exception with stack trace
     */
    fun logException(tag: String, message: String, exception: Exception) {
        Log.e(tag, "$message: ${exception.message}", exception)
        exception.printStackTrace()
    }
    
    /**
     * Log lifecycle events
     */
    fun logLifecycle(activityName: String, event: String) {
        Log.d("${TAG}_Lifecycle", "$activityName: $event")
    }
}