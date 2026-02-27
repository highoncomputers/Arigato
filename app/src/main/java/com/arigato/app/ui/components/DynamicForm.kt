package com.arigato.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arigato.app.core.generator.ValidationResult
import com.arigato.app.domain.entity.Parameter
import com.arigato.app.domain.entity.ParameterType
import com.arigato.app.domain.entity.Tool

@Composable
fun DynamicForm(
    tool: Tool,
    parameterValues: Map<String, String>,
    validationErrors: Map<String, ValidationResult>,
    onParameterChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val requiredParams = tool.parameters.filter { it.isRequired }
    val optionalParams = tool.parameters.filter { !it.isRequired }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (requiredParams.isNotEmpty()) {
            SectionCard(title = "Required Parameters") {
                requiredParams.forEach { param ->
                    ParameterInputField(
                        parameter = param,
                        value = parameterValues[param.name] ?: "",
                        onValueChange = { onParameterChange(param.name, it) },
                        validationResult = validationErrors[param.name],
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (optionalParams.isNotEmpty()) {
            SectionCard(title = "Optional Parameters") {
                optionalParams.forEach { param ->
                    ParameterInputField(
                        parameter = param,
                        value = parameterValues[param.name] ?: param.defaultValue ?: "",
                        onValueChange = { onParameterChange(param.name, it) },
                        validationResult = validationErrors[param.name],
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (tool.parameters.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = "This tool runs without additional parameters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
