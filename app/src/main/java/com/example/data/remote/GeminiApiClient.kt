package com.example.data.remote

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response DTOs using Moshi ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float? = 0.7f,
    val topP: Float? = 0.95f,
    val topK: Int? = 40,
    val maxOutputTokens: Int? = 1000
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

/**
 * Real Gemini AI Client for Islamic Scholar AI Chat and Voice.
 * Uses official recommended model 'gemini-3.5-flash'.
 */
object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Dedicated Islamic Scholar System Prompt with authentic Quran & Hadith citations
    private val islamicScholarSystemInstruction = GeminiContent(
        parts = listOf(
            GeminiPart(
                text = """
                    You are 'Noor', an authentic, highly knowledgeable, gentle, and empathetic Islamic AI Scholar & Companion.
                    Your objectives:
                    1. Provide spiritually enriching, accurate, and moderate Islamic guidance based on the Holy Quran, authentic Sunnah (Sahih Bukhari, Sahih Muslim, Sunan Abi Dawud, At-Tirmidhi, etc.), and well-established consensus (Ijma).
                    2. Maintain a warm, welcoming, respectful, and respectful Islamic demeanor. Begin answers with 'As-salamu alaykum' or 'Bismillah' where appropriate.
                    3. Clearly cite Ayahs (e.g., [Surah Al-Baqarah 2:186]) and Hadith collections whenever making specific claims.
                    4. Accommodate questions in English, Urdu, or Arabic seamlessly.
                    5. Distinguish between obligatory matters (Fard/Wajib), recommended (Mustahabb), permissible (Mubah), disliked (Makruh), and forbidden (Haram).
                    6. Keep responses clear, concise, and beautifully structured with bullet points or short paragraphs for mobile reading.
                """.trimIndent()
            )
        )
    )

    /**
     * Sends conversation to Gemini 3.5 Flash and returns the scholar response.
     */
    suspend fun chat(
        conversationHistory: List<Pair<String, Boolean>>, // text to isUser
        prompt: String,
        modelName: String = "gemini-1.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API key is not configured in Secrets panel.")
                )
            }

            val contents = mutableListOf<GeminiContent>()

            // Add recent context (up to last 6 messages to preserve context token budget)
            val recentContext = conversationHistory.takeLast(6)
            for ((text, isUser) in recentContext) {
                contents.add(
                    GeminiContent(
                        role = if (isUser) "user" else "model",
                        parts = listOf(GeminiPart(text = text))
                    )
                )
            }

            // Current user prompt
            contents.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = prompt))
                )
            )

            val geminiRequest = GeminiRequest(
                contents = contents,
                systemInstruction = islamicScholarSystemInstruction,
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.7f,
                    topP = 0.95f,
                    topK = 40,
                    maxOutputTokens = 1200
                )
            )

            val jsonBody = requestAdapter.toJson(geminiRequest)
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())

            val activeModel = if (modelName.isNotBlank()) modelName else MODEL_NAME
            val url = "$BASE_URL/$activeModel:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseString = response.body?.string()

            if (!response.isSuccessful || responseString == null) {
                Log.e(TAG, "Gemini API failed code=${response.code}: $responseString")
                return@withContext Result.failure(
                    Exception("Gemini API error (HTTP ${response.code}): ${responseString ?: "empty body"}")
                )
            }

            val parsedResponse = responseAdapter.fromJson(responseString)
            val replyText = parsedResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (replyText.isNullOrBlank()) {
                val errorMsg = parsedResponse?.error?.message ?: "No response received from Gemini AI."
                return@withContext Result.failure(Exception(errorMsg))
            }

            Result.success(replyText.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Error in GeminiApiClient.chat: ${e.message}", e)
            Result.failure(e)
        }
    }
}

/**
 * Real-time Islamic AI Voice Synthesis Engine using Android TextToSpeech.
 * Provides voice reading of AI responses and Quranic/Arabic verses with pitch & speech rate tuning.
 */
class IslamicVoiceEngine(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    var isSpeaking = false
        private set

    var onSpeakingStateChanged: ((Boolean) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.apply {
                    // Try Arabic/UK/US high-quality serene voice
                    val preferredLocales = listOf(
                        Locale("ar"),
                        Locale("en", "GB"),
                        Locale("en", "US"),
                        Locale.getDefault()
                    )
                    for (loc in preferredLocales) {
                        if (isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE) {
                            language = loc
                            break
                        }
                    }
                    setPitch(0.96f) // Soothing, calm scholar tone
                    setSpeechRate(0.92f) // Gentle, clear articulation
                }

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                        onSpeakingStateChanged?.invoke(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                        onSpeakingStateChanged?.invoke(false)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        onSpeakingStateChanged?.invoke(false)
                    }
                })
            }
        }
    }

    fun speak(text: String, utteranceId: String = "ai_voice_${System.currentTimeMillis()}") {
        if (!isInitialized || tts == null) return

        // Clean markdown stars/bullets for clean audio reading
        val cleanSpeech = text
            .replace(Regex("[#*`_~]"), "")
            .replace(Regex("📖\\s*"), "")
            .replace(Regex("\\n+"), ". ")
            .trim()

        stop()
        tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (isSpeaking) {
            tts?.stop()
            isSpeaking = false
            onSpeakingStateChanged?.invoke(false)
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
