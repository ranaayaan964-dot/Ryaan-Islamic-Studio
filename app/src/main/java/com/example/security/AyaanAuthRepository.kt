package com.example.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import com.example.BuildConfig
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.database.AppDatabase
import com.example.data.database.UserEntity
import com.example.data.repository.SettingsRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AyaanAuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val userDao = AppDatabase.getInstance(appContext).userDao()

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
                "Username must be 3–30 characters using letters, numbers, dots, underscores or hyphens."
            }
            require(android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                "Enter a valid email address."
            }
            require(password.length >= 8) { "Password must be at least 8 characters." }
            check(userDao.getUserByUsernameOrEmail(normalizedUsername) == null) {
                "That username is already registered."
            }

            val credential = auth.createUserWithEmailAndPassword(normalizedEmail, password).await()
            val firebaseUser = credential.user ?: error("Firebase did not return the new user.")
            val uid = firebaseUser.uid
            firestore.collection("users").document(uid).set(
                mapOf(
                    "uid" to uid,
                    "fullName" to fullName.trim(),
                    "username" to normalizedUsername,
                    "email" to normalizedEmail,
                    "authProvider" to "email",
                    "passwordHash" to sha256(password),
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()

            persistUser(
                uid = uid,
                username = normalizedUsername,
                fullName = fullName.trim(),
                email = normalizedEmail,
                role = "User",
                passwordHash = sha256(password),
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
            if (isAdminUsername(normalizedIdentifier) && password == ADMIN_PASSWORD) {
                return@runCatching persistUser(
                    uid = "local_admin",
                    username = "ayaan",
                    fullName = "Ayaan Studio Administrator",
                    email = ADMIN_EMAIL,
                    role = "Administrator",
                    admin = true,
                    rememberMe = rememberMe
                )
            }

            val email = if (normalizedIdentifier.contains("@")) {
                normalizedIdentifier
            } else {
                val username = normalizedIdentifier.lowercase()
                val localAccount = userDao.getUserByUsername(username)
                localAccount?.takeIf { it.firebaseUid.isNotBlank() }?.email
                    ?: firestore.collection("users")
                        .whereEqualTo("username", username)
                        .limit(1)
                        .get()
                        .await()
                        .documents
                        .firstOrNull()
                        ?.getString("email")
                    ?: throw IllegalArgumentException("Account not found.")
            }

            val passwordHash = sha256(password)
            try {
                val credential = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = credential.user ?: error("Firebase did not return the signed-in user.")
                val document = firestore.collection("users").document(firebaseUser.uid).get().await()
                firestore.collection("users").document(firebaseUser.uid).set(
                    mapOf("passwordHash" to passwordHash, "lastLoginAt" to System.currentTimeMillis()),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
                persistUser(
                    uid = firebaseUser.uid,
                    username = document.getString("username")
                        ?: firebaseUser.email?.substringBefore("@")
                        ?: normalizedIdentifier,
                    fullName = document.getString("fullName") ?: firebaseUser.displayName ?: "Ayaan User",
                    email = firebaseUser.email ?: email,
                    role = "User",
                    passwordHash = passwordHash,
                    rememberMe = rememberMe
                )
            } catch (_: Exception) {
                val localAccount = userDao.getUserByUsernameOrEmail(normalizedIdentifier.lowercase())
                    ?: userDao.getUserByUsernameOrEmail(email.lowercase())
                check(localAccount != null && localAccount.passwordHash == passwordHash) {
                    "Email, username, or password is incorrect."
                }
                persistUser(
                    uid = localAccount.firebaseUid.ifBlank { "local_${localAccount.id}" },
                    username = localAccount.username,
                    fullName = localAccount.fullName,
                    email = localAccount.email,
                    role = localAccount.role,
                    passwordHash = passwordHash,
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
                        return@runCatching syncGoogleUser(firebaseUser.uid, firebaseUser.displayName ?: "Ayaan User", firebaseUser.email ?: error("The Google account has no email address."), rememberMe)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "CredentialManager Google Sign-In failed; trying provider fallback", e)
                    }
                }
            }

            val activity = unwrapActivity(context)
                ?: error("Google account picker is unavailable outside an Activity. Use the Google Account fallback.")
            val providerResult = auth.startActivityForSignInWithProvider(
                activity,
                OAuthProvider.newBuilder("google.com").build()
            ).await()
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

    suspend fun signInWithGoogleEmailFallback(
        fullName: String,
        googleEmail: String,
        rememberMe: Boolean = true
    ): Result<UserEntity> = runCatching {
        val normalizedName = fullName.trim().ifBlank { "Ayaan User" }
        val normalizedEmail = googleEmail.trim().lowercase()
        require(android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) { "Enter a valid Gmail address." }
        withContext(Dispatchers.IO) {
            val uid = "google_${normalizedEmail.replace("[^a-zA-Z0-9]".toRegex(), "_")}"
            syncGoogleUser(uid, normalizedName, normalizedEmail, rememberMe)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedEmail = email.trim().lowercase()
            require(android.util.Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) { "Enter a valid email address." }
            auth.sendPasswordResetEmail(normalizedEmail).await()
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
            auth.sendPasswordResetEmail(normalizedEmail).await()
            otpEmail = normalizedEmail
            otpCode = otp
            otpExpiresAt = System.currentTimeMillis() + 10 * 60 * 1000
            if (smtpSent) "Verification code sent to $normalizedEmail and Firebase reset email requested." else "Firebase reset email requested. SMTP is not configured on this device."
        }.onFailure {
            if (it is CancellationException) throw it
            Log.e(TAG, "OTP password reset request failed", it)
        }
    }

    suspend fun completePasswordReset(email: String, enteredOtp: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedEmail = email.trim().lowercase()
            require(newPassword.length >= 8) { "New password must be at least 8 characters." }
            require(otpEmail == normalizedEmail) { "Request a new verification code for this email." }
            require(System.currentTimeMillis() <= otpExpiresAt) { "The verification code has expired." }
            require(otpCode == enteredOtp.trim()) { "Invalid verification code." }

            val passwordHash = sha256(newPassword)
            val localUser = userDao.getUserByUsernameOrEmail(normalizedEmail)
                ?: userDao.getUserByUsernameOrEmail(normalizedEmail.substringBefore("@"))
            val updatedUser = localUser?.copy(passwordHash = passwordHash, loginTimestamp = System.currentTimeMillis())
            if (updatedUser != null) userDao.updateUser(updatedUser)

            val firebaseUser = auth.currentUser
            if (firebaseUser != null && firebaseUser.email?.equals(normalizedEmail, ignoreCase = true) == true) {
                firebaseUser.updatePassword(newPassword).await()
            }

            if (localUser?.firebaseUid?.isNotBlank() == true) {
                firestore.collection("users").document(localUser.firebaseUid).set(
                    mapOf("passwordHash" to passwordHash, "passwordUpdatedAt" to System.currentTimeMillis()),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
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
        val document = firestore.collection("users").document(uid).get().await()
        val username = document.getString("username") ?: "${usernameBase}_${uid.takeLast(6).lowercase()}"
        val createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()
        firestore.collection("users").document(uid).set(
            mapOf(
                "uid" to uid,
                "fullName" to fullName,
                "username" to username,
                "email" to normalizedEmail,
                "authProvider" to "google",
                "createdAt" to createdAt,
                "passwordHash" to ""
            )
        ).await()
        return persistUser(
            uid = uid,
            username = username,
            fullName = fullName,
            email = normalizedEmail,
            role = "User",
            passwordHash = "",
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

    private suspend fun persistUser(
        uid: String,
        username: String,
        fullName: String,
        email: String,
        role: String,
        passwordHash: String = "",
        admin: Boolean = false,
        rememberMe: Boolean = true
    ): UserEntity {
        val existing = userDao.getUserByFirebaseUid(uid) ?: if (admin) {
            userDao.getUserByUsername("ayaan")
                ?: userDao.getUserByUsername("ryaan")
                ?: userDao.getUserByUsername("ryan")
        } else {
            null
        }
        val entity = UserEntity(
            id = existing?.id ?: userDao.getNextUserId(),
            username = username.lowercase(),
            passwordHash = passwordHash,
            fullName = fullName,
            email = email.lowercase(),
            sessionToken = uid,
            isLoggedIn = true,
            loginTimestamp = System.currentTimeMillis(),
            role = role,
            firebaseUid = if (admin) "" else uid
        )
        userDao.logoutOtherUsers(entity.id)
        userDao.insertUser(entity)
        SessionManager.getInstance(appContext).saveCredentials(entity.username, "", rememberMe = rememberMe)
        SettingsRepository.getInstance(appContext).setLoggedIn(true, entity.username)
        return entity
    }

    private fun isAdminUsername(username: String): Boolean =
        username.trim().equals("ayaan", ignoreCase = true) ||
            username.trim().equals("ryaan", ignoreCase = true) ||
            username.trim().equals("ryan", ignoreCase = true)

    private var otpEmail: String? = null
    private var otpCode: String? = null
    private var otpExpiresAt: Long = 0L

    private fun sha256(value: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        return digest.digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val TAG = "AyaanAuthRepository"
        const val ADMIN_PASSWORD = "11221122"
        const val ADMIN_EMAIL = "ranaayaan964@gmail.com"
    }
}
