package com.sans.expensetracker.data.local.dao

import androidx.room.*
import com.sans.expensetracker.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Transaction
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<com.sans.expensetracker.data.local.entity.ExpenseWithTags>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE date >= :since AND date < :until ORDER BY date DESC")
    fun getExpensesBetween(since: Long, until: Long): Flow<List<com.sans.expensetracker.data.local.entity.ExpenseWithTags>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): com.sans.expensetracker.data.local.entity.ExpenseWithTags?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseTagCrossRefs(crossRefs: List<com.sans.expensetracker.data.local.entity.ExpenseTagCrossRef>)

    @Query("DELETE FROM expense_tag_ref WHERE expenseId = :expenseId")
    suspend fun deleteExpenseTagRefs(expenseId: Long)

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int

    @Query("SELECT SUM(final_price) FROM expenses WHERE date >= :since AND is_installment = 0")
    fun getTotalSpentSince(since: Long): Flow<Long?>

    @Query("SELECT SUM(final_price) FROM expenses WHERE is_installment = 0 AND date >= :since AND date < :until")
    fun getTotalSpentBetween(since: Long, until: Long): Flow<Long?>

    @Query("SELECT SUM(final_price) FROM expenses WHERE is_installment = 0")
    fun getAllTimeSpent(): Flow<Long?>
}

