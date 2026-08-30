package com.sans.finance.domain.model

enum class MomentumTrend(val label: String) {
    ACCELERATING("Accelerating"),
    STEADY("Steady"),
    DECELERATING("Decelerating")
}

data class MonthlySavingsPoint(
    val monthLabel: String,
    val yearMonthTimestamp: Long,
    val income: Long,
    val expense: Long,
    val savings: Long,
    val savingsRatePct: Double
)

data class SavingsRateVelocitySummary(
    val currentMonthSavingsRatePct: Double,
    val threeMonthAvgSavingsRatePct: Double,
    val sixMonthAvgSavingsRatePct: Double,
    val monthlyNetWorthVelocity: Long, // delta net worth per month
    val momentumTrend: MomentumTrend,
    val history: List<MonthlySavingsPoint>,
    val currencyCode: String
)
