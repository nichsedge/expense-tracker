package com.sans.finance.presentation.goals

import app.cash.turbine.test
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.data.local.entity.PortfolioSnapshotHeaderEntity
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.domain.model.Goal
import com.sans.finance.domain.model.UserPreferences
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GoalViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var goalRepository: GoalRepository
    private lateinit var portfolioRepository: PortfolioRepository
    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var localeManager: LocaleManager

    private val goalsFlow = MutableStateFlow<List<Goal>>(emptyList())
    private val snapshotFlow = MutableStateFlow<List<PortfolioHoldingEntity>>(emptyList())
    private val headerFlow = MutableStateFlow<PortfolioSnapshotHeaderEntity?>(null)
    private val prefsFlow = MutableStateFlow(UserPreferences(isPrivacyModeEnabled = false))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        goalRepository = mockk(relaxed = true)
        portfolioRepository = mockk(relaxed = true)
        userPreferencesRepository = mockk(relaxed = true)
        localeManager = mockk(relaxed = true)

        every { goalRepository.getAllGoals() } returns goalsFlow
        every { portfolioRepository.getLatestSnapshot() } returns snapshotFlow
        every { portfolioRepository.getLatestSnapshotHeader() } returns headerFlow
        every { userPreferencesRepository.userPreferences } returns prefsFlow
        every { localeManager.getCurrency() } returns "IDR"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state combines goals with correct progress for TOTAL, CATEGORY, and ASSET_CLASS`() = runTest {
        val holdings = listOf(
            PortfolioHoldingEntity(
                snapshotDate = 1000L,
                source = "Stockbit",
                accountKey = "acc1",
                account = "Stockbit Account",
                category = "Indonesian Equities",
                assetClass = "Equities",
                asset = "BBCA",
                quantity = 100.0,
                price = 10000.0,
                currency = "IDR",
                valueIdr = 1000000.0,
                details = null
            ),
            PortfolioHoldingEntity(
                snapshotDate = 1000L,
                source = "Binance",
                accountKey = "acc2",
                account = "Binance Account",
                category = "Crypto Cold Storage",
                assetClass = "Crypto",
                asset = "BTC",
                quantity = 0.1,
                price = 60000.0,
                currency = "USD",
                valueIdr = 96000000.0,
                details = null
            )
        )

        val goals = listOf(
            Goal(id = 1, name = "Total Portfolio Goal", targetAmount = 200_000_000, targetType = "TOTAL", targetName = null, currency = "IDR"),
            Goal(id = 2, name = "Crypto Goal", targetAmount = 100_000_000, targetType = "ASSET_CLASS", targetName = "Crypto", currency = "IDR"),
            Goal(id = 3, name = "Stock Goal", targetAmount = 5_000_000, targetType = "CATEGORY", targetName = "Indonesian Equities", currency = "IDR")
        )

        goalsFlow.value = goals
        snapshotFlow.value = holdings

        val viewModel = GoalViewModel(goalRepository, portfolioRepository, userPreferencesRepository, localeManager)

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(3, state.goals.size)

            val totalGoal = state.goals.find { it.goal.id == 1L }
            assertEquals(97_000_000.0, totalGoal?.currentAmount ?: 0.0, 0.1)

            val cryptoGoal = state.goals.find { it.goal.id == 2L }
            assertEquals(96_000_000.0, cryptoGoal?.currentAmount ?: 0.0, 0.1)

            val stockGoal = state.goals.find { it.goal.id == 3L }
            assertEquals(1_000_000.0, stockGoal?.currentAmount ?: 0.0, 0.1)

            assertTrue(state.categories.contains("Indonesian Equities"))
            assertTrue(state.assetClasses.contains("Equities"))
            assertTrue(state.assetClasses.contains("Crypto"))
            assertTrue(state.assetClasses.contains("Cash & Equivalents"))
        }
    }

    @Test
    fun `addGoal delegates to repository with correct currency`() = runTest {
        coEvery { goalRepository.insertGoal(any()) } returns 1L

        val viewModel = GoalViewModel(goalRepository, portfolioRepository, userPreferencesRepository, localeManager)
        viewModel.addGoal(
            name = "Emergency Fund",
            targetAmount = 50_000_000.0,
            targetType = "ASSET_CLASS",
            targetName = "Cash & Equivalents",
            deadline = 123456789L
        )

        coVerify {
            goalRepository.insertGoal(
                match {
                    it.name == "Emergency Fund" &&
                            it.targetAmount == 50_000_000L &&
                            it.targetType == "ASSET_CLASS" &&
                            it.targetName == "Cash & Equivalents" &&
                            it.currency == "IDR" &&
                            it.deadline == 123456789L
                }
            )
        }
    }
}
