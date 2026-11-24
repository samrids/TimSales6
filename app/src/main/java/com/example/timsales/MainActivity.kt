package com.example.timsales

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.timsales.base.BaseNetworkActivity
import com.example.timsales.data.model.ApiResult
import com.example.timsales.data.model.SeasonDashboard
import com.example.timsales.data.model.TimYear
import com.example.timsales.data.repository.SalesRepository
import com.example.timsales.data.repository.TimYearRepository
import com.example.timsales.utils.DebugUtils
import com.example.timsales.debug.ConfigurationChecker
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch

class MainActivity : BaseNetworkActivity() {

    private val repository: SalesRepository by lazy { SalesRepository() }
    private val timYearRepository: TimYearRepository by lazy { TimYearRepository(this) }

    private var seasons: List<TimYear> = emptyList()
    private var currentSeason: TimYear? = null
    private var selectedSeasonCode: String? = null // จดจำรอบที่เลือกไว้ข้าม onResume
    private var seasonsLoaded = false // โหลด spinner รอบแล้วหรือยัง (กันโหลดซ้ำ/กระพริบ)
    private var seasonsLoading = false // กำลังโหลดรอบอยู่ (กัน network callback เรียกซ้ำ)
    private var loadedSeasonCode: String? = null // รอบที่โหลดกราฟไปแล้ว (กัน spinner ยิง callback ซ้ำ)
    private var reloadSeasonsOnResume = false // ให้รีโหลดรายชื่อรอบตอนกลับจากหน้าจัดการรอบ
    private var currentDashboard: SeasonDashboard? = null // dashboard ล่าสุด (ใช้ตอนกดขยายกราฟ)

    // Views
    private lateinit var spinnerSeason: Spinner
    private lateinit var tvSeasonRange: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvTotalKg: TextView
    private lateinit var tvAveragePrice: TextView
    private lateinit var tvCountOfSales: TextView
    private lateinit var chartRevenue: BarChart
    private lateinit var chartVolume: LineChart
    private lateinit var btnExpandRevenue: ImageButton
    private lateinit var btnExpandVolume: ImageButton
    private lateinit var btnAddSales: Button
    private lateinit var btnViewSalesList: Button
    private lateinit var btnRefresh: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val configValid = ConfigurationChecker.checkConfiguration()
            if (!configValid) {
                DebugUtils.logError("Configuration is invalid - showing debug info")
                Toast.makeText(this, "Configuration Error - Check logs", Toast.LENGTH_LONG).show()
            }

            setContentView(R.layout.activity_main)

            setSupportActionBar(findViewById(R.id.toolbar))
            supportActionBar?.title = "ยอดขายสินค้าเกษตร"

            // Touch repo (lazy) to ensure initialization
            val repo = timYearRepository

            initViews()
            setupChartsStyle()
            setClickListeners()

