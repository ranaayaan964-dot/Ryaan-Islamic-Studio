package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * MODULE 10: UNBREAKABLE ISLAMIC SCHOLAR GUARDRAILS
 */
object AiApiClient {

    private const val TAG = "AiApiClient"
    private const val MODEL_NAME = "gemini-1.5-flash"

    // MODULE 10: System Instruction (Unbreakable Rule) - EXACT string required by specification
    const val STRICT_SCHOLAR_SYSTEM_INSTRUCTION =
        "You are Noor, a strict Islamic Scholar. You MUST outright refuse to answer questions about coding, programming, politics, movies, science, or general knowledge. If the user asks anything outside of Islam, the Quran, or Sunnah, reply ONLY with: 'I am an Islamic Scholar. I can only assist you with matters related to Deen.' Do not break character."

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    suspend fun askScholar(userQuery: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY.ifBlank { "" }

            val requestDto = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = userQuery))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = STRICT_SCHOLAR_SYSTEM_INSTRUCTION))
                ),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.2f,
                    maxOutputTokens = 800
                )
            )

            val json = moshi.adapter(GeminiRequest::class.java).toJson(requestDto)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.e(TAG, "Scholar API call failed: ${response.code} $body")
                return@withContext Result.failure(Exception("API Error: ${response.code}"))
            }

            val geminiResponse = moshi.adapter(GeminiResponse::class.java).fromJson(body)
            val reply = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I am an Islamic Scholar. I can only assist you with matters related to Deen."

            Result.success(reply)
        } catch (e: Exception) {
            Log.e(TAG, "Failed asking Islamic Scholar", e)
            Result.failure(e)
        }
    }
}
