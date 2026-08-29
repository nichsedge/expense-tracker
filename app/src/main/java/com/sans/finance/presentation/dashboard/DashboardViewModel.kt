package com.sans.finance.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.data.local.entity.AccountEntity
import com.sans.finance.data.local.entity.BudgetEntity
import com.sans.finance.data.local.entity.GoalEntity
import com.sans.finance.data.local.entity.PortfolioHoldingEntity
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
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
    val recentTransactions: List<Expense> = emptyList()
)

enum class WealthDistributionTab {
    CURRENCY, ASSET_CLASS, CATEGORY
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
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
        currencyDao.getAllRates()
    ) { txns, recurring, goals, holdings, rates ->
        ItemContext(txns, recurring, goals, holdings, rates.associate { it.code to it.rateToIdr })
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
        val distribution = calculateDistribution(items.holdings, settings.wealthDistributionTab, baseRate)

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
            recentTransactions = items.transactions.filter { it.date <= now }.sortedByDescending { it.date }.take(5)
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

    private fun calculateDistribution(holdings: List<PortfolioHoldingEntity>, tab: WealthDistributionTab, baseRate: Double): Map<String, Long> {
        val grouped = when (tab) {
            WealthDistributionTab.CURRENCY -> holdings.groupBy { it.currency }
            WealthDistributionTab.ASSET_CLASS -> holdings.groupBy { it.assetClass }
            WealthDistributionTab.CATEGORY -> holdings.groupBy { it.category }
        }
        return grouped.mapValues { entry ->
            val idrValue = entry.value.sumOf { it.valueIdr }
            if (baseRate > 0) ((idrValue / baseRate) * 100).toLong() else (idrValue * 100).toLong()
        }.toList()
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
