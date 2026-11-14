package com.example.data.remote

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object GeminiHifzClient {
    private const val TAG = "GeminiHifzClient"
    private const val MODEL_NAME = "gemini-2.5-flash"
    private const val MAX_AUDIO_SIZE_BYTES = 2 * 1024 * 1024L

    suspend fun evaluateHifzRecitation(
        cacheAudioPath: String,
        surahName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val audioFile = File(cacheAudioPath)
        if (!audioFile.isFile || audioFile.length() == 0L) {
            return@withContext Result.failure(
                IllegalArgumentException("Audio recording file is missing or empty. Please record your recitation again.")
            )
        }
        if (audioFile.length() > MAX_AUDIO_SIZE_BYTES) {
            return@withContext Result.failure(
                IllegalArgumentException("Audio file exceeds the secure size limit. Please record a shorter recitation.")
            )
        }

        try {
            val primaryError = runCatching { evaluateWithGemini(audioFile, surahName) }
                .onFailure { if (it is CancellationException) throw it }
                .getOrNull()
            if (!primaryError.isNullOrBlank()) {
                return@withContext Result.success(primaryError)
            }

            val fallback = runCatching {
                val mimeType = if (audioFile.name.endsWith(".wav", ignoreCase = true)) "audio/wav" else "audio/m4a"
                val transcript = AiProxyClient.transcribeGroqAudio(audioFile, mimeType)
                val routed = DualAiScholarEngine.askScholarWithSource(
                    userQuery = "Compare the verified transcript of the student's recitation with Surah $surahName. Transcript: [$transcript]. Identify only observable word differences; do not claim sound-based Tajweed observations from text alone.",
                    customSystemPrompt = "Compare only the provided Arabic transcript. Do not invent Quranic text, references, pronunciation observations, or Tajweed errors. State uncertainty clearly."
                )
                if (routed.source == "Provider failover unavailable") {
                    "Transcript: $transcript\n\n${compareTranscriptLocally(surahName, transcript)}"
                } else {
                    "Transcript: $transcript\n\n${routed.answer}"
                }
            }.onFailure {
                if (it is CancellationException) throw it
                Log.w(TAG, "Groq transcription fallback is unavailable.", it)
            }.getOrNull()
            Result.success(fallback ?: offlineGuidance(surahName))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Hifz evaluation failed.", error)
            Result.success(offlineGuidance(surahName))
        }
    }

    private suspend fun evaluateWithGemini(audioFile: File, surahName: String): String {
        val audioData = Base64.encodeToString(audioFile.readBytes(), Base64.NO_WRAP)
        val mimeType = if (audioFile.name.endsWith(".wav", ignoreCase = true)) "audio/wav" else "audio/mp4"
        val prompt = "Listen to this Arabic recitation for Surah $surahName. Transcribe it exactly and compare only against reliable known Quran text. Point out observable missing or substituted words. Do not claim precise Tajweed or pronunciation observations unless clearly audible; encourage review with a qualified teacher."
        val payload = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray()
                            .put(JSONObject().put("text", prompt))
                            .put(
                                JSONObject().put(
                                    "inlineData",
                                    JSONObject().put("mimeType", mimeType).put("data", audioData)
                                )
                            )
                    )
                )
            )
            .put("generationConfig", JSONObject().put("temperature", 0.2).put("maxOutputTokens", 2048))
        val response = AiProxyClient.generate("gemini", MODEL_NAME, payload)
        return response.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.let { parts ->
                (0 until parts.length())
                    .mapNotNull { parts.optJSONObject(it)?.optString("text") }
                    .joinToString("\n")
                    .trim()
            }
            ?.takeIf(String::isNotBlank)
            ?: error("Gemini returned no Hifz evaluation.")
    }

    private fun compareTranscriptLocally(surahName: String, transcript: String): String =
        "Text-only transcript comparison for $surahName: \"$transcript\". Please review the Arabic words with a qualified teacher; audio-based Tajweed and pronunciation cannot be confirmed by this fallback."

    private fun offlineGuidance(surahName: String): String =
        "AI transcription is temporarily unavailable for $surahName. No recitation accuracy score was generated. Please try again later or review the recording with a qualified Quran teacher."
}
