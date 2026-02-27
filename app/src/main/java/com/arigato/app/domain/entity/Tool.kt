package com.arigato.app.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class CommandExample(
    val command: String,
    val description: String? = null
)

@Serializable
data class Tool(
    val id: String,
    val name: String,
    val packageName: String,
    val description: String,
    val useCases: List<String> = emptyList(),
    val category: String = ToolCategory.OTHER.name,
    val parameters: List<Parameter> = emptyList(),
    val commandTemplate: String = "",
    val examples: List<CommandExample> = emptyList(),
    val requiresRoot: Boolean = false,
    val isInstalled: Boolean = false,
    val isFavorite: Boolean = false,
    val version: String? = null,
    val author: String? = null,
    val website: String? = null,
    val tags: List<String> = emptyList()
) {
    val toolCategory: ToolCategory
        get() = ToolCategory.fromString(category)
}
