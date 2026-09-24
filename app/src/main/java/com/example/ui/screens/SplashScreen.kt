package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AppDatabase
import com.example.data.database.UserSessionManager
import com.example.data.quran.database.QuranDatabase
import com.example.data.repository.LocationRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.ThemeSettingsRepository
import com.example.util.PrayerCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * MODULE 1: CINEMATIC LUXURY SPLASH SCREEN & CREATOR BRANDING
 * - Deep luxury pulsing radial gradient
 * - Center Emblem: Glowing vector Kaaba & Crescent with particle halo
 * - App Title: "RYAAN ISLAMIC STUDIO" with metallic gold text styling
 * - Creator Signature: "Created by Ryaan Usman" & "Powered by Ryaan Studio"
 *   with staggered 1.2s fade-in & floating glow
 * - Exactly 5000ms - 6000ms preloading window initializing Room, DataStore & Prayer times
 */
@Composable
fun SplashScreen(
    onNavigateNext: (isLoggedIn: Boolean) -> Unit
) {
    val context = LocalContext.current
    var isUserLoggedIn by remember { mutableStateOf(false) }
    var preloadStatus by remember { mutableStateOf("Initializing Sacred Architecture...") }
    var signatureVisible by remember { mutableStateOf(false) }

    // Floating glow animation for Creator Signature
    val signatureFloatGlow = remember { Animatable(0f) }

    // Preload & Exact Duration (5500ms total window)
    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()

        // Asynchronously activate creator signature at 1.2 seconds
        withContext(Dispatchers.Default) {
            delay(1200)
            signatureVisible = true
            signatureFloatGlow.animateTo(
                targetValue = 1f,
                animationSpec = tween(1400, easing = FastOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()

        withContext(Dispatchers.IO) {
            // 1. Initialize Room Database & Quran Engine
            preloadStatus = "Warming up Room Database & Quranic Corpus..."
            val appDb = AppDatabase.getInstance(context)
            val sessionUser = appDb.userDao().getActiveSession()

            val quranDb = QuranDatabase.getDatabase(context)
            quranDb.quranDao().getSurahCount()

            // 2. Warm up SharedPreferences / DataStore session & EncryptedSharedPreferences
            preloadStatus = "Verifying Authentication & Saved Credentials..."
            val sessionManager = com.example.security.SessionManager.getInstance(context)
            val settingsRepo = SettingsRepository.getInstance(context)
            val themeRepo = ThemeSettingsRepository.getInstance(context)
            themeRepo.getTheme()

            val hasSecureSession = sessionManager.isLoggedIn()
            val hasDataStoreSession = settingsRepo.isLoggedIn()
            val hasDbSession = UserSessionManager.hasActiveSession(context)
            isUserLoggedIn = hasSecureSession || hasDataStoreSession || hasDbSession

            if (isUserLoggedIn) {
                if (!hasSecureSession && hasDbSession) {
                    sessionManager.setLoggedIn(true)
                }
                if (!hasDataStoreSession) {
                    settingsRepo.setLoggedIn(true)
                }
            }

            // 3. Fetch or cache prayer times for today
            preloadStatus = "Synchronizing Daily Prayer Times & Qibla Vector..."
            val defaultLoc = LocationRepository.getDefaultLocation()
            PrayerCalculator.calculatePrayerTimes(defaultLoc.latitude, defaultLoc.longitude)

            preloadStatus = "Ryaan Islamic Studio Ready"
        }

        // Enforce exact 5500ms duration (within 5000ms - 6000ms requirement)
        val elapsed = System.currentTimeMillis() - startTime
        val remainingDelay = (5500L - elapsed).coerceIn(0L, 6000L)
        delay(remainingDelay)

        // Seamless transition destroying splash from backstack
        onNavigateNext(isUserLoggedIn)
    }

    // Infinite transitions for pulsing radial gradient and particle halo
    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfinite")

    // Radial gradient pulse
    val bgPulseRadius by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RadialPulse"
    )

    // Center emblem breathing scale
    val emblemScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "EmblemScale"
    )

    // Shimmering particle halo rotation & shimmer phase
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HaloRotation"
    )

    val shimmerGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ShimmerGlow"
    )

    // Deep Luxury Radial Background Canvas
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("cinematic_splash_screen")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            val maxDimension = maxOf(size.width, size.height)
            val gradientRadius = maxDimension * bgPulseRadius

            // Luxury radial gradient pulsing from deep obsidian-emerald core to abyss
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0A1C16), // Deep Emerald Heart
                        Color(0xFF06120E), // Obsidian Emerald
                        Color(0xFF030706), // Deep Obsidian Abyss
                        Color(0xFF020403)  // Pure Dark Canvas
                    ),
                    center = center,
                    radius = gradientRadius
                )
            )
        }

        // Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Subtle Brand Pill
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color(0x2010B981))
                    .border(1.dp, Color(0x40D4AF37), RoundedCornerShape(30.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "NEXT-GEN ISLAMIC ECOSYSTEM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFBBF24),
                    letterSpacing = 2.sp
                )
            }

            // Center Hero: Emblem, Particle Halo & Metallic Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Vector Kaaba & Crescent Emblem with Particle Halo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(230.dp)
                        .scale(emblemScale)
                ) {
                    // Compose Canvas for Particle Halo & Sacred Kaaba / Crescent Vector
                    Canvas(modifier = Modifier.size(230.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outerRadius = size.width * 0.46f

                        // 1. Particle Halo: 24 glowing shimmering particles orbiting the center
                        val particleCount = 24
                        for (i in 0 until particleCount) {
                            val angleDeg = (i * (360f / particleCount)) + haloRotation
                            val angleRad = angleDeg * (PI.toFloat() / 180f)
                            val distance = outerRadius * (0.78f + 0.22f * sin(angleRad * 2f + shimmerGlow))
                            val px = center.x + distance * cos(angleRad)
                            val py = center.y + distance * sin(angleRad)
                            val particleRadius = if (i % 3 == 0) 3.5f else 2.2f
                            val particleAlpha = ((0.35f + 0.65f * sin(angleRad + shimmerGlow * 3f)).coerceIn(0.2f, 1f))

                            drawCircle(
                                color = if (i % 2 == 0) Color(0xFFD4AF37).copy(alpha = particleAlpha)
                                        else Color(0xFF10B981).copy(alpha = particleAlpha),
                                radius = particleRadius,
                                center = Offset(px, py)
                            )
                        }

                        // 2. Concentric Orbit Rings
                        drawCircle(
                            color = Color(0x30D4AF37),
                            radius = outerRadius * 0.88f,
                            center = center,
                            style = Stroke(width = 1.2f, cap = StrokeCap.Round)
                        )
                        drawCircle(
                            color = Color(0x2010B981),
                            radius = outerRadius * 0.72f,
                            center = center,
                            style = Stroke(width = 1.0f)
                        )

                        // 3. Glowing Crescent Moon Vector
                        val crescentPath = Path().apply {
                            val cr = outerRadius * 0.58f
                            // Outer arc
                            addArc(
                                oval = androidx.compose.ui.geometry.Rect(
                                    center.x - cr,
                                    center.y - cr,
                                    center.x + cr,
                                    center.y + cr
                                ),
                                startAngleDegrees = 40f,
                                sweepAngleDegrees = 280f
                            )
                        }
                        drawPath(
                            path = crescentPath,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFD4AF37),
                                    Color(0xFFFBBF24),
                                    Color(0xFF10B981),
                                    Color(0xFFD4AF37)
                                ),
                                center = center
                            ),
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // 4. Center Kaaba Cubic Emblem
                        val boxSize = outerRadius * 0.48f
                        val kLeft = center.x - boxSize / 2f
                        val kTop = center.y - boxSize / 2f + 4f

                        // Kaaba Base Black Cube
                        drawRoundRect(
                            color = Color(0xFF0F1714),
                            topLeft = Offset(kLeft, kTop),
                            size = Size(boxSize, boxSize),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        // Kaaba Outer Border
                        drawRoundRect(
                            color = Color(0xFFD4AF37),
                            topLeft = Offset(kLeft, kTop),
                            size = Size(boxSize, boxSize),
                            cornerRadius = CornerRadius(8f, 8f),
                            style = Stroke(width = 1.8f)
                        )

                        // Gold Kiswah Belt (Hizam)
                        val beltY = kTop + boxSize * 0.22f
                        val beltHeight = boxSize * 0.14f
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFB45309),
                                    Color(0xFFFBBF24),
                                    Color(0xFFD4AF37),
                                    Color(0xFFFBBF24)
                                )
                            ),
                            topLeft = Offset(kLeft + 2f, beltY),
                            size = Size(boxSize - 4f, beltHeight)
                        )

                        // Gold Door of Kaaba (Bab al-Kaaba)
                        val doorWidth = boxSize * 0.22f
                        val doorHeight = boxSize * 0.38f
                        val doorX = kLeft + boxSize * 0.62f
                        val doorY = kTop + boxSize * 0.52f
                        drawRoundRect(
                            color = Color(0xFFFBBF24),
                            topLeft = Offset(doorX, doorY),
                            size = Size(doorWidth, doorHeight),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }

                    // Sacred Arabic Calligraphy center aura
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0x20D4AF37)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ﷲ",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFEF08A),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // APP TITLE: "RYAAN ISLAMIC STUDIO" with Metallic Gold Text Styling
                val metallicGoldBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFEF3C7),
                        Color(0xFFFBBF24),
                        Color(0xFFD4AF37),
                        Color(0xFFF59E0B),
                        Color(0xFFFEF08A)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(400f, 100f)
                )

                Text(
                    text = "RYAAN ISLAMIC STUDIO",
                    style = TextStyle(
                        brush = metallicGoldBrush,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 4.sp,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "THE ULTIMATE SPIRITUAL SUPER APP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981),
                    letterSpacing = 3.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Preload status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x33000000))
                        .border(1.dp, Color(0x3010B981), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = preloadStatus,
                        fontSize = 11.sp,
                        color = Color(0xFFE2E8F0),
                        maxLines = 1
                    )
                }
            }

            // CREATOR SIGNATURE (CRITICAL BRANDING)
            // Anchored precisely at bottom with safe-area padding
            // "Created by Ryaan Usman" (16.sp, FontWeight.SemiBold, glowing serif)
            // "Powered by Ryaan Studio" (12.sp, letterSpacing: 2.sp, metallic subtext)
            // Staggered fade-in & floating glow activated after 1.2 seconds
            AnimatedVisibility(
                visible = signatureVisible,
                enter = fadeIn(animationSpec = tween(900, easing = FastOutSlowInEasing)) +
                        slideInVertically(
                            initialOffsetY = { 40 },
                            animationSpec = tween(900, easing = FastOutSlowInEasing)
                        )
            ) {
                val glowAlpha = (0.55f + 0.45f * signatureFloatGlow.value * shimmerGlow)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("creator_signature_branding")
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(
                                elevation = (12 * signatureFloatGlow.value).dp,
                                shape = RoundedCornerShape(22.dp),
                                ambientColor = Color(0x40D4AF37),
                                spotColor = Color(0x5010B981)
                            )
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0x350F1F1C),
                                        Color(0x50050B0A)
                                    )
                                )
                            )
                            .border(
                                1.2.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0x4010B981),
                                        Color(0xFFD4AF37).copy(alpha = glowAlpha),
                                        Color(0x4010B981)
                                    )
                                ),
                                RoundedCornerShape(22.dp)
                            )
                            .padding(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Created by Ryaan Usman",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Serif,
                                color = Color(0xFFFEF3C7),
                                textAlign = TextAlign.Center,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.alpha(glowAlpha)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "POWERED BY RYAAN STUDIO",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFBBF24),
                                textAlign = TextAlign.Center,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
