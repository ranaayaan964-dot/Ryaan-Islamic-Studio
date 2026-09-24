package com.example.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.data.model.PrayerTimeItem
import com.example.data.model.PrayerType
import com.example.data.repository.AladhanRepository
import java.util.Calendar

object PrayerAlarmScheduler {

    private const val TAG = "PrayerAlarmScheduler"
    private const val PREFS_NAME = "sahiwal_prayer_alarms_prefs"

    const val EXTRA_ALERT_TYPE = "EXTRA_ALERT_TYPE"
    const val ALERT_TYPE_15_MIN = "15_MIN"
    const val ALERT_TYPE_5_MIN_AUDIO = "5_MIN_AUDIO"
    const val ALERT_TYPE_EXACT = "EXACT"

    const val REQUEST_CODE_OFFSET_15_MIN = 1000
    const val REQUEST_CODE_OFFSET_5_MIN = 2000

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch exact alarm settings: ${e.message}")
            }
        }
    }

    /**
     * Ensures all 5 daily prayer alarms are armed and persistent, even on first install or after app reboot.
     */
    fun ensureAllAlarmsScheduled(context: Context) {
        PrayerAlarmService.createNotificationChannel(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasSavedTimes = PrayerType.values().any { 
            it != PrayerType.SUNRISE && prefs.getLong("time_${it.name}", 0L) > 0L 
        }

        if (hasSavedTimes) {
            Log.d(TAG, "Restoring existing prayer alarms from preferences...")
            rescheduleFromStorage(context)
        } else {
            Log.d(TAG, "Initializing default daily prayer alarms for Sahiwal...")
            val (_, fallbackItems) = AladhanRepository.getSahiwalFallbackSchedule()
            scheduleAllActivePrayers(context, fallbackItems)
        }
    }

    /**
     * Automatically advances the alarm for the next occurrence (+24 hours) after an alarm triggers,
     * ensuring that background alerts fire seamlessly every single day without requiring the app to be reopened.
     */
    fun reschedulePrayerForNextDay(context: Context, prayerName: String) {
        val type = PrayerType.values().find { 
            it.displayName.equals(prayerName, ignoreCase = true) || 
            it.name.equals(prayerName, ignoreCase = true) 
        } ?: return

        if (type == PrayerType.SUNRISE) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("enabled_${type.name}", true)
        if (!isEnabled) return

        var storedTime = prefs.getLong("time_${type.name}", 0L)
        val formatted = prefs.getString("fmt_${type.name}", "") ?: ""
        val now = System.currentTimeMillis()

        if (storedTime <= 0L) {
            val (_, fallbackItems) = AladhanRepository.getSahiwalFallbackSchedule()
            val item = fallbackItems.find { it.type == type }
            if (item != null) {
                storedTime = item.timeMillis
            }
        }

        var nextTriggerTime = storedTime
        while (nextTriggerTime <= now) {
            nextTriggerTime += 24 * 60 * 60 * 1000L
        }

        val updatedItem = PrayerTimeItem(
            type = type,
            timeFormatted = formatted.ifEmpty { type.displayName },
            timeMillis = nextTriggerTime,
            isAlarmEnabled = true
        )

        schedulePrayerAlarm(context, updatedItem)

        prefs.edit()
            .putLong("time_${type.name}", nextTriggerTime)
            .putBoolean("enabled_${type.name}", true)
            .apply()

        Log.d(TAG, "Rescheduled $prayerName for tomorrow at $nextTriggerTime")
    }

    @SuppressLint("ScheduleExactAlarm")
    fun schedulePrayerAlarm(context: Context, prayer: PrayerTimeItem) {
        if (!prayer.isAlarmEnabled || prayer.type == PrayerType.SUNRISE) {
            cancelPrayerAlarm(context, prayer)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        var triggerTime = prayer.timeMillis
        val now = System.currentTimeMillis()

        // Advance to next occurrence in the future (today or tomorrow)
        while (triggerTime <= now) {
            triggerTime += 24 * 60 * 60 * 1000L
        }

        val baseRequestCode = prayer.type.ordinal

        // 1. EXACT PRAYER ALARM (Full Adhan & Lock-Screen Takeover)
        val exactIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = "com.example.ACTION_PRAYER_ALARM"
            putExtra("prayer_name", prayer.type.displayName)
            putExtra("prayer_arabic", prayer.type.arabicName)
            putExtra(EXTRA_ALERT_TYPE, ALERT_TYPE_EXACT)
            putExtra("is_pre_reminder", false)
        }
        val exactPendingIntent = PendingIntent.getBroadcast(
            context,
            baseRequestCode,
            exactIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleExactOrAllowWhileIdle(alarmManager, triggerTime, exactPendingIntent)
        Log.d(TAG, "Scheduled EXACT alarm for ${prayer.type.displayName} at $triggerTime (rc=$baseRequestCode)")

        // 2. 15-MINUTE PRE-REMINDER (High-Priority Notification + Gentle Chime)
        val pre15Time = triggerTime - (15 * 60 * 1000L)
        if (pre15Time > now) {
            val pre15Intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = "com.example.ACTION_PRAYER_ALARM"
                putExtra("prayer_name", prayer.type.displayName)
                putExtra("prayer_arabic", prayer.type.arabicName)
                putExtra(EXTRA_ALERT_TYPE, ALERT_TYPE_15_MIN)
                putExtra("is_pre_reminder", true)
            }
            val pre15RequestCode = baseRequestCode + REQUEST_CODE_OFFSET_15_MIN
            val pre15PendingIntent = PendingIntent.getBroadcast(
                context,
                pre15RequestCode,
                pre15Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleExactOrAllowWhileIdle(alarmManager, pre15Time, pre15PendingIntent)
            Log.d(TAG, "Scheduled 15-MIN pre-reminder for ${prayer.type.displayName} at $pre15Time (rc=$pre15RequestCode)")
        }

        // 3. 5-MINUTE AUDIO ALERT ENGINE (Foreground Service + Quranic Ayat / Durood / Naat)
        val pre5Time = triggerTime - (5 * 60 * 1000L)
        if (pre5Time > now) {
            val pre5Intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = "com.example.ACTION_PRAYER_ALARM"
                putExtra("prayer_name", prayer.type.displayName)
                putExtra("prayer_arabic", prayer.type.arabicName)
                putExtra(EXTRA_ALERT_TYPE, ALERT_TYPE_5_MIN_AUDIO)
                putExtra("is_pre_reminder", false)
            }
            val pre5RequestCode = baseRequestCode + REQUEST_CODE_OFFSET_5_MIN
            val pre5PendingIntent = PendingIntent.getBroadcast(
                context,
                pre5RequestCode,
                pre5Intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleExactOrAllowWhileIdle(alarmManager, pre5Time, pre5PendingIntent)
            Log.d(TAG, "Scheduled 5-MIN AUDIO alert for ${prayer.type.displayName} at $pre5Time (rc=$pre5RequestCode)")
        }
    }

    private fun scheduleExactOrAllowWhileIdle(
        alarmManager: AlarmManager,
        triggerTime: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (se: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm: ${se.message}, falling back to setAndAllowWhileIdle")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (ex2: Exception) {
                Log.e(TAG, "Fallback alarm scheduling error: ${ex2.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}")
        }
    }

    fun cancelPrayerAlarm(context: Context, prayer: PrayerTimeItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = "com.example.ACTION_PRAYER_ALARM"
        }
        val baseRequestCode = prayer.type.ordinal

        // 1. Cancel EXACT alarm
        val exactPendingIntent = PendingIntent.getBroadcast(
            context,
            baseRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(exactPendingIntent)

        // 2. Cancel 15-MIN pre-reminder
        val pre15PendingIntent = PendingIntent.getBroadcast(
            context,
            baseRequestCode + REQUEST_CODE_OFFSET_15_MIN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pre15PendingIntent)

        // 3. Cancel 5-MIN audio alert
        val pre5PendingIntent = PendingIntent.getBroadcast(
            context,
            baseRequestCode + REQUEST_CODE_OFFSET_5_MIN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pre5PendingIntent)

        // 4. Cancel legacy pre-reminder (requestCode + 500) if scheduled previously
        val legacyPendingIntent = PendingIntent.getBroadcast(
            context,
            baseRequestCode + 500,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(legacyPendingIntent)
    }

    fun scheduleAllActivePrayers(context: Context, prayers: List<PrayerTimeItem>) {
        PrayerAlarmService.createNotificationChannel(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        val now = System.currentTimeMillis()
        
        prayers.forEach { prayer ->
            if (prayer.type != PrayerType.SUNRISE) {
                if (prayer.isAlarmEnabled) {
                    var triggerTime = prayer.timeMillis
                    while (triggerTime <= now) {
                        triggerTime += 24 * 60 * 60 * 1000L
                    }
                    val itemToSchedule = prayer.copy(timeMillis = triggerTime)
                    schedulePrayerAlarm(context, itemToSchedule)
                    prefs.putLong("time_${prayer.type.name}", triggerTime)
                    prefs.putString("fmt_${prayer.type.name}", prayer.timeFormatted)
                    prefs.putBoolean("enabled_${prayer.type.name}", true)
                } else {
                    cancelPrayerAlarm(context, prayer)
                    prefs.putBoolean("enabled_${prayer.type.name}", false)
                }
            }
        }
        prefs.apply()
        Log.d(TAG, "Scheduled and persisted all active daily prayers (Fajr, Dhuhr, Asr, Maghrib, Isha)")
    }

    fun rescheduleFromStorage(context: Context) {
        PrayerAlarmService.createNotificationChannel(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prayerTypes = listOf(
            PrayerType.FAJR,
            PrayerType.DHUHR,
            PrayerType.ASR,
            PrayerType.MAGHRIB,
            PrayerType.ISHA
        )

        val now = System.currentTimeMillis()
        val editor = prefs.edit()

        for (type in prayerTypes) {
            val enabled = prefs.getBoolean("enabled_${type.name}", true)
            var timeMillis = prefs.getLong("time_${type.name}", 0L)
            val formatted = prefs.getString("fmt_${type.name}", "") ?: ""

            if (enabled) {
                if (timeMillis <= 0L) {
                    val (_, fallbackItems) = AladhanRepository.getSahiwalFallbackSchedule()
                    timeMillis = fallbackItems.find { it.type == type }?.timeMillis ?: 0L
                }

                while (timeMillis <= now && timeMillis > 0L) {
                    timeMillis += 24 * 60 * 60 * 1000L
                }

                if (timeMillis > 0L) {
                    val item = PrayerTimeItem(
                        type = type,
                        timeFormatted = formatted.ifEmpty { type.displayName },
                        timeMillis = timeMillis,
                        isAlarmEnabled = true
                    )
                    schedulePrayerAlarm(context, item)
                    editor.putLong("time_${type.name}", timeMillis)
                    editor.putBoolean("enabled_${type.name}", true)
                }
            }
        }
        editor.apply()
        Log.d(TAG, "Restored and rescheduled all stored prayer alarms")
    }

    /**
     * Triggers an immediate Azan alarm for user verification
     */
    fun triggerTestAlarmNow(
        context: Context,
        prayerName: String = "Asr",
        alertType: String = ALERT_TYPE_EXACT
    ) {
        val serviceIntent = Intent(context, PrayerAlarmService::class.java).apply {
            this.action = PrayerAlarmService.ACTION_START_AZAN
            putExtra(PrayerAlarmService.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmService.EXTRA_PRAYER_ARABIC, "العصر")
            putExtra(PrayerAlarmService.EXTRA_ALERT_TYPE, alertType)
            putExtra(PrayerAlarmService.EXTRA_IS_PRE_REMINDER, alertType == ALERT_TYPE_15_MIN)
            putExtra(PrayerAlarmService.EXTRA_IS_TEST_TRIGGER, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
