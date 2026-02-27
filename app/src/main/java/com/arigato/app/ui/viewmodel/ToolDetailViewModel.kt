package com.arigato.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arigato.app.core.intelligence.SuggestionEngine
import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.entity.ToolSuggestion
import com.arigato.app.domain.usecase.GetToolUseCase
import com.arigato.app.domain.usecase.ManageFavoritesUseCase
import com.arigato.app.domain.usecase.SearchToolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolDetailUiState(
    val tool: Tool? = null,
    val isLoading: Boolean = true,
    val suggestions: List<ToolSuggestion> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ToolDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getToolUseCase: GetToolUseCase,
    private val manageFavoritesUseCase: ManageFavoritesUseCase,
    private val searchToolsUseCase: SearchToolsUseCase,
    private val suggestionEngine: SuggestionEngine
) : ViewModel() {
    private val toolId: String = checkNotNull(savedStateHandle["toolId"])

    private val _uiState = MutableStateFlow(ToolDetailUiState())
    val uiState: StateFlow<ToolDetailUiState> = _uiState.asStateFlow()

    init {
        loadTool()
    }

    private fun loadTool() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val tool = getToolUseCase(toolId)
            if (tool != null) {
                val allTools = searchToolsUseCase.getAllTools().first()
                val suggestions = suggestionEngine.suggestNextTools(tool, allTools)
                _uiState.value = ToolDetailUiState(
                    tool = tool,
                    isLoading = false,
                    suggestions = suggestions
                )
            } else {
                _uiState.value = ToolDetailUiState(
                    isLoading = false,
                    error = "Tool not found"
                )
            }
        }
    }

    fun toggleFavorite() {
        val tool = _uiState.value.tool ?: return
        viewModelScope.launch {
            manageFavoritesUseCase.toggleFavorite(tool)
            _uiState.value = _uiState.value.copy(
                tool = tool.copy(isFavorite = !tool.isFavorite)
            )
        }
    }
}
