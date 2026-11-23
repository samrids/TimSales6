package com.example.timsales

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.example.timsales.base.BaseNetworkActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timsales.adapter.SalesRecordAdapter
import com.example.timsales.data.model.ApiResult
import com.example.timsales.data.model.SalesRecord
import com.example.timsales.data.model.TimYear
import com.example.timsales.data.repository.SalesRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class SalesListActivity : BaseNetworkActivity() {

    private val repository: SalesRepository by lazy { SalesRepository() }
    private lateinit var adapter: SalesRecordAdapter
    
    // Views
    private lateinit var rvSalesList: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var fabAddSales: FloatingActionButton

    private var salesRecords = mutableListOf<SalesRecord>()

    // รอบการขายที่เลือกมาจากหน้าหลัก (ใช้กรองรายการ)
    private var currentSeason: TimYear? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales_list)

        // Top app bar with back navigation
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // อ่านรอบการขายที่ส่งมาจากหน้าหลัก
        readSeasonFromIntent()

    // Initialize repository (lazy) - touch to ensure initialization
    val repo = repository

        // Initialize views
        initViews()
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Set click listeners
        setClickListeners()
        
        // Load data
        loadSalesRecords()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun readSeasonFromIntent() {
        val year = intent.getIntExtra("SEASON_YEAR", 0)
        currentSeason = if (year > 0) {
            TimYear(
                saleYear = year,
                startDate = intent.getStringExtra("SEASON_START"),
                endDate = intent.getStringExtra("SEASON_END"),
                seasonCode = intent.getStringExtra("SEASON_CODE")
            )
        } else {
            null
        }
        // แสดงรอบบน toolbar
        currentSeason?.let { supportActionBar?.subtitle = it.getSeasonLabel() }
    }

    private fun initViews() {
        rvSalesList = findViewById(R.id.rvSalesList)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        fabAddSales = findViewById(R.id.fabAddSales)
    }

    private fun setupRecyclerView() {
        adapter = SalesRecordAdapter(
            salesRecords = salesRecords,
            onEditClick = { record ->
                editSalesRecord(record)
            },
            onDeleteClick = { record ->
                showDeleteConfirmation(record)
            }
        )
        
        rvSalesList.layoutManager = LinearLayoutManager(this)
        rvSalesList.adapter = adapter
    }

    private fun setClickListeners() {
        fabAddSales.setOnClickListener {
            val intent = Intent(this, AddSalesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadSalesRecords() {
        // ตรวจสอบการเชื่อมต่อ Internet ก่อน
        if (!isNetworkAvailableForApi()) {
            return
        }
        
        lifecycleScope.launch {
            showLoading(true)

            // ถ้ามีรอบที่เลือก ให้กรองเฉพาะรอบนั้น ไม่งั้นแสดงทั้งหมด
            val season = currentSeason
            val result = if (season != null) {
                repository.getSalesRecordsForSeason(season)
            } else {
                repository.getAllSalesRecords()
            }

            when (result) {
                is ApiResult.Success -> {
                    showLoading(false)
                    val records = result.data
                    updateRecordsList(records)
                    showError(false)
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    showError(true, result.exception.message)
                    Toast.makeText(
                        this@SalesListActivity,
                        "เกิดข้อผิดพลาด: ${result.exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {
                    showLoading(false)
                }
            }
        }
    }

    private fun updateRecordsList(records: List<SalesRecord>) {
        salesRecords.clear()
        salesRecords.addAll(records)
        adapter.updateData(salesRecords)
        
        // Show/hide empty state
        if (records.isEmpty()) {
            rvSalesList.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvSalesList.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun editSalesRecord(record: SalesRecord) {
        val intent = Intent(this, EditSalesActivity::class.java).apply {
            putExtra("SALES_ID", record.id)
            putExtra("SALE_DATE", record.saleDate)
            putExtra("WEIGHT", record.kg)
            putExtra("UNIT_PRICE", record.unitPrice)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmation(record: SalesRecord) {
        AlertDialog.Builder(this)
            .setTitle("ยืนยันการลบ")
            .setMessage("คุณแน่ใจหรือไม่ที่จะลบข้อมูลการขายวันที่ ${record.getFormattedDate()}?\n\nยอดเงิน: ${record.getFormattedAmount()}")
            .setPositiveButton("ลบ") { _, _ ->
                deleteSalesRecord(record)
            }
            .setNegativeButton("ยกเลิก", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteSalesRecord(record: SalesRecord) {
        // ตรวจสอบการเชื่อมต่อ Internet ก่อน
        if (!isNetworkAvailableForApi()) {
            return
        }
        
        lifecycleScope.launch {
            showLoading(true)
            
            try {
                // Log ข้อมูล record เพื่อ debug
                println("DEBUG: Attempting to delete record")
                println("DEBUG: Record ID: ${record.id}")
                println("DEBUG: Record Date: ${record.saleDate}")
                println("DEBUG: Record Amount: ${record.amount}")
                println("DEBUG: Record kg: ${record.kg}")
                println("DEBUG: Record unitPrice: ${record.unitPrice}")
                
                // ตรวจสอบว่า ID ถูกต้องหรือไม่
                if (record.id <= 0) {
                    showLoading(false)
                    Toast.makeText(
                        this@SalesListActivity,
                        "ไม่สามารถลบได้: ข้อมูล ID ไม่ถูกต้อง (ID: ${record.id})",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }
                
                when (val result = repository.deleteSalesRecord(record.id)) {
                    is ApiResult.Success -> {
                        showLoading(false)
                        
                        // แสดงข้อความสำเร็จ
                        Toast.makeText(
                            this@SalesListActivity,
                            "ลบข้อมูลเรียบร้อยแล้ว",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // โหลดข้อมูลใหม่จาก server เพื่อให้แน่ใจว่าข้อมูลถูกลบจริง
                        loadSalesRecords()
                    }
                    is ApiResult.Error -> {
                        showLoading(false)
                        
                        // Log error เพื่อ debug
                        println("DEBUG: Delete error: ${result.exception.message}")
                        
                        Toast.makeText(
                            this@SalesListActivity,
                            "เกิดข้อผิดพลาดในการลบข้อมูล: ${result.exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    else -> {
                        showLoading(false)
                        Toast.makeText(
                            this@SalesListActivity,
                            "เกิดข้อผิดพลาดที่ไม่ทราบสาเหตุ",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                showLoading(false)
                println("DEBUG: Exception in deleteSalesRecord: ${e.message}")
                Toast.makeText(
                    this@SalesListActivity,
                    "เกิดข้อผิดพลาด: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        fabAddSales.isEnabled = !show
    }

    private fun showError(show: Boolean, message: String? = null) {
        tvError.visibility = if (show) View.VISIBLE else View.GONE
        message?.let { tvError.text = "เกิดข้อผิดพลาด: $it" }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        loadSalesRecords()
    }
    
    // ==================== Network Methods ====================
    
    /**
     * เรียกใหม่หลังจาก network กลับมา
     */
    override fun onRetryAfterNetworkRestore() {
        // โหลดข้อมูลใหม่เมื่อ network กลับมา
        loadSalesRecords()
    }
    
    /**
     * เมื่อ network ไม่พร้อมใช้งาน
     */
    override fun onNetworkUnavailable() {
        super.onNetworkUnavailable()
        
        // แสดงข้อความ error ในหน้าจอ
        runOnUiThread {
            showError(true, "ไม่มีการเชื่อมต่อ Internet\nไม่สามารถโหลดข้อมูลได้")
            showLoading(false)
        }
    }
    
    /**
     * เมื่อ network พร้อมใช้งาน
     */
    override fun onNetworkAvailable() {
        super.onNetworkAvailable()
        
        // ซ่อน error message
        runOnUiThread {
            showError(false)
        }
    }
}