package com.arigato.app.domain.usecase

import com.arigato.app.core.intelligence.WorkflowOrchestrator
import com.arigato.app.core.intelligence.SuggestionEngine
import com.arigato.app.domain.entity.Workflow
import com.arigato.app.domain.entity.WorkflowExecutionState
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class RunWorkflowUseCase @Inject constructor(
    private val orchestrator: WorkflowOrchestrator,
    private val suggestionEngine: SuggestionEngine
) {
    val executionState: StateFlow<WorkflowExecutionState?> = orchestrator.state

    suspend fun run(workflowId: String, initialParams: Map<String, String> = emptyMap()) {
        val workflow = suggestionEngine.getAllWorkflows().find { it.id == workflowId } ?: return
        orchestrator.run(workflow, initialParams)
    }

    suspend fun run(workflow: Workflow, initialParams: Map<String, String> = emptyMap()) {
        orchestrator.run(workflow, initialParams)
    }

    fun cancel() {
        orchestrator.cancel()
    }

    fun getAllWorkflows(): List<Workflow> = suggestionEngine.getAllWorkflows()

    fun getWorkflow(id: String): Workflow? = suggestionEngine.getAllWorkflows().find { it.id == id }
}
