package com.example.ui.screens

import android.content.Context
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerAlarmService
import com.example.audio.AudioCacheManager
import com.example.audio.IslamicSoundCatalog
import com.example.audio.IslamicSoundItem
import com.example.audio.PrayerSoundPreferences
import com.example.audio.SoundCategory
import com.example.audio.SoundEvent
import com.example.audio.SoundPreviewPlayer
import com.example.ui.components.GlassCard
import com.example.ui.components.NotificationMessageCustomizerDialog
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary

enum class SoundFilterTab(val label: String) {
    ALL("All Tones (20+)"),
    BUNDLED_RAW("Bundled Local (5)"),
    CACHED_CUSTOM("Cached Ringtones"),
    GLOBAL_ADHANS("Global Adhans"),
    SHORT_REMINDERS("Short Reminders"),
    PREMIUM_NAATS("Sacred Naats"),
    TAHAJJUD_ZEN("Tahajjud & Zen")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundHubScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Initialize audio cache
    LaunchedEffect(Unit) {
        AudioCacheManager.initialize(context)
    }

    // Stop preview on screen exit
    DisposableEffect(Unit) {
        onDispose {
            SoundPreviewPlayer.stop()
        }
    }

    var selectedFilterTab by remember { mutableStateOf(SoundFilterTab.ALL) }
    var soundToAssign by remember { mutableStateOf<IslamicSoundItem?>(null) }
    var showAssignSheet by remember { mutableStateOf(false) }
    var showMessageCustomizer by remember { mutableStateOf(false) }

    // Alarm active states
    val isAlarmRinging by PrayerAlarmService.isAlarmRinging.collectAsState()
    val activePrayerName by PrayerAlarmService.activePrayerName.collectAsState()

    // Sound assignments state
    var fajrSound by remember { mutableStateOf(PrayerSoundPreferences.getSoundItemForEvent(context, SoundEvent.FAJR)) }
    var regularSound by remember { mutableStateOf(PrayerSoundPreferences.getSoundItemForEvent(context, SoundEvent.REGULAR)) }
    var prePrayerSound by remember { mutableStateOf(PrayerSoundPreferences.getSoundItemForEvent(context, SoundEvent.PRE_PRAYER)) }
    var tahajjudSound by remember { mutableStateOf(PrayerSoundPreferences.getSoundItemForEvent(context, SoundEvent.TAHAJJUD)) }
    var preReminderEnabled by remember { mutableStateOf(PrayerSoundPreferences.isPrePrayerReminderEnabled(context)) }

    // Audio preview state
    val playingSoundId by SoundPreviewPlayer.playingSoundId.collectAsState()
    val isPreviewPlaying by SoundPreviewPlayer.isPlaying.collectAsState()
    val isPreviewLoading by SoundPreviewPlayer.isLoading.collectAsState()

    // Download state
    val downloadingSoundId by AudioCacheManager.downloadingSoundId.collectAsState()
    val downloadProgress by AudioCacheManager.downloadProgress.collectAsState()
    val cachedSoundIds by AudioCacheManager.cachedSounds.collectAsState()

    val displayedSounds = remember(selectedFilterTab, cachedSoundIds) {
        when (selectedFilterTab) {
            SoundFilterTab.ALL -> IslamicSoundCatalog.allSounds
            SoundFilterTab.BUNDLED_RAW -> IslamicSoundCatalog.allSounds.filter { it.isBundledRaw }
            SoundFilterTab.CACHED_CUSTOM -> IslamicSoundCatalog.allSounds.filter { cachedSoundIds.contains(it.id) || it.isBundledRaw }
            SoundFilterTab.GLOBAL_ADHANS -> IslamicSoundCatalog.getSoundsByCategory(SoundCategory.GLOBAL_ADHANS)
            SoundFilterTab.SHORT_REMINDERS -> IslamicSoundCatalog.getSoundsByCategory(SoundCategory.SHORT_REMINDERS)
            SoundFilterTab.PREMIUM_NAATS -> IslamicSoundCatalog.getSoundsByCategory(SoundCategory.PREMIUM_NAATS)
            SoundFilterTab.TAHAJJUD_ZEN -> IslamicSoundCatalog.getSoundsByCategory(SoundCategory.TAHAJJUD_ZEN)
        }
    }

