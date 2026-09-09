package com.example.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("PrayerAlarmReceiver", "Received action: $action")

        // Ensure notification channel is always initialized
        PrayerAlarmService.createNotificationChannel(context)

        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED || 
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Automatically reschedule all exact daily prayer alarms after device restart
            Log.d("PrayerAlarmReceiver", "Device rebooted or package replaced. Rescheduling all prayer alarms...")
            PrayerAlarmScheduler.ensureAllAlarmsScheduled(context)
            return
        }

        if (action == PrayerAlarmService.ACTION_STOP_AZAN) {
            Log.d("PrayerAlarmReceiver", "Stopping Azan via receiver action")
            PrayerAlarmService.stopAlarm(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(PrayerAlarmService.NOTIFICATION_ID)
            return
        }

        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        val prayerArabic = intent.getStringExtra("prayer_arabic") ?: "الصلاة"
        val isPreReminder = intent.getBooleanExtra("is_pre_reminder", false)

        Log.d("PrayerAlarmReceiver", "Alarm triggered for $prayerName ($prayerArabic) [isPreReminder=$isPreReminder].")
        if (!isPreReminder) {
            // Automatically reschedule main prayer for tomorrow so background alerts continue indefinitely
            PrayerAlarmScheduler.reschedulePrayerForNextDay(context, prayerName)
        }

        // Launch Foreground Service for background playback, wake lock, and persistent notification
        val serviceIntent = Intent(context, PrayerAlarmService::class.java).apply {
            this.action = PrayerAlarmService.ACTION_START_AZAN
            putExtra(PrayerAlarmService.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmService.EXTRA_PRAYER_ARABIC, prayerArabic)
            putExtra(PrayerAlarmService.EXTRA_IS_PRE_REMINDER, isPreReminder)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
            Log.d("PrayerAlarmReceiver", "Successfully started PrayerAlarmService for $prayerName [isPreReminder=$isPreReminder]")
        } catch (e: Exception) {
            Log.e("PrayerAlarmReceiver", "Failed to start PrayerAlarmService foreground service: ${e.message}")
            try {
                // Post high-priority notification directly as fallback
                val notification = PrayerAlarmService.buildPrayerNotification(context, prayerName, prayerArabic, isPreReminder)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.notify(PrayerAlarmService.NOTIFICATION_ID, notification)
            } catch (ne: Exception) {
                Log.e("PrayerAlarmReceiver", "Fallback notification error: ${ne.message}")
            }
            // Fallback direct playback
            AdhanSoundPlayer.playAdhanAlert(context, looping = !isPreReminder)
        }
    }
}

