package com.example.timsales.data.model

/**
 * จุดข้อมูลยอดขายรายเดือนภายในรอบการขาย (สำหรับกราฟ)
 */
data class MonthlyPoint(
    val yearMonth: String,   // YYYY-MM เช่น "2026-02"
    val label: String,       // ป้ายแกน X เช่น "ก.พ. 69" หรือ "ก.พ. 26"
    val revenue: Double = 0.0,
    val kg: Double = 0.0,
    val count: Int = 0
)

/**
 * ข้อมูลรวมของ dashboard สำหรับ 1 รอบการขาย
 */
data class SeasonDashboard(
    val seasonCode: String,
    val statistics: SalesStatistics,
    val monthly: List<MonthlyPoint>
)
