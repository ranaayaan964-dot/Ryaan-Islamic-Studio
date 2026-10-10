package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.model.PrayerType

/**
 * Background WorkManager Worker to trigger local push notifications and alerts
 * for the daily namaz times configured in the application metadata.
 */
class NamazAlarmWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "NamazAlarmWorker"
        const val CHANNEL_ID = "namaz_workmanager_channel"
        const val NOTIFICATION_ID_BASE = 8000

        const val KEY_PRAYER_NAME = "key_prayer_name"
        const val KEY_PRAYER_ARABIC = "key_prayer_arabic"
        const val KEY_TIME_FORMATTED = "key_time_formatted"
        const val KEY_IS_PRE_REMINDER = "key_is_pre_reminder"
        const val KEY_IS_TEST = "key_is_test"

        /**
         * Ensures the high-priority notification channel is created on Android O+
         */
        fun createNamazNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

                val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
                if (existing == null) {
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()

                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        "Daily Namaz WorkManager Alarms",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Triggers local push notifications and alarms for daily namaz times configured in app metadata"
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
                        setBypassDnd(true)
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        if (alertUri != null) {
                            setSound(alertUri, audioAttributes)
                        }
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }
    }

    override suspend fun doWork(): Result {
        val prayerName = inputData.getString(KEY_PRAYER_NAME) ?: "Prayer"
        val prayerArabic = inputData.getString(KEY_PRAYER_ARABIC) ?: "الصلاة"
        val timeFormatted = inputData.getString(KEY_TIME_FORMATTED) ?: ""
        val isPreReminder = inputData.getBoolean(KEY_IS_PRE_REMINDER, false)
        val isTest = inputData.getBoolean(KEY_IS_TEST, false)

        Log.d(TAG, "Executing NamazAlarmWorker for $prayerName ($prayerArabic) [isPreReminder=$isPreReminder, isTest=$isTest]")

        try {
            // 1. Post local push notification
            showNamazPushNotification(prayerName, prayerArabic, timeFormatted, isPreReminder, isTest)

            // 2. Play Adhan audio / alert sound
            try {
                // Also optionally forward to PrayerAlarmService for foreground media playback & wake lock
                val serviceIntent = Intent(context, PrayerAlarmService::class.java).apply {
                    action = PrayerAlarmService.ACTION_START_AZAN
                    putExtra(PrayerAlarmService.EXTRA_PRAYER_NAME, prayerName)
                    putExtra(PrayerAlarmService.EXTRA_PRAYER_ARABIC, prayerArabic)
                    putExtra(PrayerAlarmService.EXTRA_IS_PRE_REMINDER, isPreReminder)
                    putExtra(PrayerAlarmService.EXTRA_IS_TEST_TRIGGER, isTest)
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Foreground service launch failed, playing direct audio fallback: ${e.message}")
                AdhanSoundPlayer.playAdhanAlert(context, looping = !isPreReminder)
            }

            // 3. Auto-reschedule next occurrence for this prayer tomorrow
            if (!isPreReminder && !isTest) {
                val prayerType = PrayerType.values().find {
                    it.displayName.equals(prayerName, ignoreCase = true) ||
                    it.name.equals(prayerName, ignoreCase = true)
                }
                if (prayerType != null) {
                    NamazWorkManagerScheduler.reschedulePrayerForNextDay(context, prayerType)
                }
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error running NamazAlarmWorker for $prayerName: ${e.message}", e)
            return Result.retry()
        }
    }

    private fun showNamazPushNotification(
        prayerName: String,
        prayerArabic: String,
        timeFormatted: String,
        isPreReminder: Boolean,
        isTest: Boolean
    ) {
        createNamazNotificationChannel(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // PendingIntent to launch app
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("opened_from_alarm", true)
            putExtra("prayer_name", prayerName)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            prayerName.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // PendingIntent to silence alarm
        val stopIntent = Intent(context, PrayerAlarmService::class.java).apply {
            action = PrayerAlarmService.ACTION_STOP_AZAN
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            prayerName.hashCode() + 100,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val testPrefix = if (isTest) "🔔 [TEST ALERT] " else ""
        val title = if (isPreReminder) {
            "${testPrefix}⏳ 15 Minutes to $prayerName Prayer ($prayerArabic)"
        } else {
            "${testPrefix}🕌 Time for $prayerName Prayer ($prayerArabic)"
        }

        val body = when {
            isTest -> "Local push notification test for $prayerName namaz from WorkManager background service."
            isPreReminder -> "Prepare for $prayerName Namaz ($timeFormatted). It will start in approximately 15 minutes. Perform Wudu and find tranquility."
            else -> "It is now time for $prayerName Namaz ($timeFormatted) according to your app metadata schedule. Hayya 'alas-Salah (Come to prayer)."
        }

        val notificationId = NOTIFICATION_ID_BASE + (prayerName.hashCode() % 1000).let { if (it < 0) -it else it }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSubText("Metadata Namaz Alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .setFullScreenIntent(openAppPendingIntent, true)
            .addAction(
                android.R.drawable.ic_menu_today,
                "OPEN APP",
                openAppPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "SILENCE ALARM",
                stopPendingIntent
            )
            .setVibrate(longArrayOf(0, 500, 200, 500, 200, 800))
            .build()

        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Posted push notification ID $notificationId for $prayerName")
    }
}
