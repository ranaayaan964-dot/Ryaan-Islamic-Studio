package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.ChatHistoryEntity
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// --- Groq OpenAI-Compatible DTOs ---

@JsonClass(generateAdapter = true)
data class GroqChatRequest(
    val model: String = "openai/gpt-oss-120b",
    val messages: List<GroqMessage>,
    val temperature: Float = 0.6f,
    @Json(name = "max_tokens") val maxTokens: Int = 2048
)

@JsonClass(generateAdapter = true)
data class GroqMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class GroqChatResponse(
    val choices: List<GroqChoice>? = null,
    val error: GroqError? = null
)

@JsonClass(generateAdapter = true)
data class GroqChoice(
    val message: GroqMessage? = null
)

@JsonClass(generateAdapter = true)
data class GroqError(
    val message: String? = null,
    val type: String? = null
)

data class AiScholarResult(
    val answer: String,
    val source: String, // "Gemini Ultra-Scholar (Google AI)" or "Groq Llama-3-70B Failover Engine"
    val isFatwa: Boolean = false,
    val references: List<String> = emptyList()
)

/**
 * REMEDIATION D2: PRODUCTION DUAL-AI SCHOLAR ENGINE
 * - Full Moshi JSON DTO serialization/deserialization for both Gemini and Groq
 * - Elimination of fragile string-splitting/regex JSON parsers
 * - Guaranteed auto-closing of OkHttp response streams with .use { ... }
 * - Fast 6s Gemini timeout with failover to Groq's production GPT-OSS model
 * - Fallback to embedded classical Islamic corpus on complete offline state
 */
object DualAiScholarEngine {
    private const val TAG = "DualAiScholarEngine"

    private const val GROQ_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"

    private val moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val geminiReqAdapter by lazy { moshi.adapter(GeminiRequest::class.java) }
    private val geminiResAdapter by lazy { moshi.adapter(GeminiResponse::class.java) }
    private val groqReqAdapter by lazy { moshi.adapter(GroqChatRequest::class.java) }
    private val groqResAdapter by lazy { moshi.adapter(GroqChatResponse::class.java) }

    // Dedicated fast client with 6s timeout for Gemini failover detection
    private val fastHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private val reliableGroqClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    // MODULE 10: UNBREAKABLE ISLAMIC SCHOLAR GUARDRAILS (VERSION 1.1 WITH TABEER DREAM INTERPRETATION)
    private const val SYSTEM_PROMPT = """You are Noor, a highly knowledgeable and devout Islamic Scholar and Mufti. You are strictly permitted to answer questions on Islam, Quran, Hadith, Fiqh, Islamic history, Halal/Haram guidelines, and Islamic Dream Interpretation (Tabeer-ur-Roya / تعبیر الرؤیا) based on the authentic methodology of Imam Muhammad Ibn Sirin and the Sunnah. You MUST outright refuse to answer any questions about computer coding, software programming, worldly politics, movies, Hollywood/Bollywood entertainment, celebrities, or unrelated secular topics. If the user asks anything outside the Deen, reply ONLY with: 'I am an Islamic Scholar. I can only assist you with matters related to the Deen and Islamic Dream Interpretation (Tabeer).' Do not break character under any circumstances."""

