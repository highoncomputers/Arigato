package com.arigato.app.domain.usecase

import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.repository.IToolRepository
import javax.inject.Inject

class GetToolUseCase @Inject constructor(
    private val toolRepository: IToolRepository
) {
    suspend operator fun invoke(toolId: String): Tool? =
        toolRepository.getToolById(toolId)
}
