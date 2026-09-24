package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.model.TasbeehDhikr
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavySurfaceVariant
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LightEmeraldBackground
import com.example.ui.theme.LocalThemeIsDark

@Composable
fun TasbeehScreen() {
    val context = LocalContext.current

    val dhikrList = remember {
        listOf(
            TasbeehDhikr("1", "SubhanAllah", "سُبْحَانَ ٱللَّٰهِ", "Glory be to Allah", 33),
            TasbeehDhikr("2", "Alhamdulillah", "ٱلْحَمْدُ لِلَّٰهِ", "Praise be to Allah", 33),
            TasbeehDhikr("3", "Allahu Akbar", "ٱللَّٰهُ أَكْبَرُ", "Allah is the Greatest", 33),
            TasbeehDhikr("4", "Astaghfirullah", "أَسْتَغْفِرُ ٱللَّٰهَ", "I seek forgiveness from Allah", 100),
            TasbeehDhikr("5", "La ilaha illallah", "لَا إِلٰهَ إِلَّا ٱللَّٰهُ", "There is no god but Allah", 100)
        )
    }

    var selectedDhikr by remember { mutableStateOf(dhikrList[0]) }
    var currentCount by remember { mutableIntStateOf(0) }
    var completedLaps by remember { mutableIntStateOf(0) }
    var targetCount by remember { mutableIntStateOf(33) }
    var isHapticEnabled by remember { mutableStateOf(true) }
    var showResetDialog by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }

    val tapScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = tween(durationMillis = 80),
        label = "TapScale"
    )

    fun handleTap() {
        if (isHapticEnabled) {
            triggerTasbeehHaptic(context, isComplete = (currentCount + 1) % targetCount == 0)
        }
        val next = currentCount + 1
        if (next >= targetCount) {
            currentCount = 0
            completedLaps++
        } else {
            currentCount = next
        }
    }

    val isDark = LocalThemeIsDark.current
    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            listOf(
                DarkNavyBackground,
                Color(0xFF0C1635),
                Color(0xFF032B28),
                DarkNavyBackground
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                LightEmeraldBackground,
                Color(0xFFE7F3EC),
                Color(0xFFF0FDF4),
                LightEmeraldBackground
            )
        )
    }

    val progress = (currentCount.toFloat() / targetCount).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tasbeeh_screen")
            .background(bgGradient)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header & Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Digital Tasbeeh",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Touch screen to count remembrance",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    // Haptic Toggle
                    IconButton(
                        onClick = { isHapticEnabled = !isHapticEnabled },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isHapticEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Toggle vibration",
                            tint = if (isHapticEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Reset Button
                    IconButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset counter",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Dhikr selector row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dhikrList) { dhikr ->
                    val isSelected = dhikr.id == selectedDhikr.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else (if (isDark) Color(0x22E5C07B) else Color(0x18065F46))
                            )
                            .clickable {
                                selectedDhikr = dhikr
                                targetCount = dhikr.defaultTarget
                                currentCount = 0
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = dhikr.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                if (isDark) Color(0xFF1B1302) else Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            }

            // Active Dhikr Arabic Card
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = selectedDhikr.arabic,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = selectedDhikr.meaning,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Big Tapping Circle
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .scale(tapScale),
                contentAlignment = Alignment.Center
            ) {
                // Circular Progress Ring
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = if (isDark) Color(0x22E5C07B) else Color(0x22065F46),
                    strokeWidth = 8.dp
                )

                // Interactive Tap Surface
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (isDark) {
                                    listOf(Color(0xFF1A2A54), DarkNavySurfaceVariant)
                                } else {
                                    listOf(Color(0xFFFFFFFF), Color(0xFFD1FAE5))
                                }
                            )
                        )
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                isPressed = true
                                handleTap()
                                isPressed = false
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$currentCount",
                            fontSize = 62.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "TARGET: $targetCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TAP HERE",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Target Selector & Laps Bottom Section
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cycles Completed",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$completedLaps laps",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Target presets: 33, 99, 100
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(33, 99, 100).forEach { preset ->
                            val isChosen = targetCount == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isChosen) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        targetCount = preset
                                        currentCount = 0
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$preset",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isChosen) {
                                        if (isDark) Color(0xFF1B1302) else Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Tasbeeh Counter?") },
            text = { Text("This will reset your current count and completed laps back to zero.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        currentCount = 0
                        completedLaps = 0
                        showResetDialog = false
                    }
                ) {
                    Text("Reset", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

private fun triggerTasbeehHaptic(context: Context, isComplete: Boolean) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isComplete) {
                // Double pulse vibration when target reached
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 120), -1))
            } else {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(if (isComplete) 150 else 30)
        }
    } catch (_: Exception) {}
}
