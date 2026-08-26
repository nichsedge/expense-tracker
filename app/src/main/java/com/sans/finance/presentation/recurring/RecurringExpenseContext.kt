package com.sans.finance.presentation.recurring

import com.sans.finance.domain.model.Expense

data class RecurringExpenseContext(
    val expense: Expense,
    val categoryName: String? = null,
    val accountName: String? = null,
    val currencyCode: String = "USD"
)
