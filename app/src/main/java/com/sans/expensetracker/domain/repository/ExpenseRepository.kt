package com.sans.expensetracker.domain.repository

import com.sans.expensetracker.domain.model.Expense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun getAllExpenses(): Flow<List<Expense>>
    fun getExpensesBetween(since: Long, until: Long): Flow<List<Expense>>
    suspend fun getExpenseById(id: Long): Expense?
    suspend fun insertExpense(expense: Expense): Long
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
    fun getTotalSpentSince(since: Long): Flow<Long?>
    fun getTotalSpentBetween(since: Long, until: Long): Flow<Long?>
    fun getAllTimeSpent(): Flow<Long?>
}
