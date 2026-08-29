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
    val daysLeftInMonth: Int,
    val spendingVelocity: Float, // 1.0 means exactly on track, > 1.0 is overspending
    val categoryBudgetProgress: List<CategoryBudgetProgress> = emptyList()
)

data class CategoryBudgetProgress(
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String?,
    val budgetAmount: Long,
    val spentAmount: Long,
    val progress: Float
)
