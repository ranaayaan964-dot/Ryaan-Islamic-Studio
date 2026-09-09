package com.example.ar

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class NearbyMasjid(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int,
    val bearingDegrees: Float,
    val address: String
)

data class ArOrientationState(
    val azimuthDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val qiblaBearingDegrees: Float = 262.5f,
    val deltaToQiblaDegrees: Float = 0f,
    val isAlignedWithQibla: Boolean = false,
    val distanceToKaabaKm: Int = 3624,
    val nearbyMasjids: List<NearbyMasjid> = emptyList()
)

/**
 * ArQiblaEngine
 *
 * Fuses hardware rotation sensors with spherical trigonometry
 * to provide real-time AR orientation and 3D Mosque projections.
 */
class ArQiblaEngine(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _orientationState = MutableStateFlow(ArOrientationState())
    val orientationState: StateFlow<ArOrientationState> = _orientationState.asStateFlow()

    // Kaaba Coordinates (Makkah Al-Mukarramah)
    companion object {
        const val KAABA_LATITUDE = 21.422487
        const val KAABA_LONGITUDE = 39.826206

        // Default: Sahiwal, Punjab, Pakistan
        const val DEFAULT_USER_LAT = 30.6682
        const val DEFAULT_USER_LNG = 73.1114
    }

    private var currentLat = DEFAULT_USER_LAT
    private var currentLng = DEFAULT_USER_LNG
    private var qiblaBearing = 262.5f

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val lastAccelerometer = FloatArray(3)
    private val lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    init {
        updateLocation(DEFAULT_USER_LAT, DEFAULT_USER_LNG)
    }

    fun updateLocation(lat: Double, lng: Double) {
        currentLat = lat
        currentLng = lng
        qiblaBearing = calculateQiblaBearing(lat, lng)

        val masjids = generateNearbyMasjids(lat, lng)
        _orientationState.value = _orientationState.value.copy(
            qiblaBearingDegrees = qiblaBearing,
            distanceToKaabaKm = calculateDistanceKm(lat, lng, KAABA_LATITUDE, KAABA_LONGITUDE),
            nearbyMasjids = masjids
        )
    }

    fun start() {
        if (rotationSensor != null) {
            sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            sensorManager?.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            processOrientation(rotationMatrix)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
            lastAccelerometerSet = true
            tryComputeMatrix()
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
            lastMagnetometerSet = true
            tryComputeMatrix()
        }
    }

    private fun tryComputeMatrix() {
        if (lastAccelerometerSet && lastMagnetometerSet) {
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                lastAccelerometer,
                lastMagnetometer
            )
            if (success) {
                processOrientation(rotationMatrix)
            }
        }
    }

    private fun processOrientation(rMatrix: FloatArray) {
        // Remap coordinates for camera AR landscape/portrait
        val outR = FloatArray(9)
        SensorManager.remapCoordinateSystem(
            rMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            outR
        )
        SensorManager.getOrientation(outR, orientationAngles)

        val azimuthRad = orientationAngles[0]
        var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
        if (azimuthDeg < 0) azimuthDeg += 360f

        val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        var delta = (qiblaBearing - azimuthDeg)
        while (delta > 180f) delta -= 360f
        while (delta < -180f) delta += 360f

        val isAligned = kotlin.math.abs(delta) <= 4.0f

        _orientationState.value = _orientationState.value.copy(
            azimuthDegrees = azimuthDeg,
            pitchDegrees = pitchDeg,
            rollDegrees = rollDeg,
            deltaToQiblaDegrees = delta,
            isAlignedWithQibla = isAligned
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun calculateQiblaBearing(lat: Double, lng: Double): Float {
        val userLatRad = Math.toRadians(lat)
        val userLngRad = Math.toRadians(lng)
        val kaabaLatRad = Math.toRadians(KAABA_LATITUDE)
        val kaabaLngRad = Math.toRadians(KAABA_LONGITUDE)

        val deltaLng = kaabaLngRad - userLngRad
        val y = sin(deltaLng) * cos(kaabaLatRad)
        val x = cos(userLatRad) * sin(kaabaLatRad) - sin(userLatRad) * cos(kaabaLatRad) * cos(deltaLng)

        var bearing = Math.toDegrees(atan2(y, x)).toFloat()
        if (bearing < 0) bearing += 360f
        return bearing
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val r = 6371 // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (r * c).toInt()
    }

    private fun generateNearbyMasjids(userLat: Double, userLng: Double): List<NearbyMasjid> {
        val sampleMasjids = listOf(
            Triple("Central Jamia Masjid Sahiwal", 30.6695, 73.1130),
            Triple("Masjid Bilal & Islamic Center", 30.6660, 73.1080),
            Triple("Masjid Al-Noor High Street", 30.6720, 73.1150),
            Triple("Masjid Gulberg Model Town", 30.6640, 73.1180),
            Triple("Jamia Masjid Farooq-e-Azam", 30.6710, 73.1090)
        )

        return sampleMasjids.mapIndexed { idx, (name, mLat, mLng) ->
            val dist = (calculateDistanceKm(userLat, userLng, mLat, mLng) * 1000).coerceAtLeast(250 + (idx * 180))
            val bearing = calculateQiblaBearing(userLat, userLng) + ((idx - 2) * 18f)
            NearbyMasjid(
                id = "masjid_$idx",
                name = name,
                latitude = mLat,
                longitude = mLng,
                distanceMeters = dist,
                bearingDegrees = (bearing % 360f + 360f) % 360f,
                address = "Near Main Boulevard, Sahiwal"
            )
        }
    }
}
