package com.sans.expensetracker.data.local.entity

import androidx.room.*

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String // Icon name or resource string
)
