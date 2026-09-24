package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alarm.NamazAlarmWorker
import com.example.alarm.NamazWorkManagerScheduler
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.PrayerAlarmService
import com.example.data.database.AppDatabase
import com.example.data.model.LocationInfo
import com.example.data.repository.LocationRepository
import com.example.service.location.LocationServicesWrapper
import com.example.ui.components.UpdateDialog
import com.example.ui.screens.AiCompanionScreen
import com.example.ui.screens.ArabicScannerScreen
import com.example.ui.screens.GlobalDuaWallScreen
import com.example.ui.screens.HabitTrackerScreen
import com.example.ui.screens.HajjFamilyTrackerScreen
import com.example.ui.screens.HalalFinanceScreen
import com.example.ui.screens.HifzCheckerScreen
import com.example.ui.screens.KidsLearningScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NextGenDashboardScreen
import com.example.ui.screens.PrayerTimesScreen
import com.example.ui.screens.QuranTafsirScreen
import com.example.ui.screens.SmartZakatScreen
import com.example.ui.screens.SoundHubScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.screens.FamilyTrackerScreen
import com.example.ui.screens.GlobalDuaScreen
import com.example.ui.screens.HalalScannerScreen
import com.example.ui.screens.HifzRecordingScreen
import com.example.ui.screens.QuranReaderScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ZakatCalculatorScreen
import com.example.security.SessionManager
import com.example.util.BatteryOptimizationHelper
import com.example.util.UpdateViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AppDestination {
    SPLASH,
    LOGIN,
    MAIN_APP
}

enum class EnterpriseFeatureScreen {
    NONE,
    SETTINGS,
    PRAYER_TIMES,
    QURAN_TAFSIR,
    TABEER_DREAM,
    ARABIC_SCANNER,
    GLOBAL_DUA_WALL,
    HAJJ_TRACKER,
    HALAL_FINANCE,
    KIDS_LEARNING,
    SMART_ZAKAT,
    HALAL_SCANNER,
    ZAKAT_CALCULATOR,
    GLOBAL_DUA,
    QURAN_READER,
    FAMILY_TRACKER,
    HIFZ_RECORDING
}

class MainActivity : ComponentActivity() {

    private var userLocation by mutableStateOf(LocationRepository.getDefaultLocation())

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
        // MODULE 4: ZERO-FLICKER WINDOW TRANSITION
        // Splash and themes bleed seamlessly behind the Android status bar and navigation bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // MODULE 17: Request battery optimization whitelist & OEM background persistence
        BatteryOptimizationHelper.requestIgnoreBatteryOptimization(this)

        PrayerAlarmService.createNotificationChannel(this)
        PrayerAlarmScheduler.ensureAllAlarmsScheduled(this)

        // WorkManager Background Alarm Service for Daily Namaz Times from App Metadata
        NamazAlarmWorker.createNamazNotificationChannel(this)
        NamazWorkManagerScheduler.scheduleAllNamazFromMetadata(this)
        NamazWorkManagerScheduler.schedulePeriodicDailySync(this)

        // Observe cached coordinates from DataStore to minimize battery impact and provide instant location
        lifecycleScope.launch {
            LocationServicesWrapper.getInstance(this@MainActivity)
                .cachedLocationFlow
                .collect { cached ->
                    userLocation = cached.toLocationInfo()
                }
        }

        requestAppPermissions()

