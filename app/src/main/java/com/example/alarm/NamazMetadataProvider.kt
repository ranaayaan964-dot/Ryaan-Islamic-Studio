package com.example.alarm

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.example.data.model.PrayerTimeItem
import com.example.data.model.PrayerType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Data class representing daily namaz configuration specified in app metadata.
 */
data class NamazMetadataConfig(
    val fajrTime: String,
    val sunriseTime: String,
    val dhuhrTime: String,
    val asrTime: String,
    val maghribTime: String,
    val ishaTime: String,
    val calculationMethod: String,
    val juristicSchool: String,
    val notificationsEnabled: Boolean,
    val preReminderMinutes: Int
)

/**
 * Utility to parse and resolve daily namaz times and settings specified in AndroidManifest app metadata.
 */
object NamazMetadataProvider {

    private const val TAG = "NamazMetadataProvider"

    // Metadata keys specified in AndroidManifest.xml <meta-data> tags
    const val META_KEY_FAJR = "namaz_time_fajr"
    const val META_KEY_SUNRISE = "namaz_time_sunrise"
    const val META_KEY_DHUHR = "namaz_time_dhuhr"
    const val META_KEY_ASR = "namaz_time_asr"
    const val META_KEY_MAGHRIB = "namaz_time_maghrib"
    const val META_KEY_ISHA = "namaz_time_isha"
    const val META_KEY_METHOD = "namaz_calculation_method"
    const val META_KEY_SCHOOL = "namaz_juristic_school"
    const val META_KEY_ENABLED = "namaz_notifications_enabled"
    const val META_KEY_PRE_REMINDER = "namaz_pre_reminder_minutes"

    // Default times if metadata is missing
    const val DEFAULT_FAJR = "05:15"
    const val DEFAULT_SUNRISE = "06:30"
    const val DEFAULT_DHUHR = "12:30"
    const val DEFAULT_ASR = "16:00"
    const val DEFAULT_MAGHRIB = "18:25"
    const val DEFAULT_ISHA = "20:00"
    const val DEFAULT_METHOD = "University of Islamic Sciences, Karachi"
    const val DEFAULT_SCHOOL = "Hanafi"

    /**
     * Reads and parses all Namaz metadata configured in AndroidManifest.xml
     */
    fun loadNamazMetadata(context: Context): NamazMetadataConfig {
        var bundle: Bundle? = null
        try {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA
                )
            }
            bundle = appInfo.metaData
        } catch (e: Exception) {
            Log.w(TAG, "Could not load application metadata: ${e.message}")
        }

        val fajr = bundle?.getString(META_KEY_FAJR)?.trim()?.ifEmpty { null } ?: DEFAULT_FAJR
        val sunrise = bundle?.getString(META_KEY_SUNRISE)?.trim()?.ifEmpty { null } ?: DEFAULT_SUNRISE
        val dhuhr = bundle?.getString(META_KEY_DHUHR)?.trim()?.ifEmpty { null } ?: DEFAULT_DHUHR
        val asr = bundle?.getString(META_KEY_ASR)?.trim()?.ifEmpty { null } ?: DEFAULT_ASR
        val maghrib = bundle?.getString(META_KEY_MAGHRIB)?.trim()?.ifEmpty { null } ?: DEFAULT_MAGHRIB
        val isha = bundle?.getString(META_KEY_ISHA)?.trim()?.ifEmpty { null } ?: DEFAULT_ISHA
        val method = bundle?.getString(META_KEY_METHOD) ?: DEFAULT_METHOD
        val school = bundle?.getString(META_KEY_SCHOOL) ?: DEFAULT_SCHOOL
        val enabled = bundle?.getBoolean(META_KEY_ENABLED, true) ?: true
        val preReminder = bundle?.getInt(META_KEY_PRE_REMINDER, 15) ?: 15

        return NamazMetadataConfig(
            fajrTime = fajr,
            sunriseTime = sunrise,
            dhuhrTime = dhuhr,
            asrTime = asr,
            maghribTime = maghrib,
            ishaTime = isha,
            calculationMethod = method,
            juristicSchool = school,
            notificationsEnabled = enabled,
            preReminderMinutes = preReminder
        )
    }

    /**
     * Returns the prayer time string (e.g. "05:15") from metadata for a given PrayerType.
     */
    fun getPrayerTimeString(config: NamazMetadataConfig, type: PrayerType): String {
        return when (type) {
            PrayerType.FAJR -> config.fajrTime
            PrayerType.SUNRISE -> config.sunriseTime
            PrayerType.DHUHR -> config.dhuhrTime
            PrayerType.ASR -> config.asrTime
            PrayerType.MAGHRIB -> config.maghribTime
            PrayerType.ISHA -> config.ishaTime
        }
    }

    /**
     * Formats 24-hour time string ("16:00") into 12-hour AM/PM string ("04:00 PM")
     */
    fun formatTo12Hour(time24: String): String {
        return try {
            val parts = time24.split(":")
            val hour = parts[0].trim().toInt()
            val min = parts[1].trim().toInt()
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
            }
            val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            sdf.format(calendar.time)
        } catch (e: Exception) {
            time24
        }
    }

    /**
     * Calculates epoch millis for the next occurrence of a given HH:mm time.
     * If the time has already passed today, rolls over to tomorrow.
     */
    fun calculateNextTriggerMillis(time24: String, nowMillis: Long = System.currentTimeMillis()): Long {
        return try {
            val parts = time24.split(":")
            val hour = parts[0].trim().toInt()
            val minute = parts[1].trim().toInt()

            val calendar = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (calendar.timeInMillis <= nowMillis) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            calendar.timeInMillis
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating trigger millis for $time24: ${e.message}")
            nowMillis + 60_000L // 1 minute safety fallback
        }
    }

    /**
     * Converts metadata schedule into a list of PrayerTimeItem objects for UI and scheduling.
     */
    fun getDailyNamazItems(context: Context): List<PrayerTimeItem> {
        val config = loadNamazMetadata(context)
        val now = System.currentTimeMillis()

        return PrayerType.values().map { type ->
            val timeStr = getPrayerTimeString(config, type)
            val nextMillis = calculateNextTriggerMillis(timeStr, now)
            val formatted = formatTo12Hour(timeStr)

            PrayerTimeItem(
                type = type,
                timeFormatted = formatted,
                timeMillis = nextMillis,
                isPassed = false,
                isNext = false,
                isAlarmEnabled = config.notificationsEnabled && type != PrayerType.SUNRISE
            )
        }
    }
}
