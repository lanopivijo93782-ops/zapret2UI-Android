package com.asterlike.zapret2ui.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Палитра как в оригинале Themes/Theme.xaml — тёмная, фиолетовый акцент
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8B7FF5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2A2550),
    secondary = Color(0xFF34D399),
    tertiary = Color(0xFFF5A623),
    background = Color(0xFF12151C),
    surface = Color(0xFF1A1E2A),
    surfaceVariant = Color(0xFF242938),
    onBackground = Color(0xFFE8E8F0),
    onSurface = Color(0xFFE8E8F0),
    error = Color(0xFFEF5350)
)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E5FF),
    secondary = Color(0xFF00B894),
    background = Color(0xFFF8F9FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F2F6),
    onBackground = Color(0xFF12151C),
    onSurface = Color(0xFF12151C)
)
private val AmoledScheme = darkColorScheme(
    primary = Color(0xFF8B7FF5),
    background = Color.Black,
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF1A1A1A),
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ZapretTheme(
    themeMode: String = "system",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = when (themeMode) {
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        "amoled" -> AmoledScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
