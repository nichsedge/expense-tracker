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
    fun getAllTags(): Flow<List<String>>
    
    // Category management
    fun getAllCategories(): Flow<List<com.sans.expensetracker.data.local.entity.CategoryEntity>>
    suspend fun insertCategory(category: com.sans.expensetracker.data.local.entity.CategoryEntity)
    suspend fun updateCategory(category: com.sans.expensetracker.data.local.entity.CategoryEntity)
    suspend fun deleteCategory(category: com.sans.expensetracker.data.local.entity.CategoryEntity)
    
    // Tag management
    fun getAllTagEntities(): Flow<List<com.sans.expensetracker.data.local.entity.TagEntity>>
    suspend fun updateTag(tag: com.sans.expensetracker.data.local.entity.TagEntity)
    suspend fun deleteTag(tag: com.sans.expensetracker.data.local.entity.TagEntity)
    
    fun getSpendingByCategoryBetween(since: Long, until: Long): Flow<List<com.sans.expensetracker.data.local.entity.CategorySpent>>
    fun getDailySpendingBetween(since: Long, until: Long): Flow<List<com.sans.expensetracker.data.local.entity.DaySpent>>
}
