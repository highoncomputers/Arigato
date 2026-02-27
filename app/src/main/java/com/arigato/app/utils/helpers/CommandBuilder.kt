package com.arigato.app.utils.helpers

import com.arigato.app.domain.entity.Parameter
import com.arigato.app.domain.entity.ParameterType
import com.arigato.app.domain.entity.Tool
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandBuilder @Inject constructor() {
    fun build(tool: Tool, params: Map<String, String>): String {
        if (tool.commandTemplate.isNotBlank() && tool.commandTemplate.contains("{")) {
            return buildFromTemplate(tool.commandTemplate, tool.parameters, params)
        }
        return buildFromParameters(tool.packageName, tool.parameters, params)
    }

    private fun buildFromTemplate(template: String, parameters: List<Parameter>, params: Map<String, String>): String {
        var command = template
        parameters.forEach { param ->
            val value = params[param.name]
            if (value != null && value.isNotBlank()) {
                val safeValue = sanitize(value)
                if (param.flag != null) {
                    command = command.replace("{${param.name}}", safeValue)
                } else {
                    command = command.replace("{${param.name}}", safeValue)
                }
            } else {
                command = command.replace("${param.flag} {${param.name}}", "")
                command = command.replace("{${param.name}}", "")
            }
        }
        return command.trim().replace(Regex("\\s+"), " ")
    }

    private fun buildFromParameters(toolName: String, parameters: List<Parameter>, params: Map<String, String>): String {
        val parts = mutableListOf(toolName)
        val positionals = mutableListOf<String>()

        parameters.forEach { param ->
            val value = params[param.name] ?: param.defaultValue ?: return@forEach
            if (value.isBlank()) return@forEach

            val safeValue = sanitize(value)

            when {
                param.isPositional -> positionals.add(safeValue)
                param.parameterType == ParameterType.FLAG -> {
                    if (value == "true") param.flag?.let { parts.add(it) }
                }
                param.flag != null -> {
                    parts.add(param.flag)
                    parts.add(quoteIfNeeded(safeValue))
                }
                else -> positionals.add(safeValue)
            }
        }

        parts.addAll(positionals)
        return parts.joinToString(" ")
    }

    fun sanitize(value: String): String {
        return value.replace(Regex("[;`\$|&<>\\n\\r]"), "")
            .trim()
    }

    private fun quoteIfNeeded(value: String): String {
        return if (value.contains(" ")) "'$value'" else value
    }

    fun preview(tool: Tool, params: Map<String, String>): String {
        return build(tool, params)
    }
}
