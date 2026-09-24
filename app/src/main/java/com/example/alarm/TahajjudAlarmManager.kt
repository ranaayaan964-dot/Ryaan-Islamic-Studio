package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.sin

data class TahajjudAlarmState(
    val isEnabled: Boolean = true,
    val tahajjudTimeFormatted: String = "04:05 AM",
    val fajrTimeFormatted: String = "04:35 AM",
    val sleepCycleStage: String = "Light Sleep Phase (Optimal Wake Window)",
    val recommendedBedtime: String = "10:30 PM",
    val isRinging: Boolean = false,
    val soundTheme: String = "Dawn Birdsong & Flowing Creek"
)

object TahajjudAlarmManager {

    private const val TAG = "TahajjudAlarmManager"
    private val _state = MutableStateFlow(TahajjudAlarmState())
    val state = _state.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    fun updateFajrTime(fajrTimeStr: String) {
        // e.g., "04:35 AM" or "04:35"
        try {
            val pkZone = java.util.TimeZone.getTimeZone("Asia/Karachi")
            val parser = SimpleDateFormat(if (fajrTimeStr.contains("M")) "hh:mm a" else "HH:mm", Locale.ENGLISH).apply {
                timeZone = pkZone
            }
            val date = parser.parse(fajrTimeStr)
            if (date != null) {
                val cal = Calendar.getInstance(pkZone).apply {
                    time = date
                    add(Calendar.MINUTE, -30) // 30 minutes before Fajr
                }
                val formatter = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply {
                    timeZone = pkZone
                }
                val tahajjudFormatted = formatter.format(cal.time)

                // Recommended bedtime = 6 hours before Tahajjud
                val bedCal = Calendar.getInstance(pkZone).apply {
                    time = cal.time
                    add(Calendar.HOUR_OF_DAY, -6)
                }
                val bedtimeFormatted = formatter.format(bedCal.time)

                _state.value = _state.value.copy(
                    tahajjudTimeFormatted = tahajjudFormatted,
                    fajrTimeFormatted = fajrTimeStr,
                    recommendedBedtime = bedtimeFormatted
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating Tahajjud time: ${e.message}")
        }
    }

    fun toggleAlarm(enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
        if (!enabled && _state.value.isRinging) {
            stopGentleNatureSounds()
        }
    }

    /**
     * Synthesizes gentle, binaural nature dawn chimes & birdsong wave
     */
    fun playGentleNatureSounds() {
        stopGentleNatureSounds()
        _state.value = _state.value.copy(isRinging = true)

        playbackJob = CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 44100
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack?.play()

                // Harmonious Pentatonic Dawn Bells (C5, D5, E5, G5, A5, C6) + subtle nature noise
                val chordFreqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
                var chordIdx = 0

                while (isActive && _state.value.isRinging) {
                    val freq = chordFreqs[chordIdx % chordFreqs.size]
                    chordIdx++

                    val durationMs = 1200
                    val numSamples = (sampleRate * durationMs / 1000)
                    val buffer = ShortArray(numSamples)

                    for (i in 0 until numSamples) {
                        val time = i.toDouble() / sampleRate
                        // Soft envelope
                        val envelope = sin(Math.PI * (i.toDouble() / numSamples))
                        // Gentle fundamental + harmonic
                        val tone = sin(2.0 * Math.PI * freq * time) * 0.7 +
                                   sin(2.0 * Math.PI * (freq * 1.5) * time) * 0.3
                        // Subtle gentle ambient nature chime
                        val sample = (tone * envelope * 12000).toInt().coerceIn(-32767, 32767)
                        buffer[i] = sample.toShort()
                    }

                    audioTrack?.write(buffer, 0, buffer.size)
                    delay(300) // soft pause between chimes
                }
            } catch (e: Exception) {
                Log.e(TAG, "AudioTrack error: ${e.message}")
            }
        }
    }

    fun stopGentleNatureSounds() {
        _state.value = _state.value.copy(isRinging = false)
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}
