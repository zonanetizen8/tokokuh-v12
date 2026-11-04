package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TokokuhDarkColorScheme = darkColorScheme(
    primary = TokokuhAccent,
    onPrimary = Color(0xFF1E1E1E),
    primaryContainer = Color(0xFF4A3B05),
    onPrimaryContainer = Color(0xFFFFE082),
    secondary = TokokuhAccent,
    onSecondary = Color(0xFF1E1E1E),
    secondaryContainer = TokokuhSecondary,
    onSecondaryContainer = TokokuhTextPrimary,
    background = TokokuhBackground,
    onBackground = TokokuhTextPrimary,
    surface = TokokuhCard,
    onSurface = TokokuhTextPrimary,
    surfaceVariant = TokokuhSurfaceVariant,
    onSurfaceVariant = TokokuhOnSurfaceVariant,
    outline = TokokuhBorder,
    error = TokokuhError,
    onError = Color.White
)

@Composable
fun TokokuhTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TokokuhDarkColorScheme,
        typography = Typography,
        content = content
    )
}
