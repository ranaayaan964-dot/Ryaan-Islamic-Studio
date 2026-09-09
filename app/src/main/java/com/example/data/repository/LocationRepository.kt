package com.example.data.repository

import com.example.data.model.LocationInfo

object LocationRepository {

    val popularCities = listOf(
        LocationInfo("Makkah", "Saudi Arabia", 21.4225, 39.8262),
        LocationInfo("Madinah", "Saudi Arabia", 24.5247, 39.5692),
        LocationInfo("Cairo", "Egypt", 30.0444, 31.2357),
        LocationInfo("Istanbul", "Turkey", 41.0082, 28.9784),
        LocationInfo("Dubai", "United Arab Emirates", 25.2048, 55.2708),
        LocationInfo("London", "United Kingdom", 51.5074, -0.1278),
        LocationInfo("New York", "United States", 40.7128, -74.0060),
        LocationInfo("Karachi", "Pakistan", 24.8607, 67.0011),
        LocationInfo("Lahore", "Pakistan", 31.5204, 74.3587),
        LocationInfo("Dhaka", "Bangladesh", 23.8103, 90.4125),
        LocationInfo("Jakarta", "Indonesia", -6.2088, 106.8456),
        LocationInfo("Kuala Lumpur", "Malaysia", 3.1390, 101.6869),
        LocationInfo("Toronto", "Canada", 43.6532, -79.3832),
        LocationInfo("Sydney", "Australia", -33.8688, 151.2093)
    )

    fun getDefaultLocation(): LocationInfo = popularCities[0] // Makkah
}
