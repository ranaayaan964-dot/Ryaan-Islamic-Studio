package com.example.service.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.LocationInfo
import com.example.util.QiblaCalculator
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val Context.locationCacheDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ryaan_location_cache_preferences"
)

/**
 * Immutable snapshot of coordinates cached in Jetpack DataStore.
 */
data class CachedLocationData(
    val latitude: Double = QiblaCalculator.KAABA_LATITUDE,
    val longitude: Double = QiblaCalculator.KAABA_LONGITUDE,
    val altitude: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val timestamp: Long = 0L,
    val cityName: String = "Makkah",
    val countryName: String = "Saudi Arabia",
    val qiblaBearing: Float = 0f,
    val distanceToKaabaKm: Int = 0,
    val isHighAccuracy: Boolean = false
) {
    /**
     * Determines whether the cached location is fresh within [maxAgeMs].
     * Default freshness ceiling is 15 minutes.
     */
    fun isFresh(maxAgeMs: Long = LocationServicesWrapper.POLLING_INTERVAL_MS): Boolean {
        if (timestamp <= 0L) return false
        val age = System.currentTimeMillis() - timestamp
        return age in 0..maxAgeMs
    }

    /**
     * Maps the cached coordinates directly to [LocationInfo] for UI consumers.
     */
    fun toLocationInfo(): LocationInfo = LocationInfo(
        cityName = cityName,
        countryName = countryName,
        latitude = latitude,
        longitude = longitude
    )
}

/**
 * Robust Location Services Wrapper
 *
 * Implements:
 * 1. Google Play Services FusedLocationProviderClient integration.
 * 2. 15-Minute Polling Interval with balanced power accuracy and displacement gating (150m)
 *    to preserve battery longevity while the device is stationary.
 * 3. Jetpack DataStore coordinate caching with immediate cold-start availability.
 * 4. High-Accuracy Qibla Mode: On-demand satellite lock for sub-degree Qibla bearing calculations,
 *    auto-reverting to low-power polling once resolved.
 * 5. Defensive permission and sensor fallbacks (Makkah/Sahiwal).
 */
class LocationServicesWrapper private constructor(private val context: Context) {

    companion object {
        private const val TAG = "LocationServicesWrapper"

        // 15-minute polling interval to strictly minimize battery drain
        const val POLLING_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes = 900,000 ms
        const val FASTEST_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        const val MIN_DISPLACEMENT_METERS = 150.0f // 150 meters displacement threshold
        const val MAX_UPDATE_DELAY_MS = 15 * 60 * 1000L // Batched delivery window

        // High-accuracy timeout boundary for Qibla calculation
        const val HIGH_ACCURACY_TIMEOUT_MS = 15_000L

        // DataStore Keys
        val KEY_LATITUDE = doublePreferencesKey("loc_latitude")
        val KEY_LONGITUDE = doublePreferencesKey("loc_longitude")
        val KEY_ALTITUDE = doublePreferencesKey("loc_altitude")
        val KEY_ACCURACY = floatPreferencesKey("loc_accuracy")
        val KEY_TIMESTAMP = longPreferencesKey("loc_timestamp")
        val KEY_CITY_NAME = stringPreferencesKey("loc_city_name")
        val KEY_COUNTRY_NAME = stringPreferencesKey("loc_country_name")
        val KEY_QIBLA_BEARING = floatPreferencesKey("loc_qibla_bearing")
        val KEY_DISTANCE_TO_KAABA_KM = intPreferencesKey("loc_distance_to_kaaba_km")
        val KEY_IS_HIGH_ACCURACY = booleanPreferencesKey("loc_is_high_accuracy")

        @Volatile
        private var INSTANCE: LocationServicesWrapper? = null

        fun getInstance(context: Context): LocationServicesWrapper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationServicesWrapper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val dataStore = context.locationCacheDataStore
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isPollingActive = AtomicBoolean(false)

    private val _currentLocationState = MutableStateFlow(CachedLocationData())
    val currentLocationState: StateFlow<CachedLocationData> = _currentLocationState.asStateFlow()

    private var locationCallback: LocationCallback? = null

    init {
        // Cold-start warm up: observe DataStore and prime the in-memory StateFlow
        coroutineScope.launch {
            cachedLocationFlow.collect { cached ->
                _currentLocationState.value = cached
            }
        }
    }

    /**
     * Reactive stream of cached coordinates persisted in Jetpack DataStore.
     * Guaranteed zero-battery consumption for passive collectors.
     */
    val cachedLocationFlow: Flow<CachedLocationData> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading location DataStore: ${exception.message}", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val lat = preferences[KEY_LATITUDE] ?: QiblaCalculator.KAABA_LATITUDE
            val lng = preferences[KEY_LONGITUDE] ?: QiblaCalculator.KAABA_LONGITUDE
            val alt = preferences[KEY_ALTITUDE] ?: 0.0
            val acc = preferences[KEY_ACCURACY] ?: 0f
            val time = preferences[KEY_TIMESTAMP] ?: 0L
            val city = preferences[KEY_CITY_NAME] ?: "Makkah"
            val country = preferences[KEY_COUNTRY_NAME] ?: "Saudi Arabia"
            val qibla = preferences[KEY_QIBLA_BEARING] ?: QiblaCalculator.calculateQiblaBearing(lat, lng)
            val distance = preferences[KEY_DISTANCE_TO_KAABA_KM] ?: QiblaCalculator.calculateDistanceToKaaba(lat, lng)
            val isHighAcc = preferences[KEY_IS_HIGH_ACCURACY] ?: false

            CachedLocationData(
                latitude = lat,
                longitude = lng,
                altitude = alt,
                accuracyMeters = acc,
                timestamp = time,
                cityName = city,
                countryName = country,
                qiblaBearing = qibla,
                distanceToKaabaKm = distance,
                isHighAccuracy = isHighAcc
            )
        }

