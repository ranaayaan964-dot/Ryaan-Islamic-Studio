package com.example.audio

import android.content.Context
import android.util.Log
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object SnoozeAudioDownloader {

    private const val TAG = "SnoozeAudioDownloader"

    // High quality recitation of Ayatul Kursi (Surah Al-Baqarah Ayah 255 - Mishary Alafasy)
    private const val DEFAULT_AYATUL_KURSI_URL =
        "https://everyayah.com/data/Alafasy_128kbps/002255.mp3"
    private const val SNOOZE_AUDIO_FILE_NAME = "snooze_ayatul_kursi.mp3"

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun downloadAyatulKursiAudio(
        context: Context,
        audioUrl: String = DEFAULT_AYATUL_KURSI_URL
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val targetFile = File(context.filesDir, SNOOZE_AUDIO_FILE_NAME)

            // If already downloaded and valid size, verify and return
            if (targetFile.exists() && targetFile.length() > 10_000) {
                Log.d(TAG, "Snooze Ayat audio already downloaded: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                SettingsRepository.getInstance(context).setSnoozeAudioPath(targetFile.absolutePath)
                return@withContext Result.success(targetFile)
            }

            Log.d(TAG, "Downloading Ayat recitation from $audioUrl to ${targetFile.absolutePath}...")
            val request = Request.Builder()
                .url(audioUrl)
                .addHeader("User-Agent", "Ryaan-Islamic-Studio-Android")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val tempFile = File(context.filesDir, "${SNOOZE_AUDIO_FILE_NAME}.tmp")

            FileOutputStream(tempFile).use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }

            if (tempFile.exists() && tempFile.length() > 5_000) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)

                // CRITICAL LOGIC: ONLY after verified file.exists(), update SettingsRepository
                if (targetFile.exists() && targetFile.length() > 0) {
                    SettingsRepository.getInstance(context).setSnoozeAudioPath(targetFile.absolutePath)
                    Log.d(TAG, "Successfully downloaded Ayat audio: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
                    Result.success(targetFile)
                } else {
                    Result.failure(Exception("Downloaded file verification failed"))
                }
            } else {
                tempFile.delete()
                Result.failure(Exception("Downloaded file is too small or corrupt"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download Snooze Ayat audio", e)
            Result.failure(e)
        }
    }

    fun getLocalSnoozeAudioFile(context: Context): File? {
        val file = File(context.filesDir, SNOOZE_AUDIO_FILE_NAME)
        return if (file.exists() && file.length() > 0) file else null
    }
}
