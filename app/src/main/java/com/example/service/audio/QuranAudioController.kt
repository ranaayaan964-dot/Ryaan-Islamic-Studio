package com.example.service.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.example.data.quran.model.AyahEntity
import com.example.data.quran.repository.HifzRepeatMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object QuranAudioController {

    private var audioService: QuranAudioService? = null
    private var isBound = false
    private val controllerScope = CoroutineScope(Dispatchers.Main + Job())

    private val _serviceConnectedFlow = MutableStateFlow(false)
    val serviceConnectedFlow: StateFlow<Boolean> = _serviceConnectedFlow.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _currentDurationMs = MutableStateFlow(0L)
    val currentDurationMs: StateFlow<Long> = _currentDurationMs.asStateFlow()

    private val _activeSurahId = MutableStateFlow(1)
    val activeSurahId: StateFlow<Int> = _activeSurahId.asStateFlow()

    private val _activeSurahName = MutableStateFlow("Al-Fatihah")
    val activeSurahName: StateFlow<String> = _activeSurahName.asStateFlow()

    private val _activeQari = MutableStateFlow(QariId.ALAFASY)
    val activeQari: StateFlow<QariId> = _activeQari.asStateFlow()

    private val _isOfflineAudio = MutableStateFlow(false)
    val isOfflineAudio: StateFlow<Boolean> = _isOfflineAudio.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(HifzRepeatMode.OFF)
    val repeatMode: StateFlow<HifzRepeatMode> = _repeatMode.asStateFlow()

    private val _loopingAyah = MutableStateFlow<AyahEntity?>(null)
    val loopingAyah: StateFlow<AyahEntity?> = _loopingAyah.asStateFlow()

    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()

    private var syncJob: Job? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? QuranAudioService.QuranAudioBinder
            val svc = binder?.getService()
            audioService = svc
            isBound = true
            _serviceConnectedFlow.value = true

            if (svc != null) {
                syncJob?.cancel()
                syncJob = controllerScope.launch {
                    launch { svc.isPlayingFlow.collect { _isPlaying.value = it } }
                    launch { svc.currentPositionMsFlow.collect { _currentPositionMs.value = it } }
                    launch { svc.currentDurationMsFlow.collect { _currentDurationMs.value = it } }
                    launch { svc.activeSurahIdFlow.collect { _activeSurahId.value = it } }
                    launch { svc.activeSurahNameFlow.collect { _activeSurahName.value = it } }
                    launch { svc.activeQariFlow.collect { _activeQari.value = it } }
                    launch { svc.isOfflineAudioFlow.collect { _isOfflineAudio.value = it } }
                    launch { svc.playbackSpeedFlow.collect { _playbackSpeed.value = it } }
                    launch { svc.repeatModeFlow.collect { _repeatMode.value = it } }
                    launch { svc.loopingAyahFlow.collect { _loopingAyah.value = it } }
                    launch { svc.sleepTimerSecondsFlow.collect { _sleepTimerSeconds.value = it } }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            syncJob?.cancel()
            syncJob = null
            audioService = null
            isBound = false
            _serviceConnectedFlow.value = false
            _isPlaying.value = false
        }
    }

    fun bindService(context: Context) {
        if (!isBound) {
            val intent = Intent(context, QuranAudioService::class.java)
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                context.startService(intent)
            }
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun playSurah(context: Context, surahId: Int, surahName: String, audioUrl: String, startPositionMs: Long = 0L) {
        bindService(context)
        audioService?.playSurah(surahId, surahName, audioUrl, startPositionMs)
    }

    fun playSurahWithQari(context: Context, surahId: Int, surahName: String, qariId: QariId, startPositionMs: Long = 0L) {
        bindService(context)
        audioService?.playSurahWithQari(surahId, surahName, qariId, startPositionMs)
    }

    fun playAyahLoop(
        context: Context,
        surahId: Int,
        surahName: String,
        qariId: QariId,
        ayah: AyahEntity,
        repeatMode: HifzRepeatMode
    ) {
        bindService(context)
        audioService?.playAyahLoop(surahId, surahName, qariId, ayah, repeatMode)
    }

    fun setPlaybackSpeed(speed: Float) {
        audioService?.setPlaybackSpeed(speed)
    }

    fun setRepeatMode(mode: HifzRepeatMode, ayah: AyahEntity? = null) {
        audioService?.setRepeatMode(mode, ayah)
    }

    fun setSleepTimer(minutes: Int) {
        audioService?.setSleepTimer(minutes)
    }

    fun togglePlayPause(context: Context) {
        bindService(context)
        audioService?.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        audioService?.seekTo(positionMs)
    }

    fun stopAudio() {
        audioService?.stopAudio()
    }

    fun unbindService(context: Context) {
        if (isBound) {
            try {
                context.unbindService(connection)
            } catch (_: Exception) {}
            syncJob?.cancel()
            syncJob = null
            isBound = false
            audioService = null
            _serviceConnectedFlow.value = false
        }
    }
}
