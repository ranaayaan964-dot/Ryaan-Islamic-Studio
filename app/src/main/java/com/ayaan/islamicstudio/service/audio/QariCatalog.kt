package com.ayaan.islamicstudio.service.audio

import java.util.Locale

enum class QariId {
    ALAFASY,
    ABDUL_BASIT,
    ABDUL_BASIT_MUJAWWAD,
    SUDAIS,
    AL_MUAIQLY,
    AL_GHAMDI,
    AL_MINSHAWI,
    AL_HUSARY
}

data class QariProfile(
    val id: QariId,
    val nameEnglish: String,
    val nameArabic: String,
    val title: String,
    val style: String,
    val flag: String,
    val baseUrl: String,
    val everyAyahFolder: String,
    val islamicNetworkSlug: String,
    val mp3QuranBaseUrl: String
) {
    val ayahFolder: String get() = everyAyahFolder
}

object QariCatalog {
    val qaris = listOf(
        QariProfile(
            id = QariId.ALAFASY,
            nameEnglish = "Mishary Rashid Alafasy",
            nameArabic = "مشاري راشد العفاسي",
            title = "World Renowned Reciter",
            style = "Murattal",
            flag = "🇰🇼",
            baseUrl = "https://download.quranicaudio.com/quran/mishaari_raashid_al_3afaasee/",
            everyAyahFolder = "Alafasy_128kbps",
            islamicNetworkSlug = "ar.alafasy",
            mp3QuranBaseUrl = "https://server8.mp3quran.net/afs/"
        ),
        QariProfile(
            id = QariId.ABDUL_BASIT,
            nameEnglish = "Abdul Basit (Murattal)",
            nameArabic = "عبد الباسط عبد الصمد",
            title = "Golden Voice of Egypt",
            style = "Murattal",
            flag = "🇪🇬",
            baseUrl = "https://download.quranicaudio.com/quran/abdul_baasit_murattal/",
            everyAyahFolder = "Abdul_Basit_Murattal_192kbps",
            islamicNetworkSlug = "ar.abdulbasitmurattal",
            mp3QuranBaseUrl = "https://server8.mp3quran.net/basit/"
        ),
        QariProfile(
            id = QariId.ABDUL_BASIT_MUJAWWAD,
            nameEnglish = "Abdul Basit (Mujawwad)",
            nameArabic = "عبد الباسط عبد الصمد - مجود",
            title = "Egyptian Mujawwad Recitation",
            style = "Mujawwad",
            flag = "🇪🇬",
            baseUrl = "https://download.quranicaudio.com/quran/abdul_baasit_mujawwad/",
            everyAyahFolder = "Abdul_Basit_Mujawwad_128kbps",
            islamicNetworkSlug = "ar.abdulsamad",
            mp3QuranBaseUrl = "https://server8.mp3quran.net/basit_mjwd/"
        ),
        QariProfile(
            id = QariId.SUDAIS,
            nameEnglish = "Abdur-Rahman As-Sudais",
            nameArabic = "عبد الرحمن السديس",
            title = "Imam of Masjid Al-Haram",
            style = "Murattal",
            flag = "🇸🇦",
            baseUrl = "https://download.quranicaudio.com/quran/abdurrahman_as-sudais/",
            everyAyahFolder = "Abdurrahmaan_As-Sudais_192kbps",
            islamicNetworkSlug = "ar.abdurrahmaansudais",
            mp3QuranBaseUrl = "https://server8.mp3quran.net/sds/"
        ),
        QariProfile(
            id = QariId.AL_MUAIQLY,
            nameEnglish = "Maher Al-Muaiqly",
            nameArabic = "ماهر المعيقلي",
            title = "Imam of Masjid Al-Haram",
            style = "Haramain Makkah",
            flag = "🇸🇦",
            baseUrl = "https://download.quranicaudio.com/quran/maher_almu3aiqly/year1440/",
            everyAyahFolder = "MaherAlMuaiqly128kbps",
            islamicNetworkSlug = "ar.mahermuaiqly",
            mp3QuranBaseUrl = "https://server8.mp3quran.net/maher/"
        ),
        QariProfile(
            id = QariId.AL_GHAMDI,
            nameEnglish = "Saad Al-Ghamdi",
            nameArabic = "سعد الغامدي",
            title = "Soulful & Emotional",
            style = "Murattal",
            flag = "🇸🇦",
            baseUrl = "https://download.quranicaudio.com/quran/sa3d_al-ghaamidee/complete/",
            everyAyahFolder = "Ghamadi_40kbps",
            islamicNetworkSlug = "ar.saoodshuraym",
            mp3QuranBaseUrl = "https://server8.mp3quran.net/s_gmd/"
        ),
        QariProfile(
            id = QariId.AL_MINSHAWI,
            nameEnglish = "Muhammad Siddiq Al-Minshawi",
            nameArabic = "محمد صديق المنشاوي",
            title = "Master of Tajweed",
            style = "Murattal",
            flag = "🇪🇬",
            baseUrl = "https://download.quranicaudio.com/quran/muhammad_siddeeq_al-minshaawee/",
            everyAyahFolder = "Minshawy_Murattal_128kbps",
            islamicNetworkSlug = "ar.minshawi",
            mp3QuranBaseUrl = "https://server8.mp3quran.net/minsh/"
        ),
        QariProfile(
            id = QariId.AL_HUSARY,
            nameEnglish = "Mahmoud Khalil Al-Husary",
            nameArabic = "محمود خليل الحصري",
            title = "Master Tajweed Reciter",
            style = "Murattal",
            flag = "🇪🇬",
            baseUrl = "https://download.quranicaudio.com/quran/muhammad_al_husary/",
            everyAyahFolder = "Husary_128kbps",
            islamicNetworkSlug = "ar.husary",
            mp3QuranBaseUrl = "https://server8.mp3quran.net/husr/"
        )
    )

    fun getQariById(id: QariId): QariProfile =
        qaris.firstOrNull { it.id == id } ?: qaris.first()

    fun getSurahAudioUrl(qariId: QariId, surahId: Int): String {
        val qari = getQariById(qariId)
        val formatted = surahId.coerceIn(1, 114).toString().padStart(3, '0')
        return "${qari.baseUrl}$formatted.mp3"
    }

    fun buildAyahUrl(qari: QariProfile, surahNumber: Int, ayahNumberInSurah: Int): String {
        val surah = surahNumber.coerceIn(1, 114).toString().padStart(3, '0')
        val ayah = ayahNumberInSurah.coerceAtLeast(1).toString().padStart(3, '0')
        return "https://everyayah.com/data/${qari.everyAyahFolder}/${surah}${ayah}.mp3"
    }

    fun buildAyahFallbackUrl(qari: QariProfile, globalAyahNumber: Int): String =
        "https://cdn.islamic.network/quran/audio/128/${qari.islamicNetworkSlug}/${globalAyahNumber.coerceAtLeast(1)}.mp3"

    fun getAyahAudioUrl(qariId: QariId, surahId: Int, ayahNumber: Int): String =
        buildAyahUrl(getQariById(qariId), surahId, ayahNumber)

    fun getAyahAudioUrlFallback(qariId: QariId, globalAyahNumber: Int): String =
        buildAyahFallbackUrl(getQariById(qariId), globalAyahNumber)
}
