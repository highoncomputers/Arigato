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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.arigato.app.domain.entity.CommandExample
import com.arigato.app.domain.entity.Tool
import com.arigato.app.domain.entity.ToolSuggestion
import com.arigato.app.ui.components.CategoryChip
import com.arigato.app.ui.viewmodel.ToolDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToExecute: (String) -> Unit,
    onNavigateToTool: (String) -> Unit,
    viewModel: ToolDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.tool?.name ?: "Tool Details",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    uiState.tool?.let { tool ->
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (tool.isFavorite) Icons.Default.Favorite
                                else Icons.Default.FavoriteBorder,
                                contentDescription = "Toggle favorite",
                                tint = if (tool.isFavorite) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            uiState.tool?.let { tool ->
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToExecute(tool.id) },
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    text = { Text("Execute") }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error!!,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.tool != null -> {
                    ToolDetailContent(
                        tool = uiState.tool!!,
                        suggestions = uiState.suggestions,
                        onNavigateToTool = onNavigateToTool,
                        onExecute = { onNavigateToExecute(uiState.tool!!.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolDetailContent(
    tool: Tool,
    suggestions: List<ToolSuggestion>,
    onNavigateToTool: (String) -> Unit,
    onExecute: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ToolHeaderSection(tool)
        }

        if (tool.description.isNotBlank()) {
            item {
                InfoSection(title = "Description") {
                    Text(
                        text = tool.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (tool.useCases.isNotEmpty()) {
            item {
                InfoSection(title = "Use Cases") {
                    tool.useCases.forEach { useCase ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = useCase,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        if (tool.parameters.isNotEmpty()) {
            item {
                InfoSection(title = "Parameters (${tool.parameters.size})") {
                    tool.parameters.forEach { param ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = param.flag ?: param.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (param.isRequired) {
                                        Text(
                                            text = " *",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                param.description?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = param.type.lowercase(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }

        if (tool.examples.isNotEmpty()) {
            item {
                InfoSection(title = "Examples") {
                    tool.examples.take(5).forEach { example ->
                        CommandExampleItem(example = example)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        if (suggestions.isNotEmpty()) {
            item {
                InfoSection(title = "Related Tools") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(suggestions) { suggestion ->
                            SuggestionChip(
                                onClick = { onNavigateToTool(suggestion.tool.id) },
                                label = { Text(suggestion.tool.name) }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ToolHeaderSection(tool: Tool) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryChip(category = tool.toolCategory.displayName)
            if (tool.requiresRoot) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Root",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (tool.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tool.tags) { tag ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun CommandExampleItem(example: CommandExample) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D0D0D)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            example.description?.let { desc ->
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = example.command,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF00FF41)
            )
        }
    }
}
