package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * MODULE 2: DYNAMIC MULTI-THEME LUXURY DESIGN SYSTEM
 * 3 distinct ultra-luxury palettes:
 * - PEARL_WHITE: Pearl White Luxury (Default Light)
 * - MIDNIGHT_EMERALD: Royal Midnight Emerald (Dark Luxury)
 * - VELVET_SAPPHIRE: Imperial Velvet Sapphire (Midnight Blue Luxury)
 */
enum class ThemeType(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val previewPrimary: Color,
    val previewSecondary: Color,
    val previewBackground: Color,
    val isDark: Boolean
) {
    PEARL_WHITE(
        id = "pearl_white",
        displayName = "Pearl White Luxury",
        subtitle = "Royal Emerald & Pure Platinum Gold",
        previewPrimary = Color(0xFF065F46),
        previewSecondary = Color(0xFFD4AF37),
        previewBackground = Color(0xFFFFFFFF),
        isDark = false
    ),
    MIDNIGHT_EMERALD(
        id = "midnight_emerald",
        displayName = "Royal Midnight Emerald",
        subtitle = "Deep Obsidian Black & Luminous Emerald Glow",
        previewPrimary = Color(0xFF10B981),
        previewSecondary = Color(0xFFFBBF24),
        previewBackground = Color(0xFF050B0A),
        isDark = true
    ),
    VELVET_SAPPHIRE(
        id = "velvet_sapphire",
        displayName = "Imperial Velvet Sapphire",
        subtitle = "Deep Royal Navy & Electric Ice Blue",
        previewPrimary = Color(0xFF38BDF8),
        previewSecondary = Color(0xFFE2E8F0),
        previewBackground = Color(0xFF030712),
        isDark = true
    );

    val previewAccent: Color
        get() = previewSecondary
}
