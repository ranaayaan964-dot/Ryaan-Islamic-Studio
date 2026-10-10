package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.GitHubApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppVersionInfo(
    val latestVersionName: String,
    val latestVersionCode: Int = 0,
    val releaseDate: String = "GitHub Release",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val isMandatory: Boolean = true
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

/**
 * Production-Grade GitHub In-App Update Manager.
 *
 * Exclusively queries the live GitHub Releases API for:
 * https://api.github.com/repos/ranaayaan964-dot/Ryaan-Islamic-Studio/releases/latest
 *
 * Performs strict mathematical semantic version comparison against BuildConfig.VERSION_NAME.
 * Absolutely zero mock data, zero simulation, and zero fake timers.
 */
object AppUpdateManager {

    private const val TAG = "AppUpdateManager"
    const val DEFAULT_GITHUB_RELEASE_URL =
        "https://github.com/ranaayaan964-dot/Ryaan-Islamic-Studio/releases/latest"

    val currentVersionName: String
        get() = BuildConfig.VERSION_NAME

    val currentVersionCode: Int
        get() = BuildConfig.VERSION_CODE

    val currentVersionDisplay: String
        get() = "v$currentVersionName (Build $currentVersionCode)"

    private val gitHubService: GitHubApiService by lazy {
        GitHubApiService.create()
    }

    /**
     * Checks for updates by querying GitHub Releases API directly on Dispatchers.IO.
     * Compares the remote tag against [BuildConfig.VERSION_NAME] using [VersionComparator].
     */
    suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        val currentName = currentVersionName
        val currentCode = currentVersionCode

        try {
            Log.d(TAG, "Querying live GitHub API for latest release... Local version: $currentName")
            val response = gitHubService.getLatestRelease()

            if (response.isSuccessful) {
                val release = response.body()
                val remoteTag = release?.tag_name?.trim().orEmpty()
                val downloadUrl = release?.html_url?.trim().orEmpty().ifBlank { DEFAULT_GITHUB_RELEASE_URL }

                Log.d(TAG, "GitHub release response: tag_name='$remoteTag', html_url='$downloadUrl'")

                if (remoteTag.isNotBlank() && VersionComparator.isUpdateAvailable(remoteTag, currentName)) {
                    Log.i(TAG, "Update detected! Remote: $remoteTag > Local: $currentName")
                    val info = AppVersionInfo(
                        latestVersionName = remoteTag,
                        latestVersionCode = currentCode + 1,
                        releaseDate = "Latest GitHub Release",
                        releaseNotes = "Version $remoteTag of Ryaan Islamic Studio is now available! Please update to get the latest features and bug fixes.",
                        downloadUrl = downloadUrl,
                        isMandatory = true
                    )
                    return@withContext UpdateCheckResult.UpdateAvailable(info, currentName, currentCode)
                } else {
                    Log.d(TAG, "Application is up to date: Local $currentName >= Remote $remoteTag")
                    return@withContext UpdateCheckResult.UpToDate(currentName, currentCode)
                }
            } else if (response.code() == 404) {
                // No releases published yet on the repository
                Log.d(TAG, "GitHub returned 404: No releases currently published. App is up to date.")
                return@withContext UpdateCheckResult.UpToDate(currentName, currentCode)
            } else {
                Log.w(TAG, "GitHub API HTTP ${response.code()}: ${response.message()}")
                // Attempt fallback via direct OkHttp call
                val directRelease = GitHubApiService.fetchLatestReleaseDirect()
                if (directRelease != null && directRelease.tagName.isNotBlank()) {
                    val remoteTag = directRelease.tagName
                    val downloadUrl = directRelease.htmlUrl.ifBlank { DEFAULT_GITHUB_RELEASE_URL }
                    if (VersionComparator.isUpdateAvailable(remoteTag, currentName)) {
                        val info = AppVersionInfo(
                            latestVersionName = remoteTag,
                            latestVersionCode = currentCode + 1,
                            releaseDate = "Latest GitHub Release",
                            releaseNotes = "Version $remoteTag of Ryaan Islamic Studio is now available! Please update to get the latest features and bug fixes.",
                            downloadUrl = downloadUrl,
                            isMandatory = true
                        )
                        return@withContext UpdateCheckResult.UpdateAvailable(info, currentName, currentCode)
                    } else {
                        return@withContext UpdateCheckResult.UpToDate(currentName, currentCode)
                    }
                }
                return@withContext UpdateCheckResult.Error(
                    message = "Unable to connect to GitHub releases (HTTP ${response.code()})",
                    currentVersionName = currentName,
                    currentVersionCode = currentCode
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "GitHub update check exception: ${e.localizedMessage}")
            // Check direct OkHttp fallback before failing
            try {
                val directRelease = GitHubApiService.fetchLatestReleaseDirect()
                if (directRelease != null && directRelease.tagName.isNotBlank()) {
                    val remoteTag = directRelease.tagName
                    val downloadUrl = directRelease.htmlUrl.ifBlank { DEFAULT_GITHUB_RELEASE_URL }
                    if (VersionComparator.isUpdateAvailable(remoteTag, currentName)) {
                        val info = AppVersionInfo(
                            latestVersionName = remoteTag,
                            latestVersionCode = currentCode + 1,
                            releaseDate = "Latest GitHub Release",
                            releaseNotes = "Version $remoteTag of Ryaan Islamic Studio is now available! Please update to get the latest features and bug fixes.",
                            downloadUrl = downloadUrl,
                            isMandatory = true
                        )
                        return@withContext UpdateCheckResult.UpdateAvailable(info, currentName, currentCode)
                    } else {
                        return@withContext UpdateCheckResult.UpToDate(currentName, currentCode)
                    }
                }
            } catch (ignored: Exception) {
                // Ignore fallback exception
            }

            return@withContext UpdateCheckResult.Error(
                message = e.localizedMessage ?: "Failed to check GitHub releases",
                currentVersionName = currentName,
                currentVersionCode = currentCode
            )
        }
    }

    /**
     * Executes the exact intent to launch the device browser directly to the GitHub release page.
     */
    fun openDownloadUrl(context: Context, githubHtmlUrl: String) {
        try {
            val url = githubHtmlUrl.ifBlank { DEFAULT_GITHUB_RELEASE_URL }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch browser intent for URL: $githubHtmlUrl", e)
        }
    }
}
