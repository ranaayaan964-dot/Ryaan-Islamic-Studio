package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reschedules persisted alarms after system boot or app replacement.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d(TAG, "BootReceiver received action: $action")
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                PrayerAlarmScheduler.ensureAllAlarmsScheduled(appContext)
                NamazWorkManagerScheduler.scheduleAllNamazFromMetadata(appContext)
                NamazWorkManagerScheduler.schedulePeriodicDailySync(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore alarms after $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
