package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite

/**
 * Enterprise Soft Neumorphism Card with 8.dp elevation and dual-shadow aesthetics
 * for the Pearl White Luxury UI System.
 */
@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 8.dp,
    backgroundColor: Color = PureWhite,
    borderColor: Color = Color(0x20D4AF37), // Subtle platinum gold border
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color(0x180F172A),
                spotColor = Color(0x1F064E3B)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
    ) {
        content()
    }
}

/**
 * Geometric Islamic Watermark Background with 0.03 opacity, creating an ultra-luxury
 * sacred ambience across all screen canvases.
 */
@Composable
fun IslamicWatermarkBackground(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PearlBackground,
    watermarkColor: Color = DeepRoyalEmerald,
    opacity: Float = 0.03f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val step = 120.dp.toPx()
                    val starRadius = 24.dp.toPx()
                    val effectiveColor = watermarkColor.copy(alpha = opacity)
                    val patternPath = Path()

                    var x = 0f
                    while (x < size.width + step) {
                        var y = 0f
                        while (y < size.height + step) {
                            for (i in 0 until 16) {
                                val radius = if (i % 2 == 0) starRadius else starRadius * 0.45f
                                val angle = i * Math.PI / 8.0
                                val px = x + (radius * kotlin.math.cos(angle)).toFloat()
                                val py = y + (radius * kotlin.math.sin(angle)).toFloat()
                                if (i == 0) patternPath.moveTo(px, py) else patternPath.lineTo(px, py)
                            }
                            patternPath.close()
                            patternPath.moveTo(x, y - starRadius)
                            patternPath.lineTo(x + step, y - starRadius)
                            patternPath.moveTo(x - starRadius, y)
                            patternPath.lineTo(x - starRadius, y + step)
                            y += step
                        }
                        x += step
                    }

                    onDrawBehind {
                        drawPath(
                            path = patternPath,
                            color = effectiveColor,
                            style = Stroke(width = 1.1f)
                        )
                    }
                }
        )

        content()
    }
}
