package com.arigato.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arigato.app.domain.entity.ExecutionStatus
import com.arigato.app.ui.components.DynamicForm
import com.arigato.app.ui.components.OutputViewer
import com.arigato.app.ui.components.copyOutputToClipboard
import com.arigato.app.ui.viewmodel.ExecutionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExecutionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = uiState.tool?.name ?: "Execute",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        uiState.executionStatus?.let { status ->
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(status = status)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.outputLines.isNotEmpty()) {
                        IconButton(onClick = {
                            copyOutputToClipboard(context, uiState.outputLines)
                        }) {
                            Icon(Icons.Default.ContentCopy, "Copy output")
                        }
                        IconButton(onClick = viewModel::clearOutput) {
                            Icon(Icons.Default.Clear, "Clear output")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val tool = uiState.tool ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(if (uiState.outputLines.isNotEmpty()) 0.45f else 1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!uiState.isTermuxAvailable) {
                    TermuxWarningBanner()
                }

                if (tool.requiresRoot) {
                    RootWarningBanner()
                }

                DynamicForm(
                    tool = tool,
                    parameterValues = uiState.parameterValues,
                    validationErrors = uiState.validationErrors,
                    onParameterChange = viewModel::updateParameter
                )

                if (uiState.commandPreview.isNotBlank()) {
                    CommandPreviewCard(command = uiState.commandPreview)
                }

                ExecutionControls(
                    isRunning = uiState.executionStatus == ExecutionStatus.RUNNING,
                    isTermuxAvailable = uiState.isTermuxAvailable,
                    onExecute = viewModel::execute,
                    onStop = viewModel::terminate,
                    onLaunchTermux = viewModel::launchInTermux
                )
            }

            if (uiState.outputLines.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    text = "Output (${uiState.outputLines.size} lines)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutputViewer(
                    outputLines = uiState.outputLines,
                    modifier = Modifier.weight(0.55f)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ExecutionStatus) {
    val (color, label) = when (status) {
        ExecutionStatus.RUNNING -> MaterialTheme.colorScheme.primary to "Running"
        ExecutionStatus.COMPLETED -> Color(0xFF4CAF50) to "Done"
        ExecutionStatus.FAILED -> MaterialTheme.colorScheme.error to "Failed"
        ExecutionStatus.CANCELLED -> MaterialTheme.colorScheme.tertiary to "Stopped"
        else -> MaterialTheme.colorScheme.secondary to status.name
    }
    Badge(containerColor = color) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun CommandPreviewCard(command: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF0D0D0D),
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp)
    ) {
        Text(
            text = "Command Preview",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF888888)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$ $command",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF00FF41),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ExecutionControls(
    isRunning: Boolean,
    isTermuxAvailable: Boolean,
    onExecute: () -> Unit,
    onStop: () -> Unit,
    onLaunchTermux: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isRunning) {
            Button(
                onClick = onStop,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Stop")
            }
        } else {
            Button(
                onClick = onExecute,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Execute")
            }
        }

        if (isTermuxAvailable) {
            FilledTonalButton(
                onClick = onLaunchTermux,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.OpenInNew, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("In Termux")
            }
        }
    }
}

@Composable
private fun TermuxWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Termux Not Found",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Install Termux from F-Droid for tool execution",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun RootWarningBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⚠ This tool requires root (superuser) access",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
