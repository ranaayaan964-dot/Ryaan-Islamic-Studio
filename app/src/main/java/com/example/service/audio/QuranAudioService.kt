package com.example.service.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.MainActivity
import com.example.R
import com.example.data.quran.model.AyahEntity
import com.example.data.quran.repository.HifzRepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class QuranAudioService : Service() {

    private val binder = QuranAudioBinder()
    private var exoPlayer: ExoPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackerJob: Job? = null

    // State flows for UI sync
    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow.asStateFlow()

    private val _currentPositionMsFlow = MutableStateFlow(0L)
    val currentPositionMsFlow: StateFlow<Long> = _currentPositionMsFlow.asStateFlow()

    private val _currentDurationMsFlow = MutableStateFlow(0L)
    val currentDurationMsFlow: StateFlow<Long> = _currentDurationMsFlow.asStateFlow()

    private val _activeSurahIdFlow = MutableStateFlow(1)
    val activeSurahIdFlow: StateFlow<Int> = _activeSurahIdFlow.asStateFlow()

    private val _activeSurahNameFlow = MutableStateFlow("Al-Fatihah")
    val activeSurahNameFlow: StateFlow<String> = _activeSurahNameFlow.asStateFlow()

    private val _activeQariFlow = MutableStateFlow(QariId.ALAFASY)
    val activeQariFlow: StateFlow<QariId> = _activeQariFlow.asStateFlow()

    private val _isOfflineAudioFlow = MutableStateFlow(false)
    val isOfflineAudioFlow: StateFlow<Boolean> = _isOfflineAudioFlow.asStateFlow()

    private val _playbackSpeedFlow = MutableStateFlow(1.0f)
    val playbackSpeedFlow: StateFlow<Float> = _playbackSpeedFlow.asStateFlow()

    private val _repeatModeFlow = MutableStateFlow(HifzRepeatMode.OFF)
    val repeatModeFlow: StateFlow<HifzRepeatMode> = _repeatModeFlow.asStateFlow()

    private val _loopingAyahFlow = MutableStateFlow<AyahEntity?>(null)
    val loopingAyahFlow: StateFlow<AyahEntity?> = _loopingAyahFlow.asStateFlow()

    private val _sleepTimerSecondsFlow = MutableStateFlow<Int?>(null)
    val sleepTimerSecondsFlow: StateFlow<Int?> = _sleepTimerSecondsFlow.asStateFlow()

    private var sleepTimerJob: Job? = null
    private var stopOnSurahEnd = false

    private var remainingLoopCount = 0

    inner class QuranAudioBinder : Binder() {
        fun getService(): QuranAudioService = this@QuranAudioService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializePlayer()
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlayingFlow.value = isPlaying
                    updateNotification()
                    if (isPlaying) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            _currentDurationMsFlow.value = duration.coerceAtLeast(0L)
                            // Restore playback speed
                            exoPlayer?.setPlaybackSpeed(_playbackSpeedFlow.value)
                        }
                        Player.STATE_ENDED -> {
                            _isPlayingFlow.value = false
                            _currentPositionMsFlow.value = 0L
                            stopProgressTracker()
                            if (stopOnSurahEnd) {
                                stopAudio()
                                stopOnSurahEnd = false
                                _sleepTimerSecondsFlow.value = null
                            }
                        }
                        else -> Unit
                    }
                }
            })
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        if (minutes == 0) {
            stopOnSurahEnd = false
            _sleepTimerSecondsFlow.value = null
            return
        }
        if (minutes == -1) {
            stopOnSurahEnd = true
            _sleepTimerSecondsFlow.value = -1 // Indicates end-of-surah
            return
        }
        stopOnSurahEnd = false
        var remaining = minutes * 60
        _sleepTimerSecondsFlow.value = remaining
        sleepTimerJob = serviceScope.launch {
            while (remaining > 0) {
                delay(1000L)
                remaining--
                _sleepTimerSecondsFlow.value = remaining
            }
            stopAudio()
            _sleepTimerSecondsFlow.value = null
        }
    }

    fun playSurah(
        surahId: Int,
        surahName: String,
        audioUrl: String,
        startPositionMs: Long = 0L
    ) {
        _activeSurahIdFlow.value = surahId
        _activeSurahNameFlow.value = surahName

        val qari = _activeQariFlow.value
        val isDownloaded = QuranDownloadManager.isSurahDownloaded(this, qari, surahId)
        _isOfflineAudioFlow.value = isDownloaded

        val mediaItem = if (isDownloaded) {
            val file = QuranDownloadManager.getLocalSurahFile(this, qari, surahId)
            MediaItem.fromUri(Uri.fromFile(file))
        } else {
            MediaItem.fromUri(audioUrl.ifEmpty { QariCatalog.getSurahAudioUrl(qari, surahId) })
        }

        exoPlayer?.let { player ->
            player.setMediaItem(mediaItem)
            player.prepare()
            player.setPlaybackSpeed(_playbackSpeedFlow.value)
            if (startPositionMs > 0L) {
                player.seekTo(startPositionMs)
            }
            player.play()
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    fun playSurahWithQari(
        surahId: Int,
        surahName: String,
        qariId: QariId,
        startPositionMs: Long = 0L
    ) {
        _activeQariFlow.value = qariId
        val audioUrl = QariCatalog.getSurahAudioUrl(qariId, surahId)
        playSurah(surahId, surahName, audioUrl, startPositionMs)
    }

    fun playAyahLoop(
        surahId: Int,
        surahName: String,
        qariId: QariId,
        ayah: AyahEntity,
        repeatMode: HifzRepeatMode
    ) {
        _activeQariFlow.value = qariId
        _repeatModeFlow.value = repeatMode
        _loopingAyahFlow.value = ayah
        remainingLoopCount = if (repeatMode.loopCount == -1) Int.MAX_VALUE else repeatMode.loopCount

        val currentSurah = _activeSurahIdFlow.value
        if (currentSurah != surahId || exoPlayer?.playbackState == Player.STATE_IDLE) {
            playSurahWithQari(surahId, surahName, qariId, ayah.audioStartTimeMs)
        } else {
            seekTo(ayah.audioStartTimeMs)
            if (exoPlayer?.isPlaying != true) {
                exoPlayer?.play()
            }
        }
    }

    fun setRepeatMode(mode: HifzRepeatMode, ayah: AyahEntity? = null) {
        _repeatModeFlow.value = mode
        _loopingAyahFlow.value = ayah
        remainingLoopCount = if (mode.loopCount == -1) Int.MAX_VALUE else mode.loopCount
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _playbackSpeedFlow.value = clamped
        exoPlayer?.setPlaybackSpeed(clamped)
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPositionMsFlow.value = positionMs
    }

    fun stopAudio() {
        exoPlayer?.stop()
        _isPlayingFlow.value = false
        _loopingAyahFlow.value = null
        stopProgressTracker()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun startProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = serviceScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val currentPos = player.currentPosition
                        _currentPositionMsFlow.value = currentPos
                        _currentDurationMsFlow.value = player.duration.coerceAtLeast(0L)

                        // Check Hifz Repeat loop boundary
                        val loopingAyah = _loopingAyahFlow.value
                        val repeatMode = _repeatModeFlow.value
                        if (loopingAyah != null && repeatMode != HifzRepeatMode.OFF) {
                            if (loopingAyah.audioEndTimeMs > 0 && currentPos >= loopingAyah.audioEndTimeMs) {
                                if (remainingLoopCount > 0) {
                                    if (repeatMode != HifzRepeatMode.REPEAT_INFINITE) {
                                        remainingLoopCount--
                                    }
                                    player.seekTo(loopingAyah.audioStartTimeMs)
                                } else {
                                    // Finished configured repetitions
                                    _repeatModeFlow.value = HifzRepeatMode.OFF
                                    _loopingAyahFlow.value = null
                                }
                            }
                        }
                    }
                }
                delay(100L) // 100ms precision for word-by-word timestamp highlighting
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackerJob?.cancel()
        progressTrackerJob = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quran Audio Recitations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Multi-Qari Quran Audio Recitations"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val qariProfile = QariCatalog.getQariById(_activeQariFlow.value)
        val offlineTag = if (_isOfflineAudioFlow.value) " [Offline MP3]" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Surah ${_activeSurahNameFlow.value}$offlineTag")
            .setContentText("Qari: ${qariProfile.nameEnglish} • Noble Quran")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(_isPlayingFlow.value)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        stopProgressTracker()
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "quran_audio_channel"
        const val NOTIFICATION_ID = 2001
    }
}
