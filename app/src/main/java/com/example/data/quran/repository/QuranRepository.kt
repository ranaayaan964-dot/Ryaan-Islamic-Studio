package com.example.data.quran.repository

import android.content.Context
import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.data.quran.database.QuranDatabase
import com.example.data.quran.model.AyahEntity
import com.example.data.quran.model.AyahWithSurahInfo
import com.example.data.quran.model.BookmarkEntity
import com.example.data.quran.model.SurahEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class QuranRepository(context: Context) {

    private val db = QuranDatabase.getDatabase(context)
    private val dao = db.quranDao()

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

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
        val local = dao.getAyahsForSurah(surahId)
        if (local.isNotEmpty()) {
            return@withContext local
        }

        // Fetch from api.alquran.cloud and cache into Room database
        try {
            val url = "https://api.alquran.cloud/v1/surah/$surahId/editions/quran-uthmani,en.sahih"
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                val data = json.getJSONArray("data")
                val uthmaniEdition = data.getJSONObject(0)
                val englishEdition = if (data.length() > 1) data.getJSONObject(1) else null

                val uthmaniAyahs = uthmaniEdition.getJSONArray("ayahs")
                val englishAyahs = englishEdition?.getJSONArray("ayahs")

                val entities = mutableListOf<AyahEntity>()
                for (i in 0 until uthmaniAyahs.length()) {
                    val uObj = uthmaniAyahs.getJSONObject(i)
                    val globalNum = uObj.getInt("number")
                    val verseNum = uObj.getInt("numberInSurah")
                    val textArabic = uObj.getString("text")
                    val textEnglish = englishAyahs?.optJSONObject(i)?.optString("text") ?: ""

                    entities.add(
                        AyahEntity(
                            globalVerseNumber = globalNum,
                            surahId = surahId,
                            verseNumber = verseNum,
                            textArabic = textArabic,
                            textEnglishTranslation = textEnglish,
                            tafsirIbnKathir = "Surah $surahId Verse $verseNum"
                        )
                    )
                }
                if (entities.isNotEmpty()) {
                    dao.insertAyahs(entities)
                    Log.d("QuranRepository", "Successfully fetched and cached ${entities.size} ayahs for Surah $surahId into Room.")
                    return@withContext entities
                }
            }
        } catch (e: Exception) {
            Log.e("QuranRepository", "Error fetching remote Ayahs for Surah $surahId", e)
        }

        // Return empty or local if any
        return@withContext dao.getAyahsForSurah(surahId)
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
