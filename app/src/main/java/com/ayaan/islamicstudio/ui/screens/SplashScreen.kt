package com.ayaan.islamicstudio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ayaan.islamicstudio.data.database.AppDatabase
import com.ayaan.islamicstudio.data.database.UserSessionManager
import com.ayaan.islamicstudio.data.quran.database.QuranDatabase
import com.ayaan.islamicstudio.data.repository.LocationRepository
import com.ayaan.islamicstudio.data.repository.SettingsRepository
import com.ayaan.islamicstudio.data.repository.ThemeSettingsRepository
import com.ayaan.islamicstudio.security.SessionManager
import com.ayaan.islamicstudio.ui.components.LuxuryAmbientBackdrop
import com.ayaan.islamicstudio.util.PrayerCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

private val SplashGold = Color(0xFFF5D06B)
private val SplashGoldDeep = Color(0xFFD4AF37)
private val SplashEmerald = Color(0xFF0FB98A)
private val SplashIvory = Color(0xFFFFF8E8)
private const val SPLASH_DURATION_MILLIS = 2400

@Composable
fun SplashScreen(
    onNavigateNext: (isLoggedIn: Boolean) -> Unit
) {
    val context = LocalContext.current
    val todayLabel = remember {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.ENGLISH).format(Date())
    }
    val loadingProgress = remember { Animatable(0f) }
    val emblemEntrance = remember { Animatable(0.78f) }
    val infiniteTransition = rememberInfiniteTransition(label = "LuxurySplashMotion")
    val ambientPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AmbientPulse"
    )
    val latticeRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 46000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LatticeRotation"
    )
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HaloRotation"
    )
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0.34f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GoldShimmer"
    )

    LaunchedEffect(Unit) {
        launch {
            emblemEntrance.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        val preloadResult = async(Dispatchers.IO) {
            val appDb = AppDatabase.getInstance(context)
            appDb.userDao().getActiveSession()

            val quranDb = QuranDatabase.getDatabase(context)
            quranDb.quranDao().getSurahCount()

            val sessionManager = SessionManager.getInstance(context)
            val settingsRepository = SettingsRepository.getInstance(context)
            val themeRepository = ThemeSettingsRepository.getInstance(context)
            themeRepository.getTheme()

            val hasSecureSession = sessionManager.isLoggedIn()
            val hasDataStoreSession = settingsRepository.isLoggedIn()
            val hasDatabaseSession = UserSessionManager.hasActiveSession(context)
            val hasExistingSession = hasSecureSession || hasDataStoreSession || hasDatabaseSession

            if (hasExistingSession) {
                if (!hasSecureSession && hasDatabaseSession) {
                    sessionManager.setLoggedIn(true)
                }
                if (!hasDataStoreSession) {
                    settingsRepository.setLoggedIn(true)
                }
            }

            val location = LocationRepository.getDefaultLocation()
            PrayerCalculator.calculatePrayerTimes(location.latitude, location.longitude)
            hasExistingSession
        }

        loadingProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = SPLASH_DURATION_MILLIS,
                easing = FastOutSlowInEasing
            )
        )
        onNavigateNext(preloadResult.await())
    }

    LuxuryAmbientBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cinematic_splash_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithCache {
                    val center = Offset(size.width / 2f, size.height * 0.39f)
                    val backgroundBrush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF073529),
                            Color(0xFF041F18),
                            Color(0xFF031A14),
                            Color(0xFF020C09)
                        ),
                        center = center,
                        radius = size.maxDimension * 0.86f
                    )
                    val ambientGoldBrush = Brush.radialGradient(
                        colors = listOf(
                            SplashGold.copy(alpha = 0.17f),
                            SplashGoldDeep.copy(alpha = 0.055f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.minDimension * 0.44f
                    )
                    val latticeCenter = Offset(size.width * 0.81f, size.height * 0.18f)
                    val latticePath = createRubElHizbPath(
                        center = latticeCenter,
                        radius = size.minDimension * 0.31f
                    )
                    val stars = List(18) { index ->
                        CachedSplashStar(
                            center = Offset(
                                x = ((index * 47 + 13) % 97) / 97f * size.width,
                                y = (0.035f + ((index * 29 + 7) % 41) / 100f) * size.height
                            ),
                            radius = (1.15f + index % 3 * 0.65f).dp.toPx(),
                            phase = ((index * 19) % 11) / 11f
                        )
                    }

                    onDrawBehind {
                        drawRect(backgroundBrush)
                        drawCircle(
                            brush = ambientGoldBrush,
                            radius = size.minDimension * 0.44f * ambientPulse,
                            center = center
                        )
                        rotate(
                            degrees = latticeRotation,
                            pivot = latticeCenter
                        ) {
                            drawPath(
                                path = latticePath,
                                color = SplashGold.copy(alpha = 0.055f),
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                            for (ring in 0..2) {
                                drawCircle(
                                    color = SplashGold.copy(alpha = 0.024f + ring * 0.008f),
                                    radius = size.minDimension * (0.085f + ring * 0.075f),
                                    center = latticeCenter,
                                    style = Stroke(width = 0.8.dp.toPx())
                                )
                            }
                        }
                        stars.forEach { star ->
                            val starAlpha = (0.18f + shimmer * (0.35f + star.phase * 0.35f))
                                .coerceIn(0.12f, 0.72f)
                            drawCircle(
                                color = SplashGold.copy(alpha = starAlpha),
                                radius = star.radius,
                                center = star.center
                            )
                        }
                    }
                }
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                val isWideLandscape = maxWidth >= 720.dp && maxHeight <= 620.dp
                val horizontalPadding = if (isWideLandscape) 36.dp else 22.dp
                val emblemSize = when {
                    isWideLandscape -> 172.dp
                    maxHeight <= 650.dp -> 164.dp
                    maxWidth <= 360.dp -> 178.dp
                    else -> 218.dp
                }

                if (isWideLandscape) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = horizontalPadding, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(34.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SplashBrandBlock(
                                modifier = Modifier.weight(1.08f),
                                emblemSize = emblemSize,
                                entranceScale = emblemEntrance.value,
                                ambientPulse = ambientPulse,
                                haloRotation = haloRotation,
                                shimmer = shimmer,
                                todayLabel = todayLabel,
                                compact = true
                            )
                            SplashLoadingPanel(
                                modifier = Modifier.weight(0.92f),
                                progress = loadingProgress.value,
                                compact = true
                            )
                        }
                        SplashFooter()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight)
                            .padding(horizontal = horizontalPadding, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        SplashBrandBlock(
                            emblemSize = emblemSize,
                            entranceScale = emblemEntrance.value,
                            ambientPulse = ambientPulse,
                            haloRotation = haloRotation,
                            shimmer = shimmer,
                            todayLabel = todayLabel,
                            compact = maxHeight <= 720.dp || maxWidth <= 360.dp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        SplashLoadingPanel(
                            progress = loadingProgress.value,
                            compact = maxHeight <= 720.dp || maxWidth <= 360.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        SplashFooter()
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashBrandBlock(
    modifier: Modifier = Modifier,
    emblemSize: Dp,
    entranceScale: Float,
    ambientPulse: Float,
    haloRotation: Float,
    shimmer: Float,
    todayLabel: String,
    compact: Boolean
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            color = SplashGold.copy(alpha = 0.9f + shimmer * 0.1f),
            fontSize = if (compact) 19.sp else 23.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(0.88f + shimmer * 0.12f)
        )

        Spacer(modifier = Modifier.height(if (compact) 8.dp else 14.dp))

        SplashMedallion(
            size = emblemSize,
            scale = entranceScale * ambientPulse,
            haloRotation = haloRotation,
            shimmer = shimmer
        )

        Spacer(modifier = Modifier.height(if (compact) 10.dp else 18.dp))

        Text(
            text = "AYAAN ISLAMIC STUDIO",
            style = TextStyle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFF7D6),
                        SplashGold,
                        SplashGoldDeep,
                        Color(0xFFFFE89A)
                    )
                ),
                fontSize = if (compact) 20.sp else 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                letterSpacing = if (compact) 2.sp else 3.2.sp,
                textAlign = TextAlign.Center
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(9.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x22D4AF37))
                .border(1.dp, SplashGold.copy(alpha = 0.72f), RoundedCornerShape(999.dp))
                .padding(horizontal = 16.dp, vertical = 7.dp)
        ) {
            Text(
                text = "🕌 TODAY PRAYER TIME • NOBLE QURAN",
                color = SplashGold,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x25064E3B))
                .border(1.dp, SplashEmerald.copy(alpha = 0.38f), RoundedCornerShape(999.dp))
                .padding(horizontal = 13.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${todayLabel.uppercase(Locale.ENGLISH)} • PRAYER READY",
                color = SplashIvory.copy(alpha = 0.88f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SplashMedallion(
    size: Dp,
    scale: Float,
    haloRotation: Float,
    shimmer: Float
) {
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .drawWithCache {
                val center = Offset(this.size.width / 2f, this.size.height / 2f)
                val outerRadius = this.size.minDimension * 0.39f
                val medallionBrush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF12684F),
                        Color(0xFF064E3B),
                        Color(0xFF03271E)
                    ),
                    center = center,
                    radius = outerRadius
                )
                val goldRingBrush = Brush.sweepGradient(
                    colors = listOf(
                        SplashGoldDeep,
                        SplashGold,
                        Color(0xFFFFF0B2),
                        SplashEmerald,
                        SplashGoldDeep
                    ),
                    center = center
                )
                val mihrabPath = Path().apply {
                    val width = outerRadius * 0.72f
                    val height = outerRadius * 0.9f
                    moveTo(center.x - width / 2f, center.y + height * 0.46f)
                    lineTo(center.x - width / 2f, center.y - height * 0.04f)
                    quadraticBezierTo(
                        center.x - width / 2f,
                        center.y - height * 0.56f,
                        center.x,
                        center.y - height * 0.69f
                    )
                    quadraticBezierTo(
                        center.x + width / 2f,
                        center.y - height * 0.56f,
                        center.x + width / 2f,
                        center.y - height * 0.04f
                    )
                    lineTo(center.x + width / 2f, center.y + height * 0.46f)
                    close()
                }
                val dashEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(7.dp.toPx(), 6.dp.toPx())
                )

                onDrawWithContent {
                    drawCircle(
                        brush = medallionBrush,
                        radius = outerRadius,
                        center = center
                    )
                    drawCircle(
                        brush = goldRingBrush,
                        radius = outerRadius,
                        center = center,
                        style = Stroke(width = 3.2.dp.toPx())
                    )
                    drawCircle(
                        color = SplashGold.copy(alpha = 0.32f + shimmer * 0.38f),
                        radius = outerRadius * 0.77f,
                        center = center,
                        style = Stroke(width = 1.3.dp.toPx())
                    )
                    rotate(degrees = haloRotation, pivot = center) {
                        drawCircle(
                            color = SplashGold.copy(alpha = 0.68f),
                            radius = outerRadius * 1.12f,
                            center = center,
                            style = Stroke(
                                width = 1.6.dp.toPx(),
                                pathEffect = dashEffect,
                                cap = StrokeCap.Round
                            )
                        )
                    }

                    val crescentRadius = outerRadius * 0.24f
                    val crescentCenter = Offset(center.x + outerRadius * 0.27f, center.y - outerRadius * 0.47f)
                    drawCircle(
                        color = SplashGold,
                        radius = crescentRadius,
                        center = crescentCenter
                    )
                    drawCircle(
                        color = Color(0xFF064E3B),
                        radius = crescentRadius * 0.98f,
                        center = Offset(
                            x = crescentCenter.x + crescentRadius * 0.42f,
                            y = crescentCenter.y - crescentRadius * 0.12f
                        )
                    )

                    drawPath(
                        path = mihrabPath,
                        color = SplashGold.copy(alpha = 0.2f),
                    )
                    drawPath(
                        path = mihrabPath,
                        brush = goldRingBrush,
                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawCircle(
                        color = SplashGold.copy(alpha = 0.9f),
                        radius = outerRadius * 0.038f,
                        center = Offset(center.x, center.y + outerRadius * 0.09f)
                    )
                    drawLine(
                        color = SplashGold.copy(alpha = 0.8f),
                        start = Offset(center.x, center.y + outerRadius * 0.15f),
                        end = Offset(center.x, center.y + outerRadius * 0.45f),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawContent()
                }
            },
        contentAlignment = Alignment.Center
    )
}

