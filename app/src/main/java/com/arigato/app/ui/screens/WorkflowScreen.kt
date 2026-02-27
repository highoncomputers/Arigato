package com.arigato.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arigato.app.domain.entity.WorkflowExecutionStatus
import com.arigato.app.ui.components.WorkflowStepCard
import com.arigato.app.ui.viewmodel.WorkflowViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowScreen(
    onNavigateBack: () -> Unit,
    viewModel: WorkflowViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.workflow?.name ?: "Workflow",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val workflow = uiState.workflow
            if (workflow == null) {
                Text(
                    text = "Workflow not found",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
                return@Scaffold
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    WorkflowHeaderCard(
                        description = workflow.description,
                        stepCount = workflow.steps.size,
                        tags = workflow.tags
                    )
                }

                uiState.executionState?.let { execState ->
                    item {
                        WorkflowProgressBar(
                            progress = execState.progress,
                            status = execState.status
                        )
                    }

                    items(execState.steps.size) { idx ->
                        WorkflowStepCard(
                            stepNumber = idx + 1,
                            step = execState.steps[idx]
                        )
                    }
                } ?: run {
                    items(workflow.steps.size) { idx ->
                        val step = workflow.steps[idx]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${idx + 1}. ${step.toolName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                step.note?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (step.isOptional) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Optional step",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    uiState.error?.let { error ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (uiState.isRunning) {
                            Button(
                                onClick = viewModel::cancel,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Stop, null)
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("Stop Workflow")
                            }
                        } else {
                            Button(
                                onClick = { viewModel.runWorkflow() },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isRunning
                            ) {
                                Icon(Icons.Default.PlayArrow, null)
                                Spacer(modifier = Modifier.padding(4.dp))
                                Text("Run Workflow")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkflowHeaderCard(
    description: String,
    stepCount: Int,
    tags: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$stepCount steps · ${tags.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun WorkflowProgressBar(
    progress: Float,
    status: WorkflowExecutionStatus
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when (status) {
                    WorkflowExecutionStatus.RUNNING -> "Running…"
                    WorkflowExecutionStatus.COMPLETED -> "Completed"
                    WorkflowExecutionStatus.FAILED -> "Failed"
                    WorkflowExecutionStatus.CANCELLED -> "Cancelled"
                    else -> "Pending"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when (status) {
                    WorkflowExecutionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    WorkflowExecutionStatus.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = when (status) {
                WorkflowExecutionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                WorkflowExecutionStatus.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}
