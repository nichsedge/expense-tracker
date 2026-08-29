package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.InstallmentRepository
import javax.inject.Inject

class DeleteExpenseUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val installmentRepository: InstallmentRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(expense: Expense, deleteEntirePlan: Boolean = false) {
        // Reverse balance effect
        adjustBalance(expense, isReverse = true)

        if (deleteEntirePlan && expense.isInstallment) {
            expenseRepository.deleteExpense(expense)
        } else if (expense.isInstallmentPayment) {
            expenseRepository.deleteExpense(expense)
        } else {
            expenseRepository.deleteExpense(expense)
        }
    }

    private suspend fun adjustBalance(expense: Expense, isReverse: Boolean) {
        if (expense.isInstallmentPayment) {
            if (expense.status == "Paid") {
                val delta = if (isReverse) expense.amount else -expense.amount
                accountRepository.updateBalance(expense.accountId, delta)
            }
            return
        }

        if (expense.isInstallment) return // Anchor doesn't affect balance

        if (expense.type == "TRANSFER") {
            val multiplier = if (isReverse) 1 else -1
            accountRepository.updateBalance(expense.accountId, expense.amount * multiplier)
            expense.toAccountId?.let { toId ->
                accountRepository.updateBalance(toId, expense.amount * -multiplier)
            }
        } else {
            val isIncome = expense.type == "INCOME"
            val multiplier = if (isReverse) -1 else 1
            val delta = if (isIncome) expense.amount * multiplier else -expense.amount * multiplier
            accountRepository.updateBalance(expense.accountId, delta)
        }
    }
}