    /**
     * Checks if either FINE or COARSE location permission is granted.
     */
    fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    /**
     * Checks if hardware location services (GPS or Network provider) are enabled.
     */
    fun isLocationHardwareEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        val gpsEnabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
        val networkEnabled = try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
        return gpsEnabled || networkEnabled
    }

    /**
     * Starts battery-optimized 15-minute periodic location polling.
     * Uses PRIORITY_BALANCED_POWER_ACCURACY with 150m minimum displacement
     * so that stationary devices never activate satellite receivers unnecessarily.
     */
    fun startPeriodicPolling() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Cannot start periodic location polling: Location permission not granted")
            return
        }

        if (isPollingActive.getAndSet(true)) {
            Log.d(TAG, "Periodic location polling is already active.")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            POLLING_INTERVAL_MS
        )
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_DISPLACEMENT_METERS)
            .setMaxUpdateDelayMillis(MAX_UPDATE_DELAY_MS)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                Log.d(TAG, "Periodic 15-minute location fix received: (${location.latitude}, ${location.longitude}) ±${location.accuracy}m")
                coroutineScope.launch {
                    persistLocationToDataStore(location, isHighAccuracy = false)
                }
            }
        }

        this.locationCallback = callback

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
            )
            Log.i(TAG, "Started 15-minute balanced location polling successfully.")
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException initiating location updates: ${se.message}", se)
            isPollingActive.set(false)
        } catch (e: Exception) {
            Log.e(TAG, "Failed initiating location updates: ${e.message}", e)
            isPollingActive.set(false)
        }
    }

    /**
     * Stops the 15-minute background location polling.
     */
    fun stopPeriodicPolling() {
        if (!isPollingActive.getAndSet(false)) return
        locationCallback?.let { callback ->
            try {
                fusedLocationClient.removeLocationUpdates(callback)
                Log.i(TAG, "Stopped periodic location updates successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing location updates: ${e.message}", e)
            } finally {
                locationCallback = null
            }
        }
    }

    /**
     * High-Accuracy Qibla Mode
     *
     * Specifically engineered for Qibla direction calculation:
     * 1. Evaluates existing DataStore cache: If cached coordinates are fresh (<15 mins) and
     *    accuracy is acceptable (<= 30 meters), returns the cached result immediately
     *    with ZERO battery impact.
     * 2. If stale or forced, initiates a single-shot PRIORITY_HIGH_ACCURACY request with
     *    a strict 15-second timeout boundary.
     * 3. Upon receiving coordinates, calculates exact spherical Qibla bearing, Kaaba distance,
     *    and geocoded metadata, then persists directly to DataStore.
     * 4. Shuts down GPS immediately to prevent battery depletion.
     */
    suspend fun requestHighAccuracyQiblaLocation(
        forceFreshGps: Boolean = false,
        timeoutMs: Long = HIGH_ACCURACY_TIMEOUT_MS
    ): Result<CachedLocationData> = withContext(Dispatchers.IO) {
        val currentCached = getCachedLocation()

        // Battery optimization: Return fresh cached location if already accurate
        if (!forceFreshGps && currentCached.isFresh() && currentCached.accuracyMeters in 0.1f..30.0f) {
            Log.d(TAG, "Using fresh cached location for Qibla calculation (${currentCached.accuracyMeters}m accuracy). GPS skipped.")
            return@withContext Result.success(currentCached)
        }

        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission absent. Falling back to cached DataStore location.")
            return@withContext Result.success(currentCached)
        }

        val cts = CancellationTokenSource()

        val freshLocation: Location? = try {
            withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    try {
                        fusedLocationClient.getCurrentLocation(
                            Priority.PRIORITY_HIGH_ACCURACY,
                            cts.token
                        ).addOnSuccessListener { loc ->
                            if (continuation.isActive) continuation.resume(loc)
                        }.addOnFailureListener { ex ->
                            if (continuation.isActive) continuation.resumeWithException(ex)
                        }.addOnCanceledListener {
                            if (continuation.isActive) continuation.cancel()
                        }
                    } catch (se: SecurityException) {
                        if (continuation.isActive) continuation.resumeWithException(se)
                    }

                    continuation.invokeOnCancellation {
                        cts.cancel()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "High-accuracy location request failed: ${e.message}")
            null
        }

        if (freshLocation != null) {
            Log.i(TAG, "High-accuracy Qibla fix acquired: (${freshLocation.latitude}, ${freshLocation.longitude}) ±${freshLocation.accuracy}m")
            val saved = persistLocationToDataStore(freshLocation, isHighAccuracy = true)
            return@withContext Result.success(saved)
        }

        // Fallback: Last known location
        val lastLocation = try {
            suspendCancellableCoroutine<Location?> { continuation ->
                try {
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { loc -> if (continuation.isActive) continuation.resume(loc) }
                        .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
                        .addOnCanceledListener { if (continuation.isActive) continuation.resume(null) }
                } catch (se: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        } catch (_: Exception) {
            null
        }

        if (lastLocation != null) {
            Log.d(TAG, "Using last known location fallback: (${lastLocation.latitude}, ${lastLocation.longitude})")
            val saved = persistLocationToDataStore(lastLocation, isHighAccuracy = false)
            return@withContext Result.success(saved)
        }

        // Final fallback: Cached DataStore coordinates
        Result.success(currentCached)
    }

    /**
     * Suspended snapshot of current coordinates in DataStore.
     */
    suspend fun getCachedLocation(): CachedLocationData {
        return cachedLocationFlow.first()
    }

    /**
     * Persists location coordinates, calculates Qibla bearing, reverses geocode, and commits to DataStore.
     */
    private suspend fun persistLocationToDataStore(
        location: Location,
        isHighAccuracy: Boolean
    ): CachedLocationData = withContext(Dispatchers.IO) {
        val lat = location.latitude
        val lng = location.longitude
        val alt = if (location.hasAltitude()) location.altitude else 0.0
        val acc = if (location.hasAccuracy()) location.accuracy else 0f
        val timestamp = System.currentTimeMillis()

        val qiblaBearing = QiblaCalculator.calculateQiblaBearing(lat, lng)
        val distanceKm = QiblaCalculator.calculateDistanceToKaaba(lat, lng)

        val (cityName, countryName) = resolveGeocodedAddress(lat, lng)

        dataStore.edit { preferences ->
            preferences[KEY_LATITUDE] = lat
            preferences[KEY_LONGITUDE] = lng
            preferences[KEY_ALTITUDE] = alt
            preferences[KEY_ACCURACY] = acc
            preferences[KEY_TIMESTAMP] = timestamp
            preferences[KEY_CITY_NAME] = cityName
            preferences[KEY_COUNTRY_NAME] = countryName
            preferences[KEY_QIBLA_BEARING] = qiblaBearing
            preferences[KEY_DISTANCE_TO_KAABA_KM] = distanceKm
            preferences[KEY_IS_HIGH_ACCURACY] = isHighAccuracy
        }

        val resultData = CachedLocationData(
            latitude = lat,
            longitude = lng,
            altitude = alt,
            accuracyMeters = acc,
            timestamp = timestamp,
            cityName = cityName,
            countryName = countryName,
            qiblaBearing = qiblaBearing,
            distanceToKaabaKm = distanceKm,
            isHighAccuracy = isHighAccuracy
        )

        _currentLocationState.value = resultData
        resultData
    }

    /**
     * Resolves the city and country name using Geocoder with fallback handling.
     */
    private fun resolveGeocodedAddress(latitude: Double, longitude: Double): Pair<String, String> {
        return try {
            if (!Geocoder.isPresent()) {
                return Pair("Current Location", "GPS")
            }
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            val address = addresses?.firstOrNull()
            if (address != null) {
                val city = address.locality
                    ?: address.subAdminArea
                    ?: address.adminArea
                    ?: "Current Location"
                val country = address.countryName ?: "GPS"
                Pair(city, country)
            } else {
                Pair("Current Location", "GPS")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Geocoder lookup failed: ${e.message}")
            Pair("Current Location", "GPS")
        }
    }
}
