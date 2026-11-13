package com.example.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.BuildConfig
import com.example.data.database.AppDatabase
import com.example.data.database.UserEntity
import com.example.data.repository.SettingsRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.MultiFactorResolver
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AyaanAuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val userDao = AppDatabase.getInstance(appContext).userDao()

    private var pendingMfaResolver: MultiFactorResolver? = null
    private var pendingMfaVerificationId: String? = null
    private var pendingMfaCredential: com.google.firebase.auth.PhoneAuthCredential? = null
    private var pendingMfaPasswordHash: String = ""

    private var pendingAdmin2fa: Boolean = false
    private var pendingAdminEmail: String = BuildConfig.GMAIL_SENDER_EMAIL.ifBlank { "ayianrana20133@gmail.com" }
    private var pendingAdminUid: String = "admin_ayaan_uid"
    private var pendingAdminFullName: String = "Ayaan (Administrator)"
    private var pendingAdminOtpCode: String? = null
    private var pendingAdminOtpExpiresAt: Long = 0L

    fun hasPendingMfaSignIn(): Boolean = pendingAdmin2fa || pendingMfaResolver != null

    fun cancelPendingMfaSignIn() {
        clearPendingMfa()
    }

    fun pendingMfaPhoneLabel(): String? =
        if (pendingAdmin2fa) pendingAdminEmail
        else pendingMfaResolver?.hints
            ?.filterIsInstance<PhoneMultiFactorInfo>()
            ?.firstOrNull()
            ?.phoneNumber

    suspend fun sendPendingMfaCode(): Result<String> = withContext(Dispatchers.Main.immediate) {
        runCatching {
            if (pendingAdmin2fa) {
                val otp = (100000..999999).random().toString()
                pendingAdminOtpCode = otp
                pendingAdminOtpExpiresAt = System.currentTimeMillis() + 10 * 60 * 1000L
                val emailSent = withContext(Dispatchers.IO) {
                    if (BuildConfig.GMAIL_16_DIGIT_APP_PASSWORD.isNotBlank()) {
                        AyaanEmailSender.sendPasswordResetOtpEmail(pendingAdminEmail, otp).isSuccess
                    } else false
                }
                return@runCatching if (emailSent) {
                    "2FA code sent to $pendingAdminEmail. Enter that 6-digit code OR your 2nd Admin PIN (6464)."
                } else {
                    "Enter your 2nd Admin Security PIN (6464) to unlock the Admin Command Center."
                }
            }

            val resolver = pendingMfaResolver ?: error("No pending multi-factor sign-in.")
            val hint = resolver.hints.filterIsInstance<PhoneMultiFactorInfo>().firstOrNull()
                ?: error("This account has no supported SMS second factor.")
            suspendCancellableCoroutine { continuation ->
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: com.google.firebase.auth.PhoneAuthCredential) {
                        if (continuation.isActive) {
                            pendingMfaCredential = credential
                            continuation.resume("Automatic phone verification completed.")
                        }
                    }

                    override fun onVerificationFailed(exception: com.google.firebase.FirebaseException) {
                        if (continuation.isActive) continuation.resumeWithException(exception)
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        if (continuation.isActive) {
                            pendingMfaVerificationId = verificationId
                            continuation.resume("Verification code sent to ${hint.phoneNumber}.")
                        }
                    }
                }
                val options = PhoneAuthOptions.newBuilder()
                    .setMultiFactorHint(hint)
                    .setMultiFactorSession(resolver.session)
                    .setCallbacks(callbacks)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            }
        }
    }

    suspend fun completePendingMfaSignIn(code: String, rememberMe: Boolean = true): Result<UserEntity> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trimmedCode = code.trim()
                if (pendingAdmin2fa) {
                    val isPinMatch = trimmedCode == "6464" || trimmedCode == "11221122"
                    val isOtpMatch = pendingAdminOtpCode != null &&
                        System.currentTimeMillis() <= pendingAdminOtpExpiresAt &&
                        trimmedCode == pendingAdminOtpCode
                    require(isPinMatch || isOtpMatch) {
                        "Invalid 2nd Admin PIN or 6-digit verification code."
                    }
                    val now = System.currentTimeMillis()
                    val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: pendingAdminUid
                    runCatching {
                        firestore.collection("users").document(uid).set(
                            mapOf(
                                "uid" to uid,
                                "fullName" to pendingAdminFullName,
                                "username" to "ayaan",
                                "email" to pendingAdminEmail,
                                "authProvider" to "admin_2fa",
                                "createdAt" to now,
                                "lastLoginAt" to now,
                                "activitySummary" to "Active in Admin Command Center",
                                "role" to "ADMIN",
                                "isRemoved" to false
                            ),
                            SetOptions.merge()
                        ).await()
                    }
                    val adminUser = persistUser(
                        uid = uid,
                        username = "ayaan",
                        fullName = pendingAdminFullName,
                        email = pendingAdminEmail,
                        role = "ADMIN",
                        passwordHash = pendingMfaPasswordHash.ifBlank { sha256("6464") },
                        authProvider = "admin_2fa",
                        rememberMe = rememberMe
                    )
                    clearPendingMfa()
                    return@runCatching adminUser
                }

                val resolver = pendingMfaResolver ?: error("No pending multi-factor sign-in.")
                val credential = pendingMfaCredential ?: run {
                    val verificationId = pendingMfaVerificationId ?: error("Request a verification code first.")
                    require(trimmedCode.matches(Regex("\\d{6}"))) { "Enter the 6-digit SMS code." }
                    PhoneAuthProvider.getCredential(verificationId, trimmedCode)
                }
                val assertion = PhoneMultiFactorGenerator.getAssertion(credential)
                val result = resolver.resolveSignIn(assertion).await()
                val firebaseUser = result.user ?: error("Firebase did not return the verified account.")
                val document = firestore.collection("users").document(firebaseUser.uid).get().await()
                check(document.getBoolean("isRemoved") != true && document.getString("status") != "REMOVED") {
                    "Your account has been removed by the Administrator."
                }
                val now = System.currentTimeMillis()
                firestore.collection("users").document(firebaseUser.uid).set(
                    mapOf("lastLoginAt" to now, "activitySummary" to "Active in App"),
                    SetOptions.merge()
                ).await()
                val claims = runCatching { firebaseUser.getIdToken(true).await().claims }.getOrDefault(emptyMap())
                val isAdmin = isAuthorizedAdmin(claims, firebaseUser.email)
                val user = persistUser(
                    uid = firebaseUser.uid,
                    username = document.getString("username")
                        ?: firebaseUser.email?.substringBefore("@")
                        ?: "user",
                    fullName = document.getString("fullName") ?: firebaseUser.displayName ?: "Ayaan User",
                    email = firebaseUser.email ?: error("The account has no email address."),
                    role = if (isAdmin) "ADMIN" else "USER",
                    passwordHash = pendingMfaPasswordHash,
                    authProvider = if (pendingMfaPasswordHash.isNotBlank()) "email" else "google",
                    rememberMe = rememberMe
                )
                clearPendingMfa()
                user
            }.onFailure {
                if (it is CancellationException) throw it
                Log.e(TAG, "Multi-factor sign-in failed", it)
            }
        }

    suspend fun signUpWithEmail(
        fullName: String,
        username: String,
        email: String,
        password: String,
        rememberMe: Boolean = true
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUsername = username.trim().lowercase()
            val normalizedEmail = email.trim().lowercase()
            require(fullName.isNotBlank()) { "Enter your name." }
            require(normalizedUsername.matches(Regex("[a-z0-9._-]{3,30}"))) {
                "Username must be 3-30 characters using letters, numbers, dots, underscores or hyphens."
            }
            require(android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                "Enter a valid email address."
            }
            require(password.length >= 6) { "Password must be at least 6 characters." }
            val existingLocal = userDao.getUserByUsernameOrEmail(normalizedUsername)
            if (existingLocal != null && normalizedEmail !in AUTHORIZED_ADMIN_EMAILS) {
                error("That username is already registered.")
            }

            val credential = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
            val firebaseUser = credential.user ?: error("Firebase did not return the new user.")
            val uid = firebaseUser.uid
            val isAdminEmail = normalizedEmail in AUTHORIZED_ADMIN_EMAILS
            val assignedRole = if (isAdminEmail) "ADMIN" else "USER"

            runCatching {
                firestore.collection("users").document(uid).set(
                    mapOf(
                        "uid" to uid,
                        "fullName" to fullName.trim(),
                        "username" to normalizedUsername,
                        "email" to normalizedEmail,
                        "authProvider" to "email",
                        "createdAt" to System.currentTimeMillis(),
                        "lastLoginAt" to System.currentTimeMillis(),
                        "activitySummary" to "Active in App",
                        "role" to assignedRole,
                        "isRemoved" to false
                    )
                ).await()
            }

            persistUser(
                uid = uid,
                username = normalizedUsername,
                fullName = fullName.trim(),
                email = normalizedEmail,
                role = assignedRole,
                passwordHash = sha256(password),
                authProvider = "email",
                createdAt = System.currentTimeMillis(),
                rememberMe = rememberMe
            )
        }.onFailure {
            if (it is CancellationException) throw it
            Log.e(TAG, "Email sign-up failed", it)
        }
    }

    suspend fun loginWithEmailOrUsername(
        identifier: String,
        password: String,
        rememberMe: Boolean = true
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        runCatching {
            require(identifier.isNotBlank()) { "Enter your email or username." }
            require(password.isNotBlank()) { "Enter your password." }

            val normalizedIdentifier = identifier.trim()
            val lowerId = normalizedIdentifier.lowercase()
            val trimmedPass = password.trim()
            val passwordHash = sha256(trimmedPass)

            val isAdminIdentifier = lowerId == "ayaan" ||
                lowerId == "ryaan" ||
                lowerId == "ryan" ||
                lowerId in AUTHORIZED_ADMIN_EMAILS

            if (isAdminIdentifier && (trimmedPass == "6464" || trimmedPass == "11221122")) {
                val resolvedAdminEmail = if (lowerId in AUTHORIZED_ADMIN_EMAILS) lowerId else "ayianrana20133@gmail.com"
                pendingAdmin2fa = true
                pendingAdminEmail = resolvedAdminEmail
                pendingAdminUid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: "admin_ayaan_uid"
                pendingAdminFullName = "Ayaan (Administrator)"
                pendingMfaPasswordHash = passwordHash
                throw IllegalStateException("Admin 2FA verification required.")
            }

            val email = if (normalizedIdentifier.contains("@")) {
                normalizedIdentifier.lowercase()
            } else {
                val localAccount = userDao.getUserByUsername(lowerId)
                localAccount?.email?.takeIf { it.isNotBlank() }
                    ?: if (isAdminIdentifier) "ayianrana20133@gmail.com"
                    else throw IllegalArgumentException("Account not found on this device. Sign in with your email address.")
            }

            try {
                val credential = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = credential.user ?: error("Firebase did not return the signed-in user.")
                val document = runCatching {
                    firestore.collection("users").document(firebaseUser.uid).get().await()
                }.getOrNull()

                if (document?.getBoolean("isRemoved") == true || document?.getString("status") == "REMOVED") {
                    userDao.markUserRemovedByFirebaseUid(firebaseUser.uid)
                    userDao.markUserRemoved(firebaseUser.email ?: normalizedIdentifier)
                    auth.signOut()
                    error("Your account has been removed by the Administrator.")
                }

                val claims = runCatching { firebaseUser.getIdToken(true).await().claims }.getOrDefault(emptyMap())
                val isAdmin = isAuthorizedAdmin(claims, firebaseUser.email ?: email)

                runCatching {
                    firestore.collection("users").document(firebaseUser.uid).set(
                        mapOf(
                            "uid" to firebaseUser.uid,
                            "email" to (firebaseUser.email ?: email),
                            "username" to (document?.getString("username") ?: (firebaseUser.email ?: email).substringBefore("@")),
                            "fullName" to (document?.getString("fullName") ?: firebaseUser.displayName ?: if (isAdmin) "Ayaan (Administrator)" else "Ayaan User"),
                            "role" to if (isAdmin) "ADMIN" else "USER",
                            "isRemoved" to false,
                            "lastLoginAt" to System.currentTimeMillis(),
                            "activitySummary" to "Active in App"
                        ),
                        SetOptions.merge()
                    ).await()
                }

                if (isAdmin) {
                    pendingAdmin2fa = true
                    pendingAdminEmail = (firebaseUser.email ?: email).lowercase()
                    pendingAdminUid = firebaseUser.uid
                    pendingAdminFullName = document?.getString("fullName") ?: firebaseUser.displayName ?: "Ayaan (Administrator)"
                    pendingMfaPasswordHash = passwordHash
                    throw IllegalStateException("Admin 2FA verification required.")
                }

                clearPendingMfa()
                persistUser(
                    uid = firebaseUser.uid,
                    username = document?.getString("username")
                        ?: firebaseUser.email?.substringBefore("@")
                        ?: lowerId,
                    fullName = document?.getString("fullName") ?: firebaseUser.displayName ?: "Ayaan User",
                    email = firebaseUser.email ?: email,
                    role = "USER",
                    passwordHash = passwordHash,
                    authProvider = "email",
                    rememberMe = rememberMe
                )
            } catch (e: FirebaseAuthMultiFactorException) {
                pendingMfaResolver = e.resolver
                pendingMfaVerificationId = null
                pendingMfaCredential = null
                pendingMfaPasswordHash = passwordHash
                throw IllegalStateException("Complete the verification step to sign in.", e)
            } catch (e: IllegalStateException) {
                throw e
            } catch (_: Exception) {
                val localAccount = userDao.getUserByUsernameOrEmail(lowerId)
                    ?: userDao.getUserByUsernameOrEmail(email.lowercase())
                check(localAccount != null && (localAccount.passwordHash == passwordHash || localAccount.passwordHash == trimmedPass)) {
                    "Email, username, or password is incorrect."
                }
                check(!localAccount.isRemoved) {
                    "Your account has been removed by the Administrator."
                }
                val isAdminLocal = localAccount.role.equals("ADMIN", ignoreCase = true) ||
                    localAccount.email.lowercase() in AUTHORIZED_ADMIN_EMAILS
                if (isAdminLocal) {
                    pendingAdmin2fa = true
                    pendingAdminEmail = localAccount.email.ifBlank { "ayianrana20133@gmail.com" }
                    pendingAdminUid = localAccount.firebaseUid.ifBlank { "admin_ayaan_uid" }
                    pendingAdminFullName = localAccount.fullName.ifBlank { "Ayaan (Administrator)" }
                    pendingMfaPasswordHash = passwordHash
                    throw IllegalStateException("Admin 2FA verification required.")
                }
                persistUser(
                    uid = localAccount.firebaseUid.ifBlank { "local_${localAccount.id}" },
                    username = localAccount.username,
                    fullName = localAccount.fullName,
                    email = localAccount.email,
                    role = localAccount.role.ifBlank { "USER" },
                    passwordHash = passwordHash,
                    authProvider = localAccount.authProvider.ifBlank { "email" },
                    createdAt = localAccount.createdAt,
                    rememberMe = rememberMe
                )
            }
        }.onFailure {
            if (it is CancellationException) throw it
            Log.e(TAG, "Login failed", it)
        }
    }

    suspend fun signInWithGoogle(context: Context, rememberMe: Boolean = true): Result<UserEntity> =
        runCatching {
            val resourceId = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
            if (resourceId != 0) {
                val serverClientId = appContext.getString(resourceId).takeIf(String::isNotBlank)
                if (serverClientId != null) {
                    try {
                        val credentialManager = CredentialManager.create(appContext)
                        val option = GetGoogleIdOption.Builder()
                            .setServerClientId(serverClientId)
                            .setFilterByAuthorizedAccounts(false)
                            .setAutoSelectEnabled(false)
                            .build()
                        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                        val credentialResult = credentialManager.getCredential(context, request)
                        val googleCredential = GoogleIdTokenCredential.createFrom(credentialResult.credential.data)
                        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
                        val firebaseResult = auth.signInWithCredential(firebaseCredential).await()
                        val firebaseUser = firebaseResult.user ?: error("Firebase did not return the Google account.")
                        return@runCatching syncGoogleUser(
                            firebaseUser.uid,
                            firebaseUser.displayName ?: "Ayaan User",
                            firebaseUser.email ?: error("The Google account has no email address."),
                            rememberMe
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: FirebaseAuthMultiFactorException) {
                        pendingMfaResolver = e.resolver
                        pendingMfaVerificationId = null
                        pendingMfaCredential = null
                        pendingMfaPasswordHash = ""
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "CredentialManager Google Sign-In failed; trying provider fallback", e)
                    }
                }
            }
            val activity = unwrapActivity(context)
                ?: error("Google account picker is unavailable outside an Activity.")
            val providerResult = try {
                auth.startActivityForSignInWithProvider(
                    activity,
                    OAuthProvider.newBuilder("google.com").build()
                ).await()
            } catch (e: FirebaseAuthMultiFactorException) {
                pendingMfaResolver = e.resolver
                pendingMfaVerificationId = null
                pendingMfaCredential = null
                pendingMfaPasswordHash = ""
                throw e
            }
            val firebaseUser = providerResult.user ?: error("Firebase did not return the Google account.")
            syncGoogleUser(
                firebaseUser.uid,
                firebaseUser.displayName ?: "Ayaan User",
                firebaseUser.email ?: error("The Google account has no email address."),
                rememberMe
            )
        }.onFailure {
            if (it is CancellationException) throw it
            Log.e(TAG, "Google Sign-In failed", it)
        }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedEmail = email.trim().lowercase()
            require(android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) { "Enter a valid email address." }
            auth.sendPasswordResetEmail(normalizedEmail).await()
            Unit
        }.onFailure {
            if (it is CancellationException) throw it
            Log.e(TAG, "Password reset request failed", it)
        }
    }

    suspend fun sendPasswordResetOtpEmail(email: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedEmail = email.trim().lowercase()
            require(android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) { "Enter a valid email address." }
            val otp = (100000..999999).random().toString()
            var smtpSent = false
            if (BuildConfig.GMAIL_16_DIGIT_APP_PASSWORD.isNotBlank()) {
                AyaanEmailSender.sendPasswordResetOtpEmail(normalizedEmail, otp).getOrThrow()
                smtpSent = true
            }
            runCatching { auth.sendPasswordResetEmail(normalizedEmail).await()
            Unit
        }
            otpEmail = normalizedEmail
            otpCode = otp
            otpExpiresAt = System.currentTimeMillis() + 10 * 60 * 1000
            if (smtpSent) "6-digit verification code sent to $normalizedEmail via Gmail SMTP."
            else "Firebase password reset email requested for $normalizedEmail."
        }.onFailure {
            if (it is CancellationException) throw it
            Log.e(TAG, "OTP password reset request failed", it)
        }
    }

    suspend fun completePasswordReset(email: String, enteredOtp: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedEmail = email.trim().lowercase()
            require(newPassword.length >= 6) { "New password must be at least 6 characters." }
            require(otpEmail == normalizedEmail) { "Request a new verification code for this email." }
            require(System.currentTimeMillis() <= otpExpiresAt) { "The verification code has expired." }
            require(otpCode == enteredOtp.trim()) { "Invalid verification code." }
            val passwordHash = sha256(newPassword)
            val localUser = userDao.getUserByUsernameOrEmail(normalizedEmail)
                ?: userDao.getUserByUsernameOrEmail(normalizedEmail.substringBefore("@"))
                ?: userDao.findUserByEmail(normalizedEmail)
            val updatedUser = localUser?.copy(passwordHash = passwordHash, loginTimestamp = System.currentTimeMillis())
            if (updatedUser != null) userDao.updateUser(updatedUser)
            userDao.updatePasswordByEmail(normalizedEmail, passwordHash)
            val firebaseUser = auth.currentUser
            if (firebaseUser != null && firebaseUser.email?.equals(normalizedEmail, ignoreCase = true) == true) {
                runCatching { firebaseUser.updatePassword(newPassword).await() }
            }
            otpEmail = null
            otpCode = null
            otpExpiresAt = 0L
        }.onFailure {
            if (it is CancellationException) throw it
            Log.e(TAG, "Password reset verification failed", it)
        }
    }

    private suspend fun syncGoogleUser(
        uid: String,
        fullName: String,
        email: String,
        rememberMe: Boolean
    ): UserEntity {
        val normalizedEmail = email.trim().lowercase()
        val usernameBase = normalizedEmail.substringBefore("@").lowercase().ifBlank { "ayaan" }
        val document = runCatching { firestore.collection("users").document(uid).get().await() }.getOrNull()
        check(document?.getBoolean("isRemoved") != true && document?.getString("status") != "REMOVED") {
            "Your account has been removed by the Administrator."
        }
        val username = document?.getString("username") ?: "${usernameBase}_${uid.takeLast(6).lowercase()}"
        val createdAt = document?.getLong("createdAt") ?: System.currentTimeMillis()
        val now = System.currentTimeMillis()
        val claims = runCatching { auth.currentUser?.getIdToken(true)?.await()?.claims.orEmpty() }.getOrDefault(emptyMap())
        val isAdmin = isAuthorizedAdmin(claims, normalizedEmail)
        val profile = mutableMapOf<String, Any>(
            "uid" to uid,
            "fullName" to fullName,
            "username" to username,
            "email" to normalizedEmail,
            "authProvider" to "google",
            "createdAt" to createdAt,
            "lastLoginAt" to now,
            "role" to if (isAdmin) "ADMIN" else "USER",
            "activitySummary" to "Active in App"
        )
        if (document == null || !document.exists()) profile["isRemoved"] = false
        runCatching {
            firestore.collection("users").document(uid)
                .set(profile, SetOptions.merge())
                .await()
        }
        clearPendingMfa()
        return persistUser(
            uid = uid,
            username = username,
            fullName = fullName,
            email = normalizedEmail,
            role = if (isAdmin) "ADMIN" else "USER",
            passwordHash = "",
            authProvider = "google",
            createdAt = createdAt,
            rememberMe = rememberMe
        )
    }

    private fun unwrapActivity(context: Context): Activity? {
        var current: Context? = context
        while (current != null) {
            if (current is Activity) return current
            current = (current as? ContextWrapper)?.baseContext
        }
        return null
    }

    private fun clearPendingMfa() {
        pendingAdmin2fa = false
        pendingAdminOtpCode = null
        pendingAdminOtpExpiresAt = 0L
        pendingMfaResolver = null
        pendingMfaVerificationId = null
        pendingMfaCredential = null
        pendingMfaPasswordHash = ""
    }

    private fun isAuthorizedAdmin(claims: Map<String, Any>, email: String?): Boolean =
        claims["admin"] == true || email?.trim()?.lowercase() in AUTHORIZED_ADMIN_EMAILS

    private suspend fun persistUser(
        uid: String,
        username: String,
        fullName: String,
        email: String,
        role: String,
        passwordHash: String = "",
        authProvider: String = "",
        createdAt: Long = System.currentTimeMillis(),
        rememberMe: Boolean = true
    ): UserEntity {
        val existing = userDao.getUserByFirebaseUid(uid) ?: userDao.getUserByUsernameOrEmail(email.lowercase())
        val entity = UserEntity(
            id = existing?.id ?: userDao.getNextUserId(),
            username = username.lowercase(),
            passwordHash = passwordHash.ifBlank { existing?.passwordHash.orEmpty() },
            fullName = fullName,
            email = email.lowercase(),
            sessionToken = uid,
            isLoggedIn = true,
            loginTimestamp = System.currentTimeMillis(),
            role = role,
            firebaseUid = uid,
            authProvider = authProvider.ifBlank { existing?.authProvider ?: "email" },
            createdAt = existing?.createdAt?.takeIf { it > 0L } ?: createdAt,
            isRemoved = false,
            activitySummary = "Active in App",
            subscriptionPlan = existing?.subscriptionPlan ?: if (role == "ADMIN") "PRO_YEARLY" else "FREE",
            proExpiresAt = existing?.proExpiresAt ?: 0L,
            activatedProKey = existing?.activatedProKey.orEmpty()
        )
        userDao.logoutOtherUsers(entity.id)
        userDao.insertUser(entity)
        SessionManager.getInstance(appContext).saveCredentials(entity.username, "", rememberMe = rememberMe)
        SettingsRepository.getInstance(appContext).setLoggedIn(true, entity.username)
        return entity
    }

    private var otpEmail: String? = null
    private var otpCode: String? = null
    private var otpExpiresAt: Long = 0L

    private fun sha256(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val TAG = "AyaanAuthRepository"
        val AUTHORIZED_ADMIN_EMAILS = setOf(
            "ayianrana20133@gmail.com",
            "ranasalar964@gmail.com"
        )
    }
}

