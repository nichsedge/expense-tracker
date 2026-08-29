package com.sans.finance.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investment_metadata")
data class InvestmentMetadataEntity(
    @PrimaryKey val code: String, // e.g. "ST010T4", "ST012T4"
    val rate: Double,             // e.g. 0.0640
    val type: String = "SUKUK",   // "SUKUK", "BOND"
    val updatedAt: Long = System.currentTimeMillis()
)
