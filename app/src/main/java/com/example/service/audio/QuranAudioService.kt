package com.example.service.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.MainActivity
import com.example.R
import com.example.data.quran.api.QuranJsonApi
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

/**
 * PRODUCTION-GRADE BACKGROUND AUDIO PLAYER SERVICE (Media3 ExoPlayer)
 *
 * Dedicated foreground service streaming high-quality Quran recitations across all 114 Surahs
 * with multi-Qari support (Mishary Alafasy, Abdul Basit, Al-Muaiqly, Al-Ghamdi, Al-Minshawi).
 *
 * Compliant with Android 14/15 background execution & mediaPlayback foreground service restrictions:
 * - Proper speech/media AudioAttributes with automatic audio focus handling.
 * - Handles audio becoming noisy (pauses when headphones unplugged).
 * - Rich media notification with PendingIntent transport actions (Play/Pause, Seek +/-10s, Stop).
 * - Sub-100ms precision progress tracker for real-time Compose slider synchronization.
 * - Automatic repeat modes, speed scaling, and sleep timer shutdown.
 */
class QuranAudioService : Service() {

    private val binder = QuranAudioBinder()
    private var exoPlayer: ExoPlayer? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressTrackerJob: Job? = null

    // State flows for real-time Compose UI sync
    private val _isPlayingFlow = MutableStateFlow(false)
    val isPlayingFlow: StateFlow<Boolean> = _isPlayingFlow.asStateFlow()

    private val _isBufferingFlow = MutableStateFlow(false)
    val isBufferingFlow: StateFlow<Boolean> = _isBufferingFlow.asStateFlow()

    private val _currentPositionMsFlow = MutableStateFlow(0L)
    val currentPositionMsFlow: StateFlow<Long> = _currentPositionMsFlow.asStateFlow()

    private val _currentDurationMsFlow = MutableStateFlow(0L)
    val currentDurationMsFlow: StateFlow<Long> = _currentDurationMsFlow.asStateFlow()

    private val _activeSurahIdFlow = MutableStateFlow(1)
    val activeSurahIdFlow: StateFlow<Int> = _activeSurahIdFlow.asStateFlow()

    private val _activeSurahNameFlow = MutableStateFlow("Al-Fatihah")
    val activeSurahNameFlow: StateFlow<String> = _activeSurahNameFlow.asStateFlow()

