package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.LocationInfo
import com.example.data.repository.LocationRepository
import com.example.sensor.CompassSensorManager
import com.example.service.location.LocationServicesWrapper
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.util.QiblaCalculator
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * MODULE 7: 3D AR-ONLY QIBLA UI PURGE
 * Purged old analog canvas dial, needle, and complex rotation canvas.
 * CameraX AR background with Massive Digital Direction Banner.
 */
@Composable
fun QiblaScreen(
    currentLocation: LocationInfo = LocationRepository.getDefaultLocation(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val locationWrapper = remember(context) { LocationServicesWrapper.getInstance(context) }
    val cachedLoc by locationWrapper.cachedLocationFlow.collectAsStateWithLifecycle(
        initialValue = locationWrapper.currentLocationState.value
    )

    // Evaluate coordinates: prefer cached high-accuracy coordinates if present; fallback to passed currentLocation
    val activeLat = if (cachedLoc.timestamp > 0L) cachedLoc.latitude else currentLocation.latitude
    val activeLng = if (cachedLoc.timestamp > 0L) cachedLoc.longitude else currentLocation.longitude
    val activeCity = if (cachedLoc.timestamp > 0L) cachedLoc.cityName else currentLocation.cityName
    val activeCountry = if (cachedLoc.timestamp > 0L) cachedLoc.countryName else currentLocation.countryName

    var isCalibratingGps by remember { mutableStateOf(false) }
    var isArViewMode by remember { mutableStateOf(true) }

    // Request GPS permission and coordinates using FusedLocationProviderClient
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            coroutineScope.launch {
                isCalibratingGps = true
                locationWrapper.requestHighAccuracyQiblaLocation(forceFreshGps = true)
                isCalibratingGps = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!locationWrapper.hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            isCalibratingGps = true
            locationWrapper.requestHighAccuracyQiblaLocation(forceFreshGps = false)
            isCalibratingGps = false
        }
    }

    // Camera permission handling
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Compass sensor manager
    val compassSensorManager = remember { CompassSensorManager(context) }
    DisposableEffect(Unit) {
        compassSensorManager.startListening()
        onDispose {
            compassSensorManager.stopListening()
        }
    }

    val currentAzimuth by compassSensorManager.azimuthFlow.collectAsStateWithLifecycle(initialValue = 0f)
    val qiblaBearing = remember(activeLat, activeLng) {
        QiblaCalculator.calculateQiblaBearing(activeLat, activeLng)
    }
    val distanceKm = remember(activeLat, activeLng) {
        QiblaCalculator.calculateDistanceToKaaba(activeLat, activeLng)
    }

    // MODULE 7 LOGIC: val relativeAngle = (qiblaBearing - currentAzimuth + 360f) % 360f
    val relativeAngle = ((qiblaBearing - currentAzimuth + 360f) % 360f)
    val isFacingKaaba = relativeAngle < 3f || relativeAngle > 357f

    // Success Green color definition
    val successGreen = Color(0xFF10B981)
    val bannerBgColor by animateColorAsState(
        targetValue = if (isFacingKaaba) successGreen else DeepRoyalEmerald,
        animationSpec = tween(300),
        label = "BannerBgColor"
    )

    // Haptic feedback when lock-on achieved
    var hasVibratedOnLock by remember { mutableStateOf(false) }
    LaunchedEffect(isFacingKaaba) {
        if (isFacingKaaba && !hasVibratedOnLock) {
            hasVibratedOnLock = true
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 200), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(150)
                }
            } catch (_: Exception) {}
        } else if (!isFacingKaaba) {
            hasVibratedOnLock = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("ar_qibla_screen")
    ) {
        // 1. CameraX PreviewView or Celestial 3D Compass Background
        if (isArViewMode && hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant Celestial Islamic Compass Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF064E3B), // Deep Emerald Glow
                                Color(0xFF0A192F), // Islamic Navy
                                Color(0xFF020617)  // Deepest Midnight
                            )
                        )
                    )
            ) {
                // Subtle Islamic Star / Circle aura
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = PlatinumGold.copy(alpha = 0.05f),
                        radius = size.minDimension * 0.48f,
                        center = center
                    )
                    drawCircle(
                        color = RadiantEmerald.copy(alpha = 0.04f),
                        radius = size.minDimension * 0.38f,
                        center = center
                    )
                }
            }
        }

        // Overlay scrim with subtle gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Navigation Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isArViewMode && hasCameraPermission) "AR Qibla Viewfinder" else "3D Precision Compass",
                        color = PureWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-Time Sensor & GPS Fusion",
                        color = PlatinumGold,
                        fontSize = 11.sp
                    )
                }
            }

            // Mode Toggle Button (AR Camera vs Digital Compass)
            IconButton(
                onClick = {
                    if (!hasCameraPermission && !isArViewMode) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    isArViewMode = !isArViewMode
                },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .border(1.dp, PlatinumGold.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isArViewMode && hasCameraPermission) Icons.Default.Explore else Icons.Default.Videocam,
                    contentDescription = "Toggle AR/Compass",
                    tint = PlatinumGold
                )
            }
        }

        // Massive Digital Banner at the top of Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = bannerBgColor,
            border = androidx.compose.foundation.BorderStroke(2.dp, PlatinumGold.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Determine text according to user spec
                val bannerText = when {
                    isFacingKaaba -> "FACING KAABA (0°)"
                    relativeAngle > 180f -> "TURN LEFT ${(360f - relativeAngle).toInt()}°"
                    else -> "TURN RIGHT ${relativeAngle.toInt()}°"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isFacingKaaba) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else if (relativeAngle > 180f) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateLeft,
                            contentDescription = null,
                            tint = PlatinumGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.RotateRight,
                            contentDescription = null,
                            tint = PlatinumGold,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Text(
                        text = bannerText,
                        color = PureWhite,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isFacingKaaba) "Direct Alignment with Holy Kaaba (Al-Masjid al-Haram)" else "Rotate your device horizontally until aligned",
                    color = PureWhite.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Center Dynamic Sensor-Based Compass Dial & Rotating Qibla Needle (Size: 280.dp)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp),
            contentAlignment = Alignment.Center
        ) {
            // 1. Outer Compass Rose Dial - Rotates with -currentAzimuth so N points true North
            Box(
                modifier = Modifier
                    .size(270.dp)
                    .rotate(-currentAzimuth),
                contentAlignment = Alignment.Center
            ) {
                // Compass Circle with tick marks and cardinal points
                Canvas(modifier = Modifier.size(260.dp)) {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Draw outer border ring
                    drawCircle(
                        color = PlatinumGold.copy(alpha = 0.4f),
                        radius = radius,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    )

                    // Draw 30-degree tick marks
                    for (degree in 0 until 360 step 30) {
                        val angleRad = Math.toRadians((degree - 90).toDouble())
                        val isCardinal = degree % 90 == 0
                        val tickLen = if (isCardinal) 12.dp.toPx() else 6.dp.toPx()
                        val startX = (center.x + (radius - tickLen) * cos(angleRad)).toFloat()
                        val startY = (center.y + (radius - tickLen) * sin(angleRad)).toFloat()
                        val endX = (center.x + radius * cos(angleRad)).toFloat()
                        val endY = (center.y + radius * sin(angleRad)).toFloat()

                        drawLine(
                            color = if (isCardinal) PlatinumGold else PlatinumGold.copy(alpha = 0.35f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (isCardinal) 2.5.dp.toPx() else 1.5.dp.toPx()
                        )
                    }
                }

                // Cardinal letters: N, E, S, W
                Text(
                    text = "N",
                    color = Color(0xFFEF4444),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                )
                Text(
                    text = "E",
                    color = PureWhite.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                )
                Text(
                    text = "S",
                    color = PureWhite.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                )
                Text(
                    text = "W",
                    color = PureWhite.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
                )
            }

            // 2. Qibla Direction Needle - Points directly towards Kaaba in real-time
            // Angle relative to phone orientation: relativeAngle = (qiblaBearing - currentAzimuth + 360) % 360
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .rotate(relativeAngle),
                contentAlignment = Alignment.Center
            ) {
                // Upper Kaaba Needle Indicator
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Kaaba Direction Arrow",
                        tint = if (isFacingKaaba) successGreen else PlatinumGold,
                        modifier = Modifier.size(34.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isFacingKaaba) successGreen else PlatinumGold.copy(alpha = 0.9f),
                        modifier = Modifier.padding(1.dp)
                    ) {
                        Text(
                            text = "KAABA",
                            color = if (isFacingKaaba) PureWhite else Color.Black,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // 3. Central Target Reticle Hub
            Surface(
                shape = CircleShape,
                color = if (isFacingKaaba) successGreen.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.55f),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isFacingKaaba) 3.5.dp else 1.5.dp,
                    color = if (isFacingKaaba) successGreen else PlatinumGold
                ),
                modifier = Modifier.size(130.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = "Kaaba Target",
                            tint = if (isFacingKaaba) PureWhite else PlatinumGold,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isFacingKaaba) "0° ALIGNED" else "${relativeAngle.toInt()}°",
                            color = if (isFacingKaaba) PureWhite else PlatinumGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Bottom telemetry card
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.75f),
            border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Location & DataStore Cache Telemetry Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = PlatinumGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$activeCity, $activeCountry",
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (cachedLoc.isHighAccuracy) Color(0xFF10B981).copy(alpha = 0.2f) else PlatinumGold.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(
                                0.5.dp,
                                if (cachedLoc.isHighAccuracy) Color(0xFF10B981) else PlatinumGold
                            )
                        ) {
                            Text(
                                text = if (isCalibratingGps) "Calibrating..." else if (cachedLoc.isHighAccuracy) "High-Acc GPS" else "15m Cache",
                                color = if (cachedLoc.isHighAccuracy) Color(0xFF10B981) else PlatinumGold,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isCalibratingGps = true
                                    locationWrapper.requestHighAccuracyQiblaLocation(forceFreshGps = true)
                                    isCalibratingGps = false
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Calibrate GPS",
                                tint = if (isCalibratingGps) PlatinumGold else PureWhite.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "QIBLA BEARING",
                            color = PlatinumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${qiblaBearing.toInt()}° WSW",
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CURRENT HEADING",
                            color = PlatinumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${currentAzimuth.toInt()}°",
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "MAKKAH DISTANCE",
                            color = PlatinumGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$distanceKm km",
                            color = PureWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
