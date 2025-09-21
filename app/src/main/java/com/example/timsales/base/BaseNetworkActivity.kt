package com.example.timsales.base

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.timsales.R
import com.example.timsales.utils.NetworkUtils
import com.example.timsales.utils.NetworkType

/**
 * Base Activity ที่มีการตรวจสอบ Network connectivity
 */
abstract class BaseNetworkActivity : AppCompatActivity() {
    
    private var networkReceiver: NetworkChangeReceiver? = null
    private var noInternetDialog: AlertDialog? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // เริ่มการติดตาม network status
        NetworkUtils.startNetworkMonitoring(this)
        
        // สังเกตการเปลี่ยนแปลง network status
        NetworkUtils.isNetworkAvailable.observe(this, Observer { isAvailable ->
            handleNetworkChange(isAvailable)
        })
    }
    
    override fun onResume() {
        super.onResume()
        
        // Register network change receiver
        networkReceiver = NetworkChangeReceiver()
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)
        
        // ตรวจสอบ network status เริ่มต้น
        checkInitialNetworkStatus()
    }
    
    override fun onPause() {
        super.onPause()
        
        // Unregister network change receiver
        networkReceiver?.let { receiver ->
            try {
                unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Receiver might not be registered
            }
        }
        networkReceiver = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // หยุดการติดตาม network
        NetworkUtils.stopNetworkMonitoring()
        
        // ปิด dialogs
        noInternetDialog?.dismiss()
    }
    
    /**
     * ตรวจสอบ network status เมื่อเริ่มต้น
     */
    private fun checkInitialNetworkStatus() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            handleNetworkChange(false)
        }
    }
    
    /**
     * จัดการเมื่อสถานะ network เปลี่ยนแปลง
     */
    private fun handleNetworkChange(isAvailable: Boolean) {
        if (isAvailable) {
            onNetworkAvailable()
        } else {
            onNetworkUnavailable()
        }
    }
    
    /**
     * เมื่อ network พร้อมใช้งาน
     */
    protected open fun onNetworkAvailable() {
        // ปิด dialog เมื่อ network กลับมา
        noInternetDialog?.dismiss()
        
        // ไม่แสดงข้อความแจ้งเตือนเมื่อ network กลับมา
        // เพียงแค่เรียก onRetryAfterNetworkRestore() สำหรับ activity ที่ต้องการ
        onRetryAfterNetworkRestore()
    }
    
    /**
     * เมื่อ network ไม่พร้อมใช้งาน
     */
    protected open fun onNetworkUnavailable() {
        showNoInternetDialog()
    }
    
    /**
     * แสดง dialog เมื่อไม่มี internet
     */
    private fun showNoInternetDialog() {
        if (noInternetDialog?.isShowing == true) return
        
        noInternetDialog = AlertDialog.Builder(this)
            .setTitle("ไม่มีการเชื่อมต่อ Internet")
            .setMessage("กรุณาตรวจสอบการเชื่อมต่อ Internet ของคุณ\n\n" +
                       "• ตรวจสอบ WiFi หรือ Mobile Data\n" +
                       "• ลองเปิด-ปิด Airplane Mode\n" +
                       "• ตรวจสอบสัญญาณ Internet")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("ลองอีกครั้ง") { _, _ ->
                checkNetworkAndRetry()
            }
            .setNegativeButton("ตั้งค่า Internet") { _, _ ->
                openNetworkSettings()
            }
            .setCancelable(false)
            .create()
            
        noInternetDialog?.show()
    }
    
    /**
     * ตรวจสอบ network และลองใหม่
     */
    private fun checkNetworkAndRetry() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            onNetworkAvailable()
            onRetryAfterNetworkRestore()
        } else {
            Toast.makeText(this, "ยังไม่มีการเชื่อมต่อ Internet", Toast.LENGTH_SHORT).show()
            showNoInternetDialog()
        }
    }
    
    /**
     * เปิดหน้า settings สำหรับ network
     */
    private fun openNetworkSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "ไม่สามารถเปิดหน้าตั้งค่าได้", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * ตรวจสอบ network ก่อนเรียก API
     */
    protected fun isNetworkAvailableForApi(): Boolean {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showNoNetworkToast()
            return false
        }
        return true
    }
    
    /**
     * แสดง toast เมื่อไม่มี network สำหรับ API call
     */
    private fun showNoNetworkToast() {
        Toast.makeText(
            this,
            "ไม่มีการเชื่อมต่อ Internet\nกรุณาตรวจสอบการเชื่อมต่อของคุณ",
            Toast.LENGTH_LONG
        ).show()
    }
    
    /**
     * Abstract method สำหรับ retry หลังจาก network กลับมา
     */
    protected abstract fun onRetryAfterNetworkRestore()
    
    /**
     * BroadcastReceiver สำหรับตรวจสอบการเปลี่ยนแปลง network
     */
    private inner class NetworkChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ConnectivityManager.CONNECTIVITY_ACTION) {
                val isNetworkAvailable = NetworkUtils.isNetworkAvailable(this@BaseNetworkActivity)
                handleNetworkChange(isNetworkAvailable)
            }
        }
    }
}