    fun refreshAssignments() {
        fajrSound = PrayerSoundPreferences.getSoundItemForEvent(context, SoundEvent.FAJR)
        regularSound = PrayerSoundPreferences.getSoundItemForEvent(context, SoundEvent.REGULAR)
        prePrayerSound = PrayerSoundPreferences.getSoundItemForEvent(context, SoundEvent.PRE_PRAYER)
        tahajjudSound = PrayerSoundPreferences.getSoundItemForEvent(context, SoundEvent.TAHAJJUD)
        preReminderEnabled = PrayerSoundPreferences.isPrePrayerReminderEnabled(context)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "SoundWavePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sound_hub_screen")
            .background(
                Brush.verticalGradient(
                    listOf(
                        DarkNavyBackground,
                        Color(0xFF0B192C),
                        Color(0xFF042F2E),
                        DarkNavyBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // TOP HEADER BAR
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x22FFFFFF))
                            .border(1.dp, Color(0x33E5C07B), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Islamic Audio Hub",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x3310B981))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "20+ Tones",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }
                        Text(
                            text = "Adhans, Sacred Naats & Spiritual Alarms",
                            fontSize = 11.5.sp,
                            color = GoldAccent
                        )
                    }
                }

                if (isPreviewPlaying) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFDC2626))
                            .clickable { SoundPreviewPlayer.stop() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Preview",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Stop",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // MAIN SCROLLABLE BODY
            // ==========================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ACTIVE ALARM RINGING BANNER (WITH STOP BUTTON)
                if (isAlarmRinging) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B))))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "ALARM RINGING ($activePrayerName)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "High-priority STREAM_ALARM channel",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFFFCA5A5)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White)
                                        .clickable {
                                            PrayerAlarmService.stopAlarm(context)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "STOP",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }

                // SECTION 1: EVENT ASSIGNMENTS SUMMARY CARD
                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Event Assignments",
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "CUSTOM EVENT ASSIGNMENTS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldAccent,
                                        letterSpacing = 0.8.sp
                                    )
                                }

                                Text(
                                    text = "Tap any to customize",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 1. FAJR ASSIGNMENT
                            EventAssignmentRow(
                                icon = Icons.Default.Mosque,
                                iconColor = Color(0xFF38BDF8),
                                title = "Fajr Prayer Alarm",
                                subtitle = "Mandatory: 'As-Salatu Khairum Minan Naum'",
                                assignedSoundTitle = fajrSound.title,
                                isSpecialFajr = true,
                                onClick = {
                                    selectedFilterTab = SoundFilterTab.GLOBAL_ADHANS
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. REGULAR PRAYERS ASSIGNMENT
                            EventAssignmentRow(
                                icon = Icons.Default.Alarm,
                                iconColor = Color(0xFFF59E0B),
                                title = "Regular Prayers (Dhuhr, Asr, Maghrib, Isha)",
                                subtitle = "Plays at exact local adhan time",
                                assignedSoundTitle = regularSound.title,
                                isSpecialFajr = false,
                                onClick = {
                                    selectedFilterTab = SoundFilterTab.GLOBAL_ADHANS
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. 15-MINUTE PRE-PRAYER REMINDER
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x1810B981))
                                    .border(1.dp, Color(0x3310B981), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Pre-Prayer",
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "15-Min Pre-Prayer Alert",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Current Tone: ${prePrayerSound.title}",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }

                                Switch(
                                    checked = preReminderEnabled,
                                    onCheckedChange = { isChecked ->
                                        preReminderEnabled = isChecked
                                        PrayerSoundPreferences.setPrePrayerReminderEnabled(context, isChecked)
                                        Toast.makeText(
                                            context,
                                            if (isChecked) "15-min alerts enabled" else "15-min alerts disabled",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF10B981),
                                        checkedTrackColor = Color(0x6610B981),
                                        uncheckedThumbColor = Color(0xFF64748B),
                                        uncheckedTrackColor = Color(0x3364748B)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 4. TAHAJJUD WAKE-UP
                            EventAssignmentRow(
                                icon = Icons.Default.NightsStay,
                                iconColor = Color(0xFFA78BFA),
                                title = "Tahajjud Wake-Up Alarm",
                                subtitle = "Gentle binaural dawn waves & nature",
                                assignedSoundTitle = tahajjudSound.title,
                                isSpecialFajr = false,
                                onClick = {
                                    selectedFilterTab = SoundFilterTab.TAHAJJUD_ZEN
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // ==========================================
                            // INSTANT TEST ALERTS & NOTIFICATIONS
                            // ==========================================
                            Text(
                                text = "INSTANT TEST HEADS-UP NOTIFICATIONS:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Audition assigned adhans, vibration & custom alert text on STREAM_ALARM instantly",
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Grid of test prayer alert buttons
                            val testPrayers = listOf(
                                Triple("Fajr", "الفجر", false),
                                Triple("Dhuhr", "الظهر", false),
                                Triple("Asr", "العصر", false),
                                Triple("Maghrib", "المغرب", false),
                                Triple("Isha", "العشاء", false),
                                Triple("Pre-Alert", "تنبيه الصلاة", true)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                testPrayers.forEach { (pName, pArabic, isPre) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isPre) Color(0x3310B981)
                                                else Color(0x28E5C07B)
                                            )
                                            .border(
                                                1.dp,
                                                if (isPre) Color(0x6610B981)
                                                else Color(0x55E5C07B),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                PrayerAlarmService.triggerTestAlarm(
                                                    context = context,
                                                    prayerName = pName,
                                                    prayerArabic = pArabic,
                                                    isPreReminder = isPre
                                                )
                                                Toast.makeText(context, "Testing $pName notification alert with assigned sound...", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 11.dp, vertical = 7.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = when (pName) {
                                                    "Fajr" -> "🌅"
                                                    "Dhuhr" -> "🌞"
                                                    "Asr" -> "🌤️"
                                                    "Maghrib" -> "🌇"
                                                    "Isha" -> "🌙"
                                                    else -> "⏳"
                                                },
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "Test $pName",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isPre) Color(0xFF6EE7B7) else Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // CUSTOMIZE REMINDER TEXT BUTTON
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x2238BDF8))
                                    .border(1.dp, Color(0x4438BDF8), RoundedCornerShape(12.dp))
                                    .clickable { showMessageCustomizer = true }
                                    .padding(vertical = 9.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Customize Reminder Texts",
                                    tint = Color(0xFF7DD3FC),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Customize Reminder Text & Body Style",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBAE6FD)
                                )
                            }
                        }
                    }
                }

                // SECTION 2: CATEGORY TABS
                item {
                    Text(
                        text = "EXPLORE SOUND LIBRARY (${displayedSounds.size} / ${IslamicSoundCatalog.allSounds.size} AVAILABLE):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SoundFilterTab.values().forEach { tab ->
                            val isSelected = selectedFilterTab == tab
                            val count = when (tab) {
                                SoundFilterTab.ALL -> IslamicSoundCatalog.allSounds.size
                                SoundFilterTab.BUNDLED_RAW -> IslamicSoundCatalog.allSounds.count { it.isBundledRaw }
                                SoundFilterTab.CACHED_CUSTOM -> IslamicSoundCatalog.allSounds.count { cachedSoundIds.contains(it.id) || it.isBundledRaw }
                                SoundFilterTab.GLOBAL_ADHANS -> IslamicSoundCatalog.getSoundsByCategory(SoundCategory.GLOBAL_ADHANS).size
                                SoundFilterTab.SHORT_REMINDERS -> IslamicSoundCatalog.getSoundsByCategory(SoundCategory.SHORT_REMINDERS).size
                                SoundFilterTab.PREMIUM_NAATS -> IslamicSoundCatalog.getSoundsByCategory(SoundCategory.PREMIUM_NAATS).size
                                SoundFilterTab.TAHAJJUD_ZEN -> IslamicSoundCatalog.getSoundsByCategory(SoundCategory.TAHAJJUD_ZEN).size
                            }
                            val (tabIcon, tabColor) = when (tab) {
                                SoundFilterTab.ALL -> Icons.Default.Tune to Color(0xFFE5C07B)
                                SoundFilterTab.BUNDLED_RAW -> Icons.Default.Check to Color(0xFF10B981)
                                SoundFilterTab.CACHED_CUSTOM -> Icons.Default.DownloadDone to Color(0xFF38BDF8)
                                SoundFilterTab.GLOBAL_ADHANS -> Icons.Default.Mosque to Color(0xFF38BDF8)
                                SoundFilterTab.SHORT_REMINDERS -> Icons.Default.Star to Color(0xFF10B981)
                                SoundFilterTab.PREMIUM_NAATS -> Icons.Default.MusicNote to Color(0xFFF472B6)
                                SoundFilterTab.TAHAJJUD_ZEN -> Icons.Default.SelfImprovement to Color(0xFFA78BFA)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) Brush.horizontalGradient(
                                            listOf(Color(0xFF0F766E), Color(0xFF047857))
                                        ) else Brush.horizontalGradient(
                                            listOf(Color(0x331E293B), Color(0x44334155))
                                        )
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSelected) GoldPrimary else Color(0x22E5C07B)
                                        ),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedFilterTab = tab }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = tabIcon,
                                        contentDescription = tab.label,
                                        tint = if (isSelected) Color.White else tabColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tab.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFFE2E8F0)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0x44FFFFFF) else Color(0x33000000))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "$count",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else GoldAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 3: SOUND ITEMS IN SELECTED FILTER
                items(displayedSounds, key = { it.id }) { sound ->
                    val isPlayingThis = (playingSoundId == sound.id) && isPreviewPlaying
                    val isBufferingThis = (playingSoundId == sound.id) && isPreviewLoading
                    val isDownloadingThis = (downloadingSoundId == sound.id)
                    val isCached = sound.isBundledRaw || cachedSoundIds.contains(sound.id)

                    // Check which events this sound is assigned to
                    val isAssignedFajr = fajrSound.id == sound.id
                    val isAssignedRegular = regularSound.id == sound.id
                    val isAssignedPrePrayer = prePrayerSound.id == sound.id
                    val isAssignedTahajjud = tahajjudSound.id == sound.id

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isPlayingThis) {
                                    Modifier.border(
                                        1.5.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                GoldPrimary.copy(alpha = pulseAlpha),
                                                EmeraldAccent.copy(alpha = pulseAlpha)
                                            )
                                        ),
                                        RoundedCornerShape(20.dp)
                                    )
                                } else Modifier
                            ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Sound Header: Title, Arabic, and Duration
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sound.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sound.arabicTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = GoldAccent
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = sound.artistOrOrigin,
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                // Badges
                                Column(horizontalAlignment = Alignment.End) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (sound.isBundledRaw) Color(0x3310B981)
                                                else if (isCached) Color(0x3338BDF8)
                                                else Color(0x2294A3B8)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (sound.isBundledRaw) "Bundled Offline"
                                                   else if (isCached) "Cached"
                                                   else "Cloud Audio",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (sound.isBundledRaw) Color(0xFF6EE7B7)
                                                   else if (isCached) Color(0xFF7DD3FC)
                                                   else Color(0xFFCBD5E1)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = sound.durationFormatted,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GoldSecondary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = sound.description,
                                fontSize = 11.5.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 16.sp
                            )

                            // Assigned tags row if active
                            if (isAssignedFajr || isAssignedRegular || isAssignedPrePrayer || isAssignedTahajjud) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isAssignedFajr) {
                                        AssignmentBadge("Active on Fajr", Color(0xFF38BDF8))
                                    }
                                    if (isAssignedRegular) {
                                        AssignmentBadge("Active on Regular Prayers", Color(0xFFF59E0B))
                                    }
                                    if (isAssignedPrePrayer) {
                                        AssignmentBadge("Active on 15-Min Alert", Color(0xFF10B981))
                                    }
                                    if (isAssignedTahajjud) {
                                        AssignmentBadge("Active on Tahajjud", Color(0xFFA78BFA))
                                    }
                                }
                            }

                            // Download Progress Bar if downloading
                            if (isDownloadingThis) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Downloading & Caching...",
                                            fontSize = 11.sp,
                                            color = GoldAccent
                                        )
                                        Text(
                                            text = "${(downloadProgress * 100).toInt()}%",
                                            fontSize = 11.sp,
                                            color = GoldAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = GoldPrimary,
                                        trackColor = Color(0x33E5C07B)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Buttons Row: Preview Play/Stop, Download, Assign
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // PLAY / STOP PREVIEW BUTTON
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isPlayingThis) Color(0xFFDC2626)
                                            else Color(0x33E5C07B)
                                        )
                                        .border(
                                            1.dp,
                                            if (isPlayingThis) Color(0xFFEF4444) else Color(0x66E5C07B),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            SoundPreviewPlayer.playOrPause(context, sound)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isBufferingThis) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlayingThis) "Pause" else "Play Preview",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPlayingThis) "Playing Preview" else "Preview Tone",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // DOWNLOAD / CACHE BUTTON
                                    if (!sound.isBundledRaw) {
                                        IconButton(
                                            onClick = {
                                                if (!isCached && !isDownloadingThis) {
                                                    AudioCacheManager.downloadAndCacheSound(
                                                        context,
                                                        sound,
                                                        onSuccess = {
                                                            Toast.makeText(context, "${sound.title} cached offline!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        onError = { err ->
                                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                } else if (isCached) {
                                                    Toast.makeText(context, "Already saved locally on device", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(if (isCached) Color(0x2210B981) else Color(0x2238BDF8))
                                        ) {
                                            Icon(
                                                imageVector = if (isCached) Icons.Default.DownloadDone else Icons.Default.CloudDownload,
                                                contentDescription = "Download",
                                                tint = if (isCached) Color(0xFF34D399) else Color(0xFF38BDF8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }

                                    // ASSIGN BUTTON
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF0F766E), Color(0xFF047857))
                                                )
                                            )
                                            .border(1.dp, Color(0x4434D399), RoundedCornerShape(12.dp))
                                            .clickable {
                                                soundToAssign = sound
                                                showAssignSheet = true
                                            }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Alarm,
                                                contentDescription = "Assign",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Set As...",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // MODAL BOTTOM SHEET: ASSIGN TO EVENT
    // ==========================================
    if (showAssignSheet && soundToAssign != null) {
        val sound = soundToAssign!!
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { showAssignSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF0B1B30),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x66E5C07B))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Assign Tone to Prayer Event",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${sound.title} (${sound.arabicTitle})",
                    fontSize = 13.sp,
                    color = GoldAccent
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SELECT EVENT TARGET:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // OPTION 1: FAJR PRAYER
                val isFajrTone = sound.id == "fajr_special" || sound.description.contains("As-Salatu Khairum Minan Naum", ignoreCase = true)
                AssignOptionCard(
                    title = "Fajr Prayer Alarm",
                    subtitle = if (isFajrTone) "Includes 'As-Salatu Khairum Minan Naum' (Recommended)"
                               else "Custom Fajr call without special phrase",
                    badgeText = if (isFajrTone) "AUTHENTIC FAJR" else "OPTIONAL",
                    badgeColor = if (isFajrTone) Color(0xFF10B981) else Color(0xFFF59E0B),
                    isSelected = fajrSound.id == sound.id,
                    onClick = {
                        PrayerSoundPreferences.setSoundForEvent(context, SoundEvent.FAJR, sound)
                        refreshAssignments()
                        showAssignSheet = false
                        Toast.makeText(context, "Assigned to Fajr Prayer Alarm!", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // OPTION 2: REGULAR PRAYERS (Dhuhr, Asr, Maghrib, Isha)
                AssignOptionCard(
                    title = "Regular Prayers (Dhuhr, Asr, Maghrib, Isha)",
                    subtitle = "Applies to all 4 daytime and evening prayers",
                    badgeText = "ALL 4 PRAYERS",
                    badgeColor = Color(0xFF38BDF8),
                    isSelected = regularSound.id == sound.id,
                    onClick = {
                        PrayerSoundPreferences.setSoundForEvent(context, SoundEvent.REGULAR, sound)
                        refreshAssignments()
                        showAssignSheet = false
                        Toast.makeText(context, "Assigned to Regular Prayers (Dhuhr, Asr, Maghrib, Isha)!", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // OPTION 3: 15-MINUTE PRE-PRAYER REMINDER
                AssignOptionCard(
                    title = "15-Minute Pre-Prayer Reminder",
                    subtitle = "Plays gentle alert 15 minutes before Adhan for Wudu",
                    badgeText = "PRE-ADHAN",
                    badgeColor = Color(0xFFFBBF24),
                    isSelected = prePrayerSound.id == sound.id,
                    onClick = {
                        PrayerSoundPreferences.setSoundForEvent(context, SoundEvent.PRE_PRAYER, sound)
                        refreshAssignments()
                        showAssignSheet = false
                        Toast.makeText(context, "Assigned to 15-Minute Pre-Prayer Reminder!", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // OPTION 4: TAHAJJUD WAKE-UP
                AssignOptionCard(
                    title = "Tahajjud Wake-Up Alarm",
                    subtitle = "Optimal awakening for late-night prayer vigil",
                    badgeText = "NIGHT VIGIL",
                    badgeColor = Color(0xFFA78BFA),
                    isSelected = tahajjudSound.id == sound.id,
                    onClick = {
                        PrayerSoundPreferences.setSoundForEvent(context, SoundEvent.TAHAJJUD, sound)
                        refreshAssignments()
                        showAssignSheet = false
                        Toast.makeText(context, "Assigned to Tahajjud Wake-Up Alarm!", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Notification Message Phrasing & Style Customizer Dialog
        if (showMessageCustomizer) {
            NotificationMessageCustomizerDialog(
                context = context,
                onDismiss = { showMessageCustomizer = false }
            )
        }
    }
}

@Composable
private fun EventAssignmentRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    assignedSoundTitle: String,
    isSpecialFajr: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x18FFFFFF))
            .border(1.dp, Color(0x22E5C07B), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isSpecialFajr) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x3338BDF8))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Fajr Special",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7DD3FC)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = assignedSoundTitle,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x22E5C07B))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Change",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )
        }
    }
}

@Composable
private fun AssignOptionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) Color(0x3310B981) else Color(0x18FFFFFF))
            .border(
                1.dp,
                if (isSelected) Color(0xFF10B981) else Color(0x22FFFFFF),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColor.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = Color(0xFFCBD5E1)
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color(0xFF34D399),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AssignmentBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.8.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
