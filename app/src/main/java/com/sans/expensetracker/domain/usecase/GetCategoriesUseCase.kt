package com.sans.expensetracker.domain.usecase

import com.sans.expensetracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val dao: com.sans.expensetracker.data.local.dao.CategoryDao
) {
    operator fun invoke(): Flow<List<CategoryEntity>> {
        return dao.getAllCategories()
    }
}
