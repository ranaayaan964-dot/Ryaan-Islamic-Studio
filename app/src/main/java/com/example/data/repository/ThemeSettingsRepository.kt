package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "ryaan_theme_preferences")

/**
 * MODULE 3: PERSISTENT THEME REPOSITORY
 * Stores and reactive stream for the user's active theme selection:
 * PEARL_WHITE, MIDNIGHT_EMERALD, VELVET_SAPPHIRE.
 */
class ThemeSettingsRepository(private val context: Context) {

    private val dataStore = context.themeDataStore

    companion object {
        val KEY_THEME_TYPE = stringPreferencesKey("key_active_theme_type")

        @Volatile
        private var INSTANCE: ThemeSettingsRepository? = null

        fun getInstance(context: Context): ThemeSettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThemeSettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Reactive stream of current ThemeType. Changing this instantly re-colors the entire app!
     */
    val currentThemeFlow: Flow<ThemeType> = dataStore.data.map { preferences ->
        val rawName = preferences[KEY_THEME_TYPE]
        if (rawName != null) {
            try {
                ThemeType.valueOf(rawName)
            } catch (e: Exception) {
                // Graceful fallback for legacy names if any
                when (rawName) {
                    "LIGHT_GLASS" -> ThemeType.PEARL_WHITE
                    "DARK_GLASS" -> ThemeType.MIDNIGHT_EMERALD
                    "BROWN_GLASS" -> ThemeType.MIDNIGHT_EMERALD
                    "BLACK_GLASS" -> ThemeType.VELVET_SAPPHIRE
                    else -> ThemeType.PEARL_WHITE
                }
            }
        } else {
            ThemeType.PEARL_WHITE
        }
    }

    suspend fun setTheme(themeType: ThemeType) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_TYPE] = themeType.name
        }
    }

    suspend fun getTheme(): ThemeType {
        return currentThemeFlow.first()
    }
}
