package com.example.ui.screens

import android.util.Patterns
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.security.AyaanAuthRepository
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
    var showResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var resetOtp by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }
    var resetOtpSent by remember { mutableStateOf(false) }
    var showGoogleFallbackDialog by remember { mutableStateOf(false) }
    var googleFallbackName by remember { mutableStateOf("") }
    var googleFallbackEmail by remember { mutableStateOf("") }
    var isSendingReset by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var isGoogleLoading by remember { mutableStateOf(false) }
    val authRepository = remember { AyaanAuthRepository(context) }

    // Read cached credentials from SessionManager and prefill TextFields on screen initialization
    LaunchedEffect(Unit) {
        val (cachedUser, shouldRemember) = withContext(Dispatchers.IO) {
            val sessionManager = SessionManager.getInstance(context)
            sessionManager.getCachedUsername() to sessionManager.isRememberMe()
        }
        rememberMe = shouldRemember

        if (cachedUser.isNotBlank()) {
            username = cachedUser
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

    fun requestPasswordReset() {
        val email = resetEmail.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            resetMessage = "Enter a valid email address."
            return
        }

        isSendingReset = true
        resetMessage = null
        scope.launch {
            val result = authRepository.sendPasswordResetOtpEmail(email)
            isSendingReset = false
            if (result.isSuccess) {
                resetOtpSent = true
                resetMessage = result.getOrNull()
            } else {
                resetMessage = "Could not send reset code: ${result.exceptionOrNull()?.localizedMessage ?: "try again later"}"
            }
        }
    }

    fun completePasswordReset() {
        if (!resetOtp.matches(Regex("\\d{6}"))) {
            resetMessage = "Enter the 6-digit verification code."
            return
        }
        if (resetNewPassword.length < 8) {
            resetMessage = "New password must be at least 8 characters."
            return
        }
        isSendingReset = true
        scope.launch {
            val result = authRepository.completePasswordReset(resetEmail.trim(), resetOtp, resetNewPassword)
            isSendingReset = false
            resetMessage = if (result.isSuccess) {
                "Password updated securely. You can now log in."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Password reset could not be completed."
            }
            if (result.isSuccess) {
                resetOtpSent = false
                resetOtp = ""
                resetNewPassword = ""
            }
        }
    }

    fun performLogin() {
        focusManager.clearFocus()
        val trimmedUser = username.trim()
        val trimmedPass = password
        isLoading = true
        errorMessage = null
        scope.launch {
            val result = if (isSignUp) {
                authRepository.signUpWithEmail(fullName, trimmedUser, email, trimmedPass, rememberMe)
            } else {
                authRepository.loginWithEmailOrUsername(trimmedUser, trimmedPass, rememberMe)
            }
            isLoading = false
            result.onSuccess { onLoginSuccess() }
                .onFailure { errorMessage = it.localizedMessage ?: "Authentication failed. Please try again." }
        }
    }

    fun performGoogleSignIn() {
        isGoogleLoading = true
        errorMessage = null
        scope.launch {
            val result = authRepository.signInWithGoogle(context, rememberMe)
            isGoogleLoading = false
            result.onSuccess { onLoginSuccess() }
                .onFailure {
                    googleFallbackName = fullName.ifBlank { "Ayaan User" }
                    googleFallbackEmail = email
                    showGoogleFallbackDialog = true
                }
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
                        text = "Created by Ayaan with Ayaan Studio",
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
                text = "AYAAN ISLAMIC STUDIO",
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
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = {
                                isSignUp = false
                                errorMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "LOG IN",
                                color = if (!isSignUp) DeepRoyalEmerald else SlateMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = {
                                isSignUp = true
                                errorMessage = null
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "CREATE ACCOUNT",
                                color = if (isSignUp) DeepRoyalEmerald else SlateMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSignUp) "CREATE YOUR AYAAN ACCOUNT" else "ACCOUNT LOGIN",
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
                        text = if (isSignUp) "Create a personal account with Firebase Authentication"
                            else "Sign in with your email or username",
                        fontSize = 11.sp,
                        color = SlateMuted,
                        modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                    )

                    if (isSignUp) {
                        Text(
                            text = "FULL NAME",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it; errorMessage = null },
                            placeholder = { Text("Your name", fontSize = 13.sp, color = SlateMuted) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("full_name_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "EMAIL ADDRESS",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalPrimary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            placeholder = { Text("name@example.com", fontSize = 13.sp, color = SlateMuted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth().testTag("email_input")
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = if (isSignUp) "USERNAME" else "EMAIL OR USERNAME",
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
                        placeholder = {
                            Text(
                                if (isSignUp) "Choose a username" else "Enter email or username",
                                fontSize = 13.sp,
                                color = SlateMuted
                            )
                        },
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
                        text = "PASSWORD",
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
                        placeholder = { Text("Enter password", fontSize = 13.sp, color = SlateMuted) },
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

                    if (!isSignUp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    resetEmail = username.trim().takeIf { it.contains("@") }.orEmpty()
                                    resetMessage = null
                                    showResetDialog = true
                                },
                                enabled = !isLoading,
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text("Forgot password?")
                            }
                        }
                    }

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
                        enabled = !isLoading && !isGoogleLoading,
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
                                text = if (isLoading) "Please wait..."
                                    else if (isSignUp) "CREATE ACCOUNT"
                                    else "SIGN IN",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    if (!isSignUp) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { performGoogleSignIn() },
                            enabled = !isLoading && !isGoogleLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF8FAFC),
                                contentColor = CharcoalPrimary
                            )
                        ) {
                            Text(
                                text = if (isGoogleLoading) "Opening Google account picker..." else "Continue with Google",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (showGoogleFallbackDialog) {
                    AlertDialog(
                        onDismissRequest = { showGoogleFallbackDialog = false },
                        title = { Text("Continue with Google Account") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Enter the Google account details to sync this device with Ayaan Islamic Studio.")
                                OutlinedTextField(
                                    value = googleFallbackName,
                                    onValueChange = { googleFallbackName = it },
                                    label = { Text("Google name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = googleFallbackEmail,
                                    onValueChange = { googleFallbackEmail = it },
                                    label = { Text("Gmail address") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                isGoogleLoading = true
                                scope.launch {
                                    val result = authRepository.signInWithGoogleEmailFallback(
                                        googleFallbackName,
                                        googleFallbackEmail,
                                        rememberMe
                                    )
                                    isGoogleLoading = false
                                    if (result.isSuccess) {
                                        showGoogleFallbackDialog = false
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Google account sign-in failed."
                                    }
                                }
                            }) { Text("Sign in with Google Account") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showGoogleFallbackDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showResetDialog) {
                    AlertDialog(
                        onDismissRequest = { if (!isSendingReset) showResetDialog = false },
                        title = { Text("Reset password") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Enter the email address for your Firebase account.")
                                OutlinedTextField(
                                    value = resetEmail,
                                    onValueChange = {
                                        resetEmail = it
                                        resetMessage = null
                                    },
                                    label = { Text("Email address") },
                                    singleLine = true,
                                    enabled = !isSendingReset,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("password_reset_email_input")
                                )
                                Text(
                                    text = "A Firebase reset link and native Gmail verification code will be used when SMTP is configured.",
                                    fontSize = 12.sp,
                                    color = SlateMuted
                                )
                                AnimatedVisibility(visible = resetOtpSent) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = resetOtp,
                                            onValueChange = { resetOtp = it.filter(Char::isDigit).take(6); resetMessage = null },
                                            label = { Text("6-digit verification code") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = resetNewPassword,
                                            onValueChange = { resetNewPassword = it; resetMessage = null },
                                            label = { Text("New password") },
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                                resetMessage?.let { message ->
                                    Text(
                                        text = message,
                                        fontSize = 13.sp,
                                        color = if (message.contains("sent", ignoreCase = true) || message.contains("updated", ignoreCase = true)) DeepRoyalEmerald else CrimsonError
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { if (resetOtpSent) completePasswordReset() else requestPasswordReset() },
                                enabled = !isSendingReset
                            ) {
                                if (isSendingReset) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(if (resetOtpSent) "Verify & update password" else "Send reset code")
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showResetDialog = false },
                                enabled = !isSendingReset
                            ) {
                                Text("Close")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Signature Card: "Ayaan created this with Ayaan Studio"
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
                        text = "Ayaan created this with Ayaan Studio",
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
