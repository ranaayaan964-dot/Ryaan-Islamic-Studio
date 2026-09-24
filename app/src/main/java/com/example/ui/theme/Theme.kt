package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =========================================================
// MODULE 2: MATERIAL 3 COLOR SCHEMES FOR 3 LUXURY PALETTES
// =========================================================

// THEME 1: "PEARL WHITE LUXURY" (Default Light)
private val PearlWhiteColorScheme = lightColorScheme(
    primary = RoyalEmeraldPrimary, // #065F46
    onPrimary = PureWhite,
    primaryContainer = EmeraldSurfaceLight,
    onPrimaryContainer = RoyalEmeraldPrimary,
    secondary = PlatinumGoldPrimary, // #D4AF37
    onSecondary = PureWhite,
    secondaryContainer = GoldenSun,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = RoyalEmeraldSecondary, // #047857
    onTertiary = PureWhite,
    background = PearlWhiteBackground, // #FFFFFF
    onBackground = SlateOnyx, // #0F172A
    surface = PearlWhiteSurface,
    onSurface = SlateOnyx,
    surfaceVariant = PearlWhiteSurfaceVariant,
    onSurfaceVariant = MutedTitanium, // #64748B
    outline = BorderGoldAccent,
    error = CrimsonError,
    onError = PureWhite
)

// THEME 2: "ROYAL MIDNIGHT EMERALD" (Dark Luxury)
private val MidnightEmeraldColorScheme = darkColorScheme(
    primary = LuminousEmeraldGlow, // #10B981
    onPrimary = Color(0xFF050B0A),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = ChampagneGold, // #FBBF24
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = EmeraldGlowSecondary, // #059669
    onTertiary = PureWhite,
    background = ObsidianBlackBackground, // #050B0A
    onBackground = CrispIvory, // #F1F5F9
    surface = FrostedDarkSurface, // #0F1F1C
    onSurface = CrispIvory,
    surfaceVariant = Color(0xFF132A26),
    onSurfaceVariant = SilverMist, // #94A3B8
    outline = Color(0x5010B981),
    error = CrimsonError,
    onError = PureWhite
)

// THEME 3: "IMPERIAL VELVET SAPPHIRE" (Midnight Blue Luxury)
private val VelvetSapphireColorScheme = darkColorScheme(
    primary = ElectricCyan, // #38BDF8
    onPrimary = Color(0xFF030712),
    primaryContainer = Color(0xFF0369A1),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = PlatinumSilver, // #E2E8F0
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = IceBlue, // #0284C7
    onTertiary = PureWhite,
    background = RoyalNavyBackground, // #030712
    onBackground = PureWhite,
    surface = MidnightSlateSurface, // #0F172A
    onSurface = PureWhite,
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = SoftSlate, // #94A3B8
    outline = Color(0x5038BDF8),
    error = CrimsonError,
    onError = PureWhite
)

val LocalThemeIsDark = staticCompositionLocalOf { false }

@Composable
fun MyApplicationTheme(
    theme: ThemeType = ThemeType.PEARL_WHITE,
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val glassColors = when (theme) {
        ThemeType.PEARL_WHITE -> PearlWhiteThemeColors
        ThemeType.MIDNIGHT_EMERALD -> MidnightEmeraldThemeColors
        ThemeType.VELVET_SAPPHIRE -> VelvetSapphireThemeColors
    }

    val materialColorScheme = when (theme) {
        ThemeType.PEARL_WHITE -> PearlWhiteColorScheme
        ThemeType.MIDNIGHT_EMERALD -> MidnightEmeraldColorScheme
        ThemeType.VELVET_SAPPHIRE -> VelvetSapphireColorScheme
    }

    CompositionLocalProvider(
        LocalThemeType provides theme,
        LocalGlassTheme provides glassColors,
        LocalThemeIsDark provides glassColors.isDark
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}
