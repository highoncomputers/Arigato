package com.arigato.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arigato.app.domain.entity.ExecutionRecord
import com.arigato.app.domain.repository.IExecutionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val executions: List<ExecutionRecord> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val executionRepository: IExecutionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            executionRepository.getExecutionHistory().collect { executions ->
                _uiState.value = HistoryUiState(executions = executions, isLoading = false)
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch { executionRepository.clearHistory() }
    }

    fun deleteExecution(id: Long) {
        viewModelScope.launch { executionRepository.deleteExecution(id) }
    }
}
