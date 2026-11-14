package com.example.alarm

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.repository.AladhanRepository
import kotlinx.coroutines.CancellationException

/**
 * Periodic WorkManager worker that refreshes exact prayer alarms from the
 * calculated schedule without creating duplicate metadata-based alerts.
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
        Log.d(TAG, "Refreshing exact prayer alarms from the calculated schedule...")
        return try {
            val prayerSchedule = try {
                AladhanRepository.fetchSahiwalPrayerTimings().getOrThrow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Prayer-time refresh failed; calculating today's schedule locally", e)
                AladhanRepository.getSahiwalFallbackSchedule()
            }
            PrayerAlarmScheduler.scheduleAllActivePrayers(context, prayerSchedule.second)
            Log.d(TAG, "Successfully refreshed exact prayer alarms.")
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync daily namaz workers: ${e.message}", e)
            Result.retry()
        }
    }
}
