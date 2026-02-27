package com.arigato.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arigato.app.data.local.datastore.UserPreferences
import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.entity.ToolCategory
import com.arigato.app.domain.usecase.ManageFavoritesUseCase
import com.arigato.app.domain.usecase.SearchToolsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToolListUiState(
    val tools: List<Tool> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: ToolCategory? = null,
    val isLoading: Boolean = false,
    val showFavoritesOnly: Boolean = false
)

@HiltViewModel
class ToolListViewModel @Inject constructor(
    private val searchToolsUseCase: SearchToolsUseCase,
    private val manageFavoritesUseCase: ManageFavoritesUseCase,
    private val userPreferences: UserPreferences
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<ToolCategory?>(null)
    private val _showFavoritesOnly = MutableStateFlow(false)
    private val _isLoading = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val _tools = _selectedCategory.flatMapLatest { category ->
        if (category != null) {
            searchToolsUseCase.getByCategory(category)
        } else {
            searchToolsUseCase.getAllTools()
        }
    }

    @OptIn(FlowPreview::class)
    val uiState: StateFlow<ToolListUiState> = combine(
        _tools,
        _searchQuery.debounce(300),
        _selectedCategory,
        _isLoading,
        _showFavoritesOnly
    ) { tools, query, category, loading, favoritesOnly ->
        val filtered = when {
            favoritesOnly -> tools.filter { it.isFavorite }
            query.isNotBlank() -> tools.filter { tool ->
                tool.name.contains(query, ignoreCase = true) ||
                        tool.description.contains(query, ignoreCase = true) ||
                        tool.tags.any { it.contains(query, ignoreCase = true) }
            }
            else -> tools
        }
        ToolListUiState(
            tools = filtered,
            searchQuery = query,
            selectedCategory = category,
            isLoading = loading,
            showFavoritesOnly = favoritesOnly
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ToolListUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: ToolCategory?) {
        _selectedCategory.value = category
    }

    fun setShowFavoritesOnly(show: Boolean) {
        _showFavoritesOnly.value = show
        if (show) _selectedCategory.value = null
    }

    fun toggleFavorite(tool: Tool) {
        viewModelScope.launch {
            manageFavoritesUseCase.toggleFavorite(tool)
        }
    }
}
