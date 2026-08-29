package com.sans.finance.domain.repository

import com.sans.finance.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: String): Flow<List<Category>>
    suspend fun insertCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun updateCategories(categories: List<Category>)
    suspend fun deleteCategory(category: Category)
}
