package com.sans.expensetracker.data.local.entity

import androidx.room.*

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // timestamp
    val platform: String?,
    val merchant: String?,
    @ColumnInfo(name = "item_name") val itemName: String,
    val quantity: Int,
    @ColumnInfo(name = "original_price") val originalPrice: Long,
    @ColumnInfo(name = "final_price") val finalPrice: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,
    val status: String,
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean,
    @ColumnInfo(name = "is_installment") val isInstallment: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