        setContent {
            var currentDestination by rememberSaveable { mutableStateOf(AppDestination.SPLASH) }
            val updateViewModel: UpdateViewModel = viewModel()
            val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
            val themeRepo = remember { com.example.data.repository.ThemeSettingsRepository.getInstance(this@MainActivity) }
            val currentTheme by themeRepo.currentThemeFlow.collectAsStateWithLifecycle(initialValue = com.example.ui.theme.ThemeType.PEARL_WHITE)

            // Dynamically adjust system bar icon colors according to active theme & screen
            val insetsController = remember(window) { WindowInsetsControllerCompat(window, window.decorView) }
            androidx.compose.runtime.LaunchedEffect(currentDestination, currentTheme) {
                if (currentDestination == AppDestination.SPLASH) {
                    // Splash screen uses deep obsidian radial gradient - light icons
                    insetsController.isAppearanceLightStatusBars = false
                    insetsController.isAppearanceLightNavigationBars = false
                } else {
                    // Dark icons for Pearl White (#FFFFFF), light icons for Midnight Emerald & Velvet Sapphire
                    insetsController.isAppearanceLightStatusBars = !currentTheme.isDark
                    insetsController.isAppearanceLightNavigationBars = !currentTheme.isDark
                }
            }

            MyApplicationTheme(theme = currentTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (currentDestination) {
                        AppDestination.SPLASH -> {
                            SplashScreen(
                                onNavigateNext = { isLoggedIn ->
                                    currentDestination = if (isLoggedIn) AppDestination.MAIN_APP else AppDestination.LOGIN
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
                                onLogout = {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        SessionManager.getInstance(this@MainActivity).logout()
                                        AppDatabase.getInstance(this@MainActivity).userDao().logout()
                                        com.example.data.repository.SettingsRepository.getInstance(this@MainActivity).setLoggedIn(false)
                                    }
                                    currentDestination = AppDestination.LOGIN
                                }
                            )
                        }
                    }

                    // Un-cancellable In-App GitHub Update Dialog
                    if (updateState.showUpdateDialog && updateState.newVersionName.isNotBlank()) {
                        UpdateDialog(
                            newVersionName = updateState.newVersionName,
                            downloadUrl = updateState.downloadUrl
                        )
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

    override fun onStart() {
        super.onStart()
        val locationWrapper = LocationServicesWrapper.getInstance(this)
        if (locationWrapper.hasLocationPermission()) {
            locationWrapper.startPeriodicPolling()
        }
    }

    override fun onStop() {
        super.onStop()
        LocationServicesWrapper.getInstance(this).stopPeriodicPolling()
    }

    private fun fetchDeviceLocation() {
        val locationWrapper = LocationServicesWrapper.getInstance(this)
        locationWrapper.startPeriodicPolling()
        lifecycleScope.launch {
            locationWrapper.requestHighAccuracyQiblaLocation(forceFreshGps = false)
        }
    }
}

@Composable
fun MainAppContainer(
    currentLocation: LocationInfo,
    onLocationChange: (LocationInfo) -> Unit,
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeEnterpriseScreen by remember { mutableStateOf(EnterpriseFeatureScreen.NONE) }

    val glassColors = com.example.ui.theme.LocalGlassTheme.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            if (activeEnterpriseScreen == EnterpriseFeatureScreen.NONE) {
                // Adaptive Glassmorphism floating bottom navigation bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    NavigationBar(
                        modifier = Modifier
                            .clip(RoundedCornerShape(26.dp))
                            .border(1.2.dp, glassColors.navBarBorder, RoundedCornerShape(26.dp)),
                        containerColor = glassColors.navBarBackground,
                        tonalElevation = 6.dp
                    ) {
                        // Tab 0: Namaz & Alarms
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
                                selectedIconColor = glassColors.onPrimary,
                                selectedTextColor = glassColors.primary,
                                indicatorColor = glassColors.primary,
                                unselectedIconColor = glassColors.textMuted,
                                unselectedTextColor = glassColors.textMuted
                            )
                        )

                        // Tab 1: 20+ Islamic Sound Hub
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
                                selectedIconColor = glassColors.onPrimary,
                                selectedTextColor = glassColors.primary,
                                indicatorColor = glassColors.primary,
                                unselectedIconColor = glassColors.textMuted,
                                unselectedTextColor = glassColors.textMuted
                            )
                        )

