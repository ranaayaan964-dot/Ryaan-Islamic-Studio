package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.data.database.QazaPrayerEntity
import com.example.data.repository.QazaNamazRepository
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldSurfaceLight
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * MODULE: ADVANCED QAZA NAMAZ TRACKER (VERSION 1.1)
 * Luxury Pearl-White and Emerald themed Missed Prayers screen featuring:
 * - Real-time Jetpack Compose Canvas Donut Progress Wheel with animated gradient arcs
 * - Full Room persistence for 5 daily prayers (Fajr, Dhuhr, Asr, Maghrib, Isha) + Witr
 * - Interactive grid cards with instant +1 / -1 quick action buttons and heavy haptic feedback
 * - Scholarly Islamic guidance banner quoting Bukhari Hadith on fulfilling debts to Allah
 * - Bulk calculation wizard modal for calculating debts by days, months, or years
 */
@Composable
fun QazaNamazScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val repository = remember { QazaNamazRepository.getInstance(context) }

    LaunchedEffect(Unit) {
        repository.initializeDefaultsIfNeeded()
    }

    val prayers by repository.getQazaPrayersFlow().collectAsStateWithLifecycle(initialValue = emptyList())

    BackHandler {
        onNavigateBack()
    }

    var showCalculatorDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val totalMissed = prayers.sumOf { it.missedCount }
    val totalCompletedToday = prayers.sumOf { it.completedToday }
    val totalCompletedHistorical = prayers.sumOf { it.totalCompleted }
    val totalOverall = (totalMissed + totalCompletedHistorical).coerceAtLeast(1)
    val rawCompletionRatio = (totalCompletedHistorical.toFloat() / totalOverall.toFloat()).coerceIn(0f, 1f)

    val animatedSweepAngle by animateFloatAsState(
        targetValue = rawCompletionRatio * 360f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "qaza_donut_sweep"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PearlBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("qaza_namaz_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .border(1.dp, PlatinumGold.copy(alpha = 0.4f), CircleShape)
                            .testTag("qaza_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Qaza Namaz Tracker",
                            color = DeepRoyalEmerald,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "قضاء الصلوات • Fulfill Sacred Debts",
                            color = PlatinumGold,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showCalculatorDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldSurfaceLight)
                            .testTag("qaza_calculator_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calculator",
                            tint = RadiantEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .border(1.dp, Color(0x22000000), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1. JETPACK COMPOSE CANVAS PROGRESS WHEEL & STATS HERO CARD
                item {
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 8.dp,
                        cornerRadius = 24.dp,
                        backgroundColor = PureWhite,
                        borderColor = PlatinumGold.copy(alpha = 0.35f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Overall Qaza Debt Progress",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepRoyalEmerald
                            )
                            Text(
                                text = "Track every prayer completed towards liberation from debt",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Interactive Donut Chart using Jetpack Compose Canvas
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(210.dp)
                            ) {
                                Canvas(modifier = Modifier.size(190.dp)) {
                                    val strokeWidth = 18.dp.toPx()
                                    val diameter = size.minDimension - strokeWidth
                                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                                    val arcSize = Size(diameter, diameter)

                                    // Background Track Ring
                                    drawArc(
                                        color = Color(0xFFE2E8F0),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                    )

                                    // Dynamic Animated Completion Arc
                                    if (animatedSweepAngle > 0f) {
                                        drawArc(
                                            brush = Brush.sweepGradient(
                                                listOf(
                                                    RadiantEmerald,
                                                    PlatinumGold,
                                                    DeepRoyalEmerald,
                                                    RadiantEmerald
                                                )
                                            ),
                                            startAngle = -90f,
                                            sweepAngle = animatedSweepAngle,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        )

                                        // Glowing Indicator Dot at Arc Tip
                                        val angleRad = Math.toRadians((animatedSweepAngle - 90.0))
                                        val radius = diameter / 2f
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        val tipX = (centerX + radius * cos(angleRad)).toFloat()
                                        val tipY = (centerY + radius * sin(angleRad)).toFloat()

                                        drawCircle(
                                            color = PureWhite,
                                            radius = strokeWidth * 0.42f,
                                            center = Offset(tipX, tipY)
                                        )
                                        drawCircle(
                                            color = PlatinumGold,
                                            radius = strokeWidth * 0.28f,
                                            center = Offset(tipX, tipY)
                                        )
                                    }
                                }

                                // Center Content in Donut
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(rawCompletionRatio * 100).toInt()}%",
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Black,
                                        color = DeepRoyalEmerald,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Text(
                                        text = "FULFILLED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                        color = RadiantEmerald
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$totalMissed Remaining",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PlatinumGold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Three Metrics Badges
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PearlBackground)
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Remaining Debt", fontSize = 10.sp, color = SlateMuted)
                                    Text(
                                        text = "$totalMissed",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .width(1.dp)
                                        .background(Color(0xFFCBD5E1))
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Prayed Today", fontSize = 10.sp, color = SlateMuted)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "+$totalCompletedToday",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RadiantEmerald
                                        )
                                        if (totalCompletedToday > 0) {
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = RadiantEmerald, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .width(1.dp)
                                        .background(Color(0xFFCBD5E1))
                                )

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "All-Time Cleared", fontSize = 10.sp, color = SlateMuted)
                                    Text(
                                        text = "$totalCompletedHistorical",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepRoyalEmerald
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. ENCOURAGING ISLAMIC WISDOM BANNER
                item {
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 4.dp,
                        cornerRadius = 20.dp,
                        backgroundColor = PureWhite,
                        borderColor = PlatinumGold.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSurfaceLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SelfImprovement,
                                        contentDescription = "Wisdom",
                                        tint = DeepRoyalEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Prophetic Wisdom on Missed Prayers",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepRoyalEmerald
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "فَدَيْنُ اللَّهِ أَحَقُّ أَنْ يُقْضَى",
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = DeepRoyalEmerald,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "\"A debt owed to Allah has more right to be paid.\" [Sahih al-Bukhari 1953]",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CharcoalPrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Consensus of Muslim jurists holds that missed prayers remain an obligation upon the believer. Strive to offer at least 1 or 2 Qaza prayers with each daily Jamaat.",
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                                color = SlateMuted
                            )
                        }
                    }
                }

                // Section Label
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Obligatory Prayers (5 Farz + Witr)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald
                        )
                        Text(
                            text = "Tap +1 after offering",
                            fontSize = 11.sp,
                            color = RadiantEmerald,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 3. INTERACTIVE PRAYER CARDS GRID
                // Rendered as paired cards for beautiful compact and expanded layouts
                val pairedPrayers = prayers.chunked(2)
                items(pairedPrayers.size) { rowIndex ->
                    val pair = pairedPrayers[rowIndex]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        pair.forEach { prayer ->
                            Box(modifier = Modifier.weight(1f)) {
                                QazaPrayerCard(
                                    prayer = prayer,
                                    onPerformed = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            repository.recordPrayerPerformed(prayer.prayerId)
                                        }
                                    },
                                    onIncrementMissed = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scope.launch {
                                            repository.incrementMissed(prayer.prayerId, 1)
                                        }
                                    },
                                    onDecrementMissed = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scope.launch {
                                            repository.decrementMissed(prayer.prayerId, 1)
                                        }
                                    }
                                )
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // CALCULATOR / BULK WIZARD DIALOG
        if (showCalculatorDialog) {
            QazaCalculatorDialog(
                onDismiss = { showCalculatorDialog = false },
                onApplyDays = { days ->
                    scope.launch {
                        repository.bulkSetDays(days)
                    }
                    showCalculatorDialog = false
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }

        // RESET CONFIRMATION DIALOG
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text(text = "Reset Qaza Debt?", fontWeight = FontWeight.Bold, color = DeepRoyalEmerald) },
                text = { Text("Are you sure you want to clear your missed prayers counter? This action will set current debts to 0.", color = CharcoalSecondary) },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch { repository.resetAllDebts() }
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Text("Reset All Debts", color = PureWhite)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel", color = SlateMuted)
                    }
                },
                containerColor = PureWhite,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

