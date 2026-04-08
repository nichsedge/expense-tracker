package com.sans.expensetracker.presentation.recurring

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sans.expensetracker.domain.model.Expense
import com.sans.expensetracker.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecurringExpensesViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _expenses = expenseRepository.getAllExpenses()

    val state = combine(
        _expenses,
        expenseRepository.getAllCategories()
    ) { expenses, categories ->
        val recurringExpenses = expenses.filter { it.isRecurring }
        RecurringExpensesState(
            recurringExpenses = recurringExpenses,
            categories = categories,
            totalMonthlyRecurring = recurringExpenses.sumOf { it.amount }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecurringExpensesState()
    )
}

data class RecurringExpensesState(
    val recurringExpenses: List<Expense> = emptyList(),
    val categories: List<com.sans.expensetracker.data.local.entity.CategoryEntity> = emptyList(),
    val totalMonthlyRecurring: Long = 0L
)
