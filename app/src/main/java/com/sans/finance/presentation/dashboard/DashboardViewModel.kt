package com.sans.finance.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.BudgetEntity
import com.sans.finance.data.local.entity.GoalEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.repository.AccountTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardState(
    val netWorth: Long = 0L,
    val totalAssets: Long = 0L,
    val totalLiabilities: Long = 0L,

    val upcomingBills: List<Expense> = emptyList(),
    val goals: List<DashboardGoal> = emptyList(),
    val projectedBalance30Days: Long = 0L,
    val wealthDistribution: Map<String, Long> = emptyMap(),
    val aiSuggestions: List<String> = emptyList(),
    val isLoading: Boolean = true,
    // Monthly cash flow
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val monthlyCashFlow: Long = 0L,
    val monthlySavingsRate: Float = 0f,
    // Global budget
    val globalBudget: Long = 0L,
    val globalSpent: Long = 0L,
    val currentCurrency: String = "USD",
    val last30DaysTrend: List<Long> = emptyList(),
    val daysLeftInMonth: Int = 0,
    val isPrivacyModeEnabled: Boolean = false,
    val wealthDistributionTab: WealthDistributionTab = WealthDistributionTab.CATEGORY,
    // Financial Freedom
    val annualExpense: Long = 0L,
    val financialFreedomYears: Double = 0.0,
    val financialFreedomScore: Float = 0f,
    val isFireManualEnabled: Boolean = false,
    val manualFireAnnualExpense: Long = 0L,
    val recentTransactions: List<Expense> = emptyList(),
    val spendingVelocity: Float = 0f,
    val categoryBudgets: List<com.sans.finance.domain.model.CategoryBudgetProgress> = emptyList()
)

