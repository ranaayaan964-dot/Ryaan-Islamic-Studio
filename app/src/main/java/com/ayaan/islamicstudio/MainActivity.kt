package com.ayaan.islamicstudio

import android.app.Activity
import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayaan.islamicstudio.alarm.NamazAlarmWorker
import com.ayaan.islamicstudio.alarm.NamazWorkManagerScheduler
import com.ayaan.islamicstudio.alarm.PrayerAlarmScheduler
import com.ayaan.islamicstudio.alarm.PrayerAlarmService
import com.ayaan.islamicstudio.data.database.AppDatabase
import com.ayaan.islamicstudio.data.repository.AdminRepository
import com.ayaan.islamicstudio.data.repository.ProSubscriptionRepository
import com.ayaan.islamicstudio.data.model.LocationInfo
import com.ayaan.islamicstudio.data.repository.LocationRepository
import com.ayaan.islamicstudio.audio.AudioCacheManager
import com.ayaan.islamicstudio.service.audio.QuranAudioController
import com.ayaan.islamicstudio.service.location.LocationServicesWrapper
import com.ayaan.islamicstudio.ui.components.UpdateDialog
import com.ayaan.islamicstudio.ui.components.WilmsAiPromotionDialog
import com.ayaan.islamicstudio.ui.components.ads.UnityAdsController
import com.ayaan.islamicstudio.ui.components.ads.UnityInterstitialManager
import com.ayaan.islamicstudio.ui.components.ads.UnityRewardedManager
import com.ayaan.islamicstudio.ui.components.audio.QuranPlaybackControlBar
import com.ayaan.islamicstudio.ui.screens.AiCompanionScreen
import com.ayaan.islamicstudio.ui.screens.ArabicScannerScreen
import com.ayaan.islamicstudio.ui.screens.ArQiblaScreen
import com.ayaan.islamicstudio.ui.screens.GlobalDuaWallScreen
import com.ayaan.islamicstudio.ui.screens.HabitTrackerScreen
import com.ayaan.islamicstudio.ui.screens.HifzCheckerScreen
import com.ayaan.islamicstudio.ui.screens.KidsLearningScreen
import com.ayaan.islamicstudio.ui.screens.LoginScreen
import com.ayaan.islamicstudio.ui.screens.NextGenDashboardScreen
import com.ayaan.islamicstudio.ui.screens.PrayerTimesScreen
import com.ayaan.islamicstudio.ui.screens.QazaNamazScreen
import com.ayaan.islamicstudio.ui.screens.QuranTafsirScreen
import com.ayaan.islamicstudio.ui.screens.SmartZakatScreen
import com.ayaan.islamicstudio.ui.screens.SoundHubScreen
import com.ayaan.islamicstudio.ui.screens.SplashScreen
import com.ayaan.islamicstudio.ui.screens.TahajjudWakeupScreen
import com.ayaan.islamicstudio.ui.screens.TermsAgreementScreen
import com.ayaan.islamicstudio.ui.theme.DeepRoyalEmerald
import com.ayaan.islamicstudio.ui.theme.MyApplicationTheme
import com.ayaan.islamicstudio.ui.theme.PlatinumGold
import com.ayaan.islamicstudio.ui.theme.PureWhite
import com.ayaan.islamicstudio.ui.screens.FamilyTrackerScreen
import com.ayaan.islamicstudio.ui.screens.GlobalDuaScreen
import com.ayaan.islamicstudio.ui.screens.HalalScannerScreen
import com.ayaan.islamicstudio.ui.screens.HifzRecordingScreen
import com.ayaan.islamicstudio.ui.screens.QuranReaderScreen
import com.ayaan.islamicstudio.ui.screens.SettingsScreen
import com.ayaan.islamicstudio.ui.screens.ZakatCalculatorScreen
import com.ayaan.islamicstudio.security.SessionManager
import com.ayaan.islamicstudio.util.UpdateViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class AppDestination {
    SPLASH,
    LOGIN,
    AGREEMENT,
    PLAN_SELECTION,
    ADMIN_DASHBOARD,
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
    AR_QIBLA,
    TAHAJJUD_WAKEUP,
    KIDS_LEARNING,
    SMART_ZAKAT,
    HALAL_SCANNER,
    ZAKAT_CALCULATOR,
    GLOBAL_DUA,
    QURAN_READER,
    FAMILY_TRACKER,
    HIFZ_RECORDING,
    QAZA_NAMAZ,
    USER_ADMIN_INBOX,
    TASBEEH,
    PRO_ACTIVATION
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext ?: return null
    }
    return current as? Activity
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

        lifecycleScope.launch(Dispatchers.IO) {
            AudioCacheManager.initialize(this@MainActivity)
            NamazWorkManagerScheduler.cancelScheduledMetadataAlarms(this@MainActivity)
            PrayerAlarmService.createNotificationChannel(this@MainActivity)
            PrayerAlarmScheduler.ensureAllAlarmsScheduled(this@MainActivity)

            NamazAlarmWorker.createNamazNotificationChannel(this@MainActivity)
            NamazWorkManagerScheduler.schedulePeriodicDailySync(this@MainActivity)
        }

        // Observe cached coordinates from DataStore to minimize battery impact and provide instant location
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                LocationServicesWrapper.getInstance(this@MainActivity)
                    .cachedLocationFlow
                    .collect { cached ->
                        userLocation = cached.toLocationInfo()
                    }
                }
        }

        requestAppPermissions()

        setContent {
            var currentDestination by remember { mutableStateOf(AppDestination.SPLASH) }
            var showWilmsPromotion by remember { mutableStateOf(false) }
            val updateViewModel: UpdateViewModel = viewModel()
            val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
            val themeRepo = remember { com.ayaan.islamicstudio.data.repository.ThemeSettingsRepository.getInstance(this@MainActivity) }
            val currentTheme by themeRepo.currentThemeFlow.collectAsStateWithLifecycle(initialValue = com.ayaan.islamicstudio.ui.theme.ThemeType.PEARL_WHITE)
            val activeUser by remember {
                AppDatabase.getInstance(this@MainActivity).userDao().getCurrentUserFlow()
            }.collectAsStateWithLifecycle(initialValue = null)
            val proRepository = remember { ProSubscriptionRepository.getInstance(this@MainActivity) }
            val proStatus by proRepository.proStatusFlow.collectAsStateWithLifecycle()
            var isAdminSession by remember { mutableStateOf(false) }
            var adminCheckResolved by remember { mutableStateOf(false) }

            LaunchedEffect(currentDestination, activeUser?.firebaseUid) {
                if (currentDestination == AppDestination.ADMIN_DASHBOARD) {
                    adminCheckResolved = false
                    val verification = try {
                        AdminRepository(this@MainActivity).isAdminSession()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        null
                    }
                    isAdminSession = verification == true
                    adminCheckResolved = true
                    if (adminCheckResolved && verification == false) {
                        currentDestination = AppDestination.MAIN_APP
                    }
                } else if (currentDestination == AppDestination.MAIN_APP) {
                    val verification = try {
                        AdminRepository(this@MainActivity).isAdminSession()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Exception) {
                        null
                    }
                    isAdminSession = verification == true
                    adminCheckResolved = true
                } else {
                    isAdminSession = false
                    adminCheckResolved = true
                }
            }

            val logout = {
                lifecycleScope.launch(Dispatchers.IO) {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    SessionManager.getInstance(this@MainActivity).logout()
                    AppDatabase.getInstance(this@MainActivity).userDao().logout()
                    com.ayaan.islamicstudio.data.repository.SettingsRepository.getInstance(this@MainActivity).setLoggedIn(false)
                }
                currentDestination = AppDestination.LOGIN
            }

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
                                    currentDestination = if (isLoggedIn) AppDestination.AGREEMENT else AppDestination.LOGIN
                                    showWilmsPromotion = true
                                }
                            )
                        }
                        AppDestination.LOGIN -> {
                            LoginScreen(
                                onLoginSuccess = {
                                    currentDestination = AppDestination.AGREEMENT
                                }
                            )
                        }
                        AppDestination.AGREEMENT -> {
                            TermsAgreementScreen(
                                onAgree = {
                                    lifecycleScope.launch {
                                        val admin = try {
                                            AdminRepository(this@MainActivity).isAdminSession()
                                        } catch (error: CancellationException) {
                                            throw error
                                        } catch (_: Exception) {
                                            false
                                        }
                                        isAdminSession = admin
                                        adminCheckResolved = true
                                        currentDestination = if (admin) {
                                            AppDestination.ADMIN_DASHBOARD
                                        } else {
                                            runCatching {
                                                proRepository.refreshCurrentProStatus()
                                            }
                                            if (proRepository.proStatusFlow.value.isProActive) {
                                                AppDestination.MAIN_APP
                                            } else {
                                                AppDestination.PLAN_SELECTION
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        AppDestination.PLAN_SELECTION -> {
                            com.ayaan.islamicstudio.ui.screens.ProSubscriptionScreen(
                                onNavigateBack = { currentDestination = AppDestination.LOGIN },
                                onContinueWithFree = { currentDestination = AppDestination.MAIN_APP },
                                isPostLoginStep = true,
                                onProActivated = { currentDestination = AppDestination.MAIN_APP }
                            )
                        }
                        AppDestination.ADMIN_DASHBOARD -> {
                            com.ayaan.islamicstudio.ui.screens.AdminDashboardScreen(
                                onOpenUserApp = { currentDestination = AppDestination.MAIN_APP },
                                onLogout = logout
                            )
                        }
                        AppDestination.MAIN_APP -> {
                            MainAppContainer(
                                currentLocation = userLocation,
                                onLocationChange = { userLocation = it },
                                onLogout = logout,
                                isAdmin = isAdminSession,
                                onOpenAdminDashboard = { currentDestination = AppDestination.ADMIN_DASHBOARD }
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
                    if (showWilmsPromotion && !updateState.showUpdateDialog) {
                        WilmsAiPromotionDialog(
                            onDismiss = { showWilmsPromotion = false }
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

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrayerAlarmScheduler.canScheduleExactAlarms(this@MainActivity)) {
                PrayerAlarmScheduler.rescheduleFromStorage(this@MainActivity)
            }
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
    onLogout: () -> Unit = {},
    isAdmin: Boolean = false,
    onOpenAdminDashboard: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeEnterpriseScreen by remember { mutableStateOf(EnterpriseFeatureScreen.NONE) }
    var selectedTafsirSurahId by remember { mutableIntStateOf(1) }
    BackHandler(enabled = activeEnterpriseScreen != EnterpriseFeatureScreen.NONE || selectedTab != 0) {
        if (activeEnterpriseScreen != EnterpriseFeatureScreen.NONE) {
            activeEnterpriseScreen = EnterpriseFeatureScreen.NONE
        } else {
            selectedTab = 0
        }
    }

    val context = LocalContext.current
    val glassColors = com.ayaan.islamicstudio.ui.theme.LocalGlassTheme.current
    val unityAdsReady by UnityAdsController.isInitialized.collectAsStateWithLifecycle()

    LaunchedEffect(unityAdsReady) {
        if (unityAdsReady) {
            context.findActivity()?.let { activity ->
                UnityInterstitialManager.preloadInterstitial(activity)
                UnityRewardedManager.preloadRewarded(activity)
            }
        }
    }

    val openWithInterstitial: (() -> Unit) -> Unit = { openModule ->
        val activity = context.findActivity()
        if (activity == null) {
            openModule()
        } else {
            UnityInterstitialManager.showInterstitial(activity, onAdClosed = openModule)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            val isAiTaskScreen = selectedTab == 2 || selectedTab == 3
            if (activeEnterpriseScreen == EnterpriseFeatureScreen.NONE && !isAiTaskScreen) {
                val isAudioPlaying by QuranAudioController.isPlaying.collectAsStateWithLifecycle()
                val audioDurationMs by QuranAudioController.currentDurationMs.collectAsStateWithLifecycle()
                val audioPosMs by QuranAudioController.currentPositionMs.collectAsStateWithLifecycle()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // DOCKED PLAYBACK CONTROL BAR (Features Play/Pause, Track Progress, Seeking & Qari info)
                    if (isAudioPlaying || audioDurationMs > 0L || audioPosMs > 0L) {
                        QuranPlaybackControlBar(
                            modifier = Modifier.padding(bottom = 2.dp),
                            onTitleClick = {
                                activeEnterpriseScreen = EnterpriseFeatureScreen.QURAN_READER
                            }
                        )
                    }

                    // Adaptive Glassmorphism floating bottom navigation bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
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
                            onClick = {
                                if (selectedTab != 4) {
                                    openWithInterstitial { selectedTab = 4 }
                                }
                            },
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
                        onLogout = onLogout,
                        onOpenProActivation = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRO_ACTIVATION }
                    )
                }
                EnterpriseFeatureScreen.PRO_ACTIVATION -> {
                    com.ayaan.islamicstudio.ui.screens.ProSubscriptionScreen(
                        onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE },
                        onContinueWithFree = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE },
                        onProActivated = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE }
                    )
                }
                EnterpriseFeatureScreen.QURAN_TAFSIR -> {
                    QuranTafsirScreen(
                        initialSurahId = selectedTafsirSurahId,
                        onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE },
                        onOpenProActivation = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRO_ACTIVATION }
                    )
                }
                EnterpriseFeatureScreen.TABEER_DREAM -> {
                    com.ayaan.islamicstudio.ui.screens.TabeerDreamScreen(
                        onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE },
                        onOpenProActivation = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRO_ACTIVATION }
                    )
                }
                EnterpriseFeatureScreen.ARABIC_SCANNER -> {
                    ArabicScannerScreen(
                        onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE },
                        onOpenProActivation = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRO_ACTIVATION }
                    )
                }
                EnterpriseFeatureScreen.GLOBAL_DUA_WALL -> {
                    GlobalDuaWallScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.AR_QIBLA -> {
                    ArQiblaScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.TAHAJJUD_WAKEUP -> {
                    TahajjudWakeupScreen(onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.KIDS_LEARNING -> {
                    KidsLearningScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.SMART_ZAKAT -> {
                    SmartZakatScreen(
                        onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE },
                        onOpenProActivation = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRO_ACTIVATION }
                    )
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
                    ZakatCalculatorScreen(
                        onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE },
                        onOpenProActivation = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRO_ACTIVATION }
                    )
                }
                EnterpriseFeatureScreen.GLOBAL_DUA -> {
                    GlobalDuaScreen(onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.QURAN_READER -> {
                    QuranReaderScreen(
                        onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE },
                        onOpenTafsir = {
                            selectedTafsirSurahId = it
                            activeEnterpriseScreen = EnterpriseFeatureScreen.QURAN_TAFSIR
                        }
                    )
                }
                EnterpriseFeatureScreen.FAMILY_TRACKER -> {
                    FamilyTrackerScreen(onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.HIFZ_RECORDING -> {
                    HifzRecordingScreen(
                        onBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE },
                        onOpenProActivation = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRO_ACTIVATION }
                    )
                }
                EnterpriseFeatureScreen.QAZA_NAMAZ -> {
                    QazaNamazScreen(onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE })
                }
                EnterpriseFeatureScreen.USER_ADMIN_INBOX -> {
                    com.ayaan.islamicstudio.ui.screens.UserAdminInboxScreen(
                        onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE }
                    )
                }
                EnterpriseFeatureScreen.TASBEEH -> {
                    com.ayaan.islamicstudio.ui.screens.TasbeehScreen(
                        onNavigateBack = { activeEnterpriseScreen = EnterpriseFeatureScreen.NONE }
                    )
                }
                EnterpriseFeatureScreen.NONE -> {
                    when (selectedTab) {
                        0 -> NextGenDashboardScreen(
                            onLogout = onLogout,
                            onNavigateToSoundHub = { selectedTab = 1 },
                            onNavigateToQuran = { activeEnterpriseScreen = EnterpriseFeatureScreen.QURAN_READER },
                            onNavigateToDream = { activeEnterpriseScreen = EnterpriseFeatureScreen.TABEER_DREAM },
                            onNavigateToScanner = { activeEnterpriseScreen = EnterpriseFeatureScreen.ARABIC_SCANNER },
                            onNavigateToDuaWall = { activeEnterpriseScreen = EnterpriseFeatureScreen.GLOBAL_DUA_WALL },
                            onNavigateToKids = { activeEnterpriseScreen = EnterpriseFeatureScreen.KIDS_LEARNING },
                            onNavigateToZakat = {
                                openWithInterstitial {
                                    activeEnterpriseScreen = EnterpriseFeatureScreen.SMART_ZAKAT
                                }
                            },
                            onNavigateToPrayerTimes = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRAYER_TIMES },
                            onNavigateToSettings = { activeEnterpriseScreen = EnterpriseFeatureScreen.SETTINGS },
                            onNavigateToArQibla = { activeEnterpriseScreen = EnterpriseFeatureScreen.AR_QIBLA },
                            onNavigateToTahajjud = { activeEnterpriseScreen = EnterpriseFeatureScreen.TAHAJJUD_WAKEUP },
                            onNavigateToQaza = { activeEnterpriseScreen = EnterpriseFeatureScreen.QAZA_NAMAZ },
                            onNavigateToAdminInbox = { activeEnterpriseScreen = EnterpriseFeatureScreen.USER_ADMIN_INBOX },
                            onNavigateToHalalScanner = { activeEnterpriseScreen = EnterpriseFeatureScreen.HALAL_SCANNER },
                            onNavigateToFamilyTracker = { activeEnterpriseScreen = EnterpriseFeatureScreen.FAMILY_TRACKER },
                            onNavigateToTasbeeh = { activeEnterpriseScreen = EnterpriseFeatureScreen.TASBEEH },
                            onNavigateToHifzStudio = { activeEnterpriseScreen = EnterpriseFeatureScreen.HIFZ_RECORDING },
                            onOpenProActivation = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRO_ACTIVATION },
                            isAdmin = isAdmin,
                            onOpenAdminDashboard = onOpenAdminDashboard
                        )
                        1 -> SoundHubScreen(onNavigateBack = { selectedTab = 0 })
                        2 -> AiCompanionScreen(
                            onOpenProActivation = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRO_ACTIVATION }
                        )
                        3 -> HifzCheckerScreen(
                            onOpenProActivation = { activeEnterpriseScreen = EnterpriseFeatureScreen.PRO_ACTIVATION }
                        )
                        4 -> HabitTrackerScreen()
                    }
                }
            }
        }
    }
}
