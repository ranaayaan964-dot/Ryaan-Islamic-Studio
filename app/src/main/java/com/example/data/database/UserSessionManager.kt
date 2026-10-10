package com.example.data.database

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Helper manager for handling user credentials and active session verification.
 */
object UserSessionManager {

    /**
     * Verifies whether an active user session exists in AppDatabase or EncryptedSharedPreferences.
     * Returns true if user is logged in with stored session, false otherwise.
     */
    suspend fun hasActiveSession(context: Context): Boolean = withContext(Dispatchers.IO) {
        val secureLoggedIn = com.example.security.SessionManager.getInstance(context).isLoggedIn()
        if (secureLoggedIn) return@withContext true
        val appDb = AppDatabase.getInstance(context)
        val user = appDb.userDao().getActiveSession() ?: appDb.userDao().getCurrentUser()
        user != null && user.isLoggedIn
    }

    /**
     * Retrieves the stored user credentials and session entity.
     */
    suspend fun getStoredUser(context: Context): UserEntity? = withContext(Dispatchers.IO) {
        val appDb = AppDatabase.getInstance(context)
        appDb.userDao().getCurrentUser()
    }

    /**
     * Stores user credentials and establishes an authenticated session.
     */
    suspend fun saveUserSession(
        context: Context,
        username: String,
        passwordHash: String = "",
        fullName: String = "Ryaan Usman",
        email: String = "ranaayaan964@gmail.com",
        role: String = "Administrator"
    ): UserEntity = withContext(Dispatchers.IO) {
        val appDb = AppDatabase.getInstance(context)
        val entity = UserEntity(
            id = 1L,
            username = username,
            passwordHash = passwordHash,
            fullName = fullName,
            email = email,
            sessionToken = "session_${System.currentTimeMillis()}_${username.hashCode()}",
            isLoggedIn = true,
            loginTimestamp = System.currentTimeMillis(),
            role = role
        )
        appDb.userDao().insertUser(entity)
        entity
    }

    /**
     * Clears active session credentials on logout while preserving cached username.
     */
    suspend fun logout(context: Context) = withContext(Dispatchers.IO) {
        com.example.security.SessionManager.getInstance(context).logout()
        val appDb = AppDatabase.getInstance(context)
        appDb.userDao().logout()
    }
}
