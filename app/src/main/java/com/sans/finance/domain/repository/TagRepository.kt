package com.sans.finance.domain.repository

import com.sans.finance.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun getAllTags(): Flow<List<String>>
    fun getVisibleTags(): Flow<List<String>>
    fun getAllTagEntities(): Flow<List<Tag>>
    suspend fun updateTag(tag: Tag)
    suspend fun updateTags(tags: List<Tag>)
    suspend fun deleteTag(tag: Tag)
    suspend fun cleanOrphanedTags()
    suspend fun syncTagsForExpense(expenseId: Long, tagNames: List<String>)
}
