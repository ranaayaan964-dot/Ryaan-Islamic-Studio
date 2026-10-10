package com.example.service

import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.example.data.model.PrayerTimeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class AutoSilentState(
    val isEnabled: Boolean = true,
    val isCurrentlySilent: Boolean = false,
    val reason: String = "Monitoring Active",
    val remainingMinutes: Int = 0,
    val isMosqueGeofenceTriggered: Boolean = false,
    val nearbyMosqueName: String = "Jamia Masjid Central Sahiwal",
    val distanceToMosqueMeters: Int = 120
)

object AutoSilentManager {

    private const val TAG = "AutoSilentManager"
    private const val JAMAAT_SILENT_DURATION_MILLIS = 20 * 60 * 1000L // 20 minutes
    private const val MOSQUE_GEOFENCE_RADIUS_METERS = 200.0 // 200 meters

    // Well-known coordinates for Central Mosque Sahiwal
    private const val CENTRAL_MOSQUE_LAT = 30.6698
    private const val CENTRAL_MOSQUE_LNG = 73.1070

    private val _silentState = MutableStateFlow(AutoSilentState())
    val silentState = _silentState.asStateFlow()

    private var previousRingerMode: Int? = null

    fun hasNotificationPolicyAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.isNotificationPolicyAccessGranted == true
        } else {
            true
        }
    }

    fun requestNotificationPolicyAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch DND settings: ${e.message}")
            }
        }
    }

    fun toggleEnabled(enabled: Boolean, context: Context) {
        if (enabled && !hasNotificationPolicyAccess(context)) {
            requestNotificationPolicyAccess(context)
            _silentState.value = _silentState.value.copy(
                isEnabled = false,
                isCurrentlySilent = false,
                reason = "DND Permission Required"
            )
            return
        }
        _silentState.value = _silentState.value.copy(isEnabled = enabled)
        if (!enabled && _silentState.value.isCurrentlySilent) {
            restoreSound(context)
        }
    }

    fun evaluateSilentCondition(
        context: Context,
        prayers: List<PrayerTimeItem>,
        userLat: Double = 30.6699, // default near Central Mosque
        userLng: Double = 73.1071
    ) {
        if (!_silentState.value.isEnabled) return

        if (!hasNotificationPolicyAccess(context)) {
            if (_silentState.value.isEnabled || _silentState.value.isCurrentlySilent) {
                _silentState.value = _silentState.value.copy(
                    isEnabled = false,
                    isCurrentlySilent = false,
                    reason = "DND Permission Required"
                )
            }
            return
        }

        val now = System.currentTimeMillis()
        var prayerSilentActive = false
        var activePrayerName = ""
        var remainingMins = 0

        // 1. Check if now is within [prayer.timeMillis, prayer.timeMillis + 20 mins]
        for (prayer in prayers) {
            val prayerStart = prayer.timeMillis
            val prayerEnd = prayerStart + JAMAAT_SILENT_DURATION_MILLIS

            if (now in prayerStart..prayerEnd) {
                prayerSilentActive = true
                activePrayerName = prayer.type.displayName
                remainingMins = (((prayerEnd - now) / 1000) / 60).toInt() + 1
                break
            }
        }

        // 2. Check Mosque Geofence proximity (simulated or real distance)
        val distanceToMosque = calculateDistanceMeters(
            userLat, userLng,
            CENTRAL_MOSQUE_LAT, CENTRAL_MOSQUE_LNG
        )
        val isInsideMosque = distanceToMosque <= MOSQUE_GEOFENCE_RADIUS_METERS

        val shouldBeSilent = prayerSilentActive || isInsideMosque
        val reason = when {
            prayerSilentActive -> "$activePrayerName Jamaat in progress ($remainingMins mins left)"
            isInsideMosque -> "Inside Mosque Geofence (${distanceToMosque.toInt()}m)"
            else -> "Standby (Auto-activates during Namaz / Mosque entry)"
        }

        if (shouldBeSilent && !_silentState.value.isCurrentlySilent) {
            applySilentMode(context)
        } else if (!shouldBeSilent && _silentState.value.isCurrentlySilent) {
            restoreSound(context)
        }

        _silentState.value = _silentState.value.copy(
            isCurrentlySilent = shouldBeSilent,
            reason = reason,
            remainingMinutes = remainingMins,
            isMosqueGeofenceTriggered = isInsideMosque,
            distanceToMosqueMeters = distanceToMosque.toInt()
        )
    }

    private fun applySilentMode(context: Context) {
        if (!hasNotificationPolicyAccess(context)) {
            Log.w(TAG, "Cannot mute phone: Do Not Disturb permission not granted")
            return
        }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (previousRingerMode == null) {
                previousRingerMode = audioManager.ringerMode
            }
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            Log.d(TAG, "Phone muted for Namaz Jamaat / Mosque geofence")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing Notification Policy Access for ringer mode: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to switch ringer mode: ${e.message}")
        }
    }

    private fun restoreSound(context: Context) {
        if (!hasNotificationPolicyAccess(context)) {
            Log.w(TAG, "Cannot restore sound: Do Not Disturb permission not granted")
            return
        }
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            audioManager.ringerMode = previousRingerMode ?: AudioManager.RINGER_MODE_NORMAL
            previousRingerMode = null
            Log.d(TAG, "Phone sound restored after Namaz")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing Notification Policy Access to restore ringer: ${e.message}")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to restore sound: ${e.message}")
        }
    }

    private fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
