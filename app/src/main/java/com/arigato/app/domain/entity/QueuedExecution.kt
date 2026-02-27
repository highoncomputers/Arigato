package com.arigato.app.domain.entity

enum class ExecutionPriority(val level: Int) {
    LOW(0),
    NORMAL(1),
    HIGH(2)
}

data class QueuedExecution(
    val id: String,
    val tool: Tool,
    val params: Map<String, String>,
    val priority: ExecutionPriority = ExecutionPriority.NORMAL,
    val enqueuedAt: Long = System.currentTimeMillis()
)

enum class WorkflowExecutionStatus {
    PENDING,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class WorkflowExecutionState(
    val workflowId: String,
    val workflowName: String,
    val steps: List<WorkflowStepState>,
    val status: WorkflowExecutionStatus = WorkflowExecutionStatus.PENDING,
    val startedAt: Long? = null,
    val completedAt: Long? = null
) {
    val currentStepIndex: Int
        get() = steps.indexOfFirst {
            it.status == WorkflowExecutionStatus.RUNNING ||
                    it.status == WorkflowExecutionStatus.PENDING
        }

    val progress: Float
        get() {
            if (steps.isEmpty()) return 0f
            val done = steps.count {
                it.status == WorkflowExecutionStatus.COMPLETED ||
                        it.status == WorkflowExecutionStatus.FAILED
            }
            return done.toFloat() / steps.size
        }
}

data class WorkflowStepState(
    val toolId: String,
    val toolName: String,
    val note: String?,
    val isOptional: Boolean,
    val status: WorkflowExecutionStatus = WorkflowExecutionStatus.PENDING,
    val resolvedParams: Map<String, String> = emptyMap(),
    val outputLines: List<OutputLine> = emptyList(),
    val exitCode: Int? = null
)
