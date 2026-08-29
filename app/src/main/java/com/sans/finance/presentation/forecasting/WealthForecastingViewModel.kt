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
    val projectionYears: Int = 20,
    val projections: List<ProjectionPoint> = emptyList(),
    val whatIfProjections: List<ProjectionPoint> = emptyList(),
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
    private val _projectionYears = MutableStateFlow(25)
    private val _emergencyFundMonths = MutableStateFlow(6)
    private val _extraMonthlyContribution = MutableStateFlow(0L)

    val state = combine(
        getWealthMetricsUseCase(),
        _expectedRoi,
        _projectionYears,
        _emergencyFundMonths,
        _extraMonthlyContribution
    ) { metrics, roi, years, efMonths, extraContrib ->
        val currentNetWorth = metrics.cashAssets + metrics.portfolioValue
        val projections = calculateProjections(currentNetWorth, metrics.monthlySavings, roi, years)
        val whatIfProjections = calculateProjections(currentNetWorth, metrics.monthlySavings + extraContrib, roi, years)

        val fireNumber = metrics.monthlyBurn * 12 * 25
        val yearsToFire = projections.find { it.value >= fireNumber }?.year
        val whatIfYearsToFire = whatIfProjections.find { it.value >= fireNumber }?.year

        val emergencyFundTarget = metrics.monthlyBurn * efMonths
        val currentEmergencyFund = metrics.cashAssets

        ForecastingState(
            currentNetWorth = currentNetWorth,
            monthlySavings = metrics.monthlySavings,
            monthlyExpenses = metrics.monthlyBurn,
            expectedRoi = roi,
            projectionYears = years,
            projections = projections,
            whatIfProjections = whatIfProjections,
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
