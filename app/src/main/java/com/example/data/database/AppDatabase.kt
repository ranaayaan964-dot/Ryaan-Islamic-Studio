package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserEntity::class, ChatHistoryEntity::class, QazaPrayerEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun qazaDao(): QazaDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val chatColumns = db.query("PRAGMA table_info(`chat_history`)").use { cursor ->
                    buildSet {
                        val nameColumn = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) {
                            if (nameColumn >= 0) add(cursor.getString(nameColumn))
                        }
                    }
                }

                if (chatColumns.isEmpty()) {
                    db.execSQL(
                        """CREATE TABLE IF NOT EXISTS `chat_history` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `messageText` TEXT NOT NULL,
                            `isFromUser` INTEGER NOT NULL,
                            `timestamp` INTEGER NOT NULL,
                            `scholarSource` TEXT NOT NULL,
                            `isFatwa` INTEGER NOT NULL DEFAULT 0,
                            `referencesJson` TEXT NOT NULL DEFAULT ''
                        )""".trimIndent()
                    )
                } else {
                    if ("isFatwa" !in chatColumns) {
                        db.execSQL("ALTER TABLE `chat_history` ADD COLUMN `isFatwa` INTEGER NOT NULL DEFAULT 0")
                    }
                    if ("referencesJson" !in chatColumns) {
                        db.execSQL("ALTER TABLE `chat_history` ADD COLUMN `referencesJson` TEXT NOT NULL DEFAULT ''")
                    }
                }

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `qaza_prayers` (
                        `prayerId` TEXT NOT NULL,
                        `prayerName` TEXT NOT NULL,
                        `prayerArabic` TEXT NOT NULL,
                        `missedCount` INTEGER NOT NULL,
                        `completedToday` INTEGER NOT NULL,
                        `totalCompleted` INTEGER NOT NULL,
                        `rakats` INTEGER NOT NULL,
                        `displayOrder` INTEGER NOT NULL,
                        `lastUpdatedDate` TEXT NOT NULL,
                        PRIMARY KEY(`prayerId`)
                    )""".trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "enterprise_app_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
