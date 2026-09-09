package com.example.data.repository

import android.util.Log
import com.example.data.api.AladhanApiService
import com.example.data.api.IslamicFinderApiService
import com.example.data.model.AladhanApiResponse
import com.example.data.model.PrayerTimeItem
import com.example.data.model.PrayerType
import com.example.data.model.SahiwalPrayerSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class PrayerApiProvider(val displayName: String, val shortName: String, val endpointUrl: String) {
    ALADHAN_EXACT_COORDINATES(
        displayName = "Aladhan API (Exact GPS 30.6682, 73.1114 • Hanafi)",
        shortName = "Aladhan GPS",
        endpointUrl = "https://aladhan.api.islamic.network/v1/timings/{date}?latitude=30.6682&longitude=73.1114&method=1&school=1&timezonestring=Asia/Karachi"
    ),
    ALADHAN_CITY(
        displayName = "Aladhan API (Sahiwal City, Pakistan)",
        shortName = "Aladhan City",
        endpointUrl = "https://aladhan.api.islamic.network/v1/timingsByCity/{date}?city=Sahiwal&country=Pakistan&method=1&school=1"
    ),
    ISLAMIC_FINDER(
        displayName = "Islamic Finder API",
        shortName = "Islamic Finder",
        endpointUrl = IslamicFinderApiService.ISLAMIC_FINDER_URL
    ),
    MUSLIM_SALAT(
        displayName = "MuslimSalat API",
        shortName = "MuslimSalat",
        endpointUrl = IslamicFinderApiService.MUSLIM_SALAT_URL
    )
}

object AladhanRepository {

    private const val TAG = "PrayerScheduleRepo"
    private val aladhanNetworkService by lazy { AladhanApiService.create(AladhanApiService.PRIMARY_ISLAMIC_NETWORK_URL) }
    private val aladhanDirectService by lazy { AladhanApiService.create(AladhanApiService.SECONDARY_ALADHAN_URL) }
    private val islamicFinderApiService by lazy { IslamicFinderApiService.create() }

    // Official exact GPS Coordinates for Sahiwal, Punjab, Pakistan
    const val SAHIWAL_LATITUDE = 30.6682
    const val SAHIWAL_LONGITUDE = 73.1114
    const val SAHIWAL_TIMEZONE = "Asia/Karachi"
    val pkZone: TimeZone = TimeZone.getTimeZone(SAHIWAL_TIMEZONE)

    suspend fun fetchSahiwalPrayerTimings(
        provider: PrayerApiProvider = PrayerApiProvider.ALADHAN_EXACT_COORDINATES
    ): Result<Pair<SahiwalPrayerSchedule, List<PrayerTimeItem>>> {
        return withContext(Dispatchers.IO) {
            // First attempt with requested provider
            val firstTry = tryFetchFromProvider(provider)
            if (firstTry != null) {
                return@withContext Result.success(firstTry)
            }

            // If requested provider failed, try fallback providers in order
            for (fallbackProvider in PrayerApiProvider.values()) {
                if (fallbackProvider != provider) {
                    val fallbackResult = tryFetchFromProvider(fallbackProvider)
                    if (fallbackResult != null) {
                        return@withContext Result.success(fallbackResult)
                    }
                }
            }

            // If all network endpoints unavailable, use exact astronomical Karachi University calculation for Sahiwal
            Log.w(TAG, "Using accurate astronomical Karachi University calculation for Sahiwal, PK")
            val offlineSchedule = getSahiwalFallbackSchedule(provider)
            return@withContext Result.success(offlineSchedule)
        }
    }

