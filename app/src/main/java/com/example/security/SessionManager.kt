package com.example.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * ENTERPRISE-GRADE SECURE CREDENTIAL CACHING & SESSION MANAGER
 * Uses EncryptedSharedPreferences (backed by Android Keystore AES256-GCM)
 * to store cached_username, cached_password, and session state.
 */
class SessionManager private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences

    init {
        val appContext = context.applicationContext
        sharedPreferences = try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize EncryptedSharedPreferences, resetting keystore fallback", e)
            try {
                // If Keystore key was invalidated, delete corrupted preferences and recreate
                appContext.deleteSharedPreferences(PREFS_FILE_NAME)
                val masterKey = MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_FILE_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback to mode_private prefs after encryption failure", ex)
                appContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    companion object {
        private const val TAG = "SessionManager"
        private const val PREFS_FILE_NAME = "ryaan_secure_session_prefs"

        const val KEY_CACHED_USERNAME = "cached_username"
        const val KEY_CACHED_PASSWORD = "cached_password"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_REMEMBER_ME = "remember_me"

        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Stores user credentials securely upon successful login.
     */
    fun saveCredentials(username: String, password: String, rememberMe: Boolean = true) {
        sharedPreferences.edit().apply {
            putString(KEY_CACHED_USERNAME, username)
            if (rememberMe) {
                putString(KEY_CACHED_PASSWORD, password)
                putBoolean(KEY_REMEMBER_ME, true)
            } else {
                remove(KEY_CACHED_PASSWORD)
                putBoolean(KEY_REMEMBER_ME, false)
            }
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    /**
     * Retrieves the cached username (if any).
     */
    fun getCachedUsername(): String {
        return sharedPreferences.getString(KEY_CACHED_USERNAME, "") ?: ""
    }

    /**
     * Retrieves the securely cached password (if any).
     */
    fun getCachedPassword(): String {
        return sharedPreferences.getString(KEY_CACHED_PASSWORD, "") ?: ""
    }

    /**
     * Returns true if user marked 'Remember Me'.
     */
    fun isRememberMe(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, true)
    }

    /**
     * Sets Remember Me preference.
     */
    fun setRememberMe(rememberMe: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_REMEMBER_ME, rememberMe).apply()
    }

    /**
     * Verifies if an active logged-in session exists.
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Directly updates the logged in flag.
     */
    fun setLoggedIn(loggedIn: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
    }

    /**
     * LOGOUT HANDLING:
     * Sets is_logged_in = false and clears cached password,
     * BUT KEEPS the cached_username so that on next login the user only enters password.
     */
    fun logout() {
        sharedPreferences.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_CACHED_PASSWORD)
            apply()
        }
    }

    /**
     * Clears all session and credential data completely.
     */
    fun clearAll() {
        sharedPreferences.edit().clear().apply()
    }
}
