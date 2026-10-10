package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.TahajjudAlarmManager
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import kotlinx.coroutines.delay

/**
 * Smart Tahajjud Wakeup Screen (Version 1.1)
 * Displays the real-time calculated Tahajjud window (Last Third of the Night),
 * live dynamic countdown to Tahajjud, custom wakeup offset selector,
 * large system alarm toggle, circadian sleep cycle optimizer, and gentle nature sound previews.
 */
@Composable
fun TahajjudWakeupScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state by TahajjudAlarmManager.state.collectAsStateWithLifecycle()

    BackHandler {
        TahajjudAlarmManager.stopGentleNatureSounds()
        onBack()
    }

    var selectedSoundIndex by remember { mutableStateOf(0) }
    val soundThemes = listOf(
        "Dawn Birdsong & Flowing Creek",
        "Soft Zen Singing Bowl",
        "Gentle Rain On Palm Leaves",
        "Serene Bamboo Flute & Wind"
    )

    // Live 1-second countdown ticker
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val remainingMillis = (state.nextAlarmTimestampMillis - currentTimeMillis).coerceAtLeast(0L)
    val remainingHours = (remainingMillis / (1000 * 60 * 60)) % 24
    val remainingMinutes = (remainingMillis / (1000 * 60)) % 60
    val remainingSeconds = (remainingMillis / 1000) % 60

    val countdownText = if (state.isEnabled && remainingMillis > 0) {
        String.format("%02dh %02dm %02ds remaining", remainingHours, remainingMinutes, remainingSeconds)
    } else if (state.isEnabled) {
        "Tahajjud window active now"
    } else {
        "Alarm disabled"
    }

    val presetOffsets = listOf(15, 30, 45, 60, 90)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF07121E),
                        Color(0xFF0C2234),
                        Color(0xFF05111B)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("tahajjud_wakeup_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        TahajjudAlarmManager.stopGentleNatureSounds()
                        onBack()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0x33FFFFFF), CircleShape)
                        .testTag("tahajjud_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Dedicated Tahajjud Wakeup",
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Qiyam al-Layl (قيام الليل) • Last Third of Night",
                        color = PlatinumGold,
                        fontSize = 11.5.sp
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. HERO ALARM STATUS & LARGE TOGGLE CARD
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD0C202F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.45f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Large Toggle Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(if (state.isEnabled) Color(0x3310B981) else Color(0x2264748B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NightsStay,
                                            contentDescription = "Tahajjud",
                                            tint = if (state.isEnabled) PlatinumGold else Color(0xFF94A3B8),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Tahajjud System Alarm",
                                            color = PureWhite,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (state.isEnabled) "Wakes lockscreen with full audio" else "Alarm paused",
                                            color = if (state.isEnabled) RadiantEmerald else Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Switch(
                                    checked = state.isEnabled,
                                    onCheckedChange = {
                                        TahajjudAlarmManager.toggleAlarm(context, it)
                                        val msg = if (it) "Tahajjud alarm activated for ${state.tahajjudTimeFormatted}" else "Tahajjud alarm turned off"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("tahajjud_alarm_toggle"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PureWhite,
                                        checkedTrackColor = RadiantEmerald,
                                        uncheckedThumbColor = Color(0xFF94A3B8),
                                        uncheckedTrackColor = Color(0x44020B11)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Large Time Display
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0x55020B11))
                                    .border(1.dp, if (state.isEnabled) RadiantEmerald.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(18.dp))
                                    .padding(vertical = 18.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = state.tahajjudTimeFormatted,
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (state.isEnabled) PlatinumGold else Color(0xFF64748B),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.HourglassTop,
                                            contentDescription = null,
                                            tint = if (state.isEnabled) RadiantEmerald else Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = countdownText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.isEnabled) RadiantEmerald else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. REAL-TIME CALCULATED TAHAJJUD WINDOW (LAST THIRD OF THE NIGHT)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD0C202F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3338BDF8))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Window",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Calculated Last Third of the Night Window",
                                    color = PureWhite,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Window Begins", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text(
                                        text = state.tahajjudWindowStartFormatted,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "Last 1/3 Night", color = Color(0xFF64748B), fontSize = 9.5.sp)
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Optimal Sunnah", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text(
                                        text = state.tahajjudTimeFormatted,
                                        color = PlatinumGold,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "${state.minutesBeforeFajr}m before Fajr", color = RadiantEmerald, fontSize = 9.5.sp)
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text(text = "Window Ends", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text(
                                        text = state.tahajjudWindowEndFormatted,
                                        color = Color(0xFFF59E0B),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "Fajr Adhan", color = Color(0xFF64748B), fontSize = 9.5.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Mathematically calculated by dividing the interval from Maghrib (${state.maghribTimeFormatted}) to Fajr (${state.fajrTimeFormatted}) into three equal parts. The final third is the pinnacle time for accepted supplications (Mustajab Dua).",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // 3. CUSTOM WAKEUP TIME PICKER
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD0C202F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.35f))
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
                                Text(
                                    text = "Custom Wakeup Timing",
                                    color = PureWhite,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${state.minutesBeforeFajr} min before Fajr",
                                    color = RadiantEmerald,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Preset Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presetOffsets.forEach { offset ->
                                    val isSelected = state.minutesBeforeFajr == offset
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) RadiantEmerald else Color(0x22020B11))
                                            .border(1.dp, if (isSelected) PlatinumGold else Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                                            .clickable { TahajjudAlarmManager.setCustomWakeupOffset(context, offset) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${offset}m",
                                            color = if (isSelected) PureWhite else Color(0xFFCBD5E1),
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Fine Adjustment Slider
                            Text(
                                text = "Adjust offset: ${state.minutesBeforeFajr} minutes before Fajr",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Slider(
                                value = state.minutesBeforeFajr.toFloat(),
                                onValueChange = { TahajjudAlarmManager.setCustomWakeupOffset(context, it.toInt()) },
                                valueRange = 10f..120f,
                                steps = 21,
                                colors = SliderDefaults.colors(
                                    thumbColor = PlatinumGold,
                                    activeTrackColor = RadiantEmerald,
                                    inactiveTrackColor = Color(0x4464748B)
                                )
                            )
                        }
                    }
                }

                // 4. CIRCADIAN RHYTHM & SLEEP CYCLE OPTIMIZER
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD0C202F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x2238BDF8))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = "Sleep Cycle",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Circadian Rhythm & Sleep Optimizer",
                                    color = PureWhite,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Recommended Bedtime", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text(text = state.recommendedBedtime, color = Color(0xFF38BDF8), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Optimal Sleep Stage", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    Text(text = "Light Sleep Phase", color = Color(0xFF34D399), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Timing your wake-up to coincide with the end of a 90-minute sleep cycle eliminates sleep inertia, allowing you to stand in Qiyam with complete mental alertness.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // 5. GENTLE AUDIO THEME & CHIME PREVIEW
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD0C202F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.3f))
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
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Chime",
                                        tint = PlatinumGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Gentle Nature Sound Theme",
                                        color = PureWhite,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (state.isRinging) Color(0xFFEF4444) else RadiantEmerald,
                                    modifier = Modifier.clickable {
                                        if (state.isRinging) {
                                            TahajjudAlarmManager.stopGentleNatureSounds()
                                        } else {
                                            TahajjudAlarmManager.playGentleNatureSounds()
                                            Toast.makeText(context, "Playing serene nature chime preview...", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (state.isRinging) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Stop",
                                            tint = PureWhite,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (state.isRinging) "Stop Audio" else "Preview Chime",
                                            color = PureWhite,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            soundThemes.forEachIndexed { index, theme ->
                                val isSelected = selectedSoundIndex == index
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0x3310B981) else Color(0x22020B11))
                                        .border(
                                            1.dp,
                                            if (isSelected) RadiantEmerald else Color.Transparent,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { selectedSoundIndex = index }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = theme,
                                        color = if (isSelected) PureWhite else Color(0xFF94A3B8),
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = RadiantEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 6. VIRTUES & AUTHENTIC PROPHETIC DUAS
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD0C202F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Virtue of Qiyam al-Layl",
                                color = PlatinumGold,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "وَمِنَ ٱلَّيْلِ فَتَهَجَّدْ بِهِۦ نَافِلَةً لَّكَ عَسَىٰٓ أَن يَبْعَثَكَ رَبُّكَ مَقَامًا مَّحْمُودًا",
                                color = PureWhite,
                                fontSize = 17.sp,
                                fontFamily = FontFamily.Serif,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"And from [part of] the night, pray with it as additional [worship] for you; it is expected that your Lord will resurrect you to a praised station.\" [Surah Al-Isra 17:79]",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Sahih al-Bukhari: \"Our Lord descends each night to the lowest heaven during the last third of the night, asking: Who is calling upon Me that I may answer him?\"",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xDD0C202F)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Prophetic Supplication When Waking for Tahajjud",
                                color = PlatinumGold,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "اللَّهُمَّ لَكَ الحَمْدُ أَنْتَ نُورُ السَّمَاوَاتِ وَالأَرْضِ وَمَنْ فِيهِنَّ، وَلَكَ الحَمْدُ أَنْتَ قَيِّمُ السَّمَاوَاتِ وَالأَرْضِ وَمَنْ فِيهِنَّ",
                                color = PureWhite,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Serif,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"O Allah, to You belongs all praise; You are the Light of the heavens and the earth and all that is within them. To You belongs all praise; You are the Sustainer of the heavens and the earth and all that is within them.\" [Sahih al-Bukhari 1120]",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
