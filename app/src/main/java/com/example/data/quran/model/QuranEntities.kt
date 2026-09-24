package com.example.data.quran.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val id: Int, // 1 to 114
    val nameArabic: String,
    val nameEnglish: String,
    val nameTranslation: String,
    val revelationType: String, // "Meccan" or "Medinan"
    val totalVerses: Int,
    val audioUrl: String, // Mishary Rashid Alafasy audio URL
    val audioDurationSeconds: Int = 0
)

@Entity(
    tableName = "ayahs",
    foreignKeys = [
        ForeignKey(
            entity = SurahEntity::class,
            parentColumns = ["id"],
            childColumns = ["surahId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["surahId", "verseNumber"]), Index(value = ["globalVerseNumber"])]
)
data class AyahEntity(
    @PrimaryKey val globalVerseNumber: Int, // 1 to 6236
    val surahId: Int, // 1 to 114
    val verseNumber: Int, // e.g. 1, 2, 3 in Surah
    val textArabic: String,
    val textEnglishTranslation: String, // Sahih International
    val textUrduTranslation: String = "", // Fateh Muhammad Jalandhari
    val tafsirIbnKathir: String,
    val audioStartTimeMs: Long = 0L, // Word/Verse audio sync timestamp start
    val audioEndTimeMs: Long = 0L    // Word/Verse audio sync timestamp end
)

@Entity(
    tableName = "quran_bookmarks",
    indices = [Index(value = ["surahId", "verseNumber"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val surahId: Int,
    val verseNumber: Int,
    val surahNameEnglish: String,
    val surahNameArabic: String,
    val textArabic: String,
    val textEnglishTranslation: String,
    val textUrduTranslation: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AyahWithSurahInfo(
    val globalVerseNumber: Int,
    val surahId: Int,
    val surahNameArabic: String,
    val surahNameEnglish: String,
    val verseNumber: Int,
    val textArabic: String,
    val textEnglishTranslation: String,
    val textUrduTranslation: String = "",
    val tafsirIbnKathir: String,
    val audioStartTimeMs: Long,
    val audioEndTimeMs: Long
)
