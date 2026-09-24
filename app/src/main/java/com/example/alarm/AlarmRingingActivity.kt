package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald

/**
 * MODULE 3: THE LOCK-SCREEN OVERRIDE ACTIVITY
 * MODULE 4: THE 7-MINUTE EXACT SNOOZE ENGINE (UI & INTEGRATION)
 */
class AlarmRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // MODULE 3: Inject exact window flags to override lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: intent.getStringExtra("prayer_name") ?: "Prayer"
        val prayerArabic = intent.getStringExtra("PRAYER_ARABIC") ?: intent.getStringExtra("prayer_arabic") ?: "الصلاة"

        setContent {
            AlarmRingingContent(
                prayerName = prayerName,
                prayerArabic = prayerArabic,
                onSnoozeClicked = { handleSnooze(prayerName, prayerArabic) },
                onDismissClicked = { handleDismiss() }
            )
        }
    }

    private fun handleSnooze(prayerName: String, prayerArabic: String) {
        // MODULE 4: Snooze Logic
        // 1. Instantly stop the PrayerAlarmService
        PrayerAlarmService.stopAlarm(this)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(PrayerAlarmService.NOTIFICATION_ID)
        nm?.cancel(PrayerAlarmService.NOTIFICATION_ID_PRE5)
        nm?.cancel(PrayerAlarmService.NOTIFICATION_ID_PRE15)

        // 2. Calculate exact timestamp: val snoozeTimeMillis = System.currentTimeMillis() + (7 * 60 * 1000L)
        val snoozeTimeMillis = System.currentTimeMillis() + (7 * 60 * 1000L)

        // 3. Check canScheduleExactAlarms() and call alarmManager.setExactAndAllowWhileIdle
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeIntent = Intent(this, SnoozeReceiver::class.java).apply {
            putExtra("PRAYER_NAME", prayerName)
            putExtra("PRAYER_ARABIC", prayerArabic)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            this,
            SnoozeReceiver.SNOOZE_REQUEST_CODE,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTimeMillis,
                        snoozePendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTimeMillis,
                        snoozePendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTimeMillis,
                    snoozePendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, snoozePendingIntent)
        }

        // 4. Fire Toast
        Toast.makeText(this, "Snoozed for 7 minutes", Toast.LENGTH_SHORT).show()

        // 5. Call finish() to destroy Activity and let user rest
        finish()
    }

    private fun handleDismiss() {
        PrayerAlarmService.stopAlarm(this)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(PrayerAlarmService.NOTIFICATION_ID)
        nm?.cancel(PrayerAlarmService.NOTIFICATION_ID_PRE5)
        nm?.cancel(PrayerAlarmService.NOTIFICATION_ID_PRE15)

        Toast.makeText(this, "Adhan Dismissed", Toast.LENGTH_SHORT).show()
        finish()
    }
}

@Composable
private fun AlarmRingingContent(
    prayerName: String,
    prayerArabic: String,
    onSnoozeClicked: () -> Unit,
    onDismissClicked: () -> Unit
) {
    // Pulsing animation
    val infiniteTransition = rememberInfiniteTransition(label = "AlarmPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlow"
    )

    // MODULE 3: Pearl White & Emerald gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PearlBackground,
                        PureWhite,
                        DeepRoyalEmerald.copy(alpha = 0.18f),
                        DeepRoyalEmerald.copy(alpha = 0.35f)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header tag
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = DeepRoyalEmerald.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HAYYA 'ALA AS-SALAH",
                            color = DeepRoyalEmerald,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Arabic title
                Text(
                    text = prayerArabic,
                    fontSize = 36.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumGold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // MODULE 3: Massive bold prayer name (48.sp)
                Text(
                    text = prayerName.uppercase(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black,
                    color = CharcoalPrimary,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "Prayer Time Has Arrived",
                    fontSize = 15.sp,
                    color = CharcoalPrimary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // Pulsing Center Icon / Jewel
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(220.dp)
                    .scale(pulseScale)
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    PlatinumGold.copy(alpha = pulseGlow * 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Middle ring
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(DeepRoyalEmerald.copy(alpha = 0.15f))
                        .border(2.dp, PlatinumGold, CircleShape)
                )

                // Inner core
                Surface(
                    modifier = Modifier.size(110.dp),
                    shape = CircleShape,
                    color = DeepRoyalEmerald,
                    shadowElevation = 16.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = "Mosque",
                            tint = PureWhite,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            // MODULE 4: Two massive buttons side-by-side: "SNOOZE (7 MINS)" and "DISMISS"
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Snooze Button
                    Button(
                        onClick = onSnoozeClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PlatinumGold,
                            contentColor = PureWhite
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SNOOZE (7 MINS)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Dismiss Button
                    Button(
                        onClick = onDismissClicked,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepRoyalEmerald,
                            contentColor = PureWhite
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DISMISS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
