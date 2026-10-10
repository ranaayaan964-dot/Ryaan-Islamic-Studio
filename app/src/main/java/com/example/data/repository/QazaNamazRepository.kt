package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.AppDatabase
import com.example.data.database.QazaPrayerEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository managing Qaza (Missed) Namaz persistence with Room Database.
 * Accurately tracks debts for the 5 daily prayers (Fajr, Dhuhr, Asr, Maghrib, Isha) plus Witr.
 */
class QazaNamazRepository private constructor(context: Context) {

    private val qazaDao = AppDatabase.getInstance(context).qazaDao()

    companion object {
        private const val TAG = "QazaNamazRepository"

        @Volatile
        private var INSTANCE: QazaNamazRepository? = null

        fun getInstance(context: Context): QazaNamazRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = QazaNamazRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }

        private fun getTodayDateString(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        }

        val DEFAULT_PRAYERS = listOf(
            QazaPrayerEntity(
                prayerId = "fajr",
                prayerName = "Fajr",
                prayerArabic = "فجر",
                missedCount = 12,
                completedToday = 0,
                totalCompleted = 14,
                rakats = 2,
                displayOrder = 1,
                lastUpdatedDate = getTodayDateString()
            ),
            QazaPrayerEntity(
                prayerId = "dhuhr",
                prayerName = "Dhuhr",
                prayerArabic = "ظهر",
                missedCount = 18,
                completedToday = 0,
                totalCompleted = 8,
                rakats = 4,
                displayOrder = 2,
                lastUpdatedDate = getTodayDateString()
            ),
            QazaPrayerEntity(
                prayerId = "asr",
                prayerName = "Asr",
                prayerArabic = "عصر",
                missedCount = 15,
                completedToday = 0,
                totalCompleted = 10,
                rakats = 4,
                displayOrder = 3,
                lastUpdatedDate = getTodayDateString()
            ),
            QazaPrayerEntity(
                prayerId = "maghrib",
                prayerName = "Maghrib",
                prayerArabic = "مغرب",
                missedCount = 9,
                completedToday = 0,
                totalCompleted = 15,
                rakats = 3,
                displayOrder = 4,
                lastUpdatedDate = getTodayDateString()
            ),
            QazaPrayerEntity(
                prayerId = "isha",
                prayerName = "Isha",
                prayerArabic = "عشاء",
                missedCount = 24,
                completedToday = 0,
                totalCompleted = 6,
                rakats = 4,
                displayOrder = 5,
                lastUpdatedDate = getTodayDateString()
            ),
            QazaPrayerEntity(
                prayerId = "witr",
                prayerName = "Witr",
                prayerArabic = "وتر واجب",
                missedCount = 20,
                completedToday = 0,
                totalCompleted = 4,
                rakats = 3,
                displayOrder = 6,
                lastUpdatedDate = getTodayDateString()
            )
        )
    }

    fun getQazaPrayersFlow(): Flow<List<QazaPrayerEntity>> {
        return qazaDao.getAllQazaPrayersFlow().flowOn(Dispatchers.IO)
    }

    suspend fun initializeDefaultsIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val existing = qazaDao.getAllQazaPrayers()
            val today = getTodayDateString()

            if (existing.isEmpty()) {
                qazaDao.insertAll(DEFAULT_PRAYERS)
                Log.d(TAG, "Initialized default Qaza Namaz records in Room DB")
            } else {
                // If new day, reset completedToday
                val needsReset = existing.any { it.lastUpdatedDate != today }
                if (needsReset) {
                    qazaDao.resetDailyCounts(today)
                    Log.d(TAG, "Reset daily completed Qaza prayer counts for date: $today")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Qaza prayers: ${e.message}", e)
        }
    }

    /**
     * User performs a Qaza prayer: decrements missed debt, increments completed today & total completed.
     */
    suspend fun recordPrayerPerformed(prayerId: String) = withContext(Dispatchers.IO) {
        try {
            qazaDao.recordPerformed(prayerId, 1)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording performed prayer $prayerId: ${e.message}", e)
        }
    }

    /**
     * User adds to missed debt count.
     */
    suspend fun incrementMissed(prayerId: String, delta: Int = 1) = withContext(Dispatchers.IO) {
        try {
            qazaDao.addMissed(prayerId, delta)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing missed prayer $prayerId: ${e.message}", e)
        }
    }

    /**
     * User decrements missed debt count directly.
     */
    suspend fun decrementMissed(prayerId: String, delta: Int = 1) = withContext(Dispatchers.IO) {
        try {
            qazaDao.reduceMissed(prayerId, delta)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrementing missed prayer $prayerId: ${e.message}", e)
        }
    }

    /**
     * Bulk calculation wizard: Sets or adds N days of missed prayers across all 6 prayers.
     */
    suspend fun bulkAddDays(days: Int) = withContext(Dispatchers.IO) {
        try {
            val existing = qazaDao.getAllQazaPrayers()
            val updated = existing.map { prayer ->
                prayer.copy(missedCount = prayer.missedCount + days)
            }
            qazaDao.insertAll(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error in bulkAddDays: ${e.message}", e)
        }
    }

    /**
     * Bulk set exact days across all 6 prayers.
     */
    suspend fun bulkSetDays(days: Int) = withContext(Dispatchers.IO) {
        try {
            val existing = qazaDao.getAllQazaPrayers()
            val updated = existing.map { prayer ->
                prayer.copy(missedCount = days.coerceAtLeast(0))
            }
            qazaDao.insertAll(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error in bulkSetDays: ${e.message}", e)
        }
    }

    suspend fun resetAllDebts() = withContext(Dispatchers.IO) {
        try {
            val existing = qazaDao.getAllQazaPrayers()
            val updated = existing.map { prayer ->
                prayer.copy(missedCount = 0, completedToday = 0)
            }
            qazaDao.insertAll(updated)
        } catch (e: Exception) {
            Log.e(TAG, "Error in resetAllDebts: ${e.message}", e)
        }
    }
}
