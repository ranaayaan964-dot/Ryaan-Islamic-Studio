package com.example.audio

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object PrayerSoundPreferences {

    private const val TAG = "PrayerSoundPrefs"
    private const val PREFS_NAME = "prayer_sound_hub_prefs"

    private const val KEY_PRE_REMINDER_ENABLED = "pre_prayer_reminder_enabled"
    private const val KEY_SOUND_ID_PREFIX = "sound_id_event_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isPrePrayerReminderEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PRE_REMINDER_ENABLED, true)
    }

    fun setPrePrayerReminderEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PRE_REMINDER_ENABLED, enabled).apply()
    }

    fun getSoundItemForEvent(context: Context, event: SoundEvent): IslamicSoundItem {
        val soundId = getPrefs(context).getString(KEY_SOUND_ID_PREFIX + event.eventId, event.defaultSoundId)
            ?: event.defaultSoundId
        return IslamicSoundCatalog.getSoundById(soundId)
    }

    fun setSoundForEvent(context: Context, event: SoundEvent, sound: IslamicSoundItem) {
        getPrefs(context).edit()
            .putString(KEY_SOUND_ID_PREFIX + event.eventId, sound.id)
            .apply()
        Log.d(TAG, "Assigned sound ${sound.title} (${sound.id}) to event ${event.title}")
    }

    /**
     * Resolves the dynamic audio URI for background alarm playback.
     * Guaranteed to return a valid playable URI (raw resource, cached file, or fallback).
     */
    fun getSoundUriForPrayer(context: Context, prayerName: String, isPreReminder: Boolean = false): String {
        val event = when {
            isPreReminder -> SoundEvent.PRE_PRAYER
            prayerName.equals("Fajr", ignoreCase = true) -> SoundEvent.FAJR
            prayerName.equals("Tahajjud", ignoreCase = true) -> SoundEvent.TAHAJJUD
            else -> SoundEvent.REGULAR
        }

        val soundItem = getSoundItemForEvent(context, event)
        val uri = AudioCacheManager.resolvePlayableUri(context, soundItem)
        Log.d(TAG, "Resolved audio URI for $prayerName (preReminder=$isPreReminder): $uri [sound=${soundItem.title}]")
        return uri
    }

    /**
     * Returns the title of the assigned sound for display in UI cards or notifications.
     */
    fun getAssignedSoundTitleForPrayer(context: Context, prayerName: String, isPreReminder: Boolean = false): String {
        val event = when {
            isPreReminder -> SoundEvent.PRE_PRAYER
            prayerName.equals("Fajr", ignoreCase = true) -> SoundEvent.FAJR
            prayerName.equals("Tahajjud", ignoreCase = true) -> SoundEvent.TAHAJJUD
            else -> SoundEvent.REGULAR
        }
        return getSoundItemForEvent(context, event).title
    }
}
