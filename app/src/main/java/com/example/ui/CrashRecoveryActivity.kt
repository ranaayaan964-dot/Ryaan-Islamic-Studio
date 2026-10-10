package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.ui.theme.BorderGoldAccent
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CharcoalSecondary
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.GoldenAmber
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateMuted
import com.example.util.GlobalCrashHandler

/**
 * CrashRecoveryActivity
 *
 * Graceful fallback activity displayed when an unexpected uncaught exception occurs.
 * Protects user experience by preventing system "Force Close" dialogues and offering
 * one-tap recovery or cache clearance.
 */
class CrashRecoveryActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
        const val EXTRA_STACK_TRACE = "extra_stack_trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE) ?: "An unexpected state change occurred."
        val stackTrace = intent.getStringExtra(EXTRA_STACK_TRACE) ?: "No additional diagnostic info."

        setContent {
            CrashRecoveryScreen(
                errorMessage = errorMessage,
                stackTrace = stackTrace,
                onRestartApp = { restartApp(clearCache = false) },
                onClearCacheAndRestart = { restartApp(clearCache = true) }
            )
        }
    }

    private fun restartApp(clearCache: Boolean) {
        if (clearCache) {
            try {
                cacheDir.deleteRecursively()
                GlobalCrashHandler.clearCrashLogs(this)
            } catch (_: Exception) {
                // Ignore cache deletion errors
            }
        }

        val restartIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(restartIntent)
        finish()
    }
}

@Composable
fun CrashRecoveryScreen(
    errorMessage: String,
    stackTrace: String,
    onRestartApp: () -> Unit,
    onClearCacheAndRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PearlBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("crash_recovery_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Shield / Recovery Icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                DeepRoyalEmerald.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(PureWhite, PearlBackground)
                            )
                        )
                        .border(
                            BorderStroke(
                                2.dp,
                                Brush.linearGradient(
                                    listOf(PlatinumGold, DeepRoyalEmerald)
                                )
                            ),
                            CircleShape
                        )
                        .shadow(8.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Safe Recovery",
                        tint = DeepRoyalEmerald,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status Badge
            Surface(
                shape = RoundedCornerShape(50),
                color = DeepRoyalEmerald.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, DeepRoyalEmerald.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ZERO-CRASH SHIELD ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = DeepRoyalEmerald,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Session Safely Recovered",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = CharcoalPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Alhamdulillah, Ryaan Islamic Studio protected your session and intercepted an unexpected runtime delay.",
                fontSize = 14.sp,
                color = CharcoalSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Primary: Restart App
            Button(
                onClick = onRestartApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = PlatinumGold.copy(alpha = 0.5f))
                    .testTag("restart_app_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(PlatinumGold, GoldenAmber)
                            )
                        )
                        .border(
                            1.dp,
                            PureWhite.copy(alpha = 0.40f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart",
                            tint = CharcoalPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Restart Ryaan Islamic Studio",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary: Clear Cache & Restart
            OutlinedButton(
                onClick = onClearCacheAndRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("clear_cache_restart_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderGoldAccent),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = PureWhite)
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = "Clear Cache",
                    tint = DeepRoyalEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Clear Cache & Restart",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepRoyalEmerald
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle Technical Diagnostic Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDetails = !showDetails },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x3394A3B8)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = null,
                        tint = SlateMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showDetails) "Hide Technical Diagnostic" else "View Diagnostic Info",
                        fontSize = 12.sp,
                        color = SlateMuted
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (showDetails) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = SlateMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showDetails) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0x33E5C07B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Intercepted Error:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 11.sp,
                            color = Color(0xFFF87171),
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Diagnostic Trace:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PlatinumGold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stackTrace.take(800),
                            fontSize = 10.sp,
                            color = Color(0xFFCBD5E1),
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
