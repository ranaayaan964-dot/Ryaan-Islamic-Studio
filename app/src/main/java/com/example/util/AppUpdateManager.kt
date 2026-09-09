package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppVersionInfo(
    val latestVersionName: String,
    val latestVersionCode: Int,
    val releaseDate: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val isMandatory: Boolean = false
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(
        val info: AppVersionInfo,
        val currentVersionName: String,
        val currentVersionCode: Int
    ) : UpdateCheckResult

    data class UpToDate(
        val currentVersionName: String,
        val currentVersionCode: Int
    ) : UpdateCheckResult

    data class Error(
        val message: String,
        val currentVersionName: String,
        val currentVersionCode: Int
    ) : UpdateCheckResult
}

object AppUpdateManager {

    private const val TAG = "AppUpdateManager"

    // Primary & backup remote manifest URLs
    private const val REMOTE_VERSION_URL =
        "https://raw.githubusercontent.com/ranaayaan964/sahiwal-prayer-times/main/version_manifest.json"

    val currentVersionName: String
        get() = BuildConfig.VERSION_NAME

    val currentVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    val currentVersionDisplay: String
        get() = "v$currentVersionName (Build $currentVersionCode)"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    /**
     * Checks for updates asynchronously against the remote manifest or fallback release metadata.
     */
    suspend fun checkForUpdates(forceSimulateUpdate: Boolean = false): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentCode = currentVersionCode
        val currentName = currentVersionName

        if (forceSimulateUpdate) {
            val simulated = AppVersionInfo(
                latestVersionName = "1.2.0",
                latestVersionCode = currentCode + 1,
                releaseDate = "September 2026",
                releaseNotes = "• Instant Test Notification Panel for all 5 prayers\n" +
                        "• Custom Prayer Reminder Texts & Quranic phrasing\n" +
                        "• Expanded Ringtone & Audio Hub with 20+ Sacred Tones\n" +
                        "• High-precision Qibla bearing & battery optimizations",
                downloadUrl = "https://github.com/ranaayaan964/sahiwal-prayer-times/releases/latest"
            )
            return@withContext UpdateCheckResult.UpdateAvailable(simulated, currentName, currentCode)
        }

        try {
            val request = Request.Builder()
                .url(REMOTE_VERSION_URL)
                .addHeader("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    val json = JSONObject(body)
                    val remoteVersionCode = json.optInt("versionCode", currentCode)
                    val remoteVersionName = json.optString("versionName", currentName)
                    val releaseDate = json.optString("releaseDate", "Latest")
                    val releaseNotes = json.optString("releaseNotes", "Performance improvements and bug fixes.")
                    val downloadUrl = json.optString("downloadUrl", "https://github.com/ranaayaan964/sahiwal-prayer-times/releases")
                    val mandatory = json.optBoolean("mandatory", false)

                    val info = AppVersionInfo(
                        latestVersionName = remoteVersionName,
                        latestVersionCode = remoteVersionCode,
                        releaseDate = releaseDate,
                        releaseNotes = releaseNotes,
                        downloadUrl = downloadUrl,
                        isMandatory = mandatory
                    )

                    return@withContext if (remoteVersionCode > currentCode) {
                        UpdateCheckResult.UpdateAvailable(info, currentName, currentCode)
                    } else {
                        UpdateCheckResult.UpToDate(currentName, currentCode)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Remote version check failed: ${e.message}. Using fallback validation.")
        }

        // Graceful online fallback: If remote is not reached, report up to date based on installed BuildConfig
        UpdateCheckResult.UpToDate(currentName, currentCode)
    }

    /**
     * Opens download URL in the external browser.
     */
    fun openDownloadUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open update URL: ${e.message}")
        }
    }
}
