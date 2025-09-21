package com.example.timsales

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.timsales.data.model.TimYear
import com.example.timsales.data.repository.TimYearRepository
import com.example.timsales.utils.DebugUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class AddEditTimYearActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AddEditTimYearActivity"
    }

    // Repository
    private val timYearRepository: TimYearRepository by lazy { TimYearRepository(this) }

    // Views
    private lateinit var etSaleYear: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    // Data
    private var isEditMode = false
    private var timYearId = 0
    private var existingSaleYear = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // ...existing code...
            
            setContentView(R.layout.activity_add_edit_timyear)
            
            // Initialize repository (lazy) - touch to ensure initialization
            val repo = timYearRepository
            // Get data from intent
            getIntentData()
            
            // Set up toolbar
            setupToolbar()
            
            // Initialize views
            initViews()
            
            // Set click listeners
            setClickListeners()
            
            // Setup UI based on mode
            setupUI()
            
            // ...existing code...
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun getIntentData() {
        try {
            isEditMode = intent.getBooleanExtra("EDIT_MODE", false)
            
            if (isEditMode) {
                timYearId = intent.getIntExtra("TIMYEAR_ID", 0)
                existingSaleYear = intent.getIntExtra("SALE_YEAR", 0)
            }
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error getting intent data", e)
        }
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(findViewById(R.id.toolbar))
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            
            val title = if (isEditMode) "แก้ไขปีขาย" else "เพิ่มปีขาย"
            supportActionBar?.title = title
            
            
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error setting up toolbar", e)
        }
    }

    private fun initViews() {
        try {
            
            
            etSaleYear = findViewById(R.id.etSaleYear)
            btnSave = findViewById(R.id.btnSave)
            btnCancel = findViewById(R.id.btnCancel)
            
            
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in initViews", e)
            throw e
        }
    }

    private fun setClickListeners() {
        try {
            
            
            btnSave.setOnClickListener {
                
                saveTimYear()
            }
            
            btnCancel.setOnClickListener {
                
                finish()
            }
            
            
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in setClickListeners", e)
            throw e
        }
    }

    private fun setupUI() {
        try {
            if (isEditMode) {
                // แก้ไข: แสดงข้อมูลเดิม
                etSaleYear.setText(existingSaleYear.toString())
                btnSave.text = "บันทึกการแก้ไข"
                
            } else {
                // เพิ่มใหม่: แสดงปีปัจจุบัน
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                etSaleYear.setText(currentYear.toString())
                btnSave.text = "เพิ่มปีขาย"
                
            }
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error setting up UI", e)
        }
    }

    private fun saveTimYear() {
        try {
            
            
            // Validate input
            val yearText = etSaleYear.text.toString().trim()
            if (!isValidInput(yearText)) {
                return
            }
            
            val saleYear = yearText.toInt()
            
            
            // Disable save button to prevent multiple clicks
            btnSave.isEnabled = false
            
            lifecycleScope.launch {
                try {
                    if (isEditMode) {
                        // แก้ไข
                        updateTimYear(saleYear)
                    } else {
                        // เพิ่มใหม่
                        createTimYear(saleYear)
                    }
                    
                } catch (e: Exception) {
                    DebugUtils.logException(TAG, "Error in save coroutine", e)
                    Toast.makeText(this@AddEditTimYearActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                    btnSave.isEnabled = true
                }
            }
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in saveTimYear", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }
    }

    private fun isValidInput(yearText: String): Boolean {
        try {
            // ตรวจสอบว่าไม่ว่าง
            if (yearText.isEmpty()) {
                Toast.makeText(this, "กรุณาระบุปีขาย", Toast.LENGTH_SHORT).show()
                etSaleYear.requestFocus()
                return false
            }
            
            // ตรวจสอบว่าเป็นตัวเลข
            val year = try {
                yearText.toInt()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "กรุณาระบุปีเป็นตัวเลข", Toast.LENGTH_SHORT).show()
                etSaleYear.requestFocus()
                return false
            }
            
            // ตรวจสอบช่วงปี (1900-2100)
            if (year < 1900 || year > 2100) {
                Toast.makeText(this, "ปีต้องอยู่ระหว่าง 1900-2100", Toast.LENGTH_SHORT).show()
                etSaleYear.requestFocus()
                return false
            }
            
            
            return true
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error in isValidInput", e)
            Toast.makeText(this, "Error validating input: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private suspend fun createTimYear(saleYear: Int) {
        try {
            
            
            val result = timYearRepository.createTimYear(saleYear)
            if (result.isSuccess) {
                val createdTimYear = result.getOrNull()
                
                
                Toast.makeText(this@AddEditTimYearActivity, "เพิ่มปี $saleYear เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                
                setResult(RESULT_OK)
                finish()
                
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                DebugUtils.logError("Failed to create TimYear: $errorMessage")
                
                Toast.makeText(this@AddEditTimYearActivity, errorMessage, Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true
            }
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error creating TimYear", e)
            Toast.makeText(this@AddEditTimYearActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
            btnSave.isEnabled = true
        }
    }

    private suspend fun updateTimYear(saleYear: Int) {
        try {
            
            
            val timYearToUpdate = TimYear(
                id = timYearId,
                saleYear = saleYear
            )
            
            val result = timYearRepository.updateTimYear(timYearToUpdate)
            if (result.isSuccess) {
                
                
                Toast.makeText(this@AddEditTimYearActivity, "แก้ไขปี $saleYear เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                
                setResult(RESULT_OK)
                finish()
                
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                DebugUtils.logError("Failed to update TimYear: $errorMessage")
                
                Toast.makeText(this@AddEditTimYearActivity, "ไม่สามารถแก้ไขได้: $errorMessage", Toast.LENGTH_LONG).show()
                btnSave.isEnabled = true
            }
            
        } catch (e: Exception) {
            DebugUtils.logException(TAG, "Error updating TimYear", e)
            Toast.makeText(this@AddEditTimYearActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
            btnSave.isEnabled = true
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