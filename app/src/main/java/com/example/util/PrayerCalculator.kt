package com.example.util

import com.example.data.model.CountdownState
import com.example.data.model.PrayerTimeItem
import com.example.data.model.PrayerType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Astronomical solar calculation engine for Islamic Prayer Times.
 * Implements standard Muslim World League / Umm al-Qura calculation parameters.
 */
object PrayerCalculator {

    private const val FAJR_TWILIGHT_ANGLE = 18.0
    private const val ISHA_TWILIGHT_ANGLE = 17.5
    private const val SUN_ALTITUDE_SUNRISE = -0.8333

    data class RawPrayerTimes(
        val fajr: Double,
        val sunrise: Double,
        val dhuhr: Double,
        val asr: Double,
        val maghrib: Double,
        val isha: Double
    )

    fun calculatePrayerTimes(
        latitude: Double,
        longitude: Double,
        calendar: Calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Karachi")),
        alarmSettings: Map<PrayerType, Boolean> = emptyMap()
    ): List<PrayerTimeItem> {
        val pkZone = java.util.TimeZone.getTimeZone("Asia/Karachi")
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val timezoneOffset = calendar.timeZone.getOffset(calendar.timeInMillis) / 3600000.0

        val julianDay = getJulianDay(year, month, day)
        val raw = computeRawTimes(julianDay, latitude, longitude, timezoneOffset)

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply {
            timeZone = pkZone
        }
        val now = Calendar.getInstance(pkZone).timeInMillis

        val prayerEntries = listOf(
            PrayerType.FAJR to raw.fajr,
            PrayerType.SUNRISE to raw.sunrise,
            PrayerType.DHUHR to raw.dhuhr,
            PrayerType.ASR to raw.asr,
            PrayerType.MAGHRIB to raw.maghrib,
            PrayerType.ISHA to raw.isha
        )

        // Find which is next
        var nextFound = false
        val items = mutableListOf<PrayerTimeItem>()

        for ((type, decimalHour) in prayerEntries) {
            val prayerCalendar = (calendar.clone() as Calendar).apply {
                val hour = floor(decimalHour).toInt()
                val minute = floor((decimalHour - hour) * 60).toInt()
                val second = floor(((decimalHour - hour) * 60 - minute) * 60).toInt()
                set(Calendar.HOUR_OF_DAY, hour % 24)
                set(Calendar.MINUTE, minute % 60)
                set(Calendar.SECOND, second % 60)
                set(Calendar.MILLISECOND, 0)
            }

            val prayerMillis = prayerCalendar.timeInMillis
            val isPassed = now > prayerMillis
            val isNext = !isPassed && !nextFound && type != PrayerType.SUNRISE
            if (isNext) {
                nextFound = true
            }

            val isAlarmOn = alarmSettings[type] ?: (type != PrayerType.SUNRISE)

            items.add(
                PrayerTimeItem(
                    type = type,
                    timeFormatted = timeFormat.format(Date(prayerMillis)),
                    timeMillis = prayerMillis,
                    isPassed = isPassed,
                    isNext = isNext,
                    isAlarmEnabled = isAlarmOn
                )
            )
        }

        // If all prayers today have passed, Fajr of tomorrow is next
        if (!nextFound) {
            val updatedItems = items.map { item ->
                if (item.type == PrayerType.FAJR) item.copy(isNext = true) else item
            }
            return updatedItems
        }

        return items
    }

    fun computeCountdown(prayers: List<PrayerTimeItem>): CountdownState {
        val now = System.currentTimeMillis()
        val nextPrayer = prayers.firstOrNull { it.isNext }
            ?: prayers.firstOrNull { it.type == PrayerType.FAJR }
            ?: return CountdownState()

        var diffMillis = nextPrayer.timeMillis - now
        if (diffMillis < 0) {
            // Means tomorrow's Fajr
            diffMillis += 24 * 60 * 60 * 1000L
        }

        val totalSeconds = (diffMillis / 1000).toInt()
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        // Assume average interval between prayers is ~4 hours for progress ring
        val intervalSeconds = 4 * 3600f
        val elapsedSeconds = (intervalSeconds - totalSeconds).coerceAtLeast(0f)
        val progress = (elapsedSeconds / intervalSeconds).coerceIn(0f, 1f)

        return CountdownState(
            nextPrayerName = nextPrayer.type.displayName,
            nextPrayerArabic = nextPrayer.type.arabicName,
            formattedTime = nextPrayer.timeFormatted,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            progress = progress
        )
    }

    private fun computeRawTimes(
        julianDay: Double,
        latitude: Double,
        longitude: Double,
        timezone: Double
    ): RawPrayerTimes {
        val d = julianDay - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dSin(g) + 0.020 * dSin(2 * g))

        val e = 23.439 - 0.00000036 * d
        val ra = fixAngle(dAtan2(dCos(e) * dSin(l), dCos(l))) / 15.0

        val declination = dAsin(dSin(e) * dSin(l))
        val equationOfTime = q / 15.0 - ra

        // Solar noon (Dhuhr)
        val dhuhr = 12.0 + timezone - (longitude / 15.0) - equationOfTime

        // Sunrise & Sunset
        val sunAlt = SUN_ALTITUDE_SUNRISE
        val sunHourAngle = hourAngle(latitude, declination, sunAlt)
        val sunrise = dhuhr - sunHourAngle
        val sunset = dhuhr + sunHourAngle

        // Fajr
        val fajrHourAngle = hourAngle(latitude, declination, -FAJR_TWILIGHT_ANGLE)
        val fajr = dhuhr - fajrHourAngle

        // Isha
        val ishaHourAngle = hourAngle(latitude, declination, -ISHA_TWILIGHT_ANGLE)
        val isha = dhuhr + ishaHourAngle

        // Asr (Shafi'i: shadow length factor = 1)
        val asrAngle = -dAtan(1.0 + dTan(abs(latitude - declination)))
        val asrHourAngle = hourAngle(latitude, declination, asrAngle)
        val asr = dhuhr + asrHourAngle

        return RawPrayerTimes(
            fajr = fixHour(fajr),
            sunrise = fixHour(sunrise),
            dhuhr = fixHour(dhuhr),
            asr = fixHour(asr),
            maghrib = fixHour(sunset),
            isha = fixHour(isha)
        )
    }

    private fun hourAngle(latitude: Double, declination: Double, angle: Double): Double {
        val cosHA = (dSin(angle) - dSin(latitude) * dSin(declination)) / (dCos(latitude) * dCos(declination))
        val clampedCos = cosHA.coerceIn(-1.0, 1.0)
        return dAcos(clampedCos) / 15.0
    }

    private fun getJulianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun fixAngle(angle: Double): Double {
        var a = angle - 360.0 * floor(angle / 360.0)
        if (a < 0) a += 360.0
        return a
    }

    private fun fixHour(hour: Double): Double {
        var h = hour - 24.0 * floor(hour / 24.0)
        if (h < 0) h += 24.0
        return h
    }

    private fun dSin(degrees: Double) = sin(Math.toRadians(degrees))
    private fun dCos(degrees: Double) = cos(Math.toRadians(degrees))
    private fun dTan(degrees: Double) = tan(Math.toRadians(degrees))
    private fun dAsin(value: Double) = Math.toDegrees(asin(value))
    private fun dAcos(value: Double) = Math.toDegrees(acos(value))
    private fun dAtan(value: Double) = Math.toDegrees(atan(value))
    private fun dAtan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
}
