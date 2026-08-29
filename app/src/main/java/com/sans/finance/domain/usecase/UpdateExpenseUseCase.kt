package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.InstallmentRepository
import com.sans.finance.domain.repository.TagRepository
import javax.inject.Inject

class UpdateExpenseUseCase @Inject constructor(
    private val repository: ExpenseRepository,
    private val installmentRepository: InstallmentRepository,
    private val accountRepository: AccountRepository,
    private val tagRepository: TagRepository,
    private val createInstallmentPlanUseCase: CreateInstallmentPlanUseCase
) {
    suspend operator fun invoke(expense: Expense, durationMonths: Int? = null) {
        val oldExpense = repository.getExpenseById(expense.id) ?: return

        // Reverse old balance effect
        adjustBalance(oldExpense, isReverse = true)

        // Apply new balance effect
        adjustBalance(expense, isReverse = false)

        // Sync tags (if not synthetic payment which might not have its own tags stored in expense_tag_ref)
        if (!expense.isInstallmentPayment) {
            tagRepository.syncTagsForExpense(expense.id, expense.tags)
        }

        repository.updateExpense(expense)

        // Handle installment transitions for parent transactions
        if (!expense.isInstallmentPayment) {
            if (oldExpense.isInstallment && !expense.isInstallment) {
                installmentRepository.deleteInstallmentByExpenseId(expense.id)
            } else if (expense.isInstallment) {
                if (durationMonths != null && durationMonths > 0) {
                    installmentRepository.deleteInstallmentByExpenseId(expense.id)
                    createInstallmentPlanUseCase(
                        expenseId = expense.id,
                        totalAmount = expense.amount,
                        durationMonths = durationMonths,
                        startDate = expense.date
                    )
                }
            }
        }
    }

    private suspend fun adjustBalance(expense: Expense, isReverse: Boolean) {
        if (expense.isInstallmentPayment) {
            // For installment items, we only care if they are "Paid"
            if (expense.status == "Paid") {
                val delta = if (isReverse) expense.amount else -expense.amount
                accountRepository.updateBalance(expense.accountId, delta)
            }
            return
        }

        // For regular transactions
        // Note: Anchor installments (isInstallment = true) usually shouldn't affect balance if items do.
        // But we follow existing repository logic which was applying it.
        // If we want to fix double counting, we should check it here.
        if (expense.isInstallment) return // Fix double counting: anchor doesn't affect balance

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
