package com.example.timsales

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.timsales.base.BaseNetworkActivity
import com.example.timsales.data.model.ApiResult
import com.example.timsales.data.repository.SalesRepository
import com.example.timsales.data.repository.TimYearRepository
import com.example.timsales.utils.DebugUtils
import com.example.timsales.debug.ConfigurationChecker
import kotlinx.coroutines.launch

class MainActivity : BaseNetworkActivity() {

    private val repository: SalesRepository by lazy { SalesRepository() }
    private val timYearRepository: TimYearRepository by lazy { TimYearRepository(this) }
    private var currentYear: Int = 2025 // Default fallback year
    
    // Views
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvTotalKg: TextView
    private lateinit var tvAveragePrice: TextView
    private lateinit var tvCountOfSales: TextView
    private lateinit var btnAddSales: Button
    private lateinit var btnViewSalesList: Button
    private lateinit var btnRefresh: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            
            // Check configuration first
            val configValid = ConfigurationChecker.checkConfiguration()
            if (!configValid) {
                DebugUtils.logError("Configuration is invalid - showing debug info")
                Toast.makeText(this, "Configuration Error - Check logs", Toast.LENGTH_LONG).show()
            }
            
            setContentView(R.layout.activity_main)
            
            // Set up toolbar
            setSupportActionBar(findViewById(R.id.toolbar))
            supportActionBar?.title = "ระบบเก็บข้อมูลยอดขายสินค้าเกษตร"
            
            // Initialize repositories (lazy) - touch to ensure initialization
            val repo = timYearRepository
            
            // Initialize views
            initViews()
            
            // Set click listeners
            setClickListeners()
            
