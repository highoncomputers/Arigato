package com.arigato.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arigato.app.core.generator.SchemaBuilder
import com.arigato.app.domain.entity.Tool
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.toolDataStore: DataStore<Preferences> by preferencesDataStore(name = "tools")

@Singleton
class ToolDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val schemaBuilder: SchemaBuilder
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private object Keys {
        private const val TOOL_PREFIX = "tool_"
        private const val TOOL_COUNT_KEY = "tool_count"

        fun toolId(id: String) = stringPreferencesKey("$TOOL_PREFIX$id")
        val toolCount = stringPreferencesKey(TOOL_COUNT_KEY)
    }

    fun getAllTools(): Flow<List<Tool>> = context.toolDataStore.data.map { prefs ->
        prefs.asMap().keys
            .filterIsInstance<stringPreferencesKey>()
            .filter { it.name.startsWith("tool_") }
            .mapNotNull { key ->
                val jsonString = prefs[key]
                jsonString?.let { parseTool(it) }
            }
            .sortedBy { it.name }
    }

    fun getToolsByCategory(category: String): Flow<List<Tool>> = context.toolDataStore.data.map { prefs ->
        prefs.asMap().keys
            .filterIsInstance<stringPreferencesKey>()
            .filter { it.name.startsWith("tool_") }
            .mapNotNull { key ->
                val jsonString = prefs[key]
                jsonString?.let { parseTool(it) }
            }
            .filter { it.category == category }
            .sortedBy { it.name }
    }

    fun getFavoriteTools(): Flow<List<Tool>> = context.toolDataStore.data.map { prefs ->
        prefs.asMap().keys
            .filterIsInstance<stringPreferencesKey>()
            .filter { it.name.startsWith("tool_") }
            .mapNotNull { key ->
                val jsonString = prefs[key]
                jsonString?.let { parseTool(it) }
            }
            .filter { it.isFavorite }
            .sortedBy { it.name }
    }

    suspend fun getToolById(id: String): Tool? {
        val key = Keys.toolId(id)
        val jsonString = context.toolDataStore.data.map { it[key] }.firstOrNull()
        return jsonString?.let { parseTool(it) }
    }

    suspend fun searchTools(query: String): List<Tool> {
        val prefs = context.toolDataStore.data.firstOrNull() ?: return emptyList()
        val lowerQuery = query.lowercase()

        return prefs.asMap().keys
            .filterIsInstance<stringPreferencesKey>()
            .filter { it.name.startsWith("tool_") }
            .mapNotNull { key ->
                val jsonString = prefs[key]
                jsonString?.let { parseTool(it) }
            }
            .filter { tool ->
                tool.name.lowercase().contains(lowerQuery) ||
                        tool.description.lowercase().contains(lowerQuery) ||
                        tool.tags.any { it.lowercase().contains(lowerQuery) }
            }
            .sortedWith(compareBy(
                { !it.name.lowercase().startsWith(lowerQuery) },
                { it.name.lowercase() }
            ))
    }

    suspend fun insertTool(tool: Tool) {
        val jsonString = json.encodeToString(tool)
        context.toolDataStore.edit { it[Keys.toolId(tool.id)] = jsonString }
        updateCount()
    }

    suspend fun insertTools(tools: List<Tool>) {
        context.toolDataStore.edit { prefs ->
            tools.forEach { tool ->
                val jsonString = json.encodeToString(tool)
                prefs[Keys.toolId(tool.id)] = jsonString
            }
        }
        updateCount()
    }

    suspend fun updateTool(tool: Tool) {
        val jsonString = json.encodeToString(tool)
        context.toolDataStore.edit { it[Keys.toolId(tool.id)] = jsonString }
    }

    suspend fun setFavorite(toolId: String, isFavorite: Boolean) {
        val tool = getToolById(toolId) ?: return
        val updatedTool = tool.copy(isFavorite = isFavorite)
        updateTool(updatedTool)
    }

    suspend fun updateInstallStatus(toolId: String, isInstalled: Boolean) {
        val tool = getToolById(toolId) ?: return
        val updatedTool = tool.copy(isInstalled = isInstalled)
        updateTool(updatedTool)
    }

    fun getToolCount(): Flow<Int> = context.toolDataStore.data.map { prefs ->
        prefs[Keys.toolCount]?.toIntOrNull() ?: 0
    }

    suspend fun deleteAll() {
        context.toolDataStore.edit { prefs ->
            prefs.asMap().keys
                .filterIsInstance<stringPreferencesKey>()
                .filter { it.name.startsWith("tool_") }
                .forEach { key -> prefs.remove(key) }
            prefs.remove(Keys.toolCount)
        }
    }

    private suspend fun updateCount() {
        val count = context.toolDataStore.data.map { prefs ->
            prefs.asMap().keys
                .filterIsInstance<stringPreferencesKey>()
                .count { it.name.startsWith("tool_") }
        }.firstOrNull() ?: 0
        context.toolDataStore.edit { it[Keys.toolCount] = count.toString() }
    }

    private fun parseTool(jsonString: String): Tool? {
        return runCatching {
            json.decodeFromString<Tool>(jsonString)
        }.getOrNull()
    }
}
