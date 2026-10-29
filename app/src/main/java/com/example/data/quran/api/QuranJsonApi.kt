package com.example.data.quran.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.quran.model.JuzCatalog
import com.example.data.quran.seed.QuranSeedData
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Data model for Surah summary metadata.
 */
data class SurahInfo(
    val number: Int,
    val name: String,
    val englishName: String,
    val englishNameTranslation: String,
    val numberOfAyahs: Int,
    val revelationType: String
)

/**
 * Data model for an individual Ayah (Verse).
 */
data class AyahData(
    val numberInSurah: Int,
    val globalNumber: Int,
    val arabicText: String,
    val englishTranslation: String,
    val audioUrl: String,
    val surahNumber: Int = 1,
    val surahNameArabic: String = "الفاتحة",
    val surahNameEnglish: String = "Al-Fatihah",
    val juzNumber: Int = 1,
    val urduTranslation: String = ""
)

/**
 * Complete Surah data including its metadata and all Ayahs.
 */
data class SurahFullData(
    val info: SurahInfo,
    val ayahs: List<AyahData>
)

/**
 * Network and caching engine that fetches Surah data from a JSON API
 * (api.alquran.cloud / quran.com editions) and parses Arabic Uthmani text
 * and English Sahih International translations.
 */
object QuranJsonApi {
    private const val TAG = "QuranJsonApi"
    private const val PRIMARY_API_BASE = "https://api.alquran.cloud/v1/surah"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // In-memory cache to guarantee instant reloading without redundant network calls
    private val surahCache = ConcurrentHashMap<Int, SurahFullData>()
    private val tafsirCache = ConcurrentHashMap<String, String>()

    /**
     * Fetches complete Surah data from the JSON API.
     * Caches the result in memory upon success.
     */
    suspend fun fetchSurah(surahNumber: Int): Result<SurahFullData> = withContext(Dispatchers.IO) {
        val clampedNumber = surahNumber.coerceIn(1, 114)

        // Return from memory cache if already fetched
        surahCache[clampedNumber]?.let {
            return@withContext Result.success(it)
        }

        val url = "$PRIMARY_API_BASE/$clampedNumber/editions/quran-uthmani,en.sahih,ur.jalandhry"
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    Log.w(TAG, "API failed with code ${response.code}. Falling back to pre-seeded dataset.")
                    val fallback = getOfflineFallback(clampedNumber)
                    return@withContext Result.success(fallback)
                }

                val json = JSONObject(responseBody)
                val status = json.optString("status", "")
                if (status != "OK") {
                    val fallback = getOfflineFallback(clampedNumber)
                    return@withContext Result.success(fallback)
                }

                val dataArray = json.getJSONArray("data")
                if (dataArray.length() < 3) {
                    throw IllegalStateException("Quran API response did not include Arabic, English, and Urdu editions")
                }
                val uthmaniEdition = dataArray.getJSONObject(0)
                val englishEdition = if (dataArray.length() > 1) dataArray.getJSONObject(1) else null
                val urduEdition = if (dataArray.length() > 2) dataArray.getJSONObject(2) else null

                val surahMeta = SurahInfo(
                    number = uthmaniEdition.getInt("number"),
                    name = uthmaniEdition.getString("name"),
                    englishName = uthmaniEdition.getString("englishName"),
                    englishNameTranslation = uthmaniEdition.getString("englishNameTranslation"),
                    numberOfAyahs = uthmaniEdition.getInt("numberOfAyahs"),
                    revelationType = uthmaniEdition.getString("revelationType")
                )

                val uthmaniAyahs = uthmaniEdition.getJSONArray("ayahs")
                val englishAyahs = englishEdition?.getJSONArray("ayahs")
                val urduAyahs = urduEdition?.getJSONArray("ayahs")
                if (englishAyahs == null || urduAyahs == null ||
                    uthmaniAyahs.length() != surahMeta.numberOfAyahs ||
                    englishAyahs.length() != surahMeta.numberOfAyahs ||
                    urduAyahs.length() != surahMeta.numberOfAyahs
                ) {
                    throw IllegalStateException("Quran API returned incomplete Surah editions")
                }

