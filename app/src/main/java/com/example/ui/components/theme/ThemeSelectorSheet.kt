package com.example.ui.components.theme

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalGlassTheme
import com.example.ui.theme.ThemeType
import kotlinx.coroutines.launch

/**
 * MODULE 3: THEME SELECTOR MODAL BOTTOM SHEET
 * Polished luxury bottom sheet displaying 3 preview cards for:
 * 1. PEARL WHITE LUXURY (Default Light)
 * 2. ROYAL MIDNIGHT EMERALD (Dark Luxury)
 * 3. IMPERIAL VELVET SAPPHIRE (Midnight Blue Luxury)
 * Triggers light haptic feedback click on tap and updates DataStore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectorSheet(
    currentTheme: ThemeType,
    onSelectTheme: (ThemeType) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val glassColors = LocalGlassTheme.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = glassColors.surface,
        contentColor = glassColors.textPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(glassColors.textMuted.copy(alpha = 0.35f))
            )
        },
        modifier = Modifier.testTag("theme_selector_modal_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(glassColors.chipBackground)
                            .border(1.dp, glassColors.chipBorder, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = glassColors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Luxury Design System",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = glassColors.textPrimary
                        )
                        Text(
                            text = "Choose your sacred aesthetic atmosphere",
                            fontSize = 12.sp,
                            color = glassColors.textMuted
                        )
                    }
                }

                IconButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(glassColors.chipBackground)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = glassColors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3 Theme Preview Cards
            ThemeType.values().forEach { themeItem ->
                val isSelected = currentTheme == themeItem
                ThemePreviewCard(
                    themeType = themeItem,
                    isSelected = isSelected,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onSelectTheme(themeItem)
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Branding footer note
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(glassColors.chipBackground.copy(alpha = 0.5f))
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Crafted with Luxury Glassmorphism • Ryaan Studio",
                    fontSize = 11.sp,
                    color = glassColors.textMuted,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ThemePreviewCard(
    themeType: ThemeType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val glassColors = LocalGlassTheme.current

    val themeIcon = when (themeType) {
        ThemeType.PEARL_WHITE -> Icons.Default.LightMode
        ThemeType.MIDNIGHT_EMERALD -> Icons.Default.DarkMode
        ThemeType.VELVET_SAPPHIRE -> Icons.Default.WaterDrop
    }

    // Color Swatches defined by Module 2 specs
    val swatches = when (themeType) {
        ThemeType.PEARL_WHITE -> listOf(
            Color(0xFF065F46), // Deep Royal Emerald
            Color(0xFFD4AF37), // Pure Platinum Gold
            Color(0xFFFFFFFF), // Pure Pearl White
            Color(0xFF0F172A)  // Slate Onyx
        )
        ThemeType.MIDNIGHT_EMERALD -> listOf(
            Color(0xFF10B981), // Luminous Emerald Glow
            Color(0xFFFBBF24), // Champagne Gold
            Color(0xFF050B0A), // Deep Obsidian Black
            Color(0xFFF1F5F9)  // Crisp Ivory
        )
        ThemeType.VELVET_SAPPHIRE -> listOf(
            Color(0xFF38BDF8), // Electric Cyan
            Color(0xFFE2E8F0), // Pure Platinum Silver
            Color(0xFF030712), // Deep Royal Navy
            Color(0xFFFFFFFF)  // Pure White
        )
    }

    val cardBorder = if (isSelected) {
        BorderStroke(2.dp, themeType.previewPrimary)
    } else {
        BorderStroke(1.dp, glassColors.chipBorder)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isSelected) 8.dp else 2.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = themeType.previewPrimary.copy(alpha = 0.15f),
                spotColor = themeType.previewPrimary.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        themeType.previewBackground.copy(alpha = if (themeType.isDark) 0.95f else 0.9f),
                        themeType.previewBackground
                    )
                )
            )
            .border(cardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(16.dp)
            .testTag("theme_card_${themeType.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Theme icon container
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(themeType.previewPrimary.copy(alpha = 0.18f))
                        .border(1.2.dp, themeType.previewPrimary.copy(alpha = 0.45f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = themeIcon,
                        contentDescription = null,
                        tint = themeType.previewPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = themeType.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (themeType.isDark) Color.White else Color(0xFF0F172A)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(themeType.previewPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = themeType.previewPrimary,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = themeType.subtitle,
                        fontSize = 11.5.sp,
                        color = if (themeType.isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4 Palette Swatches (Gold, Emerald, Sapphire, etc.)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        swatches.forEach { swatchColor ->
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )
                        }
                    }
                }
            }

            // Selection Radio / Check Indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) themeType.previewPrimary else Color.Transparent
                    )
                    .border(
                        1.5.dp,
                        if (isSelected) themeType.previewPrimary else (if (themeType.isDark) Color(0x60FFFFFF) else Color(0x30000000)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (themeType.previewPrimary == Color.White) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
