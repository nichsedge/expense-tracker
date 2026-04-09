package com.sans.expensetracker.data.local.entity

import androidx.room.ColumnInfo

data class InstallmentPaymentRow(
    val id: Long,
    val date: Long,
    @ColumnInfo(name = "item_name") val itemName: String,
    val amount: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    val merchant: String?
)
