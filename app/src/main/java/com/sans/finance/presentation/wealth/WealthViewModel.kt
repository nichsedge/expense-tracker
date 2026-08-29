package com.sans.finance.presentation.wealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sans.finance.data.util.LocaleManager
import com.sans.finance.data.worker.CloudSyncAndBackupWorker
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
    private val portfolioRepository: PortfolioRepository,
    private val getWealthMetricsUseCase: com.sans.finance.domain.usecase.GetWealthMetricsUseCase,
    private val localeManager: LocaleManager
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)

    private val baseState = combine(
        getWealthMetricsUseCase(),
        portfolioRepository.getLatestSnapshotHeader(),
        portfolioRepository.getLatestSnapshot(),
        localeManager.privacyMode
    ) { metrics, latestHeader, latestHoldings, privacyMode ->
        val sources = latestHoldings
            .groupBy { it.source }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }

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
        val nextPayoutDateStr = "10 " + com.sans.finance.core.util.DateFormatterUtils.getMonthYearFormatter().format(nextPayoutCal.time)

        WealthState(
            cashAssets = metrics.cashAssets,
            liabilities = metrics.liabilities,
            portfolioValue = metrics.portfolioValue,
            monthlyBurn = metrics.monthlyBurn,
            runwayMonths = metrics.runwayMonths,
            monthlyPassiveIncome = metrics.monthlyPassiveIncome,
            annualPassiveIncome = metrics.annualPassiveIncome,
            nextPayoutDateStr = nextPayoutDateStr,
            fiCoveragePct = metrics.fiCoveragePct,
            fiStage = metrics.fiStage,
            fiNextStageGap = metrics.fiNextStageGap,
            lastSnapshotDate = latestHeader?.snapshotDate,
            portfolioSources = sources,
            currencyCode = metrics.currencyCode,
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
