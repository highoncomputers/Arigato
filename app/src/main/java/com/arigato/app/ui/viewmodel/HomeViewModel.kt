package com.arigato.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arigato.app.core.intelligence.SuggestionEngine
import com.arigato.app.data.local.datastore.UserPreferences
import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.entity.Workflow
import com.arigato.app.domain.repository.IToolRepository
import com.arigato.app.domain.usecase.SearchToolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val recentTools: List<Tool> = emptyList(),
    val favoriteTools: List<Tool> = emptyList(),
    val featuredWorkflows: List<Workflow> = emptyList(),
    val totalToolCount: Int = 0,
    val isDisclaimerAccepted: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val toolRepository: IToolRepository,
    private val searchToolsUseCase: SearchToolsUseCase,
    private val suggestionEngine: SuggestionEngine,
    private val userPreferences: UserPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadToolsIfNeeded()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                searchToolsUseCase.getFavorites(),
                userPreferences.recentTools,
                userPreferences.isDisclaimerAccepted,
                toolRepository.getToolCount()
            ) { favorites, recentIds, disclaimerAccepted, toolCount ->
                val allTools = searchToolsUseCase.getAllTools().first()
                val recentTools = recentIds
                    .mapNotNull { id -> allTools.find { it.id == id } }
                    .take(5)
                HomeUiState(
                    recentTools = recentTools,
                    favoriteTools = favorites.take(5),
                    featuredWorkflows = suggestionEngine.getAllWorkflows().take(4),
                    totalToolCount = toolCount,
                    isDisclaimerAccepted = disclaimerAccepted,
                    isLoading = false
                )
            }.collectLatest { state ->
                _uiState.value = state
            }
        }
    }

    private fun loadToolsIfNeeded() {
        viewModelScope.launch {
            val count = toolRepository.getToolCount().first()
            if (count == 0) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                toolRepository.loadToolsFromAssets()
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch {
            userPreferences.setDisclaimerAccepted(true)
        }
    }
}
