package com.arigato.app.data.repository

import com.arigato.app.data.local.datastore.ExecutionDataStore
import com.arigato.app.domain.entity.ExecutionRecord
import com.arigato.app.domain.entity.ExecutionStatus
import com.arigato.app.domain.entity.OutputLine
import com.arigato.app.domain.repository.IExecutionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecutionRepositoryImpl @Inject constructor(
    private val executionDataStore: ExecutionDataStore
) : IExecutionRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val outputMutex = Mutex()
    private val outputBuffers = mutableMapOf<Long, MutableList<OutputLine>>()

    override fun getExecutionHistory(): Flow<List<ExecutionRecord>> =
        executionDataStore.getAllExecutions()

    override fun getExecutionsByTool(toolId: String): Flow<List<ExecutionRecord>> =
        executionDataStore.getExecutionsByTool(toolId)

    override suspend fun getExecutionById(id: Long): ExecutionRecord? =
        executionDataStore.getExecutionById(id)

    override suspend fun saveExecution(record: ExecutionRecord): Long {
        return executionDataStore.insertExecution(record)
    }

    override suspend fun updateExecutionStatus(id: Long, status: ExecutionStatus, exitCode: Int?, endTime: Long?) {
        executionDataStore.updateStatus(id, status.name, exitCode, endTime)
        if (status in listOf(ExecutionStatus.COMPLETED, ExecutionStatus.FAILED, ExecutionStatus.CANCELLED)) {
            flushOutputBuffer(id)
        }
    }

    override suspend fun appendOutput(id: Long, line: OutputLine) {
        outputMutex.withLock {
            val buffer = outputBuffers.getOrPut(id) { mutableListOf() }
            buffer.add(line)
            if (buffer.size >= 50 || line is OutputLine.Exit) {
                flushOutputBuffer(id)
            }
        }
    }

    override suspend fun clearHistory() {
        executionDataStore.deleteAll()
        outputBuffers.clear()
    }

    override suspend fun deleteExecution(id: Long) {
        executionDataStore.deleteById(id)
        outputBuffers.remove(id)
    }

    private suspend fun flushOutputBuffer(id: Long) {
        val lines = outputBuffers[id] ?: return
        if (lines.isEmpty()) return

        val existing = executionDataStore.getExecutionById(id)
        if (existing != null) {
            val existingLines = existing.outputLines
            val allLines = existingLines + lines
            val updatedRecord = existing.copy(outputLines = allLines)
            executionDataStore.updateExecution(updatedRecord)
        }
        outputBuffers[id]?.clear()
    }
}
