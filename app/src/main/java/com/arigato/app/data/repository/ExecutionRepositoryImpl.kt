package com.arigato.app.data.repository

import com.arigato.app.data.local.database.dao.ExecutionDao
import com.arigato.app.data.local.database.entity.ExecutionEntity
import com.arigato.app.domain.entity.ExecutionRecord
import com.arigato.app.domain.entity.ExecutionStatus
import com.arigato.app.domain.entity.OutputLine
import com.arigato.app.domain.repository.IExecutionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecutionRepositoryImpl @Inject constructor(
    private val executionDao: ExecutionDao
) : IExecutionRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val outputMutex = Mutex()
    private val outputBuffers = mutableMapOf<Long, MutableList<OutputLine>>()

    override fun getExecutionHistory(): Flow<List<ExecutionRecord>> =
        executionDao.getAllExecutions().map { entities -> entities.map { it.toDomain() } }

    override fun getExecutionsByTool(toolId: String): Flow<List<ExecutionRecord>> =
        executionDao.getExecutionsByTool(toolId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getExecutionById(id: Long): ExecutionRecord? =
        executionDao.getExecutionById(id)?.toDomain()

    override suspend fun saveExecution(record: ExecutionRecord): Long {
        val entity = record.toEntity()
        return executionDao.insertExecution(entity)
    }

    override suspend fun updateExecutionStatus(id: Long, status: ExecutionStatus, exitCode: Int?, endTime: Long?) {
        executionDao.updateStatus(id, status.name, exitCode, endTime)
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
        executionDao.deleteAll()
        outputBuffers.clear()
    }

    override suspend fun deleteExecution(id: Long) {
        executionDao.deleteById(id)
        outputBuffers.remove(id)
    }

    private suspend fun flushOutputBuffer(id: Long) {
        val lines = outputBuffers[id] ?: return
        if (lines.isEmpty()) return

        val existing = executionDao.getExecutionById(id)
        if (existing != null) {
            val existingLines = runCatching {
                json.decodeFromString<List<OutputLine>>(existing.outputJson)
            }.getOrDefault(emptyList())

            val allLines = existingLines + lines
            val newJson = json.encodeToString(allLines)
            executionDao.updateOutput(id, newJson)
        }
        outputBuffers[id]?.clear()
    }

    private fun ExecutionEntity.toDomain(): ExecutionRecord {
        val params = runCatching {
            json.decodeFromString<Map<String, String>>(parametersJson)
        }.getOrDefault(emptyMap())
        val outputLines = runCatching {
            json.decodeFromString<List<OutputLine>>(outputJson)
        }.getOrDefault(emptyList())

        return ExecutionRecord(
            id = id,
            toolId = toolId,
            toolName = toolName,
            command = command,
            parameters = params,
            startTime = startTime,
            endTime = endTime,
            status = runCatching { ExecutionStatus.valueOf(status) }.getOrDefault(ExecutionStatus.FAILED),
            exitCode = exitCode,
            outputLines = outputLines
        )
    }

    private fun ExecutionRecord.toEntity(): ExecutionEntity = ExecutionEntity(
        id = id,
        toolId = toolId,
        toolName = toolName,
        command = command,
        parametersJson = json.encodeToString(parameters),
        startTime = startTime,
        endTime = endTime,
        status = status.name,
        exitCode = exitCode,
        outputJson = json.encodeToString(outputLines)
    )
}
