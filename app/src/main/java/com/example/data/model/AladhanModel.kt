package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AladhanApiResponse(
    @Json(name = "code") val code: Int?,
    @Json(name = "status") val status: String?,
    @Json(name = "data") val data: AladhanData?
)

@JsonClass(generateAdapter = true)
data class AladhanData(
    @Json(name = "timings") val timings: AladhanTimings?,
    @Json(name = "date") val date: AladhanDate?,
    @Json(name = "meta") val meta: AladhanMeta?
)

@JsonClass(generateAdapter = true)
data class AladhanTimings(
    @Json(name = "Fajr") val fajr: String?,
    @Json(name = "Sunrise") val sunrise: String?,
    @Json(name = "Dhuhr") val dhuhr: String?,
    @Json(name = "Asr") val asr: String?,
    @Json(name = "Sunset") val sunset: String?,
    @Json(name = "Maghrib") val maghrib: String?,
    @Json(name = "Isha") val isha: String?,
    @Json(name = "Imsak") val imsak: String?,
    @Json(name = "Midnight") val midnight: String?,
    @Json(name = "Firstthird") val firstThird: String?,
    @Json(name = "Lastthird") val lastThird: String?
)

@JsonClass(generateAdapter = true)
data class AladhanDate(
    @Json(name = "readable") val readable: String?,
    @Json(name = "timestamp") val timestamp: String?,
    @Json(name = "hijri") val hijri: AladhanHijriDate?,
    @Json(name = "gregorian") val gregorian: AladhanGregorianDate?
)

@JsonClass(generateAdapter = true)
data class AladhanHijriDate(
    @Json(name = "date") val date: String?,
    @Json(name = "day") val day: String?,
    @Json(name = "month") val month: AladhanMonth?,
    @Json(name = "year") val year: String?
)

@JsonClass(generateAdapter = true)
data class AladhanGregorianDate(
    @Json(name = "date") val date: String?,
    @Json(name = "day") val day: String?,
    @Json(name = "month") val month: AladhanMonth?,
    @Json(name = "year") val year: String?
)

@JsonClass(generateAdapter = true)
data class AladhanMonth(
    @Json(name = "number") val number: Int?,
    @Json(name = "en") val en: String?,
    @Json(name = "ar") val ar: String?
)

@JsonClass(generateAdapter = true)
data class AladhanMeta(
    @Json(name = "latitude") val latitude: Double?,
    @Json(name = "longitude") val longitude: Double?,
    @Json(name = "timezone") val timezone: String?,
    @Json(name = "method") val method: AladhanMethod?
)

@JsonClass(generateAdapter = true)
data class AladhanMethod(
    @Json(name = "id") val id: Int?,
    @Json(name = "name") val name: String?
)

data class SahiwalPrayerSchedule(
    val city: String = "Sahiwal",
    val country: String = "Pakistan",
    val method: String = "University of Islamic Sciences, Karachi (Method 1)",
    val apiSource: String = "Islamic Finder API",
    val hijriDate: String = "24 Safar 1448 AH",
    val gregorianDate: String = "",
    val fajr: String = "04:35",
    val sunrise: String = "05:54",
    val dhuhr: String = "12:15",
    val asr: String = "15:45",
    val maghrib: String = "18:36",
    val isha: String = "19:55",
    val imsak: String = "04:25",
    val midnight: String = "00:15",
    val tahajjudWindow: String = "04:05", // 30 minutes before Fajr
    val isLiveFromApi: Boolean = true
)

@JsonClass(generateAdapter = true)
data class IslamicFinderResponse(
    @Json(name = "title") val title: String? = null,
    @Json(name = "results") val results: IslamicFinderResults? = null,
    @Json(name = "prayer_method_name") val prayerMethodName: String? = null
)

@JsonClass(generateAdapter = true)
data class IslamicFinderResults(
    @Json(name = "Fajr") val fajr: String? = null,
    @Json(name = "Duha") val duha: String? = null,
    @Json(name = "Dhuhr") val dhuhr: String? = null,
    @Json(name = "Asr") val asr: String? = null,
    @Json(name = "Maghrib") val maghrib: String? = null,
    @Json(name = "Isha") val isha: String? = null
)

@JsonClass(generateAdapter = true)
data class MuslimSalatResponse(
    @Json(name = "title") val title: String? = null,
    @Json(name = "items") val items: List<MuslimSalatItem>? = null
)

@JsonClass(generateAdapter = true)
data class MuslimSalatItem(
    @Json(name = "date_for") val dateFor: String? = null,
    @Json(name = "fajr") val fajr: String? = null,
    @Json(name = "shurooq") val shurooq: String? = null,
    @Json(name = "dhuhr") val dhuhr: String? = null,
    @Json(name = "asr") val asr: String? = null,
    @Json(name = "maghrib") val maghrib: String? = null,
    @Json(name = "isha") val isha: String? = null
)

