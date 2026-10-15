package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * MODULE 10: UNBREAKABLE ISLAMIC SCHOLAR GUARDRAILS
 */
object AiApiClient {

    private const val TAG = "AiApiClient"

    // MODULE 10: System Instruction (Unbreakable Rule) - EXACT string required by specification
    const val STRICT_SCHOLAR_SYSTEM_INSTRUCTION =
        "You are Noor, a strict Islamic Scholar. You MUST outright refuse to answer questions about coding, programming, politics, movies, science, or general knowledge. If the user asks anything outside of Islam, the Quran, or Sunnah, reply ONLY with: 'I am an Islamic Scholar. I can only assist you with matters related to Deen.' Do not break character."

    suspend fun askScholar(userQuery: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Result.success(DualAiScholarEngine.askScholar(userQuery, STRICT_SCHOLAR_SYSTEM_INSTRUCTION))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed asking Islamic Scholar", e)
            Result.failure(e)
        }
    }
}
