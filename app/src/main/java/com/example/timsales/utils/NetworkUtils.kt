package com.example.timsales.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Utility class สำหรับตรวจสอบสถานะการเชื่อมต่อ Network
 */
object NetworkUtils {
    
    private val _isNetworkAvailable = MutableLiveData<Boolean>()
    val isNetworkAvailable: LiveData<Boolean> = _isNetworkAvailable
    
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    
    /**
     * ตรวจสอบว่ามีการเชื่อมต่อ Internet หรือไม่
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * ตรวจสอบประเภทของการเชื่อมต่อ
     */
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
            
            return when {
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo ?: return NetworkType.NONE
            
            return when (networkInfo.type) {
                ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR
                ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                else -> NetworkType.OTHER
            }
        }
    }
    
    /**
     * เริ่มการติดตาม Network status
     */
    fun startNetworkMonitoring(context: Context) {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isNetworkAvailable.postValue(true)
                }
                
                override fun onLost(network: Network) {
                    _isNetworkAvailable.postValue(false)
                }
                
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    _isNetworkAvailable.postValue(hasInternet)
                }
            }
            
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
                
            connectivityManager?.registerNetworkCallback(networkRequest, networkCallback!!)
        }
        
        // Initial check
        _isNetworkAvailable.postValue(isNetworkAvailable(context))
    }
    
    /**
     * หยุดการติดตาม Network status
     */
    fun stopNetworkMonitoring() {
        networkCallback?.let { callback ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager?.unregisterNetworkCallback(callback)
            }
        }
        connectivityManager = null
        networkCallback = null
    }
    
    /**
     * ตรวจสอบว่าสามารถเชื่อมต่อไปยัง Grist server ได้หรือไม่
     */
    suspend fun canReachGristServer(): Boolean {
        return try {
            val process = ProcessBuilder()
                .command("ping", "-c", "1", "docs.getgrist.com")
                .start()
            
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * ประเภทของการเชื่อมต่อ Network
 */
enum class NetworkType {
    NONE,
    WIFI,
    CELLULAR,
    ETHERNET,
    OTHER
}