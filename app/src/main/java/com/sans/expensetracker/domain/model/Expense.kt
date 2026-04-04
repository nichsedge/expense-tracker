package com.sans.expensetracker.domain.model

data class Expense(
    val id: Long = 0,
    val date: Long,
    val itemName: String,
    val amount: Long,
    val categoryId: Long,
    val isRecurring: Boolean = false,
    val isInstallment: Boolean = false,
    val merchant: String? = null,
    val tags: List<String> = emptyList(),
    val quantity: Int = 1
)
