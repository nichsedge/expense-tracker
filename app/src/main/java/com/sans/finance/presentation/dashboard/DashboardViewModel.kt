package com.sans.finance.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.finance.data.local.dao.SnapshotTotal
import com.sans.finance.domain.model.Expense
import com.sans.finance.domain.model.Goal
import com.sans.finance.domain.model.WealthDistributionTab
import com.sans.finance.domain.repository.ExpenseRepository
import com.sans.finance.domain.repository.GoalRepository
import com.sans.finance.domain.repository.PortfolioRepository
import com.sans.finance.domain.repository.UserPreferencesRepository
import com.sans.finance.domain.usecase.GetDashboardSummaryUseCase
import com.sans.finance.domain.usecase.GetFinancialFreedomStatsUseCase
import com.sans.finance.domain.usecase.GetNetWorthTrendUseCase
import com.sans.finance.domain.usecase.GetWealthDistributionUseCase
import com.sans.finance.data.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    val monthlyIncome: Long = 0L,
    val monthlyExpense: Long = 0L,
    val monthlyCashFlow: Long = 0L,
    val monthlySavingsRate: Float = 0f,
    val globalBudget: Long = 0L,
    val globalSpent: Long = 0L,
    val currentCurrency: String = "USD",
    val last30DaysTrend: List<Long> = emptyList(),
    val daysLeftInMonth: Int = 0,
    val isPrivacyModeEnabled: Boolean = false,
    val wealthDistributionTab: WealthDistributionTab = WealthDistributionTab.CATEGORY,
    val annualExpense: Long = 0L,
    val financialFreedomYears: Double = 0.0,
    val financialFreedomScore: Float = 0f,
    val isFireManualEnabled: Boolean = false,
    val manualFireAnnualExpense: Long = 0L,
    val recentTransactions: List<Expense> = emptyList(),
    val spendingVelocity: Float = 0f,
    val categoryBudgets: List<com.sans.finance.domain.model.CategoryBudgetProgress> = emptyList()
)

data class DashboardGoal(
    val name: String,
    val progress: Float
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val goalRepository: GoalRepository,
    private val portfolioRepository: PortfolioRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val getFinancialFreedomStatsUseCase: GetFinancialFreedomStatsUseCase,
    private val getWealthDistributionUseCase: GetWealthDistributionUseCase,
    private val getNetWorthTrendUseCase: GetNetWorthTrendUseCase
) : ViewModel() {

    private val _wealthDistributionTab = MutableStateFlow(WealthDistributionTab.CATEGORY)

    private val summaryFlow = getDashboardSummaryUseCase()
    private val freedomFlow = getFinancialFreedomStatsUseCase()
    private val prefsFlow = userPreferencesRepository.userPreferences
    private val trendFlow = getNetWorthTrendUseCase(30)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val wealthDistributionFlow = _wealthDistributionTab.flatMapLatest { tab ->
        getWealthDistributionUseCase(tab)
    }

    val state: StateFlow<DashboardState> = combine(
        summaryFlow,
        freedomFlow,
        prefsFlow,
        _wealthDistributionTab,
        wealthDistributionFlow,
        trendFlow,
        portfolioRepository.getTotalValueOverTime(),
        expenseRepository.getExpensesBetween(0, Long.MAX_VALUE),
        expenseRepository.getRecurringExpenses(),
        goalRepository.getAllGoals()
    ) { args ->
        val summary = args[0] as com.sans.finance.domain.model.DashboardSummary
        val freedom = args[1] as com.sans.finance.domain.model.FinancialFreedomStats
        val prefs = args[2] as com.sans.finance.domain.model.UserPreferences
        val tab = args[3] as WealthDistributionTab
        @Suppress("UNCHECKED_CAST")
        val distribution = args[4] as Map<String, Long>
        @Suppress("UNCHECKED_CAST")
        val trend = args[5] as List<Long>
        @Suppress("UNCHECKED_CAST")
        val portfolioHistory = args[6] as List<SnapshotTotal>
        @Suppress("UNCHECKED_CAST")
        val allExpenses = args[7] as List<Expense>
        @Suppress("UNCHECKED_CAST")
        val recurring = args[8] as List<Expense>
        @Suppress("UNCHECKED_CAST")
        val goals = args[9] as List<Goal>

        val now = System.currentTimeMillis()
        val suggestions = generateAiSuggestions(summary, freedom, recurring, goals)

        DashboardState(
            netWorth = summary.netWorth,
            totalAssets = summary.totalAssets,
            totalLiabilities = summary.totalLiabilities,
            upcomingBills = (recurring + allExpenses.filter { it.isInstallmentPayment && it.status == "Pending" && it.date >= now })
                .sortedBy { if (it.isInstallmentPayment) it.date else it.nextDueDate ?: Long.MAX_VALUE }
                .take(3),
            goals = goals.map { goal ->
                val currentAmountIdr = when (goal.targetType) {
                    "TOTAL" -> portfolioHistory.lastOrNull()?.totalIdr ?: 0.0
                    else -> 0.0
                }
                DashboardGoal(name = goal.name, progress = (currentAmountIdr / goal.targetAmount).toFloat().coerceIn(0f, 1f))
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
            isPrivacyModeEnabled = prefs.isPrivacyModeEnabled,
            wealthDistributionTab = tab,
            annualExpense = freedom.annualExpense,
            financialFreedomYears = freedom.yearsOfCover,
            financialFreedomScore = freedom.freedomScore,
            isFireManualEnabled = prefs.fireManualEnabled,
            manualFireAnnualExpense = prefs.manualFireAnnualExpense,
            recentTransactions = allExpenses.filter { it.date <= now }.sortedByDescending { it.date }.take(5),
            spendingVelocity = summary.spendingVelocity,
            categoryBudgets = summary.categoryBudgetProgress
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    private fun generateAiSuggestions(summary: com.sans.finance.domain.model.DashboardSummary, freedom: com.sans.finance.domain.model.FinancialFreedomStats, recurring: List<Expense>, goals: List<Goal>): List<String> {
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
        viewModelScope.launch {
            userPreferencesRepository.setPrivacyModeEnabled(!state.value.isPrivacyModeEnabled)
        }
    }

    fun setWealthDistributionTab(tab: WealthDistributionTab) {
        _wealthDistributionTab.value = tab
    }

    fun setFireManualEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFireManualEnabled(enabled)
        }
    }

    fun setManualFireAnnualExpense(amount: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setManualFireAnnualExpense(amount)
        }
    }
}
