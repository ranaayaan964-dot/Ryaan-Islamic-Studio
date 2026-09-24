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
import androidx.core.app.ServiceCompat
import com.example.R
import com.example.audio.SnoozeAudioDownloader
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * REMEDIATION A1: HARDENED PRAYER ALARM SERVICE (ANDROID 14 & 15 READY)
 * - Immediate foreground promotion with ServiceCompat.startForeground
 * - Safe handling of nullable MediaPlayer.create()
 * - Structured serviceScope lifecycle with SupervisorJob
 * - Proper WakeLock & AudioFocus cleanup
 */
class PrayerAlarmService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var autoTimeoutJob: Job? = null

    companion object {
        private const val TAG = "PrayerAlarmService"
        const val CHANNEL_ID = "ryaan_prayer_alarm_channel"
        const val NOTIFICATION_ID = 9991
        const val NOTIFICATION_ID_PRE15 = 9992
        const val NOTIFICATION_ID_PRE5 = 9993

        const val ACTION_START_AZAN = "com.example.ACTION_START_AZAN"
        const val ACTION_STOP_AZAN = "com.example.ACTION_STOP_AZAN"

        const val EXTRA_PRAYER_NAME = "prayer_name"
        const val EXTRA_PRAYER_ARABIC = "prayer_arabic"
        const val EXTRA_IS_PRE_REMINDER = "is_pre_reminder"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        const val EXTRA_IS_TEST_TRIGGER = "is_test_trigger"

        const val EXTRA_ALERT_TYPE = "EXTRA_ALERT_TYPE"
        const val ALERT_TYPE_15_MIN = "15_MIN"
        const val ALERT_TYPE_5_MIN_AUDIO = "5_MIN_AUDIO"
        const val ALERT_TYPE_EXACT = "EXACT"

        fun triggerTestAlarm(
            context: Context,
            prayerName: String = "Fajr",
            prayerArabic: String = "",
            isPreReminder: Boolean = false,
            alertType: String = if (isPreReminder) ALERT_TYPE_15_MIN else ALERT_TYPE_EXACT
        ) {
            val intent = Intent(context, PrayerAlarmService::class.java).apply {
                action = ACTION_START_AZAN
                putExtra(EXTRA_PRAYER_NAME, prayerName)
                putExtra(EXTRA_PRAYER_ARABIC, prayerArabic)
                putExtra(EXTRA_IS_PRE_REMINDER, isPreReminder || alertType == ALERT_TYPE_15_MIN)
                putExtra(EXTRA_ALERT_TYPE, alertType)
                putExtra(EXTRA_IS_TEST_TRIGGER, true)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start PrayerAlarmService for test alarm", e)
            }
        }

        private val _isAlarmRinging = MutableStateFlow(false)
        val isAlarmRinging = _isAlarmRinging.asStateFlow()

        private val _activePrayerName = MutableStateFlow<String?>(null)
        val activePrayerName = _activePrayerName.asStateFlow()

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
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

        // Immediately promote to foreground in onCreate to satisfy Android 14+ strict timeouts
        val initialNotification = buildForegroundNotification(
            prayerName = "Prayer",
            prayerArabic = "الصلاة",
            isPreReminder = false,
            isSnooze = false
        )
        promoteToForeground(initialNotification, NOTIFICATION_ID)

        // Acquire partial WakeLock with safety ceiling (10 minutes)
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RyaanStudio:AlarmWakeLock").apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L)
            }
            Log.d(TAG, "PrayerAlarmService onCreate: WakeLock acquired for 10 mins")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wake lock", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_AZAN) {
            Log.d(TAG, "Received ACTION_STOP_AZAN. Stopping service and releasing resources.")
            stopAzanAlarm()
            stopSelf()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val prayerArabic = intent?.getStringExtra(EXTRA_PRAYER_ARABIC) ?: "الصلاة"
        val isPreReminder = intent?.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false) ?: false
        val isSnooze = intent?.getBooleanExtra(EXTRA_IS_SNOOZE, false) ?: false

        val alertType = intent?.getStringExtra(EXTRA_ALERT_TYPE)
            ?: if (isPreReminder) ALERT_TYPE_15_MIN else ALERT_TYPE_EXACT

        Log.d(TAG, "onStartCommand: prayer=$prayerName ($prayerArabic), alertType=$alertType, isSnooze=$isSnooze")

        return when (alertType) {
            ALERT_TYPE_15_MIN -> {
                handle15MinPreReminder(prayerName, prayerArabic)
                START_NOT_STICKY
            }
            ALERT_TYPE_5_MIN_AUDIO -> {
                handle5MinAudioAlert(prayerName, prayerArabic)
                START_STICKY
            }
            else -> {
                handleExactPrayerAlarm(prayerName, prayerArabic, isSnooze)
                START_STICKY
            }
        }
    }

    /**
     * 15-MINUTE PRE-REMINDER:
     * Shows high-priority notification and plays a short gentle chime.
     * No foreground loop needed; demotes and stops once played.
     */
    private fun handle15MinPreReminder(prayerName: String, prayerArabic: String) {
        _activePrayerName.value = prayerName

        val notification = buildPre15Notification(prayerName, prayerArabic)
        promoteToForeground(notification, NOTIFICATION_ID_PRE15)

        playGentleChime {
            Log.d(TAG, "15-min pre-reminder chime finished. Releasing foreground service.")
            stopExistingPlayer()
            releaseWakeLock()
            @Suppress("DEPRECATION")
            stopForeground(false)
            stopSelf()
        }

        // Safety fallback timer for chime
        autoTimeoutJob?.cancel()
        autoTimeoutJob = serviceScope.launch {
            delay(12 * 1000L)
            stopExistingPlayer()
            releaseWakeLock()
            @Suppress("DEPRECATION")
            stopForeground(false)
            stopSelf()
        }
    }

    /**
     * 5-MINUTE AUDIO ALERT ENGINE:
     * Starts Foreground Service, shows notification with Jamaat countdown,
     * and uses MediaPlayer to play Quranic Ayat / Durood audio (R.raw.pre_prayer_ayat).
     */
    private fun handle5MinAudioAlert(prayerName: String, prayerArabic: String) {
        _isAlarmRinging.value = true
        _activePrayerName.value = prayerName

        val notification = buildPre5Notification(prayerName, prayerArabic)
        promoteToForeground(notification, NOTIFICATION_ID_PRE5)

        playPrePrayerAyatAudio()

        // 5-minute safety timeout to auto-stop right before exact prayer time
        autoTimeoutJob?.cancel()
        autoTimeoutJob = serviceScope.launch {
            delay(5 * 60 * 1000L)
            Log.d(TAG, "5-min pre-prayer audio alert timeout reached.")
            stopAzanAlarm()
            stopSelf()
        }
    }

    /**
     * EXACT PRAYER TIME:
     * Full-screen notification, full Adhan audio loop, and AlarmRingingActivity launch.
     */
    private fun handleExactPrayerAlarm(prayerName: String, prayerArabic: String, isSnooze: Boolean) {
        _isAlarmRinging.value = true
        _activePrayerName.value = prayerName

        val notification = buildForegroundNotification(prayerName, prayerArabic, isPreReminder = false, isSnooze = isSnooze)
        promoteToForeground(notification, NOTIFICATION_ID)

        playAlarmAudio(prayerName, isSnooze)

        // Launch AlarmRingingActivity for full-screen alarm experience
        try {
            val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
                putExtra("PRAYER_NAME", prayerName)
                putExtra("PRAYER_ARABIC", prayerArabic)
                putExtra("IS_SNOOZE", isSnooze)
                putExtra(EXTRA_ALERT_TYPE, ALERT_TYPE_EXACT)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(fullScreenIntent)
            Log.d(TAG, "Launched AlarmRingingActivity for exact prayer alarm")
        } catch (e: Exception) {
            Log.w(TAG, "Could not directly launch AlarmRingingActivity: ${e.message}")
        }

        // Auto-dismiss safety timeout: 7 minutes
        autoTimeoutJob?.cancel()
        autoTimeoutJob = serviceScope.launch {
            delay(7 * 60 * 1000L)
            Log.d(TAG, "Azan alarm auto-timeout reached. Stopping service.")
            stopAzanAlarm()
            stopSelf()
        }
    }

    private fun promoteToForeground(notification: Notification, notificationId: Int = NOTIFICATION_ID) {
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(
                this,
                notificationId,
                notification,
                fgsType
            )
        } catch (e: Exception) {
            Log.e(TAG, "ServiceCompat.startForeground failed", e)
        }
    }

    private fun buildPre15Notification(prayerName: String, prayerArabic: String): Notification {
        val openIntent = Intent(this, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("opened_from_alarm", true)
            putExtra("prayer_name", prayerName)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            1015,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⏳ 15 mins to $prayerName, time for Wudu"
        val text = "Prepare for $prayerName ($prayerArabic). Time for Wudu and Sunnah prayers."

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()
    }

    private fun buildPre5Notification(prayerName: String, prayerArabic: String): Notification {
        val stopIntent = Intent(this, PrayerAlarmService::class.java).apply {
            action = ACTION_STOP_AZAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1005,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openIntent = Intent(this, com.example.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("opened_from_alarm", true)
            putExtra("prayer_name", prayerName)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            1006,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "✨ 5 minutes remaining to Jamaat"
        val text = "Reciting Quranic Ayat & Durood for $prayerName ($prayerArabic). Head towards the Masjid."

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Recitation", stopPendingIntent)
            .build()
    }

    private fun playGentleChime(onComplete: () -> Unit) {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(AudioManager.STREAM_NOTIFICATION)
            .build()

        stopExistingPlayer()

        val shortZikrRes = resources.getIdentifier("short_zikr_beep", "raw", packageName)
        val subhanallahRes = resources.getIdentifier("subhanallah_reminder", "raw", packageName)

        if (shortZikrRes != 0 || subhanallahRes != 0) {
            try {
                val player = if (shortZikrRes != 0) {
                    MediaPlayer.create(
                        this,
                        shortZikrRes,
                        audioAttributes,
                        audioManager?.generateAudioSessionId() ?: 0
                    )
                } else {
                    null
                } ?: if (subhanallahRes != 0) {
                    MediaPlayer.create(
                        this,
                        subhanallahRes,
                        audioAttributes,
                        audioManager?.generateAudioSessionId() ?: 0
                    )
                } else null

                if (player != null) {
                    player.isLooping = false
                    player.setOnCompletionListener { onComplete() }
                    player.start()
                    mediaPlayer = player
                    Log.d(TAG, "Playing gentle 15-min notification chime")
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gentle chime player creation failed: ${e.message}")
            }
        }

        // Fallback to system notification ringtone
        try {
            val notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (notifUri != null) {
                val player = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(this@PrayerAlarmService, notifUri)
                    isLooping = false
                    setOnCompletionListener { onComplete() }
                    prepare()
                    start()
                }
                mediaPlayer = player
                return
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Fallback notification ringtone failed", ex)
        }

        onComplete()
    }

    private fun playPrePrayerAyatAudio() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setLegacyStreamType(AudioManager.STREAM_ALARM)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .build()
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        }

        stopExistingPlayer()

        // 0. Dynamic Local / Downloaded Storage Alternative (avoids bundling large audio files > 20MB in res/raw)
        val candidateFiles = listOf(
            File(filesDir, "pre_prayer_ayat.mp3"),
            File(filesDir, "snooze_ayatul_kursi.mp3"),
            File(File(filesDir, "islamic_sounds"), "ayat_al_kursi.mp3")
        )
        for (candidate in candidateFiles) {
            if (candidate.exists() && candidate.length() > 5000) {
                try {
                    val player = MediaPlayer().apply {
                        setAudioAttributes(audioAttributes)
                        setDataSource(candidate.absolutePath)
                        isLooping = true
                        prepare()
                        start()
                    }
                    mediaPlayer = player
                    Log.d(TAG, "MediaPlayer successfully playing dynamic audio from: ${candidate.name}")
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "Failed playing local dynamic audio ${candidate.name}: ${e.message}")
                }
            }
        }

        // 1. Primary: Bundled pre_prayer_ayat if present
        val prePrayerRes = resources.getIdentifier("pre_prayer_ayat", "raw", packageName)
        if (prePrayerRes != 0) {
            try {
                val player = MediaPlayer.create(
                    this,
                    prePrayerRes,
                    audioAttributes,
                    audioManager?.generateAudioSessionId() ?: 0
                )
                if (player != null) {
                    player.isLooping = true
                    player.start()
                    mediaPlayer = player
                    Log.d(TAG, "MediaPlayer successfully playing pre_prayer_ayat")
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed playing pre_prayer_ayat: ${e.message}")
            }
        }

        // 2. Fallback: Bundled subhanallah_reminder if present
        val subhanallahFallbackRes = resources.getIdentifier("subhanallah_reminder", "raw", packageName)
        if (subhanallahFallbackRes != 0) {
            try {
                val player = MediaPlayer.create(
                    this,
                    subhanallahFallbackRes,
                    audioAttributes,
                    audioManager?.generateAudioSessionId() ?: 0
                )
                if (player != null) {
                    player.isLooping = true
                    player.start()
                    mediaPlayer = player
                    Log.d(TAG, "MediaPlayer playing fallback subhanallah_reminder")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed playing fallback subhanallah_reminder", e)
            }
        }

        // 3. Dynamic Remote Streaming Alternative (Streaming from remote CDN)
        try {
            val streamUrl = "https://everyayah.com/data/Alafasy_128kbps/002255.mp3"
            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(streamUrl)
                isLooping = true
                setOnPreparedListener { mp ->
                    mp.start()
                    Log.d(TAG, "Remote dynamic stream started playing successfully")
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Failed initializing remote streaming alternative: ${e.message}")
        }
    }

    private fun playAlarmAudio(prayerName: String, isSnooze: Boolean) {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setLegacyStreamType(AudioManager.STREAM_ALARM)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(audioAttributes)
                .build()
            audioFocusRequest?.let { audioManager?.requestAudioFocus(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        }

        stopExistingPlayer()

        // Check for downloaded Snooze Ayat MP3 if snooze
        val snoozeFile = SnoozeAudioDownloader.getLocalSnoozeAudioFile(this)
        if (isSnooze && snoozeFile != null && snoozeFile.exists() && snoozeFile.length() > 0) {
            try {
                Log.d(TAG, "Playing local Snooze Ayat MP3: ${snoozeFile.absolutePath}")
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(snoozeFile.absolutePath)
                    isLooping = true
                    prepare()
                    start()
                }
                return
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play local snooze MP3, falling back to standard sound", e)
            }
        }

        // Fallback or standard Adhan sound
        val rawResName = if (prayerName.contains("Fajr", ignoreCase = true)) {
            "fajr_special"
        } else {
            "standard_adhan"
        }
        val rawResId = resources.getIdentifier(rawResName, "raw", packageName)

        if (rawResId != 0) {
            try {
                val player = MediaPlayer.create(
                    this,
                    rawResId,
                    audioAttributes,
                    audioManager?.generateAudioSessionId() ?: 0
                )
                if (player != null) {
                    player.isLooping = true
                    player.start()
                    mediaPlayer = player
                    Log.d(TAG, "MediaPlayer successfully playing Adhan loop on STREAM_ALARM")
                    return
                } else {
                    Log.w(TAG, "MediaPlayer.create returned null, falling back to system ringtone")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize standard Adhan player from raw resource", e)
            }
        }

        // Secondary fallback to default system alarm/ringtone
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        if (ringtoneUri != null) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(this@PrayerAlarmService, ringtoneUri)
                    isLooping = true
                    prepare()
                    start()
                }
                Log.d(TAG, "MediaPlayer playing fallback system ringtone")
            } catch (ex: Exception) {
                Log.e(TAG, "Failed fallback ringtone player", ex)
            }
        }
    }

    private fun buildForegroundNotification(
        prayerName: String,
        prayerArabic: String,
        isPreReminder: Boolean,
        isSnooze: Boolean
    ): Notification {
        val stopIntent = Intent(this, PrayerAlarmService::class.java).apply {
            action = ACTION_STOP_AZAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1001,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, AlarmRingingActivity::class.java).apply {
            putExtra("PRAYER_NAME", prayerName)
            putExtra("PRAYER_ARABIC", prayerArabic)
            putExtra("IS_SNOOZE", isSnooze)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            1002,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isSnooze) "Snooze Alert - $prayerName ($prayerArabic)" else "Time for $prayerName ($prayerArabic)"
        val text = "Adhan is calling. Tap to view or snooze."

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Adhan", stopPendingIntent)
            .build()
    }

    private fun stopExistingPlayer() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "WakeLock released")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing WakeLock", e)
        }
    }

    private fun stopAzanAlarm() {
        _isAlarmRinging.value = false
        _activePrayerName.value = null
        autoTimeoutJob?.cancel()
        stopExistingPlayer()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }

        releaseWakeLock()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIFICATION_ID)
        nm?.cancel(NOTIFICATION_ID_PRE5)
        nm?.cancel(NOTIFICATION_ID_PRE15)

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAzanAlarm()
        serviceScope.cancel()
    }
}

