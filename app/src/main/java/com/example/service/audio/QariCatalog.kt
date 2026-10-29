package com.example.service.audio

enum class QariId {
    ALAFASY,
    ABDUL_BASIT,
    AL_MUAIQLY,
    AL_GHAMDI,
    AL_MINSHAWI
}

data class QariProfile(
    val id: QariId,
    val nameEnglish: String,
    val nameArabic: String,
    val title: String,
    val style: String,
    val flag: String,
    val baseUrl: String,
    val ayahFolder: String
)

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
            ayahFolder = "Alafasy_128kbps"
        ),
        QariProfile(
            id = QariId.ABDUL_BASIT,
            nameEnglish = "Abdul Basit Abdul Samad",
            nameArabic = "عبد الباسط عبد الصمد",
            title = "Golden Voice of Egypt",
            style = "Murattal",
            flag = "🇪🇬",
            baseUrl = "https://download.quranicaudio.com/quran/abdul_baasit_murattal/",
            ayahFolder = "Abdul_Basit_Murattal_192kbps"
        ),
        QariProfile(
            id = QariId.AL_MUAIQLY,
            nameEnglish = "Maher Al-Muaiqly",
            nameArabic = "ماهر المعيقلي",
            title = "Imam of Masjid Al-Haram",
            style = "Haramain Makkah",
            flag = "🇸🇦",
            baseUrl = "https://download.quranicaudio.com/quran/maher_almu3aiqly/year1440/",
            ayahFolder = "MaherAlMuaiqly128kbps"
        ),
        QariProfile(
            id = QariId.AL_GHAMDI,
            nameEnglish = "Saad Al-Ghamdi",
            nameArabic = "سعد الغامدي",
            title = "Soulful & Emotional",
            style = "Murattal",
            flag = "🇸🇦",
            baseUrl = "https://download.quranicaudio.com/quran/sa3d_al-ghaamidee/complete/",
            ayahFolder = "Ghamadi_40kbps"
        ),
        QariProfile(
            id = QariId.AL_MINSHAWI,
            nameEnglish = "Muhammad Siddiq Al-Minshawi",
            nameArabic = "محمد صديق المنشاوي",
            title = "Master of Tajweed",
            style = "Murattal",
            flag = "🇪🇬",
            baseUrl = "https://download.quranicaudio.com/quran/muhammad_siddeeq_al-minshaawee/",
            ayahFolder = "Minshawy_Murattal_128kbps"
        )
    )

    fun getQariById(id: QariId): QariProfile {
        return qaris.firstOrNull { it.id == id } ?: qaris.first()
    }

    fun getSurahAudioUrl(qariId: QariId, surahId: Int): String {
        val qari = getQariById(qariId)
        val formatted = String.format(java.util.Locale.US, "%03d.mp3", surahId)
        return "${qari.baseUrl}$formatted"
    }

    fun getAyahAudioUrl(qariId: QariId, surahId: Int, ayahNumber: Int): String {
        val qari = getQariById(qariId)
        val fileName = String.format(java.util.Locale.US, "%03d%03d.mp3", surahId, ayahNumber)
        return "https://everyayah.com/data/${qari.ayahFolder}/$fileName"
    }
}
