package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AdhanSoundPlayer
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerAlarmService
import com.example.alarm.TahajjudAlarmManager
import com.example.data.model.PrayerTimeItem
import com.example.data.model.PrayerType
import com.example.data.model.SahiwalPrayerSchedule
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.LocationRepository
import com.example.data.repository.AladhanRepository
import com.example.data.repository.PrayerApiProvider
import com.example.service.AutoSilentManager
import com.example.service.PrayerCountdownEngine
import com.example.ui.components.AppVersionManagementDialog
import com.example.ui.components.GlassCard
import com.example.ui.components.TestNotificationCard
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavySurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.util.AppUpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NextGenDashboardScreen(
    onLogout: (() -> Unit)? = null,
    onNavigateToSoundHub: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedProvider by remember { mutableStateOf(PrayerApiProvider.ALADHAN_EXACT_COORDINATES) }
    val initialData = remember { AladhanRepository.getSahiwalFallbackSchedule(PrayerApiProvider.ALADHAN_EXACT_COORDINATES) }
    var schedule by remember { mutableStateOf(initialData.first) }
    val prayerItems = remember { mutableStateListOf<PrayerTimeItem>().apply { addAll(initialData.second) } }
    var isLoading by remember { mutableStateOf(true) }
    var isPlayingAdhanTest by remember { mutableStateOf(false) }
    var showQiblaModal by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Live continuous ticking countdown engine state
    val countdownState by PrayerCountdownEngine.countdownState.collectAsState()

    // Alarm active states
    val isAlarmRinging by PrayerAlarmService.isAlarmRinging.collectAsState()
    val activePrayerName by PrayerAlarmService.activePrayerName.collectAsState()

    // Auto-Silent & Tahajjud States
    val autoSilentState by AutoSilentManager.silentState.collectAsState()
    val tahajjudState by TahajjudAlarmManager.state.collectAsState()

    // Sync Auto-Silent switch state with system DND permission on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasDnd = AutoSilentManager.hasNotificationPolicyAccess(context)
                if (autoSilentState.isEnabled && !hasDnd) {
                    AutoSilentManager.toggleEnabled(false, context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initialize Countdown Engine on first launch
    LaunchedEffect(Unit) {
        PrayerCountdownEngine.startEngine(prayerItems.toList(), schedule)
    }

    // Fetch Sahiwal API Data
    fun loadSahiwalTimings(provider: PrayerApiProvider = selectedProvider) {
        scope.launch {
            isLoading = true
            val result = AladhanRepository.fetchSahiwalPrayerTimings(provider)
            result.onSuccess { (fetchedSchedule, items) ->
                schedule = fetchedSchedule
                prayerItems.clear()
                prayerItems.addAll(items)
                TahajjudAlarmManager.updateFajrTime(fetchedSchedule.fajr)

                // Update continuous live countdown engine with new prayer timings and schedule
                PrayerCountdownEngine.updatePrayers(items, fetchedSchedule)

                // Schedule system alarms
                PrayerAlarmScheduler.scheduleAllActivePrayers(context, items)

                // Evaluate auto-silent condition
                AutoSilentManager.evaluateSilentCondition(context, items)
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedProvider) {
        loadSahiwalTimings(selectedProvider)
    }

    // Synchronize daily prayer schedule list badges & auto silent on every engine tick
    LaunchedEffect(countdownState.tickCounter) {
        val targetType = countdownState.nextPrayer?.type
        val isAllPassedToday = countdownState.isTomorrow
        val now = System.currentTimeMillis()
        for (i in prayerItems.indices) {
            val item = prayerItems[i]
            val passed = if (isAllPassedToday) {
                true
            } else {
                item.timeMillis <= now
            }
            val next = (item.type == targetType)
            if (item.isPassed != passed || item.isNext != next) {
                prayerItems[i] = item.copy(isPassed = passed, isNext = next)
            }
        }
        AutoSilentManager.evaluateSilentCondition(context, prayerItems)
    }

    // Glowing animations
    val infiniteTransition = rememberInfiniteTransition(label = "DashboardPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
            .background(
                Brush.verticalGradient(
                    listOf(
                        DarkNavyBackground,
                        Color(0xFF07182E),
                        Color(0xFF042B28),
                        DarkNavyBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Prominent Full-Width Alert when Azan / Namaz Alarm is actively ringing
            AnimatedVisibility(visible = isAlarmRinging) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                    border = BorderStroke(1.5.dp, GoldPrimary),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Alarm Active",
                                tint = GoldPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "🕌 AZAN ALARM RINGING",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldAccent
                                )
                                Text(
                                    text = "Time for ${activePrayerName ?: "Prayer"} in Sahiwal",
                                    fontSize = 11.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                        Button(
                            onClick = { PrayerAlarmService.stopAlarm(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("⏹ STOP AZAN", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            // Location & Date Header (Strictly Sahiwal, Pakistan with API Selector)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 14.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = GoldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Sahiwal, Punjab, Pakistan",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "GPS 30.6682° N, 73.1114° E • Asia/Karachi",
                                fontSize = 11.sp,
                                color = GoldAccent,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = schedule.method,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${schedule.hijriDate} • ${schedule.gregorianDate}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = GoldSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onLogout != null) {
                                IconButton(
                                    onClick = { onLogout() },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x22EF4444))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "Sign Out",
                                        tint = Color(0xFFFCA5A5),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            IconButton(
                                onClick = { showUpdateDialog = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2238BDF8))
                                    .testTag("open_update_dialog_header_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = "Check for Updates",
                                    tint = Color(0xFF7DD3FC),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { showQiblaModal = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x2210B981))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Explore,
                                    contentDescription = "Qibla Direction",
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = { loadSahiwalTimings(selectedProvider) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22E5C07B))
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = GoldPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh API",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // API Source Switcher Chips
                    Text(
                        text = "SELECT PRAYER API SOURCE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrayerApiProvider.values().forEach { provider ->
                            val isSelected = selectedProvider == provider
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) Brush.horizontalGradient(
                                            listOf(Color(0xFF0F766E), Color(0xFF047857))
                                        ) else Brush.horizontalGradient(
                                            listOf(Color(0x221E293B), Color(0x33334155))
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) EmeraldAccent else Color(0x3364748B),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (selectedProvider != provider) {
                                            selectedProvider = provider
                                        }
                                    }
                                    .padding(vertical = 8.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = provider.shortName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )
                                    if (isSelected) {
                                        Text(
                                            text = "● Active",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = EmeraldAccent
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Midnight 11:57 PM Edge Case Test Mode Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (countdownState.isSimulatedMidnight) Color(0xFF1E3A8A) else Color(0x18E5C07B)
                            )
                            .border(
                                1.dp,
                                if (countdownState.isSimulatedMidnight) Color(0xFF60A5FA) else Color(0x33E5C07B),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                PrayerCountdownEngine.setSimulatedMidnight(!countdownState.isSimulatedMidnight)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Midnight Test",
                                tint = if (countdownState.isSimulatedMidnight) Color(0xFF93C5FD) else GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (countdownState.isSimulatedMidnight)
                                    "🌙 11:57 PM Midnight ACTIVE (Fajr +1 Day)"
                                else
                                    "🌙 Test 11:57 PM Midnight Rollover Edge Case",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (countdownState.isSimulatedMidnight) Color(0xFFBFDBFE) else GoldAccent
                            )
                        }
                        Text(
                            text = if (countdownState.isSimulatedMidnight) "Reset Device Time" else "Simulate 11:57 PM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick 45-Minute Prayer Grace Period Test Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (countdownState.isSimulatedGracePeriod) Color(0xFF064E3B) else Color(0x18E5C07B)
                            )
                            .border(
                                1.dp,
                                if (countdownState.isSimulatedGracePeriod) Color(0xFF10B981) else Color(0x33E5C07B),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                PrayerCountdownEngine.setSimulatedGracePeriod(!countdownState.isSimulatedGracePeriod)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Grace Period Test",
                                tint = if (countdownState.isSimulatedGracePeriod) Color(0xFF6EE7B7) else GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (countdownState.isSimulatedGracePeriod)
                                    "🕌 45-Min Grace ACTIVE (12:12 PM)"
                                else
                                    "🕌 Test 45-Min Grace Period (12:12 PM)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (countdownState.isSimulatedGracePeriod) Color(0xFFA7F3D0) else GoldAccent
                            )
                        }
                        Text(
                            text = if (countdownState.isSimulatedGracePeriod) "Reset Real Time" else "Simulate 12:12 PM",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Test Alarm Trigger Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x18E5C07B))
                            .border(1.dp, Color(0x33E5C07B), RoundedCornerShape(10.dp))
                            .clickable {
                                PrayerAlarmScheduler.triggerTestAlarmNow(
                                    context,
                                    countdownState.targetPrayerName
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Test Alarm",
                                tint = GoldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Test Background Azan Alarm & Heads-Up",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GoldAccent
                            )
                        }
                        Text(
                            text = "Ring Now",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Islamic Audio & Ringtone Hub Banner Entry Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clickable { onNavigateToSoundHub?.invoke() },
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0F766E), Color(0xFF047857))
                                    )
                                )
                                .border(1.dp, GoldPrimary, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SpatialAudio,
                                contentDescription = "Islamic Audio Hub",
                                tint = GoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Islamic Audio & Ringtone Hub",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0x3310B981))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "20+ Tones",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6EE7B7)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Makkah / Madinah Adhans • Sacred Naats • Zikr Alerts",
                                fontSize = 10.5.sp,
                                color = GoldAccent
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33E5C07B))
                            .border(1.dp, Color(0x66E5C07B), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Manage →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }
                }
            }

            // ==========================================
            // TEST ALERTS & CUSTOM REMINDER HUB
            // ==========================================
            TestNotificationCard(
                modifier = Modifier.padding(bottom = 16.dp),
                onOpenSoundHub = onNavigateToSoundHub
            )

            // 3D Glassmorphic Next Prayer Hero Card
            val isProgress = countdownState.isPrayerInProgress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .then(
                        if (isProgress) {
                            Modifier.border(
                                2.dp,
                                Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF34D399), Color(0xFF059669))),
                                RoundedCornerShape(26.dp)
                            )
                        } else Modifier
                    )
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isProgress) Color(0xFF064E3B)
                                    else if (countdownState.isTomorrow) Color(0xFF1E3A8A)
                                    else Color(0x33E5C07B)
                                )
                                .border(
                                    1.dp,
                                    if (isProgress) Color(0xFF10B981)
                                    else if (countdownState.isTomorrow) Color(0xFF60A5FA)
                                    else GoldPrimary,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isProgress) "JAMAAT IN PROGRESS"
                                else if (countdownState.isTomorrow) "🌅 TOMORROW MORNING"
                                else "🕌 UPCOMING PRAYER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isProgress) Color(0xFF6EE7B7)
                                else if (countdownState.isTomorrow) Color(0xFFBFDBFE)
                                else GoldSecondary,
                                letterSpacing = 1.2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = countdownState.targetPrayerName,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isProgress) Color(0xFF6EE7B7) else Color.White
                        )

                        Text(
                            text = countdownState.targetPrayerArabic,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isProgress) Color(0xFFA7F3D0) else GoldAccent,
                            fontFamily = FontFamily.Serif
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = countdownState.targetPrayerTimeFormatted,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE2E8F0)
                            )
                            if (isProgress) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF065F46))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "• JAMAAT ACTIVE (45m)",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA7F3D0),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (countdownState.isTomorrow) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• Sahiwal Dawn",
                                    fontSize = 12.sp,
                                    color = GoldAccent,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (countdownState.currentDeviceTime.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Current Device Time: ${countdownState.currentDeviceTime}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Digital Countdown Display
                        Box(
                            modifier = Modifier
                                .scale(pulseScale)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isProgress) {
                                        Brush.linearGradient(
                                            listOf(Color(0xFF064E3B), Color(0xFF065F46), Color(0xFF047857))
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(Color(0xFF0F2642), Color(0xFF083D36))
                                        )
                                    }
                                )
                                .border(
                                    1.5.dp,
                                    if (isProgress) {
                                        Brush.linearGradient(listOf(Color(0xFF34D399), Color(0xFF10B981), Color(0xFF6EE7B7)))
                                    } else {
                                        Brush.linearGradient(listOf(GoldPrimary, EmeraldAccent))
                                    },
                                    RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = countdownState.formattedTime,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isProgress) Color(0xFF6EE7B7) else GoldAccent,
                                    letterSpacing = 2.sp
                                )
                                Text(
                                    text = if (isProgress)
                                        "Jamaat / Prayer Window Remaining (45 min)"
                                    else
                                        "Time Remaining Until ${countdownState.targetPrayerName}",
                                    fontSize = 10.sp,
                                    fontWeight = if (isProgress) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isProgress) Color(0xFFA7F3D0) else Color(0xFF94A3B8)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                    // Adhan Alert Audio Preview
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1AE5C07B))
                            .clickable {
                                if (isPlayingAdhanTest) {
                                    AdhanSoundPlayer.stopAdhan()
                                    isPlayingAdhanTest = false
                                } else {
                                    AdhanSoundPlayer.playAdhanAlert(context)
                                    isPlayingAdhanTest = true
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isPlayingAdhanTest) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "Adhan Audio Test",
                            tint = GoldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlayingAdhanTest) "Stop Adhan Audio" else "Test Adhan Alert Sound",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GoldSecondary
                        )
                    }
                }
            }
        }

            // Qibla Compass & Figure-8 Calibration Guide Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { showQiblaModal = true },
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x25E5C07B))
                                .border(1.dp, GoldPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Qibla Direction",
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Qibla Direction (261° WSW)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Live compass dial with Figure-8 calibration guide",
                                fontSize = 11.sp,
                                color = GoldAccent
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x2210B981))
                            .border(1.dp, Color(0x6610B981), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Open",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF6EE7B7)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = Color(0xFF6EE7B7),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            // Next-Gen Feature 1: AUTO-SILENT MODE Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
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
                                    .background(if (autoSilentState.isCurrentlySilent) Color(0x33EF4444) else Color(0x2210B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (autoSilentState.isCurrentlySilent) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                    contentDescription = "Auto Silent",
                                    tint = if (autoSilentState.isCurrentlySilent) Color(0xFFFCA5A5) else EmeraldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Auto-Silent Mode",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (autoSilentState.isCurrentlySilent) "SILENT ACTIVE • 20m Namaz Window" else "Standby (Auto-mutes during Jamaat)",
                                    fontSize = 10.5.sp,
                                    color = if (autoSilentState.isCurrentlySilent) EmeraldAccent else Color(0xFF94A3B8)
                                )
                            }
                        }

                        Switch(
                            checked = autoSilentState.isEnabled,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    val hasDnd = AutoSilentManager.hasNotificationPolicyAccess(context)
                                    if (!hasDnd) {
                                        Toast.makeText(
                                            context,
                                            "Please grant Do Not Disturb permission to allow auto-muting",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        try {
                                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open DND settings: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                        AutoSilentManager.toggleEnabled(false, context)
                                    } else {
                                        AutoSilentManager.toggleEnabled(true, context)
                                    }
                                } else {
                                    AutoSilentManager.toggleEnabled(false, context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = EmeraldAccent,
                                checkedTrackColor = Color(0x5510B981),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0x331E293B)
                            )
                        )
                    }

                    val hasDndPermission = AutoSilentManager.hasNotificationPolicyAccess(context)
                    if (!hasDndPermission) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x22F59E0B))
                                .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(10.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚠️ DND Access Required: Tap here to allow app to mute ringer during Namaz",
                                fontSize = 11.5.sp,
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Keeps phone completely silent for 20 minutes following each prayer time.\n• Automatically detects entry into ${autoSilentState.nearbyMosqueName} (${autoSilentState.distanceToMosqueMeters}m away).",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1),
                        lineHeight = 16.sp
                    )
                }
            }

            // Next-Gen Feature 2: SMART SLEEP WAKE-UP FOR TAHAJJUD Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
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
                                    .background(Color(0x2238BDF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = "Tahajjud",
                                    tint = Color(0xFF7DD3FC),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Smart Tahajjud Alarm",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Wakes 30m before Sahiwal Fajr (${tahajjudState.tahajjudTimeFormatted})",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF7DD3FC)
                                )
                            }
                        }

                        Switch(
                            checked = tahajjudState.isEnabled,
                            onCheckedChange = { TahajjudAlarmManager.toggleAlarm(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GoldPrimary,
                                checkedTrackColor = Color(0x55E5C07B),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0x331E293B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sound: ${tahajjudState.soundTheme}\nOptimal Bedtime: ${tahajjudState.recommendedBedtime}",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 16.sp
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x2238BDF8))
                                .clickable {
                                    if (tahajjudState.isRinging) {
                                        TahajjudAlarmManager.stopGentleNatureSounds()
                                    } else {
                                        TahajjudAlarmManager.playGentleNatureSounds()
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (tahajjudState.isRinging) "Stop Nature Chime" else "Preview Chime",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF7DD3FC)
                            )
                        }
                    }
                }
            }

            // Real-Time Sahiwal Prayer Schedule Card
            Text(
                text = "SAHIWAL DAILY PRAYER SCHEDULE • ${schedule.apiSource.uppercase()}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = GoldSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    prayerItems.forEach { prayer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (prayer.isNext) Color(0x22E5C07B) else Color(0x101E293B))
                                .border(
                                    1.dp,
                                    if (prayer.isNext) GoldPrimary else Color(0x15E5C07B),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (prayer.type) {
                                        PrayerType.FAJR -> "🌅"
                                        PrayerType.SUNRISE -> "☀️"
                                        PrayerType.DHUHR -> "🌞"
                                        PrayerType.ASR -> "🌤️"
                                        PrayerType.MAGHRIB -> "🌇"
                                        PrayerType.ISHA -> "🌙"
                                    },
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = prayer.type.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (prayer.isNext) GoldAccent else Color.White
                                        )
                                        if (prayer.isNext) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(GoldPrimary)
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "NEXT",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1B1302)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = prayer.type.arabicName,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = prayer.timeFormatted,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (prayer.isNext) GoldAccent else Color(0xFFCBD5E1)
                                )

                                if (prayer.type != PrayerType.SUNRISE) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    IconButton(
                                        onClick = {
                                            val idx = prayerItems.indexOf(prayer)
                                            if (idx != -1) {
                                                val updated = prayer.copy(isAlarmEnabled = !prayer.isAlarmEnabled)
                                                prayerItems[idx] = updated
                                                if (updated.isAlarmEnabled) {
                                                    PrayerAlarmScheduler.schedulePrayerAlarm(context, updated)
                                                } else {
                                                    PrayerAlarmScheduler.cancelPrayerAlarm(context, updated)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (prayer.isAlarmEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                            contentDescription = "Toggle Alarm",
                                            tint = if (prayer.isAlarmEnabled) GoldPrimary else Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Additional timings: Imsak and Midnight
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Imsak: ${schedule.imsak}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "Islamic Midnight: ${schedule.midnight}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // App Version & Update Checker Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .testTag("dashboard_version_update_card"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showUpdateDialog = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sahiwal Prayer Times",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Installed: ${AppUpdateManager.currentVersionDisplay}",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x22E5C07B))
                            .border(1.dp, Color(0x44E5C07B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Check Updates",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }
        }

        // Check for Updates Dialog
        if (showUpdateDialog) {
            AppVersionManagementDialog(
                onDismiss = { showUpdateDialog = false }
            )
        }

        // Qibla Compass & Calibration Full Screen Dialog
        if (showQiblaModal) {
            Dialog(
                onDismissRequest = { showQiblaModal = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF020617))
                ) {
                    QiblaScreen(
                        location = LocationRepository.getDefaultLocation(),
                        onBack = { showQiblaModal = false }
                    )
                }
            }
        }
    }
}
