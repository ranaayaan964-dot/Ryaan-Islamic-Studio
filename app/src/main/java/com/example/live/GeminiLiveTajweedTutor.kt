package com.example.live

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LiveTajweedState(
    val isConnected: Boolean = false,
    val isStreamingAudio: Boolean = false,
    val isTutorSpeaking: Boolean = false,
    val liveTranscript: String = "Ready to begin recitation...",
    val latestInterruptionNote: String? = null,
    val audioAmplitude: Float = 0f,
    val mistakeCount: Int = 0
)

class GeminiLiveTajweedTutor(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val _state = MutableStateFlow(LiveTajweedState())
    val state: StateFlow<LiveTajweedState> = _state.asStateFlow()

    fun startSession(targetSurah: String = "Surah Al-Fatiha") {
        _state.value = _state.value.copy(
            isConnected = false,
            isStreamingAudio = false,
            isTutorSpeaking = false,
            liveTranscript = "Live streaming is unavailable until a secure server-issued Gemini Live session is configured. Use the recorded Hifz checker to analyze $targetSurah."
        )
    }

    fun stopSession() {
        _state.value = _state.value.copy(
            isConnected = false,
            isStreamingAudio = false,
            isTutorSpeaking = false
        )
    }
}
