package com.sans.expensetracker.domain.usecase

import com.sans.expensetracker.domain.model.Expense
import com.sans.expensetracker.domain.repository.ExpenseRepository
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository
) {
    suspend operator fun invoke(expense: Expense): Long {
        return repository.insertExpense(expense)
    }
}
