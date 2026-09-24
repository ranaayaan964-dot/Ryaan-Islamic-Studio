package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * MODULE 4: THE 7-MINUTE EXACT SNOOZE ENGINE (RECEIVER LOGIC)
 */
class SnoozeReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SnoozeReceiver"
        const val SNOOZE_REQUEST_CODE = 7001

        fun scheduleSnooze(context: Context, prayerName: String, prayerArabic: String = "الصلاة") {
            // 1. Stop PrayerAlarmService (stopping MediaPlayer and releasing WakeLock)
            PrayerAlarmService.stopAlarm(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(PrayerAlarmService.NOTIFICATION_ID)

            // 2. Calculate exact timestamp: +7 minutes
            val snoozeTimeMillis = System.currentTimeMillis() + (7 * 60 * 1000L)

            // 3. Prepare PendingIntent for SnoozeReceiver
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SnoozeReceiver::class.java).apply {
                putExtra("PRAYER_NAME", prayerName)
                putExtra("PRAYER_ARABIC", prayerArabic)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                SNOOZE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 4. Schedule exact alarm while idle
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            snoozeTimeMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            snoozeTimeMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        snoozeTimeMillis,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Exact 7-minute snooze scheduled for $snoozeTimeMillis ($prayerName)")
            } catch (e: SecurityException) {
                Log.e(TAG, "Exact alarm permission missing, fallback to standard alarm", e)
                alarmManager.set(AlarmManager.RTC_WAKEUP, snoozeTimeMillis, pendingIntent)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
        val prayerArabic = intent.getStringExtra("PRAYER_ARABIC") ?: "الصلاة"

        Log.d(TAG, "7-Minute Snooze expired! Ringing alarm again for $prayerName")

        // Trigger alarm receiver with isSnooze = true
        val alarmIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra("PRAYER_NAME", prayerName)
            putExtra("PRAYER_ARABIC", prayerArabic)
            putExtra("is_snooze", true)
        }
        context.sendBroadcast(alarmIntent)
    }
}
