package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.AppDatabase
import com.example.data.database.UserEntity
import com.example.security.SessionManager
import com.example.ui.components.IslamicWatermarkBackground
import com.example.ui.components.NeumorphicCard
import com.example.ui.theme.CharcoalPrimary
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepRoyalEmerald
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.PearlBackground
import com.example.ui.theme.PlatinumGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RadiantEmerald
import com.example.ui.theme.SlateMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MODULE 1: PEARL WHITE LUXURY ADMIN LOGIN & STRICT SESSION PERSISTENCE
 * Authorized Administrator Console Only (username: ryaan / password: 11221122).
 * Strictly removed any demo quick-access; built as a high-security Pro app portal.
 * Features signature branding: "Created by Ryaan Usman with Ryaan Studio"
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Read cached credentials from SessionManager and prefill TextFields on screen initialization
    LaunchedEffect(Unit) {
        val sessionManager = SessionManager.getInstance(context)
        val cachedUser = sessionManager.getCachedUsername()
        val cachedPass = sessionManager.getCachedPassword()
        rememberMe = sessionManager.isRememberMe()

        if (cachedUser.isNotBlank()) {
            username = cachedUser
        }
        if (cachedPass.isNotBlank()) {
            password = cachedPass
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LoginBrandingGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BadgeGlow"
    )

    fun performLogin() {
        focusManager.clearFocus()
        val trimmedUser = username.trim()
        val trimmedPass = password.trim()

        if (trimmedUser == "ryaan" && trimmedPass == "11221122") {
            errorMessage = null
            isLoading = true

            scope.launch {
                withContext(Dispatchers.IO) {
                    val sessionManager = SessionManager.getInstance(context)
                    sessionManager.saveCredentials(trimmedUser, trimmedPass, rememberMe)

                    val appDb = AppDatabase.getInstance(context)
                    appDb.userDao().insertUser(
                        UserEntity(
                            id = 1L,
                            username = "ryaan",
                            passwordHash = trimmedPass,
                            fullName = "Ryaan Usman (System Administrator)",
                            email = "ranaayaan964@gmail.com",
                            sessionToken = "session_${System.currentTimeMillis()}",
                            isLoggedIn = true,
                            loginTimestamp = System.currentTimeMillis()
                        )
                    )
                    com.example.data.repository.SettingsRepository.getInstance(context).setLoggedIn(true, "ryaan")
                }
                isLoading = false
                onLoginSuccess()
            }
        } else {
            errorMessage = "ACCESS DENIED: Unauthorized credentials. This portal is strictly restricted to authorized Administrator access."
        }
    }

    IslamicWatermarkBackground(
        modifier = Modifier
            .fillMaxSize()
            .testTag("login_screen")
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        backgroundColor = PearlBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top Creator Attribution Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFEF3C7).copy(alpha = glowAlpha * 0.8f),
                                PureWhite,
                                Color(0xFFFEF3C7).copy(alpha = glowAlpha * 0.8f)
                            )
                        )
                    )
                    .border(1.2.dp, PlatinumGold.copy(alpha = glowAlpha), RoundedCornerShape(20.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = PlatinumGold,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Created by Ryaan Usman with Ryaan Studio",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = DeepRoyalEmerald,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = PlatinumGold,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Sacred Medallion Emblem
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .shadow(14.dp, CircleShape, spotColor = Color(0x33064E3B))
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                PureWhite,
                                Color(0xFFF0FDF4),
                                Color(0xFFFEF3C7)
                            )
                        )
                    )
                    .border(2.dp, PlatinumGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "﷽",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepRoyalEmerald,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NOBLE PRAYER PRO",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                color = DeepRoyalEmerald,
                letterSpacing = 2.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Security Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFECFDF5))
                    .border(1.dp, Color(0x3010B981), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security",
                    tint = DeepRoyalEmerald,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RESTRICTED ACCESS • ADMIN CONSOLE ONLY",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepRoyalEmerald,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Soft Neumorphic Login Card
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 10.dp,
                cornerRadius = 24.dp,
                backgroundColor = PureWhite,
                borderColor = Color(0x35D4AF37)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ADMINISTRATOR LOGIN",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PlatinumGold,
                            letterSpacing = 1.2.sp
                        )
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = DeepRoyalEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Authorized administrator credentials required for system access",
                        fontSize = 11.sp,
                        color = SlateMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                    )

                    // Admin Username Input
                    Text(
                        text = "ADMIN USERNAME",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        placeholder = { Text("Enter admin username", fontSize = 13.sp, color = SlateMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Username",
                                tint = DeepRoyalEmerald
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepRoyalEmerald,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedLabelColor = DeepRoyalEmerald,
                            unfocusedLabelColor = SlateMuted,
                            focusedTextColor = CharcoalPrimary,
                            unfocusedTextColor = CharcoalPrimary,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Admin Password Input
                    Text(
                        text = "ADMIN PASSWORD",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        placeholder = { Text("Enter security password", fontSize = 13.sp, color = SlateMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password",
                                tint = DeepRoyalEmerald
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isPasswordVisible) "Hide" else "Show",
                                    tint = SlateMuted
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Password
                        ),
                        keyboardActions = KeyboardActions(onDone = { performLogin() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepRoyalEmerald,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedLabelColor = DeepRoyalEmerald,
                            unfocusedLabelColor = SlateMuted,
                            focusedTextColor = CharcoalPrimary,
                            unfocusedTextColor = CharcoalPrimary,
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Remember Me Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { rememberMe = !rememberMe }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = DeepRoyalEmerald,
                                uncheckedColor = SlateMuted,
                                checkmarkColor = PureWhite
                            ),
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("remember_me_checkbox")
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Remember Me",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CharcoalPrimary
                        )
                    }

                    // Error Message
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFEF2F2))
                                .border(1.dp, Color(0x33EF4444), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = CrimsonError,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = CrimsonError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Authenticate Button
                    Button(
                        onClick = { performLogin() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepRoyalEmerald,
                            contentColor = PureWhite
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PlatinumGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isLoading) "Verifying Credentials..." else "AUTHENTICATE AS ADMIN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Signature Card: "Ryaan Usman created this with Ryaan Studio"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFEF3C7).copy(alpha = glowAlpha),
                                PureWhite,
                                Color(0xFFFEF3C7).copy(alpha = glowAlpha)
                            )
                        )
                    )
                    .border(1.2.dp, PlatinumGold.copy(alpha = glowAlpha), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ryaan Usman created this with Ryaan Studio",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = DeepRoyalEmerald,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.alpha(glowAlpha)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Engineered with Sacred Craftsmanship & Modern AI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        fontStyle = FontStyle.Italic,
                        color = PlatinumGold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

