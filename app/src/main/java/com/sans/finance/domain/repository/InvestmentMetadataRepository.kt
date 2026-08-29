package com.sans.finance.domain.repository

import com.sans.finance.data.local.entity.InvestmentMetadataEntity
import kotlinx.coroutines.flow.Flow

interface InvestmentMetadataRepository {
    fun getAllMetadata(): Flow<List<InvestmentMetadataEntity>>
    suspend fun insertMetadata(metadata: InvestmentMetadataEntity)
    suspend fun insertAllMetadata(metadata: List<InvestmentMetadataEntity>)
}
