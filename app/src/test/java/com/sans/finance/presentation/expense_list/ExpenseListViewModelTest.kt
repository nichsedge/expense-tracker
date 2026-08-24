package com.sans.finance.presentation.expense_list

import app.cash.turbine.test
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.model.DaySpent
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.FilteredExpensesData
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.InstallmentRepository
import com.sans.finance.domain.usecase.GetCategoriesUseCase
import com.sans.finance.domain.usecase.ObserveFilteredExpensesUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var observeFilteredExpensesUseCase: ObserveFilteredExpensesUseCase
    private lateinit var expenseRepository: com.sans.finance.domain.repository.ExpenseRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var installmentRepository: InstallmentRepository
    private lateinit var getCategoriesUseCase: GetCategoriesUseCase
    private lateinit var budgetRepository: com.sans.finance.domain.repository.BudgetRepository
    private lateinit var localeManager: LocaleManager

    private val filteredResult = MutableStateFlow(
        FilteredExpensesData(expenses = emptyList(), dailySpending = emptyList())
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        observeFilteredExpensesUseCase = mockk()
        every { observeFilteredExpensesUseCase.invoke(any()) } returns filteredResult

        expenseRepository = mockk(relaxed = true)
        every { expenseRepository.getAllTags() } returns flowOf(emptyList())
        every { expenseRepository.getTotalSpentBetween(any(), any()) } returns flowOf(null)

        accountRepository = mockk(relaxed = true)
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())

        installmentRepository = mockk(relaxed = true)
        every { installmentRepository.getTotalPaidAmountBetween(any(), any()) } returns flowOf(null)

        getCategoriesUseCase = mockk()
        every { getCategoriesUseCase.invoke() } returns flowOf(emptyList<Category>())

        budgetRepository = mockk(relaxed = true)
        every { budgetRepository.getAllBudgets() } returns flowOf(emptyList())

        localeManager = mockk()
        every { localeManager.getCurrency() } returns "IDR"
        every { localeManager.privacyMode } returns MutableStateFlow(false)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = ExpenseListViewModel(
        observeFilteredExpensesUseCase = observeFilteredExpensesUseCase,
        repository = expenseRepository,
        accountRepository = accountRepository,
        installmentRepository = installmentRepository,
        getCategoriesUseCase = getCategoriesUseCase,
        budgetRepository = budgetRepository,
        localeManager = localeManager
    )

    private fun expense(
        id: Long,
        amount: Long,
        type: String = "EXPENSE"
    ) = Expense(
        id = id,
        date = 1_700_000_000_000L,
        title = "Tx $id",
        amount = amount,
        categoryId = 1,
        type = type
    )

    @Test
    fun `initial state uses currency from LocaleManager and stops loading after data arrives`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("IDR", state.currentCurrency)
        assertFalse(state.isLoading)
    }

    @Test
    fun `filtered expenses update totals and daily spending`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        filteredResult.value = FilteredExpensesData(
            expenses = listOf(
                expense(id = 1, amount = 10_000),
                expense(id = 2, amount = 25_000),
                expense(id = 3, amount = 50_000, type = "INCOME")
            ),
            dailySpending = listOf(DaySpent(day = 1_700_000_000_000L, amount = 35_000))
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(3, state.expenses.size)
        assertEquals(35_000L, state.totalFilteredExpense)
        assertEquals(50_000L, state.totalFilteredIncome)
        assertEquals(15_000L, state.totalFilteredAmount)
        assertEquals(35_000L, state.dailySpending[1_700_000_000_000L])
    }

    @Test
    fun `toggleCategoryFilter adds then removes category`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleCategoryFilter(7)
        assertTrue(viewModel.state.value.selectedCategoryIds.contains(7))

        viewModel.toggleCategoryFilter(7)
        assertFalse(viewModel.state.value.selectedCategoryIds.contains(7))
    }

    @Test
    fun `toggleTypeFilter adds then removes type`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleTypeFilter("INCOME")
        assertTrue(viewModel.state.value.selectedTypes.contains("INCOME"))

        viewModel.toggleTypeFilter("INCOME")
        assertFalse(viewModel.state.value.selectedTypes.contains("INCOME"))
    }

    @Test
    fun `clearFilters resets query selections and amounts`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateSearchQuery("coffee")
        viewModel.toggleCategoryFilter(3)
        viewModel.updateAmountFilter(min = 1_000, max = 9_000)

        viewModel.clearFilters()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("", state.searchQuery)
        assertTrue(state.selectedCategoryIds.isEmpty())
        assertEquals(null, state.minAmount)
        assertEquals(null, state.maxAmount)
        assertEquals(DateRangeFilter.THIS_MONTH, state.activeDateFilter)
    }

    @Test
    fun `state emits updates through turbine`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            // Initial value
            assertEquals(true, awaitItem().isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
