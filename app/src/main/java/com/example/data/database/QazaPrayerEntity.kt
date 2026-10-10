package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing Qaza (Missed) Prayer records.
 * Tracks missed debt count and daily/total completed counts for:
 * Fajr, Dhuhr, Asr, Maghrib, Isha, and Witr.
 */
@Entity(tableName = "qaza_prayers")
data class QazaPrayerEntity(
    @PrimaryKey val prayerId: String, // "fajr", "dhuhr", "asr", "maghrib", "isha", "witr"
    val prayerName: String,
    val prayerArabic: String,
    val missedCount: Int,
    val completedToday: Int,
    val totalCompleted: Int,
    val rakats: Int,
    val displayOrder: Int,
    val lastUpdatedDate: String // "YYYY-MM-DD"
)
