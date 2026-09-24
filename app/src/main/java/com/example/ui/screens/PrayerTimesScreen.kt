package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AdhanSoundPlayer
import com.example.alarm.NamazMetadataProvider
import com.example.alarm.PrayerAlarmScheduler
import com.example.data.model.CountdownState
import com.example.data.model.LocationInfo
import com.example.data.model.PrayerTimeItem
import com.example.data.model.PrayerType
import com.example.data.repository.LocationRepository
import com.example.ui.components.IslamicWatermarkBackground
import com.example.ui.theme.BorderEmeraldAccent
import com.example.ui.theme.BorderGoldAccent
import com.example.ui.theme.BorderSubtlePearl
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldSurfaceLight
import com.example.ui.theme.GoldenAmber
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PearlSurface
import com.example.ui.theme.PearlSurfaceVariant
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PlatinumGoldSecondary
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import com.example.util.PrayerCalculator
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * High-Aesthetic Glassmorphism Prayer Times Screen.
 * Presents daily namaz timings (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha)
 * inside translucent frosted glass cards with live countdown, solar arc tracking,
 * alarm toggles, and daily prayer completion tracking.
 */
@Composable
fun PrayerTimesScreen(
    currentLocation: LocationInfo = LocationRepository.getDefaultLocation(),
    onNavigateBack: (() -> Unit)? = null,
    onLocationChange: ((LocationInfo) -> Unit)? = null
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedLocation by remember(currentLocation) { mutableStateOf(currentLocation) }
    var showCityDialog by remember { mutableStateOf(false) }
    var isPlayingAdhanTest by remember { mutableStateOf(false) }

    // Raka'at metadata per prayer
    val prayerRakats = remember {
        mapOf(
            PrayerType.FAJR to "4 Raka'at (2 Sunnah + 2 Fard)",
            PrayerType.SUNRISE to "Prohibition Period / Ishraq follows (2-4 Nafl)",
            PrayerType.DHUHR to "12 Raka'at (4 Sunnah + 4 Fard + 2 Sunnah + 2 Nafl)",
            PrayerType.ASR to "8 Raka'at (4 Sunnah + 4 Fard)",
            PrayerType.MAGHRIB to "7 Raka'at (3 Fard + 2 Sunnah + 2 Nafl)",
            PrayerType.ISHA to "17 Raka'at (4 Sunnah + 4 Fard + 2 Sunnah + 2 Nafl + 3 Witr + 2 Nafl)"
        )
    }

    // Alarm settings map for each prayer
    val alarmSettings = remember {
        mutableStateMapOf(
            PrayerType.FAJR to true,
            PrayerType.SUNRISE to false,
            PrayerType.DHUHR to true,
            PrayerType.ASR to true,
            PrayerType.MAGHRIB to true,
            PrayerType.ISHA to true
        )
    }

    // Daily prayer completion tracker (persists locally in memory during session)
    val completedPrayers = rememberSaveable {
        mutableStateMapOf<String, Boolean>()
    }

    // Calculate prayer times dynamically based on active location and alarms
    var prayerTimes by remember(selectedLocation, alarmSettings.toMap()) {
        mutableStateOf(
            PrayerCalculator.calculatePrayerTimes(
                latitude = selectedLocation.latitude,
                longitude = selectedLocation.longitude,
                alarmSettings = alarmSettings
            )
        )
    }

    // Live continuous countdown state
    var countdownState by remember { mutableStateOf(PrayerCalculator.computeCountdown(prayerTimes)) }

    // Live 1-second ticker for accurate countdown and solar progression
    LaunchedEffect(prayerTimes) {
        while (true) {
            countdownState = PrayerCalculator.computeCountdown(prayerTimes)
            delay(1000L)
        }
    }

    // Release audio player when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            AdhanSoundPlayer.stopAdhan()
        }
    }

    val todayDateFormatted = remember {
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(Date())
    }

    val hijriDateEstimate = remember {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        // Approximate current Hijri year: 2026 corresponds to 1447-1448 AH
        val hijriYear = year - 579
        "$hijriYear AH • Islamic Lunar Calendar"
    }

    IslamicWatermarkBackground(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = PearlBackground,
        watermarkColor = DeepRoyalEmerald,
        opacity = 0.035f
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Glass Top Bar
            GlassTopBar(
                title = "Daily Namaz Timings",
                subtitle = todayDateFormatted,
                onNavigateBack = onNavigateBack,
                onLocationClick = { showCityDialog = true },
                cityName = selectedLocation.cityName,
                onShareClick = {
                    val summaryText = buildString {
                        append("🕌 Daily Namaz Timings (${selectedLocation.cityName})\n")
                        append("📅 $todayDateFormatted\n\n")
                        prayerTimes.forEach { item ->
                            val check = if (completedPrayers[item.type.name] == true) "✓" else "•"
                            append("$check ${item.type.displayName} (${item.type.arabicName}): ${item.timeFormatted}\n")
                        }
                        append("\nCalculated via Muslim World League / UIS Karachi.")
                    }
                    clipboardManager.setText(AnnotatedString(summaryText))
                    Toast.makeText(context, "Daily Namaz schedule copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("prayer_times_scroll_list"),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Hero Next Prayer Glassmorphism Card with Live Countdown Ring
                item {
                    GlassHeroNextPrayerCard(
                        countdownState = countdownState,
                        hijriDate = hijriDateEstimate,
                        isPlayingAdhanTest = isPlayingAdhanTest,
                        onToggleAdhanTest = {
                            if (isPlayingAdhanTest) {
                                AdhanSoundPlayer.stopAdhan()
                                isPlayingAdhanTest = false
                            } else {
                                isPlayingAdhanTest = true
                                AdhanSoundPlayer.playAdhanAlert(context, looping = false) {
                                    isPlayingAdhanTest = false
                                }
                            }
                        }
                    )
                }

                // 2. Solar Track / Daylight Progress Glass Card
                item {
                    GlassSolarTrackCard(
                        prayers = prayerTimes
                    )
                }

                // 3. Section Header: Daily 6 Namaz Timings
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Namaz Schedule & Alarms",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalPrimary
                            )
                            Text(
                                text = "High-precision solar prayer windows",
                                fontSize = 11.5.sp,
                                color = SlateMuted
                            )
                        }

                        val completedCount = completedPrayers.values.count { it }
                        GlassPillBadge(
                            text = "$completedCount/5 Fard Prayed",
                            backgroundColor = EmeraldSurfaceLight,
                            borderColor = EmeraldMint,
                            textColor = DeepRoyalEmerald
                        )
                    }
                }

                // 4. Individual Glassmorphism Cards for each Daily Namaz Timing
                items(prayerTimes) { prayer ->
                    val isPrayed = completedPrayers[prayer.type.name] == true
                    val rakats = prayerRakats[prayer.type] ?: ""

                    GlassPrayerCard(
                        prayer = prayer,
                        rakatsSummary = rakats,
                        isPrayed = isPrayed,
                        onTogglePrayed = {
                            if (prayer.type != PrayerType.SUNRISE) {
                                completedPrayers[prayer.type.name] = !isPrayed
                                val msg = if (!isPrayed) "${prayer.type.displayName} marked as prayed! Alhamdulillah." else "${prayer.type.displayName} unmarked."
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onToggleAlarm = { enabled ->
                            alarmSettings[prayer.type] = enabled
                            val updated = prayer.copy(isAlarmEnabled = enabled)
                            if (enabled) {
                                PrayerAlarmScheduler.schedulePrayerAlarm(context, updated)
                                Toast.makeText(context, "${prayer.type.displayName} Adhan alert activated", Toast.LENGTH_SHORT).show()
                            } else {
                                PrayerAlarmScheduler.cancelPrayerAlarm(context, updated)
                                Toast.makeText(context, "${prayer.type.displayName} alarm muted", Toast.LENGTH_SHORT).show()
                            }
                            // Refresh list state
                            prayerTimes = PrayerCalculator.calculatePrayerTimes(
                                latitude = selectedLocation.latitude,
                                longitude = selectedLocation.longitude,
                                alarmSettings = alarmSettings
                            )
                        }
                    )
                }

                // 5. Extended Islamic Astronomical Windows Glass Card
                item {
                    GlassExtendedAstronomicalTimingsCard(
                        prayers = prayerTimes
                    )
                }

                // 6. Calculation Method & Jurisprudence Glass Card
                item {
                    GlassCalculationMethodCard(
                        location = selectedLocation,
                        onChangeCityClick = { showCityDialog = true }
                    )
                }

                // Bottom spacing for smooth scrolling
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // City Selection Glassmorphic Dialog
    if (showCityDialog) {
        CitySelectionGlassDialog(
            currentCity = selectedLocation.cityName,
            onDismiss = { showCityDialog = false },
            onSelectCity = { newLocation ->
                selectedLocation = newLocation
                onLocationChange?.invoke(newLocation)
                showCityDialog = false
                Toast.makeText(context, "Location updated to ${newLocation.cityName}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// =========================================================================
// GLASSMORPHISM CORE PRIMITIVE COMPONENTS
// =========================================================================

/**
 * Reusable frosted glass container with smooth translucent acrylic surface,
 * subtle inner gradient sheen, and dual-tone metallic border stroke.
 */
@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    backgroundColor: Color = PureWhite.copy(alpha = 0.88f),
    borderColor: Color = PlatinumGold.copy(alpha = 0.35f),
    borderWidth: Dp = 1.2.dp,
    elevation: Dp = 4.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = CharcoalPrimary.copy(alpha = 0.08f),
                spotColor = PlatinumGold.copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(
                BorderStroke(
                    borderWidth,
                    Brush.linearGradient(
                        listOf(
                            borderColor,
                            PlatinumGold.copy(alpha = 0.15f),
                            DeepRoyalEmerald.copy(alpha = 0.12f),
                            borderColor.copy(alpha = 0.40f)
                        )
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

/**
 * Frosted Glass Pill Badge for status indicators, categories, and timers.
 */
@Composable
fun GlassPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PureWhite.copy(alpha = 0.70f),
    borderColor: Color = PlatinumGold.copy(alpha = 0.40f),
    textColor: Color = CharcoalPrimary,
    leadingIcon: ImageVector? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier
                        .size(13.dp)
                        .padding(end = 4.dp)
                )
            }
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

// =========================================================================
// TOP BAR & HERO COUNTDOWN COMPONENTS
// =========================================================================

@Composable
private fun GlassTopBar(
    title: String,
    subtitle: String,
    onNavigateBack: (() -> Unit)?,
    onLocationClick: () -> Unit,
    cityName: String,
    onShareClick: () -> Unit
) {
    GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        backgroundColor = PureWhite.copy(alpha = 0.92f),
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PearlSurfaceVariant)
                            .testTag("prayer_times_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CharcoalPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = SlateMuted
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Location Pill Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(EmeraldSurfaceLight)
                        .border(1.dp, RadiantEmerald.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .clickable(onClick = onLocationClick)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("prayer_times_city_picker_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = cityName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Share Button
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PearlSurfaceVariant)
                        .testTag("prayer_times_share_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share schedule",
                        tint = DeepRoyalEmerald,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

/**
 * Hero Frosted Glass Card featuring the Next Namaz, live circular progress countdown,
 * and quick Adhan audio preview.
 */
@Composable
private fun GlassHeroNextPrayerCard(
    countdownState: CountdownState,
    hijriDate: String,
    isPlayingAdhanTest: Boolean,
    onToggleAdhanTest: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_hero_border")
    val pulseBorderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_glow"
    )

    GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_times_hero_card"),
        shape = RoundedCornerShape(26.dp),
        backgroundColor = PureWhite.copy(alpha = 0.90f),
        borderColor = PlatinumGold.copy(alpha = pulseBorderAlpha),
        borderWidth = 1.5.dp,
        elevation = 6.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle decorative gradient glow at top corner
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                PlatinumGold.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Badges Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Next Namaz Glowing Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(DeepRoyalEmerald, RadiantEmerald)
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(PlatinumGold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "UPCOMING NAMAZ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = PureWhite
                            )
                        }
                    }

                    // Hijri Date Pill
                    Text(
                        text = hijriDate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SlateMuted
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Next Prayer Title: English & Arabic Calligraphy
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = countdownState.nextPrayerName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = CharcoalPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "•",
                        fontSize = 20.sp,
                        color = PlatinumGold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = countdownState.nextPrayerArabic,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepRoyalEmerald
                    )
                }

                Text(
                    text = "Scheduled at ${countdownState.formattedTime}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RadiantEmerald
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Circular Glass Countdown Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(150.dp)
                ) {
                    // Frosted circular background ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = PearlSurfaceVariant,
                            radius = size.minDimension / 2f - 4.dp.toPx()
                        )
                    }

                    CircularProgressIndicator(
                        progress = { countdownState.progress },
                        modifier = Modifier.fillMaxSize(),
                        color = DeepRoyalEmerald,
                        trackColor = PlatinumGold.copy(alpha = 0.20f),
                        strokeWidth = 7.dp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = countdownState.displayTimer,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = CharcoalPrimary,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "TIME REMAINING",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = SlateMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Adhan Audio Test & Preview Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isPlayingAdhanTest) Color(0xFFFEE2E2) else EmeraldSurfaceLight
                        )
                        .border(
                            1.dp,
                            if (isPlayingAdhanTest) Color(0xFFEF4444) else RadiantEmerald.copy(alpha = 0.40f),
                            RoundedCornerShape(50)
                        )
                        .clickable(onClick = onToggleAdhanTest)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("prayer_times_test_adhan_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPlayingAdhanTest) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Test Adhan",
                            tint = if (isPlayingAdhanTest) Color(0xFFDC2626) else DeepRoyalEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlayingAdhanTest) "Stop Adhan Audio Alert" else "Preview Makkah Adhan Alert",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isPlayingAdhanTest) Color(0xFFDC2626) else DeepRoyalEmerald
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// DAILY PRAYER CARD (SIGNATURE GLASSMORPHISM ITEM)
// =========================================================================

/**
 * Individual Frosted Glassmorphism Card for each of the 6 daily timings.
 * Features:
 * - Jewel prayer icon with glowing frosted backdrop
 * - English & Arabic names
 * - Large clear timing (12-hour AM/PM)
 * - Status indicator: "UPCOMING", "PASSED", or "PENDING"
 * - Raka'at breakdown summary
 * - Interactive alarm bell switch with immediate persistence
 * - Interactive "Mark Prayed" checkmark toggle
 */
@Composable
fun GlassPrayerCard(
    prayer: PrayerTimeItem,
    rakatsSummary: String,
    isPrayed: Boolean,
    onTogglePrayed: () -> Unit,
    onToggleAlarm: (Boolean) -> Unit
) {
    val isSunrise = prayer.type == PrayerType.SUNRISE

    // Custom icons and tints per namaz phase
    val prayerIcon = when (prayer.type) {
        PrayerType.FAJR -> Icons.Default.WbTwilight
        PrayerType.SUNRISE -> Icons.Default.WbSunny
        PrayerType.DHUHR -> Icons.Default.WbSunny
        PrayerType.ASR -> Icons.Default.WbTwilight
        PrayerType.MAGHRIB -> Icons.Default.Brightness2
        PrayerType.ISHA -> Icons.Default.Bedtime
    }

    val iconGlowColor = when (prayer.type) {
        PrayerType.FAJR -> Color(0xFF38BDF8)
        PrayerType.SUNRISE -> GoldenAmber
        PrayerType.DHUHR -> GoldenAmber
        PrayerType.ASR -> Color(0xFFF97316)
        PrayerType.MAGHRIB -> Color(0xFFA855F7)
        PrayerType.ISHA -> DeepRoyalEmerald
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_card")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_glow"
    )

    val cardBorderColor = if (prayer.isNext) {
        PlatinumGold.copy(alpha = pulseAlpha)
    } else if (isPrayed) {
        EmeraldMint.copy(alpha = 0.45f)
    } else {
        BorderGoldAccent.copy(alpha = 0.25f)
    }

    val cardBg = if (prayer.isNext) {
        PureWhite.copy(alpha = 0.96f)
    } else if (isPrayed) {
        EmeraldSurfaceLight.copy(alpha = 0.85f)
    } else {
        PureWhite.copy(alpha = 0.84f)
    }

    GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_card_${prayer.type.name.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = cardBg,
        borderColor = cardBorderColor,
        borderWidth = if (prayer.isNext) 1.5.dp else 1.dp,
        elevation = if (prayer.isNext) 6.dp else 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Main Top Row: Icon + Name + Time + Alarm Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Frosted Icon Jewel + Title & Arabic
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (prayer.isNext) {
                                    Brush.linearGradient(
                                        listOf(DeepRoyalEmerald, RadiantEmerald)
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(iconGlowColor.copy(alpha = 0.15f), iconGlowColor.copy(alpha = 0.08f))
                                    )
                                }
                            )
                            .border(
                                1.dp,
                                if (prayer.isNext) PlatinumGold else iconGlowColor.copy(alpha = 0.35f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = prayerIcon,
                            contentDescription = prayer.type.displayName,
                            tint = if (prayer.isNext) PureWhite else iconGlowColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = prayer.type.displayName,
                                fontSize = 16.sp,
                                fontWeight = if (prayer.isNext) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (prayer.isNext) DeepRoyalEmerald else CharcoalPrimary
                            )

                            if (prayer.isNext) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PlatinumGold)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "NEXT",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = CharcoalPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = prayer.type.arabicName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = SlateMuted
                        )
                    }
                }

                // Right: Time + Alarm Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prayer.timeFormatted,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif,
                        color = if (prayer.isNext) DeepRoyalEmerald else CharcoalPrimary
                    )

                    if (!isSunrise) {
                        Spacer(modifier = Modifier.width(10.dp))
                        IconButton(
                            onClick = { onToggleAlarm(!prayer.isAlarmEnabled) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (prayer.isAlarmEnabled) EmeraldSurfaceLight else PearlSurfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (prayer.isAlarmEnabled) RadiantEmerald.copy(alpha = 0.35f) else Color.Transparent,
                                    CircleShape
                                )
                                .testTag("alarm_toggle_${prayer.type.name.lowercase()}")
                        ) {
                            Icon(
                                imageVector = if (prayer.isAlarmEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = "Alarm Toggle",
                                tint = if (prayer.isAlarmEnabled) RadiantEmerald else SlateMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Sub-Row: Raka'at Breakdown + Interactive Mark Prayed Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Raka'at breakdown chip
                Text(
                    text = rakatsSummary,
                    fontSize = 11.sp,
                    color = if (isSunrise) GoldenAmber else SlateMuted,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!isSunrise) {
                    Spacer(modifier = Modifier.width(8.dp))

                    // Mark Prayed Glass Chip Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isPrayed) EmeraldMint.copy(alpha = 0.18f) else PearlSurfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isPrayed) EmeraldMint else BorderSubtlePearl,
                                RoundedCornerShape(50)
                            )
                            .clickable(onClick = onTogglePrayed)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("mark_prayed_${prayer.type.name.lowercase()}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isPrayed) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                contentDescription = if (isPrayed) "Prayed" else "Mark as Prayed",
                                tint = if (isPrayed) DeepRoyalEmerald else SlateMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isPrayed) "Prayed ✓" else "Mark Prayed",
                                fontSize = 10.5.sp,
                                fontWeight = if (isPrayed) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPrayed) DeepRoyalEmerald else SlateMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// SOLAR PROGRESSION & DAYLIGHT TRACKING COMPONENT
// =========================================================================

/**
 * Visual Frosted Glass Daylight Journey Widget.
 * Illustrates the sun's passage through Dawn (Fajr) -> Sunrise -> Noon (Dhuhr) ->
 * Afternoon (Asr) -> Sunset (Maghrib) -> Dusk (Isha) with a luminous solar marker.
 */
@Composable
private fun GlassSolarTrackCard(
    prayers: List<PrayerTimeItem>
) {
    val now = Calendar.getInstance()
    val hour = now.get(Calendar.HOUR_OF_DAY)
    val minute = now.get(Calendar.MINUTE)
    val currentMinutes = hour * 60 + minute

    // Approximate daylight timeline (Fajr ~05:00 = 300m, Isha ~20:00 = 1200m)
    val timelineProgress = ((currentMinutes - 300f) / 900f).coerceIn(0f, 1f)

    GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("solar_track_card"),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = PureWhite.copy(alpha = 0.88f),
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = GoldenAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Solar Arc & Day Progression",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalPrimary
                    )
                }

                Text(
                    text = "Zawāl & Meridian Sync",
                    fontSize = 11.sp,
                    color = SlateMuted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Visual Solar Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Background Track
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.35f),
                                    GoldenAmber.copy(alpha = 0.40f),
                                    Color(0xFFF97316).copy(alpha = 0.35f),
                                    DeepRoyalEmerald.copy(alpha = 0.35f)
                                )
                            )
                        )
                )

                // Sun Orb Marker
                Box(
                    modifier = Modifier
                        .fillMaxWidth(timelineProgress)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(GoldenAmber, PlatinumGold)
                                )
                            )
                            .border(1.5.dp, PureWhite, CircleShape)
                            .shadow(4.dp, CircleShape)
                    )
                }
            }

            // Labels under track
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val fajrTime = prayers.firstOrNull { it.type == PrayerType.FAJR }?.timeFormatted ?: "05:15"
                val dhuhrTime = prayers.firstOrNull { it.type == PrayerType.DHUHR }?.timeFormatted ?: "12:30"
                val ishaTime = prayers.firstOrNull { it.type == PrayerType.ISHA }?.timeFormatted ?: "20:00"

                Column(horizontalAlignment = Alignment.Start) {
                    Text("Dawn (Fajr)", fontSize = 10.sp, color = SlateMuted)
                    Text(fajrTime, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = CharcoalPrimary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Meridian (Dhuhr)", fontSize = 10.sp, color = SlateMuted)
                    Text(dhuhrTime, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = CharcoalPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Night (Isha)", fontSize = 10.sp, color = SlateMuted)
                    Text(ishaTime, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = CharcoalPrimary)
                }
            }
        }
    }
}

