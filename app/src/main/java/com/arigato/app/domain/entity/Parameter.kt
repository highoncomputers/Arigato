package com.arigato.app.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class ParameterValidation(
    val pattern: String? = null,
    val hint: String? = null,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null
)

@Serializable
data class Parameter(
    val name: String,
    val flag: String? = null,
    val description: String? = null,
    val type: String = "TEXT",
    val isRequired: Boolean = false,
    val defaultValue: String? = null,
    val options: List<String>? = null,
    val validation: ParameterValidation? = null,
    val isPositional: Boolean = false
) {
    val parameterType: ParameterType
        get() = runCatching { ParameterType.valueOf(type) }.getOrDefault(ParameterType.TEXT)
}
