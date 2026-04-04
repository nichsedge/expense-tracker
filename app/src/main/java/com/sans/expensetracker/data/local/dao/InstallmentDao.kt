package com.sans.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.sans.expensetracker.data.local.entity.InstallmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstallmentDao {
    @Query("SELECT * FROM installments ORDER BY created_at DESC")
    fun getAllInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE status = 'Active' ORDER BY next_due_date ASC")
    fun getActiveInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE expense_id = :expenseId")
    suspend fun getInstallmentByExpenseId(expenseId: Long): InstallmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: InstallmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallmentItem(item: com.sans.expensetracker.data.local.entity.InstallmentItemEntity)

    @Query("SELECT * FROM installment_items WHERE installment_id = :installmentId")
    fun getItemsByInstallmentId(installmentId: Long): Flow<List<com.sans.expensetracker.data.local.entity.InstallmentItemEntity>>

    @Query("UPDATE installment_items SET status = :status WHERE id = :itemId")
    suspend fun updateInstallmentItemStatus(itemId: Long, status: String)

    @Query("SELECT * FROM installment_items WHERE id = :id")
    suspend fun getInstallmentItemById(id: Long): com.sans.expensetracker.data.local.entity.InstallmentItemEntity?

    @Query("SELECT * FROM installments WHERE id = :id")
    suspend fun getInstallmentById(id: Long): InstallmentEntity?

    @Query("SELECT SUM(amount) FROM installment_items WHERE installment_id = :installmentId AND status = 'Paid'")
    suspend fun getPaidAmountForInstallment(installmentId: Long): Long?

    @Query("SELECT SUM(amount) FROM installment_items WHERE status = 'Paid' AND due_date >= :since AND due_date < :until")
    fun getTotalPaidAmountBetween(since: Long, until: Long): Flow<Long?>

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Query("SELECT COUNT(*) FROM installments")
    suspend fun getInstallmentCount(): Int
}
