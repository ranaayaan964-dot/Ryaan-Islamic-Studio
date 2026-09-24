package com.example.data.quran.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.service.audio.QariId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LastReadInfo(
    val surahId: Int,
    val verseNumber: Int,
    val surahNameEnglish: String,
    val surahNameArabic: String,
    val timestamp: Long
)

enum class HifzRepeatMode(val displayName: String, val loopCount: Int) {
    OFF("Continuous", 0),
    REPEAT_1X("Repeat 1x", 1),
    REPEAT_3X("Repeat 3x", 3),
    REPEAT_5X("Repeat 5x", 5),
    REPEAT_INFINITE("Infinite Loop", -1)
}

class QuranReadingPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("quran_reading_prefs", Context.MODE_PRIVATE)

    private val _lastReadFlow = MutableStateFlow<LastReadInfo?>(loadLastRead())
    val lastReadFlow: StateFlow<LastReadInfo?> = _lastReadFlow.asStateFlow()

    private val _selectedQariFlow = MutableStateFlow(loadSelectedQari())
    val selectedQariFlow: StateFlow<QariId> = _selectedQariFlow.asStateFlow()

    private val _arabicFontSizeFlow = MutableStateFlow(prefs.getFloat(KEY_ARABIC_FONT_SIZE, 24f))
    val arabicFontSizeFlow: StateFlow<Float> = _arabicFontSizeFlow.asStateFlow()

    private val _showEnglishFlow = MutableStateFlow(prefs.getBoolean(KEY_SHOW_ENGLISH, true))
    val showEnglishFlow: StateFlow<Boolean> = _showEnglishFlow.asStateFlow()

    private val _showUrduFlow = MutableStateFlow(prefs.getBoolean(KEY_SHOW_URDU, true))
    val showUrduFlow: StateFlow<Boolean> = _showUrduFlow.asStateFlow()

    private val _repeatModeFlow = MutableStateFlow(loadRepeatMode())
    val repeatModeFlow: StateFlow<HifzRepeatMode> = _repeatModeFlow.asStateFlow()

    private val _playbackSpeedFlow = MutableStateFlow(prefs.getFloat(KEY_PLAYBACK_SPEED, 1.0f))
    val playbackSpeedFlow: StateFlow<Float> = _playbackSpeedFlow.asStateFlow()

    private val _tajweedEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_TAJWEED_ENABLED, true))
    val tajweedEnabledFlow: StateFlow<Boolean> = _tajweedEnabledFlow.asStateFlow()

    private val _dailyGoalFlow = MutableStateFlow(prefs.getInt(KEY_DAILY_GOAL, 10))
    val dailyGoalFlow: StateFlow<Int> = _dailyGoalFlow.asStateFlow()

    private val _todayVersesReadFlow = MutableStateFlow(loadTodayVersesRead())
    val todayVersesReadFlow: StateFlow<Int> = _todayVersesReadFlow.asStateFlow()

    private val _khatamVersesCompletedFlow = MutableStateFlow(prefs.getInt(KEY_KHATAM_VERSES, 45))
    val khatamVersesCompletedFlow: StateFlow<Int> = _khatamVersesCompletedFlow.asStateFlow()

    private fun loadTodayVersesRead(): Int {
        val todayStr = getTodayDateString()
        val savedDate = prefs.getString(KEY_TODAY_DATE, "")
        return if (savedDate == todayStr) {
            prefs.getInt(KEY_TODAY_VERSES, 0)
        } else {
            0
        }
    }

    private fun getTodayDateString(): String {
        val cal = java.util.Calendar.getInstance()
        return "${cal.get(java.util.Calendar.YEAR)}-${cal.get(java.util.Calendar.MONTH)}-${cal.get(java.util.Calendar.DAY_OF_MONTH)}"
    }

    fun setTajweedEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TAJWEED_ENABLED, enabled).apply()
        _tajweedEnabledFlow.value = enabled
    }

    fun setDailyGoal(goal: Int) {
        val clamped = goal.coerceIn(1, 100)
        prefs.edit().putInt(KEY_DAILY_GOAL, clamped).apply()
        _dailyGoalFlow.value = clamped
    }

    fun incrementVersesRead() {
        val todayStr = getTodayDateString()
        val savedDate = prefs.getString(KEY_TODAY_DATE, "")
        val currentToday = if (savedDate == todayStr) prefs.getInt(KEY_TODAY_VERSES, 0) else 0
        val newToday = currentToday + 1
        val newKhatam = (prefs.getInt(KEY_KHATAM_VERSES, 45) + 1).coerceAtMost(6236)

        prefs.edit()
            .putString(KEY_TODAY_DATE, todayStr)
            .putInt(KEY_TODAY_VERSES, newToday)
            .putInt(KEY_KHATAM_VERSES, newKhatam)
            .apply()

        _todayVersesReadFlow.value = newToday
        _khatamVersesCompletedFlow.value = newKhatam
    }

    private fun loadLastRead(): LastReadInfo? {
        val surahId = prefs.getInt(KEY_LAST_SURAH_ID, -1)
        if (surahId == -1) return null
        return LastReadInfo(
            surahId = surahId,
            verseNumber = prefs.getInt(KEY_LAST_VERSE_NUMBER, 1),
            surahNameEnglish = prefs.getString(KEY_LAST_SURAH_NAME_EN, "Al-Fatihah") ?: "Al-Fatihah",
            surahNameArabic = prefs.getString(KEY_LAST_SURAH_NAME_AR, "الفاتحة") ?: "الفاتحة",
            timestamp = prefs.getLong(KEY_LAST_TIMESTAMP, System.currentTimeMillis())
        )
    }

    fun saveLastRead(surahId: Int, verseNumber: Int, surahNameEnglish: String, surahNameArabic: String) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(KEY_LAST_SURAH_ID, surahId)
            .putInt(KEY_LAST_VERSE_NUMBER, verseNumber)
            .putString(KEY_LAST_SURAH_NAME_EN, surahNameEnglish)
            .putString(KEY_LAST_SURAH_NAME_AR, surahNameArabic)
            .putLong(KEY_LAST_TIMESTAMP, now)
            .apply()

        _lastReadFlow.value = LastReadInfo(
            surahId = surahId,
            verseNumber = verseNumber,
            surahNameEnglish = surahNameEnglish,
            surahNameArabic = surahNameArabic,
            timestamp = now
        )
    }

    private fun loadSelectedQari(): QariId {
        val name = prefs.getString(KEY_SELECTED_QARI, QariId.ALAFASY.name) ?: QariId.ALAFASY.name
        return try {
            QariId.valueOf(name)
        } catch (_: Exception) {
            QariId.ALAFASY
        }
    }

    fun setQari(qariId: QariId) {
        prefs.edit().putString(KEY_SELECTED_QARI, qariId.name).apply()
        _selectedQariFlow.value = qariId
    }

    fun setArabicFontSize(size: Float) {
        val clamped = size.coerceIn(16f, 38f)
        prefs.edit().putFloat(KEY_ARABIC_FONT_SIZE, clamped).apply()
        _arabicFontSizeFlow.value = clamped
    }

    fun setShowEnglish(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ENGLISH, show).apply()
        _showEnglishFlow.value = show
    }

    fun setShowUrdu(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_URDU, show).apply()
        _showUrduFlow.value = show
    }

    private fun loadRepeatMode(): HifzRepeatMode {
        val name = prefs.getString(KEY_REPEAT_MODE, HifzRepeatMode.OFF.name) ?: HifzRepeatMode.OFF.name
        return try {
            HifzRepeatMode.valueOf(name)
        } catch (_: Exception) {
            HifzRepeatMode.OFF
        }
    }

    fun setRepeatMode(mode: HifzRepeatMode) {
        prefs.edit().putString(KEY_REPEAT_MODE, mode.name).apply()
        _repeatModeFlow.value = mode
    }

    fun setPlaybackSpeed(speed: Float) {
        val validSpeed = speed.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_PLAYBACK_SPEED, validSpeed).apply()
        _playbackSpeedFlow.value = validSpeed
    }

    companion object {
        private const val KEY_LAST_SURAH_ID = "last_surah_id"
        private const val KEY_LAST_VERSE_NUMBER = "last_verse_number"
        private const val KEY_LAST_SURAH_NAME_EN = "last_surah_name_en"
        private const val KEY_LAST_SURAH_NAME_AR = "last_surah_name_ar"
        private const val KEY_LAST_TIMESTAMP = "last_timestamp"
        private const val KEY_SELECTED_QARI = "selected_qari"
        private const val KEY_ARABIC_FONT_SIZE = "arabic_font_size"
        private const val KEY_SHOW_ENGLISH = "show_english"
        private const val KEY_SHOW_URDU = "show_urdu"
        private const val KEY_REPEAT_MODE = "repeat_mode"
        private const val KEY_PLAYBACK_SPEED = "playback_speed"
        private const val KEY_TAJWEED_ENABLED = "tajweed_enabled"
        private const val KEY_DAILY_GOAL = "daily_reading_goal"
        private const val KEY_TODAY_VERSES = "today_verses_read"
        private const val KEY_TODAY_DATE = "today_verses_date"
        private const val KEY_KHATAM_VERSES = "khatam_verses_count"
    }
}
