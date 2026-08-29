package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.CategoryDao
import com.sans.finance.data.local.entity.CategoryEntity
import com.sans.finance.domain.model.Category
import com.sans.finance.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCategoriesByType(type: String): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(type).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun updateCategories(categories: List<Category>) {
        categoryDao.updateCategories(categories.map { it.toEntity() })
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }

    private fun CategoryEntity.toDomain() = Category(
        id = id,
        name = name,
        icon = icon,
        orderIndex = orderIndex,
        type = type
    )

    private fun Category.toEntity() = CategoryEntity(
        id = id,
        name = name,
        icon = icon,
        orderIndex = orderIndex,
        type = type
    )
}
