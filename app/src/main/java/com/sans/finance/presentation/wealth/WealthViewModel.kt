package com.sans.finance.presentation.wealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.data.worker.CloudSyncAndBackupWorker
import com.sans.finance.domain.repository.AccountRepository
import com.sans.finance.domain.repository.PortfolioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WealthState(
    val cashAssets: Long = 0L,
    val liabilities: Long = 0L,
    val portfolioValue: Long = 0L,
    val monthlyBurn: Long = 0L,
    val runwayMonths: Double = 0.0,
    val monthlyPassiveIncome: Long = 0L,
    val annualPassiveIncome: Long = 0L,
    val nextPayoutDateStr: String = "10th of every month",
    val fiCoveragePct: Double = 0.0,
    val fiStage: String = "Foundation Stage (<25%)",
    val fiNextStageGap: Long = 0L,
    val lastSnapshotDate: Long? = null,
    val portfolioSources: List<Pair<String, Int>> = emptyList(),
    val currencyCode: String = "IDR",
    val isPrivacyModeEnabled: Boolean = false,
    val isSyncing: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class WealthViewModel @Inject constructor(
    accountRepository: AccountRepository,
    portfolioRepository: PortfolioRepository,
    accountTypeRepository: com.sans.finance.domain.repository.AccountTypeRepository,
    private val expenseDao: com.sans.finance.data.local.dao.ExpenseDao,
    private val currencyDao: com.sans.finance.data.local.dao.CurrencyDao,
    private val localeManager: LocaleManager
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)

    private val portfolioState = combine(
        portfolioRepository.getLatestSnapshotHeader(),
        portfolioRepository.getLatestSnapshot()
    ) { latestHeader, latestHoldings -> latestHeader to latestHoldings }

    private val ninetyDaysAgo = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
    private val recentExpenses = expenseDao.getExpensesBetween(ninetyDaysAgo, System.currentTimeMillis() + 86400000L)

    private data class AccountsContext(
        val accounts: List<com.sans.finance.data.local.entity.AccountEntity>,
        val types: List<com.sans.finance.data.local.entity.AccountTypeEntity>,
        val rates: Map<String, Double>,
        val expenses: List<com.sans.finance.data.local.entity.ExpenseWithTags>
    )

    private val accountsContext = combine(
        accountRepository.getAllAccounts(),
        accountTypeRepository.getAllAccountTypes(),
        currencyDao.getAllRates(),
        recentExpenses
    ) { accounts, types, rates, expenses ->
        val ratesMap = rates.associate { it.code to it.rateToIdr }
        AccountsContext(accounts, types, ratesMap, expenses)
    }

    private val baseState = combine(
        accountsContext,
        portfolioState,
        localeManager.privacyMode
    ) { accContext, (latestHeader, latestHoldings), privacyMode ->
        val accounts = accContext.accounts
        val types = accContext.types
        val ratesMap = accContext.rates
        val expenses = accContext.expenses

        val liabilityTypeNames = types.filter { it.isLiability }.map { it.name }.toSet()
        val baseCurrency = localeManager.getCurrency()
        val baseRate = if (baseCurrency == "IDR") 1.0 else (ratesMap[baseCurrency] ?: 1.0)

        fun convertToBase(amount: Long, from: String): Long {
            if (from == baseCurrency) return amount
            val fromRate = if (from == "IDR") 1.0 else (ratesMap[from] ?: 1.0)
            val toRate = baseRate
            if (toRate == 0.0) return amount
            return ((amount * fromRate) / toRate).toLong()
        }

        val cashAssets = accounts
            .filter { it.type !in liabilityTypeNames && it.type != "Investment" }
            .sumOf { convertToBase(it.balance, it.currency) }
        val liabilities = accounts
            .filter { it.type in liabilityTypeNames }
            .sumOf { convertToBase(it.balance, it.currency) }

        val portfolioValueIdr = latestHoldings.sumOf { it.valueIdr }
        val portfolioValue = if (baseRate > 0) ((portfolioValueIdr / baseRate) * 100).toLong() else (portfolioValueIdr * 100).toLong()
        val sources = latestHoldings
            .groupBy { it.source }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

        // Calculate 3-month average monthly burn
        val pureExpenses = expenses.filter { it.expense.type == "EXPENSE" && !it.expense.isInstallment }
        val total90dExpense = pureExpenses.sumOf { convertToBase(it.expense.amount, "IDR") }
        val monthlyBurn = (total90dExpense / 3.0).toLong()
        val runwayMonths = if (monthlyBurn > 0L) {
            (cashAssets.toDouble() / monthlyBurn.toDouble()).coerceAtMost(99.0)
        } else {
            if (cashAssets > 0L) 99.0 else 0.0
        }

        // Calculate SBN / Sukuk monthly coupon passive income
        val sukukRates = mapOf(
            "ST010T4" to 0.0640,
            "ST012T4" to 0.0655,
            "ST013T2" to 0.0640,
            "ST014T2" to 0.0640
        )
        var totalMonthlyCouponIdr = 0.0
        latestHoldings.forEach { holding ->
            val asset = holding.asset
            val cat = holding.category
            if (cat.contains("SBN", ignoreCase = true) || cat.contains("Bond", ignoreCase = true) || asset.contains("Sukuk", ignoreCase = true)) {
                var rate = 0.0625
                for ((code, r) in sukukRates) {
                    if (asset.contains(code, ignoreCase = true)) {
                        rate = r
                        break
                    }
                }
                val grossMonthly = (holding.valueIdr * rate) / 12.0
                val netMonthly = grossMonthly * 0.90
                totalMonthlyCouponIdr += netMonthly
            }
        }
        val monthlyPassiveIncome = if (baseRate > 0) ((totalMonthlyCouponIdr / baseRate) * 100).toLong() else (totalMonthlyCouponIdr * 100).toLong()
        val annualPassiveIncome = monthlyPassiveIncome * 12

        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val nextMonth = if (currentDay >= 10) calendar.get(java.util.Calendar.MONTH) + 1 else calendar.get(java.util.Calendar.MONTH)
        val nextYear = if (nextMonth > 11) calendar.get(java.util.Calendar.YEAR) + 1 else calendar.get(java.util.Calendar.YEAR)
        val adjMonth = if (nextMonth > 11) 0 else nextMonth
        val nextPayoutCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, nextYear)
            set(java.util.Calendar.MONTH, adjMonth)
            set(java.util.Calendar.DAY_OF_MONTH, 10)
        }
        val nextPayoutDateStr = "10 " + java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.US).format(nextPayoutCal.time)

        val fiCoveragePct = if (monthlyBurn > 0L) {
            ((monthlyPassiveIncome.toDouble() / monthlyBurn.toDouble()) * 100.0).coerceAtMost(100.0)
        } else {
            0.0
        }

        val (fiStage, fiNextStageGap) = when {
            fiCoveragePct >= 100.0 -> Pair("Full Financial Freedom (100%+)", 0L)
            fiCoveragePct >= 50.0 -> {
                val gap = (monthlyBurn - monthlyPassiveIncome).coerceAtLeast(0L)
                Pair("Lean FI Achieved (50%+)", gap)
            }
            fiCoveragePct >= 25.0 -> {
                val halfBurn = (monthlyBurn * 0.50).toLong()
                val gap = (halfBurn - monthlyPassiveIncome).coerceAtLeast(0L)
                Pair("Emerging FI Buffer (25%+)", gap)
            }
            else -> {
                val quarterBurn = (monthlyBurn * 0.25).toLong()
                val gap = (quarterBurn - monthlyPassiveIncome).coerceAtLeast(0L)
                Pair("Foundation Stage (<25%)", gap)
            }
        }

        WealthState(
            cashAssets = cashAssets,
            liabilities = liabilities,
            portfolioValue = portfolioValue,
            monthlyBurn = monthlyBurn,
            runwayMonths = runwayMonths,
            monthlyPassiveIncome = monthlyPassiveIncome,
            annualPassiveIncome = annualPassiveIncome,
            nextPayoutDateStr = nextPayoutDateStr,
            fiCoveragePct = fiCoveragePct,
            fiStage = fiStage,
            fiNextStageGap = fiNextStageGap,
            lastSnapshotDate = latestHeader?.snapshotDate,
            portfolioSources = sources,
            currencyCode = baseCurrency,
            isPrivacyModeEnabled = privacyMode,
            isSyncing = false,
            isLoading = false
        )
    }

    val state = combine(baseState, _isSyncing) { base, isSyncing ->
        base.copy(isSyncing = isSyncing)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WealthState()
    )

    fun triggerCloudSync(context: android.content.Context) {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val workRequest = OneTimeWorkRequestBuilder<CloudSyncAndBackupWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
                kotlinx.coroutines.delay(2000)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun togglePrivacyMode() {
        localeManager.setPrivacyModeEnabled(!localeManager.isPrivacyModeEnabled())
    }
}
