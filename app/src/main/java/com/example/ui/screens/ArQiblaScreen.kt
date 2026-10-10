package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ar.ArOrientationState
import com.example.ar.ArQiblaEngine
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.EmeraldAccent
import kotlin.math.roundToInt

/**
 * PURE 3D AR QIBLA (LIVE CAMERA & YELLOW GUIDE LINES ONLY)
 * Strictly eliminates all traditional 2D compass dials, circles, and needles.
 * Relies purely on the live camera viewport (PreviewView) with:
 * - 3D vivid yellow guide lines projected on the ground leading to the Kaaba
 * - 3D floating Holy Kaaba pin marker
 * - 3D floating nearby mosque markers
 * - Turn-direction HUD with haptic pulse alignment feedback
 */
@Composable
fun ArQiblaScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler {
        onNavigateBack()
    }

    val arEngine = remember { ArQiblaEngine(context) }
    val orientationState by arEngine.orientationState.collectAsStateWithLifecycle()

    var hasCameraPermission by remember { mutableStateOf(false) }
    var showGroundPath by remember { mutableStateOf(true) }
    var showNearbyMasjids by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (perm == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        arEngine.start()
        onDispose {
            arEngine.stop()
        }
    }

    // Trigger haptic pulse when perfectly aligned with Kaaba
    LaunchedEffect(orientationState.isAlignedWithQibla) {
        if (orientationState.isAlignedWithQibla) {
            triggerHapticPulse(context)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ArPulse")
    val yellowPulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "YellowPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ar_qibla_screen")
            .background(DarkNavyBackground)
    ) {
        // 1. Live Camera Preview (Purely CameraX PreviewView)
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Permission Request Fallback View
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF04101A), Color(0xFF072127), Color(0xFF030D15))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Camera Permission Required",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Camera Access Required for AR View",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Point camera at your surroundings to project 3D yellow guide lines",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Grant Camera Permission", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. 3D Vivid Yellow Ground Guide Lines to Holy Kaaba
        if (showGroundPath) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Calculate screen X offset based on delta to Qibla (horizontal FOV approx 60 degrees)
                val fovDegrees = 60f
                val delta = orientationState.deltaToQiblaDegrees
                val normalizedX = (delta / (fovDegrees / 2f)).coerceIn(-1.6f, 1.6f)

                val targetX = (canvasWidth / 2f) - (normalizedX * (canvasWidth / 2f))
                val targetY = canvasHeight * 0.42f // Horizon convergence vanishing point

                // 3D Yellow Perspective Pathway
                val basePath = Path().apply {
                    moveTo(canvasWidth * 0.15f, canvasHeight)
                    lineTo(canvasWidth * 0.85f, canvasHeight)
                    lineTo(targetX + 42f, targetY)
                    lineTo(targetX - 42f, targetY)
                    close()
                }

                // Bright yellow guide lines styling
                val yellowPrimary = Color(0xFFFFD700) // Vivid Gold/Yellow
                val yellowAmber = Color(0xFFF59E0B)

                drawPath(
                    path = basePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            yellowPrimary.copy(alpha = 0.50f * yellowPulse),
                            yellowAmber.copy(alpha = 0.12f)
                        ),
                        startY = targetY,
                        endY = canvasHeight
                    )
                )

                // Left 3D Yellow Ground Guide Line
                drawLine(
                    color = yellowPrimary.copy(alpha = 0.95f),
                    start = Offset(canvasWidth * 0.15f, canvasHeight),
                    end = Offset(targetX - 42f, targetY),
                    strokeWidth = 7f
                )

                // Right 3D Yellow Ground Guide Line
                drawLine(
                    color = yellowPrimary.copy(alpha = 0.95f),
                    start = Offset(canvasWidth * 0.85f, canvasHeight),
                    end = Offset(targetX + 42f, targetY),
                    strokeWidth = 7f
                )

                // Center Dynamic Guidance Beam (Glowing Yellow/White)
                drawLine(
                    color = Color(0xFFFFFBEB).copy(alpha = 0.95f),
                    start = Offset(canvasWidth / 2f, canvasHeight),
                    end = Offset(targetX, targetY),
                    strokeWidth = 5f
                )

                // Perspective Transverse Grid Ties (Yellow Chevrons on Ground)
                val stepCount = 5
                for (step in 1..stepCount) {
                    val progress = step / (stepCount + 1f)
                    val y = canvasHeight - (progress * (canvasHeight - targetY))
                    val leftX = (canvasWidth * 0.15f) + (progress * ((targetX - 42f) - (canvasWidth * 0.15f)))
                    val rightX = (canvasWidth * 0.85f) + (progress * ((targetX + 42f) - (canvasWidth * 0.85f)))

                    drawLine(
                        color = yellowPrimary.copy(alpha = 0.65f * (1f - (progress * 0.4f))),
                        start = Offset(leftX, y),
                        end = Offset(rightX, y),
                        strokeWidth = 3.5f
                    )
                }
            }
        }

        // 3. 3D Floating Kaaba Marker in Camera Field of View
        val deltaKaaba = orientationState.deltaToQiblaDegrees
        val isKaabaInFov = deltaKaaba in -38f..38f

        if (isKaabaInFov) {
            val horizontalOffsetPercent = (deltaKaaba / 38f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 170.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset {
                        IntOffset(x = (-horizontalOffsetPercent * 320).roundToInt(), y = 0)
                    }
                ) {
                    // Floating 3D Kaaba Icon with Yellow/Emerald Halo
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                if (orientationState.isAlignedWithQibla) Color(0xFF10B981) else Color(0xEE0F172A)
                            )
                            .border(
                                2.5.dp,
                                if (orientationState.isAlignedWithQibla) Color(0xFFFFD700) else Color(0xFFFBBF24),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🕋",
                            fontSize = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (orientationState.isAlignedWithQibla) Color(0xEE064E3B) else Color(0xEE0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (orientationState.isAlignedWithQibla) Color(0xFFFFD700) else Color(0x66F59E0B)
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (orientationState.isAlignedWithQibla) "QIBLA LOCKED ✓" else "HOLY KAABA",
                                color = if (orientationState.isAlignedWithQibla) Color(0xFFFFD700) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                            Text(
                                text = "${orientationState.distanceToKaabaKm} km • Makkah Al-Mukarramah",
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. 3D Floating Nearby Mosques Markers
        if (showNearbyMasjids) {
            orientationState.nearbyMasjids.forEachIndexed { index, masjid ->
                var deltaM = (masjid.bearingDegrees - orientationState.azimuthDegrees)
                while (deltaM > 180f) deltaM -= 360f
                while (deltaM < -180f) deltaM += 360f

                if (deltaM in -42f..42f) {
                    val mOffset = (deltaM / 42f)
                    val yJitter = (index * 26) - 15

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = (270 + yJitter).dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xDD0F172A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .offset { IntOffset(x = (-mOffset * 340).roundToInt(), y = 0) }
                                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mosque,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = masjid.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${masjid.distanceMeters}m away • ${masjid.bearingDegrees.toInt()}°",
                                        color = Color(0xFFFFD700),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Top Navigation & Status HUD Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE091522)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.2.dp,
                        if (orientationState.isAlignedWithQibla) EmeraldAccent else Color(0x66FFD700),
                        RoundedCornerShape(18.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0x33FFFFFF), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (orientationState.isAlignedWithQibla) Color(0xFF10B981) else Color(0xFFFFD700)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (orientationState.isAlignedWithQibla) "QIBLA ALIGNED" else "SEEKING KAABA",
                                    color = if (orientationState.isAlignedWithQibla) Color(0xFF6EE7B7) else Color(0xFFFFD700),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Heading: ${orientationState.azimuthDegrees.toInt()}° | Qibla: ${orientationState.qiblaBearingDegrees.toInt()}°",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Turn direction hint badge
                    val deltaVal = orientationState.deltaToQiblaDegrees
                    val turnHint = when {
                        orientationState.isAlignedWithQibla -> "Aligned ✓"
                        deltaVal > 0 -> "Turn Right ${deltaVal.toInt()}°"
                        else -> "Turn Left ${kotlin.math.abs(deltaVal).toInt()}°"
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (orientationState.isAlignedWithQibla) Color(0x3310B981) else Color(0x33FFD700)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = turnHint,
                            color = if (orientationState.isAlignedWithQibla) Color(0xFF6EE7B7) else Color(0xFFFFD700),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // AR Overlay Feature Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showGroundPath,
                    onClick = { showGroundPath = !showGroundPath },
                    label = { Text("Yellow Ground Lines", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x33FFD700),
                        selectedLabelColor = Color(0xFFFFD700)
                    )
                )
                FilterChip(
                    selected = showNearbyMasjids,
                    onClick = { showNearbyMasjids = !showNearbyMasjids },
                    label = { Text("3D Mosques (${orientationState.nearbyMasjids.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0x3310B981),
                        selectedLabelColor = Color(0xFF6EE7B7)
                    )
                )
            }
        }

        // 6. Bottom HUD Info Card (STRICTLY NO 2D COMPASS DIAL OR NEEDLE)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xEE05101A)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x44FFD700), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Holy Kaaba (Makkah Al-Mukarramah)",
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = "Distance: ${orientationState.distanceToKaabaKm} km • Bearing: ${orientationState.qiblaBearingDegrees.toInt()}°",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Pure Camera AR Mode • Follow Yellow Lines to Face Kaaba",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    IconButton(
                        onClick = { arEngine.updateLocation(ArQiblaEngine.DEFAULT_USER_LAT, ArQiblaEngine.DEFAULT_USER_LNG) },
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0x2238BDF8), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recalibrate Sensors",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun triggerHapticPulse(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(80)
        }
    } catch (_: Exception) {}
}
