package com.ayaan.islamicstudio.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SoundPreviewPlayer {
    private const val TAG = "SoundPreviewPlayer"

    private val _playingSoundId = MutableStateFlow<String?>(null)
    val playingSoundId = _playingSoundId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering = _isBuffering.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError = _playbackError.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var prepareJob: Job? = null
    private val playerLock = Any()
    private var activeMirrorIndex = 0
    private var activeMirrors = emptyList<String>()
    private var activeSoundId: String? = null

    fun playOrPause(context: Context, sound: IslamicSoundItem) {
        synchronized(playerLock) {
            if (_playingSoundId.value == sound.id && _isPlaying.value) {
                stop()
                return
            }
            stopInternal()
            activeSoundId = sound.id
            activeMirrors = (listOf(sound.remoteAudioUrl) + sound.fallbackUrls).distinct().filter(String::isNotBlank)
            activeMirrorIndex = 0
            _playingSoundId.value = sound.id
            _isLoading.value = true
            _isBuffering.value = false
            _playbackError.value = null
        }

        prepareJob = CoroutineScope(Dispatchers.IO).launch {
            val localFile = AudioCacheManager.getLocalFileForSound(context, sound.id)
            val uri = if (localFile.exists() && localFile.length() >= 8192L) {
                Uri.fromFile(localFile)
            } else {
                null
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                synchronized(playerLock) {
                    if (_playingSoundId.value == sound.id) {
                        if (uri != null) {
                            startPlayback(context, uri.toString(), sound.id)
                        } else if (activeMirrors.isNotEmpty()) {
                            startPlayback(context, activeMirrors.first(), sound.id)
                            CoroutineScope(Dispatchers.IO).launch {
                                AudioCacheManager.downloadAndCacheSound(
                                    context,
                                    sound,
                                    onSuccess = null,
                                    onError = null
                                )
                            }
                        } else {
                            _isLoading.value = false
                            _playbackError.value = "Connect to internet to stream or download this audio"
                            stopInternal()
                        }
                    }
                }
            }
        }
    }

    private fun startPlayback(context: Context, uri: String, soundId: String) {
        releaseInternal()
        try {
            val httpDataSource = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent("AyaanIslamicStudio/1.1.1")
            val player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(
                    androidx.media3.exoplayer.source.DefaultMediaSourceFactory(httpDataSource)
                )
                .build()
                .apply {
                    setMediaItem(MediaItem.fromUri(uri))
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            if (_playingSoundId.value == soundId) {
                                _isPlaying.value = isPlaying
                            }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (_playingSoundId.value != soundId) return
                            when (playbackState) {
                                Player.STATE_BUFFERING -> {
                                    _isBuffering.value = true
                                    _isLoading.value = true
                                }
                                Player.STATE_READY -> {
                                    _isBuffering.value = false
                                    _isLoading.value = false
                                    _playbackError.value = null
                                }
                                Player.STATE_ENDED -> stopInternal()
                                Player.STATE_IDLE -> _isBuffering.value = false
                            }
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            if (_playingSoundId.value != soundId) return
                            Log.w(TAG, "Real MP3 preview failed: ${error.errorCodeName}")
                            val nextIndex = activeMirrorIndex + 1
                            if (nextIndex < activeMirrors.size) {
                                activeMirrorIndex = nextIndex
                                val nextUri = activeMirrors[nextIndex]
                                releaseInternal()
                                startPlayback(context, nextUri, soundId)
                            } else {
                                _isBuffering.value = false
                                _isLoading.value = false
                                _playbackError.value =
                                    "Connect to internet to stream or download this audio"
                                stopInternal()
                            }
                        }
                    })
                    prepare()
                    playWhenReady = true
                }
            exoPlayer = player
        } catch (error: Exception) {
            Log.w(TAG, "Could not start real MP3 preview.", error)
            _isBuffering.value = false
            _isLoading.value = false
            _playbackError.value = "Connect to internet to stream or download this audio"
            stopInternal()
        }
    }

    private fun releaseInternal() {
        try {
            exoPlayer?.release()
        } catch (_: Exception) {}
        exoPlayer = null
    }

    private fun stopInternal() {
        prepareJob?.cancel()
        prepareJob = null
        releaseInternal()
        _isPlaying.value = false
        _isLoading.value = false
        _isBuffering.value = false
        _playingSoundId.value = null
        activeSoundId = null
    }

    fun stop() {
        synchronized(playerLock) { stopInternal() }
    }
}
