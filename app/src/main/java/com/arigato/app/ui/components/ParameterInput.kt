package com.arigato.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.arigato.app.core.generator.ValidationResult
import com.arigato.app.domain.entity.Parameter
import com.arigato.app.domain.entity.ParameterType

@Composable
fun ParameterInputField(
    parameter: Parameter,
    value: String,
    onValueChange: (String) -> Unit,
    validationResult: ValidationResult? = null,
    modifier: Modifier = Modifier
) {
    when (parameter.parameterType) {
        ParameterType.SELECT -> SelectInputField(parameter, value, onValueChange, validationResult, modifier)
        ParameterType.FLAG, ParameterType.BOOLEAN -> FlagInputField(parameter, value, onValueChange, modifier)
        ParameterType.PASSWORD -> PasswordInputField(parameter, value, onValueChange, validationResult, modifier)
        ParameterType.FILE_PATH, ParameterType.WORDLIST -> FileInputField(parameter, value, onValueChange, validationResult, modifier)
        ParameterType.NUMBER, ParameterType.PORT -> NumberInputField(parameter, value, onValueChange, validationResult, modifier)
        else -> TextInputField(parameter, value, onValueChange, validationResult, modifier)
    }
}

@Composable
private fun TextInputField(
    parameter: Parameter,
    value: String,
    onValueChange: (String) -> Unit,
    validationResult: ValidationResult?,
    modifier: Modifier = Modifier
) {
    val keyboardType = when (parameter.parameterType) {
        ParameterType.IP_ADDRESS, ParameterType.CIDR -> KeyboardType.Ascii
        ParameterType.URL -> KeyboardType.Uri
        else -> KeyboardType.Text
    }
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    buildString {
                        append(parameter.label ?: parameter.name)
                        if (parameter.isRequired) append(" *")
                    }
                )
            },
            placeholder = (parameter.hint ?: parameter.validation?.hint)?.let { { Text(it) } },
            supportingText = {
                val msg = validationResult?.errorMessage ?: parameter.description
                if (msg != null) {
                    Text(
                        text = msg,
                        color = if (validationResult?.isValid == false)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            isError = validationResult?.isValid == false,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        parameter.flag?.let { flag ->
            Text(
                text = "Flag: $flag",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
private fun NumberInputField(
    parameter: Parameter,
    value: String,
    onValueChange: (String) -> Unit,
    validationResult: ValidationResult?,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.matches(Regex("[0-9,\\-]*"))) onValueChange(it) },
        label = {
            Text(
                buildString {
                    append(parameter.label ?: parameter.name)
                    if (parameter.isRequired) append(" *")
                }
            )
        },
        placeholder = (parameter.hint ?: parameter.validation?.hint)?.let { { Text(it) } },
        supportingText = {
            val msg = validationResult?.errorMessage ?: parameter.description
            if (msg != null) {
                Text(
                    text = msg,
                    color = if (validationResult?.isValid == false)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        isError = validationResult?.isValid == false,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordInputField(
    parameter: Parameter,
    value: String,
    onValueChange: (String) -> Unit,
    validationResult: ValidationResult?,
    modifier: Modifier = Modifier
) {
    var showPassword by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                buildString {
                    append(parameter.name)
                    if (parameter.isRequired) append(" *")
                }
            )
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (showPassword) "Hide password" else "Show password"
                )
            }
        },
        isError = validationResult?.isValid == false,
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun SelectInputField(
    parameter: Parameter,
    value: String,
    onValueChange: (String) -> Unit,
    validationResult: ValidationResult?,
    modifier: Modifier = Modifier
) {
    val options = parameter.options ?: return
    var expanded by remember { mutableStateOf(false) }
    val selectedValue = value.ifBlank { parameter.defaultValue ?: options.firstOrNull()?.value.orEmpty() }
    val selectedLabel = options.firstOrNull { it.value == selectedValue }?.label ?: selectedValue

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(parameter.label ?: parameter.name) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onValueChange(option.value)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FlagInputField(
    parameter: Parameter,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Checkbox(
            checked = value == "true",
            onCheckedChange = { checked -> onValueChange(if (checked) "true" else "false") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = parameter.label ?: parameter.name,
                style = MaterialTheme.typography.bodyMedium
            )
            parameter.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FileInputField(
    parameter: Parameter,
    value: String,
    onValueChange: (String) -> Unit,
    validationResult: ValidationResult?,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                buildString {
                    append(parameter.label ?: parameter.name)
                    if (parameter.isRequired) append(" *")
                }
            )
        },
        placeholder = { Text(parameter.hint ?: "/path/to/file") },
        trailingIcon = {
            Icon(Icons.Default.FolderOpen, contentDescription = "Browse file")
        },
        supportingText = parameter.description?.let { desc ->
            { Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        isError = validationResult?.isValid == false,
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}
