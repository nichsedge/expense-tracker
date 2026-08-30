package com.sans.finance.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class BenchmarkType(val displayName: String, val annualCagr: Double, val description: String) {
    SP500("S&P 500", 0.105, "US Large-Cap Equity (10.5% historical CAGR)"),
    IHSG("IDX Composite (IHSG)", 0.078, "Indonesian Equity Benchmark (7.8% historical CAGR)"),
    GOLD("Gold (XAU)", 0.082, "Global Safe Haven & Inflation Hedge (8.2% CAGR)"),
    DEPOSIT("Risk-Free Deposit", 0.0575, "Bank Indonesia 7-Day Reverse Repo Rate (5.75%)")
}

@Serializable
data class BenchmarkPoint(
    val dateEpochMs: Long,
    val portfolioIndex: Double,  // Base 100 on Day 0
    val benchmarkIndex: Double,  // Base 100 on Day 0
    val portfolioValueInBase: Double
)

@Serializable
data class PortfolioBenchmarkComparison(
    val benchmarkType: BenchmarkType,
    val portfolioTotalReturnPct: Double,
    val benchmarkTotalReturnPct: Double,
    val alphaPct: Double, // Portfolio Return - Benchmark Return
    val isOutperforming: Boolean,
    val durationDays: Int,
    val trajectory: List<BenchmarkPoint>
)
