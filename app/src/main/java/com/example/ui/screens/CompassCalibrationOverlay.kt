package com.example.ui.screens

import android.content.Context
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen overlay tutorial and interactive animation guiding users through
 * the Figure-8 (∞) compass calibration gesture for maximum Qibla direction accuracy.
 */
@Composable
fun CompassCalibrationOverlay(
    currentAccuracy: Int,
    onCalibrateFinished: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var calibrationProgress by remember { mutableFloatStateOf(if (currentAccuracy >= SensorManager.SENSOR_STATUS_ACCURACY_HIGH) 1.0f else 0.25f) }
    var simulatedAccuracy by remember { mutableIntStateOf(currentAccuracy) }
    var isCalibratingStep by remember { mutableStateOf(false) }

    // Auto-advance calibration demonstration if user stays on screen
    LaunchedEffect(isCalibratingStep) {
        if (isCalibratingStep) {
            val startProgress = calibrationProgress
            val steps = 30
            for (i in 1..steps) {
                delay(80)
                calibrationProgress = (startProgress + (1f - startProgress) * (i.toFloat() / steps)).coerceAtMost(1f)
            }
            simulatedAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            isCalibratingStep = false
            triggerCalibrationHaptic(context)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("compass_calibration_overlay")
                .background(Color(0xE6050B1B))
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x66E5C07B),
                                Color(0x2210B981),
                                Color(0x44E5C07B)
                            )
                        ),
                        RoundedCornerShape(28.dp)
                    ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22E5C07B))
                                    .border(1.dp, GoldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CompassCalibration,
                                    contentDescription = "Calibrate",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Compass Calibration",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Wave device in a Figure-8 loop",
                                    fontSize = 11.5.sp,
                                    color = GoldAccent
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Real-Time Sensor Accuracy Status Pill
                    AccuracyStatusBadge(accuracy = simulatedAccuracy)

                    Spacer(modifier = Modifier.height(18.dp))

                    // The Interactive Figure-8 Animation Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0x250A132C))
                            .border(1.dp, Color(0x33E5C07B), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        FigureEightAnimationCanvas(
                            isCalibrating = isCalibratingStep || calibrationProgress < 1f
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Calibration Progress Indicator
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (calibrationProgress >= 0.99f) "Sensor Calibrated" else "Sensor Calibration Progress",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (calibrationProgress >= 0.99f) SuccessGreen else GoldAccent
                            )
                            Text(
                                text = "${(calibrationProgress * 100).toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (calibrationProgress >= 0.99f) SuccessGreen else Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { calibrationProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (calibrationProgress >= 0.99f) SuccessGreen else GoldPrimary,
                            trackColor = Color(0x33FFFFFF)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Step-by-Step Instruction Cards
                    CalibrationStepItem(
                        stepNumber = "1",
                        title = "Avoid Magnetic Interference",
                        description = "Move away from metallic tables, laptops, charging cables, and magnetic covers."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CalibrationStepItem(
                        stepNumber = "2",
                        title = "Wave in a Figure-8 (∞) Motion",
                        description = "Smoothly sweep your phone in the infinity loop shown above 2–3 times in the air."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CalibrationStepItem(
                        stepNumber = "3",
                        title = "Hold Horizontally Level",
                        description = "Keep phone flat on your palm or level horizontally pointing toward the Qibla needle."
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                isCalibratingStep = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0x33E5C07B),
                                contentColor = GoldPrimary
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Calibrate Wave",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                onCalibrateFinished()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (calibrationProgress >= 0.99f) SuccessGreen else EmeraldAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (calibrationProgress >= 0.99f) "Accuracy Confirmed" else "Done / Calibrated",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated Canvas drawing the infinity / figure-8 loop path, motion arrows,
 * glowing particle trail, and a rotating phone graphic along the trajectory.
 */
@Composable
fun FigureEightAnimationCanvas(
    isCalibrating: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Figure8Loop")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "MotionTime"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radiusX = size.width * 0.38f * pulseScale
        val radiusY = size.height * 0.34f * pulseScale

        // 1. Draw glowing background figure-8 path
        val fullPath = Path()
        val numSamples = 120
        for (i in 0..numSamples) {
            val t = (i.toFloat() / numSamples) * (2 * PI).toFloat()
            val px = centerX + radiusX * sin(t)
            val py = centerY + radiusY * (sin(2 * t) / 2f)
            if (i == 0) {
                fullPath.moveTo(px, py)
            } else {
                fullPath.lineTo(px, py)
            }
        }
        fullPath.close()

        // Outer glow
        drawPath(
            path = fullPath,
            brush = Brush.horizontalGradient(
                listOf(
                    Color(0x33E5C07B),
                    Color(0x5510B981),
                    Color(0x33E5C07B)
                )
            ),
            style = Stroke(
                width = 8.dp.toPx(),
                cap = StrokeCap.Round
            )
        )

        // Dashed guide path
        drawPath(
            path = fullPath,
            color = Color(0xAAE5C07B),
            style = Stroke(
                width = 2.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f),
                cap = StrokeCap.Round
            )
        )

        // 2. Draw directional indicator arrows on each loop
        drawLoopArrow(
            centerX = centerX - radiusX * 0.65f,
            centerY = centerY - radiusY * 0.22f,
            angleDeg = -45f,
            color = EmeraldAccent
        )
        drawLoopArrow(
            centerX = centerX + radiusX * 0.65f,
            centerY = centerY + radiusY * 0.22f,
            angleDeg = 135f,
            color = EmeraldAccent
        )

        // 3. Compute current phone position along Lemniscate
        val t = animProgress
        val phoneX = centerX + radiusX * sin(t)
        val phoneY = centerY + radiusY * (sin(2 * t) / 2f)

        // Tangent velocity vector to naturally rotate phone in flight direction
        val dx = radiusX * cos(t)
        val dy = radiusY * cos(2 * t)
        val headingAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        // 4. Draw glowing motion trail behind phone
        val trailSamples = 16
        for (j in 1..trailSamples) {
            val trailT = (t - j * 0.05f + 2 * PI.toFloat()) % (2 * PI.toFloat())
            val tx = centerX + radiusX * sin(trailT)
            val ty = centerY + radiusY * (sin(2 * trailT) / 2f)
            val alpha = (1f - (j.toFloat() / trailSamples)) * 0.7f
            val dotRadius = (5f - j * 0.25f).coerceAtLeast(1.5f).dp.toPx()

            drawCircle(
                color = if (j % 2 == 0) GoldPrimary.copy(alpha = alpha) else EmeraldAccent.copy(alpha = alpha),
                radius = dotRadius,
                center = Offset(tx, ty)
            )
        }

        // 5. Draw sleek smartphone representation at (phoneX, phoneY)
        rotate(
            degrees = headingAngle + 90f,
            pivot = Offset(phoneX, phoneY)
        ) {
            drawStylizedPhone(
                center = Offset(phoneX, phoneY),
                isCalibrating = isCalibrating
            )
        }
    }
}

private fun DrawScope.drawLoopArrow(
    centerX: Float,
    centerY: Float,
    angleDeg: Float,
    color: Color
) {
    rotate(degrees = angleDeg, pivot = Offset(centerX, centerY)) {
        val arrowPath = Path().apply {
            moveTo(centerX - 6.dp.toPx(), centerY - 6.dp.toPx())
            lineTo(centerX + 6.dp.toPx(), centerY)
            lineTo(centerX - 6.dp.toPx(), centerY + 6.dp.toPx())
        }
        drawPath(
            path = arrowPath,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawStylizedPhone(
    center: Offset,
    isCalibrating: Boolean
) {
    val phoneWidth = 28.dp.toPx()
    val phoneHeight = 52.dp.toPx()
    val topLeft = Offset(center.x - phoneWidth / 2f, center.y - phoneHeight / 2f)
    val cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())

    // Phone shadow
    drawRoundRect(
        color = Color(0x66000000),
        topLeft = topLeft.copy(y = topLeft.y + 3.dp.toPx()),
        size = Size(phoneWidth, phoneHeight),
        cornerRadius = cornerRadius
    )

    // Phone body chassis
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(
                Color(0xFF1E293B),
                Color(0xFF0F172A),
                Color(0xFF020617)
            )
        ),
        topLeft = topLeft,
        size = Size(phoneWidth, phoneHeight),
        cornerRadius = cornerRadius
    )

    // Phone border / bezel highlight
    drawRoundRect(
        color = if (isCalibrating) GoldPrimary else EmeraldAccent,
        topLeft = topLeft,
        size = Size(phoneWidth, phoneHeight),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1.5.dp.toPx())
    )

    // Screen area
    val screenMargin = 3.dp.toPx()
    val screenTopLeft = Offset(topLeft.x + screenMargin, topLeft.y + screenMargin)
    val screenSize = Size(phoneWidth - screenMargin * 2, phoneHeight - screenMargin * 2)
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(
                Color(0x3310B981),
                Color(0x55042F2E)
            )
        ),
        topLeft = screenTopLeft,
        size = screenSize,
        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
    )

    // Compass needle inside the phone screen
    val needleTop = Offset(center.x, center.y - 12.dp.toPx())
    val needleBottom = Offset(center.x, center.y + 12.dp.toPx())
    val needleLeft = Offset(center.x - 4.dp.toPx(), center.y)
    val needleRight = Offset(center.x + 4.dp.toPx(), center.y)

    // North (Red/Gold)
    val northPath = Path().apply {
        moveTo(needleTop.x, needleTop.y)
        lineTo(needleRight.x, needleRight.y)
        lineTo(center.x, center.y)
        lineTo(needleLeft.x, needleLeft.y)
        close()
    }
    drawPath(northPath, color = Color(0xFFEF4444))

    // South (Silver)
    val southPath = Path().apply {
        moveTo(needleBottom.x, needleBottom.y)
        lineTo(needleRight.x, needleRight.y)
        lineTo(center.x, center.y)
        lineTo(needleLeft.x, needleLeft.y)
        close()
    }
    drawPath(southPath, color = Color(0xFFE2E8F0))

    // Center pivot knob
    drawCircle(
        color = Color.White,
        radius = 2.dp.toPx(),
        center = center
    )
}

@Composable
fun AccuracyStatusBadge(accuracy: Int) {
    val (label, color, icon) = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Triple("Sensor Calibrated (High Accuracy)", SuccessGreen, Icons.Default.CheckCircle)
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Triple("Good Accuracy (Minor Tuning Recommended)", Color(0xFF38BDF8), Icons.Default.Info)
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> Triple("Low Accuracy • Figure-8 Calibration Advised", Color(0xFFF59E0B), Icons.Default.Warning)
        else -> Triple("Unreliable Compass • Calibration Needed", Color(0xFFEF4444), Icons.Default.Warning)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun CalibrationStepItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x18FFFFFF))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(GoldPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFFCBD5E1),
                lineHeight = 15.sp
            )
        }
    }
}

private fun triggerCalibrationHaptic(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0, 50, 60, 50),
                    -1
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(80)
        }
    } catch (_: Exception) {}
}
