package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SoundPreviewPlayer {

    private const val TAG = "SoundPreviewPlayer"

    private val _playingSoundId = MutableStateFlow<String?>(null)
    val playingSoundId = _playingSoundId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var prepareJob: Job? = null

    fun playOrPause(context: Context, sound: IslamicSoundItem) {
        if (_playingSoundId.value == sound.id && _isPlaying.value) {
            stop()
            return
        }

        stop()
        _playingSoundId.value = sound.id
        _isLoading.value = true

        prepareJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ensure audio is cached or resolved
                val uriStr = if (AudioCacheManager.isSoundAvailableLocally(context, sound)) {
                    AudioCacheManager.resolvePlayableUri(context, sound)
                } else {
                    // Download & cache in background, or fallback to synthesized tone
                    var resolvedUri: String = sound.remoteAudioUrl
                    AudioCacheManager.downloadAndCacheSound(
                        context,
                        sound,
                        onSuccess = { cachedUri ->
                            resolvedUri = cachedUri
                        }
                    )
                    AudioCacheManager.resolvePlayableUri(context, sound)
                }

                withContext(Dispatchers.Main) {
                    startPlayback(context, uriStr, sound.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating preview for ${sound.title}: ${e.message}")
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _isPlaying.value = false
                    _playingSoundId.value = null
                }
            }
        }
    }

    private fun startPlayback(context: Context, uriString: String, soundId: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
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
                    _isLoading.value = false
                    _isPlaying.value = true
                    _playingSoundId.value = soundId
                    mp.start()
                    Log.d(TAG, "Preview playback started for sound: $soundId")
                }

                setOnCompletionListener {
                    stop()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra for $soundId")
                    stop()
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start playback for $soundId: ${e.message}")
            stop()
        }
    }

    fun stop() {
        prepareJob?.cancel()
        prepareJob = null
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        _isPlaying.value = false
        _isLoading.value = false
        _playingSoundId.value = null
    }
}
