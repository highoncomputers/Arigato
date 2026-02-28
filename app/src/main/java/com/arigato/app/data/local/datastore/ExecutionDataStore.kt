package com.arigato.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arigato.app.domain.entity.ExecutionRecord
import com.arigato.app.domain.entity.ExecutionStatus
import com.arigato.app.domain.entity.OutputLine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.executionDataStore: DataStore<Preferences> by preferencesDataStore(name = "executions")

@Singleton
class ExecutionDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        private const val EXECUTION_PREFIX = "execution_"
        private const val NEXT_ID_KEY = "next_execution_id"

        fun executionId(id: Long) = stringPreferencesKey("$EXECUTION_PREFIX$id")
        val nextId = longPreferencesKey(NEXT_ID_KEY)
    }

    fun getAllExecutions(): Flow<List<ExecutionRecord>> = context.executionDataStore.data.map { prefs ->
        val records = mutableListOf<ExecutionRecord>()
        prefs.asMap().keys
            .filterIsInstance<stringPreferencesKey>()
            .filter { it.name.startsWith("execution_") }
            .mapNotNull { key ->
                val jsonString = prefs[key]
                jsonString?.let { parseExecutionRecord(it) }
            }
            .sortedByDescending { it.startTime }
    }

    fun getExecutionsByTool(toolId: String): Flow<List<ExecutionRecord>> = context.executionDataStore.data.map { prefs ->
        prefs.asMap().keys
            .filterIsInstance<stringPreferencesKey>()
            .filter { it.name.startsWith("execution_") }
            .mapNotNull { key ->
                val jsonString = prefs[key]
                jsonString?.let { parseExecutionRecord(it) }
            }
            .filter { it.toolId == toolId }
            .sortedByDescending { it.startTime }
    }

    suspend fun getExecutionById(id: Long): ExecutionRecord? {
        val key = Keys.executionId(id)
        val jsonString = context.executionDataStore.data.map { it[key] }.firstOrNull()
        return jsonString?.let { parseExecutionRecord(it) }
    }

    suspend fun insertExecution(record: ExecutionRecord): Long {
        val newId = context.executionDataStore.data.map { it[Keys.nextId] }.firstOrNull() ?: 1L
        val finalRecord = record.copy(id = newId)
        val jsonString = json.encodeToString(finalRecord)

        context.executionDataStore.edit { prefs ->
            prefs[Keys.executionId(newId)] = jsonString
            prefs[Keys.nextId] = newId + 1
        }
        return newId
    }

    suspend fun updateExecution(record: ExecutionRecord) {
        val jsonString = json.encodeToString(record)
        context.executionDataStore.edit { it[Keys.executionId(record.id)] = jsonString }
    }

    suspend fun updateStatus(id: Long, status: String, exitCode: Int?, endTime: Long?) {
        val record = getExecutionById(id) ?: return
        val updatedRecord = record.copy(
            status = runCatching { ExecutionStatus.valueOf(status) }.getOrDefault(ExecutionStatus.FAILED),
            exitCode = exitCode,
            endTime = endTime
        )
        updateExecution(updatedRecord)
    }

    suspend fun updateOutput(id: Long, outputJson: String) {
        val record = getExecutionById(id) ?: return
        val outputLines = runCatching {
            json.decodeFromString<List<OutputLine>>(outputJson)
        }.getOrDefault(emptyList())
        val updatedRecord = record.copy(outputLines = outputLines)
        updateExecution(updatedRecord)
    }

    suspend fun deleteAll() {
        context.executionDataStore.edit { prefs ->
            prefs.asMap().keys
                .filterIsInstance<stringPreferencesKey>()
                .filter { it.name.startsWith("execution_") }
                .forEach { key -> prefs.remove(key) }
        }
    }

    suspend fun deleteById(id: Long) {
        context.executionDataStore.edit { it.remove(Keys.executionId(id)) }
    }

    private fun parseExecutionRecord(jsonString: String): ExecutionRecord? {
        return runCatching {
            json.decodeFromString<ExecutionRecord>(jsonString)
        }.getOrNull()
    }
}
