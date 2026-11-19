package com.example.live

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.remote.DualAiScholarEngine
import com.example.util.EvaluatedWord
import com.example.util.QuranRecitationEvaluator
import com.example.util.WordEvaluationStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class LiveTajweedState(
    val isConnected: Boolean = false,
    val isStreamingAudio: Boolean = false,
    val isTutorSpeaking: Boolean = false,
    val liveTranscript: String = "Ready to begin recitation...",
    val latestInterruptionNote: String? = null,
    val audioAmplitude: Float = 0f,
    val mistakeCount: Int = 0,
    val evaluatedWords: List<EvaluatedWord> = emptyList(),
    val accuracyPercentage: Int = 0
)

class GeminiLiveTajweedTutor(context: Context) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val coachingScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(LiveTajweedState())
    val state: StateFlow<LiveTajweedState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var released = false
    private var sessionActive = false
    private var sessionId = 0
    private var coachingRequestId = 0
    private var retryCount = 0
    private var finalTranscript = ""
    private var partialTranscript = ""
    private var currentTargetName = "Surah Al-Fatiha"
    private val reportedMistakeIndices = mutableSetOf<Int>()
    private var pendingGuidance: String? = null

    init {
        initializeTextToSpeech()
    }

    fun startSession(targetSurah: String = "Surah Al-Fatiha") {
        runOnMain {
            if (released) return@runOnMain
            stopRecognizer()
            textToSpeech?.stop()
            coachingRequestId++
            sessionId++
            sessionActive = false
            currentTargetName = targetSurah
            finalTranscript = ""
            partialTranscript = ""
            retryCount = 0
            reportedMistakeIndices.clear()

            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                _state.value = LiveTajweedState(
                    liveTranscript = "Microphone permission is required to begin Arabic recitation."
                )
                return@runOnMain
            }
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                _state.value = LiveTajweedState(
                    liveTranscript = "Arabic speech recognition is unavailable on this device. Enable a speech recognition service and try again."
                )
                return@runOnMain
            }

            _state.value = LiveTajweedState(
                liveTranscript = "Listening for $targetSurah. Begin reciting in Arabic."
            )
            sessionActive = true
            val activeSessionId = sessionId
            try {
                val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
                recognizer = speechRecognizer
                speechRecognizer.setRecognitionListener(createListener(activeSessionId))
                startListening(speechRecognizer, activeSessionId)
            } catch (_: SecurityException) {
                failSession("Microphone permission was denied. Allow microphone access and try again.")
            } catch (_: Exception) {
                failSession("Could not start Arabic speech recognition. Please try again.")
            }
        }
    }

    fun stopSession() {
        runOnMain {
            if (!sessionActive && recognizer == null) return@runOnMain
            val text = currentTranscript()
            if (text.isNotBlank()) updateEvaluation(text, isFinal = true)
            sessionActive = false
            sessionId++
            stopRecognizer()
            textToSpeech?.stop()
            _state.value = _state.value.copy(
                isConnected = false,
                isStreamingAudio = false,
                isTutorSpeaking = false,
                audioAmplitude = 0f,
                liveTranscript = if (text.isBlank()) "Recitation session stopped." else _state.value.liveTranscript
            )
        }
    }

    fun release() {
        runOnMain {
            if (released) return@runOnMain
            stopSession()
            released = true
            coachingScope.cancel()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            ttsReady = false
            pendingGuidance = null
        }
    }

    private fun initializeTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(appContext) { status ->
                runOnMain {
                    if (released) return@runOnMain
                    val engine = textToSpeech ?: return@runOnMain
                    val languageStatus = engine.setLanguage(Locale("ar", "SA"))
                    ttsReady = status == TextToSpeech.SUCCESS &&
                        languageStatus != TextToSpeech.LANG_MISSING_DATA &&
                        languageStatus != TextToSpeech.LANG_NOT_SUPPORTED
                    if (ttsReady) pendingGuidance?.let(::speakGuidance)
                }
            }.also { engine ->
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        runOnMain { _state.value = _state.value.copy(isTutorSpeaking = true) }
                    }

                    override fun onDone(utteranceId: String?) {
                        runOnMain { _state.value = _state.value.copy(isTutorSpeaking = false) }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        runOnMain { _state.value = _state.value.copy(isTutorSpeaking = false) }
                    }
                })
            }
        } catch (_: Exception) {
            ttsReady = false
        }
    }

    private fun createListener(activeSessionId: Int) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (isCurrentSession(activeSessionId)) {
                _state.value = _state.value.copy(
                    isConnected = true,
                    isStreamingAudio = true,
                    audioAmplitude = 0f
                )
            }
        }

        override fun onBeginningOfSpeech() {
            if (isCurrentSession(activeSessionId)) {
                _state.value = _state.value.copy(audioAmplitude = 0.12f)
            }
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (isCurrentSession(activeSessionId)) {
                val amplitude = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _state.value = _state.value.copy(audioAmplitude = amplitude)
            }
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            if (isCurrentSession(activeSessionId)) {
                _state.value = _state.value.copy(audioAmplitude = 0f)
            }
        }

        override fun onError(error: Int) {
            if (!isCurrentSession(activeSessionId)) return
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                partialTranscript = ""
                scheduleListening(activeSessionId, 250L)
            } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                scheduleListening(activeSessionId, 700L)
            } else if (error == SpeechRecognizer.ERROR_NETWORK ||
                error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT
            ) {
                retryCount++
                if (retryCount <= 2) {
                    scheduleListening(activeSessionId, 900L * retryCount)
                } else {
                    failSession("Arabic speech recognition lost its network connection. Check your connection and start again.")
                }
            } else {
                failSession(recognitionErrorMessage(error))
            }
        }

        override fun onResults(results: Bundle?) {
            if (!isCurrentSession(activeSessionId)) return
            val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty().trim()
            if (spoken.isNotBlank()) {
                retryCount = 0
                finalTranscript = listOf(finalTranscript, spoken).filter(String::isNotBlank).joinToString(" ")
                partialTranscript = ""
                updateEvaluation(finalTranscript, isFinal = true)
            }
            scheduleListening(activeSessionId, 250L)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!isCurrentSession(activeSessionId)) return
            partialTranscript = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull().orEmpty().trim()
            if (partialTranscript.isNotBlank()) retryCount = 0
            val text = currentTranscript()
            if (text.isNotBlank()) updateEvaluation(text, isFinal = false)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun startListening(speechRecognizer: SpeechRecognizer, activeSessionId: Int) {
        if (!isCurrentSession(activeSessionId)) return
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            failSession("Microphone permission was revoked. Allow microphone access and try again.")
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }
        try {
            speechRecognizer.startListening(intent)
        } catch (_: SecurityException) {
            failSession("Microphone permission was denied. Allow microphone access and try again.")
        } catch (_: Exception) {
            failSession("Could not continue Arabic speech recognition. Please start a new session.")
        }
    }

    private fun scheduleListening(activeSessionId: Int, delayMillis: Long) {
        mainHandler.postDelayed({
            if (!isCurrentSession(activeSessionId)) return@postDelayed
            val currentRecognizer = recognizer
            if (currentRecognizer == null) {
                failSession("Arabic speech recognition stopped unexpectedly. Please start again.")
            } else {
                startListening(currentRecognizer, activeSessionId)
            }
        }, delayMillis)
    }

    private fun updateEvaluation(text: String, isFinal: Boolean) {
        val targetText = passageFor(currentTargetName)
        val evaluation = QuranRecitationEvaluator.evaluate(targetText, text)
        val lastMatchedIndex = evaluation.words.indexOfLast { it.recognizedMatch != null }
        val visibleWords = if (lastMatchedIndex >= 0) evaluation.words.take(lastMatchedIndex + 1) else emptyList()
        val newMistakes = if (isFinal) {
            visibleWords.withIndex().filter { (index, word) ->
                word.status != WordEvaluationStatus.CORRECT && reportedMistakeIndices.add(index)
            }
        } else {
            emptyList()
        }
        val latestMistake = newMistakes.lastOrNull()
        val updatedTranscript = if (text.isBlank()) {
            "Listening for $currentTargetName. Begin reciting in Arabic."
        } else {
            "$text\n\nLive passage match: ${evaluation.accuracyPercentage}%"
        }

        _state.value = _state.value.copy(
            liveTranscript = updatedTranscript,
            evaluatedWords = visibleWords,
            accuracyPercentage = evaluation.accuracyPercentage,
            mistakeCount = reportedMistakeIndices.size,
            latestInterruptionNote = latestMistake?.let { (_, word) ->
                "Gentle review: check the pronunciation of «${word.originalWord}». ${word.tajweedNote}."
            } ?: if (isFinal && visibleWords.isNotEmpty() &&
                visibleWords.all { it.status == WordEvaluationStatus.CORRECT }
            ) null else _state.value.latestInterruptionNote
        )

        newMistakes.firstOrNull()?.let { (_, word) ->
            speakGuidance(word.originalWord)
            requestAiCoaching(word, text, sessionId)
        }
    }

    private fun requestAiCoaching(word: EvaluatedWord, transcript: String, activeSessionId: Int) {
        val requestId = ++coachingRequestId
        val targetName = currentTargetName
        coachingScope.launch {
            try {
                val result = DualAiScholarEngine.askScholarWithSource(
                    userQuery = "A learner is practicing $targetName. The target word is «${word.originalWord}», " +
                        "their speech recognizer matched «${word.recognizedMatch.orEmpty()}», and the text comparison note is " +
                        "«${word.tajweedNote}». Their latest recognized Arabic transcript is «$transcript». " +
                        "Give one brief, gentle Arabic practice tip for repeating this word. " +
                        "Do not invent a Tajweed error or claim to hear audio.",
                    customSystemPrompt = "You are a supportive Quran memorization coach. Base advice only on the supplied " +
                        "text comparison, do not claim acoustic or Tajweed diagnosis from speech recognition, and return " +
                        "one short, kind sentence in Arabic."
                )
                if (result.source in setOf(
                        "Provider failover unavailable",
                        "AI providers unavailable",
                        "Offline Islamic guidance"
                    ) || result.answer.contains("HTTP 404", ignoreCase = true)
                ) return@launch
                val tip = result.answer.trim().take(240)
                if (tip.isBlank()) return@launch
                mainHandler.post {
                    if (released || !sessionActive || sessionId != activeSessionId ||
                        coachingRequestId != requestId
                    ) return@post
                    val currentNote = _state.value.latestInterruptionNote.orEmpty()
                    _state.value = _state.value.copy(
                        latestInterruptionNote = "$currentNote\n\nAI practice tip: $tip"
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(TAG, "Optional live recitation coaching could not be loaded.", error)
            }
        }
    }

    private fun speakGuidance(word: String) {
        if (released) return
        val prompt = "أحسنت، أعد كلمة $word بهدوء، وانتبه إلى مخرج الحرف."
        pendingGuidance = prompt
        if (!ttsReady) return
        pendingGuidance = null
        try {
            textToSpeech?.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, "tajweed-guidance")
        } catch (_: Exception) {
            _state.value = _state.value.copy(isTutorSpeaking = false)
        }
    }

    private fun passageFor(surahName: String): String = when {
        surahName.contains("Ikhlas", ignoreCase = true) ->
            "قُلْ هُوَ اللَّهُ أَحَدٌ • اللَّهُ الصَّمَدُ • لَمْ يَلِدْ وَلَمْ يُولَدْ • وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ"
        surahName.contains("Kursi", ignoreCase = true) ->
            "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ ۗ مَن ذَا الَّذِي يَشْفَعُ عِندَهُ إِلَّا بِإِذْنِهِ ۚ يَعْلَمُ مَا بَيْنَ أَيْدِيهِمْ وَمَا خَلْفَهُمْ ۖ وَلَا يُحِيطُونَ بِشَيْءٍ مِّنْ عِلْمِهِ إِلَّا بِمَا شَاءَ ۚ وَسِعَ كُرْسِيُّهُ السَّمَاوَاتِ وَالْأَرْضَ ۖ وَلَا يَئُودُهُ حِفْظُهُمَا ۚ وَهُوَ الْعَلِيُّ الْعَظِيمُ"
        surahName.contains("Mulk", ignoreCase = true) ->
            "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ • الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا وَهُوَ الْعَزِيزُ الْغَفُورُ • الَّذِي خَلَقَ سَبْعَ سَمَاوَاتٍ طِبَاقًا ۖ مَّا تَرَىٰ فِي خَلْقِ الرَّحْمَٰنِ مِن تَفَاوُتٍ ۖ فَارْجِعِ الْبَصَرَ هَلْ تَرَىٰ مِن فُطُورٍ • ثُمَّ ارْجِعِ الْبَصَرَ كَرَّتَيْنِ يَنقَلِبْ إِلَيْكَ الْبَصَرُ خَاسِئًا وَهُوَ حَسِيرٌ • وَلَقَدْ زَيَّنَّا السَّمَاءَ الدُّنْيَا بِمَصَابِيحَ وَجَعَلْنَاهَا رُجُومًا لِّلشَّيَاطِينِ وَأَعْتَدْنَا لَهُمْ عَذَابَ السَّعِيرِ"
        else ->
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ • الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ • الرَّحْمَٰنِ الرَّحِيمِ • مَالِكِ يَوْمِ الدِّينِ • إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ • اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ • صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ"
    }

    private fun currentTranscript(): String =
        listOf(finalTranscript, partialTranscript).filter(String::isNotBlank).joinToString(" ")

    private fun isCurrentSession(activeSessionId: Int): Boolean =
        !released && sessionActive && activeSessionId == sessionId

    private fun failSession(message: String) {
        sessionActive = false
        sessionId++
        stopRecognizer()
        textToSpeech?.stop()
        _state.value = _state.value.copy(
            isConnected = false,
            isStreamingAudio = false,
            isTutorSpeaking = false,
            audioAmplitude = 0f,
            liveTranscript = message
        )
    }

    private fun stopRecognizer() {
        mainHandler.removeCallbacksAndMessages(null)
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
            // Recognition service may already have released its listener.
        }
        recognizer = null
    }

    private fun recognitionErrorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Microphone permission is required. Allow access and start again."
        SpeechRecognizer.ERROR_AUDIO -> "There was an audio capture problem. Check the microphone and try again."
        SpeechRecognizer.ERROR_CLIENT -> "Arabic speech recognition stopped. Please start a new session."
        SpeechRecognizer.ERROR_SERVER -> "The speech recognition service encountered an error. Please try again."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED ->
            "The installed speech service does not support Arabic (Saudi Arabia)."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_AVAILABLE ->
            "Arabic speech data is not installed for the selected speech service."
        else -> "Speech recognition could not continue (error $error). Please start again."
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post(action)
    }

    companion object {
        private const val TAG = "GeminiLiveTajweedTutor"
    }
}
