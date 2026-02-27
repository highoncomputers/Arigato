package com.arigato.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ArigatoGreen,
    onPrimary = Color.Black,
    primaryContainer = ArigatoDarkGreen,
    onPrimaryContainer = ArigatoGreen,
    secondary = TerminalCyan,
    onSecondary = Color.Black,
    background = ArigatoBackground,
    onBackground = TerminalWhite,
    surface = ArigatoSurface,
    onSurface = TerminalWhite,
    surfaceVariant = ArigatoSurfaceVariant,
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = TerminalRed,
    onError = Color.White,
    outline = Color(0xFF404040)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006400),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F5B2),
    onPrimaryContainer = Color(0xFF002200),
    secondary = Color(0xFF0097A7),
    onSecondary = Color.White,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun ArigatoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ArigatoTypography,
        content = content
    )
}
