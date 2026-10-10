package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AppDatabase
import com.example.data.repository.SettingsRepository
import com.example.security.SessionManager
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.GoldSecondary
import com.example.ui.theme.LocalGlassTheme
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PRODUCTION SETTINGS & SECURITY SCREEN
 * Implements Google-style partial logout:
 * When user explicitly clicks "Logout", sets is_logged_in = false
 * BUT keeps the cached_username so next time they only have to enter their password.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glassTheme = LocalGlassTheme.current
    val sessionManager = remember { SessionManager.getInstance(context) }

    var cachedUsername by remember { mutableStateOf("") }
    var isRememberMe by remember { mutableStateOf(true) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cachedUsername = sessionManager.getCachedUsername().ifBlank { "ryaan" }
        isRememberMe = sessionManager.isRememberMe()
    }

    val handleLogoutClick: () -> Unit = {
        scope.launch {
            withContext(Dispatchers.IO) {
                // 1. EncryptedSharedPreferences: is_logged_in = false, keep cached_username
                sessionManager.logout()

                // 2. Clear Room active session
                AppDatabase.getInstance(context).userDao().logout()

                // 3. Clear DataStore logged in flag
                SettingsRepository.getInstance(context).setLoggedIn(false)
            }
            onLogout()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PearlBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PureWhite)
                        .border(1.dp, Color(0x20000000), CircleShape)
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CharcoalPrimary
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Security & Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalPrimary
                    )
                    Text(
                        text = "Session control & credential caching",
                        fontSize = 12.sp,
                        color = SlateMuted
                    )
                }
            }

            // User Profile & Authentication Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(DeepRoyalEmerald, EmeraldMint)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = PureWhite,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ryaan Usman",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CharcoalPrimary
                            )
                            Text(
                                text = "System Administrator",
                                fontSize = 12.sp,
                                color = DeepRoyalEmerald,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Logged in as @${cachedUsername.ifBlank { "ryaan" }}",
                                fontSize = 11.5.sp,
                                color = SlateMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFECFDF5))
                                .border(1.dp, Color(0x3010B981), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepRoyalEmerald
                            )
                        }
                    }
                }
            }

            // Credential Caching & Security Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SECURITY & STORAGE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = PlatinumGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keystore Encryption",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CharcoalPrimary
                            )
                            Text(
                                text = "AES-256 GCM encrypted SharedPreferences",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Active",
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Remember Credentials",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CharcoalPrimary
                            )
                            Text(
                                text = "Cache username for zero-friction re-entry",
                                fontSize = 11.sp,
                                color = SlateMuted
                            )
                        }
                        Switch(
                            checked = isRememberMe,
                            onCheckedChange = { checked ->
                                isRememberMe = checked
                                sessionManager.setRememberMe(checked)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = DeepRoyalEmerald
                            )
                        )
                    }
                }
            }

            // Prayer & Alarm Engine Status Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PRAYER ALARM ENGINE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepRoyalEmerald,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• 15-Min Pre-Reminder: High-Priority Wudu preparation alert",
                        fontSize = 12.sp,
                        color = CharcoalPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• 5-Min Pre-Audio: Sacred Quranic audio recitation alert",
                        fontSize = 12.sp,
                        color = CharcoalPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Exact Time: Full-screen Adhan with wake-lock guarantee",
                        fontSize = 12.sp,
                        color = CharcoalPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // EXPLICIT LOGOUT BUTTON
            // Google-style: sets is_logged_in = false, KEEPS cached_username
            Button(
                onClick = { showLogoutConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("settings_logout_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFEE2E2),
                    contentColor = CrimsonError
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = CrimsonError,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "LOGOUT & END SESSION",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Creator Attribution
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Created by Ryaan Usman",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PlatinumGold
                )
                Text(
                    text = "Powered by Ryaan Studio • Enterprise Pro v3.0",
                    fontSize = 10.5.sp,
                    color = SlateMuted
                )
            }
        }
    }

    // Confirmation Dialog before Logout
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = "Confirm Logout",
                    fontWeight = FontWeight.Bold,
                    color = CharcoalPrimary
                )
            },
            text = {
                Text(
                    text = "You will be logged out of the Administrator console. Your username '@${cachedUsername.ifBlank { "ryaan" }}' will remain securely saved for fast re-login.",
                    fontSize = 13.sp,
                    color = SlateMuted
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        handleLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonError,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Logout", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