enum class WealthDistributionTab {
    CURRENCY, ASSET_CLASS, CATEGORY
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val accountTypeRepository: AccountTypeRepository,
    private val expenseRepository: ExpenseRepository,
    private val goalRepository: GoalRepository,
    private val portfolioRepository: PortfolioRepository,
    private val getDashboardSummaryUseCase: com.sans.finance.domain.usecase.GetDashboardSummaryUseCase,
    private val getFinancialFreedomStatsUseCase: com.sans.finance.domain.usecase.GetFinancialFreedomStatsUseCase,
    private val localeManager: com.sans.finance.data.util.LocaleManager,
    private val currencyDao: com.sans.finance.data.local.dao.CurrencyDao
) : ViewModel() {

    private val _wealthDistributionTab =
        kotlinx.coroutines.flow.MutableStateFlow(WealthDistributionTab.CATEGORY)

    // Simplified finance context for items that still need in-memory processing
    private val itemContext = combine(
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
        expenseRepository.getRecurringExpenses(),
        goalRepository.getAllGoals(),
        portfolioRepository.getLatestSnapshot(),
        accountRepository.getAllAccounts(),
        accountTypeRepository.getAllAccountTypes(),
        currencyDao.getAllRates()
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val txns = args[0] as List<Expense>
        @Suppress("UNCHECKED_CAST")
        val recurring = args[1] as List<Expense>
        @Suppress("UNCHECKED_CAST")
        val goals = args[2] as List<GoalEntity>
        @Suppress("UNCHECKED_CAST")
        val holdings = args[3] as List<PortfolioHoldingEntity>
        @Suppress("UNCHECKED_CAST")
        val accounts = args[4] as List<com.sans.finance.data.local.entity.AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val types = args[5] as List<com.sans.finance.data.local.entity.AccountTypeEntity>
        @Suppress("UNCHECKED_CAST")
        val rates = args[6] as List<com.sans.finance.data.local.entity.ExchangeRateEntity>

        ItemContext(txns, recurring, goals, holdings, accounts, types, rates.associate { it.code to it.rateToIdr })
    }

    private val settingsContext = combine(
        localeManager.privacyMode,
        _wealthDistributionTab
    ) { privacy, tab ->
        SettingsContext(privacy, tab)
    }

    val state = combine(
        getDashboardSummaryUseCase(),
        getFinancialFreedomStatsUseCase(),
        itemContext,
        portfolioRepository.getTotalValueOverTime(),
        settingsContext
    ) { summary, freedom, items, portfolioHistory, settings ->
        val now = System.currentTimeMillis()
        val baseCurrency = localeManager.getCurrency()
        val baseRate = if (baseCurrency == "IDR") 1.0 else items.rates[baseCurrency] ?: 1.0

        // Optimized trend calculation (O(N + 30))
        val trend = calculateTrend(portfolioHistory, baseRate)

        // Wealth Distribution
        val distribution = calculateDistribution(items.holdings, items.accounts, items.accountTypes, settings.wealthDistributionTab, baseRate, items.rates)

        // AI Suggestions
        val suggestions = generateAiSuggestions(summary, freedom, items.recurring, items.goals)

        DashboardState(
            netWorth = summary.netWorth,
            totalAssets = summary.totalAssets,
            totalLiabilities = summary.totalLiabilities,
            upcomingBills = (items.recurring + items.transactions.filter { it.isInstallmentPayment && it.status == "Pending" && it.date >= now })
                .sortedBy { if (it.isInstallmentPayment) it.date else it.nextDueDate ?: Long.MAX_VALUE }
                .take(3),
            goals = items.goals.map { goal ->
                val currentAmountIdr = when (goal.targetType) {
                    "TOTAL" -> portfolioHistory.lastOrNull()?.totalIdr ?: 0.0
                    "CATEGORY" -> items.holdings.filter { it.category == goal.targetName }.sumOf { it.valueIdr }
                    "ASSET_CLASS" -> items.holdings.filter { it.assetClass == goal.targetName }.sumOf { it.valueIdr }
                    else -> 0.0
                }
                DashboardGoal(
                    name = goal.name,
                    progress = (currentAmountIdr / goal.targetAmount).toFloat().coerceIn(0f, 1f)
                )
            }.take(2),
            projectedBalance30Days = summary.netWorth,
            wealthDistribution = distribution,
            aiSuggestions = suggestions,
            isLoading = false,
            monthlyIncome = summary.monthlyIncome,
            monthlyExpense = summary.monthlyExpense,
            monthlyCashFlow = summary.monthlyCashFlow,
            monthlySavingsRate = summary.monthlySavingsRate,
            globalBudget = summary.globalBudget,
            globalSpent = summary.globalSpent,
            currentCurrency = summary.currentCurrency,
            last30DaysTrend = trend,
            daysLeftInMonth = summary.daysLeftInMonth,
            isPrivacyModeEnabled = settings.isPrivacyModeEnabled,
            wealthDistributionTab = settings.wealthDistributionTab,
            annualExpense = freedom.annualExpense,
            financialFreedomYears = freedom.yearsOfCover,
            financialFreedomScore = freedom.freedomScore,
            isFireManualEnabled = freedom.annualExpense > 0, // Placeholder, should ideally come from UseCase/Settings
            manualFireAnnualExpense = freedom.annualExpense,
            recentTransactions = items.transactions.filter { it.date <= now }.sortedByDescending { it.date }.take(5),
            spendingVelocity = summary.spendingVelocity,
            categoryBudgets = summary.categoryBudgetProgress
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    private fun calculateTrend(history: List<SnapshotTotal>, baseRate: Double): List<Long> {
        val now = System.currentTimeMillis()
        val trend = mutableListOf<Long>()
        val sortedHistory = history.sortedBy { it.snapshot_date }
        var historyIdx = sortedHistory.lastIndex

        for (i in 0..29) {
            val dayStart = now - (i.toLong() * 24 * 60 * 60 * 1000)
            while (historyIdx >= 0 && sortedHistory[historyIdx].snapshot_date > dayStart) {
                historyIdx--
            }
            val dayValueIdr = if (historyIdx >= 0) sortedHistory[historyIdx].totalIdr else 0.0
            val dayValue = if (baseRate > 0) ((dayValueIdr / baseRate) * 100).toLong() else (dayValueIdr * 100).toLong()
            trend.add(dayValue)
        }
        return trend.reversed()
    }

    private fun calculateDistribution(
        holdings: List<PortfolioHoldingEntity>,
        accounts: List<com.sans.finance.data.local.entity.AccountEntity>,
        accountTypes: List<com.sans.finance.data.local.entity.AccountTypeEntity>,
        tab: WealthDistributionTab,
        baseRate: Double,
        ratesMap: Map<String, Double>
    ): Map<String, Long> {
        val liabilityTypeNames = accountTypes.filter { it.isLiability }.map { it.name }.toSet()
        val nonLiabilityAccounts = accounts.filter { it.type !in liabilityTypeNames && it.type != "Investment" }

        val distribution = when (tab) {
            WealthDistributionTab.CURRENCY -> {
                val hGroup = holdings.groupBy { it.currency }
                    .mapValues { it.value.sumOf { h -> h.valueIdr } }

                val aGroup = nonLiabilityAccounts.groupBy { it.currency }
                    .mapValues { entry ->
                        entry.value.sumOf { a ->
                            val rateToIdr = if (a.currency == "IDR") 1.0 else (ratesMap[a.currency] ?: 1.0)
                            (a.balance / 100.0) * rateToIdr
                        }
                    }

                (hGroup.keys + aGroup.keys).associateWith { key ->
                    val idrValue = (hGroup[key] ?: 0.0) + (aGroup[key] ?: 0.0)
                    if (baseRate > 0) ((idrValue / baseRate) * 100).toLong() else (idrValue * 100).toLong()
                }
            }

            WealthDistributionTab.ASSET_CLASS -> {
                val hGroup = holdings.groupBy { it.assetClass }
                    .mapValues { it.value.sumOf { h -> h.valueIdr } }

                val aValue = nonLiabilityAccounts.sumOf { a ->
                    val rateToIdr = if (a.currency == "IDR") 1.0 else (ratesMap[a.currency] ?: 1.0)
                    (a.balance / 100.0) * rateToIdr
                }

                val combined = hGroup.toMutableMap()
                combined["Cash & Equivalents"] = (combined["Cash & Equivalents"] ?: 0.0) + aValue

                combined.mapValues { entry ->
                    val idrValue = entry.value
                    if (baseRate > 0) ((idrValue / baseRate) * 100).toLong() else (idrValue * 100).toLong()
                }
            }

            WealthDistributionTab.CATEGORY -> {
                val hGroup = holdings.groupBy { it.category }
                    .mapValues { it.value.sumOf { h -> h.valueIdr } }

                val aGroup = nonLiabilityAccounts.groupBy { it.type }
                    .mapValues { entry ->
                        entry.value.sumOf { a ->
                            val rateToIdr = if (a.currency == "IDR") 1.0 else (ratesMap[a.currency] ?: 1.0)
                            (a.balance / 100.0) * rateToIdr
                        }
                    }

                (hGroup.keys + aGroup.keys).associateWith { key ->
                    val idrValue = (hGroup[key] ?: 0.0) + (aGroup[key] ?: 0.0)
                    if (baseRate > 0) ((idrValue / baseRate) * 100).toLong() else (idrValue * 100).toLong()
                }
            }
        }

        return distribution.toList()
            .sortedByDescending { kotlin.math.abs(it.second) }
            .toMap()
    }

    private fun generateAiSuggestions(summary: com.sans.finance.domain.model.DashboardSummary, freedom: com.sans.finance.domain.model.FinancialFreedomStats, recurring: List<Expense>, goals: List<GoalEntity>): List<String> {
        val suggestions = mutableListOf<String>()
        if (freedom.yearsOfCover > 0 && freedom.yearsOfCover < 1.0) suggestions.add("You have less than a year of financial cover. Focus on building an emergency fund.")
        if (freedom.yearsOfCover >= 25.0) suggestions.add("🌟 Congratulations! You've reached financial independence (25x expenses).")
        else if (freedom.yearsOfCover >= 10.0) suggestions.add("Great progress! You have over a decade of freedom secured.")

        val recurringNet = recurring.sumOf { if (it.type == "INCOME") it.amount else -it.amount }
        if (recurringNet < 0) suggestions.add("Your recurring expenses exceed your recurring income. Consider reviewing subscriptions.")
        if (summary.totalAssets > 0 && goals.isEmpty()) suggestions.add("You have healthy assets but no active goals. Why not set a new savings target?")
        if (summary.monthlyIncome > 0 && summary.monthlySavingsRate < 0.1f) suggestions.add("You're saving less than 10% of your income this month. Try to reduce discretionary spending.")
        if (summary.monthlyExpense > summary.monthlyIncome && summary.monthlyIncome > 0) suggestions.add("⚠️ You're spending more than you earn this month. Review your expenses.")

        return suggestions
    }

    fun togglePrivacyMode() {
        localeManager.setPrivacyModeEnabled(!localeManager.isPrivacyModeEnabled())
    }

    fun setWealthDistributionTab(tab: WealthDistributionTab) {
        _wealthDistributionTab.value = tab
    }

    fun setFireManualEnabled(enabled: Boolean) {
        localeManager.setFireManualEnabled(enabled)
    }

    fun setManualFireAnnualExpense(amount: Long) {
        localeManager.setManualFireAnnualExpense(amount)
    }
}

// Type-safe Context wrappers
private data class ItemContext(
    val transactions: List<Expense>,
    val recurring: List<Expense>,
    val goals: List<GoalEntity>,
    val holdings: List<PortfolioHoldingEntity>,
    val accounts: List<com.sans.finance.data.local.entity.AccountEntity>,
    val accountTypes: List<com.sans.finance.data.local.entity.AccountTypeEntity>,
    val rates: Map<String, Double>
)

private data class SettingsContext(
    val isPrivacyModeEnabled: Boolean,
    val wealthDistributionTab: WealthDistributionTab
)

data class DashboardGoal(
    val name: String,
    val progress: Float
)
