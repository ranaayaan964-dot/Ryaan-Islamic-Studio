package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AdhanSoundPlayer
import com.example.alarm.PrayerAlarmScheduler
import com.example.data.model.CountdownState
import com.example.data.model.DailyInspiration
import com.example.data.model.LocationInfo
import com.example.data.model.PrayerTimeItem
import com.example.data.model.PrayerType
import com.example.data.repository.InspirationRepository
import com.example.data.repository.LocationRepository
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassPill
import com.example.ui.theme.DarkBorderGold
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LightBorderEmerald
import com.example.ui.theme.LightEmeraldBackground
import com.example.ui.theme.LocalThemeIsDark
import com.example.util.PrayerCalculator
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentLocation: LocationInfo,
    onLocationChange: (LocationInfo) -> Unit,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    var showCityDialog by remember { mutableStateOf(false) }

    // Alarm toggles map
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

    // Calculated prayer times
    var prayerTimes by remember(currentLocation, alarmSettings.toMap()) {
        mutableStateOf(
            PrayerCalculator.calculatePrayerTimes(
                latitude = currentLocation.latitude,
                longitude = currentLocation.longitude,
                alarmSettings = alarmSettings
            )
        )
    }

    // Live countdown state
    var countdownState by remember { mutableStateOf(CountdownState()) }

    // Audio test playing state
    var isTestingAdhan by remember { mutableStateOf(false) }

    // Daily inspiration index
    var inspirationIndex by remember { mutableIntStateOf(0) }
    val currentInspiration = InspirationRepository.inspirations[inspirationIndex % InspirationRepository.inspirations.size]

    // Live 1-second countdown ticker
    LaunchedEffect(prayerTimes) {
        while (true) {
            countdownState = PrayerCalculator.computeCountdown(prayerTimes)
            delay(1000)
        }
    }

    // Schedule active alarms initially
    LaunchedEffect(prayerTimes) {
        PrayerAlarmScheduler.scheduleAllActivePrayers(context, prayerTimes)
    }

    val isDark = LocalThemeIsDark.current
    val bgGradient = if (isDark) {
        Brush.verticalGradient(
            listOf(
                DarkNavyBackground,
                Color(0xFF0C1635),
                Color(0xFF072124),
                DarkNavyBackground
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                LightEmeraldBackground,
                Color(0xFFE8F5ED),
                Color(0xFFF0FDF4),
                LightEmeraldBackground
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
            .background(bgGradient)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Top Bar Header
            item {
                DashboardHeader(
                    cityName = currentLocation.cityName,
                    countryName = currentLocation.countryName,
                    isDarkTheme = isDarkTheme,
                    onLocationClick = { showCityDialog = true },
                    onToggleTheme = onToggleTheme
                )
            }

            // Hero Next Prayer Countdown Card
            item {
                NextPrayerHeroCard(
                    countdownState = countdownState,
                    isTestingAdhan = isTestingAdhan,
                    onToggleAdhanTest = {
                        if (isTestingAdhan) {
                            AdhanSoundPlayer.stopAdhan()
                            isTestingAdhan = false
                        } else {
                            isTestingAdhan = true
                            AdhanSoundPlayer.playAdhanAlert(context) {
                                isTestingAdhan = false
                            }
                        }
                    }
                )
            }

            // Daily Inspiration Card (Hadith / Quranic verse)
            item {
                DailyInspirationSection(
                    inspiration = currentInspiration,
                    onNextInspiration = {
                        inspirationIndex++
                    }
                )
            }

            // Daily Namaz Schedule Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Namaz Times",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date()),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Prayer Times List Cards
            items(prayerTimes) { prayer ->
                PrayerTimeCard(
                    prayer = prayer,
                    onToggleAlarm = { enabled ->
                        alarmSettings[prayer.type] = enabled
                        val updated = prayer.copy(isAlarmEnabled = enabled)
                        if (enabled) {
                            PrayerAlarmScheduler.schedulePrayerAlarm(context, updated)
                        } else {
                            PrayerAlarmScheduler.cancelPrayerAlarm(context, updated)
                        }
                    }
                )
            }
        }
    }

    // City Selection Dialog
    if (showCityDialog) {
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            title = {
                Text(
                    text = "Select City for Prayer Times",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(280.dp)) {
                    items(LocationRepository.popularCities) { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onLocationChange(city)
                                    showCityDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = city.cityName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = city.countryName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
fun DashboardHeader(
    cityName: String,
    countryName: String,
    isDarkTheme: Boolean,
    onLocationClick: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val isDark = LocalThemeIsDark.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Location Pill
        GlassPill(
            onClick = onLocationClick,
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$cityName, $countryName",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        // Theme Toggle Button
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (isDark) Color(0x33E5C07B) else Color(0x22065F46)
                )
                .border(
                    1.dp,
                    if (isDark) Color(0x55E5C07B) else Color(0x44065F46),
                    CircleShape
                )
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Dark/Light Mode",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun NextPrayerHeroCard(
    countdownState: CountdownState,
    isTestingAdhan: Boolean,
    onToggleAdhanTest: () -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val infiniteTransition = rememberInfiniteTransition(label = "HeroGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowScale"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .scale(glowScale),
        shape = RoundedCornerShape(28.dp),
        borderWidth = 1.5.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.radialGradient(
                        colors = if (isDark) {
                            listOf(Color(0x33E5C07B), Color(0x000F1D3D))
                        } else {
                            listOf(Color(0x22047857), Color(0x00FFFFFF))
                        }
                    )
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness2,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UPCOMING PRAYER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prayer Name & Arabic
                Text(
                    text = "${countdownState.nextPrayerName}  •  ${countdownState.nextPrayerArabic}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Scheduled at ${countdownState.formattedTime}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Circular Progress + Live Countdown Display
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(140.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { countdownState.progress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = if (isDark) Color(0x22E5C07B) else Color(0x22065F46),
                        strokeWidth = 6.dp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = countdownState.displayTimer,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "remaining",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Adhan Test / Preview Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isTestingAdhan) Color(0x44EF4444) else if (isDark) Color(0x22E5C07B) else Color(0x18065F46)
                        )
                        .border(
                            1.dp,
                            if (isTestingAdhan) Color(0xAAEF4444) else MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(50)
                        )
                        .clickable(onClick = onToggleAdhanTest)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isTestingAdhan) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Test Adhan Alert",
                            tint = if (isTestingAdhan) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTestingAdhan) "Stop Adhan Alert Sound" else "Preview Adhan Alert Tone",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isTestingAdhan) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyInspirationSection(
    inspiration: DailyInspiration,
    onNextInspiration: () -> Unit
) {
    val isDark = LocalThemeIsDark.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = inspiration.category.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onNextInspiration,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Next inspiration",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Arabic text
            Text(
                text = inspiration.arabicText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
                lineHeight = 26.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // English meaning
            Text(
                text = "“${inspiration.englishText}”",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                fontFamily = FontFamily.Serif
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "— ${inspiration.source}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PrayerTimeCard(
    prayer: PrayerTimeItem,
    onToggleAlarm: (Boolean) -> Unit
) {
    val isDark = LocalThemeIsDark.current
    val icon = when (prayer.type) {
        PrayerType.FAJR -> Icons.Default.WbTwilight
        PrayerType.SUNRISE -> Icons.Default.WbSunny
        PrayerType.DHUHR -> Icons.Default.WbSunny
        PrayerType.ASR -> Icons.Default.WbTwilight
        PrayerType.MAGHRIB -> Icons.Default.Brightness2
        PrayerType.ISHA -> Icons.Default.Brightness2
    }

    val cardBg = if (prayer.isNext) {
        if (isDark) Color(0x33E5C07B) else Color(0x26065F46)
    } else {
        if (isDark) Color(0x180F1D3D) else Color(0x80FFFFFF)
    }

    val borderColor = if (prayer.isNext) {
        MaterialTheme.colorScheme.primary
    } else {
        if (isDark) Color(0x1AE5C07B) else Color(0x18065F46)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Icon + Names
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (prayer.isNext) MaterialTheme.colorScheme.primary else Color(0x1AE5C07B)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = prayer.type.displayName,
                        tint = if (prayer.isNext) {
                            if (isDark) Color(0xFF1B1302) else Color.White
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = prayer.type.displayName,
                            fontSize = 15.sp,
                            fontWeight = if (prayer.isNext) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (prayer.isNext) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "NEXT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF1B1302) else Color.White
                                )
                            }
                        }
                    }
                    Text(
                        text = prayer.type.arabicName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Time + Alarm Toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = prayer.timeFormatted,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (prayer.type != PrayerType.SUNRISE) {
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = { onToggleAlarm(!prayer.isAlarmEnabled) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (prayer.isAlarmEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                            contentDescription = "Prayer Alarm Alert",
                            tint = if (prayer.isAlarmEnabled) MaterialTheme.colorScheme.primary else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
