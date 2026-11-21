package com.ayaan.islamicstudio.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

object AdhanSoundPlayer {

    private const val TAG = "AdhanSoundPlayer"
    private var mediaPlayer: MediaPlayer? = null
    private var synthesisJob: Job? = null
    @Volatile
    private var isPlaying = false
    private var audioManager: AudioManager? = null
    private var originalAlarmVolume: Int? = null
    @Volatile
    private var playbackGeneration = 0L

    fun isPlayingAlert(): Boolean = isPlaying

    /**
     * Plays an audible Adhan / Prayer alert.
     * Uses system alarm/ringtone or generates a harmonic spiritual chime sequence.
     */
    fun playAdhanAlert(context: Context, looping: Boolean = false, onComplete: (() -> Unit)? = null) {
        stopAdhan()
        isPlaying = true
        boostAlarmVolume(context)

        // Trigger vibration
        vibrateDevice(context)

        // Try playing system notification or alarm ringtone first
        try {
            var alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            if (alertUri != null) {
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setLegacyStreamType(AudioManager.STREAM_ALARM)
                            .build()
                    )
                            setDataSource(context, alertUri)
                            setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = looping
                    if (!looping) {
                        setOnCompletionListener {
                            stopAdhan()
                            onComplete?.invoke()
                        }
                    }
                    setOnPreparedListener { preparedPlayer ->
                        if (isPlaying && mediaPlayer === preparedPlayer) {
                            preparedPlayer.start()
                        } else {
                            preparedPlayer.release()
                        }
                    }
                    setOnErrorListener { failedPlayer, what, extra ->
                        Log.e(TAG, "System alarm audio failed: what=$what extra=$extra")
                        val shouldFallback = mediaPlayer === failedPlayer && isPlaying
                        if (shouldFallback) {
                            mediaPlayer = null
                        }
                        failedPlayer.release()
                        if (shouldFallback) {
                            playMelodicChime(looping = looping, onComplete = onComplete)
                        }
                        true
                    }
                }
                mediaPlayer = player
                player.prepareAsync()
                return
            }
        } catch (e: Exception) {
            mediaPlayer?.release()
            mediaPlayer = null
            Log.e(TAG, "Failed playing standard ringtone: ${e.message}, falling back to melodic chime")
        }

        // Fallback: Synthesize a peaceful multi-tone adhan melody via AudioTrack
        playMelodicChime(looping = looping, onComplete = onComplete)
    }

    private fun playMelodicChime(looping: Boolean = false, onComplete: (() -> Unit)?) {
        val generation = playbackGeneration
        synthesisJob = CoroutineScope(Dispatchers.Default).launch {
            var audioTrack: AudioTrack? = null
            var completed = false
            try {
                val sampleRate = 44100
                // Spiritual pentatonic notes: D4, F4, G4, A4, C5 (approx Adhan motif)
                val frequencies = doubleArrayOf(293.66, 349.23, 392.00, 440.00, 523.25, 440.00, 392.00, 349.23)
                val durationSec = 0.6

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val activeAudioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_ALARM)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBufferSize * 2)
                    .build()
                audioTrack = activeAudioTrack

                activeAudioTrack.play()

                do {
                    for (freq in frequencies) {
                        if (!isPlaying || playbackGeneration != generation) break
                        val numSamples = (sampleRate * durationSec).toInt()
                        val buffer = ShortArray(numSamples)

                        for (i in 0 until numSamples) {
                            val time = i.toDouble() / sampleRate
                            // Envelope for smooth attack & decay
                            val envelope = if (i < sampleRate * 0.05) {
                                i / (sampleRate * 0.05)
                            } else {
                                (1.0 - (i.toDouble() / numSamples)).coerceAtLeast(0.0)
                            }
                            val sample = (sin(2.0 * Math.PI * freq * time) * envelope * Short.MAX_VALUE * 0.7).toInt()
                            buffer[i] = sample.toShort()
                        }

                        activeAudioTrack.write(buffer, 0, numSamples)
                        delay(50)
                    }
                    if (looping && isPlaying && playbackGeneration == generation) {
                        delay(1000)
                    }
                } while (looping && isPlaying && playbackGeneration == generation)
                completed = isPlaying && playbackGeneration == generation
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chime synthesis error: ${e.message}")
                completed = true
            } finally {
                audioTrack?.let { track ->
                    try {
                        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not stop synthesized chime", e)
                    } finally {
                        track.release()
                    }
                }
                if (playbackGeneration == generation) {
                    isPlaying = false
                    restoreAlarmVolume()
                    if (completed) onComplete?.invoke()
                }
            }
        }
    }

    fun stopAdhan() {
        playbackGeneration++
        isPlaying = false
        synthesisJob?.cancel()
        synthesisJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        restoreAlarmVolume()
    }

    private fun restoreAlarmVolume() {
        originalAlarmVolume?.let { previousVolume ->
            try {
                audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0)
            } catch (e: Exception) {
                Log.w(TAG, "Could not restore alarm stream volume", e)
            }
        }
        originalAlarmVolume = null
    }

    private fun boostAlarmVolume(context: Context) {
        val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        audioManager = manager
        try {
            val currentVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM)
            if (currentVolume == 0) {
                if (originalAlarmVolume == null) originalAlarmVolume = currentVolume
                val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                if (maxVolume > 0) {
                    manager.setStreamVolume(
                        AudioManager.STREAM_ALARM,
                        (maxVolume * 0.7f).toInt().coerceIn(1, maxVolume),
                        0
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not raise alarm stream volume", e)
        }
    }

    private fun vibrateDevice(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 400, 200, 400, 200, 600)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 400, 200, 400), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }
}
