package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Navigation
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.theme.GoldSecondary
import kotlin.math.roundToInt

@Composable
fun ArQiblaScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val arEngine = remember { ArQiblaEngine(context) }
    val orientationState by arEngine.orientationState.collectAsState()

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
        if (perm == android.content.pm.PackageManager.PERMISSION_GRANTED) {
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
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlow"
    )

    val beamFlow by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BeamFlow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ar_qibla_screen")
            .background(DarkNavyBackground)
    ) {
        // 1. Live Camera Preview (or elegant simulated viewfinder if permission pending)
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
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
                        contentDescription = "Camera Permission Needed",
                        tint = GoldSecondary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Camera Access Required for AR View",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Point camera at your surroundings to see 3D Qibla path",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                    ) {
                        Text("Grant Camera Permission", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. AR 3D Ground Path Overlay
        if (showGroundPath) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Calculate screen X offset based on delta to Qibla (horizontal FOV approx 60 degrees)
                val fovDegrees = 60f
                val delta = orientationState.deltaToQiblaDegrees
                val normalizedX = (delta / (fovDegrees / 2f)).coerceIn(-1.5f, 1.5f)

                val targetX = (canvasWidth / 2f) - (normalizedX * (canvasWidth / 2f))
                val targetY = canvasHeight * 0.45f // Horizon level

                val basePath = Path().apply {
                    moveTo(canvasWidth * 0.2f, canvasHeight)
                    lineTo(canvasWidth * 0.8f, canvasHeight)
                    lineTo(targetX + 35f, targetY)
                    lineTo(targetX - 35f, targetY)
                    close()
                }

                val pathColor = if (orientationState.isAlignedWithQibla) {
                    Color(0xFF10B981)
                } else {
                    Color(0xFFF59E0B)
                }

                drawPath(
                    path = basePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            pathColor.copy(alpha = 0.55f),
                            pathColor.copy(alpha = 0.15f)
                        ),
                        startY = targetY,
                        endY = canvasHeight
                    )
                )

                // Dynamic glowing perspective beam lines
                drawLine(
                    color = pathColor.copy(alpha = 0.8f),
                    start = Offset(canvasWidth * 0.2f, canvasHeight),
                    end = Offset(targetX - 35f, targetY),
                    strokeWidth = 4f
                )
                drawLine(
                    color = pathColor.copy(alpha = 0.8f),
                    start = Offset(canvasWidth * 0.8f, canvasHeight),
                    end = Offset(targetX + 35f, targetY),
                    strokeWidth = 4f
                )
                // Center guidance line with animated energy pulses
                drawLine(
                    color = Color.White.copy(alpha = 0.9f),
                    start = Offset(canvasWidth / 2f, canvasHeight),
                    end = Offset(targetX, targetY),
                    strokeWidth = 3f
                )
            }
        }

        // 3. 3D Floating Kaaba Pin Marker in Camera View
        val deltaKaaba = orientationState.deltaToQiblaDegrees
        val isKaabaInFov = deltaKaaba in -35f..35f

        if (isKaabaInFov) {
            val horizontalOffsetPercent = (deltaKaaba / 35f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 180.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset {
                        IntOffset(x = (-horizontalOffsetPercent * 320).roundToInt(), y = 0)
                    }
                ) {
                    // Floating 3D Kaaba badge
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                if (orientationState.isAlignedWithQibla) Color(0xFF10B981) else Color(0xDD111827)
                            )
                            .border(
                                2.dp,
                                if (orientationState.isAlignedWithQibla) GoldSecondary else Color(0xFFF59E0B),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🕋",
                                fontSize = 28.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (orientationState.isAlignedWithQibla) Color(0xEE064E3B) else Color(0xEE1E293B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (orientationState.isAlignedWithQibla) "QIBLA LOCKED ✓" else "HOLY KAABA",
                                color = if (orientationState.isAlignedWithQibla) GoldSecondary else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "${orientationState.distanceToKaabaKm} km • Makkah",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // 4. 3D Floating Nearby Mosques Pins
        if (showNearbyMasjids) {
            orientationState.nearbyMasjids.forEachIndexed { index, masjid ->
                var deltaM = (masjid.bearingDegrees - orientationState.azimuthDegrees)
                while (deltaM > 180f) deltaM -= 360f
                while (deltaM < -180f) deltaM += 360f

                if (deltaM in -40f..40f) {
                    val mOffset = (deltaM / 40f)
                    val yJitter = (index * 24) - 20

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = (280 + yJitter).dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xCC0F172A)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .offset { IntOffset(x = (-mOffset * 340).roundToInt(), y = 0) }
                                .border(1.dp, Color(0x6610B981), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mosque,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
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
                                        color = GoldSecondary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Top Status HUD Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xDD091522)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (orientationState.isAlignedWithQibla) EmeraldAccent else Color(0x33F59E0B),
                        RoundedCornerShape(16.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (orientationState.isAlignedWithQibla) Color(0xFF10B981) else Color(0xFFF59E0B))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (orientationState.isAlignedWithQibla) "QIBLA ALIGNED" else "SEEKING KAABA",
                                color = if (orientationState.isAlignedWithQibla) Color(0xFF6EE7B7) else Color(0xFFFBBF24),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Heading: ${orientationState.azimuthDegrees.toInt()}° | Qibla: ${orientationState.qiblaBearingDegrees.toInt()}°",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Turn direction hint
                    val deltaVal = orientationState.deltaToQiblaDegrees
                    val turnHint = when {
                        orientationState.isAlignedWithQibla -> "Face Here"
                        deltaVal > 0 -> "Turn Right ${deltaVal.toInt()}°"
                        else -> "Turn Left ${kotlin.math.abs(deltaVal).toInt()}°"
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (orientationState.isAlignedWithQibla) Color(0x3310B981) else Color(0x33F59E0B)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = turnHint,
                            color = if (orientationState.isAlignedWithQibla) Color(0xFF6EE7B7) else Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AR Overlay Feature Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showGroundPath,
                    onClick = { showGroundPath = !showGroundPath },
                    label = { Text("3D Ground Path", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldAccent.copy(alpha = 0.25f),
                        selectedLabelColor = EmeraldAccent
                    )
                )
                FilterChip(
                    selected = showNearbyMasjids,
                    onClick = { showNearbyMasjids = !showNearbyMasjids },
                    label = { Text("3D Mosques (${orientationState.nearbyMasjids.size})", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldSecondary.copy(alpha = 0.25f),
                        selectedLabelColor = GoldSecondary
                    )
                )
            }
        }

        // 6. Bottom Compass Radar Mini-Dial
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 28.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xEE05101A)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(20.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mini Compass Dial
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(1.dp, Color(0xFF334155), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Compass Pointer",
                            tint = if (orientationState.isAlignedWithQibla) EmeraldAccent else GoldSecondary,
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(-orientationState.azimuthDegrees + orientationState.qiblaBearingDegrees)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Holy Kaaba, Makkah",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Lat: 21.4225° N, Lng: 39.8262° E",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }

                    IconButton(
                        onClick = { arEngine.updateLocation(ArQiblaEngine.DEFAULT_USER_LAT, ArQiblaEngine.DEFAULT_USER_LNG) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recalibrate Sensors",
                            tint = Color(0xFF38BDF8)
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
