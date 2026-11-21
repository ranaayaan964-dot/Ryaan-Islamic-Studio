package com.ayaan.islamicstudio.data.remote

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class GroqHifzResult(
    val transcript: String,
    val feedback: String,
    val targetText: String,
    val isAccurate: Boolean = true
)

object GroqHifzClient {
    private const val TAG = "GroqHifzClient"

    suspend fun transcribeAudio(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!audioFile.isFile || audioFile.length() == 0L) {
                return@withContext Result.failure(IllegalArgumentException("Recitation audio file is missing or empty."))
            }
            val mimeType = when {
                audioFile.name.endsWith(".wav", ignoreCase = true) -> "audio/wav"
                audioFile.name.endsWith(".webm", ignoreCase = true) -> "audio/webm"
                audioFile.name.endsWith(".mp3", ignoreCase = true) -> "audio/mpeg"
                audioFile.name.endsWith(".m4a", ignoreCase = true) -> "audio/mp4"
                else -> "audio/m4a"
            }
            Result.success(AiProxyClient.transcribeGroqAudio(audioFile, mimeType))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Secure audio transcription failed.", error)
            Result.failure(error)
        }
    }

    suspend fun evaluateRecitation(
        audioFile: File,
        targetText: String,
        surahName: String
    ): Result<GroqHifzResult> = withContext(Dispatchers.IO) {
        try {
            if (targetText.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Select Quran text before recording a recitation."))
            }
            val transcript = transcribeAudio(audioFile).getOrElse { return@withContext Result.failure(it) }
            val prompt =
                "Compare the verified transcript of the student's recitation with the exact target text. Target surah: $surahName. Target Arabic: [$targetText]. Student transcript: [$transcript]. Identify only observable missing, substituted, or extra words. Do not claim to hear audio or assess sound-based Tajweed from a text transcript. Give concise, encouraging feedback."
            val routed = DualAiScholarEngine.askScholarWithSource(
                userQuery = prompt,
                customSystemPrompt = "You are a careful Quran memorization text-comparison assistant. Compare only the provided Arabic strings. Do not invent ayat, references, pronunciation observations, or Tajweed errors that cannot be inferred from text. If evidence is insufficient, say so clearly."
            )
            val feedback = if (
                routed.source.contains("Provider failover unavailable", ignoreCase = true) ||
                routed.answer.contains("HTTP 404", ignoreCase = true)
            ) {
                compareTranscriptLocally(targetText, transcript)
            } else {
                routed.answer
            }
            Result.success(
                GroqHifzResult(
                    transcript = transcript,
                    feedback = feedback,
                    targetText = targetText,
                    isAccurate = normalizeArabicWords(targetText) == normalizeArabicWords(transcript)
                )
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Secure Hifz evaluation failed.", error)
            Result.failure(error)
        }
    }

    private fun compareTranscriptLocally(target: String, transcript: String): String {
        val targetWords = normalizeArabicWords(target)
        val transcriptWords = normalizeArabicWords(transcript)
        if (targetWords.isEmpty() || transcriptWords.isEmpty()) {
            return "The transcript or target text was empty, so no comparison score is available."
        }
        if (targetWords == transcriptWords) {
            return "The transcribed words match the target text. Audio-based Tajweed and pronunciation still require a qualified listening review."
        }

        val remaining = transcriptWords.toMutableList()
        val missing = targetWords.filter { word ->
            val index = remaining.indexOf(word)
            if (index >= 0) {
                remaining.removeAt(index)
                false
            } else {
                true
            }
        }
        val missingText = missing.joinToString(" ").ifBlank { "none identified" }
        val extraText = remaining.joinToString(" ").ifBlank { "none identified" }
        if (missing.isEmpty() && remaining.isEmpty()) {
            return "The transcribed words appear in a different order than the target. Please review the text and audio with a qualified teacher; audio-based Tajweed and pronunciation are not assessed."
        }
        return "Text comparison only: possible missing/substituted words: $missingText. Possible extra words: $extraText. Please review the audio with a qualified teacher for pronunciation and Tajweed."
    }

    private fun normalizeArabicWords(text: String): List<String> = text
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]"), "")
        .replace(Regex("[\\p{Punct}،؛؟۝۞۩•]"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotBlank)
}