                        // Tab 2: Dual-AI Scholar & Fatwa
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
                                selectedIconColor = glassColors.onPrimary,
                                selectedTextColor = glassColors.primary,
                                indicatorColor = glassColors.primary,
                                unselectedIconColor = glassColors.textMuted,
                                unselectedTextColor = glassColors.textMuted
                            )
                        )

                        // Tab 3: AI Hifz Recitation Checker
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
                                selectedIconColor = glassColors.onPrimary,
                                selectedTextColor = glassColors.primary,
                                indicatorColor = glassColors.primary,
                                unselectedIconColor = glassColors.textMuted,
                                unselectedTextColor = glassColors.textMuted
                            )
                        )

                        // Tab 4: Habits Tracker
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
                                selectedIconColor = glassColors.onPrimary,
                                selectedTextColor = glassColors.primary,
                                indicatorColor = glassColors.primary,
                                unselectedIconColor = glassColors.textMuted,
                                unselectedTextColor = glassColors.textMuted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 0.dp)
        ) {
            when (activeEnterpriseScreen) {
                EnterpriseFeatureScreen.SETTINGS -> {
                    SettingsScreen(
                        onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE },
                        onLogout = onLogout
                    )
                }
                EnterpriseFeatureScreen.QURAN_TAFSIR -> {
                    QuranTafsirScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.TABEER_DREAM -> {
                    com.example.ui.screens.TabeerDreamScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.ARABIC_SCANNER -> {
                    ArabicScannerScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.GLOBAL_DUA_WALL -> {
                    GlobalDuaWallScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.HAJJ_TRACKER -> {
                    HajjFamilyTrackerScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.HALAL_FINANCE -> {
                    HalalFinanceScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.KIDS_LEARNING -> {
                    KidsLearningScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.SMART_ZAKAT -> {
                    SmartZakatScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.PRAYER_TIMES -> {
                    PrayerTimesScreen(
                        currentLocation = currentLocation,
                        onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE },
                        onLocationChange = onLocationChange
                    )
                }
                EnterpriseFeatureScreen.HALAL_SCANNER -> {
                    HalalScannerScreen(onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.ZAKAT_CALCULATOR -> {
                    ZakatCalculatorScreen(onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.GLOBAL_DUA -> {
                    GlobalDuaScreen(onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.QURAN_READER -> {
                    QuranReaderScreen(onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.FAMILY_TRACKER -> {
                    FamilyTrackerScreen(onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.HIFZ_RECORDING -> {
                    HifzRecordingScreen(onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.NONE -> {
                    when (selectedTab) {
                        0 -> NextGenDashboardScreen(
                            onLogout = onLogout,
                            onNavigateToSoundHub = { selectedTab = 1 },
                            onNavigateToQuran = { activeEnterpriseScreen = EnterpriseFeatureScreen.QURAN_TAFSIR },
                            onNavigateToDream = { activeEnterpriseScreen = EnterpriseFeatureScreen.TABEER_DREAM },
                            onNavigateToScanner = { activeEnterpriseScreen = EnterpriseFeatureScreen.ARABIC_SCANNER },
                            onNavigateToDuaWall = { activeEnterpriseScreen = EnterpriseFeatureScreen.GLOBAL_DUA_WALL },
                            onNavigateToHajj = { activeEnterpriseScreen = EnterpriseFeatureScreen.HAJJ_TRACKER },
                            onNavigateToFinance = { activeEnterpriseScreen = EnterpriseFeatureScreen.HALAL_FINANCE },
                            onNavigateToKids = { activeEnterpriseScreen = EnterpriseFeatureScreen.KIDS_LEARNING },
                            onNavigateToZakat = { activeEnterpriseScreen = EnterpriseFeatureScreen.SMART_ZAKAT },
                            onNavigateToPrayerTimes = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRAYER_TIMES },
                            onNavigateToSettings = { activeEnterpriseScreen = EnterpriseFeatureScreen.SETTINGS }
                        )
                        1 -> SoundHubScreen(onNavigateBack = { selectedTab = 0 })
                        2 -> AiCompanionScreen()
                        3 -> HifzCheckerScreen()
                        4 -> HabitTrackerScreen()
                    }
                }
            }
        }
    }
}