    private val _activeSurahArabicFlow = MutableStateFlow("سُورَةُ ٱلْفَاتِحَةِ")
    val activeSurahArabicFlow: StateFlow<String> = _activeSurahArabicFlow.asStateFlow()

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> resumeAudio()
            ACTION_PAUSE -> pauseAudio()
            ACTION_TOGGLE_PLAY -> togglePlayPause()
            ACTION_STOP -> stopAudio()
            ACTION_SEEK_FORWARD -> seekForward(10000L)
            ACTION_SEEK_BACKWARD -> seekBackward(10000L)
            ACTION_NEXT_SURAH -> playNextSurah()
            ACTION_PREV_SURAH -> playPreviousSurah()
            ACTION_PLAY_SURAH -> {
                val surahId = intent.getIntExtra(EXTRA_SURAH_ID, 1)
                val surahName = intent.getStringExtra(EXTRA_SURAH_NAME) ?: "Al-Fatihah"
                val qariIdName = intent.getStringExtra(EXTRA_QARI_ID) ?: QariId.ALAFASY.name
                val qariId = try { QariId.valueOf(qariIdName) } catch (_: Exception) { QariId.ALAFASY }
                val startPos = intent.getLongExtra(EXTRA_START_POS, 0L)
                playSurahWithQari(surahId, surahName, qariId, startPos)
            }
        }
        return START_NOT_STICKY
    }

    private fun initializePlayer() {
        // Set speech/media AudioAttributes with system audio focus handling
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
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
                            Player.STATE_BUFFERING -> {
                                _isBufferingFlow.value = true
                            }
                            Player.STATE_READY -> {
                                _isBufferingFlow.value = false
                                _currentDurationMsFlow.value = duration.coerceAtLeast(0L)
                                exoPlayer?.setPlaybackSpeed(_playbackSpeedFlow.value)
                                updateNotification()
                            }
                            Player.STATE_ENDED -> {
                                _isBufferingFlow.value = false
                                _isPlayingFlow.value = false
                                _currentPositionMsFlow.value = 0L
                                stopProgressTracker()

                                if (stopOnSurahEnd) {
                                    stopAudio()
                                    stopOnSurahEnd = false
                                    _sleepTimerSecondsFlow.value = null
                                } else if (_repeatModeFlow.value == HifzRepeatMode.OFF) {
                                    // Automatic progression to next Surah in Quran
                                    playNextSurah()
                                }
                            }
                            Player.STATE_IDLE -> {
                                _isBufferingFlow.value = false
                            }
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
        val clampedSurahId = surahId.coerceIn(1, 114)
        _activeSurahIdFlow.value = clampedSurahId
        _activeSurahNameFlow.value = surahName

        // Resolve Arabic Title
        val surahInfo = QuranJsonApi.ALL_114_SURAHS.firstOrNull { it.number == clampedSurahId }
        if (surahInfo != null) {
            _activeSurahArabicFlow.value = surahInfo.name
        }

        val qari = _activeQariFlow.value
        val isDownloaded = QuranDownloadManager.isSurahDownloaded(this, qari, clampedSurahId)
        _isOfflineAudioFlow.value = isDownloaded

        val qariProfile = QariCatalog.getQariById(qari)
        val finalUrl = if (audioUrl.isNotBlank()) audioUrl else QariCatalog.getSurahAudioUrl(qari, clampedSurahId)

        val mediaMetadata = MediaMetadata.Builder()
            .setTitle("Surah $surahName")
            .setArtist(qariProfile.nameEnglish)
            .setAlbumTitle("Noble Quran Recitation")
            .setDisplayTitle("Surah $surahName")
            .setSubtitle("${qariProfile.nameArabic} • ${qariProfile.title}")
            .build()

        val mediaItem = if (isDownloaded) {
            val file = QuranDownloadManager.getLocalSurahFile(this, qari, clampedSurahId)
            MediaItem.Builder()
                .setUri(Uri.fromFile(file))
                .setMediaMetadata(mediaMetadata)
                .build()
        } else {
            MediaItem.Builder()
                .setUri(Uri.parse(finalUrl))
                .setMediaMetadata(mediaMetadata)
                .build()
        }

        exoPlayer?.let { player ->
            player.stop()
            player.setMediaItem(mediaItem)
            player.prepare()
            player.setPlaybackSpeed(_playbackSpeedFlow.value)
            if (startPositionMs > 0L) {
                player.seekTo(startPositionMs)
            }
            player.play()

            startServiceForeground()
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

    fun playNextSurah() {
        val nextId = (_activeSurahIdFlow.value % 114) + 1
        val nextInfo = QuranJsonApi.ALL_114_SURAHS.firstOrNull { it.number == nextId }
        val name = nextInfo?.englishName ?: "Surah $nextId"
        playSurahWithQari(nextId, name, _activeQariFlow.value)
    }

    fun playPreviousSurah() {
        val prevId = if (_activeSurahIdFlow.value > 1) _activeSurahIdFlow.value - 1 else 114
        val prevInfo = QuranJsonApi.ALL_114_SURAHS.firstOrNull { it.number == prevId }
        val name = prevInfo?.englishName ?: "Surah $prevId"
        playSurahWithQari(prevId, name, _activeQariFlow.value)
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

    fun pauseAudio() {
        exoPlayer?.pause()
    }

    fun resumeAudio() {
        exoPlayer?.play()
    }

    fun seekTo(positionMs: Long) {
        val safePos = positionMs.coerceIn(0L, _currentDurationMsFlow.value.coerceAtLeast(0L))
        exoPlayer?.seekTo(safePos)
        _currentPositionMsFlow.value = safePos
    }

    fun seekForward(deltaMs: Long = 10000L) {
        val current = _currentPositionMsFlow.value
        val dur = _currentDurationMsFlow.value
        val target = (current + deltaMs).coerceAtMost(dur)
        seekTo(target)
    }

    fun seekBackward(deltaMs: Long = 10000L) {
        val current = _currentPositionMsFlow.value
        val target = (current - deltaMs).coerceAtLeast(0L)
        seekTo(target)
    }

    fun stopAudio() {
        exoPlayer?.stop()
        _isPlayingFlow.value = false
        _isBufferingFlow.value = false
        _currentPositionMsFlow.value = 0L
        _loopingAyahFlow.value = null
        stopProgressTracker()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun startServiceForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
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
                                    _repeatModeFlow.value = HifzRepeatMode.OFF
                                    _loopingAyahFlow.value = null
                                }
                            }
                        }
                    }
                }
                delay(100L) // 100ms precision for silky-smooth progress scrubbing
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
                "Noble Quran Audio Streaming",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Streaming high-quality Quran recitations with media playback controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Action Intents
        val prevIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_SEEK_BACKWARD }
        val prevPending = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val toggleIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_TOGGLE_PLAY }
        val togglePending = PendingIntent.getService(this, 2, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_SEEK_FORWARD }
        val nextPending = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, QuranAudioService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val qariProfile = QariCatalog.getQariById(_activeQariFlow.value)
        val isOffline = _isOfflineAudioFlow.value
        val offlineTag = if (isOffline) " [Offline MP3]" else ""
        val isPlaying = _isPlayingFlow.value

        val surahNum = String.format(java.util.Locale.US, "%03d", _activeSurahIdFlow.value)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Surah ${_activeSurahNameFlow.value} ($surahNum)$offlineTag")
            .setContentText("${qariProfile.nameEnglish} • Noble Quran")
            .setSubText(_activeSurahArabicFlow.value)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(contentPendingIntent)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Media Action 1: Rewind 10s
            .addAction(android.R.drawable.ic_media_rew, "-10s", prevPending)
            // Media Action 2: Play or Pause
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                togglePending
            )
            // Media Action 3: Forward 10s
            .addAction(android.R.drawable.ic_media_ff, "+10s", nextPending)
            // Media Action 4: Stop
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
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

        const val ACTION_PLAY = "com.example.service.audio.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.service.audio.ACTION_PAUSE"
        const val ACTION_TOGGLE_PLAY = "com.example.service.audio.ACTION_TOGGLE_PLAY"
        const val ACTION_STOP = "com.example.service.audio.ACTION_STOP"
        const val ACTION_SEEK_FORWARD = "com.example.service.audio.ACTION_SEEK_FORWARD"
        const val ACTION_SEEK_BACKWARD = "com.example.service.audio.ACTION_SEEK_BACKWARD"
        const val ACTION_NEXT_SURAH = "com.example.service.audio.ACTION_NEXT_SURAH"
        const val ACTION_PREV_SURAH = "com.example.service.audio.ACTION_PREV_SURAH"
        const val ACTION_PLAY_SURAH = "com.example.service.audio.ACTION_PLAY_SURAH"

        const val EXTRA_SURAH_ID = "extra_surah_id"
        const val EXTRA_SURAH_NAME = "extra_surah_name"
        const val EXTRA_QARI_ID = "extra_qari_id"
        const val EXTRA_START_POS = "extra_start_pos"
    }
}
