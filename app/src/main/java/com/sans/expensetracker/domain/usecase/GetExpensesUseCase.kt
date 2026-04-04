package com.sans.expensetracker.domain.usecase

import com.sans.expensetracker.domain.model.Expense
import com.sans.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetExpensesUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    operator fun invoke(): Flow<List<Expense>> {
        return repository.getAllExpenses()
    }
}
