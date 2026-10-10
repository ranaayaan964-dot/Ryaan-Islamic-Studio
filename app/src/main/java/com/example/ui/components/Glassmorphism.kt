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
import com.example.ui.theme.LocalGlassTheme
import com.example.ui.theme.LocalThemeIsDark

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    borderWidth: Dp = 1.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = LocalGlassTheme.current
    val surfaceColor = glassColors.glassCardBackground
    val strokeColors = glassColors.glassCardBorder

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
    val glassColors = LocalGlassTheme.current
    val pillBg = glassColors.chipBackground
    val pillStroke = glassColors.chipBorder

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
