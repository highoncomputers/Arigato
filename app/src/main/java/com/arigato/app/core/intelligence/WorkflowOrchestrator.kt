package com.arigato.app.core.intelligence

import com.arigato.app.core.output.OutputParser
import com.arigato.app.domain.entity.OutputLine
import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.entity.Workflow
import com.arigato.app.domain.entity.WorkflowExecutionState
import com.arigato.app.domain.entity.WorkflowExecutionStatus
import com.arigato.app.domain.entity.WorkflowStep
import com.arigato.app.domain.entity.WorkflowStepState
import com.arigato.app.domain.repository.IToolRepository
import com.arigato.app.domain.usecase.ExecuteToolUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.toList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkflowOrchestrator @Inject constructor(
    private val executeToolUseCase: ExecuteToolUseCase,
    private val toolRepository: IToolRepository,
    private val outputParser: OutputParser
) {
    private val _state = MutableStateFlow<WorkflowExecutionState?>(null)
    val state: StateFlow<WorkflowExecutionState?> = _state.asStateFlow()

    private var cancelled = false

    suspend fun run(workflow: Workflow, initialParams: Map<String, String> = emptyMap()) {
        cancelled = false
        val stepStates = workflow.steps.map { step ->
            WorkflowStepState(
                toolId = step.toolId,
                toolName = step.toolName,
                note = step.note,
                isOptional = step.isOptional,
                resolvedParams = step.suggestedParams
            )
        }

        _state.value = WorkflowExecutionState(
            workflowId = workflow.id,
            workflowName = workflow.name,
            steps = stepStates,
            status = WorkflowExecutionStatus.RUNNING,
            startedAt = System.currentTimeMillis()
        )

        var carryParams = initialParams.toMutableMap()

        workflow.steps.forEachIndexed { index, step ->
            if (cancelled) {
                updateStatus(WorkflowExecutionStatus.CANCELLED)
                return
            }

            val tool = toolRepository.getToolById(step.toolId)
            if (tool == null) {
                if (step.isOptional) {
                    markStep(index, WorkflowExecutionStatus.FAILED, emptyList(), null)
                    return@forEachIndexed
                } else {
                    markStep(index, WorkflowExecutionStatus.FAILED, emptyList(), null)
                    updateStatus(WorkflowExecutionStatus.FAILED)
                    return
                }
            }

            val resolvedParams = buildStepParams(step, carryParams)
            markStepRunning(index, resolvedParams)

            val outputLines = runStep(tool, resolvedParams)
            val exitCode = (outputLines.filterIsInstance<OutputLine.Exit>().firstOrNull())?.code

            val stepStatus = when {
                exitCode == 0 -> WorkflowExecutionStatus.COMPLETED
                step.isOptional -> WorkflowExecutionStatus.FAILED
                else -> WorkflowExecutionStatus.FAILED
            }

            markStep(index, stepStatus, outputLines, exitCode)

            if (stepStatus == WorkflowExecutionStatus.FAILED && !step.isOptional) {
                updateStatus(WorkflowExecutionStatus.FAILED)
                return
            }

            val stdoutLines = outputLines.filterIsInstance<OutputLine.StdOut>().map { it.content }
            val findings = outputParser.parse(stdoutLines)
            carryParams.putAll(outputParser.extractTargets(findings))
        }

        if (!cancelled) {
            updateStatus(WorkflowExecutionStatus.COMPLETED)
        }
    }

    fun cancel() {
        cancelled = true
    }

    private suspend fun runStep(tool: Tool, params: Map<String, String>): List<OutputLine> {
        return runCatching {
            val (_, flow) = executeToolUseCase.execute(tool, params)
            flow.toList()
        }.getOrDefault(listOf(OutputLine.Exit(-1)))
    }

    private fun buildStepParams(step: WorkflowStep, carry: Map<String, String>): Map<String, String> {
        val merged = mutableMapOf<String, String>()
        merged.putAll(carry)
        merged.putAll(step.suggestedParams)
        return merged
    }

    private fun markStep(
        index: Int,
        status: WorkflowExecutionStatus,
        outputLines: List<OutputLine>,
        exitCode: Int?
    ) {
        val current = _state.value ?: return
        val steps = current.steps.toMutableList()
        steps[index] = steps[index].copy(status = status, outputLines = outputLines, exitCode = exitCode)
        _state.value = current.copy(steps = steps)
    }

    private fun markStepRunning(index: Int, resolvedParams: Map<String, String>) {
        val current = _state.value ?: return
        val steps = current.steps.toMutableList()
        steps[index] = steps[index].copy(
            status = WorkflowExecutionStatus.RUNNING,
            resolvedParams = resolvedParams
        )
        _state.value = current.copy(steps = steps)
    }

    private fun updateStatus(status: WorkflowExecutionStatus) {
        _state.value = _state.value?.copy(
            status = status,
            completedAt = if (status != WorkflowExecutionStatus.RUNNING) System.currentTimeMillis() else null
        )
    }
}