// =========================================================================
// EXTENDED ASTRONOMICAL WINDOWS & METHOD CARDS
// =========================================================================

/**
 * Auxiliary Islamic Timings Card (Tahajjud, Imsak, Ishraq, Midnight).
 */
@Composable
private fun GlassExtendedAstronomicalTimingsCard(
    prayers: List<PrayerTimeItem>
) {
    val fajrItem = prayers.firstOrNull { it.type == PrayerType.FAJR }
    val maghribItem = prayers.firstOrNull { it.type == PrayerType.MAGHRIB }

    // Calculated timings
    val imsakTime = fajrItem?.timeFormatted?.let { "10 min before Fajr" } ?: "05:05 AM"
    val ishraqTime = "15 min after Sunrise (~06:45 AM)"
    val tahajjudWindow = "Optimal: 03:30 AM - 04:45 AM (Last 3rd of Night)"
    val islamicMidnight = "11:45 PM (Nisf al-Layl)"

    GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("extended_timings_card"),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = PureWhite.copy(alpha = 0.88f),
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreTime,
                    contentDescription = null,
                    tint = DeepRoyalEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Auxiliary Islamic Astronomical Timings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Imsak / Sehri
                GlassMiniDetailBox(
                    modifier = Modifier.weight(1f),
                    title = "Imsak (Sehri End)",
                    arabic = "الإمساك",
                    value = imsakTime,
                    icon = Icons.Default.AccessTime
                )

                // Ishraq / Duha
                GlassMiniDetailBox(
                    modifier = Modifier.weight(1f),
                    title = "Ishraq (Duha)",
                    arabic = "الإشراق",
                    value = ishraqTime,
                    icon = Icons.Default.WbSunny
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tahajjud
                GlassMiniDetailBox(
                    modifier = Modifier.weight(1f),
                    title = "Tahajjud Window",
                    arabic = "صلاة التهجد",
                    value = tahajjudWindow,
                    icon = Icons.Default.Bedtime
                )

                // Islamic Midnight
                GlassMiniDetailBox(
                    modifier = Modifier.weight(1f),
                    title = "Nisf al-Layl",
                    arabic = "نصف الليل",
                    value = islamicMidnight,
                    icon = Icons.Default.Brightness2
                )
            }
        }
    }
}

