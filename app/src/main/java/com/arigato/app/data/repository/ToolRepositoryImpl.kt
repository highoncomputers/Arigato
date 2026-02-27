package com.arigato.app.data.repository

import android.content.Context
import com.arigato.app.core.generator.SchemaBuilder
import com.arigato.app.data.local.database.dao.ToolDao
import com.arigato.app.data.local.database.entity.ToolEntity
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
    private val toolDao: ToolDao,
    private val schemaBuilder: SchemaBuilder
) : IToolRepository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    override fun getAllTools(): Flow<List<Tool>> =
        toolDao.getAllTools().map { entities -> entities.map { it.toDomain() } }

    override fun getToolsByCategory(category: ToolCategory): Flow<List<Tool>> =
        toolDao.getToolsByCategory(category.name).map { entities -> entities.map { it.toDomain() } }

    override fun getFavoriteTools(): Flow<List<Tool>> =
        toolDao.getFavoriteTools().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getToolById(id: String): Tool? =
        toolDao.getToolById(id)?.toDomain()

    override suspend fun searchTools(query: String): List<Tool> =
        toolDao.searchTools(query).map { it.toDomain() }

    override suspend fun setFavorite(toolId: String, isFavorite: Boolean) =
        toolDao.setFavorite(toolId, isFavorite)

    override suspend fun updateInstallStatus(toolId: String, isInstalled: Boolean) =
        toolDao.updateInstallStatus(toolId, isInstalled)

    override fun getToolCount(): Flow<Int> = toolDao.getToolCount()

    override suspend fun loadToolsFromAssets() {
        val toolsDir = "tools"
        val toolFiles = context.assets.list(toolsDir) ?: return
        val entities = toolFiles
            .filter { it.endsWith(".json") }
            .mapNotNull { fileName ->
                runCatching {
                    val jsonContent = context.assets.open("$toolsDir/$fileName")
                        .bufferedReader()
                        .use { it.readText() }
                    val tool = schemaBuilder.parseToolFromJson(jsonContent) ?: return@mapNotNull null
                    tool.toEntity()
                }.getOrNull()
            }
        if (entities.isNotEmpty()) {
            toolDao.insertTools(entities)
        }
    }

    private fun ToolEntity.toDomain(): Tool {
        val tool = runCatching {
            schemaBuilder.parseToolFromJson(schemaJson)
        }.getOrNull()
        return tool?.copy(
            isInstalled = isInstalled,
            isFavorite = isFavorite
        ) ?: Tool(
            id = id,
            name = name,
            packageName = packageName,
            description = description,
            category = category,
            isInstalled = isInstalled,
            isFavorite = isFavorite,
            requiresRoot = requiresRoot,
            tags = tags.split(",").filter { it.isNotBlank() }
        )
    }

    private fun Tool.toEntity(): ToolEntity = ToolEntity(
        id = id,
        name = name,
        packageName = packageName,
        description = description,
        category = category,
        schemaJson = schemaBuilder.buildJson(this),
        isInstalled = isInstalled,
        isFavorite = isFavorite,
        requiresRoot = requiresRoot,
        tags = tags.joinToString(","),
        lastUpdated = System.currentTimeMillis()
    )
}
