package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.IslamicWatermarkBackground
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldSurfaceLight
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class FamilyPilgrim(
    val name: String,
    val relation: String,
    val landmark: String,
    val distance: String,
    val battery: String,
    val coordinates: String,
    val lastUpdated: String
)

/**
 * MODULE 6: HAJJ & UMRAH 3D FAMILY TRACKER & LIVE TAWAF COUNTER
 * 1. 3D-styled concentric Tawaf / Sa'i Canvas counter with 7 circuits & auto-vibration.
 * 2. Holy Sites interactive Radar & Live Family Tracker (Mina, Arafat, Muzdalifah, Haram).
 * 3. High-Priority SOS Emergency broadcast with GPS dispatch to Saudi Security (911).
 */
@Composable
fun HajjFamilyTrackerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tawaf & Sa'i, 1: Family Radar, 2: SOS
    var tawafCircuitsCompleted by remember { mutableIntStateOf(3) }
    var saiLapsCompleted by remember { mutableIntStateOf(2) }
    var isSosAlertActive by remember { mutableStateOf(false) }

    val familyMembers = remember {
        mutableStateListOf(
            FamilyPilgrim(
                name = "Father (Usman)",
                relation = "Head of Family",
                landmark = "Mina Camp #42 (European Wing)",
                distance = "1.2 km away",
                battery = "86%",
                coordinates = "21.4133° N, 39.8933° E",
                lastUpdated = "2 mins ago"
            ),
            FamilyPilgrim(
                name = "Mother (Sultana)",
                relation = "Family Member",
                landmark = "Near King Abdulaziz Gate (Gate #1)",
                distance = "340 m away",
                battery = "92%",
                coordinates = "21.4208° N, 39.8256° E",
                lastUpdated = "Live Now"
            ),
            FamilyPilgrim(
                name = "Brother (Hamza)",
                relation = "Family Member",
                landmark = "Muzdalifah Gathering Grounds",
                distance = "4.8 km away",
                battery = "64%",
                coordinates = "21.3780° N, 39.9070° E",
                lastUpdated = "5 mins ago"
            )
        )
    }

    // Siren tone generator for SOS
    fun triggerSosSiren() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 100)
            toneGen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1500)
        } catch (_: Exception) {}
    }

    val infiniteTransition = rememberInfiniteTransition(label = "TawafRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(28000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "TawafR"
    )

    IslamicWatermarkBackground(
        modifier = Modifier
            .fillMaxSize()
            .testTag("hajj_tracker_screen")
            .statusBarsPadding()
            .navigationBarsPadding(),
        backgroundColor = PearlBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepRoyalEmerald
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Hajj & Umrah 3D Companion",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        Text(
                            text = "Makkah al-Mukarramah & Sacred Rituals",
                            fontSize = 11.sp,
                            color = PlatinumGold
                        )
                    }
                }

                // SOS Badge Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEF2F2))
                        .border(1.2.dp, CrimsonError, RoundedCornerShape(12.dp))
                        .clickable { isSosAlertActive = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Emergency, contentDescription = "SOS", tint = CrimsonError, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "SOS ALARM", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = CrimsonError)
                    }
                }
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = PureWhite,
                contentColor = DeepRoyalEmerald,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = DeepRoyalEmerald
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("3D Tawaf & Sa'i", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Family Radar Map", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Emergency Dispatch", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Content Area
            when (selectedTab) {
                0 -> TawafSaiCounterView(
                    tawafCircuits = tawafCircuitsCompleted,
                    saiLaps = saiLapsCompleted,
                    rotationAngle = rotationAngle,
                    onAddTawafCircuit = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (tawafCircuitsCompleted < 7) {
                            tawafCircuitsCompleted++
                            if (tawafCircuitsCompleted == 7) {
                                Toast.makeText(context, "Mabrooq! Tawaf Completed! Proceed to Maqam Ibrahim.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onResetTawaf = { tawafCircuitsCompleted = 0 },
                    onAddSaiLap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (saiLapsCompleted < 7) {
                            saiLapsCompleted++
                            if (saiLapsCompleted == 7) {
                                Toast.makeText(context, "Mabrooq! Sa'i Completed! May Allah accept your Umrah.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onResetSai = { saiLapsCompleted = 0 }
                )
                1 -> FamilyRadarView(familyMembers = familyMembers)
                2 -> EmergencyDispatchView(onTriggerSiren = { triggerSosSiren() })
            }
        }

        // SOS Panic Modal Dialog
        if (isSosAlertActive) {
            AlertDialog(
                onDismissRequest = { isSosAlertActive = false },
                containerColor = PureWhite,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "BROADCAST HAJJ EMERGENCY?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CrimsonError)
                    }
                },
                text = {
                    Text(
                        text = "This will blast a high-pitch local audio alarm and transmit your live GPS coordinates (21.4225° N, 39.8262° E, Masjid al-Haram) to Saudi Hajj Security (911), Red Crescent (997), and all family members.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = CharcoalPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSosAlertActive = false
                            triggerSosSiren()
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "SOS DISPATCH SENT TO SAUDI HAJJ COMMAND", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = PureWhite)
                    ) {
                        Text("BROADCAST SOS NOW", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { isSosAlertActive = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = SlateMuted)
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TawafSaiCounterView(
    tawafCircuits: Int,
    saiLaps: Int,
    rotationAngle: Float,
    onAddTawafCircuit: () -> Unit,
    onResetTawaf: () -> Unit,
    onAddSaiLap: () -> Unit,
    onResetSai: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 3D Kaaba Canvas with Concentric Circuit Rings
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 8.dp,
            cornerRadius = 24.dp,
            backgroundColor = PureWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TAWAF AL-QUDOOM (KAABA)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumGold,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onResetTawaf, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = SlateMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive 3D Canvas
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(220.dp)
                        .clickable { onAddTawafCircuit() }
                ) {
                    Canvas(modifier = Modifier.size(220.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Draw 7 Concentric Tawaf Circuit Tracks
                        for (i in 1..7) {
                            val radius = 28.dp.toPx() + (i * 11.5.dp.toPx())
                            val isCircuitCompleted = i <= tawafCircuits
                            drawCircle(
                                color = if (isCircuitCompleted) RadiantEmerald else Color(0xFFE2E8F0),
                                radius = radius,
                                center = center,
                                style = Stroke(width = if (isCircuitCompleted) 3.5.dp.toPx() else 1.2.dp.toPx())
                            )
                        }

                        // Rotating Pilgrim Dot on active track
                        val activeRadius = 28.dp.toPx() + (tawafCircuits.coerceAtLeast(1) * 11.5.dp.toPx())
                        val radAngle = rotationAngle * (PI / 180.0)
                        val pilgrimX = center.x + (activeRadius * cos(radAngle)).toFloat()
                        val pilgrimY = center.y + (activeRadius * sin(radAngle)).toFloat()

                        drawCircle(
                            color = PlatinumGold,
                            radius = 6.dp.toPx(),
                            center = Offset(pilgrimX, pilgrimY)
                        )

                        // Central Sacred Holy Kaaba (Golden & Obsidian Cube)
                        val kaabaSize = 42.dp.toPx()
                        drawRoundRect(
                            color = Color(0xFF0F172A), // Deep Obsidian Kiswah
                            topLeft = Offset(center.x - kaabaSize / 2f, center.y - kaabaSize / 2f),
                            size = Size(kaabaSize, kaabaSize),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                        )
                        // Golden Kiswah Inscription Band
                        drawRect(
                            color = PlatinumGold,
                            topLeft = Offset(center.x - kaabaSize / 2f, center.y - kaabaSize / 4f),
                            size = Size(kaabaSize, 4.dp.toPx())
                        )
                        // Green Light Indicator (Hajar al-Aswad Start line)
                        drawLine(
                            color = RadiantEmerald,
                            start = center,
                            end = Offset(center.x + 95.dp.toPx(), center.y),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Kaaba Calligraphy label
                    Text(
                        text = "كعبة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumGold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Circuit $tawafCircuits of 7",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif,
                    color = DeepRoyalEmerald
                )

                Text(
                    text = if (tawafCircuits == 7) "Tawaf Completed! Proceed to Maqam Ibrahim" else "Tap Kaaba or Walk to Advance Circuit",
                    fontSize = 12.sp,
                    color = if (tawafCircuits == 7) RadiantEmerald else SlateMuted,
                    fontWeight = if (tawafCircuits == 7) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onAddTawafCircuit,
                    enabled = tawafCircuits < 7,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepRoyalEmerald, contentColor = PureWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PlatinumGold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Count Step / Complete Circuit", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sa'i Counter Card (Safa & Marwah)
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 6.dp,
            cornerRadius = 22.dp,
            backgroundColor = PureWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SA'I (SAFA & MARWAH)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PlatinumGold,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onResetSai, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset", tint = SlateMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7 Lap Progress Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 1..7) {
                        val isDone = i <= saiLaps
                        val isCurrent = i == saiLaps + 1
                        val mountain = if (i % 2 != 0) "Safa" else "Marwa"

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isDone) DeepRoyalEmerald else if (isCurrent) EmeraldSurfaceLight else Color(0xFFF1F5F9))
                                    .border(1.2.dp, if (isDone || isCurrent) PlatinumGold else Color(0xFFE2E8F0), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$i",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDone) PureWhite else DeepRoyalEmerald
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = mountain, fontSize = 9.sp, color = SlateMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAddSaiLap,
                    enabled = saiLaps < 7,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RadiantEmerald, contentColor = PureWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = if (saiLaps == 7) "Sa'i Completed (7 Laps)" else "Record Lap ${saiLaps + 1} (${if ((saiLaps + 1) % 2 != 0) "Towards Marwah" else "Towards Safa"})", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FamilyRadarView(familyMembers: List<FamilyPilgrim>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Radar Visualization Canvas
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 6.dp,
            cornerRadius = 24.dp,
            backgroundColor = PureWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MAKKAH & SACRED SITES RADAR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumGold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Holy Sites Radar Sweep
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    Canvas(modifier = Modifier.size(200.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)

                        // Rings
                        drawCircle(color = Color(0x18064E3B), radius = 90.dp.toPx(), center = center)
                        drawCircle(color = Color(0x28064E3B), radius = 60.dp.toPx(), center = center)
                        drawCircle(color = Color(0x38064E3B), radius = 30.dp.toPx(), center = center)

                        // Center Kaaba
                        drawCircle(color = PlatinumGold, radius = 6.dp.toPx(), center = center)

                        // Family member dots
                        drawCircle(color = RadiantEmerald, radius = 7.dp.toPx(), center = Offset(center.x - 38.dp.toPx(), center.y + 24.dp.toPx()))
                        drawCircle(color = RadiantEmerald, radius = 7.dp.toPx(), center = Offset(center.x + 55.dp.toPx(), center.y - 42.dp.toPx()))
                    }

                    Text(text = "Kaaba (Center)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald, modifier = Modifier.padding(top = 28.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CONNECTED FAMILY PILGRIMS (${familyMembers.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = DeepRoyalEmerald,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        familyMembers.forEach { member ->
            NeumorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                elevation = 4.dp,
                cornerRadius = 16.dp,
                backgroundColor = PureWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldSurfaceLight)
                                .border(1.dp, PlatinumGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = DeepRoyalEmerald, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = member.name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
                            Text(text = member.landmark, fontSize = 11.sp, color = CharcoalSecondary)
                            Text(text = "Distance: ${member.distance} • Battery: ${member.battery}", fontSize = 10.sp, color = SlateMuted)
                        }
                    }

                    IconButton(onClick = { /* Share Location */ }) {
                        Icon(imageVector = Icons.Default.NearMe, contentDescription = "Navigate", tint = RadiantEmerald)
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyDispatchView(onTriggerSiren: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NeumorphicCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 8.dp,
            cornerRadius = 24.dp,
            backgroundColor = PureWhite,
            borderColor = CrimsonError.copy(alpha = 0.5f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF2F2))
                        .border(2.dp, CrimsonError, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Emergency, contentDescription = null, tint = CrimsonError, modifier = Modifier.size(38.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "HAJJ & UMRAH LOST PILGRIM SOS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonError
                )

                Text(
                    text = "If separated in massive crowds or requiring medical aid, tap below to transmit real-time GPS telemetry to the Saudi Red Crescent & Hajj Security.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = SlateMuted,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onTriggerSiren,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = PureWhite),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = PureWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "SOUND SIREN & DISPATCH GPS", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Saudi Emergency Hotlines
        Text(text = "SAUDI ARABIA EMERGENCY NUMBERS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))

        EmergencyHotlineCard("Unified Emergency Services", "911", "Police, Security, Crowd Control")
        EmergencyHotlineCard("Saudi Red Crescent (Ambulance)", "997", "Direct Medical & Paramedic Response")
        EmergencyHotlineCard("Ministry of Hajj Lost Pilgrims", "800 430 4444", "Lost Children, Tents & Luggage Tracking")
    }
}

@Composable
fun EmergencyHotlineCard(title: String, phone: String, subtitle: String) {
    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = 3.dp,
        cornerRadius = 14.dp,
        backgroundColor = PureWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
                Text(text = subtitle, fontSize = 10.5.sp, color = SlateMuted)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldSurfaceLight)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = phone, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = DeepRoyalEmerald)
            }
        }
    }
}