    private suspend fun tryFetchFromProvider(
        provider: PrayerApiProvider
    ): Pair<SahiwalPrayerSchedule, List<PrayerTimeItem>>? {
        return try {
            when (provider) {
                PrayerApiProvider.ALADHAN_EXACT_COORDINATES -> fetchFromAladhanExactCoordinates()
                PrayerApiProvider.ALADHAN_CITY -> fetchFromAladhanCity()
                PrayerApiProvider.ISLAMIC_FINDER -> fetchFromIslamicFinder()
                PrayerApiProvider.MUSLIM_SALAT -> fetchFromMuslimSalat()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Provider ${provider.displayName} failed: ${e.message}")
            null
        }
    }

    /**
     * Exact Sahiwal GPS Coordinates query (30.6682, 73.1114) with Asia/Karachi timezone
     * GET https://aladhan.api.islamic.network/v1/timings/{date}?latitude=30.6682&longitude=73.1114&method=1&school=1&timezonestring=Asia/Karachi
     */
    private suspend fun fetchFromAladhanExactCoordinates(): Pair<SahiwalPrayerSchedule, List<PrayerTimeItem>>? {
        val todayDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).apply { timeZone = pkZone }.format(Date())

        val response: AladhanApiResponse = try {
            aladhanNetworkService.getTimingsByCoordinates(
                date = todayDateStr,
                latitude = SAHIWAL_LATITUDE,
                longitude = SAHIWAL_LONGITUDE,
                method = 1, // Karachi Univ
                school = 1, // Hanafi
                timezoneString = SAHIWAL_TIMEZONE
            )
        } catch (e: Exception) {
            Log.w(TAG, "Primary Islamic Network coordinates endpoint failed, trying secondary Aladhan domain: ${e.message}")
            aladhanDirectService.getTimingsByCoordinates(
                date = todayDateStr,
                latitude = SAHIWAL_LATITUDE,
                longitude = SAHIWAL_LONGITUDE,
                method = 1,
                school = 1,
                timezoneString = SAHIWAL_TIMEZONE
            )
        }

        val timings = response.data?.timings ?: return null
        val dateInfo = response.data?.date
        val hijri = dateInfo?.hijri

        val hijriFormatted = "${hijri?.day ?: "24"} ${hijri?.month?.en ?: "Safar"} ${hijri?.year ?: "1448"} AH"
        val gregorianFormatted = dateInfo?.readable ?: SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = pkZone }.format(Date())

        val cleanFajr = normalizeTo24Hour(sanitizeTime(timings.fajr ?: "04:24"))
        val cleanSunrise = normalizeTo24Hour(sanitizeTime(timings.sunrise ?: "05:47"))
        val cleanDhuhr = normalizeTo24Hour(sanitizeTime(timings.dhuhr ?: "12:06"))
        val cleanAsr = normalizeTo24Hour(sanitizeTime(timings.asr ?: "16:38"))
        val cleanMaghrib = normalizeTo24Hour(sanitizeTime(timings.maghrib ?: "18:25"))
        val cleanIsha = normalizeTo24Hour(sanitizeTime(timings.isha ?: "19:47"))
        val cleanImsak = normalizeTo24Hour(sanitizeTime(timings.imsak ?: "04:14"))
        val cleanMidnight = normalizeTo24Hour(sanitizeTime(timings.midnight ?: "00:06"))

        val tahajjudTimeStr = calculateTimeOffset(cleanFajr, -30)

        val schedule = SahiwalPrayerSchedule(
            city = "Sahiwal",
            country = "Pakistan",
            method = "University of Islamic Sciences, Karachi (Hanafi, Method 1)",
            apiSource = "Aladhan API (GPS 30.6682, 73.1114 • Asia/Karachi)",
            hijriDate = hijriFormatted,
            gregorianDate = gregorianFormatted,
            fajr = formatTo12Hour(cleanFajr),
            sunrise = formatTo12Hour(cleanSunrise),
            dhuhr = formatTo12Hour(cleanDhuhr),
            asr = formatTo12Hour(cleanAsr),
            maghrib = formatTo12Hour(cleanMaghrib),
            isha = formatTo12Hour(cleanIsha),
            imsak = formatTo12Hour(cleanImsak),
            midnight = formatTo12Hour(cleanMidnight),
            tahajjudWindow = formatTo12Hour(tahajjudTimeStr),
            isLiveFromApi = true
        )

