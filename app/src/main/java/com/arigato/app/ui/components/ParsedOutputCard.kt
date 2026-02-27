package com.arigato.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arigato.app.core.output.ParsedFindings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParsedOutputCard(
    findings: ParsedFindings,
    modifier: Modifier = Modifier
) {
    if (findings.isEmpty) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Extracted Findings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            if (findings.openPorts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FindingSection(title = "Open Ports") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        findings.openPorts.forEach { port ->
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = "${port.port}/${port.protocol} (${port.service})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = Color(0xFF1A3A1A)
                                )
                            )
                        }
                    }
                }
            }

            if (findings.ipAddresses.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FindingSection(title = "IP Addresses") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        findings.ipAddresses.take(10).forEach { ip ->
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = ip,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (findings.cveIds.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FindingSection(title = "CVEs") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        findings.cveIds.take(10).forEach { cve ->
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = cve,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                )
                            )
                        }
                    }
                }
            }

            if (findings.urls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FindingSection(title = "URLs") {
                    findings.urls.take(5).forEach { url ->
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (findings.hashes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FindingSection(title = "Hashes") {
                    findings.hashes.take(5).forEach { hash ->
                        Text(
                            text = "${hash.type.name}: ${hash.value}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FindingSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
    )
    Spacer(modifier = Modifier.height(4.dp))
    content()
}
