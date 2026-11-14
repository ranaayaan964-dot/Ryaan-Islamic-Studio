package com.example.data.remote

import android.util.Base64
import com.example.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object AiProxyClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun generate(
        provider: String,
        model: String,
        payload: JSONObject
    ): JSONObject = request("generate", provider, model, payload)

    private suspend fun request(
        action: String,
        provider: String,
        model: String,
        payload: JSONObject
    ): JSONObject = withContext(Dispatchers.IO) {
        require(provider in SUPPORTED_PROVIDERS) { "Unsupported AI provider." }
        require(model in SUPPORTED_MODELS) { "Unsupported AI model." }

        val endpoint = BuildConfig.AI_PROXY_BASE_URL.trim()
            .takeIf(String::isNotBlank)
            ?: error("Secure AI service is not configured.")
        val token = FirebaseAuth.getInstance().currentUser
            ?.getIdToken(false)
            ?.await()
            ?.token
            ?.takeIf(String::isNotBlank)
            ?: error("Sign in to use the AI service.")
        val requestBody = JSONObject()
            .put("action", action)
            .put("provider", provider)
            .put("model", model)
            .put("payload", payload)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            check(response.isSuccessful) {
                val message = runCatching {
                    JSONObject(body).optString("error").ifBlank { JSONObject(body).optString("message") }
                }.getOrDefault("")
                "AI service request failed (${response.code})${message.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty()}"
            }
            JSONObject(body)
        }
    }

    suspend fun transcribeGroqAudio(audioFile: File, mimeType: String): String {
        require(audioFile.isFile && audioFile.length() in 1..MAX_AUDIO_SIZE_BYTES) {
            "Audio file is missing, empty, or larger than the secure transcription limit."
        }
        val payload = JSONObject()
            .put("data", Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP))
            .put("mimeType", mimeType)
        val result = request("transcribe", "groq", "whisper-large-v3", payload)
        return result.optString("text").trim().takeIf(String::isNotBlank)
            ?: error("Speech transcription returned an empty result.")
    }

    private val SUPPORTED_PROVIDERS = setOf("gemini", "groq")
    private val SUPPORTED_MODELS = setOf(
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
