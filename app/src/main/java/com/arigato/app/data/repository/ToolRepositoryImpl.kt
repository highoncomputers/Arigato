package com.arigato.app.data.repository

import android.content.Context
import com.arigato.app.core.generator.SchemaBuilder
import com.arigato.app.data.local.datastore.ToolDataStore
import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.entity.ToolCategory
import com.arigato.app.domain.repository.IToolRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val toolDataStore: ToolDataStore,
    private val schemaBuilder: SchemaBuilder
) : IToolRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun getAllTools(): Flow<List<Tool>> = toolDataStore.getAllTools()

    override fun getToolsByCategory(category: ToolCategory): Flow<List<Tool>> =
        toolDataStore.getToolsByCategory(category.name)

    override fun getFavoriteTools(): Flow<List<Tool>> = toolDataStore.getFavoriteTools()

    override suspend fun getToolById(id: String): Tool? = toolDataStore.getToolById(id)

    override suspend fun searchTools(query: String): List<Tool> =
        toolDataStore.searchTools(query)

    override suspend fun setFavorite(toolId: String, isFavorite: Boolean) =
        toolDataStore.setFavorite(toolId, isFavorite)

    override suspend fun updateInstallStatus(toolId: String, isInstalled: Boolean) =
        toolDataStore.updateInstallStatus(toolId, isInstalled)

    override fun getToolCount(): Flow<Int> = toolDataStore.getToolCount()

    override suspend fun loadToolsFromAssets() {
        val toolsDir = "tools"
        val toolFiles = context.assets.list(toolsDir) ?: return
        val tools = toolFiles
            .filter { it.endsWith(".json") }
            .mapNotNull { fileName ->
                runCatching {
                    val jsonContent = context.assets.open("$toolsDir/$fileName")
                        .bufferedReader()
                        .use { it.readText() }
                    schemaBuilder.parseToolFromJson(jsonContent)
                }.getOrNull()
            }
        if (tools.isNotEmpty()) {
            toolDataStore.insertTools(tools)
        }
    }
}
