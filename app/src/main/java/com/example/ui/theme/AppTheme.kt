package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * MODULE 2: DYNAMIC MULTI-THEME LUXURY DESIGN SYSTEM
 * Ultra-luxury palettes: Pearl White, Royal Midnight Emerald, Imperial Velvet Sapphire
 */

@Immutable
data class GlassThemeColors(
    val background: Color,
    val surface: Color,
    val glassCardBackground: Color,
    val glassCardBorder: List<Color>,
    val glassCardStrokeColor: Color,
    val primary: Color,
    val onPrimary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentGold: Color,
    val accentEmerald: Color,
    val chipBackground: Color,
    val chipBorder: Color,
    val navBarBackground: Color,
    val navBarBorder: Color,
    val isDark: Boolean,
    val elevationDp: Int = 8
)

// --- THEME 1: "PEARL WHITE LUXURY" (Default Light) ---
val PearlWhiteThemeColors = GlassThemeColors(
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    glassCardBackground = Color(0xF5FFFFFF),
    glassCardBorder = listOf(
        Color(0x35065F46),
        Color(0x15065F46),
        Color(0x25D4AF37)
    ),
    glassCardStrokeColor = Color(0x20065F46),
    primary = Color(0xFF065F46),
    onPrimary = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF334155),
    textMuted = Color(0xFF64748B),
    accentGold = Color(0xFFD4AF37),
    accentEmerald = Color(0xFF047857),
    chipBackground = Color(0x18065F46),
    chipBorder = Color(0x30065F46),
    navBarBackground = Color(0xF8FFFFFF),
    navBarBorder = Color(0x33D4AF37),
    isDark = false,
    elevationDp = 8
)

// --- THEME 2: "ROYAL MIDNIGHT EMERALD" (Dark Luxury) ---
val MidnightEmeraldThemeColors = GlassThemeColors(
    background = Color(0xFF050B0A),
    surface = Color(0xFF0F1F1C),
    glassCardBackground = Color(0xE60F1F1C),
    glassCardBorder = listOf(
        Color(0x6010B981),
        Color(0x2510B981),
        Color(0x35FBBF24)
    ),
    glassCardStrokeColor = Color(0x4010B981),
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF050B0A),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    accentGold = Color(0xFFFBBF24),
    accentEmerald = Color(0xFF059669),
    chipBackground = Color(0x2610B981),
    chipBorder = Color(0x4D10B981),
    navBarBackground = Color(0xF2050B0A),
    navBarBorder = Color(0x4010B981),
    isDark = true,
    elevationDp = 12
)

// --- THEME 3: "IMPERIAL VELVET SAPPHIRE" (Midnight Blue Luxury) ---
val VelvetSapphireThemeColors = GlassThemeColors(
    background = Color(0xFF030712),
    surface = Color(0xFF0F172A),
    glassCardBackground = Color(0xE60F172A),
    glassCardBorder = listOf(
        Color(0x6038BDF8),
        Color(0x2038BDF8),
        Color(0x35E2E8F0)
    ),
    glassCardStrokeColor = Color(0x4038BDF8),
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF030712),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    accentGold = Color(0xFFE2E8F0),
    accentEmerald = Color(0xFF0284C7),
    chipBackground = Color(0x2638BDF8),
    chipBorder = Color(0x4D38BDF8),
    navBarBackground = Color(0xF2030712),
    navBarBorder = Color(0x4038BDF8),
    isDark = true,
    elevationDp = 12
)

// Aliases for backward compatibility
val LightGlassThemeColors = PearlWhiteThemeColors
val DarkGlassThemeColors = MidnightEmeraldThemeColors
val BrownGlassThemeColors = MidnightEmeraldThemeColors
val BlackGlassThemeColors = VelvetSapphireThemeColors

val LocalGlassTheme = staticCompositionLocalOf { PearlWhiteThemeColors }
val LocalThemeType = staticCompositionLocalOf { ThemeType.PEARL_WHITE }

// Backward compatibility alias
typealias AppTheme = ThemeType
val LocalAppTheme = LocalThemeType

object AppGlassTheme {
    val colors: GlassThemeColors
        @Composable
        @ReadOnlyComposable
        get() = LocalGlassTheme.current

    val currentTheme: ThemeType
        @Composable
        @ReadOnlyComposable
        get() = LocalThemeType.current
}
