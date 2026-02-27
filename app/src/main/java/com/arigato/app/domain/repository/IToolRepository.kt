package com.arigato.app.domain.repository

import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.entity.ToolCategory
import kotlinx.coroutines.flow.Flow

interface IToolRepository {
    fun getAllTools(): Flow<List<Tool>>
    fun getToolsByCategory(category: ToolCategory): Flow<List<Tool>>
    fun getFavoriteTools(): Flow<List<Tool>>
    suspend fun getToolById(id: String): Tool?
    suspend fun searchTools(query: String): List<Tool>
    suspend fun setFavorite(toolId: String, isFavorite: Boolean)
    suspend fun updateInstallStatus(toolId: String, isInstalled: Boolean)
    suspend fun loadToolsFromAssets()
    fun getToolCount(): Flow<Int>
}
