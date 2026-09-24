package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MemberLocation(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * MODULE 16: LOCATION FOREGROUND SERVICE (ANDROID 14 LOCATION READY)
 */
class LocationForegroundService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    companion object {
        private const val TAG = "LocationForegroundService"
        const val CHANNEL_ID = "ryaan_hajj_tracker_channel"
        const val NOTIFICATION_ID = 8891

        private val _currentLocationFlow = MutableStateFlow<Location?>(null)
        val currentLocationFlow = _currentLocationFlow.asStateFlow()

        private val _familyLocationsFlow = MutableStateFlow<List<MemberLocation>>(emptyList())
        val familyLocationsFlow = _familyLocationsFlow.asStateFlow()

        fun startService(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LocationForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                _currentLocationFlow.value = loc
                Log.d(TAG, "Location updated: ${loc.latitude}, ${loc.longitude}")

                // Update simulated family members in proximity (Makkah/Mina/Arafat)
                _familyLocationsFlow.value = listOf(
                    MemberLocation("me", "You (Leader)", loc.latitude, loc.longitude),
                    MemberLocation("fam_1", "Father (Ahmad)", loc.latitude + 0.0012, loc.longitude - 0.0008),
                    MemberLocation("fam_2", "Mother (Amina)", loc.latitude + 0.0005, loc.longitude + 0.0011),
                    MemberLocation("fam_3", "Sister (Mariam)", loc.latitude - 0.0009, loc.longitude + 0.0006)
                )
            }
        }

        startLocationTracking()
    }

    private fun startLocationTracking() {
        val notification = buildForegroundNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
            .setMinUpdateIntervalMillis(5000L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission missing for foreground service", e)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Hajj & Umrah 3D Family Radar Active")
            .setContentText("Broadcasting live secure location to your pilgrim family group every 10s")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hajj & Umrah Family Radar",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Runs background GPS tracking for family coordination in Makkah & Madinah"
            }
            nm.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
