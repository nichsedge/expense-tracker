package com.sans.finance.domain.model

data class DashboardSummary(
    val netWorth: Long,
    val totalAssets: Long,
    val totalLiabilities: Long,
    val monthlyIncome: Long,
    val monthlyExpense: Long,
    val monthlyCashFlow: Long,
    val monthlySavingsRate: Float,
    val globalBudget: Long,
    val globalSpent: Long,
    val currentCurrency: String,
    val daysLeftInMonth: Int
)
