package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================
// MODULE 2: ULTRA-LUXURY MULTI-THEME COLOR SYSTEM
// =========================================================

// --- THEME 1: "PEARL WHITE LUXURY" (Default Light) ---
// Background: Pure Pearl White (#FFFFFF, #F8FAFC)
val PearlWhiteBackground = Color(0xFFFFFFFF)
val PearlWhiteSubtle = Color(0xFFF8FAFC)
// Primary Accents: Deep Royal Emerald (#065F46, #047857)
val RoyalEmeraldPrimary = Color(0xFF065F46)
val RoyalEmeraldSecondary = Color(0xFF047857)
// Secondary Accents: Pure Platinum Gold (#D4AF37, #F59E0B)
val PlatinumGoldPrimary = Color(0xFFD4AF37)
val PlatinumGoldSecondary = Color(0xFFF59E0B)
// Surface & Cards: Soft Neumorphic floating white cards (#0D000000 shadow, 8dp)
val PearlWhiteSurface = Color(0xFFFFFFFF)
val PearlWhiteSurfaceVariant = Color(0xFFF1F5F9)
// Text: Slate Onyx (#0F172A) and Muted Titanium (#64748B)
val SlateOnyx = Color(0xFF0F172A)
val MutedTitanium = Color(0xFF64748B)

// --- THEME 2: "ROYAL MIDNIGHT EMERALD" (Dark Luxury) ---
// Background: Deep Obsidian Black (#050B0A, #0B1311)
val ObsidianBlackBackground = Color(0xFF050B0A)
val ObsidianBlackSurface = Color(0xFF0B1311)
// Primary Accents: Luminous Emerald Glow (#10B981, #059669)
val LuminousEmeraldGlow = Color(0xFF10B981)
val EmeraldGlowSecondary = Color(0xFF059669)
// Secondary Accents: Champagne Gold (#FBBF24)
val ChampagneGold = Color(0xFFFBBF24)
// Surface & Cards: Frosted Glass Dark Surfaces (#0F1F1C) with subtle 1.dp emerald borders
val FrostedDarkSurface = Color(0xFF0F1F1C)
// Text: Crisp Ivory (#F1F5F9) and Silver Mist (#94A3B8)
val CrispIvory = Color(0xFFF1F5F9)
val SilverMist = Color(0xFF94A3B8)

// --- THEME 3: "IMPERIAL VELVET SAPPHIRE" (Midnight Blue Luxury) ---
// Background: Deep Royal Navy (#030712, #0B1120)
val RoyalNavyBackground = Color(0xFF030712)
val RoyalNavySurface = Color(0xFF0B1120)
// Primary Accents: Electric Cyan & Ice Blue (#38BDF8, #0284C7)
val ElectricCyan = Color(0xFF38BDF8)
val IceBlue = Color(0xFF0284C7)
// Secondary Accents: Pure Platinum Silver (#E2E8F0)
val PlatinumSilver = Color(0xFFE2E8F0)
// Surface & Cards: Deep Midnight Slate (#0F172A) with subtle cyan glow borders
val MidnightSlateSurface = Color(0xFF0F172A)
// Text: Pure White (#FFFFFF) and Soft Slate (#94A3B8)
val SoftSlate = Color(0xFF94A3B8)

// --- Universal & Semantic Colors ---
val PureWhite = Color(0xFFFFFFFF)
val PearlBackground = PearlWhiteSubtle
val PearlSurface = PearlWhiteSurface
val PearlSurfaceVariant = PearlWhiteSurfaceVariant
val PearlSurfaceElevated = PearlWhiteSurface

// Accents (Aliases)
val DeepRoyalEmerald = RoyalEmeraldPrimary
val RadiantEmerald = RoyalEmeraldSecondary
val EmeraldMint = LuminousEmeraldGlow
val EmeraldSurfaceLight = Color(0xFFECFDF5)

val PlatinumGold = PlatinumGoldPrimary
val GoldenAmber = PlatinumGoldSecondary
val GoldenSun = Color(0xFFFEF3C7)

// Text & Structural
val CharcoalPrimary = SlateOnyx
val CharcoalSecondary = Color(0xFF334155)
val SlateMuted = MutedTitanium
val BorderSubtlePearl = Color(0xFFE2E8F0)
val BorderGoldAccent = Color(0x40D4AF37)
val BorderEmeraldAccent = Color(0x30065F46)

// Functional
val CrimsonError = Color(0xFFDC2626)
val SuccessGreen = Color(0xFF16A34A)
val NeumorphicShadowDark = Color(0x0D000000)
val NeumorphicShadowLight = Color(0x80FFFFFF)
val WatermarkPatternColor = Color(0x08065F46)

// Legacy Compatibility Aliases
val DarkNavyBackground = PearlBackground
val DarkNavySurface = PearlSurface
val DarkNavySurfaceVariant = PearlSurfaceVariant
val DarkNavySurfaceGlass = PureWhite
val GoldPrimary = PlatinumGold
val GoldSecondary = PlatinumGoldSecondary
val GoldTertiary = PlatinumGold
val GoldAccent = GoldenAmber
val DarkOnBackground = CharcoalPrimary
val DarkOnSurface = CharcoalPrimary
val DarkOnSurfaceSubtle = SlateMuted
val DarkBorderGold = BorderGoldAccent
val LightEmeraldBackground = PearlBackground
val LightEmeraldSurface = PureWhite
val LightEmeraldSurfaceVariant = PearlSurfaceVariant
val LightEmeraldSurfaceGlass = PureWhite
val EmeraldPrimary = DeepRoyalEmerald
val EmeraldSecondary = RadiantEmerald
val EmeraldTertiary = RadiantEmerald
val EmeraldAccent = EmeraldMint
val LightOnBackground = CharcoalPrimary
val LightOnSurface = CharcoalPrimary
val LightOnSurfaceSubtle = SlateMuted
val LightBorderEmerald = BorderEmeraldAccent
val SoftGold = GoldenSun
val GlassHighlight = PureWhite

