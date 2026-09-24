package com.example.data.quran.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.quran.model.AyahEntity
import com.example.data.quran.model.AyahWithSurahInfo
import com.example.data.quran.model.BookmarkEntity
import com.example.data.quran.model.SurahEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    // --- Surah Queries ---
    @Query("SELECT * FROM surahs ORDER BY id ASC")
    fun getAllSurahsFlow(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs ORDER BY id ASC")
    suspend fun getAllSurahs(): List<SurahEntity>

    @Query("SELECT * FROM surahs WHERE id = :surahId LIMIT 1")
    suspend fun getSurahById(surahId: Int): SurahEntity?

    @Query("SELECT * FROM surahs WHERE id = :surahId LIMIT 1")
    fun getSurahByIdFlow(surahId: Int): Flow<SurahEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun getSurahCount(): Int

    // --- Ayah Queries with Paging 3 ---
    @Query("SELECT * FROM ayahs ORDER BY globalVerseNumber ASC")
    fun getAyahs(): PagingSource<Int, AyahEntity>

    @Query("SELECT * FROM ayahs WHERE surahId = :surahId ORDER BY verseNumber ASC")
    fun getAyahsForSurahPaging(surahId: Int): PagingSource<Int, AyahEntity>

    @Query("SELECT * FROM ayahs WHERE surahId = :surahId ORDER BY verseNumber ASC")
    fun getAyahsForSurahFlow(surahId: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE surahId = :surahId ORDER BY verseNumber ASC")
    suspend fun getAyahsForSurah(surahId: Int): List<AyahEntity>

    @Query("""
        SELECT a.globalVerseNumber, a.surahId, s.nameArabic AS surahNameArabic, 
               s.nameEnglish AS surahNameEnglish, a.verseNumber, a.textArabic, 
               a.textEnglishTranslation, a.textUrduTranslation, a.tafsirIbnKathir, 
               a.audioStartTimeMs, a.audioEndTimeMs 
        FROM ayahs a 
        INNER JOIN surahs s ON a.surahId = s.id 
        WHERE a.textArabic LIKE '%' || :query || '%' 
           OR a.textEnglishTranslation LIKE '%' || :query || '%'
           OR a.textUrduTranslation LIKE '%' || :query || '%'
           OR a.tafsirIbnKathir LIKE '%' || :query || '%'
        ORDER BY a.globalVerseNumber ASC
    """)
    fun searchAyahsPaging(query: String): PagingSource<Int, AyahWithSurahInfo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun getAyahCount(): Int

    // --- Bookmark Queries ---
    @Query("SELECT * FROM quran_bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarksFlow(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM quran_bookmarks WHERE surahId = :surahId AND verseNumber = :verseNumber")
    suspend fun deleteBookmark(surahId: Int, verseNumber: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM quran_bookmarks WHERE surahId = :surahId AND verseNumber = :verseNumber)")
    fun isBookmarkedFlow(surahId: Int, verseNumber: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM quran_bookmarks WHERE surahId = :surahId AND verseNumber = :verseNumber)")
    suspend fun isBookmarked(surahId: Int, verseNumber: Int): Boolean

    @Delete
    suspend fun deleteBookmarkEntity(bookmark: BookmarkEntity)
}
