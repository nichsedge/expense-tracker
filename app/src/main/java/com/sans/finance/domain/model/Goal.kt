package com.sans.finance.domain.model

data class Goal(
    val id: Long = 0,
    val name: String,
    val targetAmount: Long,
    val targetType: String,
    val targetName: String?,
    val currency: String,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
