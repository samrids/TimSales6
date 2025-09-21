package com.example.timsales

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.timsales.data.model.TimYear
import com.example.timsales.data.repository.TimYearRepository
import com.example.timsales.utils.DebugUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class TimYearManagementActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TimYearManagementActivity"
    }

    // Repository
    private val timYearRepository: TimYearRepository by lazy { TimYearRepository(this) }

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TimYearAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvEmpty: LinearLayout
    private lateinit var fabAdd: FloatingActionButton

    // Activity Result Launchers
    private lateinit var addTimYearLauncher: ActivityResultLauncher<Intent>
    private lateinit var editTimYearLauncher: ActivityResultLauncher<Intent>

    // Data
    private var timYearList = mutableListOf<TimYear>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            DebugUtils.logLifecycle(TAG, "onCreate - Start")
            
            setContentView(R.layout.activity_timyear_management)
            
            // Initialize Activity Result Launchers
            initializeActivityResultLaunchers()
            
            // Set up toolbar
            setSupportActionBar(findViewById(R.id.toolbar))
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "จัดการปีขาย"
            
            // Initialize repository (lazy) - touch to ensure initialization
            val repo = timYearRepository
            
            // Initialize views
            initViews()
            
            // Setup RecyclerView
            setupRecyclerView()
            
            // Set click listeners
            setClickListeners()
            
            // Load initial data
            loadTimYears()
            
            DebugUtils.logLifecycle(TAG, "onCreate - End")
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        try {
            
            
            recyclerView = findViewById(R.id.recyclerViewTimYear)
            progressBar = findViewById(R.id.progressBar)
            tvError = findViewById(R.id.tvError)
            tvEmpty = findViewById(R.id.tvEmpty)
            fabAdd = findViewById(R.id.fabAdd)
            
            
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in initViews", e)
            throw e
        }
    }

    private fun initializeActivityResultLaunchers() {
        try {
            
            
            // Add TimYear Launcher
            addTimYearLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                try {
                    
                    
                    if (result.resultCode == RESULT_OK) {
                        
                        Toast.makeText(this, "เพิ่มปีขายเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                        loadTimYears() // Reload data
                    }
                    
                } catch (e: Exception) {
                    DebugUtils.logException(TAG, "Error in addTimYearLauncher result", e)
                }
            }
            
            // Edit TimYear Launcher
            editTimYearLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                try {
                    
                    
                    if (result.resultCode == RESULT_OK) {
                        
                        Toast.makeText(this, "แก้ไขปีขายเรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                        loadTimYears() // Reload data
                    }
                    
                } catch (e: Exception) {
                    DebugUtils.logException(TAG, "Error in editTimYearLauncher result", e)
                }
            }
            
            
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error initializing Activity Result Launchers", e)
            throw e
        }
    }

    private fun setupRecyclerView() {
        try {
            
            
            adapter = TimYearAdapter(
                timYearList = timYearList,
                onEditClick = { timYear ->
                    
                    editTimYear(timYear)
                },
                onDeleteClick = { timYear ->
                    
                    showDeleteConfirmDialog(timYear)
                }
            )
            
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter
            
            
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in setupRecyclerView", e)
            throw e
        }
    }

    private fun setClickListeners() {
        try {
            
            
            fabAdd.setOnClickListener {
                
                addTimYear()
            }
            
            
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in setClickListeners", e)
            throw e
        }
    }

    private fun loadTimYears() {
        try {
            
            
            lifecycleScope.launch {
                try {
                    showLoading(true)
                    
                    val result = timYearRepository.getAllTimYears()
                    if (result.isSuccess) {
                        val timYears = result.getOrNull() ?: emptyList()
                        
                        
                        timYearList.clear()
                        timYearList.addAll(timYears)
                        
                        adapter.notifyDataSetChanged()
                        updateUI()
                        showLoading(false)
                        
                    } else {
                        val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                        DebugUtils.logError("Failed to load TimYears: $errorMessage")
                        showError(true, "ไม่สามารถโหลดข้อมูลปีขายได้: $errorMessage")
                        showLoading(false)
                    }
                    
                } catch (e: Exception) {
                    DebugUtils.logException(TAG, "Error loading TimYears", e)
                    showError(true, "เกิดข้อผิดพลาด: ${e.message}")
                    showLoading(false)
                }
            }
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in loadTimYears", e)
            Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addTimYear() {
        try {
            
            val intent = Intent(this, AddEditTimYearActivity::class.java)
            addTimYearLauncher.launch(intent)
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error opening Add TimYear activity", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun editTimYear(timYear: TimYear) {
        try {
            
            val intent = Intent(this, AddEditTimYearActivity::class.java)
            intent.putExtra("EDIT_MODE", true)
            intent.putExtra("TIMYEAR_ID", timYear.id)
            intent.putExtra("SALE_YEAR", timYear.saleYear)
            editTimYearLauncher.launch(intent)
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error opening Edit TimYear activity", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmDialog(timYear: TimYear) {
        try {
            
            
            AlertDialog.Builder(this)
                .setTitle("ยืนยันการลบ")
                .setMessage("คุณแน่ใจหรือไม่ว่าต้องการลบปี ${timYear.saleYear}?\\n\\nการลบปีจะไม่ส่งผลกระทบต่อข้อมูลยอดขายที่มีอยู่")
                .setPositiveButton("ลบ") { _, _ ->
                    deleteTimYear(timYear)
                }
                .setNegativeButton("ยกเลิก", null)
                .show()
                
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error showing delete dialog", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteTimYear(timYear: TimYear) {
        try {
            
            
            lifecycleScope.launch {
                try {
                    showLoading(true)
                    
                    val result = timYearRepository.deleteTimYear(timYear.id)
                    if (result.isSuccess) {
                        
                        Toast.makeText(this@TimYearManagementActivity, "ลบปี ${timYear.saleYear} เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                        
                        // Reload data
                        loadTimYears()
                        
                    } else {
                        val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                        DebugUtils.logError("Failed to delete TimYear: $errorMessage")
                        Toast.makeText(this@TimYearManagementActivity, "ไม่สามารถลบปี ${timYear.saleYear} ได้: $errorMessage", Toast.LENGTH_LONG).show()
                        showLoading(false)
                    }
                    
                } catch (e: Exception) {
                    DebugUtils.logException(TAG, "Error deleting TimYear", e)
                    Toast.makeText(this@TimYearManagementActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                    showLoading(false)
                }
            }
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in deleteTimYear", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUI() {
        try {
            if (!::tvEmpty.isInitialized || !::recyclerView.isInitialized) {
                return
            }
            
            val isEmpty = timYearList.isEmpty()
            
            tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            
            
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error updating UI", e)
        }
    }

    private fun showLoading(show: Boolean) {
        try {
            if (::progressBar.isInitialized) {
                progressBar.visibility = if (show) View.VISIBLE else View.GONE
            }
            if (show && ::tvError.isInitialized) {
                tvError.visibility = View.GONE
            }
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in showLoading", e)
        }
    }

    private fun showError(show: Boolean, message: String? = null) {
        try {
            if (::tvError.isInitialized) {
                tvError.visibility = if (show) View.VISIBLE else View.GONE
                message?.let { tvError.text = it }
            }
            
            if (show) {
                if (::tvEmpty.isInitialized) {
                    tvEmpty.visibility = View.GONE
                }
                if (::recyclerView.isInitialized) {
                    recyclerView.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in showError", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        DebugUtils.logLifecycle(TAG, "onDestroy")
    }
}