package com.arigato.app.core.generator

import com.arigato.app.domain.entity.Parameter
import com.arigato.app.domain.entity.ParameterType
import javax.inject.Inject
import javax.inject.Singleton

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

@Singleton
class InputValidator @Inject constructor() {
    fun validate(parameter: Parameter, value: String): ValidationResult {
        if (value.isBlank()) {
            return if (parameter.isRequired) {
                ValidationResult(false, "${parameter.name} is required")
            } else {
                ValidationResult(true)
            }
        }

        if (containsDangerousChars(value)) {
            return ValidationResult(false, "Input contains invalid characters")
        }

        parameter.validation?.pattern?.let { pattern ->
            if (!value.matches(Regex(pattern))) {
                return ValidationResult(
                    false,
                    parameter.validation.hint ?: "Invalid format for ${parameter.name}"
                )
            }
        }

        val typeResult = validateByType(parameter.parameterType, value, parameter)
        if (!typeResult.isValid) return typeResult

        return ValidationResult(true)
    }

    fun validateAll(parameters: List<Parameter>, values: Map<String, String>): Map<String, ValidationResult> {
        return parameters.associate { param ->
            param.name to validate(param, values[param.name] ?: "")
        }
    }

    private fun validateByType(type: ParameterType, value: String, param: Parameter): ValidationResult {
        return when (type) {
            ParameterType.IP_ADDRESS -> validateIpAddress(value)
            ParameterType.URL -> validateUrl(value)
            ParameterType.PORT -> validatePort(value, param)
            ParameterType.PORT_RANGE -> validatePortRange(value)
            ParameterType.NUMBER -> validateNumber(value, param)
            ParameterType.CIDR -> validateCidr(value)
            else -> ValidationResult(true)
        }
    }

    private fun validateIpAddress(value: String): ValidationResult {
        val cidrPattern = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}(?:/[0-9]{1,2})?$")
        if (!value.matches(cidrPattern)) {
            return ValidationResult(false, "Invalid IP address format. Use: 192.168.1.1 or 192.168.0.0/24")
        }
        val octets = value.split("/").first().split(".")
        if (octets.any { it.toIntOrNull() == null || it.toInt() > 255 }) {
            return ValidationResult(false, "IP address octets must be 0-255")
        }
        return ValidationResult(true)
    }

    private fun validateUrl(value: String): ValidationResult {
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            return ValidationResult(false, "URL must start with http:// or https://")
        }
        return ValidationResult(true)
    }

    private fun validatePort(value: String, param: Parameter): ValidationResult {
        val portRangePattern = Regex("^[0-9]+([-,][0-9]+)*$")
        if (!value.matches(portRangePattern)) {
            return ValidationResult(false, "Invalid port format. Use: 80, 443, or 1-1000")
        }
        return ValidationResult(true)
    }

    private fun validatePortRange(value: String): ValidationResult {
        val rangePattern = Regex("^(\\d{1,5})-(\\d{1,5})$")
        val match = rangePattern.find(value) ?: return ValidationResult(false, "Invalid port range. Use: 1-1000")
        val start = match.groupValues[1].toInt()
        val end = match.groupValues[2].toInt()
        if (start > end) return ValidationResult(false, "Start port must be less than end port")
        if (end > 65535) return ValidationResult(false, "Port cannot exceed 65535")
        return ValidationResult(true)
    }

    private fun validateNumber(value: String, param: Parameter): ValidationResult {
        val num = value.toDoubleOrNull()
            ?: return ValidationResult(false, "${param.name} must be a number")
        param.validation?.minValue?.let { min ->
            if (num < min) return ValidationResult(false, "${param.name} must be at least $min")
        }
        param.validation?.maxValue?.let { max ->
            if (num > max) return ValidationResult(false, "${param.name} must be at most $max")
        }
        return ValidationResult(true)
    }

    private fun validateCidr(value: String): ValidationResult {
        val cidrPattern = Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}/[0-9]{1,2}$")
        if (!value.matches(cidrPattern)) {
            return ValidationResult(false, "Invalid CIDR. Use: 192.168.0.0/24")
        }
        val prefix = value.split("/").last().toIntOrNull() ?: 0
        if (prefix !in 0..32) return ValidationResult(false, "CIDR prefix must be 0-32")
        return ValidationResult(true)
    }

    private fun containsDangerousChars(value: String): Boolean {
        val dangerous = listOf(";", "&&", "||", "|", "`", "\$(",  "$(", "\n", "\r", "$()")
        return dangerous.any { value.contains(it) }
    }
}
