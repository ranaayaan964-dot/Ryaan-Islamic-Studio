package com.example.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object QiblaCalculator {

    // Kaaba coordinates in Makkah
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    /**
     * Calculates the Qibla bearing from the given location towards the Kaaba in degrees (0..360).
     */
    fun calculateQiblaBearing(userLat: Double, userLng: Double): Float {
        val phi1 = Math.toRadians(userLat)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - userLng)

        val y = sin(deltaLambda)
        val x = cos(phi1) * kotlin.math.tan(phi2) - sin(phi1) * cos(deltaLambda)

        var qibla = Math.toDegrees(atan2(y, x)).toFloat()
        qibla = (qibla + 360f) % 360f
        return qibla
    }

    /**
     * Calculates distance to Kaaba in kilometers using the Haversine formula.
     */
    fun calculateDistanceToKaaba(userLat: Double, userLng: Double): Int {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(KAABA_LATITUDE - userLat)
        val dLon = Math.toRadians(KAABA_LONGITUDE - userLng)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(userLat)) * cos(Math.toRadians(KAABA_LATITUDE)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (earthRadiusKm * c).toInt()
    }
}
