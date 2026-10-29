package com.example.data.quran.repository

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.data.quran.database.QuranDatabase
import com.example.data.quran.api.QuranJsonApi
import com.example.data.quran.model.AyahEntity
import com.example.data.quran.model.AyahWithSurahInfo
import com.example.data.quran.model.BookmarkEntity
import com.example.data.quran.model.JuzCatalog
import com.example.data.quran.model.SurahEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class QuranRepository(context: Context) {

    private val db = QuranDatabase.getDatabase(context)
    private val dao = db.quranDao()

    fun getAllSurahsFlow(): Flow<List<SurahEntity>> {
        return dao.getAllSurahsFlow().flowOn(Dispatchers.IO)
    }

    suspend fun getAllSurahs(): List<SurahEntity> = withContext(Dispatchers.IO) {
        dao.getAllSurahs()
    }

    suspend fun getSurahById(surahId: Int): SurahEntity? = withContext(Dispatchers.IO) {
        dao.getSurahById(surahId)
    }

    fun getSurahByIdFlow(surahId: Int): Flow<SurahEntity?> {
        return dao.getSurahByIdFlow(surahId).flowOn(Dispatchers.IO)
    }

    /**
     * Retrieves Ayahs from Room local database, or seamlessly fetches and caches from
     * AlQuran Cloud API for any of the 114 Surahs.
     */
    suspend fun getOrFetchAyahsForSurah(surahId: Int): List<AyahEntity> = withContext(Dispatchers.IO) {
        if (surahId !in 1..114) return@withContext emptyList()
        val local = dao.getAyahsForSurah(surahId)
        val expectedCount = dao.getSurahById(surahId)?.totalVerses
            ?: QuranJsonApi.ALL_114_SURAHS.firstOrNull { it.number == surahId }?.numberOfAyahs
            ?: 0
        if (expectedCount > 0 && local.size == expectedCount &&
            local.all { it.textArabic.isNotBlank() && it.textUrduTranslation.isNotBlank() }
        ) {
            return@withContext local
        }

        try {
            val result = QuranJsonApi.fetchSurah(surahId)
            val remote = result.getOrThrow()
            val entities = remote.ayahs
                .sortedBy { it.numberInSurah }
                .map { ayah ->
                    val previous = local.firstOrNull { it.verseNumber == ayah.numberInSurah }
                    AyahEntity(
                        globalVerseNumber = ayah.globalNumber,
                        surahId = surahId,
                        verseNumber = ayah.numberInSurah,
                        textArabic = ayah.arabicText,
                        textEnglishTranslation = ayah.englishTranslation,
                        textUrduTranslation = ayah.urduTranslation,
                        tafsirIbnKathir = previous?.tafsirIbnKathir.orEmpty(),
                        audioStartTimeMs = previous?.audioStartTimeMs ?: 0L,
                        audioEndTimeMs = previous?.audioEndTimeMs ?: 0L
                    )
                }
            if (expectedCount > 0 && entities.size == expectedCount &&
                entities.all { it.textArabic.isNotBlank() && it.textUrduTranslation.isNotBlank() }
            ) {
                dao.insertAyahs(entities)
                QuranJsonApi.cacheSurah(remote)
                Log.d("QuranRepository", "Cached ${entities.size} translated Ayahs for Surah $surahId.")
                return@withContext entities
            }
        } catch (e: Exception) {
            Log.e("QuranRepository", "Error fetching remote Ayahs for Surah $surahId", e)
        }

        local
    }

    suspend fun getOrFetchAyahsForJuz(juzId: Int): List<AyahEntity> = withContext(Dispatchers.IO) {
        val juz = JuzCatalog.getJuzById(juzId.coerceIn(1, 30)) ?: return@withContext emptyList()
        val verses = mutableListOf<AyahEntity>()
        for (surahId in juz.startSurahId..juz.endSurahId) {
            val surahAyahs = getOrFetchAyahsForSurah(surahId)
            val expectedCount = dao.getSurahById(surahId)?.totalVerses
                ?: QuranJsonApi.ALL_114_SURAHS.firstOrNull { it.number == surahId }?.numberOfAyahs
                ?: 0
            if (expectedCount == 0 || surahAyahs.size != expectedCount ||
                surahAyahs.any { it.textArabic.isBlank() || it.textUrduTranslation.isBlank() }
            ) {
                return@withContext emptyList()
            }
            verses += surahAyahs.filter { ayah ->
                when {
                    surahId == juz.startSurahId && surahId == juz.endSurahId ->
                        ayah.verseNumber in juz.startVerse..juz.endVerse
                    surahId == juz.startSurahId -> ayah.verseNumber >= juz.startVerse
                    surahId == juz.endSurahId -> ayah.verseNumber <= juz.endVerse
                    else -> true
                }
            }
        }
        verses.sortedWith(compareBy(AyahEntity::surahId, AyahEntity::verseNumber))
    }

    suspend fun cacheFetchedSurah(data: com.example.data.quran.api.SurahFullData) = withContext(Dispatchers.IO) {
        val expected = dao.getSurahById(data.info.number)?.totalVerses ?: data.info.numberOfAyahs
        if (data.ayahs.size != expected ||
            data.ayahs.any { it.arabicText.isBlank() || it.urduTranslation.isBlank() }
        ) return@withContext
        val localByVerse = dao.getAyahsForSurah(data.info.number).associateBy { it.verseNumber }
        dao.insertAyahs(
            data.ayahs.sortedBy { it.numberInSurah }.map { ayah ->
                val previous = localByVerse[ayah.numberInSurah]
                AyahEntity(
                    globalVerseNumber = ayah.globalNumber,
                    surahId = data.info.number,
                    verseNumber = ayah.numberInSurah,
                    textArabic = ayah.arabicText,
                    textEnglishTranslation = ayah.englishTranslation,
                    textUrduTranslation = ayah.urduTranslation,
                    tafsirIbnKathir = previous?.tafsirIbnKathir.orEmpty(),
                    audioStartTimeMs = previous?.audioStartTimeMs ?: 0L,
                    audioEndTimeMs = previous?.audioEndTimeMs ?: 0L
                )
            }
        )
        QuranJsonApi.cacheSurah(data)
    }

    suspend fun downloadFullQuranOffline(
        onProgress: (completedSurahs: Int, totalSurahs: Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        for (surahId in 1..114) {
            val ayahs = getOrFetchAyahsForSurah(surahId)
            val expected = dao.getSurahById(surahId)?.totalVerses
                ?: QuranJsonApi.ALL_114_SURAHS.firstOrNull { it.number == surahId }?.numberOfAyahs
                ?: 0
            if (expected == 0 || ayahs.size != expected ||
                ayahs.any { it.textArabic.isBlank() || it.textUrduTranslation.isBlank() }
            ) {
                onProgress(surahId - 1, 114)
                return@withContext false
            }
            onProgress(surahId, 114)
        }
        true
    }

    fun getAyahsPaging(surahId: Int, pageSize: Int = 20): Flow<PagingData<AyahEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                initialLoadSize = pageSize * 2,
                prefetchDistance = 5
            ),
            pagingSourceFactory = { dao.getAyahsForSurahPaging(surahId) }
        ).flow.flowOn(Dispatchers.IO)
    }

    fun searchAyahsPaging(query: String, pageSize: Int = 20): Flow<PagingData<AyahWithSurahInfo>> {
        return Pager(
            config = PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false,
                initialLoadSize = pageSize
            ),
            pagingSourceFactory = { dao.searchAyahsPaging(query) }
        ).flow.flowOn(Dispatchers.IO)
    }

    fun getAyahsForSurahFlow(surahId: Int): Flow<List<AyahEntity>> {
        return dao.getAyahsForSurahFlow(surahId).flowOn(Dispatchers.IO)
    }

    suspend fun getAyahsForSurah(surahId: Int): List<AyahEntity> = withContext(Dispatchers.IO) {
        dao.getAyahsForSurah(surahId)
    }

    suspend fun insertAyahs(ayahs: List<AyahEntity>) = withContext(Dispatchers.IO) {
        dao.insertAyahs(ayahs)
    }

    // --- Bookmarks Management ---
    fun getAllBookmarksFlow(): Flow<List<BookmarkEntity>> {
        return dao.getAllBookmarksFlow().flowOn(Dispatchers.IO)
    }

    suspend fun toggleBookmark(
        surahId: Int,
        verseNumber: Int,
        surahNameEnglish: String,
        surahNameArabic: String,
        textArabic: String,
        textEnglish: String,
        textUrdu: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val alreadyBookmarked = dao.isBookmarked(surahId, verseNumber)
        if (alreadyBookmarked) {
            dao.deleteBookmark(surahId, verseNumber)
            false
        } else {
            dao.insertBookmark(
                BookmarkEntity(
                    surahId = surahId,
                    verseNumber = verseNumber,
                    surahNameEnglish = surahNameEnglish,
                    surahNameArabic = surahNameArabic,
                    textArabic = textArabic,
                    textEnglishTranslation = textEnglish,
                    textUrduTranslation = textUrdu
                )
            )
            true
        }
    }

    fun isBookmarkedFlow(surahId: Int, verseNumber: Int): Flow<Boolean> {
        return dao.isBookmarkedFlow(surahId, verseNumber).flowOn(Dispatchers.IO)
    }

    suspend fun deleteBookmark(surahId: Int, verseNumber: Int) = withContext(Dispatchers.IO) {
        dao.deleteBookmark(surahId, verseNumber)
    }
}
