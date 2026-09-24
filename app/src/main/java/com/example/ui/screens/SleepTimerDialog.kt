package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.audio.QuranAudioController
import com.example.ui.theme.LocalThemeIsDark

data class SleepTimerOption(
    val title: String,
    val subtitle: String,
    val minutes: Int
)

@Composable
fun SleepTimerDialog(
    currentRemainingSeconds: Int?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDark = LocalThemeIsDark.current

    val options = listOf(
        SleepTimerOption("Off", "Keep playing continuously", 0),
        SleepTimerOption("15 Minutes", "Quick relaxation reading", 15),
        SleepTimerOption("30 Minutes", "Standard nighttime reading", 30),
        SleepTimerOption("45 Minutes", "Deep meditation & sleep", 45),
        SleepTimerOption("60 Minutes", "One full hour recitation", 60),
        SleepTimerOption("End of Surah", "Stop automatically when current Surah finishes", -1)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDark) Color(0x33A855F7) else Color(0x289333EA)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0x22A855F7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NightlightRound,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Audio Sleep Timer",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentRemainingSeconds != null) {
                                    if (currentRemainingSeconds == -1) "Active: End of Surah"
                                    else "Active: ${currentRemainingSeconds / 60}m ${currentRemainingSeconds % 60}s remaining"
                                } else "Timer is currently off",
                                fontSize = 11.5.sp,
                                color = if (currentRemainingSeconds != null) Color(0xFFA855F7)
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { opt ->
                        val isSelected = when {
                            opt.minutes == 0 -> currentRemainingSeconds == null
                            opt.minutes == -1 -> currentRemainingSeconds == -1
                            else -> currentRemainingSeconds != null && currentRemainingSeconds > 0 &&
                                    (currentRemainingSeconds + 59) / 60 == opt.minutes
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) Color(0xFFA855F7).copy(alpha = 0.15f)
                                    else if (isDark) Color(0xFF1E293B) else Color.White
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFFA855F7)
                                    else if (isDark) Color(0x18FFFFFF) else Color(0x12000000),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    QuranAudioController.setSleepTimer(opt.minutes)
                                    val msg = if (opt.minutes == 0) "Sleep timer cancelled"
                                    else if (opt.minutes == -1) "Sleep timer set: End of Surah"
                                    else "Sleep timer set for ${opt.minutes} minutes"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFFA855F7)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = opt.title,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFFA855F7)
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = opt.subtitle,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFA855F7))
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
