package com.example.service.audio

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object QuranDownloadManager {

    private const val TAG = "QuranDownloadManager"

    // Map key: "${qariId.name}_${surahId}", value: 0.0f..1.0f or -1f on error
    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Float>> = _downloadProgressMap.asStateFlow()

    private val activeDownloadJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getOfflineDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "quran_offline")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getLocalSurahFile(context: Context, qariId: QariId, surahId: Int): File {
        val fileName = "surah_${qariId.name.lowercase()}_${String.format(java.util.Locale.US, "%03d", surahId)}.mp3"
        return File(getOfflineDir(context), fileName)
    }

    fun isSurahDownloaded(context: Context, qariId: QariId, surahId: Int): Boolean {
        val file = getLocalSurahFile(context, qariId, surahId)
        // Verified if file exists and has size > 10KB (valid audio file)
        return file.exists() && file.length() > 10240L
    }

    fun isDownloading(qariId: QariId, surahId: Int): Boolean {
        val key = "${qariId.name}_$surahId"
        val progress = _downloadProgressMap.value[key]
        return progress != null && progress in 0.0f..0.99f
    }

    fun getProgress(qariId: QariId, surahId: Int): Float? {
        val key = "${qariId.name}_$surahId"
        return _downloadProgressMap.value[key]
    }

    fun startDownload(context: Context, qariId: QariId, surahId: Int) {
        val key = "${qariId.name}_$surahId"
        if (isDownloading(qariId, surahId)) return

        val job = scope.launch {
            val urlString = QariCatalog.getSurahAudioUrl(qariId, surahId)
            val destFile = getLocalSurahFile(context, qariId, surahId)
            val tempFile = File(destFile.parentFile, "${destFile.name}.downloading")

            try {
                updateProgress(key, 0.01f)
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 20000
                connection.instanceFollowRedirects = true
                connection.connect()

                if (connection.responseCode !in 200..299) {
                    throw Exception("HTTP error code: ${connection.responseCode}")
                }

                val contentLength = connection.contentLengthLong
                var inputStream: InputStream? = null
                var outputStream: FileOutputStream? = null

                try {
                    inputStream = connection.inputStream
                    outputStream = FileOutputStream(tempFile)

                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (contentLength > 0L) {
                            val progress = (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0.01f, 0.99f)
                            updateProgress(key, progress)
                        }
                    }

                    outputStream.flush()
                } finally {
                    outputStream?.close()
                    inputStream?.close()
                    connection.disconnect()
                }

                // Rename temp file to destination file atomically
                if (tempFile.exists() && tempFile.length() > 10240L) {
                    if (destFile.exists()) destFile.delete()
                    tempFile.renameTo(destFile)
                    updateProgress(key, 1.0f)
                } else {
                    tempFile.delete()
                    clearProgress(key)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for $key: ${e.message}", e)
                if (tempFile.exists()) tempFile.delete()
                clearProgress(key)
            } finally {
                activeDownloadJobs.remove(key)
            }
        }
        activeDownloadJobs[key] = job
    }

    fun deleteDownloadedSurah(context: Context, qariId: QariId, surahId: Int): Boolean {
        val key = "${qariId.name}_$surahId"
        val file = getLocalSurahFile(context, qariId, surahId)
        val deleted = if (file.exists()) file.delete() else false
        clearProgress(key)
        return deleted
    }

    private fun updateProgress(key: String, progress: Float) {
        val current = _downloadProgressMap.value.toMutableMap()
        current[key] = progress
        _downloadProgressMap.value = current
    }

    private fun clearProgress(key: String) {
        val current = _downloadProgressMap.value.toMutableMap()
        current.remove(key)
        _downloadProgressMap.value = current
    }
}
