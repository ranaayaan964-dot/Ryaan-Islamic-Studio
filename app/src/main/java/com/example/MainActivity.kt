package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SpatialAudio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerAlarmService
import com.example.data.model.LocationInfo
import com.example.data.repository.LocationRepository
import com.example.ui.screens.AiCompanionScreen
import com.example.ui.screens.HabitTrackerScreen
import com.example.ui.screens.HifzCheckerScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NextGenDashboardScreen
import com.example.ui.screens.SoundHubScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.LocalThemeIsDark
import com.example.ui.theme.MyApplicationTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

enum class AppDestination {
    SPLASH,
    LOGIN,
    MAIN_APP
}

class MainActivity : ComponentActivity() {

    private var userLocation by mutableStateOf(LocationRepository.getDefaultLocation())

    // Permission launcher for Location and Notifications
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            fetchDeviceLocation()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize prayer notification channel and arm all 5 daily alarms for background alerts
        PrayerAlarmService.createNotificationChannel(this)
        PrayerAlarmScheduler.ensureAllAlarmsScheduled(this)

        requestAppPermissions()

        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(true) }
            var currentDestination by rememberSaveable { mutableStateOf(AppDestination.SPLASH) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (currentDestination) {
                        AppDestination.SPLASH -> {
                            SplashScreen(
                                onNavigateToLogin = {
                                    currentDestination = AppDestination.LOGIN
                                }
                            )
                        }
                        AppDestination.LOGIN -> {
                            LoginScreen(
                                onLoginSuccess = {
                                    currentDestination = AppDestination.MAIN_APP
                                }
                            )
                        }
                        AppDestination.MAIN_APP -> {
                            MainAppContainer(
                                currentLocation = userLocation,
                                onLocationChange = { userLocation = it },
                                isDarkTheme = isDarkTheme,
                                onToggleTheme = { isDarkTheme = !isDarkTheme },
                                onLogout = {
                                    currentDestination = AppDestination.LOGIN
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun requestAppPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            fetchDeviceLocation()
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun fetchDeviceLocation() {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            // Actively ping GPS hardware to fetch exact current location (avoids null lastLocation)
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        userLocation = LocationInfo(
                            cityName = "Current Location",
                            countryName = "GPS",
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    } else {
                        // Fallback to lastLocation if cached
                        fusedClient.lastLocation.addOnSuccessListener { cachedLoc: Location? ->
                            if (cachedLoc != null) {
                                userLocation = LocationInfo(
                                    cityName = "Current Location",
                                    countryName = "GPS",
                                    latitude = cachedLoc.latitude,
                                    longitude = cachedLoc.longitude
                                )
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    // Fallback to lastLocation on failure
                    try {
                        fusedClient.lastLocation.addOnSuccessListener { cachedLoc: Location? ->
                            if (cachedLoc != null) {
                                userLocation = LocationInfo(
                                    cityName = "Current Location",
                                    countryName = "GPS",
                                    latitude = cachedLoc.latitude,
                                    longitude = cachedLoc.longitude
                                )
                            }
                        }
                    } catch (_: SecurityException) {}
                }
        } catch (_: SecurityException) {}
    }
}

@Composable
fun MainAppContainer(
    currentLocation: LocationInfo,
    onLocationChange: (LocationInfo) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isDark = LocalThemeIsDark.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            // Glassmorphic floating bottom navigation bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val navBg = if (isDark) Color(0xF00A142E) else Color(0xF4FFFFFF)
                val navBorder = if (isDark) Color(0x33E5C07B) else Color(0x28065F46)

                NavigationBar(
                    modifier = Modifier
                        .clip(RoundedCornerShape(26.dp))
                        .border(1.dp, navBorder, RoundedCornerShape(26.dp)),
                    containerColor = navBg,
                    tonalElevation = 0.dp
                ) {
                    // Tab 0: Sahiwal Namaz & Alarms
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                                contentDescription = "Namaz",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text("Namaz", fontSize = 9.5.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isDark) Color(0xFF1B1302) else Color.White,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Tab 1: 20+ Islamic Sound & Ringtone Hub
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.SpatialAudio else Icons.Outlined.SpatialAudio,
                                contentDescription = "Audio Hub",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text("Sounds", fontSize = 9.5.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isDark) Color(0xFF1B1302) else Color.White,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Tab 2: AI Companion
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.AutoAwesome else Icons.Outlined.AutoAwesome,
                                contentDescription = "AI Scholar",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text("Scholar", fontSize = 9.5.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isDark) Color(0xFF1B1302) else Color.White,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Tab 3: Hifz Recitation AI Checker
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 3) Icons.Filled.RecordVoiceOver else Icons.Outlined.RecordVoiceOver,
                                contentDescription = "Hifz AI",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text("AI Hifz", fontSize = 9.5.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isDark) Color(0xFF1B1302) else Color.White,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Tab 4: Gamified Habit Tracker
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 4) Icons.Filled.EmojiEvents else Icons.Outlined.EmojiEvents,
                                contentDescription = "Habits",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text("Habits", fontSize = 9.5.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isDark) Color(0xFF1B1302) else Color.White,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 0.dp)
        ) {
            when (selectedTab) {
                0 -> NextGenDashboardScreen(
                    onLogout = onLogout,
                    onNavigateToSoundHub = { selectedTab = 1 }
                )
                1 -> SoundHubScreen(onNavigateBack = { selectedTab = 0 })
                2 -> AiCompanionScreen()
                3 -> HifzCheckerScreen()
                4 -> HabitTrackerScreen()
            }
        }
    }
}