            // Load initial data only if config is valid
            if (configValid) {
                initializeCurrentYear()
            } else {
                // Show configuration info in error view
                showError(true, "Configuration Error - Check AppConfig.kt")
            }
            
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        try {
            
            tvTotalAmount = findViewById(R.id.tvTotalAmount)
            
            tvTotalKg = findViewById(R.id.tvTotalKg)
            
            tvAveragePrice = findViewById(R.id.tvAveragePrice)
            
            tvCountOfSales = findViewById(R.id.tvCountOfSales)
            
            btnAddSales = findViewById(R.id.btnAddSales)
            
            btnViewSalesList = findViewById(R.id.btnViewSalesList)
            
            btnRefresh = findViewById(R.id.btnRefresh)
            
            progressBar = findViewById(R.id.progressBar)
            
            tvError = findViewById(R.id.tvError)
            
            
        } catch (e: Exception) {
            throw e // Re-throw to catch in onCreate
        }
    }

    private fun setClickListeners() {
        try {
            
            btnAddSales.setOnClickListener {
                try {
                    val intent = Intent(this, AddSalesActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error opening Add Sales: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            btnViewSalesList.setOnClickListener {
                try {
                    val intent = Intent(this, SalesListActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error opening Sales List: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            btnRefresh.setOnClickListener {
                try {
                    loadStatistics()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error refreshing: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            
            
        } catch (e: Exception) {
            throw e
        }
    }

    private fun loadStatistics() {
        try {
            // ตรวจสอบ network ก่อนเรียก API
            if (!isNetworkAvailableForApi()) {
                return
            }
            
            lifecycleScope.launch {
                try {
                    showLoading(true)
                    
                    when (val result = repository.getSalesStatistics()) {
                        is ApiResult.Success -> {
                            showLoading(false)
                            val statistics = result.data
                            updateStatisticsViews(statistics)
                            showError(false)
                        }
                        is ApiResult.Error -> {
                            DebugUtils.logError("API Error: ${result.exception.message}", result.exception)
                            showLoading(false)
                            showError(true, result.exception.message)
                            Toast.makeText(
                                this@MainActivity, 
                                "เกิดข้อผิดพลาด: ${result.exception.message}", 
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {
                            showLoading(false)
                        }
                    }
                } catch (e: Exception) {
                    showLoading(false)
                    showError(true, e.message)
                    Toast.makeText(this@MainActivity, "Coroutine Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "LoadStatistics Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateStatisticsViews(statistics: com.example.timsales.data.model.SalesStatistics) {
        try {
            if (::tvTotalAmount.isInitialized) {
                tvTotalAmount.text = statistics.getFormattedTotalAmount()
            }
            if (::tvTotalKg.isInitialized) {
                tvTotalKg.text = statistics.getFormattedTotalKg()
            }
            if (::tvAveragePrice.isInitialized) {
                tvAveragePrice.text = statistics.getFormattedAveragePrice()
            }
            if (::tvCountOfSales.isInitialized) {
                tvCountOfSales.text = statistics.getFormattedRecordCount()
            }
        } catch (e: Exception) {
        }
    }

    private fun showLoading(show: Boolean) {
        try {
            if (::progressBar.isInitialized) {
                progressBar.visibility = if (show) View.VISIBLE else View.GONE
            }
            if (::btnRefresh.isInitialized) {
                btnRefresh.isEnabled = !show
            }
        } catch (e: Exception) {
        }
    }

    private fun showError(show: Boolean, message: String? = null) {
        try {
            if (::tvError.isInitialized) {
                tvError.visibility = if (show) View.VISIBLE else View.GONE
                message?.let { tvError.text = it }
            }
        } catch (e: Exception) {
        }
    }

    // ==================== Menu Methods ====================
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                try {
                    val intent = Intent(this, TimYearManagementActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Error opening settings: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    // ==================== TimYear Methods ====================
    
    private fun initializeCurrentYear() {
        try {
            // ตรวจสอบ network ก่อนเรียก API
            if (!isNetworkAvailableForApi()) {
                // ใช้ปี default และโหลดสถิติ offline
                currentYear = 2025
                updateYearDisplay()
                return
            }
            
            lifecycleScope.launch {
                try {
                    showLoading(true)
                    
                    val result = timYearRepository.initializeCurrentYearIfNeeded()
                    if (result.isSuccess) {
                        currentYear = result.getOrNull() ?: 2025
                        
                        // อัปเดต UI ให้แสดงปีปัจจุบัน
                        updateYearDisplay()
                        
                        // โหลดสถิติของปีปัจจุบัน
                        loadStatistics()
                        
                    } else {
                        Toast.makeText(this@MainActivity, "ไม่สามารถโหลดข้อมูลปีได้", Toast.LENGTH_SHORT).show()
                        
                        // ใช้ปี default และโหลดสถิติ
                        currentYear = 2025
                        updateYearDisplay()
                        loadStatistics()
                    }
                    
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    
                    // ใช้ปี default และโหลดสถิติ
                    currentYear = 2025
                    updateYearDisplay()
                    loadStatistics()
                }
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateYearDisplay() {
        try {
            // อัปเดต title ของ toolbar
            supportActionBar?.title = "ยอดขายประจำปี $currentYear"
        } catch (e: Exception) {
        }
    }
    
    private fun refreshCurrentYear() {
        try {
            initializeCurrentYear()
        } catch (e: Exception) {
            Toast.makeText(this, "Error refreshing year: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            // Refresh year and data when returning to this activity
            refreshCurrentYear()
        } catch (e: Exception) {
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    
    // ==================== Network Methods ====================
    
    /**
     * เรียกใหม่หลังจาก network กลับมา
     */
    override fun onRetryAfterNetworkRestore() {
        try {
            // Refresh year และ statistics
            refreshCurrentYear()
        } catch (e: Exception) {
            DebugUtils.logError("Error in onRetryAfterNetworkRestore", e)
        }
    }
    
    /**
     * เมื่อ network พร้อมใช้งาน
     */
    override fun onNetworkAvailable() {
        super.onNetworkAvailable()
        
        // แสดงข้อความว่า network กลับมาแล้ว
        runOnUiThread {
            showError(false)
        }
    }
    
    /**
     * เมื่อ network ไม่พร้อมใช้งาน
     */
    override fun onNetworkUnavailable() {
        super.onNetworkUnavailable()
        
        // แสดงข้อความ error
        runOnUiThread {
            showError(true, "ไม่มีการเชื่อมต่อ Internet\nกรุณาตรวจสอบการเชื่อมต่อของคุณ")
            showLoading(false)
        }
    }
}