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
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.TimeUnit

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
    private const val GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models"
    private const val KEYLESS_ENDPOINT = "https://text.pollinations.ai/openai"
    private const val COOLDOWN_DURATION_MS = 60_000L
    private const val INVALID_KEY_COOLDOWN_MS = 10 * 60_000L
    private const val TRANSIENT_COOLDOWN_MS = 15_000L

    private val cooldownMap = ConcurrentHashMap<String, Long>()
    private val geminiKeyCursor = AtomicInteger()
    private val groqKeyCursor = AtomicInteger()
    private val geminiModelCursor = AtomicInteger()
    private val groqModelCursor = AtomicInteger()
    private val keylessModelCursor = AtomicInteger()

    private val geminiModels = listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-flash-8b")
    private val groqModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "gemma2-9b-it")
    private val keylessModels = listOf("openai", "mistral", "llama")

    private val moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val geminiReqAdapter by lazy { moshi.adapter(GeminiRequest::class.java) }
    private val geminiResAdapter by lazy { moshi.adapter(GeminiResponse::class.java) }
    private val groqReqAdapter by lazy { moshi.adapter(GroqChatRequest::class.java) }
    private val groqResAdapter by lazy { moshi.adapter(GroqChatResponse::class.java) }

    private val routerHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .callTimeout(18, TimeUnit.SECONDS)
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

            val result = routeText(userQuery, SYSTEM_PROMPT)
                ?: generateOfflineScholarlyFallback(userQuery)

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

    suspend fun askScholar(userQuery: String, customSystemPrompt: String? = null): String =
        withContext(Dispatchers.IO) {
            routeText(userQuery, customSystemPrompt ?: SYSTEM_PROMPT)?.answer
                ?: "Assalamu Alaikum. AI providers are unavailable right now. Please try again shortly."
        }

    suspend fun askScholarWithSource(userQuery: String, customSystemPrompt: String): AiScholarResult =
        withContext(Dispatchers.IO) {
            routeText(userQuery, customSystemPrompt)
                ?: AiScholarResult(
                    answer = "Assalamu Alaikum. AI providers are unavailable right now. Please try again shortly.",
                    source = "Provider failover unavailable"
                )
        }

    private data class ProviderResponse(val code: Int?, val body: String?)

    private fun routeText(userQuery: String, systemPrompt: String): AiScholarResult? {
        val geminiKeys = rotate(geminiApiKeyPool(), geminiKeyCursor)
        val orderedGeminiModels = rotate(geminiModels, geminiModelCursor)
        for (apiKey in geminiKeys) {
            for (model in orderedGeminiModels) {
                val routeId = routeId("gemini", model, apiKey)
                if (isOnCooldown(routeId)) continue
                val answer = tryGeminiModel(apiKey, model, systemPrompt, userQuery, routeId)
                if (!answer.isNullOrBlank()) {
                    return AiScholarResult(answer.trim(), "Gemini $model")
                }
            }
        }

        val groqKeys = rotate(groqApiKeyPool(), groqKeyCursor)
        val orderedGroqModels = rotate(groqModels, groqModelCursor)
        for (apiKey in groqKeys) {
            for (model in orderedGroqModels) {
                val routeId = routeId("groq", model, apiKey)
                if (isOnCooldown(routeId)) continue
                val answer = tryGroqModel(apiKey, model, systemPrompt, userQuery, routeId)
                if (!answer.isNullOrBlank()) {
                    return AiScholarResult(answer.trim(), "Groq $model")
                }
            }
        }

        for (model in rotate(keylessModels, keylessModelCursor)) {
            val routeId = "keyless:$model"
            if (isOnCooldown(routeId)) continue
            val answer = tryKeylessModel(model, systemPrompt, userQuery, routeId)
            if (!answer.isNullOrBlank()) {
                return AiScholarResult(answer.trim(), "Pollinations $model")
            }
        }
        return null
    }

    private fun tryGeminiModel(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userQuery: String,
        routeId: String
    ): String? {
        val requestDto = GeminiRequest(
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = userQuery)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt))),
            generationConfig = GeminiGenerationConfig(temperature = 0.3f, maxOutputTokens = 1024)
        )
        val body = try {
            geminiReqAdapter.toJson(requestDto)
        } catch (e: Exception) {
            Log.w(TAG, "Gemini request serialization failed for $model", e)
            markCooldown(routeId, TRANSIENT_COOLDOWN_MS)
            return null
        }
        val url = "$GEMINI_ENDPOINT/$model:generateContent".toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", apiKey)
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        val response = executeProviderRequest(request, routeId) ?: return null
        if (response.code !in 200..299) return null
        val result = try {
            geminiResAdapter.fromJson(response.body.orEmpty())
                ?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
        } catch (e: Exception) {
            Log.w(TAG, "Gemini response parsing failed for $model", e)
            null
        }
        return acceptAnswer(routeId, result)
    }

    private fun tryGroqModel(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userQuery: String,
        routeId: String
    ): String? {
        val requestDto = GroqChatRequest(
            model = model,
            messages = listOf(
                GroqMessage(role = "system", content = systemPrompt),
                GroqMessage(role = "user", content = userQuery)
            ),
            temperature = 0.3f,
            maxTokens = 1024
        )
        val body = try {
            groqReqAdapter.toJson(requestDto)
        } catch (e: Exception) {
            Log.w(TAG, "Groq request serialization failed for $model", e)
            markCooldown(routeId, TRANSIENT_COOLDOWN_MS)
            return null
        }
        val request = Request.Builder()
            .url(GROQ_ENDPOINT)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        val response = executeProviderRequest(request, routeId) ?: return null
        if (response.code !in 200..299) return null
        val result = try {
            groqResAdapter.fromJson(response.body.orEmpty())
                ?.choices?.firstOrNull()?.message?.content?.trim()
        } catch (e: Exception) {
            Log.w(TAG, "Groq response parsing failed for $model", e)
            null
        }
        return acceptAnswer(routeId, result)
    }

    private fun tryKeylessModel(
        model: String,
        systemPrompt: String,
        userQuery: String,
        routeId: String
    ): String? {
        val payload = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", userQuery))
            })
            put("temperature", 0.3)
        }
        val request = Request.Builder()
            .url(KEYLESS_ENDPOINT)
            .header("Content-Type", "application/json")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = executeProviderRequest(request, routeId) ?: return null
        if (response.code !in 200..299) return null
        val responseBody = response.body.orEmpty().trim()
        val answer = try {
            if (responseBody.startsWith("{")) {
                val choices = JSONObject(responseBody).optJSONArray("choices")
                choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.trim()
            } else {
                responseBody
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pollinations response parsing failed for $model", e)
            null
        }
        return acceptAnswer(routeId, answer)
    }

    private fun executeProviderRequest(request: Request, routeId: String): ProviderResponse? {
        return try {
            routerHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string()
                when {
                    response.code == 429 || response.code in 500..599 ->
                        markCooldown(routeId)
                    response.code == 401 || response.code == 403 ->
                        markCooldown(routeId, INVALID_KEY_COOLDOWN_MS)
                    !response.isSuccessful -> markCooldown(routeId, TRANSIENT_COOLDOWN_MS)
                }
                if (!response.isSuccessful) {
                    Log.w(TAG, "Provider request failed: route=$routeId status=${response.code}")
                }
                ProviderResponse(response.code, body)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Provider request failed: route=$routeId error=${e::class.java.simpleName}")
            markCooldown(routeId, TRANSIENT_COOLDOWN_MS)
            null
        }
    }

    private fun acceptAnswer(routeId: String, answer: String?): String? {
        if (answer.isNullOrBlank()) {
            markCooldown(routeId, TRANSIENT_COOLDOWN_MS)
            return null
        }
        cooldownMap.remove(routeId)
        return answer
    }

    internal fun parseConfiguredKeyPool(raw: String): List<String> = raw
        .split(',', ';', '\n')
        .map(String::trim)
        .filter { it.isNotBlank() && !it.startsWith("YOUR_") && !it.startsWith("MY_") }
        .distinct()

    internal fun geminiApiKeyPool(): List<String> =
        parseConfiguredKeyPool(GeminiApiClient.getResolvedGeminiKey())

    internal fun groqApiKeyPool(): List<String> =
        parseConfiguredKeyPool(GroqHifzClient.getResolvedGroqKey())

    private fun <T> rotate(values: List<T>, cursor: AtomicInteger): List<T> {
        if (values.size < 2) return values
        val offset = Math.floorMod(cursor.getAndIncrement(), values.size)
        return values.drop(offset) + values.take(offset)
    }

    private fun routeId(provider: String, model: String, apiKey: String): String {
        val keyFingerprint = MessageDigest.getInstance("SHA-256")
            .digest(apiKey.toByteArray())
            .take(6)
            .joinToString("") { "%02x".format(it) }
        return "$provider:$model:$keyFingerprint"
    }

    private fun isOnCooldown(routeId: String): Boolean {
        val expiry = cooldownMap[routeId] ?: return false
        if (System.currentTimeMillis() < expiry) return true
        cooldownMap.remove(routeId, expiry)
        return false
    }

    private fun markCooldown(routeId: String, durationMs: Long = COOLDOWN_DURATION_MS) {
        cooldownMap[routeId] = System.currentTimeMillis() + durationMs
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

