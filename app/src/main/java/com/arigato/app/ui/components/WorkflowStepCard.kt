package com.arigato.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arigato.app.domain.entity.OutputLine
import com.arigato.app.domain.entity.WorkflowExecutionStatus
import com.arigato.app.domain.entity.WorkflowStepState
import com.arigato.app.ui.theme.TerminalGreen
import com.arigato.app.ui.theme.TerminalRed

@Composable
fun WorkflowStepCard(
    stepNumber: Int,
    step: WorkflowStepState,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(step.status == WorkflowExecutionStatus.RUNNING) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = step.outputLines.isNotEmpty()) { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = when (step.status) {
                WorkflowExecutionStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                WorkflowExecutionStatus.COMPLETED -> MaterialTheme.colorScheme.surface
                WorkflowExecutionStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StepStatusIcon(status = step.status)

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$stepNumber. ${step.toolName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (step.isOptional) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "optional",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    step.note?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (step.resolvedParams.isNotEmpty() && step.status == WorkflowExecutionStatus.RUNNING) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = step.resolvedParams.entries.joinToString(" ") { (k, v) -> "$k=$v" },
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                step.exitCode?.let { code ->
                    Text(
                        text = "exit $code",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (code == 0) TerminalGreen else TerminalRed
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded && step.outputLines.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(
                                color = Color(0xFF0D0D0D),
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            items(step.outputLines.take(MAX_VISIBLE_LINES)) { line ->
                                when (line) {
                                    is OutputLine.StdOut -> Text(
                                        text = line.content,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = TerminalGreen
                                    )
                                    is OutputLine.StdErr -> Text(
                                        text = line.content,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = TerminalRed
                                    )
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepStatusIcon(status: WorkflowExecutionStatus) {
    when (status) {
        WorkflowExecutionStatus.PENDING -> Icon(
            Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        WorkflowExecutionStatus.RUNNING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        WorkflowExecutionStatus.COMPLETED -> Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = TerminalGreen,
            modifier = Modifier.size(24.dp)
        )
        WorkflowExecutionStatus.FAILED -> Icon(
            Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )
        WorkflowExecutionStatus.PAUSED -> Icon(
            Icons.Default.PauseCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(24.dp)
        )
        WorkflowExecutionStatus.CANCELLED -> Icon(
            Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

private const val MAX_VISIBLE_LINES = 50
