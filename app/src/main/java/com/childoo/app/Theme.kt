package com.childoo.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF1E2266)
val Gold = Color(0xFFFFC145)
val Cream = Color(0xFFFBF8F3)
val Mint = Color(0xFF17917D)
val Coral = Color(0xFFE5674F)

private val Colors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    secondary = Mint,
    onSecondary = Color.White,
    tertiary = Gold,
    onTertiary = Navy,
    background = Cream,
    onBackground = Navy,
    surface = Color.White,
    onSurface = Navy,
    surfaceVariant = Color(0xFFF1ECE3),
    onSurfaceVariant = Color(0xFF4A4E7A),
    error = Coral,
)

@Composable
fun ChildooTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
