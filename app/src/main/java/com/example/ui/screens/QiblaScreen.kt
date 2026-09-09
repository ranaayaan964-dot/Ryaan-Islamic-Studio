package com.example.ui.screens

import android.content.Context
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LocationInfo
import com.example.sensor.CompassSensorManager
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LightEmeraldBackground
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.SuccessGreen
import com.example.util.QiblaCalculator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun QiblaScreen(
    location: LocationInfo,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sensorManager = remember { CompassSensorManager(context) }

    DisposableEffect(Unit) {
        sensorManager.startListening()
        onDispose {
            sensorManager.stopListening()
        }
    }

    val currentAzimuth by sensorManager.azimuthFlow.collectAsState()
    val isSensorAvailable by sensorManager.isSensorAvailable.collectAsState()
    val accuracy by sensorManager.accuracyFlow.collectAsState()

    var showCalibrationOverlay by remember { mutableStateOf(false) }
    var isArMode by remember { mutableStateOf(false) }

    if (isArMode) {
        ArQiblaScreen(onNavigateBack = { isArMode = false })
        return
    }

    val qiblaBearing = remember(location) {
        QiblaCalculator.calculateQiblaBearing(location.latitude, location.longitude)
    }

    val distanceKm = remember(location) {
        QiblaCalculator.calculateDistanceToKaaba(location.latitude, location.longitude)
    }

    // Calculate relative turn angle between phone azimuth and target Qibla bearing
    // relativeAngle: 0° to 360°
    val relativeAngle = (qiblaBearing - currentAzimuth + 360f) % 360f
    
    // Aligned within 3 degrees
    val isAligned = relativeAngle <= 3f || relativeAngle >= 357f

    // Turn direction and delta calculation
    val turnDirectionIsLeft = relativeAngle > 180f
    val degreesToTurn = if (turnDirectionIsLeft) {
        360f - relativeAngle
    } else {
        relativeAngle
    }

    var hasVibratedOnAligned by remember { mutableStateOf(false) }

    LaunchedEffect(isAligned) {
        if (isAligned && !hasVibratedOnAligned) {
            triggerHapticFeedback(context)
            hasVibratedOnAligned = true
        } else if (!isAligned) {
            hasVibratedOnAligned = false
        }
    }

    val isDark = LocalThemeIsDark.current
    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            listOf(
                DarkNavyBackground,
                Color(0xFF0D1737),
                Color(0xFF042F2E),
                DarkNavyBackground
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                LightEmeraldBackground,
                Color(0xFFE6F4ED),
                Color(0xFFF0FDF4),
                LightEmeraldBackground
            )
        )
    }

    val needleColor by animateColorAsState(
        targetValue = if (isAligned) SuccessGreen else MaterialTheme.colorScheme.primary,
        label = "NeedleColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qibla_screen")
            .background(bgGradient)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar with Back navigation & Calibration Shortcut
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0x33FFFFFF) else Color(0x18000000))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(38.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Qibla Direction",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${location.cityName}, ${location.countryName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // AR 3D View Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x3310B981))
                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                                .clickable { isArMode = true }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "AR 3D Mode",
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "AR 3D",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Top Bar Calibration Action Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x22E5C07B) else Color(0x18065F46))
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                .clickable { showCalibrationOverlay = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CompassCalibration,
                                    contentDescription = "Calibrate Compass",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Calib",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Low/Medium Accuracy Warning Banner
                if (accuracy < SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x28F59E0B))
                            .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(12.dp))
                            .clickable { showCalibrationOverlay = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compass interference detected. Tap to calibrate with Figure-8 gesture.",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Wave ∞",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) GoldPrimary else Color(0xFFB45309)
                        )
                    }
                }
            }

            // BIG DIGITAL INSTRUCTION CARD (CRITICAL UI UPGRADE)
            val turnDegreesRounded = degreesToTurn.toInt()
            val instructionBgColor = if (isAligned) {
                Color(0xFF059669) // Bright Success Green
            } else if (isDark) {
                Color(0x28E5C07B)
            } else {
                Color(0xFFF1F5F9)
            }
            val instructionBorderColor = if (isAligned) {
                Color(0xFF10B981)
            } else if (isDark) {
                GoldAccent
            } else {
                Color(0xFFCBD5E1)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isAligned) {
                            Brush.horizontalGradient(
                                listOf(Color(0xFF059669), Color(0xFF10B981))
                            )
                        } else if (isDark) {
                            Brush.horizontalGradient(
                                listOf(Color(0x33E5C07B), Color(0x221E293B))
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFFFFFF), Color(0xFFF8FAFC))
                            )
                        }
                    )
                    .border(
                        width = if (isAligned) 2.dp else 1.5.dp,
                        color = instructionBorderColor,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .testTag("qibla_digital_instruction_card"),
                contentAlignment = Alignment.Center
            ) {
                if (isAligned) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "FACING KAABA (0°)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Perfect Qibla alignment achieved",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFD1FAE5)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (turnDirectionIsLeft) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x44E5C07B) else Color(0x18065F46)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                                    contentDescription = "Turn Left",
                                    tint = if (isDark) GoldPrimary else Color(0xFF065F46),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "TURN LEFT BY ",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "$turnDegreesRounded°",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) GoldPrimary else Color(0xFF065F46)
                                    )
                                }
                                Text(
                                    text = "Rotate left to face Kaaba",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0x44E5C07B) else Color(0x18065F46)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                    contentDescription = "Turn Right",
                                    tint = if (isDark) GoldPrimary else Color(0xFF065F46),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "TURN RIGHT BY ",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "$turnDegreesRounded°",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) GoldPrimary else Color(0xFF065F46)
                                    )
                                }
                                Text(
                                    text = "Rotate right to face Kaaba",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Compass Dial Centerpiece
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Dial Ring Canvas (Rotates with azimuth)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(-currentAzimuth)
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.minDimension / 2 - 12.dp.toPx()

                    // Dial background circle
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(
                                if (isDark) Color(0x330F1D3D) else Color(0x99FFFFFF),
                                if (isDark) Color(0x990A1128) else Color(0xCCFFFFFF)
                            )
                        ),
                        radius = radius,
                        center = center
                    )

                    // Outer border
                    drawCircle(
                        color = if (isDark) Color(0x44E5C07B) else Color(0x44065F46),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // 72 Ticks (every 5 degrees)
                    for (i in 0 until 72) {
                        val angleDeg = i * 5.0
                        val angleRad = Math.toRadians(angleDeg)
                        val isMajor = i % 18 == 0 // N, E, S, W
                        val isMedium = i % 6 == 0
                        val tickLength = if (isMajor) 18.dp.toPx() else if (isMedium) 10.dp.toPx() else 5.dp.toPx()
                        val tickColor = if (isMajor) {
                            if (i == 0) Color(0xFFEF4444) else (if (isDark) Color(0xFFE5C07B) else Color(0xFF065F46))
                        } else {
                            if (isDark) Color(0x55E5C07B) else Color(0x55065F46)
                        }

                        val startX = center.x + (radius - tickLength) * sin(angleRad).toFloat()
                        val startY = center.y - (radius - tickLength) * cos(angleRad).toFloat()
                        val endX = center.x + radius * sin(angleRad).toFloat()
                        val endY = center.y - radius * cos(angleRad).toFloat()

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Inner Qibla Pointer Needle Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(relativeAngle)
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val needleLength = size.minDimension / 2 - 28.dp.toPx()
                    val needleWidth = 14.dp.toPx()

                    // Top arrowhead pointing to Qibla
                    val topPath = Path().apply {
                        moveTo(center.x, center.y - needleLength)
                        lineTo(center.x + needleWidth / 2, center.y - 10.dp.toPx())
                        lineTo(center.x, center.y)
                        lineTo(center.x - needleWidth / 2, center.y - 10.dp.toPx())
                        close()
                    }

                    // Bottom needle tail
                    val bottomPath = Path().apply {
                        moveTo(center.x, center.y + needleLength * 0.6f)
                        lineTo(center.x + needleWidth / 3, center.y + 8.dp.toPx())
                        lineTo(center.x, center.y)
                        lineTo(center.x - needleWidth / 3, center.y + 8.dp.toPx())
                        close()
                    }

                    drawPath(topPath, color = needleColor)
                    drawPath(bottomPath, color = if (isDark) Color(0x66FFFFFF) else Color(0x66000000))

                    // Center pivot knob
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = needleColor,
                        radius = 5.dp.toPx(),
                        center = center
                    )
                }

                // Center Kaaba Emblem Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isAligned) SuccessGreen else MaterialTheme.colorScheme.primary)
                        .border(1.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🕋",
                        fontSize = 18.sp
                    )
                }
            }

            // Quick Calibration Guide launcher button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isDark) Color(0x25E5C07B) else Color(0x18065F46))
                    .border(
                        1.dp,
                        if (isDark) Color(0x55E5C07B) else Color(0x33065F46),
                        RoundedCornerShape(50)
                    )
                    .clickable { showCalibrationOverlay = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CompassCalibration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Figure-8 Compass Calibration Guide",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Info Cards Bottom Section - DIGITAL NUMERICAL READOUT
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .testTag("qibla_numerical_readout_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Target Bearing",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Qibla: ${String.format(java.util.Locale.US, "%.1f°", qiblaBearing)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Current Heading",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "You: ${String.format(java.util.Locale.US, "%.1f°", currentAzimuth)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAligned) SuccessGreen else MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .width(1.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Distance",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$distanceKm km",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Digital Delta Alignment Counter
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isAligned) Color(0x2210B981) else if (isDark) Color(0x1CE5C07B) else Color(0x12065F46)
                            )
                            .padding(vertical = 8.dp, horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAligned) {
                                "✓ Alignment: 0.0° delta — perfectly facing Holy Kaaba"
                            } else {
                                "Difference: ${String.format(java.util.Locale.US, "%.1f°", degreesToTurn)} left to reach Qibla (${if (turnDirectionIsLeft) "Turn Left" else "Turn Right"})"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAligned) SuccessGreen else MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Compass Calibration Tutorial & Animation Overlay
        if (showCalibrationOverlay) {
            CompassCalibrationOverlay(
                currentAccuracy = accuracy,
                onCalibrateFinished = {
                    sensorManager.markCalibrated()
                },
                onDismiss = { showCalibrationOverlay = false }
            )
        }
    }
}

private fun triggerHapticFeedback(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    } catch (_: Exception) {}
}
