package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HoldingYield(
    val code: String,
    val name: String,
    val assetClass: String,
    val currentValueInBase: Double,
    val annualYieldRate: Double,        // e.g. 0.065 for 6.5%
    val estimatedAnnualIncomeInBase: Double,
    val monthlyIncomeInBase: Double
)

@Serializable
data class DividendYieldSummary(
    val totalAnnualIncomeInBase: Double,
    val totalMonthlyIncomeInBase: Double,
    val portfolioYieldOnCost: Double,    // weighted yield %
    val expenseCoveragePercentage: Double, // % of annual living expenses covered
    val holdingsWithYield: List<HoldingYield>,
    val baseCurrency: String
)
