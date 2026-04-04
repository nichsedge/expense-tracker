package com.sans.expensetracker.domain.model

data class Installment(
    val id: Long = 0,
    val expenseId: Long,
    val totalAmount: Long,
    val monthlyPayment: Long,
    val durationMonths: Int,
    val remainingBalance: Long,
    val nextDueDate: Long,
    val status: String = "Active",
    val createdAt: Long = System.currentTimeMillis()
)
