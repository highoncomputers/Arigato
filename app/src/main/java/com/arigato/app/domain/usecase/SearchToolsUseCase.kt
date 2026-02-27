package com.arigato.app.domain.usecase

import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.entity.ToolCategory
import com.arigato.app.domain.repository.IToolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchToolsUseCase @Inject constructor(
    private val toolRepository: IToolRepository
) {
    fun getAllTools(): Flow<List<Tool>> = toolRepository.getAllTools()

    fun getByCategory(category: ToolCategory): Flow<List<Tool>> =
        toolRepository.getToolsByCategory(category)

    suspend fun search(query: String): List<Tool> =
        toolRepository.searchTools(query)

    fun getFavorites(): Flow<List<Tool>> = toolRepository.getFavoriteTools()
}
