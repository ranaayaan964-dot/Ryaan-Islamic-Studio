package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalThemeIsDark

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val surfaceColor = if (isDark) {
        Color(0xCC0F1D3D)
    } else {
        Color(0xECFFFFFF)
    }

    val strokeColors = if (isDark) {
        listOf(
            Color(0x55E5C07B),
            Color(0x15E5C07B),
            Color(0x05E5C07B),
            Color(0x35E5C07B)
        )
    } else {
        listOf(
            Color(0x40065F46),
            Color(0x15065F46),
            Color(0x05065F46),
            Color(0x30065F46)
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(surfaceColor)
            .border(
                BorderStroke(
                    borderWidth,
                    Brush.linearGradient(strokeColors)
                ),
                shape = shape
            ),
        content = content
    )
}

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    text: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val isDark = LocalThemeIsDark.current
    val pillBg = if (isDark) Color(0x33E5C07B) else Color(0x22065F46)
    val pillStroke = if (isDark) Color(0x55E5C07B) else Color(0x44065F46)

    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(pillBg)
            .border(BorderStroke(1.dp, pillStroke), RoundedCornerShape(50))
            .then(clickMod)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        text()
    }
}
