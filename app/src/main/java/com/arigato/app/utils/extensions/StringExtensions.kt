package com.arigato.app.utils.extensions

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.truncate(maxLength: Int, suffix: String = "…"): String =
    if (length <= maxLength) this else take(maxLength) + suffix

fun String.toTitleCase(): String =
    split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }

fun Long.toFormattedTime(): String {
    val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return format.format(Date(this))
}

fun Long.toFormattedDateTime(): String {
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(Date(this))
}

fun Long.toDuration(): String {
    val seconds = this / 1000
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}

fun String.stripAnsiCodes(): String =
    replace(Regex("\\x1B\\[[0-9;]*[a-zA-Z]"), "")

fun String.isValidIpAddress(): Boolean =
    matches(Regex("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$")) &&
            split(".").all { it.toIntOrNull() in 0..255 }

fun String.isValidUrl(): Boolean =
    startsWith("http://") || startsWith("https://")
