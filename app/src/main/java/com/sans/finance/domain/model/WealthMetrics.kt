package com.sans.finance.domain.model

data class WealthMetrics(
    val cashAssets: Long,
    val liabilities: Long,
    val portfolioValue: Long,
    val monthlyBurn: Long,
    val runwayMonths: Double,
    val monthlyPassiveIncome: Long,
    val annualPassiveIncome: Long,
    val fiCoveragePct: Double,
    val fiStage: String,
    val fiNextStageGap: Long,
    val monthlyIncome: Long,
    val monthlyExpense: Long,
    val monthlySavings: Long,
    val currencyCode: String
)
