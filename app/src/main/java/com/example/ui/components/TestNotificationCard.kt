package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.alarm.PrayerAlarmService
import com.example.alarm.PrayerMessageStyle
import com.example.alarm.PrayerNotificationPreferences
import com.example.alarm.PreReminderMessageStyle
import com.example.audio.PrayerSoundPreferences
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary

@Composable
fun TestNotificationCard(
    modifier: Modifier = Modifier,
    onOpenSoundHub: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isAlarmRinging by PrayerAlarmService.isAlarmRinging.collectAsState()
    val activePrayerName by PrayerAlarmService.activePrayerName.collectAsState()

    var showMessageCustomizer by remember { mutableStateOf(false) }

    // Pulsing animation for active alarm banner
    val infiniteTransition = rememberInfiniteTransition(label = "AlarmPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("test_notification_panel_card"),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
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
                            .background(Color(0x22E5C07B))
                            .border(1.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Test Notification Hub",
                            tint = GoldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "TEST ALERTS & REMINDERS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Verify sound, vibration & heads-up display",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = { showMessageCustomizer = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x2210B981))
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Customize Reminder Messages",
                        tint = EmeraldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Active Alarm Banner
            AnimatedVisibility(visible = isAlarmRinging) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFDC2626).copy(alpha = pulseAlpha * 0.8f),
                                        Color(0xFF991B1B).copy(alpha = pulseAlpha * 0.9f)
                                    )
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Alarm Active",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ALARM RINGING: ${activePrayerName ?: "Test"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "High-priority wake-lock active",
                                        fontSize = 10.sp,
                                        color = Color(0xFFFFCDD2)
                                    )
                                }
                            }

                            Button(
                                onClick = { PrayerAlarmService.stopAlarm(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF991B1B)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("stop_alarm_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Alarm",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "STOP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Test Buttons Grid
            Text(
                text = "INSTANT TEST TRIGGERS (5 PRAYERS & 15-MIN WARNING):",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Fajr & Dhuhr
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TestTriggerButton(
                        modifier = Modifier.weight(1f),
                        title = "Fajr Alert",
                        subtitle = PrayerSoundPreferences.getAssignedSoundTitleForPrayer(context, "Fajr"),
                        icon = "🌅",
                        testTag = "test_fajr_button"
                    ) {
                        PrayerAlarmService.triggerTestAlarm(
                            context = context,
                            prayerName = "Fajr",
                            prayerArabic = "الفجر",
                            isPreReminder = false
                        )
                    }

                    TestTriggerButton(
                        modifier = Modifier.weight(1f),
                        title = "Dhuhr Alert",
                        subtitle = PrayerSoundPreferences.getAssignedSoundTitleForPrayer(context, "Dhuhr"),
                        icon = "🌞",
                        testTag = "test_dhuhr_button"
                    ) {
                        PrayerAlarmService.triggerTestAlarm(
                            context = context,
                            prayerName = "Dhuhr",
                            prayerArabic = "الظهر",
                            isPreReminder = false
                        )
                    }
                }

                // Row 2: Asr & Maghrib
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TestTriggerButton(
                        modifier = Modifier.weight(1f),
                        title = "Asr Alert",
                        subtitle = PrayerSoundPreferences.getAssignedSoundTitleForPrayer(context, "Asr"),
                        icon = "🌤️",
                        testTag = "test_asr_button"
                    ) {
                        PrayerAlarmService.triggerTestAlarm(
                            context = context,
                            prayerName = "Asr",
                            prayerArabic = "العصر",
                            isPreReminder = false
                        )
                    }

                    TestTriggerButton(
                        modifier = Modifier.weight(1f),
                        title = "Maghrib Alert",
                        subtitle = PrayerSoundPreferences.getAssignedSoundTitleForPrayer(context, "Maghrib"),
                        icon = "🌇",
                        testTag = "test_maghrib_button"
                    ) {
                        PrayerAlarmService.triggerTestAlarm(
                            context = context,
                            prayerName = "Maghrib",
                            prayerArabic = "المغرب",
                            isPreReminder = false
                        )
                    }
                }

                // Row 3: Isha & 15-Min Warning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TestTriggerButton(
                        modifier = Modifier.weight(1f),
                        title = "Isha Alert",
                        subtitle = PrayerSoundPreferences.getAssignedSoundTitleForPrayer(context, "Isha"),
                        icon = "🌙",
                        testTag = "test_isha_button"
                    ) {
                        PrayerAlarmService.triggerTestAlarm(
                            context = context,
                            prayerName = "Isha",
                            prayerArabic = "العشاء",
                            isPreReminder = false
                        )
                    }

                    TestTriggerButton(
                        modifier = Modifier.weight(1f),
                        title = "15-Min Warning",
                        subtitle = PrayerSoundPreferences.getAssignedSoundTitleForPrayer(context, "PreReminder", isPreReminder = true),
                        icon = "⏳",
                        highlight = true,
                        testTag = "test_pre_reminder_button"
                    ) {
                        PrayerAlarmService.triggerTestAlarm(
                            context = context,
                            prayerName = "Next Prayer",
                            prayerArabic = "الصلاة القادمة",
                            isPreReminder = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer row: Customize text & Sound Hub link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showMessageCustomizer = true },
                    modifier = Modifier.testTag("open_customizer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = EmeraldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Customize Reminder Phrasing",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldAccent
                    )
                }

                if (onOpenSoundHub != null) {
                    TextButton(onClick = onOpenSoundHub) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sound Hub →",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldPrimary
                        )
                    }
                }
            }
        }
    }

    if (showMessageCustomizer) {
        NotificationMessageCustomizerDialog(
            context = context,
            onDismiss = { showMessageCustomizer = false }
        )
    }
}

