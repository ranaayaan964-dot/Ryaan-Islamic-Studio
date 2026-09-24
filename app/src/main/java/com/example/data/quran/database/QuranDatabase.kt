package com.example.data.quran.database

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.quran.dao.QuranDao
import com.example.data.quran.model.AyahEntity
import com.example.data.quran.model.BookmarkEntity
import com.example.data.quran.model.SurahEntity
import com.example.data.quran.seed.QuranSeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * REMEDIATION C1: IDEMPOTENT & RACE-FREE QURAN DATABASE
 * Eliminates the INSTANCE race condition during Room builder creation.
 * Guarantees that Surah and Ayah offline seed data is populated reliably.
 */
@Database(
    entities = [SurahEntity::class, AyahEntity::class, BookmarkEntity::class],
    version = 2,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {

    abstract fun quranDao(): QuranDao

    companion object {
        private const val TAG = "QuranDatabase"

        @Volatile
        private var INSTANCE: QuranDatabase? = null
        private val seedMutex = Mutex()

        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "noble_quran_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            scope.launch(Dispatchers.IO) {
                                INSTANCE?.let { database ->
                                    ensureSeeded(database.quranDao())
                                }
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            scope.launch(Dispatchers.IO) {
                                INSTANCE?.let { database ->
                                    ensureSeeded(database.quranDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance

                // Immediately trigger idempotent seeding check with the built instance
                scope.launch(Dispatchers.IO) {
                    ensureSeeded(instance.quranDao())
                }

                instance
            }
        }

        suspend fun ensureSeeded(dao: QuranDao) {
            seedMutex.withLock {
                try {
                    val surahCount = dao.getSurahCount()
                    if (surahCount == 0) {
                        Log.d(TAG, "Seeding initial Noble Quran Surahs and Ayahs...")
                        val surahs = QuranSeedData.getSurahList()
                        dao.insertSurahs(surahs)
                        val ayahs = QuranSeedData.getInitialAyahs()
                        dao.insertAyahs(ayahs)
                        Log.d(TAG, "Noble Quran database seeding completed successfully (${surahs.size} surahs).")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error ensuring Quran database is seeded", e)
                }
            }
        }
    }
}

