package com.example.audio

enum class SoundCategory(val displayName: String, val arabicName: String) {
    GLOBAL_ADHANS("Global Adhans", "الأذان حول العالم"),
    SHORT_REMINDERS("Short Reminders", "أذكار وتذكيرات قصيرة"),
    PREMIUM_NAATS("Premium Naats", "المدائح والقصائد"),
    TAHAJJUD_ZEN("Tahajjud & Zen", "أجواء التهجد والسكينة")
}

enum class SoundEvent(
    val eventId: String,
    val title: String,
    val subtitle: String,
    val defaultSoundId: String
) {
    FAJR(
        eventId = "fajr_prayer",
        title = "Fajr Prayer Alarm",
        subtitle = "Includes 'As-Salatu Khairum Minan Naum'",
        defaultSoundId = "fajr_special"
    ),
    REGULAR(
        eventId = "regular_prayers",
        title = "Regular Prayers (Dhuhr, Asr, Maghrib, Isha)",
        subtitle = "Customizable to any Adhan or spiritual Naat",
        defaultSoundId = "standard_adhan"
    ),
    PRE_PRAYER(
        eventId = "pre_prayer_reminder",
        title = "15-Minute Pre-Prayer Reminder",
        subtitle = "Gentle Zikr alert for Wudu preparation",
        defaultSoundId = "subhanallah_reminder"
    ),
    TAHAJJUD(
        eventId = "tahajjud_wakeup",
        title = "Tahajjud Wake-Up Alarm",
        subtitle = "Gentle spiritual dawn awakening",
        defaultSoundId = "tahajjud_chime"
    )
}

data class IslamicSoundItem(
    val id: String,
    val title: String,
    val arabicTitle: String,
    val artistOrOrigin: String,
    val category: SoundCategory,
    val durationFormatted: String,
    val isBundledRaw: Boolean,
    val rawResName: String? = null,
    val remoteAudioUrl: String,
    val description: String,
    val accentColorHex: Long = 0xFFE5C07B
)

data class SoundAssignmentState(
    val event: SoundEvent,
    val selectedSoundId: String,
    val selectedSoundTitle: String,
    val selectedSoundUri: String
)
