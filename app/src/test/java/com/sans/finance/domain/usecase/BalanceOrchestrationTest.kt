package com.sans.finance.domain.usecase

import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.InstallmentRepository
import com.sans.finance.domain.repository.TagRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class BalanceOrchestrationTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var installmentRepository: InstallmentRepository
    private lateinit var tagRepository: TagRepository
    private lateinit var createInstallmentPlanUseCase: CreateInstallmentPlanUseCase

    private lateinit var addTransactionUseCase: AddTransactionUseCase
    private lateinit var updateExpenseUseCase: UpdateExpenseUseCase
    private lateinit var deleteExpenseUseCase: DeleteExpenseUseCase

    private val cashAccountId = 1L
    private val bankAccountId = 2L

    @Before
    fun setUp() {
        expenseRepository = mockk(relaxed = true)
        accountRepository = mockk(relaxed = true)
        installmentRepository = mockk(relaxed = true)
        tagRepository = mockk(relaxed = true)
        createInstallmentPlanUseCase = mockk(relaxed = true)

        addTransactionUseCase = AddTransactionUseCase(
            expenseRepository,
            tagRepository,
            accountRepository
        )
        updateExpenseUseCase = UpdateExpenseUseCase(
            expenseRepository,
            installmentRepository,
            accountRepository,
            tagRepository,
            createInstallmentPlanUseCase
        )
        deleteExpenseUseCase = DeleteExpenseUseCase(
            expenseRepository,
            installmentRepository,
            accountRepository
        )
    }

    private fun expense(
        id: Long = 0,
        amount: Long,
        type: String = "EXPENSE",
        accountId: Long = cashAccountId,
        toAccountId: Long? = null
    ) = Expense(
        id = id,
        date = 1000L,
        title = "Tx",
        amount = amount,
        categoryId = 1,
        type = type,
        accountId = accountId,
        toAccountId = toAccountId
    )

    @Test
    fun `AddTransactionUseCase updates balance correctly for expense`() = runBlocking {
        val tx = expense(amount = 10_000L)
        addTransactionUseCase(tx)

        coVerify { accountRepository.updateBalance(cashAccountId, -10_000L) }
    }

    @Test
    fun `AddTransactionUseCase updates balance correctly for income`() = runBlocking {
        val tx = expense(amount = 20_000L, type = "INCOME")
        addTransactionUseCase(tx)

        coVerify { accountRepository.updateBalance(cashAccountId, 20_000L) }
    }

    @Test
    fun `AddTransactionUseCase updates both balances for transfer`() = runBlocking {
        val tx = expense(amount = 5_000L, type = "TRANSFER", toAccountId = bankAccountId)
        addTransactionUseCase(tx)

        coVerify { accountRepository.updateBalance(cashAccountId, -5_000L) }
        coVerify { accountRepository.updateBalance(bankAccountId, 5_000L) }
    }

    @Test
    fun `UpdateExpenseUseCase reverses old balance and applies new balance`() = runBlocking {
        val oldTx = expense(id = 1, amount = 10_000L)
        val newTx = oldTx.copy(amount = 15_000L)

        coEvery { expenseRepository.getExpenseById(1) } returns oldTx

        updateExpenseUseCase(newTx)

        // Reverse old: -(-10k) = +10k
        coVerify { accountRepository.updateBalance(cashAccountId, 10_000L) }
        // Apply new: -15k
        coVerify { accountRepository.updateBalance(cashAccountId, -15_000L) }
    }

    @Test
    fun `UpdateExpenseUseCase handled account change correctly`() = runBlocking {
        val oldTx = expense(id = 1, amount = 10_000L, accountId = cashAccountId)
        val newTx = oldTx.copy(accountId = bankAccountId)

        coEvery { expenseRepository.getExpenseById(1) } returns oldTx

        updateExpenseUseCase(newTx)

        // Reverse old on Cash: +10k
        coVerify { accountRepository.updateBalance(cashAccountId, 10_000L) }
        // Apply new on Bank: -10k
        coVerify { accountRepository.updateBalance(bankAccountId, -10_000L) }
    }

    @Test
    fun `DeleteExpenseUseCase restores balance correctly`() = runBlocking {
        val tx = expense(id = 1, amount = 10_000L)

        deleteExpenseUseCase(tx)

        // Restore: +10k
        coVerify { accountRepository.updateBalance(cashAccountId, 10_000L) }
    }

    @Test
    fun `DeleteExpenseUseCase for paid installment payment restores balance`() = runBlocking {
        val installmentPayment = expense(id = 100_000_001L, amount = 5_000L).copy(
            isInstallmentPayment = true,
            status = "Paid"
        )

        deleteExpenseUseCase(installmentPayment)

        coVerify { accountRepository.updateBalance(cashAccountId, 5_000L) }
    }

    @Test
    fun `DeleteExpenseUseCase for pending installment payment does NOT change balance`() = runBlocking {
        val installmentPayment = expense(id = 100_000_001L, amount = 5_000L).copy(
            isInstallmentPayment = true,
            status = "Pending"
        )

        deleteExpenseUseCase(installmentPayment)

        coVerify(exactly = 0) { accountRepository.updateBalance(any(), any()) }
    }
}
