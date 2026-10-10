package com.example.alarm

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.data.model.PrayerType
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Enterprise WorkManager scheduler for triggering local push notifications
 * and alarms at daily Namaz times configured in application metadata.
 */
object NamazWorkManagerScheduler {

    private const val TAG = "NamazWMScheduler"
    const val TAG_NAMAZ_WORK = "tag_namaz_workmanager"
    const val TAG_NAMAZ_PERIODIC = "tag_namaz_periodic"

    /**
     * Reads all daily namaz times from AndroidManifest metadata and schedules
     * background OneTimeWorkRequests with precise initial delay for each prayer.
     */
    fun scheduleAllNamazFromMetadata(context: Context) {
        val config = NamazMetadataProvider.loadNamazMetadata(context)
        if (!config.notificationsEnabled) {
            Log.d(TAG, "Namaz notifications disabled in app metadata. Canceling existing workers.")
            cancelAllNamazWork(context)
            return
        }

        NamazAlarmWorker.createNamazNotificationChannel(context)
        val workManager = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()

        val activePrayers = listOf(
            PrayerType.FAJR,
            PrayerType.DHUHR,
            PrayerType.ASR,
            PrayerType.MAGHRIB,
            PrayerType.ISHA
        )

        for (prayer in activePrayers) {
            val timeStr = NamazMetadataProvider.getPrayerTimeString(config, prayer)
            val triggerMillis = NamazMetadataProvider.calculateNextTriggerMillis(timeStr, now)
            val delayMillis = (triggerMillis - now).coerceAtLeast(0L)
            val timeFormatted = NamazMetadataProvider.formatTo12Hour(timeStr)

            val inputData = Data.Builder()
                .putString(NamazAlarmWorker.KEY_PRAYER_NAME, prayer.displayName)
                .putString(NamazAlarmWorker.KEY_PRAYER_ARABIC, prayer.arabicName)
                .putString(NamazAlarmWorker.KEY_TIME_FORMATTED, timeFormatted)
                .putBoolean(NamazAlarmWorker.KEY_IS_PRE_REMINDER, false)
                .putBoolean(NamazAlarmWorker.KEY_IS_TEST, false)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<NamazAlarmWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(TAG_NAMAZ_WORK)
                .addTag(prayer.name)
                .build()

            val uniqueWorkName = "NAMAZ_WORK_${prayer.name}"
            workManager.enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            Log.d(TAG, "Scheduled WorkManager alarm for ${prayer.displayName} ($timeStr / $timeFormatted) in ${delayMillis / 1000}s [uniqueName=$uniqueWorkName]")

            // Schedule pre-prayer reminder if enabled in metadata
            if (config.preReminderMinutes > 0) {
                val preTriggerMillis = triggerMillis - (config.preReminderMinutes * 60 * 1000L)
                if (preTriggerMillis > now) {
                    val preDelayMillis = preTriggerMillis - now
                    val preInputData = Data.Builder()
                        .putString(NamazAlarmWorker.KEY_PRAYER_NAME, prayer.displayName)
                        .putString(NamazAlarmWorker.KEY_PRAYER_ARABIC, prayer.arabicName)
                        .putString(NamazAlarmWorker.KEY_TIME_FORMATTED, timeFormatted)
                        .putBoolean(NamazAlarmWorker.KEY_IS_PRE_REMINDER, true)
                        .putBoolean(NamazAlarmWorker.KEY_IS_TEST, false)
                        .build()

                    val preWorkRequest = OneTimeWorkRequestBuilder<NamazAlarmWorker>()
                        .setInitialDelay(preDelayMillis, TimeUnit.MILLISECONDS)
                        .setInputData(preInputData)
                        .addTag(TAG_NAMAZ_WORK)
                        .addTag("${prayer.name}_PRE")
                        .build()

                    val preUniqueName = "NAMAZ_PRE_WORK_${prayer.name}"
                    workManager.enqueueUniqueWork(
                        preUniqueName,
                        ExistingWorkPolicy.REPLACE,
                        preWorkRequest
                    )
                    Log.d(TAG, "Scheduled pre-reminder WorkManager alert for ${prayer.displayName} in ${preDelayMillis / 1000}s")
                }
            }
        }
    }

