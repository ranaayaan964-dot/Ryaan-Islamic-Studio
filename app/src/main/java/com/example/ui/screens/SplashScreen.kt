package com.example.ui.screens

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.DarkNavySurfaceVariant
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit
) {
    // Navigate after 3.5 seconds or immediately on click
    LaunchedEffect(Unit) {
        delay(3500)
        onNavigateToLogin()
    }

    // Glowing animation transitions
    val infiniteTransition = rememberInfiniteTransition(label = "SplashAnimations")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("splash_screen")
            .clickable { onNavigateToLogin() }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkNavyBackground,
                        Color(0xFF0C1736),
                        Color(0xFF042F2E),
                        DarkNavyBackground
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative glowing halo in background
        Box(
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
                .alpha(glowAlpha * 0.3f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(GoldAccent, Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Emblem container
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                DarkNavySurfaceVariant,
                                Color(0xFF1E293B)
                            )
                        )
                    )
                    .border(2.dp, Brush.linearGradient(listOf(GoldPrimary, GoldAccent)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Crescent & Star iconography
                Icon(
                    imageVector = Icons.Default.Brightness2,
                    contentDescription = "Crescent Moon",
                    tint = GoldPrimary,
                    modifier = Modifier.size(54.dp)
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Star",
                    tint = GoldAccent,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(bottom = 18.dp, start = 18.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Islamic Greeting & Subheading
            Text(
                text = "بِسْمِ ٱللَّٰهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = GoldSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent required text: "ryaan created this with ryaan studio"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x22E5C07B))
                    .border(1.dp, Color(0x55E5C07B), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "ryaan created this with ryaan studio",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Serif,
                    color = GoldAccent,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Prayer Times & Islamic Utilities",
                fontSize = 14.sp,
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.Normal,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Sleek circular progress indicator
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(44.dp),
                    color = GoldPrimary,
                    trackColor = Color(0x33E5C07B),
                    strokeWidth = 3.5.dp
                )
            }
        }
    }
}
