package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.TagDao
import com.sans.finance.data.local.entity.TagEntity
import com.sans.finance.domain.model.Tag
import com.sans.finance.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepositoryImpl(
    private val tagDao: TagDao,
    private val expenseDao: com.sans.finance.data.local.dao.ExpenseDao
) : TagRepository {

    override fun getAllTags(): Flow<List<String>> {
        return tagDao.getAllTags().map { entities ->
            entities.map { it.name }
        }
    }

    override fun getVisibleTags(): Flow<List<String>> {
        return tagDao.getVisibleTags().map { entities ->
            entities.map { it.name }
        }
    }

    override fun getAllTagEntities(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateTag(tag: Tag) {
        tagDao.updateTag(tag.toEntity())
    }

    override suspend fun updateTags(tags: List<Tag>) {
        tagDao.updateTags(tags.map { it.toEntity() })
    }

    override suspend fun deleteTag(tag: Tag) {
        tagDao.deleteTag(tag.toEntity())
    }

    override suspend fun cleanOrphanedTags() {
        tagDao.deleteOrphanedTags()
    }

    override suspend fun syncTagsForExpense(expenseId: Long, tagNames: List<String>) {
        expenseDao.deleteExpenseTagRefs(expenseId)
        val crossRefs = tagNames.map { tagName ->
            val existingTag = tagDao.getTagByName(tagName)
            val tagId = existingTag?.id
                ?: tagDao.insertTag(TagEntity(name = tagName))
            com.sans.finance.data.local.entity.ExpenseTagCrossRef(expenseId, tagId)
        }
        if (crossRefs.isNotEmpty()) {
            expenseDao.insertExpenseTagCrossRefs(crossRefs)
        }
    }

    private fun TagEntity.toDomain() = Tag(
        id = id,
        name = name,
        orderIndex = orderIndex,
        isVisible = isVisible
    )

    private fun Tag.toEntity() = TagEntity(
        id = id,
        name = name,
        orderIndex = orderIndex,
        isVisible = isVisible
    )
}
