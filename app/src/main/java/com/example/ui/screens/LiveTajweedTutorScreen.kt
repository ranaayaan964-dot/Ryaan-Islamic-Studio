package com.example.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.live.GeminiLiveTajweedTutor
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldSecondary

@Composable
fun LiveTajweedTutorScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val tutor = remember { GeminiLiveTajweedTutor(context) }
    val tutorState by tutor.state.collectAsState()

    var selectedSurah by remember { mutableStateOf("Surah Al-Fatiha") }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            tutor.startSession(selectedSurah)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tutor.stopSession()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LiveWave")
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WaveScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_tajweed_tutor_screen")
            .background(
                Brush.verticalGradient(
                    listOf(
                        DarkNavyBackground,
                        Color(0xFF071923),
                        Color(0xFF03221C),
                        DarkNavyBackground
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(44.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "LIVE TAJWEED TUTOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldSecondary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Gemini Multimodal Live AI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (tutorState.isStreamingAudio) Color(0x3310B981) else Color(0x33F59E0B))
                        .border(
                            1.dp,
                            if (tutorState.isStreamingAudio) EmeraldAccent else Color(0xFFF59E0B),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (tutorState.isStreamingAudio) "● WEBSOCKET LIVE" else "○ STANDBY",
                        color = if (tutorState.isStreamingAudio) Color(0xFF6EE7B7) else Color(0xFFFBBF24),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Surah Selection Chips
            val surahOptions = listOf("Surah Al-Fatiha", "Surah Al-Ikhlas", "Ayat Al-Kursi", "Surah Al-Mulk")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                surahOptions.take(3).forEach { surah ->
                    FilterChip(
                        selected = selectedSurah == surah,
                        onClick = {
                            selectedSurah = surah
                            if (tutorState.isStreamingAudio) {
                                tutor.stopSession()
                                tutor.startSession(surah)
                            }
                        },
                        label = { Text(surah, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldAccent.copy(alpha = 0.25f),
                            selectedLabelColor = EmeraldAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Central Pulsing Microphone Visualizer
            Box(
                modifier = Modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer audio wave ripple
                if (tutorState.isStreamingAudio) {
                    Box(
                        modifier = Modifier
                            .size((160 * waveScale).dp)
                            .clip(CircleShape)
                            .background(
                                (if (tutorState.isTutorSpeaking) Color(0x33F59E0B) else Color(0x2210B981))
                            )
                    )
                }

                // Inner core circle
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    if (tutorState.isTutorSpeaking) Color(0xFFD97706) else Color(0xFF047857),
                                    Color(0xFF064E3B),
                                    Color(0xFF022C22)
                                )
                            )
                        )
                        .border(
                            2.dp,
                            if (tutorState.isTutorSpeaking) GoldSecondary else EmeraldAccent,
                            CircleShape
                        )
                        .clickable {
                            if (!hasAudioPermission) {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (tutorState.isStreamingAudio) {
                                    tutor.stopSession()
                                } else {
                                    tutor.startSession(selectedSurah)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (tutorState.isStreamingAudio) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Microphone Toggle",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (tutorState.isStreamingAudio) "Listening in Real-Time (16kHz PCM)" else "Tap Microphone to Start Live Recitation",
                color = if (tutorState.isStreamingAudio) Color(0xFF6EE7B7) else Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Live Audio Spectrogram Bars
            if (tutorState.isStreamingAudio) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(24) { idx ->
                        val ampFactor = (tutorState.audioAmplitude * ((idx % 5) + 1) * 20f).coerceIn(4f, 44f)
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(ampFactor.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (tutorState.isTutorSpeaking) Color(0xFFF59E0B) else Color(0xFF10B981)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Real-Time Interruption / Correction Card
            AnimatedVisibility(visible = tutorState.isTutorSpeaking || tutorState.latestInterruptionNote != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xEE451A03)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = GoldSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LIVE VOCAL INTERVENTION",
                                color = GoldSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = tutorState.latestInterruptionNote
                                ?: "Notice: Throat letter 'Haa' (ح) articulated from the middle of the throat, rather than chest letter 'Haa' (هـ). Listen to the correction.",
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFF6EE7B7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Synthesized Voice Playing (24kHz AudioTrack)",
                                color = Color(0xFF6EE7B7),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Transcript & Guidance Log
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xCC091624)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LIVE SPEECH STREAM & TAJWEED FEED:",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = tutorState.liveTranscript,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Target: $selectedSurah",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Mistakes Intercepted: ${tutorState.mistakeCount}",
                            color = GoldSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
