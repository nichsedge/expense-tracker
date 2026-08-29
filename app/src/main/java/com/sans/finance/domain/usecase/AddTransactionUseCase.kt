package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.TagRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val tagRepository: TagRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(expense: Expense): Long {
        val expenseId = repository.insertExpense(expense)

        // Sync tags
        if (expense.tags.isNotEmpty()) {
            tagRepository.syncTagsForExpense(expenseId, expense.tags)
        }

        // Adjust account balances
        if (!expense.isInstallment) {
            if (expense.type == "TRANSFER") {
                accountRepository.updateBalance(expense.accountId, -expense.amount)
                expense.toAccountId?.let { toId ->
                    accountRepository.updateBalance(toId, expense.amount)
                }
            } else {
                val isIncome = expense.type == "INCOME"
                val delta = if (isIncome) expense.amount else -expense.amount
                accountRepository.updateBalance(expense.accountId, delta)
            }
        }

        return expenseId
    }
}
