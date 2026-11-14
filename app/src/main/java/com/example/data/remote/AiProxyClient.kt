package com.example.data.remote

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object AiProxyClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun geminiKeys(): List<String> =
        DualAiScholarEngine.parseConfiguredKeyPool(BuildConfig.GEMINI_API_KEY)

    private fun groqKeys(): List<String> =
        DualAiScholarEngine.parseConfiguredKeyPool(
            listOf(BuildConfig.GROQ_API_KEY, BuildConfig.GROQ_API_KEY2).joinToString(",")
        )

    suspend fun generate(
        provider: String,
        model: String,
        payload: JSONObject
    ): JSONObject = withContext(Dispatchers.IO) {
        require(provider in SUPPORTED_PROVIDERS) { "Unsupported AI provider." }
        require(model in SUPPORTED_MODELS) { "Unsupported AI model." }

        if (provider == "gemini") {
            generateWithGeminiOrGroqFallback(model, payload)
        } else {
            generateWithGroqDirect(model, payload)
        }
    }

    private fun generateWithGeminiOrGroqFallback(model: String, payload: JSONObject): JSONObject {
        val gKeys = geminiKeys()
        val candidateModels = listOf(
            when (model) {
                "gemini-3.5-flash", "gemini-3.1-flash-lite-preview" -> "gemini-2.5-flash"
                "gemini-3.1-pro-preview" -> "gemini-2.5-flash"
                else -> model
            },
            "gemini-2.5-flash",
            "gemini-2.0-flash"
        ).distinct()

        for (apiKey in gKeys) {
            for (targetModel in candidateModels) {
                val encodedModel = URLEncoder.encode(targetModel, "UTF-8")
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$encodedModel:generateContent"
                val req = Request.Builder()
                    .url(url)
                    .header("x-goog-api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()

                runCatching {
                    httpClient.newCall(req).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (response.isSuccessful && body.isNotBlank()) {
                            return JSONObject(body)
                        }
                    }
                }
            }
        }

        // Automatic fallback to Groq (using GROQ_API_KEY / GROQ_API_KEY2 from GitHub Secrets)
        val groqMessages = convertGeminiPayloadToGroqMessages(payload)
        val groqPayload = JSONObject()
            .put("model", "llama-3.3-70b-versatile")
            .put("messages", groqMessages)
            .put("temperature", 0.3)
            .put("max_tokens", 1500)

        val groqResponse = generateWithGroqDirect("llama-3.3-70b-versatile", groqPayload)
        val replyText = groqResponse.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.trim()
            .orEmpty()

        check(replyText.isNotBlank()) { "AI service returned an empty response." }

        return JSONObject().put(
            "candidates",
            JSONArray().put(
                JSONObject().put(
                    "content",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", replyText))
                    )
                )
            )
        )
    }

    private fun convertGeminiPayloadToGroqMessages(payload: JSONObject): JSONArray {
        val messages = JSONArray()
        val sysParts = payload.optJSONObject("systemInstruction")?.optJSONArray("parts")
        if (sysParts != null && sysParts.length() > 0) {
            val sysText = (0 until sysParts.length())
                .mapNotNull { sysParts.optJSONObject(it)?.optString("text") }
                .joinToString("\n")
                .trim()
            if (sysText.isNotBlank()) {
                messages.put(JSONObject().put("role", "system").put("content", sysText))
            }
        }

        val contents = payload.optJSONArray("contents") ?: JSONArray()
        for (i in 0 until contents.length()) {
            val turn = contents.optJSONObject(i) ?: continue
            val role = if (turn.optString("role") == "model") "assistant" else "user"
            val parts = turn.optJSONArray("parts") ?: continue
            val textBuilder = StringBuilder()
            for (j in 0 until parts.length()) {
                val p = parts.optJSONObject(j) ?: continue
                val t = p.optString("text")
                if (t.isNotBlank()) {
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(t)
                }
            }
            if (textBuilder.isNotEmpty()) {
                messages.put(JSONObject().put("role", role).put("content", textBuilder.toString()))
            }
        }
        return messages
    }

    private fun generateWithGroqDirect(model: String, payload: JSONObject): JSONObject {
        val keys = groqKeys()
        require(keys.isNotEmpty()) { "GROQ_API_KEY is not configured in BuildConfig / GitHub Secrets." }

        var lastError = "Groq request failed"
        for (apiKey in keys) {
            val req = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            try {
                httpClient.newCall(req).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful && body.isNotBlank()) {
                        return JSONObject(body)
                    }
                    lastError = "HTTP ${response.code}: $body"
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Network error"
            }
        }
        error("AI service request failed: $lastError")
    }

    suspend fun transcribeGroqAudio(audioFile: File, mimeType: String): String = withContext(Dispatchers.IO) {
        require(audioFile.isFile && audioFile.length() in 1..MAX_AUDIO_SIZE_BYTES) {
            "Audio file is missing, empty, or larger than the secure transcription limit."
        }
        val keys = groqKeys()
        require(keys.isNotEmpty()) { "GROQ_API_KEY is not configured in BuildConfig / GitHub Secrets." }

        val safeExtension = when (mimeType) {
            "audio/wav" -> "wav"
            "audio/webm" -> "webm"
            "audio/mpeg" -> "mp3"
            "audio/mp4" -> "mp4"
            else -> "m4a"
        }

        var lastError = "Transcription failed"
        for (apiKey in keys) {
            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", "whisper-large-v3")
                .addFormDataPart("language", "ar")
                .addFormDataPart("response_format", "json")
                .addFormDataPart(
                    "file",
                    "recitation.$safeExtension",
                    audioFile.asRequestBody(mimeType.toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/audio/transcriptions")
                .header("Authorization", "Bearer $apiKey")
                .post(multipartBody)
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (response.isSuccessful && body.isNotBlank()) {
                        val text = JSONObject(body).optString("text").trim()
                        if (text.isNotBlank()) return@withContext text
                    }
                    lastError = "HTTP ${response.code}: $body"
                }
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Network error"
            }
        }
        error("Speech transcription failed: $lastError")
    }

    private val SUPPORTED_PROVIDERS = setOf("gemini", "groq")
    private val SUPPORTED_MODELS = setOf(
        "gemini-1.5-flash",
        "gemini-2.5-flash",
        "gemini-3.1-pro-preview",
        "gemini-3.5-flash",
        "gemini-3.1-flash-lite-preview",
        "gemini-2.0-flash",
        "gemini-2.5-flash-lite",
        "gemini-2.5-flash-image",
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "gemma2-9b-it",
        "whisper-large-v3"
    )
    private const val MAX_AUDIO_SIZE_BYTES = 3 * 1024 * 1024L
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
}
