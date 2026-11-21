package com.ayaan.islamicstudio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaan.islamicstudio.ui.theme.LocalGlassTheme
import com.ayaan.islamicstudio.ui.theme.LocalThemeType
import com.ayaan.islamicstudio.ui.theme.ThemeType
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun LuxuryAmbientBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = LocalGlassTheme.current
    val theme = LocalThemeType.current
    val transition = rememberInfiniteTransition(label = "LuxuryAmbient")
    val shimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LuxuryShimmer"
    )

    val palette = when (theme) {
        ThemeType.PEARL_WHITE -> listOf(
            Color(0x33D4AF37),
            Color(0x22047C5A),
            glassColors.background
        )
        ThemeType.MIDNIGHT_EMERALD -> listOf(
            Color(0x6610B981),
            Color(0x66D4AF37),
            glassColors.background
        )
        ThemeType.VELVET_SAPPHIRE -> listOf(
            Color(0x6638BDF8),
            Color(0x55A78BFA),
            glassColors.background
        )
    }

    Box(
        modifier = modifier
            .background(glassColors.background)
            .drawWithCache {
                val latticePath = Path().apply {
                    val cx = size.width * 0.82f
                    val cy = size.height * 0.12f
                    val radius = min(size.width, size.height) * 0.12f
                    for (ring in 0 until 2) {
                        val r = radius * (0.75f + ring * 0.35f)
                        for (i in 0 until 8) {
                            val angle1 = (i * 45f - 22.5f) * 0.0174533f
                            val angle2 = ((i + 1) * 45f - 22.5f) * 0.0174533f
                            moveTo(cx + cos(angle1) * r, cy + sin(angle1) * r)
                            lineTo(cx + cos(angle2) * r, cy + sin(angle2) * r)
                        }
                    }
                }
                val ambientBrush = Brush.radialGradient(
                    colors = palette,
                    center = Offset(size.width * (0.2f + shimmer * 0.08f), size.height * 0.18f),
                    radius = size.minDimension * 0.75f
                )
                onDrawWithContent {
                    drawRect(ambientBrush)
                    drawCircle(
                        color = palette.first().copy(alpha = 0.12f),
                        radius = size.minDimension * 0.18f,
                        center = Offset(size.width * (0.82f - shimmer * 0.04f), size.height * 0.78f)
                    )
                    drawPath(
                        latticePath,
                        color = glassColors.accentGold.copy(alpha = if (glassColors.isDark) 0.045f else 0.055f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawContent()
                }
            },
        content = content
    )
}

@Composable
fun HighGraphicsGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    onClick: (() -> Unit)? = null,
    glowColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = LocalGlassTheme.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "GlassPressScale"
    )
    val borderBrush = remember(glassColors) {
        Brush.verticalGradient(
            listOf(
                glassColors.accentGold.copy(alpha = 0.95f),
                glassColors.glassCardStrokeColor,
                glassColors.accentEmerald.copy(alpha = 0.72f)
            )
        )
    }
    val surfaceBrush = remember(glassColors) {
        Brush.verticalGradient(
            listOf(
                glassColors.glassCardBackground,
                glassColors.surface.copy(alpha = 0.92f)
            )
        )
    }
    val ambient = glowColor ?: glassColors.accentEmerald

    Box(
        modifier = modifier
            .shadow(
                elevation = glassColors.elevationDp.dp,
                shape = shape,
                clip = false,
                ambientColor = ambient.copy(alpha = 0.24f),
                spotColor = ambient.copy(alpha = 0.32f)
            )
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ) else Modifier)
            .background(surfaceBrush)
            .border(1.25.dp, borderBrush, shape)
            .drawWithCache {
                val specularBrush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    endY = size.height * 0.28f
                )
                onDrawWithContent {
                    drawContent()
                    drawRoundRect(
                        brush = specularBrush,
                        topLeft = Offset(0f, 0f),
                        size = Size(size.width, size.height * 0.22f),
                        cornerRadius = CornerRadius(18.dp.toPx())
                    )
                }
            }
            .then(androidx.compose.ui.draw.scale(scale)),
        content = content
    )
}

