package com.sans.finance.domain.model

data class FinancialFreedomStats(
    val yearsOfCover: Double,
    val freedomScore: Float,
    val totalAssets: Long,
    val annualExpense: Long,
    val currencyCode: String
)
