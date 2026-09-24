package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * REMEDIATION A2: ZERO-ANR PRAYER ALARM RECEIVER
 * Uses goAsync() to offload all heavy I/O, database access, and WorkManager rescheduling
 * to Dispatchers.IO, preventing 10-second OS ANRs on boot and alarm triggers.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PrayerAlarmReceiver"
        const val CHANNEL_ID = "ryaan_prayer_alarm_channel"
        const val NOTIFICATION_ID = 9991
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            try {
                val action = intent.action
                Log.d(TAG, "PrayerAlarmReceiver received action: $action")

                // 1. Create NotificationChannel explicitly setting IMPORTANCE_HIGH, bypass DND, and VISIBILITY_PUBLIC
                createHighPriorityNotificationChannel(context)

                // Handle reboot / package replacement safely off the main thread
                if (action == Intent.ACTION_BOOT_COMPLETED ||
                    action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    action == "android.intent.action.QUICKBOOT_POWERON"
                ) {
                    Log.d(TAG, "Device rebooted. Rescheduling all prayer alarms asynchronously...")
                    PrayerAlarmScheduler.ensureAllAlarmsScheduled(context)
                    NamazWorkManagerScheduler.scheduleAllNamazFromMetadata(context)
                    NamazWorkManagerScheduler.schedulePeriodicDailySync(context)
                    return@launch
                }

                if (action == PrayerAlarmService.ACTION_STOP_AZAN) {
                    Log.d(TAG, "Stopping Azan via receiver action")
                    PrayerAlarmService.stopAlarm(context)
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    nm?.cancel(NOTIFICATION_ID)
                    return@launch
                }

                val prayerName = intent.getStringExtra("prayer_name") ?: intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
                val prayerArabic = intent.getStringExtra("prayer_arabic") ?: "الصلاة"
                val isPreReminder = intent.getBooleanExtra("is_pre_reminder", false)
                val isSnooze = intent.getBooleanExtra("is_snooze", false)

                // 2. Extract EXTRA_ALERT_TYPE
                val alertType = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ALERT_TYPE)
                    ?: intent.getStringExtra(PrayerAlarmService.EXTRA_ALERT_TYPE)
                    ?: if (isPreReminder) PrayerAlarmScheduler.ALERT_TYPE_15_MIN else PrayerAlarmScheduler.ALERT_TYPE_EXACT

                Log.d(TAG, "Triggering alarm alert: prayer=$prayerName ($prayerArabic), alertType=$alertType, isSnooze=$isSnooze")

                // Reschedule main prayer for next day only on exact time (not on pre-reminders or snooze)
                if (alertType == PrayerAlarmScheduler.ALERT_TYPE_EXACT && !isSnooze) {
                    PrayerAlarmScheduler.reschedulePrayerForNextDay(context, prayerName)
                }

                // If EXACT alarm, post full-screen intent notification immediately for instant lock screen wakeup
                if (alertType == PrayerAlarmScheduler.ALERT_TYPE_EXACT) {
                    val fullScreenIntent = Intent(context, AlarmRingingActivity::class.java).apply {
                        putExtra("PRAYER_NAME", prayerName)
                        putExtra("PRAYER_ARABIC", prayerArabic)
                        putExtra("IS_SNOOZE", isSnooze)
                        putExtra(PrayerAlarmService.EXTRA_ALERT_TYPE, alertType)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }

                    val fullScreenPendingIntent = PendingIntent.getActivity(
                        context,
                        NOTIFICATION_ID,
                        fullScreenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val title = if (isSnooze) "Snooze Alarm - $prayerName ($prayerArabic)" else "Adhan Call to Prayer - $prayerName ($prayerArabic)"
                    val text = "It is time for $prayerName. Tap to view or snooze."

                    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_ALARM)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setFullScreenIntent(fullScreenPendingIntent, true)
                        .setContentIntent(fullScreenPendingIntent)
                        .build()

                    try {
                        val notificationManager = NotificationManagerCompat.from(context)
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Notification permission missing or denied", e)
                    }
                }

                // Pass EXTRA_ALERT_TYPE directly to PrayerAlarmService
                val serviceIntent = Intent(context, PrayerAlarmService::class.java).apply {
                    this.action = PrayerAlarmService.ACTION_START_AZAN
                    putExtra(PrayerAlarmService.EXTRA_PRAYER_NAME, prayerName)
                    putExtra(PrayerAlarmService.EXTRA_PRAYER_ARABIC, prayerArabic)
                    putExtra(PrayerAlarmService.EXTRA_ALERT_TYPE, alertType)
                    putExtra(PrayerAlarmService.EXTRA_IS_PRE_REMINDER, alertType == PrayerAlarmScheduler.ALERT_TYPE_15_MIN)
                    putExtra(PrayerAlarmService.EXTRA_IS_SNOOZE, isSnooze)
                }

                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                    Log.d(TAG, "Started PrayerAlarmService with alertType=$alertType for $prayerName")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start PrayerAlarmService", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in PrayerAlarmReceiver execution", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun createHighPriorityNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Prayer Alarms & Azan Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Triggers unkillable alarms, Azan alerts, and lock screen notifications for daily prayers"
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

