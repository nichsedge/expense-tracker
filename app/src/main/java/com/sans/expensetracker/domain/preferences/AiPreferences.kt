package com.sans.expensetracker.domain.preferences

import kotlinx.coroutines.flow.Flow

interface AiPreferences {
    fun getAiModelPath(): Flow<String?>
    suspend fun setAiModelPath(path: String)
}