                val ayahsList = mutableListOf<AyahData>()
                val surahStr = clampedNumber.toString().padStart(3, '0')

                for (i in 0 until uthmaniAyahs.length()) {
                    val uObj = uthmaniAyahs.getJSONObject(i)
                    val globalNum = uObj.getInt("number")
                    val numInSurah = uObj.getInt("numberInSurah")
                    val arabic = uObj.getString("text")
                    val english = englishAyahs.optJSONObject(i)?.optString("text").orEmpty()
                    val urdu = urduAyahs.optJSONObject(i)?.optString("text").orEmpty()
                    if (arabic.isBlank() || english.isBlank() || urdu.isBlank()) {
                        throw IllegalStateException("Quran API returned an empty edition at Ayah $numInSurah")
                    }

                    val ayahStr = numInSurah.toString().padStart(3, '0')
                    val audio = "https://everyayah.com/data/Alafasy_128kbps/$surahStr$ayahStr.mp3"

                    ayahsList.add(
                        AyahData(
                            numberInSurah = numInSurah,
                            globalNumber = globalNum,
                            arabicText = cleanArabicBismillahPrefix(arabic, clampedNumber, numInSurah),
                            englishTranslation = english,
                            audioUrl = audio,
                            surahNumber = clampedNumber,
                            surahNameArabic = surahMeta.name,
                            surahNameEnglish = surahMeta.englishName,
                            juzNumber = JuzCatalog.getJuzForAyah(clampedNumber, numInSurah),
                            urduTranslation = urdu
                        )
                    )
                }

