package com.example.timsales

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import com.example.timsales.base.BaseNetworkActivity
import androidx.lifecycle.lifecycleScope
import com.example.timsales.data.model.ApiResult
import com.example.timsales.data.model.SalesRecordRequest
import com.example.timsales.data.repository.SalesRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EditSalesActivity : BaseNetworkActivity() {

    private val repository: SalesRepository by lazy { SalesRepository() }
    
    // Views
    private lateinit var btnSelectDate: Button
    private lateinit var etWeight: TextInputEditText
    private lateinit var etUnitPrice: TextInputEditText
    private lateinit var tvCalculatedAmount: TextView
    private lateinit var btnUpdate: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    private var selectedDate: String = ""
    private val calendar = Calendar.getInstance()
    private var salesId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_sales)
        
    // repository is lazy-initialized
        
        // Initialize views
        initViews()
        
        // Get data from intent
        getIntentData()
        
        // Set click listeners
        setClickListeners()
        
        // Setup text change listeners for calculation
        setupCalculationListeners()
    }

    private fun initViews() {
        btnSelectDate = findViewById(R.id.btnSelectDate)
        etWeight = findViewById(R.id.etWeight)
        etUnitPrice = findViewById(R.id.etUnitPrice)
        tvCalculatedAmount = findViewById(R.id.tvCalculatedAmount)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnCancel = findViewById(R.id.btnCancel)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
    }

    private fun getIntentData() {
        salesId = intent.getIntExtra("SALES_ID", 0)
        selectedDate = intent.getStringExtra("SALE_DATE") ?: ""
        val weight = intent.getDoubleExtra("WEIGHT", 0.0)
        val unitPrice = intent.getDoubleExtra("UNIT_PRICE", 0.0)

        // Populate fields with existing data
        updateDateButton()
        etWeight.setText(weight.toString())
        etUnitPrice.setText(unitPrice.toString())
        calculateAmount()
    }

    private fun setClickListeners() {
        btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        btnUpdate.setOnClickListener {
            updateRecord()
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun setupCalculationListeners() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateAmount()
            }
        }

        etWeight.addTextChangedListener(textWatcher)
        etUnitPrice.addTextChangedListener(textWatcher)
    }

    private fun showDatePicker() {
        // Parse current date
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.parse(selectedDate)
            if (currentDate != null) {
                calendar.time = currentDate
            }
        } catch (e: Exception) {
            // Use current date if parsing fails
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                selectedDate = dateFormat.format(calendar.time)
                updateDateButton()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateButton() {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(selectedDate)
            val formattedDate = outputFormat.format(date!!)
            btnSelectDate.text = "📅 $formattedDate"
        } catch (e: Exception) {
            btnSelectDate.text = "📅 $selectedDate"
        }
    }

    private fun calculateAmount() {
        try {
            val weight = etWeight.text.toString().toDoubleOrNull() ?: 0.0
            val unitPrice = etUnitPrice.text.toString().toDoubleOrNull() ?: 0.0
            val amount = weight * unitPrice
            
            tvCalculatedAmount.text = String.format("%.2f บาท", amount)
        } catch (e: Exception) {
            tvCalculatedAmount.text = "0.00 บาท"
        }
    }

    private fun updateRecord() {
        // ตรวจสอบการเชื่อมต่อ Internet ก่อน
        if (!isNetworkAvailableForApi()) {
            return
        }
        
        // Validate input
        val validationResult = validateInput()
        if (!validationResult.first) {
            showError(true, validationResult.second)
            return
        }

        val weight = etWeight.text.toString().toDouble()
        val unitPrice = etUnitPrice.text.toString().toDouble()

        val salesRecord = SalesRecordRequest(
            saleDate = selectedDate,
            kg = weight,
            unitPrice = unitPrice
        )

        lifecycleScope.launch {
            showLoading(true)
            
            when (val result = repository.updateSalesRecord(salesId, salesRecord)) {
                is ApiResult.Success -> {
                    showLoading(false)
                    Toast.makeText(
                        this@EditSalesActivity,
                        "อัปเดตข้อมูลเรียบร้อยแล้ว",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
                is ApiResult.Error -> {
                    showLoading(false)
                    showError(true, "เกิดข้อผิดพลาด: ${result.exception.message}")
                }
                else -> {
                    showLoading(false)
                }
            }
        }
    }

    private fun validateInput(): Pair<Boolean, String> {
        if (selectedDate.isEmpty()) {
            return Pair(false, "กรุณาเลือกวันที่")
        }

        if (salesId == 0) {
            return Pair(false, "ข้อมูลการขายไม่ถูกต้อง")
        }

        val weightText = etWeight.text.toString().trim()
        if (weightText.isEmpty()) {
            return Pair(false, "กรุณาใส่น้ำหนัก")
        }

        val weight = weightText.toDoubleOrNull()
        if (weight == null || weight <= 0) {
            return Pair(false, "น้ำหนักต้องเป็นตัวเลขที่มากกว่า 0")
        }

        val unitPriceText = etUnitPrice.text.toString().trim()
        if (unitPriceText.isEmpty()) {
            return Pair(false, "กรุณาใส่ราคาต่อหน่วย")
        }

        val unitPrice = unitPriceText.toDoubleOrNull()
        if (unitPrice == null || unitPrice <= 0) {
            return Pair(false, "ราคาต่อหน่วยต้องเป็นตัวเลขที่มากกว่า 0")
        }

        return Pair(true, "")
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnUpdate.isEnabled = !show
        btnCancel.isEnabled = !show
    }

    private fun showError(show: Boolean, message: String = "") {
        tvError.visibility = if (show) View.VISIBLE else View.GONE
        if (show && message.isNotEmpty()) {
            tvError.text = message
        }
    }
    
    // ==================== Network Methods ====================
    
    /**
     * เรียกใหม่หลังจาก network กลับมา
     */
    override fun onRetryAfterNetworkRestore() {
        // ไม่ต้องทำอะไรพิเศษ เพียงแค่ซ่อน error message
        showError(false)
    }
    
    /**
     * เมื่อ network ไม่พร้อมใช้งาน
     */
    override fun onNetworkUnavailable() {
        super.onNetworkUnavailable()
        
        // แสดงข้อความ error ในหน้าจอ
        runOnUiThread {
            showError(true, "ไม่มีการเชื่อมต่อ Internet\nไม่สามารถอัปเดตข้อมูลได้")
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