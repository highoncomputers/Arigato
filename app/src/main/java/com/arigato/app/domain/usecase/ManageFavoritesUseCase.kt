package com.arigato.app.domain.usecase

import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.repository.IToolRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageFavoritesUseCase @Inject constructor(
    private val toolRepository: IToolRepository
) {
    fun getFavorites(): Flow<List<Tool>> = toolRepository.getFavoriteTools()

    suspend fun toggleFavorite(tool: Tool) {
        toolRepository.setFavorite(tool.id, !tool.isFavorite)
    }

    suspend fun addFavorite(toolId: String) {
        toolRepository.setFavorite(toolId, true)
    }

    suspend fun removeFavorite(toolId: String) {
        toolRepository.setFavorite(toolId, false)
    }
}