                val fullData = SurahFullData(info = surahMeta, ayahs = ayahsList.sortedBy { it.numberInSurah })
                surahCache[clampedNumber] = fullData
                Result.success(fullData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network exception fetching Surah $clampedNumber: ${e.message}", e)
            val fallback = getOfflineFallback(clampedNumber)
            Result.success(fallback)
        }
    }

    suspend fun fetchJuz(juzNumber: Int): Result<List<AyahData>> = withContext(Dispatchers.IO) {
        val juz = JuzCatalog.getJuzById(juzNumber.coerceIn(1, 30))
            ?: return@withContext Result.failure(IllegalArgumentException("Unknown Juz $juzNumber"))
        val ayahs = mutableListOf<AyahData>()
        for (surahId in juz.startSurahId..juz.endSurahId) {
            val surah = fetchSurah(surahId).getOrElse { return@withContext Result.failure(it) }
            if (surah.ayahs.size != surah.info.numberOfAyahs ||
                surah.ayahs.any { it.arabicText.isBlank() || it.urduTranslation.isBlank() }
            ) {
                return@withContext Result.failure(
                    IllegalStateException("Complete Quran text is unavailable for Surah ${surah.info.englishName}")
                )
            }
            val surahAyahs = surah.ayahs.filter { ayah ->
                when {
                    surahId == juz.startSurahId && surahId == juz.endSurahId ->
                        ayah.numberInSurah in juz.startVerse..juz.endVerse
                    surahId == juz.startSurahId -> ayah.numberInSurah >= juz.startVerse
                    surahId == juz.endSurahId -> ayah.numberInSurah <= juz.endVerse
                    else -> true
                }
            }
            ayahs.addAll(surahAyahs)
        }
        Result.success(ayahs.sortedWith(compareBy(AyahData::surahNumber, AyahData::numberInSurah)))
    }

    suspend fun fetchTafsirAlMuyassar(surahNumber: Int, ayahNumber: Int): Result<String> = withContext(Dispatchers.IO) {
        if (surahNumber !in 1..114 || ayahNumber < 1) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Surah or Ayah number"))
        }
        val cacheKey = "$surahNumber:$ayahNumber"
        tafsirCache[cacheKey]?.let { return@withContext Result.success(it) }

        val request = Request.Builder()
            .url("https://api.alquran.cloud/v1/ayah/$cacheKey/ar.muyassar")
            .addHeader("Accept", "application/json")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("Tafsir API returned HTTP ${response.code}")
                    )
                }
                val data = JSONObject(responseBody).optJSONObject("data")
                    ?: return@withContext Result.failure(IllegalStateException("Tafsir response has no data"))
                val edition = data.optJSONObject("edition")
                val editionType = edition?.optString("type")
                val commentary = data.optString("text").trim()
                val responseSurah = data.optJSONObject("surah")?.optInt("number")
                if (editionType != "tafsir" || edition?.optString("identifier") != "ar.muyassar" ||
                    responseSurah != surahNumber || data.optInt("numberInSurah") != ayahNumber ||
                    commentary.isBlank()
                ) {
                    return@withContext Result.failure(IllegalStateException("Tafsir response is incomplete"))
                }
                tafsirCache[cacheKey] = commentary
                Result.success(commentary)
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Network exception fetching Tafsir for $cacheKey: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun cacheSurah(data: SurahFullData) {
        surahCache[data.info.number] = data
    }

    /**
     * In some Uthmani editions, the Bismillah is prefixed to the text of the first ayah.
     * We strip it from verse 1 for surahs > 1 because the reader displays a dedicated ornamental
     * Bismillah banner at the top of the Surah.
     */
    private fun cleanArabicBismillahPrefix(arabic: String, surahNum: Int, verseNum: Int): String {
        val text = arabic.removePrefix("\uFEFF")
        if (surahNum in 2..114 && surahNum != 9 && verseNum == 1) {
            val prefixes = listOf(
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
                "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
            )
            val prefix = prefixes.firstOrNull { text.startsWith(it) }
            if (prefix != null) {
                val cleaned = text.removePrefix(prefix).trim()
                return if (cleaned.isNotEmpty()) cleaned else text
            }
        }
        return text
    }

    /**
     * Offline fallback dataset for popular Surahs to guarantee instant availability.
     */
    fun getOfflineFallback(surahNumber: Int): SurahFullData {
        val info = ALL_114_SURAHS.firstOrNull { it.number == surahNumber }
            ?: SurahInfo(surahNumber, "سُورَة", "Surah $surahNumber", "Surah $surahNumber", 7, "Meccan")

        val ayahs = when (surahNumber) {
            1 -> listOf(
                AyahData(1, 1, "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "https://everyayah.com/data/Alafasy_128kbps/001001.mp3"),
                AyahData(2, 2, "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَٰلَمِينَ", "[All] praise is [due] to Allah, Lord of the worlds -", "https://everyayah.com/data/Alafasy_128kbps/001002.mp3"),
                AyahData(3, 3, "ٱلرَّحْمَٰنِ ٱلرَّحِيمِ", "The Entirely Merciful, the Especially Merciful,", "https://everyayah.com/data/Alafasy_128kbps/001003.mp3"),
                AyahData(4, 4, "مَٰلِكِ يَوْمِ ٱلدِّينِ", "Sovereign of the Day of Recompense.", "https://everyayah.com/data/Alafasy_128kbps/001004.mp3"),
                AyahData(5, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "It is You we worship and You we ask for help.", "https://everyayah.com/data/Alafasy_128kbps/001005.mp3"),
                AyahData(6, 6, "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", "Guide us to the straight path -", "https://everyayah.com/data/Alafasy_128kbps/001006.mp3"),
                AyahData(7, 7, "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ", "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray.", "https://everyayah.com/data/Alafasy_128kbps/001007.mp3")
            )
            112 -> listOf(
                AyahData(1, 6222, "قُلْ هُوَ ٱللَّهُ أَحَدٌ", "Say, \"He is Allah, [who is] One,", "https://everyayah.com/data/Alafasy_128kbps/112001.mp3"),
                AyahData(2, 6223, "ٱللَّهُ ٱلصَّمَدُ", "Allah, the Eternal Refuge.", "https://everyayah.com/data/Alafasy_128kbps/112002.mp3"),
                AyahData(3, 6224, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "He neither begets nor is born,", "https://everyayah.com/data/Alafasy_128kbps/112003.mp3"),
                AyahData(4, 6225, "وَلَمْ يَكُن لَّهُۥ كُفُوًا أَحَدٌۢ", "Nor is there to Him any equivalent.\"", "https://everyayah.com/data/Alafasy_128kbps/112004.mp3")
            )
            113 -> listOf(
                AyahData(1, 6226, "قُلْ أَعُوذُ بِرَبِّ ٱلْفَلَقِ", "Say, \"I seek refuge in the Lord of daybreak", "https://everyayah.com/data/Alafasy_128kbps/113001.mp3"),
                AyahData(2, 6227, "مِن شَرِّ مَا خَلَقَ", "From the evil of that which He created", "https://everyayah.com/data/Alafasy_128kbps/113002.mp3"),
                AyahData(3, 6228, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "And from the evil of darkness when it settles", "https://everyayah.com/data/Alafasy_128kbps/113003.mp3"),
                AyahData(4, 6229, "وَمِن شَرِّ ٱلنَّفَّٰثَٰتِ فِى ٱلْعُقَدِ", "And from the evil of the blowers in knots", "https://everyayah.com/data/Alafasy_128kbps/113004.mp3"),
                AyahData(5, 6230, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "And from the evil of an envier when he envies.\"", "https://everyayah.com/data/Alafasy_128kbps/113005.mp3")
            )
            114 -> listOf(
                AyahData(1, 6231, "قُلْ أَعُوذُ بِرَبِّ ٱلنَّاسِ", "Say, \"I seek refuge in the Lord of mankind,", "https://everyayah.com/data/Alafasy_128kbps/114001.mp3"),
                AyahData(2, 6232, "مَلِكِ ٱلنَّاسِ", "The Sovereign of mankind,", "https://everyayah.com/data/Alafasy_128kbps/114002.mp3"),
                AyahData(3, 6233, "إِلَٰهِ ٱلنَّاسِ", "The God of mankind,", "https://everyayah.com/data/Alafasy_128kbps/114003.mp3"),
                AyahData(4, 6234, "مِن شَرِّ ٱلْوَسْوَاسِ ٱلْخَنَّاسِ", "From the evil of the retreating whisperer -", "https://everyayah.com/data/Alafasy_128kbps/114004.mp3"),
                AyahData(5, 6235, "ٱلَّذِى يُوَسْوِسُ فِى صُدُورِ ٱلنَّاسِ", "Who whispers into the breasts of mankind -", "https://everyayah.com/data/Alafasy_128kbps/114005.mp3"),
                AyahData(6, 6236, "مِنَ ٱلْجِنَّةِ وَٱلنَّاسِ", "From among the jinn and mankind.\"", "https://everyayah.com/data/Alafasy_128kbps/114006.mp3")
            )
            else -> QuranSeedData.getInitialAyahs()
                .filter { it.surahId == surahNumber }
                .map { ayah ->
                    val surahPad = surahNumber.toString().padStart(3, '0')
                    val ayahPad = ayah.verseNumber.toString().padStart(3, '0')
                    AyahData(
                        numberInSurah = ayah.verseNumber,
                        globalNumber = ayah.globalVerseNumber,
                        arabicText = ayah.textArabic,
                        englishTranslation = ayah.textEnglishTranslation,
                        audioUrl = "https://everyayah.com/data/Alafasy_128kbps/$surahPad$ayahPad.mp3",
                        surahNumber = surahNumber,
                        surahNameArabic = info.name,
                        surahNameEnglish = info.englishName,
                        juzNumber = JuzCatalog.getJuzForAyah(surahNumber, ayah.verseNumber),
                        urduTranslation = ayah.textUrduTranslation
                    )
                }
        }

        val seedUrduByVerse = QuranSeedData.getInitialAyahs()
            .filter { it.surahId == surahNumber }
            .associateBy { it.verseNumber }
        val surahArabicName = info.name
        return SurahFullData(
            info,
            ayahs.map { ayah ->
                ayah.copy(
                    surahNumber = surahNumber,
                    surahNameArabic = surahArabicName,
                    surahNameEnglish = info.englishName,
                    juzNumber = JuzCatalog.getJuzForAyah(surahNumber, ayah.numberInSurah),
                    urduTranslation = ayah.urduTranslation.ifBlank {
                        seedUrduByVerse[ayah.numberInSurah]?.textUrduTranslation.orEmpty()
                    }
                )
            }
        )
    }

    /**
     * Complete index of all 114 Surahs of the Noble Quran.
     */
    val ALL_114_SURAHS = listOf(
        SurahInfo(1, "سُورَةُ ٱلْفَاتِحَةِ", "Al-Fatihah", "The Opening", 7, "Meccan"),
        SurahInfo(2, "سُورَةُ البَقَرَةِ", "Al-Baqarah", "The Cow", 286, "Medinan"),
        SurahInfo(3, "سُورَةُ آلِ عِمْرَانَ", "Ali 'Imran", "Family of Imran", 200, "Medinan"),
        SurahInfo(4, "سُورَةُ النِّسَاءِ", "An-Nisa", "The Women", 176, "Medinan"),
        SurahInfo(5, "سُورَةُ المَائِدَةِ", "Al-Ma'idah", "The Table Spread", 120, "Medinan"),
        SurahInfo(6, "سُورَةُ الأَنْعَامِ", "Al-An'am", "The Cattle", 165, "Meccan"),
        SurahInfo(7, "سُورَةُ الأَعْرَافِ", "Al-A'raf", "The Heights", 206, "Meccan"),
        SurahInfo(8, "سُورَةُ الأَنْفَالِ", "Al-Anfal", "The Spoils of War", 75, "Medinan"),
        SurahInfo(9, "سُورَةُ التَّوْبَةِ", "At-Tawbah", "The Repentance", 129, "Medinan"),
        SurahInfo(10, "سُورَةُ يُونُسَ", "Yunus", "Jonah", 109, "Meccan"),
        SurahInfo(11, "سُورَةُ هُودٍ", "Hud", "Hud", 123, "Meccan"),
        SurahInfo(12, "سُورَةُ يُوسُفَ", "Yusuf", "Joseph", 111, "Meccan"),
        SurahInfo(13, "سُورَةُ الرَّعْدِ", "Ar-Ra'd", "The Thunder", 43, "Medinan"),
        SurahInfo(14, "سُورَةُ إِبْرَاهِيمَ", "Ibrahim", "Abraham", 52, "Meccan"),
        SurahInfo(15, "سُورَةُ الحِجْرِ", "Al-Hijr", "The Rocky Tract", 99, "Meccan"),
        SurahInfo(16, "سُورَةُ النَّحْلِ", "An-Nahl", "The Bee", 128, "Meccan"),
        SurahInfo(17, "سُورَةُ الإِسْرَاءِ", "Al-Isra", "The Night Journey", 111, "Meccan"),
        SurahInfo(18, "سُورَةُ الكَهْفِ", "Al-Kahf", "The Cave", 110, "Meccan"),
        SurahInfo(19, "سُورَةُ مَرْيَمَ", "Maryam", "Mary", 98, "Meccan"),
        SurahInfo(20, "سُورَةُ طٰهٰ", "Taha", "Ta-Ha", 135, "Meccan"),
        SurahInfo(21, "سُورَةُ الأَنْبِيَاءِ", "Al-Anbiya", "The Prophets", 112, "Meccan"),
        SurahInfo(22, "سُورَةُ الحَجِّ", "Al-Hajj", "The Pilgrimage", 78, "Medinan"),
        SurahInfo(23, "سُورَةُ المُؤْمِنُونَ", "Al-Mu'minun", "The Believers", 118, "Meccan"),
        SurahInfo(24, "سُورَةُ النُّورِ", "An-Nur", "The Light", 64, "Medinan"),
        SurahInfo(25, "سُورَةُ الفُرْقَانِ", "Al-Furqan", "The Criterion", 77, "Meccan"),
        SurahInfo(26, "سُورَةُ الشُّعَرَاءِ", "Ash-Shu'ara", "The Poets", 227, "Meccan"),
        SurahInfo(27, "سُورَةُ النَّمْلِ", "An-Naml", "The Ant", 93, "Meccan"),
        SurahInfo(28, "سُورَةُ القَصَصِ", "Al-Qasas", "The Stories", 88, "Meccan"),
        SurahInfo(29, "سُورَةُ العَنْكَبُوتِ", "Al-'Ankabut", "The Spider", 69, "Meccan"),
        SurahInfo(30, "سُورَةُ الرُّومِ", "Ar-Rum", "The Romans", 60, "Meccan"),
        SurahInfo(31, "سُورَةُ لُقْمَانَ", "Luqman", "Luqman", 34, "Meccan"),
        SurahInfo(32, "سُورَةُ السَّجْدَةِ", "As-Sajdah", "The Prostration", 30, "Meccan"),
        SurahInfo(33, "سُورَةُ الأَحْزَابِ", "Al-Ahzab", "The Combined Forces", 73, "Medinan"),
        SurahInfo(34, "سُورَةُ سَبَإٍ", "Saba", "Sheba", 54, "Meccan"),
        SurahInfo(35, "سُورَةُ فَاطِرٍ", "Fatir", "Originator", 45, "Meccan"),
        SurahInfo(36, "سُورَةُ يٰسٓ", "Ya-Sin", "Ya Sin", 83, "Meccan"),
        SurahInfo(37, "سُورَةُ الصَّافَّاتِ", "As-Saffat", "Those Who Set The Ranks", 182, "Meccan"),
        SurahInfo(38, "سُورَةُ صٓ", "Sad", "The Letter Sad", 88, "Meccan"),
        SurahInfo(39, "سُورَةُ الزُّمَرِ", "Az-Zumar", "The Troops", 75, "Meccan"),
        SurahInfo(40, "سُورَةُ غَافِرٍ", "Ghafir", "The Forgiver", 85, "Meccan"),
        SurahInfo(41, "سُورَةُ فُصِّلَتْ", "Fussilat", "Explained In Detail", 54, "Meccan"),
        SurahInfo(42, "سُورَةُ الشُّورَىٰ", "Ash-Shura", "The Consultation", 53, "Meccan"),
        SurahInfo(43, "سُورَةُ الزُّخْرُفِ", "Az-Zukhruf", "The Ornaments of Gold", 89, "Meccan"),
        SurahInfo(44, "سُورَةُ الدُّخَانِ", "Ad-Dukhan", "The Smoke", 59, "Meccan"),
        SurahInfo(45, "سُورَةُ الجَاثِيَةِ", "Al-Jathiyah", "The Crouching", 37, "Meccan"),
        SurahInfo(46, "سُورَةُ الأَحْقَافِ", "Al-Ahqaf", "The Wind-Curved Sandhills", 35, "Meccan"),
        SurahInfo(47, "سُورَةُ مُحَمَّدٍ", "Muhammad", "Muhammad", 38, "Medinan"),
        SurahInfo(48, "سُورَةُ الفَتْحِ", "Al-Fath", "The Victory", 29, "Medinan"),
        SurahInfo(49, "سُورَةُ الحُجُرَاتِ", "Al-Hujurat", "The Rooms", 18, "Medinan"),
        SurahInfo(50, "سُورَةُ قٓ", "Qaf", "The Letter Qaf", 45, "Meccan"),
        SurahInfo(51, "سُورَةُ الذَّارِيَاتِ", "Adh-Dhariyat", "The Winnowing Winds", 60, "Meccan"),
        SurahInfo(52, "سُورَةُ الطُّورِ", "At-Tur", "The Mount", 49, "Meccan"),
        SurahInfo(53, "سُورَةُ النَّجْمِ", "An-Najm", "The Star", 62, "Meccan"),
        SurahInfo(54, "سُورَةُ القَمَرِ", "Al-Qamar", "The Moon", 55, "Meccan"),
        SurahInfo(55, "سُورَةُ الرَّحْمَٰنِ", "Ar-Rahman", "The Beneficent", 78, "Medinan"),
        SurahInfo(56, "سُورَةُ الوَاقِعَةِ", "Al-Waqi'ah", "The Inevitable", 96, "Meccan"),
        SurahInfo(57, "سُورَةُ الحَدِيدِ", "Al-Hadid", "The Iron", 29, "Medinan"),
        SurahInfo(58, "سُورَةُ المُجَادَلَةِ", "Al-Mujadila", "The Pleading Woman", 22, "Medinan"),
        SurahInfo(59, "سُورَةُ الحَشْرِ", "Al-Hashr", "The Exile", 24, "Medinan"),
        SurahInfo(60, "سُورَةُ المُمْتَحَنَةِ", "Al-Mumtahanah", "She That Is To Be Examined", 13, "Medinan"),
        SurahInfo(61, "سُورَةُ الصَّفِّ", "As-Saf", "The Ranks", 14, "Medinan"),
        SurahInfo(62, "سُورَةُ الجُمُعَةِ", "Al-Jumu'ah", "Friday", 11, "Medinan"),
        SurahInfo(63, "سُورَةُ المُنَافِقُونَ", "Al-Munafiqun", "The Hypocrites", 11, "Medinan"),
        SurahInfo(64, "سُورَةُ التَّغَابُنِ", "At-Taghabun", "Mutual Disillusion", 18, "Medinan"),
        SurahInfo(65, "سُورَةُ الطَّلَاقِ", "At-Talaq", "The Divorce", 12, "Medinan"),
        SurahInfo(66, "سُورَةُ التَّحْرِيمِ", "At-Tahrim", "The Prohibition", 12, "Medinan"),
        SurahInfo(67, "سُورَةُ المُلْكِ", "Al-Mulk", "The Sovereignty", 30, "Meccan"),
        SurahInfo(68, "سُورَةُ القَلَمِ", "Al-Qalam", "The Pen", 52, "Meccan"),
        SurahInfo(69, "سُورَةُ الحَاقَّةِ", "Al-Haqqah", "The Reality", 52, "Meccan"),
        SurahInfo(70, "سُورَةُ المَعَارِجِ", "Al-Ma'arij", "The Ascending Stairways", 44, "Meccan"),
        SurahInfo(71, "سُورَةُ نُوحٍ", "Nuh", "Noah", 28, "Meccan"),
        SurahInfo(72, "سُورَةُ الجِنِّ", "Al-Jinn", "The Jinn", 28, "Meccan"),
        SurahInfo(73, "سُورَةُ المُزَّمِّلِ", "Al-Muzzammil", "The Enshrouded One", 20, "Meccan"),
        SurahInfo(74, "سُورَةُ المُدَّثِّرِ", "Al-Muddaththir", "The Cloaked One", 56, "Meccan"),
        SurahInfo(75, "سُورَةُ القِيَامَةِ", "Al-Qiyamah", "The Resurrection", 40, "Meccan"),
        SurahInfo(76, "سُورَةُ الإِنْسَانِ", "Al-Insan", "Man", 31, "Medinan"),
        SurahInfo(77, "سُورَةُ المُرْسَلَاتِ", "Al-Mursalat", "The Emissaries", 50, "Meccan"),
        SurahInfo(78, "سُورَةُ النَّبَإِ", "An-Naba", "The Tidings", 40, "Meccan"),
        SurahInfo(79, "سُورَةُ النَّازِعَاتِ", "An-Nazi'at", "Those Who Drag Forth", 46, "Meccan"),
        SurahInfo(80, "سُورَةُ عَبَسَ", "'Abasa", "He Frowned", 42, "Meccan"),
        SurahInfo(81, "سُورَةُ التَّكْوِيرِ", "At-Takwir", "The Overthrowing", 29, "Meccan"),
        SurahInfo(82, "سُورَةُ الإِنْفِطَارِ", "Al-Infitar", "The Cleaving", 19, "Meccan"),
        SurahInfo(83, "سُورَةُ المُطَفِّفِينَ", "Al-Mutaffifin", "Defrauding", 36, "Meccan"),
        SurahInfo(84, "سُورَةُ الإِنْشِقَاقِ", "Al-Inshiqaq", "The Splitting Open", 25, "Meccan"),
        SurahInfo(85, "سُورَةُ البُرُوجِ", "Al-Buruj", "The Mansions of the Stars", 22, "Meccan"),
        SurahInfo(86, "سُورَةُ الطَّارِقِ", "At-Tariq", "The Morning Star", 17, "Meccan"),
        SurahInfo(87, "سُورَةُ الأَعْلَىٰ", "Al-A'la", "The Most High", 19, "Meccan"),
        SurahInfo(88, "سُورَةُ الغَاشِيَةِ", "Al-Ghashiyah", "The Overwhelming", 26, "Meccan"),
        SurahInfo(89, "سُورَةُ الفَجْرِ", "Al-Fajr", "The Dawn", 30, "Meccan"),
        SurahInfo(90, "سُورَةُ البَلَدِ", "Al-Balad", "The City", 20, "Meccan"),
        SurahInfo(91, "سُورَةُ الشَّمْسِ", "Ash-Shams", "The Sun", 15, "Meccan"),
        SurahInfo(92, "سُورَةُ اللَّيْلِ", "Al-Layl", "The Night", 21, "Meccan"),
        SurahInfo(93, "سُورَةُ الضُّحَىٰ", "Ad-Duha", "The Morning Hours", 11, "Meccan"),
        SurahInfo(94, "سُورَةُ الشَّرْحِ", "Ash-Sharh", "The Relief", 8, "Meccan"),
        SurahInfo(95, "سُورَةُ التِّينِ", "At-Tin", "The Fig", 8, "Meccan"),
        SurahInfo(96, "سُورَةُ العَلَقِ", "Al-'Alaq", "The Clot", 19, "Meccan"),
        SurahInfo(97, "سُورَةُ القَدْرِ", "Al-Qadr", "The Power", 5, "Meccan"),
        SurahInfo(98, "سُورَةُ البَيِّنَةِ", "Al-Bayyinah", "The Clear Proof", 8, "Medinan"),
        SurahInfo(99, "سُورَةُ الزَّلْزَلَةِ", "Az-Zalzalah", "The Earthquake", 8, "Medinan"),
        SurahInfo(100, "سُورَةُ العَادِيَاتِ", "Al-'Adiyat", "The Courser", 11, "Meccan"),
        SurahInfo(101, "سُورَةُ القَارِعَةِ", "Al-Qari'ah", "The Calamity", 11, "Meccan"),
        SurahInfo(102, "سُورَةُ التَّكَاثُرِ", "At-Takathur", "The Rivalry in World Increase", 8, "Meccan"),
        SurahInfo(103, "سُورَةُ العَصْرِ", "Al-'Asr", "The Declining Day", 3, "Meccan"),
        SurahInfo(104, "سُورَةُ الهُمَزَةِ", "Al-Humazah", "The Traducer", 9, "Meccan"),
        SurahInfo(105, "سُورَةُ الفِيلِ", "Al-Fil", "The Elephant", 5, "Meccan"),
        SurahInfo(106, "سُورَةُ قُرَيْشٍ", "Quraysh", "Quraysh", 4, "Meccan"),
        SurahInfo(107, "سُورَةُ المَاعُونَ", "Al-Ma'un", "The Small Kindnesses", 7, "Meccan"),
        SurahInfo(108, "سُورَةُ الكَوْثَرِ", "Al-Kawthar", "The Abundance", 3, "Meccan"),
        SurahInfo(109, "سُورَةُ الكَافِرُونَ", "Al-Kafirun", "The Disbelievers", 6, "Meccan"),
        SurahInfo(110, "سُورَةُ النَّصْرِ", "An-Nasr", "The Divine Support", 3, "Medinan"),
        SurahInfo(111, "سُورَةُ المَسَدِ", "Al-Masad", "The Palm Fiber", 5, "Meccan"),
        SurahInfo(112, "سُورَةُ الإِخْلَاصِ", "Al-Ikhlas", "The Sincerity", 4, "Meccan"),
        SurahInfo(113, "سُورَةُ الفَلَقِ", "Al-Falaq", "The Daybreak", 5, "Meccan"),
        SurahInfo(114, "سُورَةُ النَّاسِ", "An-Nas", "Mankind", 6, "Meccan")
    )
}
