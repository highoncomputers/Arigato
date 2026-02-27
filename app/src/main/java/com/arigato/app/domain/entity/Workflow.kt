package com.arigato.app.domain.entity

data class WorkflowStep(
    val toolId: String,
    val toolName: String,
    val suggestedParams: Map<String, String> = emptyMap(),
    val note: String? = null,
    val isOptional: Boolean = false
)

data class Workflow(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val steps: List<WorkflowStep>,
    val tags: List<String> = emptyList()
)

data class ToolSuggestion(
    val tool: Tool,
    val relevance: Float,
    val reason: String
)
