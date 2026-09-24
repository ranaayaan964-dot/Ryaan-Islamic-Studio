package com.example.util

import com.example.BuildConfig

/**
 * Robust Semantic Version Comparison Engine.
 *
 * Compares the app's current version (dynamically fetched from BuildConfig.VERSION_NAME)
 * against a remote GitHub release tag (e.g., "v1.0.5", "1.2.0", "v2.0.0-rc1").
 *
 * It strips the "v" or "V" prefix, splits both version strings by "." (dot) into arrays of
 * integers, and compares them digit-by-digit (Major, Minor, Patch) to determine if an
 * update is available.
 */
object VersionComparator {

    /**
     * Determines whether an update is available by comparing the remote tag name
     * against the current app version.
     *
     * @param remoteTagName The release tag from GitHub (e.g. "v1.0.5")
     * @param currentVersion Current app version, defaults to [BuildConfig.VERSION_NAME]
     * @return true if remote version > current version, false otherwise
     */
    fun isUpdateAvailable(
        remoteTagName: String,
        currentVersion: String = BuildConfig.VERSION_NAME
    ): Boolean {
        if (remoteTagName.isBlank()) return false

        val cleanRemote = cleanVersionString(remoteTagName)
        val cleanCurrent = cleanVersionString(currentVersion)

        return compareVersions(cleanRemote, cleanCurrent) > 0
    }

    /**
     * Compares two semantic version strings digit-by-digit.
     *
     * @return a positive integer if [version1] > [version2],
     *         negative if [version1] < [version2],
     *         zero if [version1] == [version2]
     */
    fun compareVersions(version1: String, version2: String): Int {
        val parts1 = parseToIntegerArray(version1)
        val parts2 = parseToIntegerArray(version2)

        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }

            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }

        return 0
    }

    /**
     * Strips leading "v" or "V", trims whitespace, and strips suffix metadata (like "-release", "-beta").
     */
    fun cleanVersionString(version: String): String {
        return version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore("-")
            .trim()
    }

    /**
     * Splits a version string by "." and parses each component into an integer array.
     * Non-integer tokens safely default to 0.
     */
    fun parseToIntegerArray(version: String): List<Int> {
        val cleaned = cleanVersionString(version)
        if (cleaned.isEmpty()) return emptyList()

        return cleaned.split(".")
            .map { segment ->
                segment.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
    }
}