@Composable
private fun SplashLoadingPanel(
    modifier: Modifier = Modifier,
    progress: Float,
    compact: Boolean
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val percentage = (clampedProgress * 100f).roundToInt()
    val stageLabel = loadingStage(clampedProgress)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x29000000))
            .border(1.dp, SplashGold.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
            .padding(horizontal = if (compact) 16.dp else 20.dp, vertical = if (compact) 13.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PREPARING YOUR SPIRITUAL DAY",
                color = SplashIvory.copy(alpha = 0.64f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(SplashGold.copy(alpha = 0.16f))
                    .border(1.dp, SplashGold.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$percentage%",
                    color = SplashGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(11.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                SplashGoldDeep,
                                SplashGold,
                                SplashEmerald
                            )
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedContent(
            targetState = stageLabel,
            transitionSpec = {
                fadeIn(tween(260)) togetherWith fadeOut(tween(180))
            },
            label = "SplashLoadingStage"
        ) { label ->
            Text(
                text = label,
                color = SplashIvory.copy(alpha = 0.92f),
                fontSize = if (compact) 11.sp else 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = if (compact) 15.sp else 17.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SplashFooter() {
    Text(
        text = "Version 1.1.1 • Crafted with Devotion by Ayaan Studio",
        color = SplashIvory.copy(alpha = 0.44f),
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.45.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 4.dp)
    )
}

private fun loadingStage(progress: Float): String = when {
    progress < 0.25f -> "Initializing Today's Prayer Times..."
    progress < 0.55f -> "Preparing 30-Para Noble Quran & Audio..."
    progress < 0.85f -> "Verifying Secure Cloud Session..."
    else -> "Bismillah — Welcome to Ayaan Islamic Studio"
}

private fun createRubElHizbPath(center: Offset, radius: Float): Path {
    return Path().apply {
        for (point in 0 until 16) {
            val angle = point * (2f * PI.toFloat() / 16f) - PI.toFloat() / 2f
            val pointRadius = if (point % 2 == 0) radius else radius * 0.54f
            val x = center.x + cos(angle) * pointRadius
            val y = center.y + sin(angle) * pointRadius
            if (point == 0) {
                moveTo(x, y)
            } else {
                lineTo(x, y)
            }
        }
        close()
        moveTo(center.x - radius * 0.72f, center.y - radius * 0.72f)
        lineTo(center.x + radius * 0.72f, center.y + radius * 0.72f)
        moveTo(center.x + radius * 0.72f, center.y - radius * 0.72f)
        lineTo(center.x - radius * 0.72f, center.y + radius * 0.72f)
    }
}

private data class CachedSplashStar(
    val center: Offset,
    val radius: Float,
    val phase: Float
)
