package com.sans.expensetracker.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.domain.repository.ExpenseRepository
import com.sans.expensetracker.domain.repository.InstallmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class StatsState(
    val thisMonthSpent: Long = 0L,
    val lastMonthSpent: Long = 0L,
    val thisYearSpent: Long = 0L,
    val lastYearSpent: Long = 0L,
    val spendingByCategory: List<com.sans.expensetracker.data.local.entity.CategorySpent> = emptyList(),
    val dailySpending: List<com.sans.expensetracker.data.local.entity.DaySpent> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val installmentRepository: InstallmentRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        val now = Calendar.getInstance()
        
        // This Month
        val thisMonthStart = getStartOfMonth(now)
        
        // Last Month
        val lastMonthStart = (thisMonthStart.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
        val lastMonthEnd = thisMonthStart
        
        // This Year
        val thisYearStart = (thisMonthStart.clone() as Calendar).apply { set(Calendar.MONTH, 0); set(Calendar.DAY_OF_YEAR, 1) }
        
        // Last Year
        val lastYearStart = (thisYearStart.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
        val lastYearEnd = thisYearStart

        val thisMonthExpFlow = expenseRepository.getTotalSpentBetween(thisMonthStart.timeInMillis, Long.MAX_VALUE)
        val thisMonthInstFlow = installmentRepository.getTotalPaidAmountBetween(thisMonthStart.timeInMillis, Long.MAX_VALUE)

        val lastMonthExpFlow = expenseRepository.getTotalSpentBetween(lastMonthStart.timeInMillis, lastMonthEnd.timeInMillis)
        val lastMonthInstFlow = installmentRepository.getTotalPaidAmountBetween(lastMonthStart.timeInMillis, lastMonthEnd.timeInMillis)

        val thisYearExpFlow = expenseRepository.getTotalSpentBetween(thisYearStart.timeInMillis, Long.MAX_VALUE)
        val thisYearInstFlow = installmentRepository.getTotalPaidAmountBetween(thisYearStart.timeInMillis, Long.MAX_VALUE)

        val lastYearExpFlow = expenseRepository.getTotalSpentBetween(lastYearStart.timeInMillis, lastYearEnd.timeInMillis)
        val lastYearInstFlow = installmentRepository.getTotalPaidAmountBetween(lastYearStart.timeInMillis, lastYearEnd.timeInMillis)

        val spendingByCategoryFlow = expenseRepository.getSpendingByCategoryBetween(thisMonthStart.timeInMillis, Long.MAX_VALUE)
        val dailySpendingFlow = expenseRepository.getDailySpendingBetween(thisMonthStart.timeInMillis, Long.MAX_VALUE)

        combine(
            combine(thisMonthExpFlow, thisMonthInstFlow) { e, i -> (e ?: 0L) + (i ?: 0L) },
            combine(lastMonthExpFlow, lastMonthInstFlow) { e, i -> (e ?: 0L) + (i ?: 0L) },
            combine(thisYearExpFlow, thisYearInstFlow) { e, i -> (e ?: 0L) + (i ?: 0L) },
            combine(lastYearExpFlow, lastYearInstFlow) { e, i -> (e ?: 0L) + (i ?: 0L) },
            spendingByCategoryFlow,
            dailySpendingFlow
        ) { values ->
            val tm = values[0] as Long
            val lm = values[1] as Long
            val ty = values[2] as Long
            val ly = values[3] as Long
            @Suppress("UNCHECKED_CAST")
            val sbc = values[4] as List<com.sans.expensetracker.data.local.entity.CategorySpent>
            @Suppress("UNCHECKED_CAST")
            val ds = values[5] as List<com.sans.expensetracker.data.local.entity.DaySpent>

            StatsState(
                thisMonthSpent = tm,
                lastMonthSpent = lm,
                thisYearSpent = ty,
                lastYearSpent = ly,
                spendingByCategory = sbc,
                dailySpending = ds,
                isLoading = false
            )
        }.onEach { newState ->
            _state.value = newState
        }.launchIn(viewModelScope)
    }

    private fun getStartOfMonth(cal: Calendar): Calendar {
        return (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
