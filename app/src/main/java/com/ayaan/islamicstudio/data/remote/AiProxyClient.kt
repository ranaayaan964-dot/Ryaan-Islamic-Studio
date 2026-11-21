package com.ayaan.islamicstudio.data.remote

import com.ayaan.islamicstudio.BuildConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
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
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.UnknownHostException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.tasks.await

internal class AiProxyException(
    message: String,
    val httpCode: Int? = null,
    val isTransient: Boolean = false,
    val isOffline: Boolean = false,
    cause: Throwable? = null
) : IOException(message, cause)

object AiProxyClient {
    private const val TAG = "AiProxyClient"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    private val geminiKeyCursor = AtomicInteger()
    private val groqKeyCursor = AtomicInteger()
    private val geminiModelCursor = AtomicInteger()
    private val groqTextModelCursor = AtomicInteger()
    private val groqVisionModelCursor = AtomicInteger()
    private val groqAudioModelCursor = AtomicInteger()

    private fun geminiKeys(): List<String> = emptyList()

    private fun groqKeys(): List<String> = emptyList()

    suspend fun generate(
        provider: String,
        model: String,
        payload: JSONObject
    ): JSONObject = withContext(Dispatchers.IO) {
        require(provider in SUPPORTED_PROVIDERS) { "Unsupported AI provider." }
        val normalizedProvider = if (provider == "gemini") "gemini" else "groq"
        val normalizedModel = when (normalizedProvider) {
            "gemini" -> when (model) {
                "gemini-2.5-flash", "gemini-2.0-flash", "gemini-2.0-flash-lite" -> model
                else -> "gemini-2.5-flash"
            }
            else -> if (model in GROQ_TEXT_MODELS || model in GROQ_VISION_MODELS) model
                else "llama-3.3-70b-versatile"
        }

        try {
            return@withContext generateViaSecureProxy(normalizedProvider, normalizedModel, payload)
        } catch (geminiError: Exception) {
            if (normalizedProvider != "gemini") throw geminiError

            val groqPayload = if (containsImage(payload)) {
                convertGeminiPayloadToGroqVisionPayload(payload)
            } else {
                convertGeminiPayloadToGroqMessages(payload)
                    .let { messages ->
                        JSONObject()
                            .put("model", GROQ_TEXT_MODELS.first())
                            .put("messages", messages)
                            .put("temperature", 0.3)
                            .put("max_tokens", 1500)
                    }
            }
            val groqModel = if (containsImage(payload)) GROQ_VISION_MODELS.first()
                else GROQ_TEXT_MODELS.first()
            try {
                val groqResponse = generateViaSecureProxy("groq", groqModel, groqPayload)
                val replyText = groqResponse.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()
                if (replyText.isBlank()) throw geminiError
                return@withContext JSONObject().put(
                    "candidates",
                    JSONArray().put(
                        JSONObject().put(
                            "content",
                            JSONObject().put("parts", JSONArray().put(JSONObject().put("text", replyText)))
                        )
                    )
                )
            } catch (_: Exception) {
                throw geminiError
            }
        }
    }

