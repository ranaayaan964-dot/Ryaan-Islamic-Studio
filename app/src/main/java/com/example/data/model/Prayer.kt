package com.example.data.model

enum class PrayerType(
    val displayName: String,
    val arabicName: String,
    val iconName: String
) {
    FAJR("Fajr", "الفجر", "ic_fajr"),
    SUNRISE("Sunrise", "الشروق", "ic_sunrise"),
    DHUHR("Dhuhr", "الظهر", "ic_dhuhr"),
    ASR("Asr", "العصر", "ic_asr"),
    MAGHRIB("Maghrib", "المغرب", "ic_maghrib"),
    ISHA("Isha", "العشاء", "ic_isha")
}

data class PrayerTimeItem(
    val type: PrayerType,
    val timeFormatted: String,
    val timeMillis: Long,
    val isPassed: Boolean = false,
    val isNext: Boolean = false,
    val isAlarmEnabled: Boolean = true
)

data class CountdownState(
    val nextPrayerName: String = "Fajr",
    val nextPrayerArabic: String = "الفجر",
    val formattedTime: String = "05:00 AM",
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val progress: Float = 0f
) {
    val displayTimer: String
        get() = String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

data class LocationInfo(
    val cityName: String = "Makkah",
    val countryName: String = "Saudi Arabia",
    val latitude: Double = 21.4225,
    val longitude: Double = 39.8262
)

data class DailyInspiration(
    val id: Int,
    val arabicText: String,
    val englishText: String,
    val source: String,
    val category: String
)

data class TasbeehDhikr(
    val id: String,
    val title: String,
    val arabic: String,
    val meaning: String,
    val defaultTarget: Int = 33
)