/**
 * Individual Interactive Card for a specific Qaza Prayer (e.g. Fajr, Dhuhr, Witr).
 */
@Composable
private fun QazaPrayerCard(
    prayer: QazaPrayerEntity,
    onPerformed: () -> Unit,
    onIncrementMissed: () -> Unit,
    onDecrementMissed: () -> Unit
) {
    NeumorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("qaza_card_${prayer.prayerId}"),
        elevation = 5.dp,
        cornerRadius = 20.dp,
        backgroundColor = PureWhite,
        borderColor = if (prayer.missedCount == 0) RadiantEmerald.copy(alpha = 0.5f) else PlatinumGold.copy(alpha = 0.25f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Prayer Name + Arabic badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = prayer.prayerName,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepRoyalEmerald
                    )
                    Text(
                        text = "${prayer.rakats} Rak'at ${if (prayer.prayerId == "witr") "Wajib" else "Farz"}",
                        fontSize = 10.sp,
                        color = SlateMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldSurfaceLight)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = prayer.prayerArabic,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = RadiantEmerald
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Count Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Missed Debt",
                        fontSize = 10.sp,
                        color = SlateMuted
                    )
                    Text(
                        text = "${prayer.missedCount}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = if (prayer.missedCount > 0) CharcoalPrimary else RadiantEmerald
                    )
                }

                if (prayer.completedToday > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(RadiantEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${prayer.completedToday} today",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RadiantEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: +1 Prayed, + Missed, - Missed
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary -1 button (if adjusting missed debt down)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(PearlBackground)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        .clickable(enabled = prayer.missedCount > 0, onClick = onDecrementMissed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Reduce Missed",
                        tint = if (prayer.missedCount > 0) CharcoalPrimary else Color(0xFFCBD5E1),
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Primary "+1 Prayed" Button
                Button(
                    onClick = onPerformed,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("qaza_performed_${prayer.prayerId}"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepRoyalEmerald,
                        contentColor = PureWhite
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = PlatinumGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Prayed", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                // Secondary +1 button (if recording an additional missed prayer)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(PearlBackground)
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                        .clickable(onClick = onIncrementMissed),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Missed",
                        tint = CharcoalPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Quick Calculator Dialog for batch-setting missed prayers across days/months/years.
 */
@Composable
private fun QazaCalculatorDialog(
    onDismiss: () -> Unit,
    onApplyDays: (Int) -> Unit
) {
    var customDaysText by remember { mutableStateOf("") }
    val quickPresets = listOf(
        Pair("7 Days", 7),
        Pair("1 Month", 30),
        Pair("3 Months", 90),
        Pair("6 Months", 180),
        Pair("1 Year", 365)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Calculate, contentDescription = null, tint = DeepRoyalEmerald)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Qaza Debt Calculator", fontWeight = FontWeight.Bold, color = DeepRoyalEmerald, fontSize = 17.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select a preset or enter the estimated number of days of missed prayers to automatically calculate your obligations:",
                    fontSize = 12.sp,
                    color = CharcoalSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Presets Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.take(3).forEach { (label, days) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldSurfaceLight)
                                .border(1.dp, RadiantEmerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { onApplyDays(days) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickPresets.drop(3).forEach { (label, days) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(EmeraldSurfaceLight)
                                .border(1.dp, RadiantEmerald.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .clickable { onApplyDays(days) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = DeepRoyalEmerald)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = customDaysText,
                    onValueChange = { customDaysText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Or enter custom days", fontSize = 12.sp) },
                    placeholder = { Text("e.g. 45", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = customDaysText.toIntOrNull() ?: 0
                    if (days > 0) {
                        onApplyDays(days)
                    }
                },
                enabled = customDaysText.toIntOrNull() != null && customDaysText.toInt() > 0,
                colors = ButtonDefaults.buttonColors(containerColor = DeepRoyalEmerald)
            ) {
                Text("Set Debt", color = PureWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SlateMuted)
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(22.dp)
    )
}