    private suspend fun generateViaSecureProxy(
        provider: String,
        model: String,
        payload: JSONObject
    ): JSONObject {
        val proxyUrl = BuildConfig.AI_PROXY_BASE_URL.trim()
        require(proxyUrl.isNotBlank()) { "Secure AI proxy is not configured." }
        val firebaseUser = FirebaseAuth.getInstance().currentUser
            ?: throw AiProxyException("Firebase sign-in is required for AI access.", httpCode = 401)
        val idToken = firebaseUser.getIdToken(true).await().token
            ?: throw AiProxyException("Firebase authentication token is unavailable.", httpCode = 401)
        val appCheckToken = FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
            ?: throw AiProxyException("Firebase App Check token is unavailable.", httpCode = 401)

        val requestPayload = JSONObject()
            .put("provider", provider)
            .put("model", model)
            .put("payload", payload)
        val request = Request.Builder()
            .url(proxyUrl)
            .header("Authorization", "Bearer $idToken")
            .header("X-Firebase-AppCheck", appCheckToken)
            .header("Content-Type", "application/json")
            .post(requestPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw httpFailure(response.code)
            }
            val result = runCatching { JSONObject(body) }.getOrElse {
                throw AiProxyException("AI proxy returned an invalid response.", httpCode = response.code)
            }
            if (provider == "gemini" && !hasGeminiContent(result)) {
                throw AiProxyException("AI proxy returned an empty Gemini response.", httpCode = response.code)
            }
            return result
        }
    }

    private fun generateWithGeminiOrGroqFallback(model: String, payload: JSONObject): JSONObject {
        val keys = rotate(geminiKeys(), geminiKeyCursor)
        val preferredModel = when {
            model in GEMINI_MODELS -> model
            model == "gemini-3.1-pro-preview" || model == "gemini-3.5-flash" ->
                "gemini-2.5-flash"
            model == "gemini-3.1-flash-lite-preview" -> "gemini-2.0-flash-lite"
            else -> "gemini-2.5-flash"
        }
        val models = listOf(preferredModel) + rotate(
            GEMINI_MODELS.filterNot { it == preferredModel },
            geminiModelCursor
        )

        var lastFailure: AiProxyException? = null
        for (apiKey in keys) {
            for (targetModel in models) {
                try {
                    val encodedModel = URLEncoder.encode(targetModel, "UTF-8")
                    val request = Request.Builder()
                        .url("https://generativelanguage.googleapis.com/v1beta/models/$encodedModel:generateContent")
                        .header("x-goog-api-key", apiKey)
                        .header("Content-Type", "application/json")
                        .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (response.isSuccessful) {
                            val result = runCatching { JSONObject(body) }.getOrNull()
                            if (result != null && hasGeminiContent(result)) return result
                            lastFailure = AiProxyException(
                                "Gemini returned an empty or invalid response.",
                                httpCode = response.code
                            )
                        } else {
                            lastFailure = httpFailure(response.code)
                        }
                    }
                } catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    lastFailure = if (error is AiProxyException) error else networkFailure(error)
                }
            }
        }

        val fallbackPayload = JSONObject()
            .put("messages", convertGeminiPayloadToGroqMessages(payload))
            .put("temperature", 0.3)
            .put("max_tokens", 1500)
        val groqModel = if (containsImage(payload)) GROQ_VISION_MODELS.first() else GROQ_TEXT_MODELS.first()
        val groqResponse = generateWithGroqDirect(groqModel, fallbackPayload)
        val replyText = groqResponse.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")
            ?.trim()
            .orEmpty()
        if (replyText.isBlank()) {
            throw lastFailure ?: AiProxyException("AI service returned an empty response.")
        }

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

    private fun convertGeminiPayloadToGroqVisionPayload(payload: JSONObject): JSONObject {
        val messages = JSONArray()
        val systemParts = payload.optJSONObject("systemInstruction")?.optJSONArray("parts")
        if (systemParts != null) {
            val systemText = (0 until systemParts.length())
                .mapNotNull { systemParts.optJSONObject(it)?.optString("text") }
                .filter(String::isNotBlank)
                .joinToString("\n")
            if (systemText.isNotBlank()) {
                messages.put(JSONObject().put("role", "system").put("content", systemText))
            }
        }

        val contents = payload.optJSONArray("contents") ?: JSONArray()
        for (i in 0 until contents.length()) {
            val turn = contents.optJSONObject(i) ?: continue
            val role = if (turn.optString("role") == "model") "assistant" else "user"
            val parts = turn.optJSONArray("parts") ?: continue
            val multimodal = JSONArray()
            for (j in 0 until parts.length()) {
                val part = parts.optJSONObject(j) ?: continue
                val text = part.optString("text")
                if (text.isNotBlank()) {
                    multimodal.put(JSONObject().put("type", "text").put("text", text))
                }
                val inline = part.optJSONObject("inlineData")
                val mime = inline?.optString("mimeType")
                val base64 = inline?.optString("data")
                if (!mime.isNullOrBlank() && mime.startsWith("image/") && !base64.isNullOrBlank()) {
                    multimodal.put(
                        JSONObject()
                            .put("type", "image_url")
                            .put("image_url", JSONObject().put("url", "data:$mime;base64,$base64"))
                    )
                }
            }
            if (multimodal.length() > 0) {
                messages.put(JSONObject().put("role", role).put("content", multimodal))
            }
        }

        return JSONObject()
            .put("model", GROQ_VISION_MODELS.first())
            .put("messages", messages)
            .put("temperature", 0.3)
            .put("max_tokens", 1500)
    }

    private fun convertGeminiPayloadToGroqMessages(payload: JSONObject): JSONArray {
        val messages = JSONArray()
        val systemParts = payload.optJSONObject("systemInstruction")?.optJSONArray("parts")
        if (systemParts != null && systemParts.length() > 0) {
            val systemText = (0 until systemParts.length())
                .mapNotNull { systemParts.optJSONObject(it)?.optString("text") }
                .filter(String::isNotBlank)
                .joinToString("\n")
            if (systemText.isNotBlank()) {
                messages.put(JSONObject().put("role", "system").put("content", systemText))
            }
        }

        val contents = payload.optJSONArray("contents") ?: JSONArray()
        for (i in 0 until contents.length()) {
            val turn = contents.optJSONObject(i) ?: continue
            val role = if (turn.optString("role") == "model") "assistant" else "user"
            val parts = turn.optJSONArray("parts") ?: continue
            val text = StringBuilder()
            val multimodalContent = JSONArray()
            var hasImage = false

            fun addText(value: String) {
                if (value.isBlank()) return
                if (text.isNotEmpty()) text.append('\n')
                text.append(value)
                multimodalContent.put(JSONObject().put("type", "text").put("text", value))
            }

            for (j in 0 until parts.length()) {
                val part = parts.optJSONObject(j) ?: continue
                val partText = part.optString("text")
                if (partText.isNotBlank()) addText(partText)

                val existingImageUrl = part.optJSONObject("image_url")
                if (existingImageUrl != null) {
                    multimodalContent.put(
                        JSONObject().put("type", "image_url").put("image_url", existingImageUrl)
                    )
                    hasImage = true
                    continue
                }
                if (part.optString("type") == "image_url") {
                    multimodalContent.put(JSONObject(part.toString()))
                    hasImage = true
                    continue
                }

                val inlineData = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                if (inlineData != null) {
                    val data = inlineData.optString("data")
                    val mimeType = inlineData.optString("mimeType")
                        .ifBlank { inlineData.optString("mime_type") }
                        .ifBlank { "image/jpeg" }
                    if (!mimeType.startsWith("image/", ignoreCase = true)) continue
                    if (data.isNotBlank()) {
                        multimodalContent.put(
                            JSONObject()
                                .put("type", "image_url")
                                .put(
                                    "image_url",
                                    JSONObject().put("url", "data:$mimeType;base64,$data")
                                )
                        )
                        hasImage = true
                    }
                }
            }

            if (text.isNotEmpty() || hasImage) {
                val content: Any = if (hasImage) multimodalContent else text.toString()
                messages.put(JSONObject().put("role", role).put("content", content))
            }
        }
        return messages
    }

    private fun generateWithGroqDirect(model: String, payload: JSONObject): JSONObject {
        val keys = rotate(groqKeys(), groqKeyCursor)
        if (keys.isEmpty()) throw IllegalStateException("AI provider is not configured.")

        val visionRequest = containsImage(payload)
        val modelPool = if (visionRequest) {
            GROQ_VISION_MODELS
        } else {
            GROQ_TEXT_MODELS
        }
        val preferredModel = if (model in modelPool) model else modelPool.first()
        val modelCursor = if (visionRequest) groqVisionModelCursor else groqTextModelCursor
        val orderedModels = listOf(preferredModel) + rotate(
            modelPool.filterNot { it == preferredModel },
            modelCursor
        )
        var lastFailure: AiProxyException? = null

        for (apiKey in keys) {
            for (targetModel in orderedModels) {
                try {
                    val requestPayload = JSONObject(payload.toString()).put("model", targetModel)
                    val request = Request.Builder()
                        .url("https://api.groq.com/openai/v1/chat/completions")
                        .header("Authorization", "Bearer " + apiKey)
                        .header("Content-Type", "application/json")
                        .post(requestPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        val body = response.body?.string().orEmpty()
                        if (response.isSuccessful && body.isNotBlank()) return JSONObject(body)
                        lastFailure = httpFailure(response.code)
                    }
                } catch (error: Exception) {
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    lastFailure = networkFailure(error)
                }
            }
        }
        throw lastFailure ?: AiProxyException("AI service request failed.")
    }

    suspend fun transcribeGroqAudio(audioFile: File, mimeType: String): String =
        withContext(Dispatchers.IO) {
            require(audioFile.isFile && audioFile.length() in 1..MAX_AUDIO_SIZE_BYTES) {
                "Audio file is missing, empty, or larger than the secure transcription limit."
            }
            val proxyUrl = BuildConfig.AI_PROXY_BASE_URL.trim()
            require(proxyUrl.isNotBlank()) { "Secure AI proxy is not configured." }
            val firebaseUser = FirebaseAuth.getInstance().currentUser
                ?: throw AiProxyException("Firebase sign-in is required for transcription.", httpCode = 401)
            val idToken = firebaseUser.getIdToken(true).await().token
                ?: throw AiProxyException("Firebase authentication token is unavailable.", httpCode = 401)
            val appCheckToken = FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
                ?: throw AiProxyException("Firebase App Check token is unavailable.", httpCode = 401)

            val safeExtension = when (mimeType) {
                "audio/wav" -> "wav"
                "audio/webm" -> "webm"
                "audio/mpeg" -> "mp3"
                "audio/mp4" -> "mp4"
                else -> "m4a"
            }
            val requestPayload = JSONObject()
                .put("action", "transcribe")
                .put("provider", "groq")
                .put("model", GROQ_AUDIO_MODELS.first())
                .put(
                    "payload",
                    JSONObject()
                        .put("language", "ar")
                        .put("mimeType", mimeType)
                        .put("fileName", "recitation.$safeExtension")
                        .put("audioBase64", android.util.Base64.encodeToString(audioFile.readBytes(), android.util.Base64.NO_WRAP))
                )

            val request = Request.Builder()
                .url(proxyUrl)
                .header("Authorization", "Bearer $idToken")
                .header("X-Firebase-AppCheck", appCheckToken)
                .header("Content-Type", "application/json")
                .post(requestPayload.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(TAG, "Secure transcription proxy returned HTTP ${response.code}.")
                    throw httpFailure(response.code)
                }
                val text = runCatching { JSONObject(body).optString("text") }.getOrDefault("").trim()
                if (text.isBlank()) throw AiProxyException("Secure transcription proxy returned an empty result.")
                text
            }
        }

    private fun hasGeminiContent(response: JSONObject): Boolean {
        val parts = response.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?: return false
        if (parts.length() == 0) return false
        return (0 until parts.length())
            .mapNotNull { parts.optJSONObject(it)?.optString("text") }
            .any(String::isNotBlank)
    }

    private fun containsImage(payload: JSONObject): Boolean {
        val contents = payload.optJSONArray("contents")
        if (contents != null) {
            for (i in 0 until contents.length()) {
                val parts = contents.optJSONObject(i)?.optJSONArray("parts") ?: continue
                for (j in 0 until parts.length()) {
                    val part = parts.optJSONObject(j) ?: continue
                    val inlineData = part.optJSONObject("inlineData") ?: part.optJSONObject("inline_data")
                    val mimeType = inlineData?.optString("mimeType")
                        ?.ifBlank { inlineData.optString("mime_type") }
                        ?.ifBlank { "image/jpeg" }
                    if (mimeType?.startsWith("image/", ignoreCase = true) == true ||
                        part.has("image_url") || part.optString("type") == "image_url"
                    ) return true
                }
            }
        }

        val messages = payload.optJSONArray("messages") ?: return false
        for (i in 0 until messages.length()) {
            val content = messages.optJSONObject(i)?.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                if (content.optJSONObject(j)?.optString("type") == "image_url") return true
            }
        }
        return false
    }

    private fun httpFailure(code: Int) = AiProxyException(
        message = "AI provider returned HTTP $code.",
        httpCode = code,
        isTransient = code == 408 || code == 425 || code == 429 || code >= 500
    )

    private fun networkFailure(error: Exception): AiProxyException {
        val cause = generateSequence<Throwable>(error) { it.cause }.toList()
        val isNetworkFailure = cause.any { it is IOException }
        val offline = cause.any {
            it is UnknownHostException || it is ConnectException ||
                it is NoRouteToHostException || (it is SocketException &&
                it.message.orEmpty().contains("network", ignoreCase = true))
        }
        return AiProxyException(
            message = if (offline) "AI provider is unreachable." else "AI provider request failed.",
            isTransient = isNetworkFailure,
            isOffline = offline,
            cause = error
        )
    }

    private fun <T> rotate(values: List<T>, cursor: AtomicInteger): List<T> {
        if (values.size < 2) return values
        val offset = Math.floorMod(cursor.getAndIncrement(), values.size)
        return values.drop(offset) + values.take(offset)
    }

    private val SUPPORTED_PROVIDERS = setOf("gemini", "groq")
    private val GEMINI_MODELS = listOf(
        "gemini-2.5-flash",
        "gemini-2.0-flash",
        "gemini-2.0-flash-lite"
    )
    private val GROQ_TEXT_MODELS = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant"
    )
    private val GROQ_VISION_MODELS = listOf(
        "meta-llama/llama-4-scout-17b-16e-instruct",
        "llama-3.2-11b-vision-preview"
    )
    private val GROQ_AUDIO_MODELS = listOf("whisper-large-v3-turbo", "whisper-large-v3")
    private const val MAX_AUDIO_SIZE_BYTES = 3 * 1024 * 1024L
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
}
