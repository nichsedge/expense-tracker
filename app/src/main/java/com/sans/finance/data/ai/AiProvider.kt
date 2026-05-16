package com.sans.finance.data.ai

interface AiProvider {
    suspend fun generateMonthlyReview(input: MonthlyReviewInput): MonthlyReviewResult
    suspend fun generatePortfolioAnalysis(input: PortfolioAnalysisInput): PortfolioAnalysisResult
}

data class MonthlyReviewInput(
    val monthLabel: String,
    val baseCurrency: String,
    val income: Long,
    val expense: Long,
    val topCategories: List<Pair<String, Long>>,
    val notes: String
)

