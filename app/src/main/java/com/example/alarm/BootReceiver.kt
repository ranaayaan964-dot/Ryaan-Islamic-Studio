package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.util.PrayerCalculator
import java.util.Calendar

/**
 * MODULE 17: BULLETPROOF BACKGROUND PERSISTENCE & OEM AUTO-START (BOOT RECEIVER)
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"

        fun scheduleDailyPrayerAlarms(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "Cannot schedule exact alarms - missing permission")
                    return
                }
            }

            // Latitude/Longitude default or cached (e.g. Makkah or user's city)
            val latitude = 21.4225
            val longitude = 39.8262
            val prayerTimes = PrayerCalculator.calculatePrayerTimes(latitude, longitude)

            prayerTimes.forEachIndexed { index, prayerItem ->
                try {
                    val triggerMillis = prayerItem.timeMillis
                    val effectiveMillis = if (triggerMillis < System.currentTimeMillis()) {
                        triggerMillis + 24 * 60 * 60 * 1000L
                    } else {
                        triggerMillis
                    }

                    val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                        action = "com.example.ACTION_PRAYER_ALARM"
                        putExtra("prayer_name", prayerItem.type.displayName)
                        putExtra("prayer_time", prayerItem.timeFormatted)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        1000 + index,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Reschedule all 5 daily prayers using setExactAndAllowWhileIdle
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            effectiveMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            effectiveMillis,
                            pendingIntent
                        )
                    }

                    Log.d(TAG, "Rescheduled ${prayerItem.type.displayName} alarm for: $effectiveMillis")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed scheduling prayer: ${prayerItem.type.displayName}", e)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "BootReceiver received action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.i(TAG, "Device rebooted or app replaced. Rescheduling all 5 daily prayers immediately...")
            scheduleDailyPrayerAlarms(context)
        }
    }
}