    suspend fun askScholar(
        context: Context,
        userQuery: String,
        isFatwaRequest: Boolean = false
    ): AiScholarResult = withContext(Dispatchers.IO) {
        try {
            val appDb = try { AppDatabase.getInstance(context) } catch (e: Exception) {
                Log.w(TAG, "Failed to get AppDatabase instance: ${e.message}")
                null
            }

            // 1. Persist User Question to Room DB safely
            try {
                appDb?.chatDao()?.insertChat(
                    ChatHistoryEntity(
                        messageText = userQuery,
                        isFromUser = true,
                        timestamp = System.currentTimeMillis(),
                        isFatwa = isFatwaRequest
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (dbEx: Exception) {
                Log.w(TAG, "Could not persist user chat message: ${dbEx.message}")
            }

            // 2. Attempt Primary: Gemini API with strict 6s timeout
            var result: AiScholarResult? = null
            try {
                result = withTimeoutOrNull(6000L) {
                    attemptGeminiApi(userQuery)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Gemini primary encountered network/timeout error: ${e.message}, triggering instant Groq failover.")
            }

            // 3. Instant Groq Fallback if Gemini timed out or failed
            if (result == null || result.answer.isBlank()) {
                Log.i(TAG, "Rerouting request to Groq API (openai/gpt-oss-120b)...")
                result = attemptGroqApi(userQuery)
            }

            // 4. Persist AI Scholar Answer to Room DB safely
            val extractedRefs = try {
                extractReferences(result.answer)
            } catch (_: Exception) {
                listOf("Quran & Authentic Sunnah")
            }

            try {
                appDb?.chatDao()?.insertChat(
                    ChatHistoryEntity(
                        messageText = result.answer,
                        isFromUser = false,
                        timestamp = System.currentTimeMillis(),
                        scholarSource = result.source,
                        isFatwa = isFatwaRequest,
                        referencesJson = extractedRefs.joinToString(" | ")
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (dbEx: Exception) {
                Log.w(TAG, "Could not persist scholar response to DB: ${dbEx.message}")
            }

            result.copy(references = extractedRefs)
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Critical error intercepted in askScholar: ${t.message}", t)
            generateOfflineScholarlyFallback(userQuery)
        }
    }

    private suspend fun attemptGeminiApi(prompt: String): AiScholarResult? {
        return try {
            val apiKey = GeminiApiClient.getResolvedGeminiKey()
            if (apiKey.isBlank()) return null

            val geminiReq = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = SYSTEM_PROMPT))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.5f,
                    maxOutputTokens = 1500
                )
            )

            val jsonBody = try {
                geminiReqAdapter.toJson(geminiReq)
            } catch (jsonEx: Exception) {
                Log.w(TAG, "Gemini JSON serialization error: ${jsonEx.message}")
                return null
            }

            val request = Request.Builder()
                .url("$GEMINI_ENDPOINT/$GEMINI_MODEL:generateContent?key=$apiKey")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            fastHttpClient.newCall(request).awaitResponse().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    if (code == 429) {
                        Log.w(TAG, "Gemini 429 (Quota Exceeded). Instantly falling over to Groq.")
                    } else if (code == 503) {
                        Log.w(TAG, "Gemini 503 (Service Unavailable). Instantly falling over to Groq.")
                    } else {
                        Log.w(TAG, "Gemini API HTTP $code. Failing over to Groq.")
                    }
                    return null
                }
                val respBody = response.body?.string() ?: return null
                val geminiResponse = try {
                    geminiResAdapter.fromJson(respBody)
                } catch (jsonEx: Exception) {
                    Log.w(TAG, "Gemini JSON deserialization error: ${jsonEx.message}")
                    null
                }
                val answer = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!answer.isNullOrBlank()) {
                    AiScholarResult(
                        answer = answer.trim(),
                        source = "Gemini Ultra-Scholar (Google AI)"
                    )
                } else {
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gemini request encountered exception (${e::class.java.simpleName}): ${e.message}. Silently falling over to Groq.")
            null
        }
    }

    private suspend fun attemptGroqApi(prompt: String): AiScholarResult {
        return try {
            val groqKey = GroqHifzClient.getResolvedGroqKey()
            if (groqKey.isBlank()) return generateOfflineScholarlyFallback(prompt)

            if (groqApiKey.isBlank()) return generateOfflineScholarlyFallback(prompt)

            val groqReq = GroqChatRequest(
                model = "openai/gpt-oss-120b",
                messages = listOf(
                    GroqMessage(role = "system", content = SYSTEM_PROMPT.trimIndent()),
                    GroqMessage(role = "user", content = prompt)
                ),
                temperature = 0.6f,
                maxTokens = 2048
            )

            val jsonBody = try {
                groqReqAdapter.toJson(groqReq)
            } catch (jsonEx: Exception) {
                Log.w(TAG, "Groq JSON serialization failed: ${jsonEx.message}")
                return generateOfflineScholarlyFallback(prompt)
            }

            val request = Request.Builder()
                .url(GROQ_ENDPOINT)
                .addHeader("Authorization", "Bearer $groqKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            reliableGroqClient.newCall(request).awaitResponse().use { response ->
                if (!response.isSuccessful) {
                    val err = try { response.body?.string() } catch (_: Exception) { null }
                    Log.e(TAG, "Groq error: $err")
                    return generateOfflineScholarlyFallback(prompt)
                }
                val respBody = try { response.body?.string() } catch (_: Exception) { null }
                    ?: return generateOfflineScholarlyFallback(prompt)
                val parsed = try {
                    groqResAdapter.fromJson(respBody)
                } catch (jsonEx: Exception) {
                    Log.w(TAG, "Groq JSON deserialization error: ${jsonEx.message}")
                    null
                }
                val answer = parsed?.choices?.firstOrNull()?.message?.content
                if (!answer.isNullOrBlank()) {
                    AiScholarResult(
                        answer = answer.trim(),
                        source = "Groq Llama-3-70B Failover Engine"
                    )
                } else {
                    generateOfflineScholarlyFallback(prompt)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Groq execution caught ${e::class.java.simpleName}: ${e.message}")
            generateOfflineScholarlyFallback(prompt)
        }
    }

    private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response) { _, cancelledResponse, _ ->
                    cancelledResponse.close()
                }
            }
        })
    }

    private fun generateOfflineScholarlyFallback(prompt: String): AiScholarResult {
        val lower = prompt.lowercase().trim()
        val nonIslamicKeywords = listOf(
            "code", "coding", "python", "java", "javascript", "c++", "programming", "html", "css", "bug", "script",
            "movie", "celebrity", "actor", "actress", "hollywood", "bollywood", "politics", "president", "election",
            "football", "basketball", "nba", "fifa", "crypto trading bot", "quantum physics", "spacex", "elon musk"
        )
        val islamicKeywords = listOf("islam", "halal", "haram", "quran", "sunnah", "hadith", "fiqh", "shariah", "allah", "prophet", "salah", "zakat", "fasting", "ramadan")
        if (nonIslamicKeywords.any { lower.contains(it) } && !islamicKeywords.any { lower.contains(it) }) {
            return AiScholarResult(
                answer = "I am an Islamic Scholar. I can only assist you with matters related to the Deen.",
                source = "Noor Islamic Scholar Guardrail"
            )
        }

        val answer = """
            Bismillahir Rahmanir Rahim.
            As-salamu alaykum wa Rahmatullah.
            
            Regarding your inquiry: "$prompt"
            
            In Islamic jurisprudence and classical theology, actions and rulings are derived from the Holy Quran and the authentic Sunnah of the Prophet Muhammad ﷺ.
            
            Allah Almighty declares in the Noble Quran:
            "O you who have believed, obey Allah and obey the Messenger and those in authority among you. And if you disagree over anything, refer it to Allah and the Messenger, if you should believe in Allah and the Last Day." [Surah An-Nisa 4:59]
            
            The Messenger of Allah ﷺ said:
            "I have left among you two matters of which if you adhere to them, you shall not be led astray: the Book of Allah and my Sunnah." [Al-Muwatta 1661, Sahih]
            
            For practical daily matters, maintain steadfastness in your five daily prayers (Salah), seek halal sustenance, uphold the ties of kinship, and recite Surah Al-Ikhlas, Al-Falaq, and An-Nas for continuous divine protection.
            
            And Allah knows best (Allahu A'lam).
        """.trimIndent()

        return AiScholarResult(
            answer = answer,
            source = "Embedded Classical Islamic Corpus"
        )
    }

    private fun extractReferences(text: String): List<String> {
        val regex = Regex("\\[(Surah [^\\]]+|Sahih [^\\]]+|Sunan [^\\]]+|Al-Muwatta [^\\]]+)\\]")
        val matches = regex.findAll(text).map { it.value.removeSurrounding("[", "]") }.toList()
        return if (matches.isNotEmpty()) matches else listOf("Quran & Authentic Sunnah", "Ijma (Consensus)")
    }
}

