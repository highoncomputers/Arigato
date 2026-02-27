package com.arigato.app.domain.repository

import com.arigato.app.domain.entity.ExecutionRecord
import com.arigato.app.domain.entity.ExecutionStatus
import com.arigato.app.domain.entity.OutputLine
import kotlinx.coroutines.flow.Flow

interface IExecutionRepository {
    fun getExecutionHistory(): Flow<List<ExecutionRecord>>
    fun getExecutionsByTool(toolId: String): Flow<List<ExecutionRecord>>
    suspend fun getExecutionById(id: Long): ExecutionRecord?
    suspend fun saveExecution(record: ExecutionRecord): Long
    suspend fun updateExecutionStatus(id: Long, status: ExecutionStatus, exitCode: Int? = null, endTime: Long? = null)
    suspend fun appendOutput(id: Long, line: OutputLine)
    suspend fun clearHistory()
    suspend fun deleteExecution(id: Long)
}
