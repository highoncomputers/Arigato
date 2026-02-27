package com.arigato.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arigato.app.domain.entity.Workflow
import com.arigato.app.domain.entity.WorkflowExecutionState
import com.arigato.app.domain.entity.WorkflowExecutionStatus
import com.arigato.app.domain.usecase.RunWorkflowUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkflowUiState(
    val workflow: Workflow? = null,
    val executionState: WorkflowExecutionState? = null,
    val isRunning: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WorkflowViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runWorkflowUseCase: RunWorkflowUseCase
) : ViewModel() {
    private val workflowId: String = checkNotNull(savedStateHandle["workflowId"])

    private val _uiState = MutableStateFlow(WorkflowUiState())
    val uiState: StateFlow<WorkflowUiState> = _uiState.asStateFlow()

    init {
        loadWorkflow()
        observeExecution()
    }

    private fun loadWorkflow() {
        val workflow = runWorkflowUseCase.getWorkflow(workflowId)
        _uiState.value = _uiState.value.copy(workflow = workflow)
    }

    private fun observeExecution() {
        viewModelScope.launch {
            runWorkflowUseCase.executionState.collectLatest { state ->
                val isRunning = state?.status == WorkflowExecutionStatus.RUNNING
                _uiState.value = _uiState.value.copy(
                    executionState = state,
                    isRunning = isRunning
                )
            }
        }
    }

    fun runWorkflow(initialParams: Map<String, String> = emptyMap()) {
        val workflow = _uiState.value.workflow ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunning = true, error = null)
            runCatching {
                runWorkflowUseCase.run(workflow, initialParams)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isRunning = false,
                    error = e.message ?: "Workflow execution failed"
                )
            }
        }
    }

    fun cancel() {
        runWorkflowUseCase.cancel()
        _uiState.value = _uiState.value.copy(isRunning = false)
    }
}
