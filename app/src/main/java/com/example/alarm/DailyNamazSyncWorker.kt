package com.example.alarm

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Periodic WorkManager Worker that ensures all daily namaz alarm workers
 * specified in app metadata remain armed and synchronized.
 */
class DailyNamazSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "DailyNamazSyncWorker"
        const val UNIQUE_PERIODIC_NAME = "DAILY_NAMAZ_PERIODIC_SYNC_WORK"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running daily Namaz background sync from app metadata...")
        return try {
            val config = NamazMetadataProvider.loadNamazMetadata(context)
            if (config.notificationsEnabled) {
                NamazWorkManagerScheduler.scheduleAllNamazFromMetadata(context)
                Log.d(TAG, "Successfully synced all daily namaz workers from metadata.")
            } else {
                Log.d(TAG, "Namaz notifications disabled in metadata; skipping schedule.")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync daily namaz workers: ${e.message}", e)
            Result.retry()
        }
    }
}
