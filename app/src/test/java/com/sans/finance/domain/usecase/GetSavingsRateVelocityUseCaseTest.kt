package com.sans.finance.domain.usecase

import com.sans.finance.data.local.dao.CurrencyDao
import com.sans.finance.data.local.entity.ExchangeRateEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.MomentumTrend
import com.sans.finance.domain.repository.ExpenseRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetSavingsRateVelocityUseCaseTest {

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var localeManager: LocaleManager
    private lateinit var currencyDao: CurrencyDao
    private lateinit var useCase: GetSavingsRateVelocityUseCase

    @Before
    fun setUp() {
        expenseRepository = mockk()
        localeManager = mockk()
        currencyDao = mockk()

        every { localeManager.getCurrency() } returns "IDR"
        every { currencyDao.getAllRates() } returns flowOf(listOf(ExchangeRateEntity("IDR", 1.0, System.currentTimeMillis())))

        useCase = GetSavingsRateVelocityUseCase(
            expenseRepository = expenseRepository,
            localeManager = localeManager,
            currencyDao = currencyDao
        )
    }

    @Test
    fun `test savings rate and velocity computation across months`() = runBlocking {
        val now = System.currentTimeMillis()
        val expenses = listOf(
            Expense(id = 1, title = "Salary", amount = 20_000_000L, date = now, categoryId = 1, accountId = 1, type = "INCOME"),
            Expense(id = 2, title = "Rent", amount = 8_000_000L, date = now, categoryId = 2, accountId = 1, type = "EXPENSE")
        )

        every { expenseRepository.getExpensesBetween(any(), any()) } returns flowOf(expenses)

        val result = useCase().first()

        // 20M income - 8M expense = 12M savings -> 60% savings rate
        assertEquals(60.0, result.currentMonthSavingsRatePct, 0.01)
        assertEquals(6, result.history.size)
        assertTrue(result.monthlyNetWorthVelocity > 0L)
    }

    @Test
    fun `test empty expenses returns zero rates safely`() = runBlocking {
        every { expenseRepository.getExpensesBetween(any(), any()) } returns flowOf(emptyList())

        val result = useCase().first()

        assertEquals(0.0, result.currentMonthSavingsRatePct, 0.01)
        assertEquals(0.0, result.threeMonthAvgSavingsRatePct, 0.01)
        assertEquals(0L, result.monthlyNetWorthVelocity)
        assertEquals(MomentumTrend.STEADY, result.momentumTrend)
    }
}
