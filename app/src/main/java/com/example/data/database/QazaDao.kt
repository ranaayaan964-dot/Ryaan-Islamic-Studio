package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QazaDao {

    @Query("SELECT * FROM qaza_prayers ORDER BY displayOrder ASC")
    fun getAllQazaPrayersFlow(): Flow<List<QazaPrayerEntity>>

    @Query("SELECT * FROM qaza_prayers ORDER BY displayOrder ASC")
    suspend fun getAllQazaPrayers(): List<QazaPrayerEntity>

    @Query("SELECT * FROM qaza_prayers WHERE prayerId = :prayerId LIMIT 1")
    suspend fun getQazaPrayerById(prayerId: String): QazaPrayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(prayers: List<QazaPrayerEntity>)

    @Update
    suspend fun update(prayer: QazaPrayerEntity)

    @Query("""
        UPDATE qaza_prayers 
        SET missedCount = CASE WHEN missedCount - :delta < 0 THEN 0 ELSE missedCount - :delta END, 
            completedToday = completedToday + :delta, 
            totalCompleted = totalCompleted + :delta 
        WHERE prayerId = :prayerId
    """)
    suspend fun recordPerformed(prayerId: String, delta: Int = 1)

    @Query("""
        UPDATE qaza_prayers 
        SET missedCount = missedCount + :delta 
        WHERE prayerId = :prayerId
    """)
    suspend fun addMissed(prayerId: String, delta: Int = 1)

    @Query("""
        UPDATE qaza_prayers 
        SET missedCount = CASE WHEN missedCount - :delta < 0 THEN 0 ELSE missedCount - :delta END 
        WHERE prayerId = :prayerId
    """)
    suspend fun reduceMissed(prayerId: String, delta: Int = 1)

    @Query("UPDATE qaza_prayers SET completedToday = 0, lastUpdatedDate = :today")
    suspend fun resetDailyCounts(today: String)

    @Query("DELETE FROM qaza_prayers")
    suspend fun clearAll()
}
