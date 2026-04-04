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

    @Query("""
        SELECT categoryId, categoryName, categoryIcon, SUM(amount) as totalAmount
        FROM (
            SELECT c.id as categoryId, c.name as categoryName, c.icon as categoryIcon, SUM(e.final_price) as amount
            FROM expenses e
            JOIN categories c ON e.category_id = c.id
            WHERE e.date >= :since AND e.date < :until AND e.is_installment = 0
            GROUP BY c.id
            UNION ALL
            SELECT c.id as categoryId, c.name as categoryName, c.icon as categoryIcon, SUM(ii.amount) as amount
            FROM installment_items ii
            JOIN installments i ON ii.installment_id = i.id
            JOIN expenses e ON i.expense_id = e.id
            JOIN categories c ON e.category_id = c.id
            WHERE ii.due_date >= :since AND ii.due_date < :until AND ii.status = 'Paid'
            GROUP BY c.id
        ) sub
        GROUP BY categoryId
    """)
    fun getSpendingByCategoryBetween(since: Long, until: Long): Flow<List<com.sans.expensetracker.data.local.entity.CategorySpent>>

    @Query("""
        SELECT day, SUM(amount) as amount
        FROM (
            SELECT (date / 86400000) * 86400000 as day, SUM(final_price) as amount
            FROM expenses
            WHERE date >= :since AND date < :until AND is_installment = 0
            GROUP BY day
            UNION ALL
            SELECT (due_date / 86400000) * 86400000 as day, SUM(amount) as amount
            FROM installment_items
            WHERE due_date >= :since AND due_date < :until AND status = 'Paid'
            GROUP BY day
        ) sub
        GROUP BY day
        ORDER BY day ASC
    """)
    fun getDailySpendingBetween(since: Long, until: Long): Flow<List<com.sans.expensetracker.data.local.entity.DaySpent>>
}

