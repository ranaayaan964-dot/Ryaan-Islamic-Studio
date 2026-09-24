package com.example.data.repository

import com.example.data.model.DailyInspiration
import java.util.Calendar

object InspirationRepository {

    val inspirations = listOf(
        DailyInspiration(
            id = 1,
            arabicText = "إِنَّ مَعَ الْعُسْرِ يُسْرًا",
            englishText = "Indeed, with hardship comes ease.",
            source = "Surah Ash-Sharh (94:6)",
            category = "Quranic Verse"
        ),
        DailyInspiration(
            id = 2,
            arabicText = "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ",
            englishText = "So remember Me; I will remember you. And be grateful to Me and do not deny Me.",
            source = "Surah Al-Baqarah (2:152)",
            category = "Quranic Verse"
        ),
        DailyInspiration(
            id = 3,
            arabicText = "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ",
            englishText = "The best among you are those who learn the Quran and teach it.",
            source = "Sahih al-Bukhari (5027)",
            category = "Prophetic Hadith"
        ),
        DailyInspiration(
            id = 4,
            arabicText = "وَقَالَ رَبُّكُمُ ادْعُونِي أَسْتَجِبْ لَكُمْ",
            englishText = "And your Lord says: 'Call upon Me; I will respond to you.'",
            source = "Surah Ghafir (40:60)",
            category = "Quranic Verse"
        ),
        DailyInspiration(
            id = 5,
            arabicText = "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا",
            englishText = "Allah does not burden a soul beyond that it can bear.",
            source = "Surah Al-Baqarah (2:286)",
            category = "Quranic Verse"
        ),
        DailyInspiration(
            id = 6,
            arabicText = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
            englishText = "Unquestionably, by the remembrance of Allah hearts are assured.",
            source = "Surah Ar-Ra'd (13:28)",
            category = "Quranic Verse"
        ),
        DailyInspiration(
            id = 7,
            arabicText = "إِنَّمَا الْأَعْمَالُ بِالنِّيَّاتِ",
            englishText = "Actions are judged by motives and intentions.",
            source = "Sahih al-Bukhari & Muslim",
            category = "Prophetic Hadith"
        )
    )

    fun getInspirationForToday(): DailyInspiration {
        val pkZone = java.util.TimeZone.getTimeZone("Asia/Karachi")
        val dayOfYear = Calendar.getInstance(pkZone).get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % inspirations.size
        return inspirations[index]
    }
}
