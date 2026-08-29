package com.sans.finance.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sans.finance.data.local.entity.InvestmentMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentMetadataDao {
    @Query("SELECT * FROM investment_metadata")
    fun getAllMetadata(): Flow<List<InvestmentMetadataEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetadata(metadata: InvestmentMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllMetadata(metadata: List<InvestmentMetadataEntity>)
}
