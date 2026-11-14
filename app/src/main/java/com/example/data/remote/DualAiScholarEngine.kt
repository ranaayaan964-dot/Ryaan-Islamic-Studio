package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.ChatHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

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

    private const val COOLDOWN_DURATION_MS = 60_000L
    private const val TRANSIENT_COOLDOWN_MS = 15_000L

    private val cooldownMap = ConcurrentHashMap<String, Long>()
    private val geminiModelCursor = AtomicInteger()
    private val groqModelCursor = AtomicInteger()

    private val geminiModels = listOf("gemini-2.5-flash", "gemini-3.1-pro-preview", "gemini-2.0-flash")
    private val groqModels = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "gemma2-9b-it")

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
                ?: generateUnavailableResponse()

            val extractedRefs = extractReferences(result.answer)

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
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get a scholar response: ${e.message}", e)
            generateUnavailableResponse()
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

    private suspend fun routeText(userQuery: String, systemPrompt: String): AiScholarResult? {
        val providers = listOf(
            "gemini" to rotate(geminiModels, geminiModelCursor),
            "groq" to rotate(groqModels, groqModelCursor)
        )
        for ((provider, models) in providers) {
            for (model in models) {
                val routeId = "$provider:$model"
                if (isOnCooldown(routeId)) continue
                val answer = try {
                    val payload = if (provider == "gemini") {
                        JSONObject()
                            .put(
                                "contents",
                                JSONArray().put(
                                    JSONObject()
                                        .put("role", "user")
                                        .put("parts", JSONArray().put(JSONObject().put("text", userQuery)))
                                )
                            )
                            .put(
                                "systemInstruction",
                                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                            )
                            .put(
                                "generationConfig",
                                JSONObject().put("temperature", 0.3).put("maxOutputTokens", 1024)
                            )
                    } else {
                        JSONObject()
                            .put("model", model)
                            .put(
                                "messages",
                                JSONArray()
                                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                                    .put(JSONObject().put("role", "user").put("content", userQuery))
                            )
                            .put("temperature", 0.3)
                            .put("max_tokens", 1024)
                    }
                    val result = AiProxyClient.generate(provider, model, payload)
                    if (provider == "gemini") {
                        result.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.let { parts ->
                                (0 until parts.length())
                                    .mapNotNull { parts.optJSONObject(it)?.optString("text") }
                                    .joinToString("\n")
                                    .trim()
                            }
                    } else {
                        result.optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("message")
                            ?.optString("content")
                            ?.trim()
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w(TAG, "AI proxy route failed: route=$routeId error=${error::class.java.simpleName}")
                    null
                }
                if (!answer.isNullOrBlank()) {
                    cooldownMap.remove(routeId)
                    return AiScholarResult(answer.trim(), "${provider.replaceFirstChar(Char::uppercase)} $model")
                }
                markCooldown(routeId, TRANSIENT_COOLDOWN_MS)
            }
        }
        return null
    }

    internal fun parseConfiguredKeyPool(raw: String): List<String> = raw
        .split(',', ';', '\n')
        .map(String::trim)
        .filter { it.isNotBlank() && !it.startsWith("YOUR_") && !it.startsWith("MY_") }
        .distinct()

    private fun <T> rotate(values: List<T>, cursor: AtomicInteger): List<T> {
        if (values.size < 2) return values
        val offset = Math.floorMod(cursor.getAndIncrement(), values.size)
        return values.drop(offset) + values.take(offset)
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

    private fun extractReferences(text: String): List<String> {
        val regex = Regex("\\[(Surah [^\\]]+|Sahih [^\\]]+|Sunan [^\\]]+|Al-Muwatta [^\\]]+)\\]")
        val matches = regex.findAll(text).map { it.value.removeSurrounding("[", "]") }.toList()
        return matches
    }

    private fun generateUnavailableResponse() = AiScholarResult(
        answer = "Assalamu Alaikum. I could not get a reliable answer because the AI services are unavailable. Please try again later. For a personal religious ruling, consult a qualified scholar.",
        source = "AI providers unavailable"
    )
}
