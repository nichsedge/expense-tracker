package com.sans.expensetracker.domain.model

data class Expense(
    val id: Long = 0,
    val date: Long,
    val itemName: String,
    val amount: Long,
    val categoryId: Long,
    val paymentMethod: String,
    val isRecurring: Boolean = false,
    val isInstallment: Boolean = false,
    val merchant: String? = null,
    val platform: String? = null,
    val quantity: Int = 1
)