@Composable
private fun TestTriggerButton(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: String,
    highlight: Boolean = false,
    testTag: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (highlight) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(Color(0x2210B981), Color(0x33059669))
                        )
                    )
                } else {
                    Modifier.background(Color(0x14E5C07B))
                }
            )
            .border(
                width = 1.dp,
                color = if (highlight) EmeraldAccent.copy(alpha = 0.5f) else Color(0x22E5C07B),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (highlight) EmeraldAccent else GoldAccent
                )
                Text(
                    text = subtitle,
                    fontSize = 9.5.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Trigger",
                tint = if (highlight) EmeraldAccent else GoldPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun NotificationMessageCustomizerDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    var selectedPrayerStyle by remember {
        mutableStateOf(PrayerNotificationPreferences.getPrayerMessageStyle(context))
    }
    var customPrayerText by remember {
        mutableStateOf(PrayerNotificationPreferences.getCustomPrayerText(context))
    }
    var selectedPreReminderStyle by remember {
        mutableStateOf(PrayerNotificationPreferences.getPreReminderMessageStyle(context))
    }
    var customPreReminderText by remember {
        mutableStateOf(PrayerNotificationPreferences.getCustomPreReminderText(context))
    }
    var vibrationEnabled by remember {
        mutableStateOf(PrayerNotificationPreferences.isVibrationEnabled(context))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x33E5C07B), RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0x2210B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = null,
                            tint = EmeraldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Customize Reminders",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Notification body phrasing & spiritual texts",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Prayer Message Style Selection
                Text(
                    text = "PRAYER ALARM MESSAGE PHRASING:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldSecondary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                PrayerMessageStyle.values().forEach { style ->
                    val isSelected = selectedPrayerStyle == style
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedPrayerStyle = style }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedPrayerStyle = style },
                            colors = RadioButtonDefaults.colors(selectedColor = GoldPrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = style.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) GoldAccent else Color.White
                            )
                            if (style != PrayerMessageStyle.CUSTOM) {
                                Text(
                                    text = style.previewText,
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (selectedPrayerStyle == PrayerMessageStyle.CUSTOM) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customPrayerText,
                        onValueChange = { customPrayerText = it },
                        label = { Text("Personalized Prayer Reminder Text", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_prayer_text_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 15-Minute Pre-Prayer Phrasing
                Text(
                    text = "15-MINUTE PRE-PRAYER PHRASING:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldSecondary,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                PreReminderMessageStyle.values().forEach { style ->
                    val isSelected = selectedPreReminderStyle == style
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedPreReminderStyle = style }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedPreReminderStyle = style },
                            colors = RadioButtonDefaults.colors(selectedColor = EmeraldAccent)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = style.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) EmeraldAccent else Color.White
                            )
                            if (style != PreReminderMessageStyle.CUSTOM) {
                                Text(
                                    text = style.previewText,
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (selectedPreReminderStyle == PreReminderMessageStyle.CUSTOM) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customPreReminderText,
                        onValueChange = { customPreReminderText = it },
                        label = { Text("Personalized Pre-Prayer Reminder Text", fontSize = 11.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_pre_reminder_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vibration Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Haptic Vibration Pattern",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "Pulsed vibration during notification alert",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldPrimary,
                            checkedTrackColor = Color(0x44E5C07B)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            PrayerNotificationPreferences.setPrayerMessageStyle(context, selectedPrayerStyle)
                            if (selectedPrayerStyle == PrayerMessageStyle.CUSTOM) {
                                PrayerNotificationPreferences.setCustomPrayerText(context, customPrayerText)
                            }
                            PrayerNotificationPreferences.setPreReminderMessageStyle(context, selectedPreReminderStyle)
                            if (selectedPreReminderStyle == PreReminderMessageStyle.CUSTOM) {
                                PrayerNotificationPreferences.setCustomPreReminderText(context, customPreReminderText)
                            }
                            PrayerNotificationPreferences.setVibrationEnabled(context, vibrationEnabled)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_reminder_preferences_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF1E1402),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save Preferences",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E1402)
                        )
                    }
                }
            }
        }
    }
}
