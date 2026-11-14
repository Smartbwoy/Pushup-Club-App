package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    secondary = AquaBlue,
    tertiary = FireOrange,
    background = PitchBlack,
    surface = DarkSurface,
    onPrimary = Color(0xFF003818),
    onSecondary = Color(0xFF00363D),
    onBackground = TextWhite,
    onSurface = TextWhite,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextGray
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
