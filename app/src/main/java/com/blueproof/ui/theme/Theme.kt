package com.blueproof.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = NavyLight,
    onPrimaryContainer = SurfaceWhite,
    secondary = TealAccent,
    onSecondary = SurfaceWhite,
    secondaryContainer = TealLight,
    onSecondaryContainer = NavyDark,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = OutlineBorder,
    error = StatusRejected,
    onError = SurfaceWhite
)

@Composable
fun BlueProofTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