@Composable
fun GlowingIslamicArcGauge(
    progress: Float,
    label: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 128.dp
) {
    val glassColors = LocalGlassTheme.current
    val clamped = progress.coerceIn(0f, 1f)
    Canvas(modifier = modifier.size(sizeDp)) {
        val diameter = size.minDimension
        val strokeWidth = diameter * 0.075f
        val radius = diameter * 0.42f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawArc(
            color = glassColors.textMuted.copy(alpha = 0.20f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(
                listOf(glassColors.accentGold, glassColors.accentEmerald, glassColors.accentGold),
                center = center
            ),
            startAngle = -90f,
            sweepAngle = 360f * clamped,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth * 1.6f, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(
                listOf(glassColors.accentGold.copy(alpha = 0.95f), glassColors.accentEmerald),
                center = center
            ),
            startAngle = -90f,
            sweepAngle = 360f * clamped,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        val angle = (-90f + 360f * clamped) * 0.0174533f
        val head = Offset(
            center.x + cos(angle) * radius,
            center.y + sin(angle) * radius
        )
        drawCircle(glassColors.accentGold.copy(alpha = 0.55f), radius = strokeWidth * 1.25f, center = head)
        drawCircle(Color.White.copy(alpha = 0.95f), radius = strokeWidth * 0.32f, center = head)

        for (tick in 0 until 24) {
            val tickAngle = (tick * 15f - 90f) * 0.0174533f
            val inner = radius + strokeWidth * 1.2f
            val outer = inner + strokeWidth * 0.55f
            drawLine(
                color = glassColors.textMuted.copy(alpha = 0.25f),
                start = Offset(center.x + cos(tickAngle) * inner, center.y + sin(tickAngle) * inner),
                end = Offset(center.x + cos(tickAngle) * outer, center.y + sin(tickAngle) * outer),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun AnimatedAudioEqualizerBars(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassTheme.current
    val transition = rememberInfiniteTransition(label = "Equalizer")
    val values = listOf(
        transition.animateFloat(8f, 28f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "Bar1").value,
        transition.animateFloat(16f, 34f, infiniteRepeatable(tween(680), RepeatMode.Reverse), label = "Bar2").value,
        transition.animateFloat(10f, 30f, infiniteRepeatable(tween(460), RepeatMode.Reverse), label = "Bar3").value,
        transition.animateFloat(18f, 36f, infiniteRepeatable(tween(760), RepeatMode.Reverse), label = "Bar4").value,
        transition.animateFloat(8f, 24f, infiniteRepeatable(tween(590), RepeatMode.Reverse), label = "Bar5").value
    )
    Row(modifier = modifier.height(38.dp), verticalAlignment = Alignment.CenterVertically) {
        values.forEachIndexed { index, height ->
            val barHeight = if (isActive) height.dp else (6 + index * 2).dp
            Box(
                Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.verticalGradient(
                            listOf(glassColors.accentGold, glassColors.accentEmerald)
                        )
                    )
            )
        }
    }
}

@Composable
fun LuxuryShimmerEffect(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glassColors = LocalGlassTheme.current
    val transition = rememberInfiniteTransition(label = "GoldFoil")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "FoilOffset"
    )
    Box(
        modifier = modifier.drawWithCache {
            val brush = Brush.linearGradient(
                listOf(
                    glassColors.accentGold.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.95f),
                    glassColors.accentGold.copy(alpha = 0.75f)
                ),
                start = Offset(size.width * offset - size.width * 0.4f, 0f),
                end = Offset(size.width * offset + size.width * 0.4f, size.height)
            )
            onDrawWithContent {
                drawContent()
                drawRect(brush, alpha = 0.22f)
            }
        },
        content = content
    )
}

@Composable
fun GoldFoilBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassTheme.current
    LuxuryShimmerEffect(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, glassColors.accentGold, RoundedCornerShape(50))
            .background(glassColors.accentGold.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = if (glassColors.isDark) glassColors.accentGold else Color(0xFF7C5A12),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
