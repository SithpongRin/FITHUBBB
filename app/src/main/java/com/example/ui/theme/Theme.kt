package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LimeAccent,
    onPrimary = CharcoalBackground,
    primaryContainer = LimeAccentDark,
    onPrimaryContainer = Color.White,
    secondary = LimeAccentSoft,
    onSecondary = CharcoalBackground,
    background = CharcoalBackground,
    onBackground = TextPrimaryDark,
    surface = CharcoalSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = CharcoalCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = LimeAccent,
    onPrimary = CharcoalBackground,
    primaryContainer = LimeAccentSoft,
    onPrimaryContainer = CharcoalBackground,
    secondary = Color(0xFF1E293B),
    onSecondary = Color.White,
    background = Color(0xFFF3F5F8),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE9EDF3),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFFD4DAE3),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun FithubTheme(
    darkTheme: Boolean = true, // Default to dark aesthetic matching reference dashboard
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FithubShapes,
        content = content
    )
}
