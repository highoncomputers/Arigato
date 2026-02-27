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
        var command = replaceFlagPlaceholders(template, parameters, params)

        parameters.forEach { param ->
            val value = params[param.name] ?: param.defaultValue
            if (!value.isNullOrBlank()) {
                val safeValue = sanitize(value)
                command = command.replace("{${param.name}}", safeValue)
            } else {
                command = command.replace("${param.flag} {${param.name}}", "")
                command = command.replace("{${param.name}}", "")
            }
        }
        command = command.replace(Regex("\\{[^}]+}"), "")
        return command.trim().replace(Regex("\\s+"), " ")
    }

    private fun replaceFlagPlaceholders(
        template: String,
        parameters: List<Parameter>,
        params: Map<String, String>
    ): String {
        val result = StringBuilder()
        var index = 0

        while (index < template.length) {
            val start = template.indexOf("{flag:", index)
            if (start == -1) {
                result.append(template.substring(index))
                break
            }
            result.append(template.substring(index, start))

            var cursor = start + 6
            var braceDepth = 1
            while (cursor < template.length && braceDepth > 0) {
                when (template[cursor]) {
                    '{' -> braceDepth++
                    '}' -> braceDepth--
                }
                cursor++
            }

            if (braceDepth != 0) {
                result.append(template.substring(start))
                break
            }

            val token = template.substring(start + 1, cursor - 1)
            val parts = token.split(":", limit = 3)
            val name = parts.getOrNull(1)
            val customTemplate = parts.getOrNull(2)

            val param = parameters.firstOrNull { it.name == name }
            val value = name?.let { params[it] ?: param?.defaultValue }
            val replacement = when {
                param == null -> ""
                param.parameterType == ParameterType.FLAG || param.parameterType == ParameterType.BOOLEAN -> {
                    if (value == "true" || value == "1" || value == "yes") {
                        customTemplate ?: param.flag.orEmpty()
                    } else {
                        ""
                    }
                }
                value.isNullOrBlank() -> ""
                customTemplate != null -> customTemplate.replace("{${param.name}}", sanitize(value))
                param.flag != null -> "${param.flag} ${quoteIfNeeded(sanitize(value))}"
                else -> sanitize(value)
            }

            result.append(replacement)
            index = cursor
        }

        return result.toString()
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
                param.parameterType == ParameterType.FLAG || param.parameterType == ParameterType.BOOLEAN -> {
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
