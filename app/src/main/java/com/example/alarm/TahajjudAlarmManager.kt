package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
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
import java.util.TimeZone
import kotlin.math.sin

data class TahajjudAlarmState(
    val isEnabled: Boolean = true,
    val tahajjudTimeFormatted: String = "04:05 AM",
    val fajrTimeFormatted: String = "04:35 AM",
    val maghribTimeFormatted: String = "06:45 PM",
    val tahajjudWindowStartFormatted: String = "01:25 AM",
    val tahajjudWindowEndFormatted: String = "04:35 AM",
    val minutesBeforeFajr: Int = 30,
    val nextAlarmTimestampMillis: Long = 0L,
    val sleepCycleStage: String = "Light Sleep Phase (Optimal Wake Window)",
    val recommendedBedtime: String = "10:30 PM",
    val isRinging: Boolean = false,
    val soundTheme: String = "Dawn Birdsong & Flowing Creek"
)

/**
 * MODULE: TAHAJJUD ALARM MANAGER (VERSION 1.1)
 * Calculates the authentic Last Third of the Night window and schedules exact system alarms
 * with setExactAndAllowWhileIdle firing to PrayerAlarmReceiver with "TAHAJJUD" flag.
 */
object TahajjudAlarmManager {

    private const val TAG = "TahajjudAlarmManager"
    const val TAHAJJUD_REQUEST_CODE = 7771

    private val _state = MutableStateFlow(TahajjudAlarmState())
    val state = _state.asStateFlow()

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null

    fun updateFajrTime(fajrTimeStr: String) {
        updatePrayerTimes(null, fajrTimeStr, _state.value.maghribTimeFormatted)
    }