    /**
     * Reschedules a prayer for the next day's occurrence (+24 hours).
     */
    fun reschedulePrayerForNextDay(context: Context, prayerType: PrayerType) {
        if (prayerType == PrayerType.SUNRISE) return

        val config = NamazMetadataProvider.loadNamazMetadata(context)
        if (!config.notificationsEnabled) return

        val workManager = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()
        val timeStr = NamazMetadataProvider.getPrayerTimeString(config, prayerType)
        val triggerMillis = NamazMetadataProvider.calculateNextTriggerMillis(timeStr, now)
        val delayMillis = (triggerMillis - now).coerceAtLeast(0L)
        val timeFormatted = NamazMetadataProvider.formatTo12Hour(timeStr)

        val inputData = Data.Builder()
            .putString(NamazAlarmWorker.KEY_PRAYER_NAME, prayerType.displayName)
            .putString(NamazAlarmWorker.KEY_PRAYER_ARABIC, prayerType.arabicName)
            .putString(NamazAlarmWorker.KEY_TIME_FORMATTED, timeFormatted)
            .putBoolean(NamazAlarmWorker.KEY_IS_PRE_REMINDER, false)
            .putBoolean(NamazAlarmWorker.KEY_IS_TEST, false)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NamazAlarmWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(TAG_NAMAZ_WORK)
            .addTag(prayerType.name)
            .build()

        val uniqueWorkName = "NAMAZ_WORK_${prayerType.name}"
        workManager.enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        Log.d(TAG, "Auto-rescheduled ${prayerType.displayName} in WorkManager for tomorrow (${delayMillis / 1000}s delay)")
    }

    /**
     * Sets up periodic background synchronization to ensure all 5 prayers stay armed indefinitely.
     */
    fun schedulePeriodicDailySync(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val periodicRequest = PeriodicWorkRequestBuilder<DailyNamazSyncWorker>(12, TimeUnit.HOURS)
            .addTag(TAG_NAMAZ_PERIODIC)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DailyNamazSyncWorker.UNIQUE_PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
        Log.d(TAG, "Enqueued 12-hour periodic Namaz background sync with WorkManager")
    }

    /**
     * Triggers an immediate test notification for a prayer using WorkManager.
     */
    fun triggerInstantTestNotification(context: Context, prayerType: PrayerType) {
        val workManager = WorkManager.getInstance(context)
        val config = NamazMetadataProvider.loadNamazMetadata(context)
        val timeStr = NamazMetadataProvider.getPrayerTimeString(config, prayerType)
        val timeFormatted = NamazMetadataProvider.formatTo12Hour(timeStr)

        val inputData = Data.Builder()
            .putString(NamazAlarmWorker.KEY_PRAYER_NAME, prayerType.displayName)
            .putString(NamazAlarmWorker.KEY_PRAYER_ARABIC, prayerType.arabicName)
            .putString(NamazAlarmWorker.KEY_TIME_FORMATTED, timeFormatted)
            .putBoolean(NamazAlarmWorker.KEY_IS_PRE_REMINDER, false)
            .putBoolean(NamazAlarmWorker.KEY_IS_TEST, true)
            .build()

        val testWorkRequest = OneTimeWorkRequestBuilder<NamazAlarmWorker>()
            .setInitialDelay(0, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueue(testWorkRequest)
        Log.d(TAG, "Enqueued instant test notification via WorkManager for ${prayerType.displayName}")
    }

    /**
     * Cancels all scheduled Namaz alarm workers.
     */
    fun cancelAllNamazWork(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TAG_NAMAZ_WORK)
        Log.d(TAG, "Canceled all WorkManager namaz workers")
    }

    /**
     * Observe all active namaz workers
     */
    fun getWorkInfosByTagFlow(context: Context): Flow<List<WorkInfo>> {
        val workManager = WorkManager.getInstance(context)
        return workManager.getWorkInfosByTagFlow(TAG_NAMAZ_WORK)
    }
}
