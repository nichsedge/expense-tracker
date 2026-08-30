package com.sans.finance.domain.model

enum class SafetyBufferTier(val label: String, val minMonths: Double) {
    CRITICAL("Critical (< 3 mo)", 0.0),
    FAIR("Fair (3-6 mo)", 3.0),
    STRONG("Strong (6-12 mo)", 6.0),
    ANTIFRAGILE("Antifragile (> 12 mo)", 12.0);

    companion object {
        fun fromRunway(months: Double): SafetyBufferTier {
            return when {
                months >= 12.0 -> ANTIFRAGILE
                months >= 6.0 -> STRONG
                months >= 3.0 -> FAIR
                else -> CRITICAL
            }
        }
    }
}

enum class StressTestScenarioType {
    ZERO_INCOME,       // Job loss / complete income disruption
    PARTIAL_INCOME,    // 50% income cut + freeze non-essentials
    INFLATION_SURGE,   // +25% cost-of-living shock / unexpected expenses
    MARKET_DRAWDOWN    // -30% portfolio impact
}

data class StressTestScenario(
    val type: StressTestScenarioType,
    val title: String,
    val description: String,
    val monthlyBurn: Long,
    val netMonthlyCashFlow: Long,
    val runwayMonths: Double,
    val tier: SafetyBufferTier
)

data class EmergencyFundStressTest(
    val liquidCashReserves: Long,
    val baselineMonthlyBurn: Long,
    val baselineRunwayMonths: Double,
    val baselineTier: SafetyBufferTier,
    val scenarios: List<StressTestScenario>,
    val currencyCode: String
)
