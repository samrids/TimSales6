package com.example.timsales

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

/**
 * แสดงกราฟแบบเต็มจอแนวนอน (ซูม/เลื่อนได้) — เปิดจากปุ่มขยายบนหน้า Dashboard
 */
class ChartFullscreenActivity : AppCompatActivity() {

    private fun c(id: Int): Int = ContextCompat.getColor(this, id)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart_fullscreen)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val title = intent.getStringExtra("TITLE") ?: "กราฟ"
        supportActionBar?.title = title

        val type = intent.getStringExtra("TYPE") ?: "bar"
        val labels = intent.getStringArrayListExtra("LABELS") ?: arrayListOf()
        val values = intent.getFloatArrayExtra("VALUES") ?: FloatArray(0)

        val bar = findViewById<BarChart>(R.id.chartBar)
        val line = findViewById<LineChart>(R.id.chartLine)

        if (type == "line") {
            bar.visibility = View.GONE
            line.visibility = View.VISIBLE
            setupLineChart(line, labels, values)
        } else {
            line.visibility = View.GONE
            bar.visibility = View.VISIBLE
            setupBarChart(bar, labels, values)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun applyCommonAxis(chart: com.github.mikephil.charting.charts.BarLineChartBase<*>, labels: List<String>) {
        val axisText = c(R.color.chart_axis_text)
        val grid = c(R.color.chart_grid)
        chart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            // เต็มจอ -> ให้ซูม/เลื่อนได้เต็มที่
            setScaleEnabled(true)
            setPinchZoom(true)
            isDragEnabled = true
            setExtraOffsets(8f, 12f, 16f, 12f)
            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                textColor = axisText
                textSize = 11f
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
                textSize = 11f
                labelRotationAngle = -45f
                valueFormatter = IndexAxisValueFormatter(labels)
            }
            setNoDataText("ไม่มีข้อมูล")
            setNoDataTextColor(axisText)
        }
    }

    private fun setupBarChart(chart: BarChart, labels: List<String>, values: FloatArray) {
        applyCommonAxis(chart, labels)
        chart.legend.isEnabled = false
        val entries = values.mapIndexed { i, v -> BarEntry(i.toFloat(), v) }
        val ds = BarDataSet(entries, "ยอดขาย (บาท)").apply {
            color = c(R.color.chart_bar)
            setDrawValues(true)
            valueTextColor = c(R.color.text_secondary)
            valueTextSize = 10f
            valueFormatter = valueLabelFormatter
            highLightColor = c(R.color.brand_primary)
        }
        chart.data = BarData(ds).apply { barWidth = 0.55f }
        chart.setFitBars(true)
        chart.animateY(600)
        chart.invalidate()
    }

    private fun setupLineChart(chart: LineChart, labels: List<String>, values: FloatArray) {
        applyCommonAxis(chart, labels)
        chart.legend.textColor = c(R.color.chart_axis_text)
        val lineColor = c(R.color.chart_line)
        val entries = values.mapIndexed { i, v -> Entry(i.toFloat(), v) }
        val ds = LineDataSet(entries, "ปริมาณ (กก.)").apply {
            color = lineColor
            setCircleColor(lineColor)
            circleRadius = 4f
            setDrawCircleHole(false)
            lineWidth = 2.5f
            mode = LineDataSet.Mode.HORIZONTAL_BEZIER
            setDrawValues(true)
            valueFormatter = valueLabelFormatter
            valueTextSize = 10f
            valueTextColor = c(R.color.text_secondary)
            setDrawFilled(true)
            fillColor = c(R.color.chart_fill)
            fillAlpha = 48
            highLightColor = c(R.color.brand_primary)
        }
        chart.data = LineData(ds)
        chart.animateX(600)
        chart.invalidate()
    }
}
