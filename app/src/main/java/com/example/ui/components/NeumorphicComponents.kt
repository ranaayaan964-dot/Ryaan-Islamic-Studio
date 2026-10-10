package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 120.dp.toPx()
            val starRadius = 24.dp.toPx()
            val effectiveColor = watermarkColor.copy(alpha = opacity)

            val width = size.width
            val height = size.height

            var x = 0f
            while (x < width + step) {
                var y = 0f
                while (y < height + step) {
                    drawIslamicEightPointStar(
                        center = Offset(x, y),
                        radius = starRadius,
                        color = effectiveColor
                    )
                    // Faint connecting lattice lines
                    drawLine(
                        color = effectiveColor,
                        start = Offset(x, y - starRadius),
                        end = Offset(x + step, y - starRadius),
                        strokeWidth = 1f
                    )
                    drawLine(
                        color = effectiveColor,
                        start = Offset(x - starRadius, y),
                        end = Offset(x - starRadius, y + step),
                        strokeWidth = 1f
                    )
                    y += step
                }
                x += step
            }
        }

        content()
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawIslamicEightPointStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path()
    val points = 8
    val innerRadius = radius * 0.45f

    for (i in 0 until points * 2) {
        val r = if (i % 2 == 0) radius else innerRadius
        val angle = i * PI / points
        val px = center.x + (r * cos(angle)).toFloat()
        val py = center.y + (r * sin(angle)).toFloat()

        if (i == 0) {
            path.moveTo(px, py)
        } else {
            path.lineTo(px, py)
        }
    }
    path.close()

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.2f, cap = StrokeCap.Round)
    )
}
