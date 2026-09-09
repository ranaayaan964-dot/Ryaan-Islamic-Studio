package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrayerAlarmService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var autoTimeoutJob: Job? = null
    private var failSafeMediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        private const val TAG = "PrayerAlarmService"
        const val CHANNEL_ID = "sahiwal_prayer_alarm_channel"
        const val NOTIFICATION_ID = 9991

        const val ACTION_START_AZAN = "com.example.ACTION_START_AZAN"
        const val ACTION_STOP_AZAN = "com.example.ACTION_STOP_AZAN"

        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_PRAYER_ARABIC = "prayer_arabic"
        const val EXTRA_IS_PRE_REMINDER = "is_pre_reminder"
        const val EXTRA_IS_TEST_TRIGGER = "is_test_trigger"

        private val _isAlarmRinging = MutableStateFlow(false)
        val isAlarmRinging = _isAlarmRinging.asStateFlow()

        private val _activePrayerName = MutableStateFlow<String?>(null)
        val activePrayerName = _activePrayerName.asStateFlow()

        /**
         * Instantly triggers a test prayer alarm / notification for verification of sound,
         * vibration, and high-priority heads-up display without waiting for actual prayer times.
         */
        fun triggerTestAlarm(
            context: Context,
            prayerName: String,
            prayerArabic: String = "الصلاة",
            isPreReminder: Boolean = false
        ) {
            val intent = Intent(context, PrayerAlarmService::class.java).apply {
                action = ACTION_START_AZAN
                putExtra(EXTRA_PRAYER_NAME, prayerName)
                putExtra(EXTRA_PRAYER_ARABIC, prayerArabic)
                putExtra(EXTRA_IS_PRE_REMINDER, isPreReminder)
                putExtra(EXTRA_IS_TEST_TRIGGER, true)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Triggered test alert for $prayerName (preReminder=$isPreReminder)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start test alert service: ${e.message}")
            }
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
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
                        "Prayer Alarms & Azan Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Triggers local alarms, Azan alerts, and notifications for all 5 daily prayer times"
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

        fun buildPrayerNotification(
            context: Context,
            prayerName: String,
            prayerArabic: String,
            isPreReminder: Boolean = false,
            isTestTrigger: Boolean = false
        ): Notification {
            createNotificationChannel(context)

            // PendingIntent to STOP Azan
            val stopIntent = Intent(context, PrayerAlarmService::class.java).apply {
                action = ACTION_STOP_AZAN
            }
            val stopPendingIntent = PendingIntent.getService(
                context,
                1001,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // PendingIntent to open app
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("opened_from_alarm", true)
                putExtra("prayer_name", prayerName)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                prayerName.hashCode(),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val soundTitle = com.example.audio.PrayerSoundPreferences.getAssignedSoundTitleForPrayer(
                context,
                prayerName,
                isPreReminder
            )

            val testPrefix = if (isTestTrigger) "🔔 [TEST ALERT] " else ""
            val contentTitle = if (isPreReminder) {
                "${testPrefix}⏳ 15 Minutes to $prayerName Prayer ($prayerArabic)"
            } else {
                "${testPrefix}🕌 Time for $prayerName Prayer ($prayerArabic)"
            }

            val contentText = PrayerNotificationPreferences.getNotificationBodyText(
                context = context,
                prayerName = prayerName,
                isPreReminder = isPreReminder,
                soundTitle = soundTitle
            )

            val subText = when {
                isTestTrigger -> "Alert & Audio Verification Test"
                isPreReminder -> "Pre-Prayer Reminder"
                else -> "Prayer Alarm"
            }

            val shouldVibrate = PrayerNotificationPreferences.isVibrationEnabled(context)
            val vibrationPattern = if (shouldVibrate) longArrayOf(0, 500, 200, 500, 200, 800) else null

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSubText(subText)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "⏹ STOP ALARM",
                    stopPendingIntent
                )
                .setDeleteIntent(stopPendingIntent)

            if (vibrationPattern != null) {
                builder.setVibrate(vibrationPattern)
            }

            return builder.build()
        }

        fun stopAlarm(context: Context) {
            val stopIntent = Intent(context, PrayerAlarmService::class.java).apply {
                action = ACTION_STOP_AZAN
            }
            try {
                context.startService(stopIntent)
            } catch (e: Exception) {
                Log.w(TAG, "Could not startService to stop alarm: ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_AZAN) {
            Log.d(TAG, "Stopping Azan alarm via ACTION_STOP_AZAN")
            stopAzanAlarm()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerArabic = intent?.getStringExtra(EXTRA_PRAYER_ARABIC) ?: "الصلاة"
        val isPreReminder = intent?.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false) ?: false
        val isTestTrigger = intent?.getBooleanExtra(EXTRA_IS_TEST_TRIGGER, false) ?: false

        Log.d(TAG, "Starting Azan alarm for $prayerName ($prayerArabic) [isPreReminder=$isPreReminder, isTest=$isTestTrigger]")
        startAzanAlarm(prayerName, prayerArabic, isPreReminder, isTestTrigger)

        return START_NOT_STICKY
    }

    private fun startAzanAlarm(
        prayerName: String,
        prayerArabic: String,
        isPreReminder: Boolean = false,
        isTestTrigger: Boolean = false
    ) {
        _isAlarmRinging.value = true
        _activePrayerName.value = prayerName

        // Acquire wake lock with screen turn-on flag
        acquireWakeLock()

        // Build Full-screen high-priority notification
        val notification = buildPrayerNotification(this, prayerName, prayerArabic, isPreReminder, isTestTrigger)

        // Start foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Request AudioFocus & play alarm through STREAM_ALARM with user-configured sound
        playAlarmAudioWithAudioFocus(prayerName, isPreReminder)

        // Auto-dismiss safety timeout: 30s for test alarms, 45s for pre-reminders, 5m for regular
        val timeoutMs = when {
            isTestTrigger -> 30 * 1000L
            isPreReminder -> 45 * 1000L
            else -> 5 * 60 * 1000L
        }
        autoTimeoutJob?.cancel()
        autoTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(timeoutMs)
            Log.d(TAG, "Azan alarm reached auto-timeout ($timeoutMs ms)")
            stopAzanAlarm()
        }
    }

    private fun playAlarmAudioWithAudioFocus(prayerName: String, isPreReminder: Boolean = false) {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(AudioManager.STREAM_ALARM)
            .build()

        // 1. Request AudioFocus specifically for ALARM stream
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { focusChange ->
                        Log.d(TAG, "Azan AudioFocus changed: $focusChange")
                    }
                    .build()
                audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed requesting AudioFocus: ${e.message}")
        }

        // 2. Ensure STREAM_ALARM volume is audible / maximum
        audioManager?.let { am ->
            try {
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                val currentVol = am.getStreamVolume(AudioManager.STREAM_ALARM)
                if (currentVol < (maxVol * 0.7f)) {
                    am.setStreamVolume(
                        AudioManager.STREAM_ALARM,
                        (maxVol * 0.95f).toInt().coerceAtLeast(1),
                        0
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not set alarm volume: ${e.message}")
            }
        }

        // 3. Dynamic User Audio URI Playback from SharedPreferences
        try {
            val dynamicUriString = com.example.audio.PrayerSoundPreferences.getSoundUriForPrayer(
                this,
                prayerName,
                isPreReminder
            )
            val dynamicUri = android.net.Uri.parse(dynamicUriString)
            Log.d(TAG, "Attempting playback of user-selected tone for $prayerName: $dynamicUriString")

            failSafeMediaPlayer?.release()
            failSafeMediaPlayer = MediaPlayer().apply {
                setDataSource(this@PrayerAlarmService, dynamicUri)
                setAudioAttributes(audioAttributes)
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_ALARM)
                isLooping = !isPreReminder // regular alarms loop until stopped, short reminder plays once
                prepare()
                start()
            }
            Log.d(TAG, "Successfully started dynamic MediaPlayer on STREAM_ALARM: $dynamicUriString")
            return
        } catch (e: Exception) {
            Log.e(TAG, "Selected sound playback failed: ${e.message}, attempting bundled raw fallback")
        }

        // Fallback 1: Try local raw bundled sound
        try {
            val rawName = if (isPreReminder) "short_zikr_beep" 
                          else if (prayerName.equals("Fajr", true)) "fajr_special"
                          else "standard_adhan"
            val rawUri = android.net.Uri.parse("android.resource://${packageName}/raw/$rawName")
            failSafeMediaPlayer?.release()
            failSafeMediaPlayer = MediaPlayer().apply {
                setDataSource(this@PrayerAlarmService, rawUri)
                setAudioAttributes(audioAttributes)
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_ALARM)
                isLooping = !isPreReminder
                prepare()
                start()
            }
            Log.d(TAG, "Fallback raw MediaPlayer playing $rawName on STREAM_ALARM")
            return
        } catch (e: Exception) {
            Log.e(TAG, "Bundled raw fallback failed: ${e.message}, trying system alarm ringtone")
        }

        // Fallback 2: System Alarm / Ringtone
        try {
            var alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            if (alertUri != null) {
                failSafeMediaPlayer?.release()
                failSafeMediaPlayer = MediaPlayer().apply {
                    setDataSource(this@PrayerAlarmService, alertUri)
                    setAudioAttributes(audioAttributes)
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = !isPreReminder
                    prepare()
                    start()
                }
                Log.d(TAG, "Fail-safe MediaPlayer playing system ringtone through STREAM_ALARM")
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "System ringtone playback failed: ${e.message}, falling back to AdhanSoundPlayer")
        }

        // Fallback 3: AdhanSoundPlayer synthesized alert
        AdhanSoundPlayer.playAdhanAlert(this, looping = !isPreReminder)
    }

    private fun stopAzanAlarm() {
        autoTimeoutJob?.cancel()
        autoTimeoutJob = null

        _isAlarmRinging.value = false
        _activePrayerName.value = null

        try {
            failSafeMediaPlayer?.stop()
            failSafeMediaPlayer?.release()
        } catch (_: Exception) {}
        failSafeMediaPlayer = null

        AdhanSoundPlayer.stopAdhan()

        // Abandon audio focus
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager?.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error abandoning audio focus: ${e.message}")
        }
        audioFocusRequest = null
        audioManager = null

        releaseWakeLock()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(NOTIFICATION_ID)

        stopSelf()
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (wakeLock == null) {
                wakeLock = powerManager?.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                    "SahiwalPrayer:AzanAlarmWakeLock"
                )?.apply {
                    setReferenceCounted(false)
                }
            }
            wakeLock?.acquire(6 * 60 * 1000L) // 6 minutes max
        } catch (e: Exception) {
            Log.e(TAG, "Failed acquiring wake lock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock: ${e.message}")
        }
        wakeLock = null
    }

    override fun onDestroy() {
        stopAzanAlarm()
        super.onDestroy()
    }
}
