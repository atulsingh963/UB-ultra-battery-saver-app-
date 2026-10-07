package com.atul.ub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val DarkBackground = Color(0xFF0A0A0C)
val SurfaceDark = Color(0xFF141418)
val SurfaceVariantDark = Color(0xFF1E1E24)
val PurpleAccent = Color(0xFF9D4EDD)
val PurpleGlow = Color(0xFFC77DFF)
val TextPrimary = Color(0xFFF8F9FA)
val TextSecondary = Color(0xFFADB5BD)
val ActiveGreen = Color(0xFF38B000)

private val DarkColorScheme = darkColorScheme(
    primary = PurpleAccent,
    secondary = PurpleGlow,
    background = DarkBackground,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun UBTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