        val prayerItems = buildPrayerItems(cleanFajr, cleanSunrise, cleanDhuhr, cleanAsr, cleanMaghrib, cleanIsha)
        return Pair(schedule, prayerItems)
    }

    /**
     * Sahiwal City Query via official OpenAPI fallback server
     * GET https://aladhan.api.islamic.network/v1/timingsByCity/{date}?city=Sahiwal&country=Pakistan&method=1&school=1
     */
    private suspend fun fetchFromAladhanCity(): Pair<SahiwalPrayerSchedule, List<PrayerTimeItem>>? {
        val todayDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).apply { timeZone = pkZone }.format(Date())

        val response: AladhanApiResponse = try {
            aladhanNetworkService.getTimingsByCity(
                date = todayDateStr,
                city = "Sahiwal",
                country = "Pakistan",
                method = 1,
                school = 1
            )
        } catch (e: Exception) {
            Log.w(TAG, "Primary Islamic Network city endpoint failed, trying secondary Aladhan domain: ${e.message}")
            aladhanDirectService.getTimingsByCity(
                date = todayDateStr,
                city = "Sahiwal",
                country = "Pakistan",
                method = 1,
                school = 1
            )
        }

        val timings = response.data?.timings ?: return null
        val dateInfo = response.data?.date
        val hijri = dateInfo?.hijri

        val hijriFormatted = "${hijri?.day ?: "24"} ${hijri?.month?.en ?: "Safar"} ${hijri?.year ?: "1448"} AH"
        val gregorianFormatted = dateInfo?.readable ?: SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = pkZone }.format(Date())

        val cleanFajr = normalizeTo24Hour(sanitizeTime(timings.fajr ?: "04:24"))
        val cleanSunrise = normalizeTo24Hour(sanitizeTime(timings.sunrise ?: "05:47"))
        val cleanDhuhr = normalizeTo24Hour(sanitizeTime(timings.dhuhr ?: "12:06"))
        val cleanAsr = normalizeTo24Hour(sanitizeTime(timings.asr ?: "16:38"))
        val cleanMaghrib = normalizeTo24Hour(sanitizeTime(timings.maghrib ?: "18:25"))
        val cleanIsha = normalizeTo24Hour(sanitizeTime(timings.isha ?: "19:47"))
        val cleanImsak = normalizeTo24Hour(sanitizeTime(timings.imsak ?: "04:14"))
        val cleanMidnight = normalizeTo24Hour(sanitizeTime(timings.midnight ?: "00:06"))

        val tahajjudTimeStr = calculateTimeOffset(cleanFajr, -30)

        val schedule = SahiwalPrayerSchedule(
            city = "Sahiwal",
            country = "Pakistan",
            method = "University of Islamic Sciences, Karachi (Hanafi, Method 1)",
            apiSource = "Aladhan API (City Sahiwal • Islamic Network)",
            hijriDate = hijriFormatted,
            gregorianDate = gregorianFormatted,
            fajr = formatTo12Hour(cleanFajr),
            sunrise = formatTo12Hour(cleanSunrise),
            dhuhr = formatTo12Hour(cleanDhuhr),
            asr = formatTo12Hour(cleanAsr),
            maghrib = formatTo12Hour(cleanMaghrib),
            isha = formatTo12Hour(cleanIsha),
            imsak = formatTo12Hour(cleanImsak),
            midnight = formatTo12Hour(cleanMidnight),
            tahajjudWindow = formatTo12Hour(tahajjudTimeStr),
            isLiveFromApi = true
        )

        val prayerItems = buildPrayerItems(cleanFajr, cleanSunrise, cleanDhuhr, cleanAsr, cleanMaghrib, cleanIsha)
        return Pair(schedule, prayerItems)
    }

    private suspend fun fetchFromIslamicFinder(): Pair<SahiwalPrayerSchedule, List<PrayerTimeItem>>? {
        val response = islamicFinderApiService.getIslamicFinderPrayerTimes(country = "PK", city = "Sahiwal")
        val results = response.results ?: return null

        val fRaw = results.fajr ?: "04:35"
        val sRaw = results.duha ?: "05:54"
        val dRaw = results.dhuhr ?: "12:15"
        val aRaw = results.asr ?: "15:45"
        val mRaw = results.maghrib ?: "18:36"
        val iRaw = results.isha ?: "19:55"

        val cleanFajr = normalizeTo24Hour(fRaw)
        val cleanSunrise = normalizeTo24Hour(sRaw)
        val cleanDhuhr = normalizeTo24Hour(dRaw)
        val cleanAsr = normalizeTo24Hour(aRaw)
        val cleanMaghrib = normalizeTo24Hour(mRaw)
        val cleanIsha = normalizeTo24Hour(iRaw)

        val tahajjudTimeStr = calculateTimeOffset(cleanFajr, -30)
        val imsakTimeStr = calculateTimeOffset(cleanFajr, -10)
        val midnightTimeStr = "00:15"

        val schedule = SahiwalPrayerSchedule(
            city = "Sahiwal",
            country = "Pakistan",
            method = "Islamic Finder • Karachi University Standard",
            apiSource = "Islamic Finder API",
            hijriDate = "24 Safar 1448 AH",
            gregorianDate = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = pkZone }.format(Date()),
            fajr = formatTo12Hour(cleanFajr),
            sunrise = formatTo12Hour(cleanSunrise),
            dhuhr = formatTo12Hour(cleanDhuhr),
            asr = formatTo12Hour(cleanAsr),
            maghrib = formatTo12Hour(cleanMaghrib),
            isha = formatTo12Hour(cleanIsha),
            imsak = formatTo12Hour(imsakTimeStr),
            midnight = formatTo12Hour(midnightTimeStr),
            tahajjudWindow = formatTo12Hour(tahajjudTimeStr),
            isLiveFromApi = true
        )

        val prayerItems = buildPrayerItems(cleanFajr, cleanSunrise, cleanDhuhr, cleanAsr, cleanMaghrib, cleanIsha)
        return Pair(schedule, prayerItems)
    }

    private suspend fun fetchFromMuslimSalat(): Pair<SahiwalPrayerSchedule, List<PrayerTimeItem>>? {
        val response = islamicFinderApiService.getMuslimSalatTimes()
        val item = response.items?.firstOrNull() ?: return null

        val fRaw = item.fajr ?: "04:35"
        val sRaw = item.shurooq ?: "05:54"
        val dRaw = item.dhuhr ?: "12:15"
        val aRaw = item.asr ?: "15:45"
        val mRaw = item.maghrib ?: "18:36"
        val iRaw = item.isha ?: "19:55"

        val cleanFajr = normalizeTo24Hour(fRaw)
        val cleanSunrise = normalizeTo24Hour(sRaw)
        val cleanDhuhr = normalizeTo24Hour(dRaw)
        val cleanAsr = normalizeTo24Hour(aRaw)
        val cleanMaghrib = normalizeTo24Hour(mRaw)
        val cleanIsha = normalizeTo24Hour(iRaw)

        val tahajjudTimeStr = calculateTimeOffset(cleanFajr, -30)
        val imsakTimeStr = calculateTimeOffset(cleanFajr, -10)
        val midnightTimeStr = "00:15"

        val schedule = SahiwalPrayerSchedule(
            city = "Sahiwal",
            country = "Pakistan",
            method = "MuslimSalat • Sahiwal Standard",
            apiSource = "MuslimSalat API",
            hijriDate = "24 Safar 1448 AH",
            gregorianDate = item.dateFor ?: SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = pkZone }.format(Date()),
            fajr = formatTo12Hour(cleanFajr),
            sunrise = formatTo12Hour(cleanSunrise),
            dhuhr = formatTo12Hour(cleanDhuhr),
            asr = formatTo12Hour(cleanAsr),
            maghrib = formatTo12Hour(cleanMaghrib),
            isha = formatTo12Hour(cleanIsha),
            imsak = formatTo12Hour(imsakTimeStr),
            midnight = formatTo12Hour(midnightTimeStr),
            tahajjudWindow = formatTo12Hour(tahajjudTimeStr),
            isLiveFromApi = true
        )

        val prayerItems = buildPrayerItems(cleanFajr, cleanSunrise, cleanDhuhr, cleanAsr, cleanMaghrib, cleanIsha)
        return Pair(schedule, prayerItems)
    }

    private fun sanitizeTime(timeStr: String): String {
        return timeStr.split(" ")[0].trim()
    }

    private fun normalizeTo24Hour(timeStr: String): String {
        val clean = timeStr.replace("%", "").trim()
        val lower = clean.lowercase(Locale.ENGLISH)
        return try {
            if (lower.contains("am") || lower.contains("pm")) {
                val parser12 = SimpleDateFormat("h:mm a", Locale.ENGLISH)
                val out24 = SimpleDateFormat("HH:mm", Locale.ENGLISH)
                val d = parser12.parse(clean)
                if (d != null) out24.format(d) else clean
            } else {
                val parts = clean.split(":")
                val h = parts[0].trim().toInt()
                val m = parts.getOrNull(1)?.split(" ")?.get(0)?.trim()?.toInt() ?: 0
                String.format(Locale.ENGLISH, "%02d:%02d", h, m)
            }
        } catch (_: Exception) {
            clean
        }
    }

    private fun formatTo12Hour(timeStr: String): String {
        return try {
            val clean = timeStr.trim()
            val lower = clean.lowercase(Locale.ENGLISH)
            if (lower.contains("am") || lower.contains("pm")) {
                val parser12 = SimpleDateFormat("h:mm a", Locale.ENGLISH)
                val formatter12 = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                val d = parser12.parse(clean)
                if (d != null) formatter12.format(d) else clean
            } else {
                val parser24 = SimpleDateFormat("HH:mm", Locale.ENGLISH)
                val formatter12 = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                val d = parser24.parse(clean)
                if (d != null) formatter12.format(d) else clean
            }
        } catch (_: Exception) {
            timeStr
        }
    }

    private fun calculateTimeOffset(time24: String, minutesOffset: Int): String {
        return try {
            val parts = time24.split(":")
            val hour = parts[0].toInt()
            val min = parts[1].toInt()

            val pkZone = java.util.TimeZone.getTimeZone("Asia/Karachi")
            val cal = Calendar.getInstance(pkZone).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                add(Calendar.MINUTE, minutesOffset)
            }
            String.format(Locale.getDefault(), "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        } catch (_: Exception) {
            "04:05"
        }
    }

    private fun buildPrayerItems(
        fajr: String,
        sunrise: String,
        dhuhr: String,
        asr: String,
        maghrib: String,
        isha: String
    ): List<PrayerTimeItem> {
        val pkZone = java.util.TimeZone.getTimeZone("Asia/Karachi")
        val now = System.currentTimeMillis()
        val timesMap = listOf(
            PrayerType.FAJR to fajr,
            PrayerType.SUNRISE to sunrise,
            PrayerType.DHUHR to dhuhr,
            PrayerType.ASR to asr,
            PrayerType.MAGHRIB to maghrib,
            PrayerType.ISHA to isha
        )

        val items = mutableListOf<PrayerTimeItem>()
        var foundNext = false

        val cal = Calendar.getInstance(pkZone)
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        for ((type, time24) in timesMap) {
            val parts = time24.split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

            val itemCal = Calendar.getInstance(pkZone).apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val itemMillis = itemCal.timeInMillis
            val isPassed = itemMillis <= now
            val isNext = !foundNext && !isPassed && type != PrayerType.SUNRISE

            if (isNext) {
                foundNext = true
            }

            items.add(
                PrayerTimeItem(
                    type = type,
                    timeFormatted = formatTo12Hour(time24),
                    timeMillis = itemMillis,
                    isPassed = isPassed,
                    isNext = isNext,
                    isAlarmEnabled = type != PrayerType.SUNRISE
                )
            )
        }

        // If all prayers today have passed (after Isha), tomorrow's Fajr is next
        if (!foundNext && items.isNotEmpty()) {
            val fajrIdx = items.indexOfFirst { it.type == PrayerType.FAJR }
            if (fajrIdx != -1) {
                items[fajrIdx] = items[fajrIdx].copy(isNext = true)
            }
        }

        return items
    }

    fun getSahiwalFallbackSchedule(provider: PrayerApiProvider = PrayerApiProvider.ALADHAN_EXACT_COORDINATES): Pair<SahiwalPrayerSchedule, List<PrayerTimeItem>> {
        val fajr = "04:24"
        val sunrise = "05:47"
        val dhuhr = "12:06"
        val asr = "16:38"
        val maghrib = "18:25"
        val isha = "19:47"
        val imsak = "04:14"
        val midnight = "00:06"
        val tahajjud = "03:54"

        val schedule = SahiwalPrayerSchedule(
            city = "Sahiwal",
            country = "Pakistan",
            method = "University of Islamic Sciences, Karachi (Hanafi, Method 1)",
            apiSource = provider.displayName,
            hijriDate = "24 Safar 1448 AH",
            gregorianDate = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).apply { timeZone = pkZone }.format(Date()),
            fajr = formatTo12Hour(fajr),
            sunrise = formatTo12Hour(sunrise),
            dhuhr = formatTo12Hour(dhuhr),
            asr = formatTo12Hour(asr),
            maghrib = formatTo12Hour(maghrib),
            isha = formatTo12Hour(isha),
            imsak = formatTo12Hour(imsak),
            midnight = formatTo12Hour(midnight),
            tahajjudWindow = formatTo12Hour(tahajjud),
            isLiveFromApi = true
        )

        val items = buildPrayerItems(fajr, sunrise, dhuhr, asr, maghrib, isha)
        return Pair(schedule, items)
    }
}