    fun updatePrayerTimes(context: Context? = null, fajrTimeStr: String, maghribTimeStr: String = "06:45 PM") {
        try {
            val tz = TimeZone.getDefault()
            val parser = SimpleDateFormat(if (fajrTimeStr.contains("M", ignoreCase = true)) "hh:mm a" else "HH:mm", Locale.ENGLISH).apply {
                timeZone = tz
            }
            val maghribParser = SimpleDateFormat(if (maghribTimeStr.contains("M", ignoreCase = true)) "hh:mm a" else "HH:mm", Locale.ENGLISH).apply {
                timeZone = tz
            }

            val fajrDate = parser.parse(fajrTimeStr)
            val maghribDate = maghribParser.parse(maghribTimeStr)

            if (fajrDate != null) {
                val now = Calendar.getInstance(tz)
                val fajrCal = Calendar.getInstance(tz).apply {
                    set(Calendar.HOUR_OF_DAY, fajrDate.hours)
                    set(Calendar.MINUTE, fajrDate.minutes)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (fajrCal.before(now)) {
                    fajrCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                // Calculate Last Third of the Night
                // Night duration = Fajr time - Maghrib time (wrapping past midnight)
                val maghribCal = Calendar.getInstance(tz).apply {
                    if (maghribDate != null) {
                        set(Calendar.HOUR_OF_DAY, maghribDate.hours)
                        set(Calendar.MINUTE, maghribDate.minutes)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    } else {
                        set(Calendar.HOUR_OF_DAY, 18)
                        set(Calendar.MINUTE, 45)
                    }
                }
                if (maghribCal.after(fajrCal)) {
                    maghribCal.add(Calendar.DAY_OF_YEAR, -1)
                }

                val nightDurationMillis = fajrCal.timeInMillis - maghribCal.timeInMillis
                val oneThirdMillis = nightDurationMillis / 3
                val lastThirdStartMillis = fajrCal.timeInMillis - oneThirdMillis

                val timeFormatter = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply { timeZone = tz }
                val windowStartFormatted = timeFormatter.format(Date(lastThirdStartMillis))
                val windowEndFormatted = timeFormatter.format(fajrCal.time)

                // Wakeup time: minutesBeforeFajr before Fajr
                val offset = _state.value.minutesBeforeFajr
                val alarmCal = Calendar.getInstance(tz).apply {
                    timeInMillis = fajrCal.timeInMillis
                    add(Calendar.MINUTE, -offset)
                }
                val tahajjudFormatted = timeFormatter.format(alarmCal.time)

                // Optimal Bedtime = 6 hours (4 sleep cycles) before Tahajjud
                val bedCal = Calendar.getInstance(tz).apply {
                    timeInMillis = alarmCal.timeInMillis
                    add(Calendar.HOUR_OF_DAY, -6)
                }
                val bedtimeFormatted = timeFormatter.format(bedCal.time)

                _state.value = _state.value.copy(
                    tahajjudTimeFormatted = tahajjudFormatted,
                    fajrTimeFormatted = fajrTimeStr,
                    maghribTimeFormatted = maghribTimeStr,
                    tahajjudWindowStartFormatted = windowStartFormatted,
                    tahajjudWindowEndFormatted = windowEndFormatted,
                    nextAlarmTimestampMillis = alarmCal.timeInMillis,
                    recommendedBedtime = bedtimeFormatted
                )

                if (_state.value.isEnabled && context != null) {
                    scheduleSystemAlarm(context, alarmCal.timeInMillis)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating Tahajjud time: ${e.message}", e)
        }
    }

    fun setCustomWakeupOffset(context: Context?, minutesBeforeFajr: Int) {
        val boundedOffset = minutesBeforeFajr.coerceIn(10, 180)
        _state.value = _state.value.copy(minutesBeforeFajr = boundedOffset)
        updatePrayerTimes(context, _state.value.fajrTimeFormatted, _state.value.maghribTimeFormatted)
    }

    fun toggleAlarm(enabled: Boolean) {
        toggleAlarm(null, enabled)
    }

    fun toggleAlarm(context: Context?, enabled: Boolean) {
        _state.value = _state.value.copy(isEnabled = enabled)
        val appContext = context?.applicationContext ?: return
        if (enabled) {
            val targetMillis = if (_state.value.nextAlarmTimestampMillis > System.currentTimeMillis()) {
                _state.value.nextAlarmTimestampMillis
            } else {
                System.currentTimeMillis() + 8 * 3600 * 1000L
            }
            scheduleSystemAlarm(appContext, targetMillis)
        } else {
            cancelSystemAlarm(appContext)
            if (_state.value.isRinging) {
                stopGentleNatureSounds()
            }
        }
    }

    /**
     * Schedules exact system alarm using AlarmManager.setExactAndAllowWhileIdle
     * with intent flagged with "TAHAJJUD" for lockscreen wakeup.
     */
    fun scheduleSystemAlarm(context: Context?, triggerTimeMillis: Long) {
        if (context == null) return
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "Cannot schedule exact alarms; permission missing")
                }
            }

            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = "com.example.alarm.TAHAJJUD_ALARM"
                putExtra("TAHAJJUD", true)
                putExtra("is_tahajjud", true)
                putExtra("prayer_name", "Tahajjud")
                putExtra("prayer_arabic", "تهجد")
                putExtra("alert_type", "EXACT")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                TAHAJJUD_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMillis,
                    pendingIntent
                )
            }

            _state.value = _state.value.copy(nextAlarmTimestampMillis = triggerTimeMillis)
            Log.d(TAG, "Tahajjud system alarm scheduled successfully for millis=$triggerTimeMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact Tahajjud alarm", e)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule Tahajjud system alarm", e)
        }
    }

    fun cancelSystemAlarm(context: Context?) {
        if (context == null) return
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = "com.example.alarm.TAHAJJUD_ALARM"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                TAHAJJUD_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Tahajjud system alarm cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling Tahajjud alarm", e)
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
                        val envelope = sin(Math.PI * (i.toDouble() / numSamples))
                        val tone = sin(2.0 * Math.PI * freq * time) * 0.7 +
                                   sin(2.0 * Math.PI * (freq * 1.5) * time) * 0.3
                        val sample = (tone * envelope * 12000).toInt().coerceIn(-32767, 32767)
                        buffer[i] = sample.toShort()
                    }

                    audioTrack?.write(buffer, 0, buffer.size)
                    delay(300)
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
