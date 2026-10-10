package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ryaan_settings_datastore")

class SettingsRepository(private val context: Context) {

    private val dataStore = context.settingsDataStore

    companion object {
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_SNOOZE_AUDIO_PATH = stringPreferencesKey("snooze_audio_path")
        val KEY_ALARM_TONE_ID = stringPreferencesKey("alarm_tone_id")
        val KEY_APP_THEME = stringPreferencesKey("app_theme")

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Module 6: DataStore Persistence (Remember Me)
     */
    val isLoggedInFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[KEY_IS_LOGGED_IN] ?: false
    }

    suspend fun setLoggedIn(loggedIn: Boolean, userName: String = "Ryaan") {
        dataStore.edit { preferences ->
            preferences[KEY_IS_LOGGED_IN] = loggedIn
            preferences[KEY_USER_NAME] = userName
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return isLoggedInFlow.first()
    }

    /**
     * Module 18: Dynamic Ayat Download & Snooze Audio Path
     */
    val snoozeAudioPathFlow: Flow<String?> = dataStore.data.map { preferences ->
        preferences[KEY_SNOOZE_AUDIO_PATH]
    }

    suspend fun setSnoozeAudioPath(filePath: String) {
        dataStore.edit { preferences ->
            preferences[KEY_SNOOZE_AUDIO_PATH] = filePath
        }
    }

    suspend fun getSnoozeAudioPath(): String? {
        return snoozeAudioPathFlow.first()
    }

    /**
     * MODULE 3: Delegated to ThemeSettingsRepository for ThemeType
     */
    val appThemeFlow: Flow<com.example.ui.theme.ThemeType> =
        ThemeSettingsRepository.getInstance(context).currentThemeFlow

    suspend fun setAppTheme(theme: com.example.ui.theme.ThemeType) {
        ThemeSettingsRepository.getInstance(context).setTheme(theme)
    }
}
