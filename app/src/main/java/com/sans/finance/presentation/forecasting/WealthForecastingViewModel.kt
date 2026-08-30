package com.sans.finance.presentation.forecasting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.math.pow

data class ForecastingState(
    val currentNetWorth: Long = 0L,
    val monthlySavings: Long = 0L,
    val monthlyExpenses: Long = 0L,
    val expectedRoi: Float = 0.07f, // 7% default
    val volatility: Float = 0.15f,   // 15% default
    val projectionYears: Int = 20,
    val projections: List<ProjectionPoint> = emptyList(),
    val whatIfProjections: List<ProjectionPoint> = emptyList(),
    val monteCarloP10: List<ProjectionPoint> = emptyList(),
    val monteCarloP50: List<ProjectionPoint> = emptyList(),
    val monteCarloP90: List<ProjectionPoint> = emptyList(),
    val fireSuccessRate: Float = 0f,
    val isLoading: Boolean = true,
    val currentCurrency: String = "USD",
    val fireNumber: Long = 0L,
    val yearsToFire: Int? = null,
    val whatIfYearsToFire: Int? = null,
    val emergencyFundTarget: Long = 0L,
    val emergencyFundMonths: Int = 6,
    val currentEmergencyFund: Long = 0L,
    val extraMonthlyContribution: Long = 0L
)

data class ProjectionPoint(
    val year: Int,
    val value: Long
)

@HiltViewModel
class WealthForecastingViewModel @Inject constructor(
    private val getWealthMetricsUseCase: com.sans.finance.domain.usecase.GetWealthMetricsUseCase,
    private val localeManager: com.sans.finance.data.util.LocaleManager
) : ViewModel() {

    private val _expectedRoi = MutableStateFlow(0.07f)
    private val _volatility = MutableStateFlow(0.15f)
    private val _projectionYears = MutableStateFlow(25)
    private val _emergencyFundMonths = MutableStateFlow(6)
    private val _extraMonthlyContribution = MutableStateFlow(0L)

    val state = combine(
        getWealthMetricsUseCase(),
        _expectedRoi,
        _volatility,
        _projectionYears,
        _emergencyFundMonths,
        _extraMonthlyContribution
    ) { args ->
        val metrics = args[0] as com.sans.finance.domain.model.WealthMetrics
        val roi = args[1] as Float
        val vol = args[2] as Float
        val years = args[3] as Int
        val efMonths = args[4] as Int
        val extraContrib = args[5] as Long
        val currentNetWorth = metrics.cashAssets + metrics.portfolioValue
        val fireNumber = metrics.monthlyBurn * 12 * 25

        val projections = calculateProjections(currentNetWorth, metrics.monthlySavings, roi, years)
        val whatIfProjections = calculateProjections(currentNetWorth, metrics.monthlySavings + extraContrib, roi, years)

        val mcResult = com.sans.finance.core.util.MonteCarloFireSimulator.simulate(
            initialWealth = currentNetWorth,
            annualSavings = (metrics.monthlySavings + extraContrib) * 12,
            meanReturn = roi.toDouble(),
            volatility = vol.toDouble(),
            inflation = 0.025,
            years = years,
            fireTarget = fireNumber,
            iterations = 1000
        )

        val mcP10 = mcResult.p10Projections.mapIndexed { idx, v -> ProjectionPoint(idx, v) }
        val mcP50 = mcResult.p50Projections.mapIndexed { idx, v -> ProjectionPoint(idx, v) }
        val mcP90 = mcResult.p90Projections.mapIndexed { idx, v -> ProjectionPoint(idx, v) }

        val yearsToFire = projections.find { it.value >= fireNumber }?.year
        val whatIfYearsToFire = whatIfProjections.find { it.value >= fireNumber }?.year

        val emergencyFundTarget = metrics.monthlyBurn * efMonths
        val currentEmergencyFund = metrics.cashAssets

        ForecastingState(
            currentNetWorth = currentNetWorth,
            monthlySavings = metrics.monthlySavings,
            monthlyExpenses = metrics.monthlyBurn,
            expectedRoi = roi,
            volatility = vol,
            projectionYears = years,
            projections = projections,
            whatIfProjections = whatIfProjections,
            monteCarloP10 = mcP10,
            monteCarloP50 = mcP50,
            monteCarloP90 = mcP90,
            fireSuccessRate = mcResult.successRate,
            isLoading = false,
            currentCurrency = metrics.currencyCode,
            fireNumber = fireNumber,
            yearsToFire = yearsToFire,
            whatIfYearsToFire = whatIfYearsToFire,
            emergencyFundTarget = emergencyFundTarget,
            emergencyFundMonths = efMonths,
            currentEmergencyFund = currentEmergencyFund,
            extraMonthlyContribution = extraContrib
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ForecastingState()
    )

    fun updateRoi(roi: Float) {
        _expectedRoi.value = roi
    }

    fun updateVolatility(vol: Float) {
        _volatility.value = vol
    }

    fun updateExtraContribution(amount: Long) {
        _extraMonthlyContribution.value = amount
    }

    fun updateProjectionYears(years: Int) {
        _projectionYears.value = years
    }

    fun updateEmergencyFundMonths(months: Int) {
        _emergencyFundMonths.value = months
    }

    private fun calculateProjections(
        initialValue: Long,
        monthlyContribution: Long,
        annualRoi: Float,
        years: Int
    ): List<ProjectionPoint> {
        val points = mutableListOf<ProjectionPoint>()
        val monthlyRoi = (1.0 + annualRoi).pow(1.0 / 12.0) - 1.0

        points.add(ProjectionPoint(0, initialValue))

        var currentValue = initialValue.toDouble()
        for (year in 1..years) {
            for (month in 1..12) {
                currentValue = (currentValue + monthlyContribution) * (1.0 + monthlyRoi)
            }
            points.add(ProjectionPoint(year, currentValue.toLong()))
        }
        return points
    }
}
