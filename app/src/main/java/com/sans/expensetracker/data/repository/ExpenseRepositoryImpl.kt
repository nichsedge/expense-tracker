package com.sans.expensetracker.data.repository

import com.sans.expensetracker.data.local.dao.ExpenseDao
import com.sans.expensetracker.data.local.entity.ExpenseEntity
import com.sans.expensetracker.domain.model.Expense
import com.sans.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val dao: ExpenseDao
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<List<Expense>> {
        return dao.getAllExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExpensesBetween(since: Long, until: Long): Flow<List<Expense>> {
        return dao.getExpensesBetween(since, until).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? {
        return dao.getExpenseById(id)?.toDomain()
    }

    override suspend fun insertExpense(expense: Expense): Long {
        return dao.insertExpense(expense.toEntity())
    }

    override suspend fun updateExpense(expense: Expense) {
        dao.updateExpense(expense.toEntity())
    }

    override suspend fun deleteExpense(expense: Expense) {
        dao.deleteExpense(expense.toEntity())
    }

    override fun getTotalSpentSince(since: Long): Flow<Long?> {
        return dao.getTotalSpentSince(since)
    }

    override fun getTotalSpentBetween(since: Long, until: Long): Flow<Long?> {
        return dao.getTotalSpentBetween(since, until)
    }

    override fun getAllTimeSpent(): Flow<Long?> {
        return dao.getAllTimeSpent()
    }

    // Internal mapping extension
    private fun ExpenseEntity.toDomain(): Expense {
        return Expense(
            id = id,
            date = date,
            itemName = itemName,
            amount = finalPrice,
            categoryId = categoryId,
            paymentMethod = paymentMethod,
            isRecurring = isRecurring,
            isInstallment = isInstallment,
            merchant = merchant,
            platform = platform,
            quantity = quantity
        )
    }

    private fun Expense.toEntity(): ExpenseEntity {
        return ExpenseEntity(
            id = id,
            date = date,
            itemName = itemName,
            finalPrice = amount,
            originalPrice = amount,
            categoryId = categoryId,
            paymentMethod = paymentMethod,
            isRecurring = isRecurring,
            isInstallment = isInstallment,
            merchant = merchant,
            platform = platform,
            quantity = quantity,
            status = "completed"
        )
    }
}
