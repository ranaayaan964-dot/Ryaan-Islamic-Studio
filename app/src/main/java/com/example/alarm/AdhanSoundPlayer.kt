package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
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
    private var isPlaying = false

    fun isPlayingAlert(): Boolean = isPlaying

    /**
     * Plays an audible Adhan / Prayer alert.
     * Uses system alarm/ringtone or generates a harmonic spiritual chime sequence.
     */
    fun playAdhanAlert(context: Context, looping: Boolean = false, onComplete: (() -> Unit)? = null) {
        stopAdhan()
        isPlaying = true

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
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, alertUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setLegacyStreamType(AudioManager.STREAM_ALARM)
                            .build()
                    )
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = looping
                    if (!looping) {
                        setOnCompletionListener {
                            stopAdhan()
                            onComplete?.invoke()
                        }
                    }
                    prepare()
                    start()
                }
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed playing standard ringtone: ${e.message}, falling back to melodic chime")
        }

        // Fallback: Synthesize a peaceful multi-tone adhan melody via AudioTrack
        playMelodicChime(looping = looping, onComplete = onComplete)
    }

    private fun playMelodicChime(looping: Boolean = false, onComplete: (() -> Unit)?) {
        synthesisJob = CoroutineScope(Dispatchers.Default).launch {
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

                val audioTrack = AudioTrack.Builder()
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

                audioTrack.play()

                do {
                    for (freq in frequencies) {
                        if (!isPlaying) break
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

                        audioTrack.write(buffer, 0, numSamples)
                        delay(50)
                    }
                    if (looping && isPlaying) {
                        delay(1000)
                    }
                } while (looping && isPlaying)

                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e(TAG, "Chime synthesis error: ${e.message}")
            } finally {
                isPlaying = false
                onComplete?.invoke()
            }
        }
    }

    fun stopAdhan() {
        isPlaying = false
        synthesisJob?.cancel()
        synthesisJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
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
