package com.sans.expensetracker.domain.preferences

import kotlinx.coroutines.flow.Flow

interface BudgetPreferences {
    fun getMonthlyBudget(): Flow<Long>
    suspend fun setMonthlyBudget(amount: Long)
}
