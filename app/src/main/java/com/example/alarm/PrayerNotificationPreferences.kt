package com.example.alarm

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

enum class PrayerMessageStyle(val id: String, val title: String, val previewText: String) {
    TRADITIONAL(
        id = "traditional",
        title = "Traditional Adhan",
        previewText = "حي على الصلاة • حي على الفلاح — Come to Prayer, Come to Success"
    ),
    QURANIC_4_103(
        id = "quranic_4_103",
        title = "Quranic Decree (Surah An-Nisa 4:103)",
        previewText = "Indeed, prayer has been decreed upon the believers a decree of specified times."
    ),
    SPIRITUAL_SERENITY(
        id = "spiritual_serenity",
        title = "Spiritual Serenity & Heart Peace",
        previewText = "Pause the dunya, purify your soul, and stand in humble remembrance before Allah."
    ),
    HADITH_FIRST_DEED(
        id = "hadith_first_deed",
        title = "Hadith (Accountability of Prayer)",
        previewText = "The first deed for which a servant will be held accountable on the Day of Judgment is prayer."
    ),
    CUSTOM(
        id = "custom",
        title = "Personalized Spiritual Note",
        previewText = "Custom reminder text entered by user"
    )
}

enum class PreReminderMessageStyle(val id: String, val title: String, val previewText: String) {
    STANDARD_15MIN(
        id = "standard_15min",
        title = "Standard 15-Minute Warning",
        previewText = "15 minutes remaining: Prepare for Wudu & Sunnah prayers."
    ),
    DHIKR_ISTIGHFAR(
        id = "dhikr_istighfar",
        title = "Dhikr & Istighfar Preparation",
        previewText = "Wrap up worldly affairs, recite Istighfar, and prepare your heart for communion."
    ),
    CONGREGATION_TAKBEER(
        id = "congregation_takbeer",
        title = "Masjid Congregation Reminder",
        previewText = "Head towards the Masjid to catch the first Takbir with the Jama'at."
    ),
    CUSTOM(
        id = "custom",
        title = "Personalized Pre-Prayer Reminder",
        previewText = "Custom pre-prayer warning entered by user"
    )
}

object PrayerNotificationPreferences {

    private const val TAG = "PrayerNotifPrefs"
    private const val PREFS_NAME = "prayer_notification_custom_prefs"

    private const val KEY_PRAYER_MESSAGE_STYLE = "key_prayer_message_style"
    private const val KEY_CUSTOM_PRAYER_TEXT = "key_custom_prayer_text"
    private const val KEY_PRE_REMINDER_STYLE = "key_pre_reminder_style"
    private const val KEY_CUSTOM_PRE_REMINDER_TEXT = "key_custom_pre_reminder_text"
    private const val KEY_ENABLE_VIBRATION = "key_enable_vibration"
    private const val KEY_ENABLE_HEADS_UP = "key_enable_heads_up"

    private const val DEFAULT_CUSTOM_PRAYER_TEXT = "O Allah, grant barakah in this prayer and accept our prostration."
    private const val DEFAULT_CUSTOM_PRE_REMINDER_TEXT = "15 minutes left: Pause work, perform Wudu, and prepare for prayer."

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getPrayerMessageStyle(context: Context): PrayerMessageStyle {
        val savedId = getPrefs(context).getString(KEY_PRAYER_MESSAGE_STYLE, PrayerMessageStyle.TRADITIONAL.id)
        return PrayerMessageStyle.values().firstOrNull { it.id == savedId } ?: PrayerMessageStyle.TRADITIONAL
    }

    fun setPrayerMessageStyle(context: Context, style: PrayerMessageStyle) {
        getPrefs(context).edit().putString(KEY_PRAYER_MESSAGE_STYLE, style.id).apply()
        Log.d(TAG, "Saved prayer message style: ${style.name}")
    }

    fun getCustomPrayerText(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_PRAYER_TEXT, DEFAULT_CUSTOM_PRAYER_TEXT)
            ?: DEFAULT_CUSTOM_PRAYER_TEXT
    }

    fun setCustomPrayerText(context: Context, text: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_PRAYER_TEXT, text.trim()).apply()
        Log.d(TAG, "Saved custom prayer text: $text")
    }

    fun getPreReminderMessageStyle(context: Context): PreReminderMessageStyle {
        val savedId = getPrefs(context).getString(KEY_PRE_REMINDER_STYLE, PreReminderMessageStyle.STANDARD_15MIN.id)
        return PreReminderMessageStyle.values().firstOrNull { it.id == savedId } ?: PreReminderMessageStyle.STANDARD_15MIN
    }

    fun setPreReminderMessageStyle(context: Context, style: PreReminderMessageStyle) {
        getPrefs(context).edit().putString(KEY_PRE_REMINDER_STYLE, style.id).apply()
        Log.d(TAG, "Saved pre-reminder style: ${style.name}")
    }

    fun getCustomPreReminderText(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_PRE_REMINDER_TEXT, DEFAULT_CUSTOM_PRE_REMINDER_TEXT)
            ?: DEFAULT_CUSTOM_PRE_REMINDER_TEXT
    }

    fun setCustomPreReminderText(context: Context, text: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_PRE_REMINDER_TEXT, text.trim()).apply()
        Log.d(TAG, "Saved custom pre-reminder text: $text")
    }

    fun isVibrationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLE_VIBRATION, true)
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLE_VIBRATION, enabled).apply()
    }

    fun isHeadsUpEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_ENABLE_HEADS_UP, true)
    }

    fun setHeadsUpEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_ENABLE_HEADS_UP, enabled).apply()
    }

    /**
     * Builds the personalized notification body text reflecting user's chosen phrasing and tone.
     */
    fun getNotificationBodyText(
        context: Context,
        prayerName: String,
        isPreReminder: Boolean,
        soundTitle: String
    ): String {
        val toneSuffix = " • Tone: $soundTitle"

        return if (isPreReminder) {
            when (getPreReminderMessageStyle(context)) {
                PreReminderMessageStyle.STANDARD_15MIN ->
                    "15 mins to $prayerName: Time for Wudu & Sunnah$toneSuffix"
                PreReminderMessageStyle.DHIKR_ISTIGHFAR ->
                    "Recite Istighfar & prepare your heart for $prayerName$toneSuffix"
                PreReminderMessageStyle.CONGREGATION_TAKBEER ->
                    "Head to the Masjid for $prayerName congregation$toneSuffix"
                PreReminderMessageStyle.CUSTOM ->
                    "${getCustomPrayerText(context)}$toneSuffix"
            }
        } else {
            when (getPrayerMessageStyle(context)) {
                PrayerMessageStyle.TRADITIONAL ->
                    if (prayerName.equals("Fajr", ignoreCase = true)) {
                        "الصلاة خير من النوم • حي على الصلاة$toneSuffix"
                    } else {
                        "حي على الصلاة • حي على الفلاح$toneSuffix"
                    }
                PrayerMessageStyle.QURANIC_4_103 ->
                    "\"Indeed, prayer has been decreed upon the believers at specified times\" (4:103)$toneSuffix"
                PrayerMessageStyle.SPIRITUAL_SERENITY ->
                    "Stand before Allah with tranquility and devotion for $prayerName$toneSuffix"
                PrayerMessageStyle.HADITH_FIRST_DEED ->
                    "The first deed accounted for is $prayerName prayer — attend with presence$toneSuffix"
                PrayerMessageStyle.CUSTOM ->
                    "${getCustomPrayerText(context)}$toneSuffix"
            }
        }
    }
}
