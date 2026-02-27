package com.arigato.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.arigato.app.domain.entity.OutputLine
import com.arigato.app.ui.theme.MonoFontFamily
import com.arigato.app.ui.theme.TerminalGreen
import com.arigato.app.ui.theme.TerminalRed
import com.arigato.app.ui.theme.TerminalWhite
import com.arigato.app.utils.extensions.stripAnsiCodes
import com.arigato.app.utils.extensions.toFormattedTime

@Composable
fun OutputViewer(
    outputLines: List<OutputLine>,
    modifier: Modifier = Modifier,
    autoScroll: Boolean = true
) {
    val listState = rememberLazyListState()
    val shouldScroll by remember(outputLines.size) {
        derivedStateOf { autoScroll && outputLines.isNotEmpty() }
    }

    LaunchedEffect(outputLines.size) {
        if (shouldScroll && outputLines.isNotEmpty()) {
            listState.animateScrollToItem(outputLines.size - 1)
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF0D0D0D))
            .fillMaxSize()
    ) {
        if (outputLines.isEmpty()) {
            Text(
                text = "$ _",
                color = TerminalGreen,
                fontFamily = MonoFontFamily,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(outputLines, key = { it.timestamp }) { line ->
                    OutputLineItem(line)
                }
            }
        }
    }
}

@Composable
private fun OutputLineItem(line: OutputLine) {
    val horizontalScroll = rememberScrollState()
    when (line) {
        is OutputLine.StdOut -> {
            Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                Text(
                    text = formatLine(line.content),
                    color = TerminalGreen,
                    fontFamily = MonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
        is OutputLine.StdErr -> {
            Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                Text(
                    text = line.content.stripAnsiCodes(),
                    color = TerminalRed,
                    fontFamily = MonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
        is OutputLine.SystemMessage -> {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF888888))) {
                        append("[${line.timestamp.toFormattedTime()}] ")
                    }
                    withStyle(SpanStyle(color = Color(0xFF00BCD4))) {
                        append(line.content)
                    }
                },
                fontFamily = MonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
        is OutputLine.Exit -> {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF888888))) {
                        append("\n[Process exited with code ")
                    }
                    withStyle(
                        SpanStyle(
                            color = if (line.code == 0) TerminalGreen else TerminalRed
                        )
                    ) {
                        append(line.code.toString())
                    }
                    withStyle(SpanStyle(color = Color(0xFF888888))) {
                        append("]")
                    }
                },
                fontFamily = MonoFontFamily,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

private fun formatLine(content: String): String = content.stripAnsiCodes()

fun copyOutputToClipboard(context: Context, outputLines: List<OutputLine>) {
    val text = outputLines
        .filterIsInstance<OutputLine.StdOut>()
        .joinToString("\n") { it.content }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Arigato Output", text))
}
