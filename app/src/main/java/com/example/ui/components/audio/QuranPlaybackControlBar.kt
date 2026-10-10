package com.example.ui.components.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.quran.repository.HifzRepeatMode
import com.example.service.audio.QariCatalog
import com.example.service.audio.QariId
import com.example.service.audio.QuranAudioController
import com.example.ui.components.quran.QariSelectionDialog
import com.example.ui.screens.SleepTimerDialog
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldSurfaceLight
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SuccessGreen
import java.util.Locale

/**
 * PRODUCTION-GRADE MEDIA3 QURAN PLAYBACK CONTROL BAR
 *
 * Displays a persistent, luxury Pearl White & Platinum Gold playback controller
 * with high-quality streaming details, interactive track progress slider,
 * play/pause transport, forward/rewind, next/prev surah, speed control, repeat modes,
 * and live audio equalizer animation.
 */
@Composable
fun QuranPlaybackControlBar(
    modifier: Modifier = Modifier,
    onTitleClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val isPlaying by QuranAudioController.isPlaying.collectAsStateWithLifecycle()
    val isBuffering by QuranAudioController.isBuffering.collectAsStateWithLifecycle()
    val currentPositionMs by QuranAudioController.currentPositionMs.collectAsStateWithLifecycle()
    val currentDurationMs by QuranAudioController.currentDurationMs.collectAsStateWithLifecycle()
    val activeSurahId by QuranAudioController.activeSurahId.collectAsStateWithLifecycle()
    val activeSurahName by QuranAudioController.activeSurahName.collectAsStateWithLifecycle()
    val activeSurahArabic by QuranAudioController.activeSurahArabic.collectAsStateWithLifecycle()
    val activeQari by QuranAudioController.activeQari.collectAsStateWithLifecycle()
    val isOfflineAudio by QuranAudioController.isOfflineAudio.collectAsStateWithLifecycle()
    val playbackSpeed by QuranAudioController.playbackSpeed.collectAsStateWithLifecycle()
    val repeatMode by QuranAudioController.repeatMode.collectAsStateWithLifecycle()
    val sleepTimerSeconds by QuranAudioController.sleepTimerSeconds.collectAsStateWithLifecycle()
    val hasActiveSleepTimer = sleepTimerSeconds?.let { it > 0 } == true

    // Scrubbing state for interactive slider
    var isDraggingSlider by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }

    // Expanded controls sheet/drawer toggle
    var isExpanded by remember { mutableStateOf(false) }
    var showQariDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val qariProfile = remember(activeQari) { QariCatalog.getQariById(activeQari) }

    // Progress computation
    val actualProgress = if (currentDurationMs > 0) {
        (currentPositionMs.toFloat() / currentDurationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val sliderValue = if (isDraggingSlider) scrubProgress else actualProgress

    val surahFormattedNumber = String.format(Locale.US, "%03d", activeSurahId)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(12.dp, RoundedCornerShape(22.dp))
            .testTag("quran_playback_control_bar"),
        shape = RoundedCornerShape(22.dp),
        color = PureWhite,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, PlatinumGold.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // ==========================================
            // ROW 1: SURAH HEADER & RECITING METADATA
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = onTitleClick != null) { onTitleClick?.invoke() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Pulsing animated equalizer or static icon
                    AudioWaveVisualizer(isPlaying = isPlaying)

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$surahFormattedNumber. Surah $activeSurahName",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepRoyalEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isOfflineAudio) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SuccessGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Offline MP3",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${qariProfile.nameEnglish} ${qariProfile.flag}",
                                fontSize = 11.sp,
                                color = SlateMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "• $activeSurahArabic",
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Serif,
                                color = PlatinumGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Expand/Collapse Chevron
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Stop/Close Button
                    IconButton(
                        onClick = { QuranAudioController.stopAudio() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Audio",
                            tint = SlateMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ==========================================
            // ROW 2: INTERACTIVE TRACK PROGRESS SLIDER
            // ==========================================
            Spacer(modifier = Modifier.height(4.dp))

            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    isDraggingSlider = true
                    scrubProgress = newValue
                },
                onValueChangeFinished = {
                    isDraggingSlider = false
                    val targetMs = (scrubProgress * currentDurationMs.toFloat()).toLong()
                    QuranAudioController.seekTo(targetMs)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .testTag("track_progress_slider"),
                colors = SliderDefaults.colors(
                    thumbColor = PlatinumGold,
                    activeTrackColor = DeepRoyalEmerald,
                    inactiveTrackColor = EmeraldSurfaceLight
                )
            )

            // Elapsed time and Total duration labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayCurrentMs = if (isDraggingSlider) {
                    (scrubProgress * currentDurationMs.toFloat()).toLong()
                } else {
                    currentPositionMs
                }
                Text(
                    text = formatDuration(displayCurrentMs),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateMuted
                )

                if (isBuffering) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = RadiantEmerald,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Buffering audio...",
                            fontSize = 10.sp,
                            color = RadiantEmerald,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = formatDuration(currentDurationMs),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ==========================================
            // ROW 3: MAIN TRANSPORT CONTROLS
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous Surah Button
                IconButton(
                    onClick = { QuranAudioController.playPreviousSurah(context) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Surah",
                        tint = DeepRoyalEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Rewind 10 Seconds
                IconButton(
                    onClick = { QuranAudioController.seekBackward(10000L) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = DeepRoyalEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Prominent Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(RadiantEmerald, DeepRoyalEmerald)
                            )
                        )
                        .border(1.5.dp, PlatinumGold, CircleShape)
                        .clickable { QuranAudioController.togglePlayPause(context) }
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = PureWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Forward 10 Seconds
                IconButton(
                    onClick = { QuranAudioController.seekForward(10000L) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        tint = DeepRoyalEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Next Surah Button
                IconButton(
                    onClick = { QuranAudioController.playNextSurah(context) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Surah",
                        tint = DeepRoyalEmerald,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // ==========================================
            // ROW 4: EXPANDABLE ADVANCED CONTROLS
            // ==========================================
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFF1F5F9))
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Playback Speed Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldSurfaceLight)
                                .clickable {
                                    val nextSpeed = when (playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 0.75f
                                        else -> 1.0f
                                    }
                                    QuranAudioController.setPlaybackSpeed(nextSpeed)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Speed: ${playbackSpeed}x",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepRoyalEmerald
                            )
                        }

                        // 2. Repeat Mode Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (repeatMode != HifzRepeatMode.OFF) EmeraldSurfaceLight
                                    else Color(0xFFF1F5F9)
                                )
                                .clickable {
                                    val nextMode = when (repeatMode) {
                                        HifzRepeatMode.OFF -> HifzRepeatMode.REPEAT_1X
                                        HifzRepeatMode.REPEAT_1X -> HifzRepeatMode.REPEAT_3X
                                        HifzRepeatMode.REPEAT_3X -> HifzRepeatMode.REPEAT_5X
                                        HifzRepeatMode.REPEAT_5X -> HifzRepeatMode.REPEAT_INFINITE
                                        HifzRepeatMode.REPEAT_INFINITE -> HifzRepeatMode.OFF
                                    }
                                    QuranAudioController.setRepeatMode(nextMode)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = if (repeatMode != HifzRepeatMode.OFF) RadiantEmerald else SlateMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = repeatMode.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (repeatMode != HifzRepeatMode.OFF) RadiantEmerald else SlateMuted
                                )
                            }
                        }

                        // 3. Reciter Selector Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldSurfaceLight)
                                .clickable { showQariDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = PlatinumGold,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reciter",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepRoyalEmerald
                                )
                            }
                        }

                        // 4. Sleep Timer Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (hasActiveSleepTimer) EmeraldSurfaceLight
                                    else Color(0xFFF1F5F9)
                                )
                                .clickable { showSleepTimerDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NightlightRound,
                                    contentDescription = null,
                                    tint = if (hasActiveSleepTimer) PlatinumGold else SlateMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                val label = if (hasActiveSleepTimer) {
                                    val m = (sleepTimerSeconds ?: 0) / 60
                                    "${m}m"
                                } else {
                                    "Timer"
                                }
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasActiveSleepTimer) PlatinumGold else SlateMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showQariDialog) {
        QariSelectionDialog(
            currentQariId = activeQari,
            onSelectQari = { selectedProfile ->
                showQariDialog = false
                QuranAudioController.playSurahWithQari(
                    context = context,
                    surahId = activeSurahId,
                    surahName = activeSurahName,
                    qariId = selectedProfile.id
                )
            },
            onDismiss = { showQariDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            currentRemainingSeconds = sleepTimerSeconds,
            onDismiss = { showSleepTimerDialog = false }
        )
    }
}

/**
 * Animated 4-bar audio equalizer visualizer.
 */
@Composable
private fun AudioWaveVisualizer(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_wave_transition")

    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    val bar4Height by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    Row(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(EmeraldSurfaceLight)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val animatedColor by animateColorAsState(
            targetValue = if (isPlaying) RadiantEmerald else PlatinumGold,
            label = "color"
        )

        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isPlaying) bar1Height.dp else 8.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(animatedColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isPlaying) bar2Height.dp else 14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(animatedColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isPlaying) bar3Height.dp else 10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(animatedColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(if (isPlaying) bar4Height.dp else 6.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(animatedColor)
        )
    }
}

/**
 * Format milliseconds into standard mm:ss representation.
 */
private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "00:00"
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes >= 60) {
        val hours = minutes / 60
        val remMinutes = minutes % 60
        String.format(Locale.US, "%d:%02d:%02d", hours, remMinutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
