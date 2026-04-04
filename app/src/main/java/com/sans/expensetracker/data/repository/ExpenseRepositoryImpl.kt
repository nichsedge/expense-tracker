package com.sans.expensetracker.data.repository

import com.sans.expensetracker.data.local.dao.ExpenseDao
import com.sans.expensetracker.data.local.entity.ExpenseEntity
import com.sans.expensetracker.domain.model.Expense
import com.sans.expensetracker.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExpenseRepositoryImpl(
    private val dao: com.sans.expensetracker.data.local.dao.ExpenseDao,
    private val tagDao: com.sans.expensetracker.data.local.dao.TagDao,
    private val categoryDao: com.sans.expensetracker.data.local.dao.CategoryDao
) : ExpenseRepository {

    override fun getAllExpenses(): Flow<List<Expense>> {
        return dao.getAllExpenses().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getExpensesBetween(since: Long, until: Long): Flow<List<Expense>> {
        return dao.getExpensesBetween(since, until).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getExpenseById(id: Long): Expense? {
        return dao.getExpenseById(id)?.toDomain()
    }

    override suspend fun insertExpense(expense: Expense): Long {
        val expenseId = dao.insertExpense(expense.toEntity())
        syncTags(expenseId, expense.tags)
        return expenseId
    }

    override suspend fun updateExpense(expense: Expense) {
        dao.updateExpense(expense.toEntity())
        syncTags(expense.id, expense.tags)
    }

    private suspend fun syncTags(expenseId: Long, tagNames: List<String>) {
        dao.deleteExpenseTagRefs(expenseId)
        val crossRefs = tagNames.map { tagName ->
            val existingTag = tagDao.getTagByName(tagName)
            val tagId = existingTag?.id ?: tagDao.insertTag(com.sans.expensetracker.data.local.entity.TagEntity(name = tagName))
            com.sans.expensetracker.data.local.entity.ExpenseTagCrossRef(expenseId, tagId)
        }
        if (crossRefs.isNotEmpty()) {
            dao.insertExpenseTagCrossRefs(crossRefs)
        }
    }

    override suspend fun deleteExpense(expense: Expense) {
        dao.deleteExpense(expense.toEntity())
    }

    override fun getTotalSpentSince(since: Long): Flow<Long?> {
        return dao.getTotalSpentSince(since)
    }

    override fun getTotalSpentBetween(since: Long, until: Long): Flow<Long?> {
        return dao.getTotalSpentBetween(since, until)
    }

    override fun getAllTimeSpent(): Flow<Long?> {
        return dao.getAllTimeSpent()
    }

    override fun getAllTags(): Flow<List<String>> {
        return tagDao.getAllTags().map { entities ->
            entities.map { it.name }
        }
    }

    override fun getAllCategories(): Flow<List<com.sans.expensetracker.data.local.entity.CategoryEntity>> {
        return categoryDao.getAllCategories()
    }

    override suspend fun insertCategory(category: com.sans.expensetracker.data.local.entity.CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    override suspend fun updateCategory(category: com.sans.expensetracker.data.local.entity.CategoryEntity) {
        categoryDao.updateCategory(category)
    }

    override suspend fun deleteCategory(category: com.sans.expensetracker.data.local.entity.CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    override fun getAllTagEntities(): Flow<List<com.sans.expensetracker.data.local.entity.TagEntity>> {
        return tagDao.getAllTags()
    }

    override suspend fun updateTag(tag: com.sans.expensetracker.data.local.entity.TagEntity) {
        tagDao.updateTag(tag)
    }

    override suspend fun deleteTag(tag: com.sans.expensetracker.data.local.entity.TagEntity) {
        tagDao.deleteTag(tag)
    }

    // Internal mapping extension
    private fun com.sans.expensetracker.data.local.entity.ExpenseWithTags.toDomain(): Expense {
        return Expense(
            id = expense.id,
            date = expense.date,
            itemName = expense.itemName,
            amount = expense.finalPrice,
            categoryId = expense.categoryId,
            isRecurring = expense.isRecurring,
            isInstallment = expense.isInstallment,
            merchant = expense.merchant,
            tags = tags.map { it.name },
            quantity = expense.quantity
        )
    }

    private fun Expense.toEntity(): com.sans.expensetracker.data.local.entity.ExpenseEntity {
        return com.sans.expensetracker.data.local.entity.ExpenseEntity(
            id = id,
            date = date,
            itemName = itemName,
            finalPrice = amount,
            originalPrice = amount,
            categoryId = categoryId,
            isRecurring = isRecurring,
            isInstallment = isInstallment,
            merchant = merchant,
            platform = tags.firstOrNull(), // Keep for legacy if needed, or null
            quantity = quantity,
            status = "completed"
        )
    }
}
