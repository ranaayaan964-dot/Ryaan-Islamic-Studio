package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkNavyGoldColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF1E1402),
    primaryContainer = Color(0xFF382D10),
    onPrimaryContainer = GoldSecondary,
    secondary = GoldSecondary,
    onSecondary = Color(0xFF221900),
    secondaryContainer = DarkNavySurfaceVariant,
    onSecondaryContainer = GoldAccent,
    tertiary = EmeraldAccent,
    onTertiary = Color.Black,
    background = DarkNavyBackground,
    onBackground = DarkOnBackground,
    surface = DarkNavySurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkNavySurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceSubtle,
    outline = DarkBorderGold,
    error = CrimsonError,
    onError = Color.White
)

private val LightEmeraldColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = GoldTertiary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = EmeraldTertiary,
    onTertiary = Color.White,
    background = LightEmeraldBackground,
    onBackground = LightOnBackground,
    surface = LightEmeraldSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightEmeraldSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceSubtle,
    outline = LightBorderEmerald,
    error = CrimsonError,
    onError = Color.White
)

val LocalThemeIsDark = staticCompositionLocalOf { true }

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkNavyGoldColorScheme else LightEmeraldColorScheme

    CompositionLocalProvider(LocalThemeIsDark provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
