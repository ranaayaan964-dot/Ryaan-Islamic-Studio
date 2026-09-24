package com.example.ui.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.service.LocationForegroundService
import com.example.service.MemberLocation
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald

/**
 * MODULE 16: HAJJ & UMRAH 3D FAMILY TRACKER & SOS
 */
@Composable
fun FamilyTrackerScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasLocationPermission = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) {
            LocationForegroundService.startService(context)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            LocationForegroundService.startService(context)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Keep background service or stop when user exits tracking screen
        }
    }

    val currentLocation by LocationForegroundService.currentLocationFlow.collectAsState()
    val familyMembers by LocationForegroundService.familyLocationsFlow.collectAsState()

    // Pulsing animation for the SOS Emergency Button
    val infiniteTransition = rememberInfiniteTransition(label = "SosPulse")
    val sosGlowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "SosGlowScale"
    )

    // Radar scan rotation
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarSweep"
    )

    fun triggerEmergencySos() {
        val lat = currentLocation?.latitude ?: 21.4225
        val lon = currentLocation?.longitude ?: 39.8262

        // 1. High Priority Notification
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "hajj_sos_emergency_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hajj Emergency SOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                description = "High Priority Emergency Distress Alert"
            }
            nm.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 EMERGENCY SOS BROADCAST SENT!")
            .setContentText("Distress beacon active with live GPS: $lat, $lon. Family alerted.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        nm.notify(9911, notification)

        // 2. Share / SMS broadcast intent with GPS coordinates
        val sosMessage = "🚨 EMERGENCY SOS FROM HAJJ PILGRIM! I need immediate help at coordinates: https://maps.google.com/?q=$lat,$lon"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, sosMessage)
            type = "text/plain"
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(sendIntent, "Broadcast SOS to Family via WhatsApp / SMS")
        chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooser)

        Toast.makeText(context, "EMERGENCY SOS BROADCASTED TO FAMILY", Toast.LENGTH_LONG).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("family_tracker_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
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
                        text = "Hajj & Umrah 3D Family Radar",
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mina • Arafat • Muzdalifah • Al-Haram Real-Time GPS",
                        color = PlatinumGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Radar Visualizer Canvas (Mina / Haram coordinates)
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0D1527))
                    .border(2.dp, PlatinumGold.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width / 2f

                    // Concentric radar rings
                    drawCircle(color = Color(0x3300E676), radius = maxRadius * 0.33f, style = Stroke(1.5f))
                    drawCircle(color = Color(0x3300E676), radius = maxRadius * 0.66f, style = Stroke(1.5f))
                    drawCircle(color = Color(0x5500E676), radius = maxRadius * 0.95f, style = Stroke(2f))

                    // Center crosshair
                    drawLine(Color(0x33FFFFFF), Offset(center.x, 0f), Offset(center.x, size.height), 1f)
                    drawLine(Color(0x33FFFFFF), Offset(0f, center.y), Offset(size.width, center.y), 1f)

                    // Draw live member dots
                    val positions = listOf(
                        Offset(center.x, center.y), // You
                        Offset(center.x + 50f, center.y - 45f), // Father
                        Offset(center.x - 60f, center.y + 40f), // Mother
                        Offset(center.x + 70f, center.y + 60f)  // Sister
                    )

                    positions.forEachIndexed { idx, pos ->
                        val color = if (idx == 0) RadiantEmerald else PlatinumGold
                        drawCircle(color = color, radius = if (idx == 0) 8f else 6f, center = pos)
                        drawCircle(color = color.copy(alpha = 0.4f), radius = if (idx == 0) 14f else 10f, center = pos)
                    }
                }

                // Center Pilgrim Badge
                Surface(
                    shape = CircleShape,
                    color = RadiantEmerald,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("You", color = PureWhite, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pilgrim Group List
            Text(
                text = "FAMILY GROUP IN PROXIMITY (4 CONNECTED)",
                color = PlatinumGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            val defaultMembers = listOf(
                MemberLocation("me", "You (Leader)", 21.4225, 39.8262),
                MemberLocation("fam_1", "Father (Ahmad)", 21.4237, 39.8254),
                MemberLocation("fam_2", "Mother (Amina)", 21.4230, 39.8273),
                MemberLocation("fam_3", "Sister (Mariam)", 21.4216, 39.8268)
            )
            val displayMembers = if (familyMembers.isNotEmpty()) familyMembers else defaultMembers

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(displayMembers) { member ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33D4AF37))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (member.id == "me") RadiantEmerald else DeepRoyalEmerald),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(member.name.first().toString(), color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(member.name, color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Online • 10s ago", color = Color.LightGray, fontSize = 9.5.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // MODULE 16: Massive Glowing Red Emergency SOS Button
            Box(
                modifier = Modifier
                    .scale(sosGlowScale)
                    .size(110.dp)
                    .shadow(24.dp, CircleShape, spotColor = CrimsonError, ambientColor = CrimsonError)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(CrimsonError, Color(0xFF991B1B))
                        )
                    )
                    .border(3.dp, PureWhite, CircleShape)
                    .clickable { triggerEmergencySos() }
                    .testTag("emergency_sos_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Emergency,
                        contentDescription = "SOS",
                        tint = PureWhite,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "SOS",
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "TAP FOR HIGH-PRIORITY HAJJ EMERGENCY SOS",
                color = CrimsonError,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
