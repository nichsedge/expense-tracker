package com.sans.finance.data.repository

import com.sans.finance.data.local.dao.GoalDao
import com.sans.finance.data.local.entity.GoalEntity
import com.sans.finance.domain.model.Goal
import com.sans.finance.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {
    override fun getAllGoals(): Flow<List<Goal>> = goalDao.getAllGoals().map { list -> list.map { it.toDomain() } }
    override suspend fun getGoalById(id: Long): Goal? = goalDao.getGoalById(id)?.toDomain()
    override suspend fun insertGoal(goal: Goal): Long = goalDao.insertGoal(goal.toEntity())
    override suspend fun updateGoal(goal: Goal) = goalDao.updateGoal(goal.toEntity())
    override suspend fun deleteGoal(goal: Goal) = goalDao.deleteGoal(goal.toEntity())

    private fun GoalEntity.toDomain() = Goal(
        id = id,
        name = name,
        targetAmount = targetAmount,
        targetType = targetType,
        targetName = targetName,
        currency = currency,
        deadline = deadline,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Goal.toEntity() = GoalEntity(
        id = id,
        name = name,
        targetAmount = targetAmount,
        targetType = targetType,
        targetName = targetName,
        currency = currency,
        deadline = deadline,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
