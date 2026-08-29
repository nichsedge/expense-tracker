package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.InvestmentMetadataDao
import com.sans.finance.data.local.entity.InvestmentMetadataEntity
import com.sans.finance.domain.repository.InvestmentMetadataRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class InvestmentMetadataRepositoryImpl @Inject constructor(
    private val dao: InvestmentMetadataDao
) : InvestmentMetadataRepository {
    override fun getAllMetadata(): Flow<List<InvestmentMetadataEntity>> = dao.getAllMetadata()
    override suspend fun insertMetadata(metadata: InvestmentMetadataEntity) = dao.insertMetadata(metadata)
    override suspend fun insertAllMetadata(metadata: List<InvestmentMetadataEntity>) = dao.insertAllMetadata(metadata)
}
