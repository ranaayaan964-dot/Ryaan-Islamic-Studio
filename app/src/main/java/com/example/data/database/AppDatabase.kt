package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserEntity::class,
        ChatHistoryEntity::class,
        QazaPrayerEntity::class,
        AdminMessageEntity::class,
        UserReportEntity::class,
        ProLicenseKeyEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun qazaDao(): QazaDao
    abstract fun adminDao(): AdminDao
    abstract fun proLicenseDao(): ProLicenseDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val userColumns = db.query("PRAGMA table_info(`users`)").use { cursor ->
                    buildSet {
                        val nameColumn = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) {
                            if (nameColumn >= 0) add(cursor.getString(nameColumn))
                        }
                    }
                }
                if ("firebase_uid" !in userColumns) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `firebase_uid` TEXT NOT NULL DEFAULT ''")
                }
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val userColumns = db.query("PRAGMA table_info(`users`)").use { cursor ->
                    buildSet {
                        val nameColumn = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) {
                            if (nameColumn >= 0) add(cursor.getString(nameColumn))
                        }
                    }
                }
                if ("auth_provider" !in userColumns) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `auth_provider` TEXT NOT NULL DEFAULT ''")
                }
                if ("created_at" !in userColumns) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `created_at` INTEGER NOT NULL DEFAULT 0")
                }
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val userColumns = db.query("PRAGMA table_info(`users`)").use { cursor ->
                    buildSet {
                        val nameColumn = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) {
                            if (nameColumn >= 0) add(cursor.getString(nameColumn))
                        }
                    }
                }
                if ("is_removed" !in userColumns) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `is_removed` INTEGER NOT NULL DEFAULT 0")
                }
                if ("activity_summary" !in userColumns) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `activity_summary` TEXT NOT NULL DEFAULT 'Active in App'")
                }
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `admin_messages` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `messageType` TEXT NOT NULL,
                        `targetAll` INTEGER NOT NULL,
                        `targetUsersCsv` TEXT NOT NULL,
                        `senderName` TEXT NOT NULL,
                        `senderUid` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )""".trimIndent()
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `user_reports` (
                        `id` TEXT NOT NULL,
                        `reporterUid` TEXT NOT NULL,
                        `reporterUsername` TEXT NOT NULL,
                        `reporterEmail` TEXT NOT NULL,
                        `reporterFullName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `subject` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `adminReply` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `is_synced` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )""".trimIndent()
                )
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val userColumns = db.query("PRAGMA table_info(`users`)").use { cursor ->
                    buildSet {
                        val nameColumn = cursor.getColumnIndex("name")
                        while (cursor.moveToNext()) {
                            if (nameColumn >= 0) add(cursor.getString(nameColumn))
                        }
                    }
                }
                if ("subscription_plan" !in userColumns) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `subscription_plan` TEXT NOT NULL DEFAULT 'FREE'")
                }
                if ("pro_expires_at" !in userColumns) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `pro_expires_at` INTEGER NOT NULL DEFAULT 0")
                }
                if ("activated_pro_key" !in userColumns) {
                    db.execSQL("ALTER TABLE `users` ADD COLUMN `activated_pro_key` TEXT NOT NULL DEFAULT ''")
                }
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `pro_license_keys` (
                        `licenseKey` TEXT NOT NULL,
                        `planTier` TEXT NOT NULL,
                        `durationDays` INTEGER NOT NULL,
                        `pricePkr` INTEGER NOT NULL,
                        `isUsed` INTEGER NOT NULL,
                        `usedByUsername` TEXT NOT NULL,
                        `usedByEmail` TEXT NOT NULL,
                        `usedByUid` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `activatedAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER NOT NULL,
                        `aiNote` TEXT NOT NULL,
                        PRIMARY KEY(`licenseKey`)
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
                    .addMigrations(MIGRATION_2_3)
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