            if (configValid) {
                loadSeasons()
            } else {
                showError(true, "Configuration Error - Check AppConfig.kt")
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initViews() {
        spinnerSeason = findViewById(R.id.spinnerSeason)
        tvSeasonRange = findViewById(R.id.tvSeasonRange)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvTotalKg = findViewById(R.id.tvTotalKg)
        tvAveragePrice = findViewById(R.id.tvAveragePrice)
        tvCountOfSales = findViewById(R.id.tvCountOfSales)
        chartRevenue = findViewById(R.id.chartRevenue)
        chartVolume = findViewById(R.id.chartVolume)
        btnExpandRevenue = findViewById(R.id.btnExpandRevenue)
        btnExpandVolume = findViewById(R.id.btnExpandVolume)
        btnAddSales = findViewById(R.id.btnAddSales)
        btnViewSalesList = findViewById(R.id.btnViewSalesList)
        btnRefresh = findViewById(R.id.btnRefresh)
        progressBar = findViewById(R.id.progressBar)
        tvError = findViewById(R.id.tvError)
    }

    private fun setClickListeners() {
        btnAddSales.setOnClickListener {
            try {
                startActivity(Intent(this, AddSalesActivity::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Error opening Add Sales: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnViewSalesList.setOnClickListener {
            try {
                val intent = Intent(this, SalesListActivity::class.java)
                currentSeason?.let { season ->
                    intent.putExtra("SEASON_CODE", season.resolveSeasonCode())
                    intent.putExtra("SEASON_YEAR", season.saleYear)
                    intent.putExtra("SEASON_START", season.startDate)
                    intent.putExtra("SEASON_END", season.endDate)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Error opening Sales List: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnRefresh.setOnClickListener { loadSeasons() }

        btnExpandRevenue.setOnClickListener {
            openChartFullscreen("bar", "ยอดขายรายเดือน (บาท)")
        }
        btnExpandVolume.setOnClickListener {
            openChartFullscreen("line", "ปริมาณรายเดือน (กก.)")
        }
    }

    /** เปิดกราฟแบบเต็มจอแนวนอน (ซูม/เลื่อนได้) */
    private fun openChartFullscreen(type: String, title: String) {
        val dash = currentDashboard
        if (dash == null || dash.monthly.isEmpty()) {
            Toast.makeText(this, "ยังไม่มีข้อมูลกราฟ", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = ArrayList(dash.monthly.map { it.label })
        val values = if (type == "line") {
            dash.monthly.map { it.kg.toFloat() }.toFloatArray()
        } else {
            dash.monthly.map { it.revenue.toFloat() }.toFloatArray()
        }
        val intent = Intent(this, ChartFullscreenActivity::class.java).apply {
            putExtra("TITLE", title)
            putExtra("TYPE", type)
            putStringArrayListExtra("LABELS", labels)
            putExtra("VALUES", values)
        }
        startActivity(intent)
    }

    // ==================== Season loading ====================

    private fun loadSeasons() {
        // กันเรียกซ้ำระหว่างกำลังโหลด (network callback ยิงถี่ทำให้กระพริบ)
        if (seasonsLoading) return
        if (!isNetworkAvailableForApi()) return

        seasonsLoading = true
        lifecycleScope.launch {
            try {
                showLoading(true)

                // ดึงรอบทั้งหมด ถ้ายังไม่มีให้สร้างรอบปัจจุบันอัตโนมัติ
                var result = timYearRepository.getAllTimYears()
                var list = result.getOrNull() ?: emptyList()
                if (result.isSuccess && list.isEmpty()) {
                    timYearRepository.initializeCurrentYearIfNeeded()
                    list = timYearRepository.getAllTimYears().getOrNull() ?: emptyList()
                }

                if (!result.isSuccess && list.isEmpty()) {
                    showLoading(false)
                    showError(true, "ไม่สามารถโหลดรอบการขายได้")
                    return@launch
                }

                seasons = list // เรียงจากใหม่ไปเก่าแล้วใน repository
                setupSpinner()
            } catch (e: Exception) {
                showLoading(false)
                showError(true, e.message)
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                seasonsLoading = false
            }
        }
    }

    private fun setupSpinner() {
        val labels = seasons.map { it.getSeasonLabel() }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSeason.adapter = adapter

        // เลือกรอบเดิมถ้ามี ไม่งั้นเลือกรอบล่าสุด (index 0)
        val targetIndex = selectedSeasonCode
            ?.let { code -> seasons.indexOfFirst { it.resolveSeasonCode() == code } }
            ?.takeIf { it >= 0 } ?: 0

        spinnerSeason.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in seasons.indices) {
                    currentSeason = seasons[position]
                    selectedSeasonCode = seasons[position].resolveSeasonCode()
                    onSeasonSelected(seasons[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        if (seasons.isNotEmpty()) {
            seasonsLoaded = true
            spinnerSeason.setSelection(targetIndex)
        } else {
            showLoading(false)
        }
    }

    private fun onSeasonSelected(season: TimYear) {
        val code = season.resolveSeasonCode()
        supportActionBar?.title = "ยอดขาย ${season.getSeasonLabel()}"
        tvSeasonRange.text = "ช่วงรอบ: ${season.getEffectiveStartDate()} ถึง ${season.getEffectiveEndDate()}"
        // กัน Spinner ยิง callback ซ้ำสำหรับรอบเดิม (สาเหตุกราฟกระพริบตอนเปิดหน้า)
        if (code == loadedSeasonCode) return
        loadedSeasonCode = code
        // เปลี่ยนรอบด้วยตัวเอง -> ให้กราฟ animate ได้
        loadDashboard(season, animate = true)
    }

    // ==================== Dashboard loading ====================

    private fun loadDashboard(season: TimYear, animate: Boolean) {
        if (!isNetworkAvailableForApi()) return

        lifecycleScope.launch {
            try {
                // โชว์ตัวโหลดเฉพาะตอนโหลดจริง/เปลี่ยนรอบ — ตอน refresh เงียบไม่ต้องโชว์ (กันกระพริบ)
                if (animate) showLoading(true)
                when (val result = repository.getSeasonDashboard(season)) {
                    is ApiResult.Success -> {
                        showLoading(false)
                        showError(false)
                        renderDashboard(result.data, animate)
                    }
                    is ApiResult.Error -> {
                        showLoading(false)
                        showError(true, result.exception.message)
                        DebugUtils.logError("Dashboard error: ${result.exception.message}", result.exception)
                    }
                    else -> showLoading(false)
                }
            } catch (e: Exception) {
                showLoading(false)
                showError(true, e.message)
            }
        }
    }

    private fun renderDashboard(dashboard: SeasonDashboard, animate: Boolean) {
        currentDashboard = dashboard
        val stats = dashboard.statistics
        tvTotalAmount.text = stats.getFormattedTotalAmount()
        tvTotalKg.text = stats.getFormattedTotalKg()
        tvAveragePrice.text = stats.getFormattedAveragePrice()
        tvCountOfSales.text = stats.getFormattedRecordCount()

        updateRevenueChart(dashboard, animate)
        updateVolumeChart(dashboard, animate)
    }

    // ==================== Charts ====================

    private fun c(id: Int): Int = ContextCompat.getColor(this, id)

    /** ฟอร์แมตตัวเลขใหญ่บนแกน เช่น 150,000 -> 150k */
    private val compactFormatter = object : ValueFormatter() {
        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
            return when {
                value >= 1_000_000f -> String.format("%.1fM", value / 1_000_000f)
                value >= 1_000f -> String.format("%.0fk", value / 1_000f)
                else -> String.format("%.0f", value)
            }
        }
    }

    /** ป้ายค่าบนแท่ง/จุด: ใส่จุลภาคเมื่อเกินพัน เช่น 16,928 (ค่า 0 ไม่แสดง) */
    private val valueLabelFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return if (value <= 0f) "" else String.format("%,.0f", value)
        }
    }

    private fun setupChartsStyle() {
        val axisText = c(R.color.chart_axis_text)
        val grid = c(R.color.chart_grid)

        chartRevenue.apply {
            description.isEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setExtraOffsets(4f, 8f, 8f, 8f)
            legend.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                textColor = axisText
                textSize = 10f
                gridColor = grid
                setDrawAxisLine(false)
                valueFormatter = compactFormatter
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f
                textColor = axisText
                textSize = 9f
                labelRotationAngle = -45f
            }
            setNoDataText("ไม่มีข้อมูลในรอบนี้")
            setNoDataTextColor(axisText)
        }

        chartVolume.apply {
            description.isEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            setExtraOffsets(4f, 8f, 8f, 8f)
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.TOP
                textColor = axisText
            }
            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                textColor = axisText
                textSize = 10f
                gridColor = grid
                setDrawAxisLine(false)
                valueFormatter = compactFormatter
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                granularity = 1f
                textColor = axisText
                textSize = 9f
                labelRotationAngle = -45f
            }
            setNoDataText("ไม่มีข้อมูลในรอบนี้")
            setNoDataTextColor(axisText)
        }
    }

    private fun updateRevenueChart(dashboard: SeasonDashboard, animate: Boolean) {
        val labels = dashboard.monthly.map { it.label }
        val entries = dashboard.monthly.mapIndexed { i, p ->
            BarEntry(i.toFloat(), p.revenue.toFloat())
        }
        val dataSet = BarDataSet(entries, "ยอดขาย (บาท)").apply {
            color = c(R.color.chart_bar)
            setDrawValues(true)
            valueFormatter = valueLabelFormatter
            valueTextSize = 8.5f
            valueTextColor = c(R.color.text_secondary)
            highLightColor = c(R.color.brand_primary)
        }
        chartRevenue.apply {
            data = BarData(dataSet).apply { barWidth = 0.55f }
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            setFitBars(true)
            if (animate) animateY(700)
            invalidate()
        }
    }

    private fun updateVolumeChart(dashboard: SeasonDashboard, animate: Boolean) {
        val labels = dashboard.monthly.map { it.label }
        val entries = dashboard.monthly.mapIndexed { i, p ->
            Entry(i.toFloat(), p.kg.toFloat())
        }
        val lineColor = c(R.color.chart_line)
        val dataSet = LineDataSet(entries, "ปริมาณ (กก.)").apply {
            color = lineColor
            setCircleColor(lineColor)
            circleRadius = 3.5f
            setDrawCircleHole(false)
            lineWidth = 2.5f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawValues(true)
            valueFormatter = valueLabelFormatter
            valueTextSize = 8.5f
            valueTextColor = c(R.color.text_secondary)
            setDrawFilled(true)
            fillColor = c(R.color.chart_fill)
            fillAlpha = 48
            highLightColor = c(R.color.brand_primary)
        }
        chartVolume.apply {
            data = LineData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            if (animate) animateX(700)
            invalidate()
        }
    }

    // ==================== UI helpers ====================

    private fun showLoading(show: Boolean) {
        if (::progressBar.isInitialized) {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun showError(show: Boolean, message: String? = null) {
        if (::tvError.isInitialized) {
            tvError.visibility = if (show) View.VISIBLE else View.GONE
            message?.let { tvError.text = it }
        }
    }

    // ==================== Menu ====================

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                try {
                    reloadSeasonsOnResume = true // อาจมีการเพิ่ม/ลบรอบ -> รีโหลด spinner ตอนกลับมา
                    startActivity(Intent(this, TimYearManagementActivity::class.java))
                } catch (e: Exception) {
                    Toast.makeText(this, "Error opening settings: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ==================== Lifecycle / network ====================

    override fun onResume() {
        super.onResume()
        try {
            // กลับจากหน้าจัดการรอบ -> รีโหลดรายชื่อรอบใหม่ (เผื่อเพิ่ม/ลบ)
            if (reloadSeasonsOnResume) {
                reloadSeasonsOnResume = false
                seasonsLoaded = false
                loadedSeasonCode = null
                loadSeasons()
                return
            }
            // ครั้งแรก onCreate โหลด spinner ให้อยู่แล้ว — onResume แค่ refresh ข้อมูลรอบเดิม
            // (ไม่สร้าง spinner ใหม่ + ไม่ animate เพื่อไม่ให้กราฟกระพริบ)
            val season = currentSeason
            if (seasonsLoaded && season != null) {
                loadDashboard(season, animate = false)
            }
        } catch (e: Exception) {
        }
    }

    override fun onRetryAfterNetworkRestore() {
        try {
            // โหลดรอบเฉพาะตอนที่ยังไม่เคยโหลดสำเร็จ (network callback ยิงถี่ ไม่ต้อง reload ทุกครั้ง)
            if (!seasonsLoaded) loadSeasons()
        } catch (e: Exception) {
            DebugUtils.logError("Error in onRetryAfterNetworkRestore", e)
        }
    }

    override fun onNetworkAvailable() {
        super.onNetworkAvailable()
        runOnUiThread { showError(false) }
    }

    override fun onNetworkUnavailable() {
        super.onNetworkUnavailable()
        runOnUiThread {
            showError(true, "ไม่มีการเชื่อมต่อ Internet\nกรุณาตรวจสอบการเชื่อมต่อของคุณ")
            showLoading(false)
        }
    }
}
