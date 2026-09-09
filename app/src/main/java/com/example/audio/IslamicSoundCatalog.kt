package com.example.audio

object IslamicSoundCatalog {

    val allSounds: List<IslamicSoundItem> = listOf(
        // ==========================================
        // 1. GLOBAL ADHANS (Category: GLOBAL_ADHANS)
        // ==========================================
        IslamicSoundItem(
            id = "fajr_special",
            title = "Fajr Special (As-Salatu Khairum Minan Naum)",
            arabicTitle = "أذان الفجر - الصلاة خير من النوم",
            artistOrOrigin = "Sheikh Ali Ahmed Mulla (Holy Makkah)",
            category = SoundCategory.GLOBAL_ADHANS,
            durationFormatted = "03:52",
            isBundledRaw = true,
            rawResName = "fajr_special",
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/adhan_fajr_makkah.mp3",
            description = "Traditional Fajr dawn adhan specifically containing the sacred phrase 'As-Salatu Khairum Minan Naum'.",
            accentColorHex = 0xFF38BDF8
        ),
        IslamicSoundItem(
            id = "standard_adhan",
            title = "Standard Melodic Adhan",
            arabicTitle = "الأذان الموحد الرخيم",
            artistOrOrigin = "Classic Maqam Hijaz Harmonized",
            category = SoundCategory.GLOBAL_ADHANS,
            durationFormatted = "03:15",
            isBundledRaw = true,
            rawResName = "standard_adhan",
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/adhan_makkah.mp3",
            description = "Bundled crisp melodic adhan with full acoustic resonance for daily prayers.",
            accentColorHex = 0xFFF59E0B
        ),
        IslamicSoundItem(
            id = "adhan_makkah",
            title = "Makkah Al-Mukarramah Adhan",
            arabicTitle = "أذان المسجد الحرام بمكة المكرمة",
            artistOrOrigin = "Sheikh Ali Mulla (Masjid Al-Haram)",
            category = SoundCategory.GLOBAL_ADHANS,
            durationFormatted = "03:45",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/adhan_makkah_ali_mulla.mp3",
            description = "Iconic, soulful call to prayer recorded live from the Holy Kaaba in Makkah.",
            accentColorHex = 0xFFE5C07B
        ),
        IslamicSoundItem(
            id = "adhan_madinah",
            title = "Madinah Al-Munawwarah Adhan",
            arabicTitle = "أذان المسجد النبوي الشريف",
            artistOrOrigin = "Sheikh Essam Bukhari (Masjid An-Nabawi)",
            category = SoundCategory.GLOBAL_ADHANS,
            durationFormatted = "03:30",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/adhan_madinah.mp3",
            description = "Peaceful, serene call from the Prophet's Mosque in the City of Light.",
            accentColorHex = 0xFF10B981
        ),
        IslamicSoundItem(
            id = "adhan_alaqsa",
            title = "Al-Aqsa Sanctuary Adhan",
            arabicTitle = "أذان المسجد الأقصى المبارك",
            artistOrOrigin = "Jerusalem Al-Quds Historic Muazzin",
            category = SoundCategory.GLOBAL_ADHANS,
            durationFormatted = "04:10",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/adhan_alaqsa.mp3",
            description = "Majestic and reverent call echoing across the courtyards of the third holiest sanctuary.",
            accentColorHex = 0xFF34D399
        ),
        IslamicSoundItem(
            id = "adhan_mishary",
            title = "Sheikh Mishary Rashid Alafasy",
            arabicTitle = "أذان بصوت مشاري بن راشد العفاسي",
            artistOrOrigin = "Kuwait Grand Mosque",
            category = SoundCategory.GLOBAL_ADHANS,
            durationFormatted = "03:40",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/adhan_alafasy.mp3",
            description = "Globally renowned, crystal-clear, deep emotional vocal rendition.",
            accentColorHex = 0xFFA78BFA
        ),
        IslamicSoundItem(
            id = "adhan_abdulbasit",
            title = "Sheikh Abdul Basit Abdul Samad",
            arabicTitle = "أذان عبد الباسط عبد الصمد",
            artistOrOrigin = "Egyptian Classical Heritage",
            category = SoundCategory.GLOBAL_ADHANS,
            durationFormatted = "04:22",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/adhan_abdulbasit.mp3",
            description = "Legendary historical recitation with unparalleled breath control and spiritual depth.",
            accentColorHex = 0xFFF472B6
        ),
        IslamicSoundItem(
            id = "adhan_istanbul",
            title = "Istanbul Blue Mosque Adhan",
            arabicTitle = "أذان جامع السلطان أحمد بإسطنبول",
            artistOrOrigin = "Ottoman Saba Maqam Tradition",
            category = SoundCategory.GLOBAL_ADHANS,
            durationFormatted = "03:55",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/adhan_istanbul.mp3",
            description = "Melancholic and contemplative Ottoman acoustic style renowned throughout Turkey.",
            accentColorHex = 0xFF60A5FA
        ),

        // ==========================================
        // 2. SHORT REMINDERS (Category: SHORT_REMINDERS)
        // ==========================================
        IslamicSoundItem(
            id = "short_zikr_beep",
            title = "Ambient Zikr Pulse",
            arabicTitle = "نبض الذكر الهادئ",
            artistOrOrigin = "Clean Pre-Prayer Alert Tone",
            category = SoundCategory.SHORT_REMINDERS,
            durationFormatted = "00:02",
            isBundledRaw = true,
            rawResName = "short_zikr_beep",
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/reminder_zikr_pulse.mp3",
            description = "Fast, non-intrusive soft bell pulses for alerting 15 minutes before prayer time.",
            accentColorHex = 0xFF10B981
        ),
        IslamicSoundItem(
            id = "subhanallah_reminder",
            title = "SubhanAllah Melodic Chime",
            arabicTitle = "نغمة سبحان الله",
            artistOrOrigin = "Harmonic Spiritual Ascension",
            category = SoundCategory.SHORT_REMINDERS,
            durationFormatted = "00:03",
            isBundledRaw = true,
            rawResName = "subhanallah_reminder",
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/reminder_subhanallah.mp3",
            description = "Gentle 3-tone ascending chime evoking the glorification of Allah.",
            accentColorHex = 0xFFE5C07B
        ),
        IslamicSoundItem(
            id = "alhamdulillah_tone",
            title = "Alhamdulillah Gratitude Tone",
            arabicTitle = "نغمة الحمد لله",
            artistOrOrigin = "Acoustic Warm Chime",
            category = SoundCategory.SHORT_REMINDERS,
            durationFormatted = "00:03",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/reminder_alhamdulillah.mp3",
            description = "Warm, resonant tone designed to inspire gratitude and readiness for salah.",
            accentColorHex = 0xFFFBBF24
        ),
        IslamicSoundItem(
            id = "allahuakbar_tone",
            title = "Allahu Akbar Sacred Takbeer",
            arabicTitle = "نغمة التكبير الخاشع",
            artistOrOrigin = "Vocal Takbeer Chime",
            category = SoundCategory.SHORT_REMINDERS,
            durationFormatted = "00:04",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/reminder_allahuakbar.mp3",
            description = "Serene, whispered takbeer reminding the soul of the Almighty's greatness.",
            accentColorHex = 0xFFF97316
        ),
        IslamicSoundItem(
            id = "astaghfirullah_tone",
            title = "Astaghfirullah Serene Istighfar",
            arabicTitle = "نغمة أستغفر الله",
            artistOrOrigin = "Soft Meditative Chime",
            category = SoundCategory.SHORT_REMINDERS,
            durationFormatted = "00:03",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/reminder_astaghfirullah.mp3",
            description = "Soothing, reflective alert ideal for pre-salah purification and mindful wudu.",
            accentColorHex = 0xFF818CF8
        ),

        // ==========================================
        // 3. PREMIUM NAATS (Category: PREMIUM_NAATS)
        // ==========================================
        IslamicSoundItem(
            id = "naat_talaal_badru",
            title = "Tala'al Badru 'Alayna",
            arabicTitle = "طلع البدر علينا من ثنيات الوداع",
            artistOrOrigin = "Historic Madinah Welcome Anthem",
            category = SoundCategory.PREMIUM_NAATS,
            durationFormatted = "01:25",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/naat_talaal_badru.mp3",
            description = "The timeless welcome nasheed sung by the Ansar upon the Prophet's arrival in Madinah.",
            accentColorHex = 0xFF34D399
        ),
        IslamicSoundItem(
            id = "naat_hasbi_rabbi",
            title = "Hasbi Rabbi Jallallah",
            arabicTitle = "حسبي ربي جل الله ما في قلبي غير الله",
            artistOrOrigin = "Acoustic Daff & Spiritual Harmonics",
            category = SoundCategory.PREMIUM_NAATS,
            durationFormatted = "01:15",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/naat_hasbi_rabbi.mp3",
            description = "Soul-stirring classic praising Allah's singular majesty, paired with traditional daff rhythm.",
            accentColorHex = 0xFFE5C07B
        ),
        IslamicSoundItem(
            id = "naat_mawla_ya_salli",
            title = "Mawla Ya Salli Wa Sallim (Burda)",
            arabicTitle = "مولاي صل وسلم دائماً أبداً",
            artistOrOrigin = "Imam Al-Busiri Classical Qasida",
            category = SoundCategory.PREMIUM_NAATS,
            durationFormatted = "01:30",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/naat_burda.mp3",
            description = "Sublime verses from the famous Ode of the Cloak praising the Prophet Muhammad ﷺ.",
            accentColorHex = 0xFF38BDF8
        ),
        IslamicSoundItem(
            id = "naat_balaghal_ula",
            title = "Balaghal 'Ula Bi Kamaalihi",
            arabicTitle = "بلغ العلى بكماله كشف الدجى بجماله",
            artistOrOrigin = "Saadi Shirazi Tribute Chants",
            category = SoundCategory.PREMIUM_NAATS,
            durationFormatted = "01:20",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/naat_balaghal_ula.mp3",
            description = "Celebrated classical ode of perfection, beauty, and peace upon the beloved Messenger.",
            accentColorHex = 0xFFF472B6
        ),
        IslamicSoundItem(
            id = "naat_faslon_ko",
            title = "Faslon Ko Takalluf Hai Humse",
            arabicTitle = "فاصلوں کو تکلف ہے ہم سے اگر",
            artistOrOrigin = "Traditional Subcontinental Naat",
            category = SoundCategory.PREMIUM_NAATS,
            durationFormatted = "01:35",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/naat_faslon_ko.mp3",
            description = "A deeply heartfelt poetic expression of yearning for the green dome of Madinah.",
            accentColorHex = 0xFFA78BFA
        ),
        IslamicSoundItem(
            id = "naat_labbaik",
            title = "Labbaik Allahumma Labbaik",
            arabicTitle = "لبيك اللهم لبيك • لبيك لا شريك لك لبيك",
            artistOrOrigin = "Sacred Hajj Talbiyah Melody",
            category = SoundCategory.PREMIUM_NAATS,
            durationFormatted = "01:10",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/naat_talbiyah.mp3",
            description = "Inspiring, reverent Talbiyah anthem evoking unity and devotion before the Almighty.",
            accentColorHex = 0xFFFBBF24
        ),

        // ==========================================
        // 4. TAHAJJUD & ZEN (Category: TAHAJJUD_ZEN)
        // ==========================================
        IslamicSoundItem(
            id = "tahajjud_chime",
            title = "Dawn Birdsong & Zen Chime",
            arabicTitle = "أجراس الفجر وأصوات الطبيعة",
            artistOrOrigin = "Optimal Tahajjud Wake Window",
            category = SoundCategory.TAHAJJUD_ZEN,
            durationFormatted = "00:15",
            isBundledRaw = true,
            rawResName = "tahajjud_chime",
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/zen_dawn_birdsong.mp3",
            description = "Bundled 432Hz harmonic healing frequency chimes coupled with gentle dawn nature ambience.",
            accentColorHex = 0xFF10B981
        ),
        IslamicSoundItem(
            id = "zen_mountain_stream",
            title = "Flowing Mountain Stream & Water",
            arabicTitle = "خرير المياه الهادئة والجبال",
            artistOrOrigin = "Natural Spring Water Ambience",
            category = SoundCategory.TAHAJJUD_ZEN,
            durationFormatted = "00:45",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/zen_mountain_stream.mp3",
            description = "Organic sound of crystal-clear alpine water flow easing the transition out of deep sleep.",
            accentColorHex = 0xFF38BDF8
        ),
        IslamicSoundItem(
            id = "zen_bamboo_flute",
            title = "Dawn Ney & Bamboo Flute",
            arabicTitle = "أنغام الناي الصوفي وسكون السحر",
            artistOrOrigin = "Meditative Sufi Ney Melody",
            category = SoundCategory.TAHAJJUD_ZEN,
            durationFormatted = "00:50",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/zen_bamboo_flute.mp3",
            description = "Gentle, resonant acoustic reed flute evoking profound stillness in the late night.",
            accentColorHex = 0xFFF59E0B
        ),
        IslamicSoundItem(
            id = "zen_binaural_dawn",
            title = "Midnight Quietude Alpha Waves",
            arabicTitle = "هدوء الليل وموجات الصفاء",
            artistOrOrigin = "Spiritual Sleep-to-Prayer Waves",
            category = SoundCategory.TAHAJJUD_ZEN,
            durationFormatted = "00:40",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/zen_binaural_dawn.mp3",
            description = "Soft, low-frequency harmonics specifically designed to awaken the body without grogginess.",
            accentColorHex = 0xFF6366F1
        ),
        IslamicSoundItem(
            id = "zen_sanctuary_bells",
            title = "Sanctuary Crystal Singing Chimes",
            arabicTitle = "أجراس السكينة الروحانية",
            artistOrOrigin = "Resonant Spiritual Awakening",
            category = SoundCategory.TAHAJJUD_ZEN,
            durationFormatted = "00:35",
            isBundledRaw = false,
            remoteAudioUrl = "https://cdn.islamic.network/prayer-times/audio/zen_sanctuary_bells.mp3",
            description = "Pure, sustained crystal harmonic tones creating an atmosphere of reverence and peace.",
            accentColorHex = 0xFFEC4899
        )
    )

    fun getSoundById(id: String): IslamicSoundItem {
        return allSounds.find { it.id == id } ?: allSounds.first()
    }

    fun getSoundsByCategory(category: SoundCategory): List<IslamicSoundItem> {
        return allSounds.filter { it.category == category }
    }
}
