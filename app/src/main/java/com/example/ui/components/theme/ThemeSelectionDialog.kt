package com.example.ui.components.theme

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.GlassCard
import com.example.ui.theme.AppGlassTheme
import com.example.ui.theme.AppTheme
import com.example.ui.theme.LocalGlassTheme
import com.example.ui.theme.ThemeType

@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeType,
    onSelectTheme: (ThemeType) -> Unit,
    onDismiss: () -> Unit
) {
    val glassColors = LocalGlassTheme.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp))
                .testTag("theme_selection_dialog"),
            color = glassColors.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, glassColors.glassCardStrokeColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
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
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(glassColors.chipBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = glassColors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "App Theme & Glass",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = glassColors.textPrimary
                            )
                            Text(
                                text = "المظهر والألوان الزجاجية",
                                fontSize = 12.sp,
                                color = glassColors.accentGold,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Text(
                        text = "4 Themes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = glassColors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(glassColors.chipBackground)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select your preferred glassmorphism aesthetic. All screens and cards update instantly.",
                    fontSize = 12.5.sp,
                    color = glassColors.textSecondary,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 3 Luxury Theme Options
                ThemeType.values().forEach { theme ->
                    val isSelected = currentTheme == theme
                    ThemeOptionCard(
                        theme = theme,
                        isSelected = isSelected,
                        onClick = { onSelectTheme(theme) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("theme_done_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = glassColors.primary,
                        contentColor = glassColors.onPrimary
                    )
                ) {
                    Text(
                        text = "Done & Apply",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    theme: ThemeType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val glassColors = LocalGlassTheme.current

    val themeIcon = when (theme) {
        ThemeType.PEARL_WHITE -> Icons.Default.LightMode
        ThemeType.MIDNIGHT_EMERALD -> Icons.Default.DarkMode
        ThemeType.VELVET_SAPPHIRE -> Icons.Default.FormatColorFill
    }

    val cardBorder = if (isSelected) {
        BorderStroke(2.dp, Brush.linearGradient(listOf(glassColors.primary, glassColors.accentGold)))
    } else {
        BorderStroke(1.dp, glassColors.chipBorder)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("theme_card_${theme.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) glassColors.chipBackground else glassColors.surface
        ),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Theme Visual Preview Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(theme.previewBackground)
                    .border(1.5.dp, theme.previewPrimary.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = themeIcon,
                    contentDescription = null,
                    tint = theme.previewPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = theme.displayName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = glassColors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = theme.subtitle,
                    fontSize = 11.5.sp,
                    color = glassColors.textSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Palette Swatches
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PaletteSwatchDot(color = theme.previewBackground, label = "Canvas")
                    PaletteSwatchDot(color = theme.previewPrimary, label = "Primary")
                    PaletteSwatchDot(color = theme.previewAccent, label = "Accent")
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = glassColors.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, glassColors.chipBorder, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatchDot(
    color: Color,
    label: String
) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
    )
}
