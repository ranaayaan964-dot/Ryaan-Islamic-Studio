package com.ayaan.islamicstudio.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * MODULE: SOUND PREVIEW PLAYER (VERSION 1.1 HARDENED AUDIO ENGINE)
 * Resolves preview audio race conditions, overlapping playbacks, and MediaPlayer
 * IllegalStateException crashes by thoroughly releasing players and serializing state transitions.
 */
object SoundPreviewPlayer {

    private const val TAG = "SoundPreviewPlayer"

    private val _playingSoundId = MutableStateFlow<String?>(null)
    val playingSoundId = _playingSoundId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    @Volatile
    private var mediaPlayer: MediaPlayer? = null
    private var prepareJob: Job? = null
    private val playerLock = Any()

    fun playOrPause(context: Context, sound: IslamicSoundItem) {
        synchronized(playerLock) {
            if (_playingSoundId.value == sound.id && _isPlaying.value) {
                stop()
                return
            }

            stop()
            _playingSoundId.value = sound.id
            _isLoading.value = true
        }

        prepareJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val uriStr = if (AudioCacheManager.isSoundAvailableLocally(context, sound)) {
                    AudioCacheManager.resolvePlayableUri(context, sound)
                } else {
                    suspendCancellableCoroutine { continuation ->
                        AudioCacheManager.downloadAndCacheSound(
                            context,
                            sound,
                            onSuccess = { cachedUri ->
                                if (continuation.isActive) continuation.resume(cachedUri)
                            },
                            onError = { message ->
                                if (continuation.isActive) {
                                    continuation.resumeWithException(IllegalStateException(message))
                                }
                            }
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    synchronized(playerLock) {
                        // Check if the user hasn't cancelled or switched while loading on IO thread
                        if (_playingSoundId.value == sound.id) {
                            startPlayback(context, uriStr, sound.id)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating preview for ${sound.title}: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (_playingSoundId.value == sound.id) stop()
                }
            }
        }
    }

    private fun startPlayback(context: Context, uriString: String, soundId: String) {
        // Thoroughly release any lingering media player instance
        releaseMediaPlayerInternal()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                        .build()
                )

                if (uriString.startsWith("android.resource://")) {
                    setDataSource(context, Uri.parse(uriString))
                } else if (uriString.startsWith("file://")) {
                    setDataSource(context, Uri.parse(uriString))
                } else if (uriString.startsWith("/")) {
                    setDataSource(uriString)
                } else {
                    setDataSource(uriString)
                }

                setOnPreparedListener { mp ->
                    synchronized(playerLock) {
                        try {
                            if (_playingSoundId.value == soundId && mediaPlayer === mp) {
                                _isLoading.value = false
                                _isPlaying.value = true
                                mp.start()
                                Log.d(TAG, "Preview playback started for sound: $soundId")
                            } else {
                                // Another sound was triggered during prepare
                                mp.release()
                            }
                        } catch (ise: IllegalStateException) {
                            Log.w(TAG, "IllegalStateException in onPrepared: ${ise.message}")
                            stop()
                        }
                    }
                }

                setOnCompletionListener {
                    synchronized(playerLock) {
                        if (mediaPlayer === it && _playingSoundId.value == soundId) stop()
                    }
                }

                setOnErrorListener { failedPlayer, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra for $soundId")
                    synchronized(playerLock) {
                        if (mediaPlayer === failedPlayer && _playingSoundId.value == soundId) stop()
                    }
                    true
                }
            }
            mediaPlayer = player
            player.prepareAsync()
        } catch (ise: IllegalStateException) {
            Log.e(TAG, "IllegalStateException during startPlayback for $soundId", ise)
            stop()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback for $soundId: ${e.message}", e)
            stop()
        }
    }

    private fun releaseMediaPlayerInternal() {
        try {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                } catch (_: IllegalStateException) {
                    // Ignored: Player may already be stopped or in uninitialized state
                }
                try {
                    mp.reset()
                } catch (_: IllegalStateException) {
                    // Ignored
                }
                mp.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception during mediaPlayer release", e)
        } finally {
            mediaPlayer = null
        }
    }

    fun stop() {
        synchronized(playerLock) {
            prepareJob?.cancel()
            prepareJob = null
            releaseMediaPlayerInternal()
            _isPlaying.value = false
            _isLoading.value = false
            _playingSoundId.value = null
        }
    }
}