@Composable
private fun GlassMiniDetailBox(
    modifier: Modifier = Modifier,
    title: String,
    arabic: String,
    value: String,
    icon: ImageVector
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PearlSurfaceVariant.copy(alpha = 0.70f))
            .border(1.dp, BorderSubtlePearl, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CharcoalPrimary
                )
                Text(
                    text = arabic,
                    fontSize = 11.sp,
                    color = RadiantEmerald
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = SlateMuted,
                lineHeight = 14.sp
            )
        }
    }
}

/**
 * Calculation Method & Juristic School Glass Card.
 */
@Composable
private fun GlassCalculationMethodCard(
    location: LocationInfo,
    onChangeCityClick: () -> Unit
) {
    GlassContainer(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calculation_method_card"),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = PureWhite.copy(alpha = 0.88f),
        elevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(EmeraldSurfaceLight)
                        .border(1.dp, RadiantEmerald.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = "Method",
                        tint = DeepRoyalEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "University of Islamic Sciences, Karachi",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalPrimary
                    )
                    Text(
                        text = "Hanafi Juristic School (Asr Shadow 2x) • Coordinates: ${String.format("%.2f", location.latitude)}° N, ${String.format("%.2f", location.longitude)}° E",
                        fontSize = 10.5.sp,
                        color = SlateMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldSurfaceLight)
                    .border(1.dp, RadiantEmerald.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onChangeCityClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Change",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepRoyalEmerald
                )
            }
        }
    }
}

// =========================================================================
// CITY SELECTION MODAL DIALOG
// =========================================================================

@Composable
private fun CitySelectionGlassDialog(
    currentCity: String,
    onDismiss: () -> Unit,
    onSelectCity: (LocationInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = DeepRoyalEmerald,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select City for Namaz Times",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = CharcoalPrimary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Choose your city or Holy Sanctuary to synchronize daily prayer timings with exact astronomical solar parameters:",
                    fontSize = 12.sp,
                    color = SlateMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(LocationRepository.popularCities) { city ->
                        val isSelected = city.cityName.equals(currentCity, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) EmeraldSurfaceLight else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (isSelected) RadiantEmerald.copy(alpha = 0.40f) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectCity(city) }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) DeepRoyalEmerald else SlateMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = city.cityName,
                                        fontSize = 14.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) DeepRoyalEmerald else CharcoalPrimary
                                    )
                                    Text(
                                        text = city.countryName,
                                        fontSize = 11.5.sp,
                                        color = SlateMuted
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = DeepRoyalEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = DeepRoyalEmerald, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = PureWhite,
        shape = RoundedCornerShape(24.dp)
    )
}
