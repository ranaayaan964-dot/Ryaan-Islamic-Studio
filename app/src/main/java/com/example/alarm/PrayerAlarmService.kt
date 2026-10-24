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
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.R
import com.example.audio.PrayerSoundPreferences
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
 * REMEDIATION A1: HARDENED PRAYER ALARM SERVICE (VERSION 1.1 CUSTOM SOUNDS ENGINE)
 * - Dynamic sound retrieval using PrayerSoundPreferences.getSoundUriForPrayer()
 * - Immediate foreground promotion with ServiceCompat.startForeground
 * - Safe handling of MediaPlayer and dynamic audio sources (raw, file, http)
 * - Structured serviceScope lifecycle with SupervisorJob
 * - Proper WakeLock & AudioFocus cleanup
 */
class PrayerAlarmService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var originalAlarmVolume: Int? = null
    private var fallbackToneGenerator: ToneGenerator? = null
    private var fallbackToneJob: Job? = null
    private var foregroundStarted = false
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

        acquireWakeLock()
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "RyaanIslamicApp:PrayerAlarmWakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire()
            }
            Log.d(TAG, "WakeLock acquired for prayer alarm")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!foregroundStarted) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_STOP_AZAN -> {
                Log.d(TAG, "ACTION_STOP_AZAN received. Halting audio and dismissing foreground service.")
                stopAzanAlarm()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_AZAN -> {
                val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
                val prayerArabic = intent.getStringExtra(EXTRA_PRAYER_ARABIC) ?: "الصلاة"
                val isPreReminder = intent.getBooleanExtra(EXTRA_IS_PRE_REMINDER, false)
                val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
                val alertType = intent.getStringExtra(EXTRA_ALERT_TYPE) ?: if (isPreReminder) ALERT_TYPE_15_MIN else ALERT_TYPE_EXACT

                Log.d(TAG, "Triggering alarm: prayer=$prayerName, alertType=$alertType, isSnooze=$isSnooze")

                when (alertType) {
                    ALERT_TYPE_15_MIN -> {
                        handle15MinPreReminder(prayerName, prayerArabic)
                    }
                    ALERT_TYPE_5_MIN_AUDIO -> {
                        handle5MinAudioAlert(prayerName, prayerArabic)
                    }
                    else -> {
                        handleExactPrayerAlarm(prayerName, prayerArabic, isSnooze)
                    }
                }
            }
        }

        return START_NOT_STICKY
    }

    /**
     * 15-MINUTE PRE-REMINDER:
     * Gentle chime sound, high-priority heads-up reminder notification for Wudu and Sunnah prayers.
     */
    private fun handle15MinPreReminder(prayerName: String, prayerArabic: String) {
        val notification = buildPre15Notification(prayerName, prayerArabic)
        promoteToForeground(notification, NOTIFICATION_ID_PRE15)

        playGentleChime {
            // After chime completes, cancel notification after 10 minutes or keep till dismissed
            autoTimeoutJob?.cancel()
            autoTimeoutJob = serviceScope.launch {
                delay(10 * 60 * 1000L)
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(NOTIFICATION_ID_PRE15)
                stopSelf()
            }
        }
    }

    /**
     * 5-MINUTE PRE-REMINDER:
     * Heads-up audio playback of Ayatul Kursi or Durood with notification.
     */
    private fun handle5MinAudioAlert(prayerName: String, prayerArabic: String) {
        _isAlarmRinging.value = true
        _activePrayerName.value = prayerName

        val notification = buildPre5Notification(prayerName, prayerArabic)
        promoteToForeground(notification, NOTIFICATION_ID_PRE5)

        playPrePrayerAyatAudio(prayerName)

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
            foregroundStarted = true
        } catch (e: Exception) {
            Log.e(TAG, "ServiceCompat.startForeground failed", e)
            stopAzanAlarm()
            stopSelf()
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
                    createRawResourcePlayer(shortZikrRes, audioAttributes)
                } else {
                    null
                } ?: if (subhanallahRes != 0) {
                    createRawResourcePlayer(subhanallahRes, audioAttributes)
                } else null

                if (player != null) {
                    player.setOnCompletionListener { onComplete() }
                    preparePlayerAsync(player, looping = false)
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
                    setOnCompletionListener { onComplete() }
                }
                preparePlayerAsync(player, looping = false)
                return
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Fallback notification ringtone failed", ex)
        }

        onComplete()
    }

    private fun playPrePrayerAyatAudio(prayerName: String) {
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

        // 0. Check custom sound assigned for PRE_PRAYER from PrayerSoundPreferences
        try {
            val customPreUri = PrayerSoundPreferences.getSoundUriForPrayer(
                context = this,
                prayerName = prayerName,
                isPreReminder = true
            )
            if (customPreUri.isNotEmpty()) {
                val player = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    if (customPreUri.startsWith("http://") || customPreUri.startsWith("https://")) {
                        setDataSource(customPreUri)
                    } else if (customPreUri.startsWith("android.resource://") || customPreUri.startsWith("file://")) {
                        setDataSource(this@PrayerAlarmService, Uri.parse(customPreUri))
                    } else {
                        setDataSource(customPreUri)
                    }
                }
                preparePlayerAsync(player, looping = true)
                Log.d(TAG, "Playing configured pre-prayer sound: $customPreUri")
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "Custom pre-prayer sound failed, proceeding to fallback", e)
        }

        // 1. Dynamic Local / Downloaded Storage Alternative
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
                    }
                    preparePlayerAsync(player, looping = true)
                    Log.d(TAG, "MediaPlayer successfully playing dynamic audio from: ${candidate.name}")
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "Failed playing local dynamic audio ${candidate.name}: ${e.message}")
                }
            }
        }

        // 2. Primary: Bundled pre_prayer_ayat if present
        val prePrayerRes = resources.getIdentifier("pre_prayer_ayat", "raw", packageName)
        if (prePrayerRes != 0) {
            try {
                val player = createRawResourcePlayer(prePrayerRes, audioAttributes)
                if (player != null) {
                    preparePlayerAsync(player, looping = true)
                    Log.d(TAG, "MediaPlayer successfully playing pre_prayer_ayat")
                    return
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed playing pre_prayer_ayat: ${e.message}")
            }
        }

        // 3. Fallback: Bundled subhanallah_reminder if present
        val subhanallahFallbackRes = resources.getIdentifier("subhanallah_reminder", "raw", packageName)
        if (subhanallahFallbackRes != 0) {
            try {
                val player = createRawResourcePlayer(subhanallahFallbackRes, audioAttributes)
                if (player != null) {
                    preparePlayerAsync(player, looping = true)
                    Log.d(TAG, "MediaPlayer playing fallback subhanallah_reminder")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed playing fallback subhanallah_reminder", e)
            }
        }

        // 4. Remote Streaming Alternative
        try {
            val streamUrl = "https://everyayah.com/data/Alafasy_128kbps/002255.mp3"
            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(streamUrl)
            }
            preparePlayerAsync(
                player,
                looping = true,
                onPrepared = { preparedPlayer ->
                    preparedPlayer.start()
                    Log.d(TAG, "Remote dynamic stream started playing successfully")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed initializing remote streaming alternative: ${e.message}")
        }
    }

    /**
     * PLAYS USER-SELECTED CUSTOM SOUND FROM SOUND HUB (VERSION 1.1)
     * Fetches the user's selected sound using PrayerSoundPreferences.getSoundUriForPrayer()
     * and sets it as the dynamic data source.
     */
    private fun playAlarmAudio(prayerName: String, isSnooze: Boolean) {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        boostAlarmVolume()

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

        // 1. If snooze, check for downloaded Snooze Ayat MP3 first
        if (isSnooze) {
            val snoozeFile = SnoozeAudioDownloader.getLocalSnoozeAudioFile(this)
            if (snoozeFile != null && snoozeFile.exists() && snoozeFile.length() > 0) {
                try {
                    Log.d(TAG, "Playing local Snooze Ayat MP3: ${snoozeFile.absolutePath}")
                    val player = MediaPlayer().apply {
                        setAudioAttributes(audioAttributes)
                        setDataSource(snoozeFile.absolutePath)
                    }
                    preparePlayerAsync(
                        player,
                        looping = true,
                        onError = { playAlarmFallback(audioAttributes) }
                    )
                    return
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to play local snooze MP3, proceeding to custom sound", e)
                }
            }
        }

        // 2. Fetch the user's selected custom sound from Sound Hub via PrayerSoundPreferences
        try {
            val customSoundUriStr = PrayerSoundPreferences.getSoundUriForPrayer(
                context = this,
                prayerName = prayerName,
                isPreReminder = false
            )
            Log.d(TAG, "Fetched custom sound URI from PrayerSoundPreferences for $prayerName: $customSoundUriStr")

            if (customSoundUriStr.isNotEmpty()) {
                val player = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    if (customSoundUriStr.startsWith("http://") || customSoundUriStr.startsWith("https://")) {
                        setDataSource(customSoundUriStr)
                    } else if (customSoundUriStr.startsWith("android.resource://") || customSoundUriStr.startsWith("file://")) {
                        setDataSource(this@PrayerAlarmService, Uri.parse(customSoundUriStr))
                    } else {
                        setDataSource(customSoundUriStr)
                    }
                }
                preparePlayerAsync(player, looping = true, onPrepared = {
                    it.start()
                    Log.d(TAG, "Custom alarm sound started successfully on STREAM_ALARM")
                }, onError = { playAlarmFallback(audioAttributes) })
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play custom sound from PrayerSoundPreferences: ${e.message}", e)
        }

        // 3. Fallback to bundled raw Adhan resource
        val rawResName = if (prayerName.contains("Fajr", ignoreCase = true)) {
            "fajr_special"
        } else {
            "standard_adhan"
        }
        val rawResId = resources.getIdentifier(rawResName, "raw", packageName)

        if (rawResId != 0) {
            try {
                val player = createRawResourcePlayer(rawResId, audioAttributes)
                if (player != null) {
                    preparePlayerAsync(
                        player,
                        looping = true,
                        onError = { playAlarmFallback(audioAttributes) }
                    )
                    Log.d(TAG, "MediaPlayer successfully playing Adhan loop on STREAM_ALARM")
                    return
                } else {
                    Log.w(TAG, "MediaPlayer.create returned null, falling back to system ringtone")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize standard Adhan player from raw resource", e)
            }
        }

        // 4. Secondary fallback to default system alarm/ringtone
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        if (ringtoneUri != null) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(this@PrayerAlarmService, ringtoneUri)
                }
                preparePlayerAsync(player, looping = true, onError = { playGeneratedAlarmBeep() })
                Log.d(TAG, "MediaPlayer playing fallback system ringtone")
                return
            } catch (ex: Exception) {
                Log.e(TAG, "Failed fallback ringtone player", ex)
            }
        }

        playGeneratedAlarmBeep()
    }

    private fun boostAlarmVolume() {
        val manager = audioManager ?: return
        try {
            if (originalAlarmVolume == null) {
                originalAlarmVolume = manager.getStreamVolume(AudioManager.STREAM_ALARM)
            }
            val maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            manager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Could not raise alarm stream volume", e)
        }
    }

    private fun restoreAlarmVolume() {
        val volume = originalAlarmVolume ?: return
        try {
            audioManager?.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore alarm stream volume", e)
        } finally {
            originalAlarmVolume = null
        }
    }

    private fun playAlarmFallback(audioAttributes: AudioAttributes) {
        val defaultAdhanId = resources.getIdentifier("default_adhan", "raw", packageName)
        if (defaultAdhanId != 0) {
            val player = createRawResourcePlayer(defaultAdhanId, audioAttributes)
            if (player != null) {
                preparePlayerAsync(player, looping = true, onError = { playGeneratedAlarmBeep() })
                return
            }
        }

        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (ringtoneUri != null) {
            try {
                val player = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    setDataSource(this@PrayerAlarmService, ringtoneUri)
                }
                preparePlayerAsync(player, looping = true, onError = { playGeneratedAlarmBeep() })
                return
            } catch (e: Exception) {
                Log.w(TAG, "Could not prepare system alarm fallback", e)
            }
        }
        playGeneratedAlarmBeep()
    }

    private fun playGeneratedAlarmBeep() {
        stopGeneratedAlarmBeep()
        try {
            fallbackToneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100).also { generator ->
                generator.startTone(ToneGenerator.TONE_PROP_BEEP, 900)
            }
            fallbackToneJob = serviceScope.launch {
                while (true) {
                    delay(1500L)
                    fallbackToneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 900)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not start generated alarm beep", e)
            stopGeneratedAlarmBeep()
        }
    }

    private fun stopGeneratedAlarmBeep() {
        fallbackToneJob?.cancel()
        fallbackToneJob = null
        fallbackToneGenerator?.release()
        fallbackToneGenerator = null
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
                try {
                    if (isPlaying) stop()
                } catch (_: IllegalStateException) {}
                try {
                    reset()
                } catch (_: IllegalStateException) {}
                release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping media player", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun preparePlayerAsync(
        player: MediaPlayer,
        looping: Boolean,
        onPrepared: (MediaPlayer) -> Unit = { it.start() },
        onError: (() -> Unit)? = null
    ) {
        player.isLooping = looping
        mediaPlayer = player
        try {
            player.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        } catch (e: Exception) {
            Log.w(TAG, "Could not set MediaPlayer wake mode", e)
        }
        watchPlayerErrors(player, onError)
        player.setOnPreparedListener { preparedPlayer ->
            try {
                onPrepared(preparedPlayer)
            } catch (e: Exception) {
                Log.e(TAG, "Could not start prepared alarm audio", e)
                releaseFailedPlayer(preparedPlayer)
            }
        }
        try {
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Could not prepare alarm audio asynchronously", e)
            releaseFailedPlayer(player)
        }
    }

    private fun createRawResourcePlayer(
        rawResourceId: Int,
        audioAttributes: AudioAttributes
    ): MediaPlayer? {
        val descriptor = try {
            resources.openRawResourceFd(rawResourceId)
        } catch (e: Exception) {
            Log.e(TAG, "Could not open raw audio resource $rawResourceId", e)
            return null
        } ?: return null

        return try {
            MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not configure raw audio resource $rawResourceId", e)
            null
        } finally {
            try {
                descriptor.close()
            } catch (e: Exception) {
                Log.w(TAG, "Could not close raw audio descriptor", e)
            }
        }
    }

    private fun watchPlayerErrors(player: MediaPlayer, onError: (() -> Unit)? = null) {
        player.setOnErrorListener { failedPlayer, what, extra ->
            Log.e(TAG, "Alarm audio playback error: what=$what extra=$extra")
            releaseFailedPlayer(failedPlayer)
            onError?.invoke()
            true
        }
    }

    private fun releaseFailedPlayer(player: MediaPlayer) {
        try {
            player.release()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release alarm audio after playback error", e)
        }
        if (mediaPlayer === player) {
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
        stopGeneratedAlarmBeep()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }

        releaseWakeLock()
        restoreAlarmVolume()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(NOTIFICATION_ID)
        nm?.cancel(NOTIFICATION_ID_PRE5)
        nm?.cancel(NOTIFICATION_ID_PRE15)

        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopAzanAlarm()
        releaseWakeLock()
        restoreAlarmVolume()
        serviceScope.cancel()
        super.onDestroy()
    }
}
