package com.sans.expensetracker.data.local.dao

import androidx.room.*
import com.sans.expensetracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int
}
