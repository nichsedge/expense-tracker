package com.sans.finance.data.ai

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyReviewInsight(
    val title: String,
    val why: String,
    val action: String,
    val severity: String = "INFO" // INFO | WARN | CRITICAL
)

data class MonthlyReviewResult(
    val headline: String,
    val insights: List<MonthlyReviewInsight>,
    val rawText: String? = null
)

@Serializable
data class PortfolioAnalysisInsight(
    val title: String,
    val observation: String,
    val suggestion: String,
    val importance: String = "MEDIUM" // LOW | MEDIUM | HIGH
)

data class PortfolioAnalysisResult(
    val summary: String,
    val insights: List<PortfolioAnalysisInsight>,
    val rawText: String? = null
)

data class PortfolioAnalysisInput(
    val dateLabel: String,
    val currency: String,
    val totalValue: Double,
    val assetAllocation: List<Pair<String, Double>>, // AssetClass to Percentage
    val healthStatus: List<String>, // "Equities: Underweight", etc.
    val xirr: Double?,
    val goals: List<String>,
    val notes: String = ""
)

