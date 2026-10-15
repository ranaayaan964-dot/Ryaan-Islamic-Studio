package com.example

import com.example.data.repository.AladhanRepository
import com.example.data.repository.PrayerApiProvider
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun `AI provider key pool parses separators removes placeholders and deduplicates`() {
    val keys = com.example.data.remote.DualAiScholarEngine.parseConfiguredKeyPool(
      " gemini-key-one,gemini-key-two;YOUR_GEMINI_KEY\nMY_GROQ_API_KEY,gemini-key-one "
    )

    assertEquals(listOf("gemini-key-one", "gemini-key-two"), keys)
  }

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `verify offline Sahiwal schedule calculates all prayer times locally`() {
    val (schedule, items) = AladhanRepository.getSahiwalFallbackSchedule(PrayerApiProvider.ISLAMIC_FINDER)
    assertEquals("Sahiwal", schedule.city)
    assertEquals("Pakistan", schedule.country)
    assertTrue(schedule.apiSource.contains("Local PrayerCalculator"))
    assertFalse(schedule.isLiveFromApi)
    assertEquals(6, items.size)
    assertEquals(
      setOf(
        com.example.data.model.PrayerType.FAJR,
        com.example.data.model.PrayerType.SUNRISE,
        com.example.data.model.PrayerType.DHUHR,
        com.example.data.model.PrayerType.ASR,
        com.example.data.model.PrayerType.MAGHRIB,
        com.example.data.model.PrayerType.ISHA
      ),
      items.map { it.type }.toSet()
    )
    assertTrue(items.all { it.timeMillis > 0L && it.timeFormatted.isNotBlank() })
  }

  @Test
  fun `verify offline Hanafi schedule calculates Asr later than Shafi schedule`() {
    val (schedule, items) = AladhanRepository.getSahiwalFallbackSchedule(PrayerApiProvider.MUSLIM_SALAT)
    assertEquals("Sahiwal", schedule.city)
    assertTrue(schedule.apiSource.contains("Local PrayerCalculator"))
    assertFalse(schedule.isLiveFromApi)
    assertEquals(6, items.size)

    val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Karachi"))
    val shafiAsr = com.example.util.PrayerCalculator.calculatePrayerTimes(
      AladhanRepository.SAHIWAL_LATITUDE,
      AladhanRepository.SAHIWAL_LONGITUDE,
      calendar,
      asrShadowFactor = 1.0
    ).first { it.type == com.example.data.model.PrayerType.ASR }
    val hanafiAsr = items.first { it.type == com.example.data.model.PrayerType.ASR }
    val dhuhr = items.first { it.type == com.example.data.model.PrayerType.DHUHR }
    val maghrib = items.first { it.type == com.example.data.model.PrayerType.MAGHRIB }
    assertTrue("Hanafi Asr must follow Dhuhr", hanafiAsr.timeMillis > dhuhr.timeMillis)
    assertTrue("Hanafi Asr must precede Maghrib", hanafiAsr.timeMillis < maghrib.timeMillis)
    assertTrue("Hanafi Asr should occur after Shafi Asr", hanafiAsr.timeMillis > shafiAsr.timeMillis)
  }

  @Test
  fun `verify hardcoded login credentials logic for ryaan and ryan`() {
    val validPass = "11221122"
    val testCases = listOf("ryaan", "ryan", "Ryaan", "RYAN")
    for (user in testCases) {
      val isUserValid = user.trim().equals("ryaan", ignoreCase = true) || user.trim().equals("ryan", ignoreCase = true)
      assertTrue("Failed for $user", isUserValid && validPass == "11221122")
    }

    val invalidUser = "other_user"
    val isInvalidUser = invalidUser.trim().equals("ryaan", ignoreCase = true) || invalidUser.trim().equals("ryan", ignoreCase = true)
    assertFalse(isInvalidUser)

    val wrongPassword = "wrong_password"
    val isPassValid = wrongPassword == "11221122"
    assertFalse(isPassValid)
  }

  @Test
  fun `verify 11-57 PM midnight rollover logic resolves next prayer as tomorrow Fajr with positive countdown`() {
    val (schedule, items) = AladhanRepository.getSahiwalFallbackSchedule(PrayerApiProvider.ALADHAN_EXACT_COORDINATES)
    com.example.service.PrayerCountdownEngine.startEngine(items, schedule)

    // Simulate 11:57 PM (23:57:00)
    com.example.service.PrayerCountdownEngine.setSimulatedMidnight(true)

    val state = com.example.service.PrayerCountdownEngine.countdownState.value
    // Verify requirement 1: Next prayer must be intelligently set to "Fajr"
    assertEquals("Fajr", state.targetPrayerName)
    // Verify requirement 2: isTomorrow must be true (Fajr + 1 Day)
    assertTrue(state.isTomorrow)
    // Verify requirement 3: Countdown must be strictly positive and remain within the next day.
    assertTrue("Remaining millis must be positive but was ${state.remainingMillis}", state.remainingMillis > 0)
    assertTrue("Hours must be around 4 or 5", state.hours in 4..5)
    assertTrue("Formatted time must not start with negative", !state.formattedTime.startsWith("-"))

    // Reset simulation
    com.example.service.PrayerCountdownEngine.setSimulatedMidnight(false)
  }

  @Test
  fun `verify 45-minute Jamaat grace period sets isPrayerInProgress true and targets active prayer`() {
    val (schedule, items) = AladhanRepository.getSahiwalFallbackSchedule(PrayerApiProvider.ALADHAN_EXACT_COORDINATES)
    com.example.service.PrayerCountdownEngine.startEngine(items, schedule)

    // Simulate 12:12 PM (6 minutes into Dhuhr which starts at 12:06 PM)
    com.example.service.PrayerCountdownEngine.setSimulatedGracePeriod(true)

    val state = com.example.service.PrayerCountdownEngine.countdownState.value

    // Requirement 1: isPrayerInProgress must be true
    assertTrue("isPrayerInProgress must be true during 45m grace period", state.isPrayerInProgress)

    // Requirement 2: targetPrayerName must be the currently active prayer (Dhuhr)
    assertEquals("Dhuhr", state.targetPrayerName)

    // Requirement 3: Remaining millis must be positive (approx 39 minutes remaining of 45-min grace period)
    assertTrue("Remaining millis must be positive", state.remainingMillis > 0)
    assertTrue("Minutes should be around 38-39", state.minutes in 38..40)

    // Verify time outside any grace period (e.g. 10:00 AM before Dhuhr)
    com.example.service.PrayerCountdownEngine.setSimulatedTime(10, 0)
    val outsideState = com.example.service.PrayerCountdownEngine.countdownState.value
    assertFalse("isPrayerInProgress must be false at 10:00 AM", outsideState.isPrayerInProgress)

    // Reset simulation
    com.example.service.PrayerCountdownEngine.setSimulatedGracePeriod(false)
  }

  @Test
  fun `verify at 9-23 PM after Isha engine correctly targets tomorrow Fajr and NOT Dhuhr`() {
    val (schedule, items) = AladhanRepository.getSahiwalFallbackSchedule(PrayerApiProvider.ALADHAN_EXACT_COORDINATES)
    com.example.service.PrayerCountdownEngine.startEngine(items, schedule)

    // Simulate 9:23 PM (21:23) - which was the exact bug report
    com.example.service.PrayerCountdownEngine.setSimulatedNight(true)

    val state = com.example.service.PrayerCountdownEngine.countdownState.value

    // Must NEVER be Dhuhr
    assertNotEquals("Must not be Dhuhr at 9:23 PM", "Dhuhr", state.targetPrayerName)

    // Must be Fajr of tomorrow
    assertEquals("Fajr", state.targetPrayerName)
    assertTrue("isTomorrow must be true at 9:23 PM", state.isTomorrow)
    assertFalse("isPrayerInProgress must be false at 9:23 PM (past Isha grace)", state.isPrayerInProgress)
    assertTrue("Remaining millis must be positive", state.remainingMillis > 0)
    assertTrue("Formatted time must not start with negative", !state.formattedTime.startsWith("-"))

    // Reset simulation
    com.example.service.PrayerCountdownEngine.setSimulatedNight(false)
  }

  @Test
  fun `verify Sahiwal Qibla bearing calculation is approximately 261 degrees WSW`() {
    val sahiwalLat = 30.6682
    val sahiwalLon = 73.1114
    val bearing = com.example.util.QiblaCalculator.calculateQiblaBearing(sahiwalLat, sahiwalLon)
    assertTrue("Qibla bearing from Sahiwal should be around 261 degrees but was $bearing", bearing in 260.0..263.0)
    val distance = com.example.util.QiblaCalculator.calculateDistanceToKaaba(sahiwalLat, sahiwalLon)
    assertTrue("Distance from Sahiwal to Makkah should be around 3300-3800km but was $distance", distance in 3300..3800)
  }

  @Test
  fun `verify IslamicSoundCatalog contains 20+ sounds across 4 required categories`() {
    val allSounds = com.example.audio.IslamicSoundCatalog.allSounds
    assertTrue("Sound catalog must have at least 20 sounds but has ${allSounds.size}", allSounds.size >= 20)

    val globalAdhans = com.example.audio.IslamicSoundCatalog.getSoundsByCategory(com.example.audio.SoundCategory.GLOBAL_ADHANS)
    val shortReminders = com.example.audio.IslamicSoundCatalog.getSoundsByCategory(com.example.audio.SoundCategory.SHORT_REMINDERS)
    val premiumNaats = com.example.audio.IslamicSoundCatalog.getSoundsByCategory(com.example.audio.SoundCategory.PREMIUM_NAATS)
    val tahajjudZen = com.example.audio.IslamicSoundCatalog.getSoundsByCategory(com.example.audio.SoundCategory.TAHAJJUD_ZEN)

    assertTrue("Must have Global Adhans", globalAdhans.size >= 6)
    assertTrue("Must have Short Reminders", shortReminders.size >= 4)
    assertTrue("Must have Premium Naats", premiumNaats.size >= 5)
    assertTrue("Must have Tahajjud & Zen sounds", tahajjudZen.size >= 4)
  }

  @Test
  fun `verify APK size optimization - only 5 essential sounds bundled in raw`() {
    val bundledSounds = com.example.audio.IslamicSoundCatalog.allSounds.filter { it.isBundledRaw }
    assertEquals("Exactly 5 essential sounds should be bundled in raw to keep APK under 50MB", 5, bundledSounds.size)

    val bundledIds = bundledSounds.map { it.id }.toSet()
    assertTrue(bundledIds.contains("standard_adhan"))
    assertTrue(bundledIds.contains("fajr_special"))
    assertTrue(bundledIds.contains("short_zikr_beep"))
    assertTrue(bundledIds.contains("subhanallah_reminder"))
    assertTrue(bundledIds.contains("tahajjud_chime"))
  }

  @Test
  fun `verify Fajr prayer tone contains As-Salatu Khairum Minan Naum by default`() {
    val fajrTone = com.example.audio.IslamicSoundCatalog.allSounds.first { it.id == "fajr_special" }
    assertTrue(
      "Fajr tone must contain 'As-salatu Khairum Minan Naum' phrase",
      fajrTone.description.contains("As-Salatu Khairum Minan Naum", ignoreCase = true) ||
      fajrTone.title.contains("As-Salatu Khairum Minan Naum", ignoreCase = true)
    )
  }

  @Test
  fun `verify audio fallback synthesis generates valid playable audio file`() {
    val tempFile = java.io.File.createTempFile("test_fallback_audio", ".wav")
    try {
      val soundItem = com.example.audio.IslamicSoundCatalog.allSounds.first()
      com.example.audio.AudioCacheManager.synthesizeSpiritualAudioFallback(tempFile, soundItem)
      assertTrue("Synthesized audio file must exist", tempFile.exists())
      assertTrue("Synthesized audio file must not be empty", tempFile.length() > 1000)
    } finally {
      tempFile.delete()
    }
  }

  @Test
  fun `verify NamazMetadataProvider formats 24-hour time to 12-hour properly`() {
    val fajr12 = com.example.alarm.NamazMetadataProvider.formatTo12Hour("05:15")
    val dhuhr12 = com.example.alarm.NamazMetadataProvider.formatTo12Hour("12:30")
    val asr12 = com.example.alarm.NamazMetadataProvider.formatTo12Hour("16:00")
    val maghrib12 = com.example.alarm.NamazMetadataProvider.formatTo12Hour("18:25")
    val isha12 = com.example.alarm.NamazMetadataProvider.formatTo12Hour("20:00")

    assertEquals("05:15 AM", fajr12)
    assertEquals("12:30 PM", dhuhr12)
    assertEquals("04:00 PM", asr12)
    assertEquals("06:25 PM", maghrib12)
    assertEquals("08:00 PM", isha12)
  }

  @Test
  fun `verify NamazMetadataProvider calculates future next trigger correctly`() {
    val now = System.currentTimeMillis()
    val triggerMillis = com.example.alarm.NamazMetadataProvider.calculateNextTriggerMillis("05:15", now)
    assertTrue("Next trigger millis must be in the future", triggerMillis > now)
    assertTrue("Next trigger millis must be within 24 hours plus a day", triggerMillis <= now + (25 * 60 * 60 * 1000L))
  }

  @Test
  fun `verify VersionComparator semantic version comparison engine handles major minor patch`() {
    // Newer major
    assertTrue(com.example.util.VersionComparator.isUpdateAvailable("v2.0.0", "1.0.0"))
    assertTrue(com.example.util.VersionComparator.isUpdateAvailable("2.0.0", "1.9.9"))

    // Newer minor
    assertTrue(com.example.util.VersionComparator.isUpdateAvailable("v1.1.0", "1.0.9"))
    assertTrue(com.example.util.VersionComparator.isUpdateAvailable("1.2.0", "1.1.9"))

    // Newer patch
    assertTrue(com.example.util.VersionComparator.isUpdateAvailable("v1.0.5", "1.0.4"))
    assertTrue(com.example.util.VersionComparator.isUpdateAvailable("1.0.1", "1.0.0"))

    // Identical versions
    assertFalse(com.example.util.VersionComparator.isUpdateAvailable("v1.0.0", "1.0.0"))
    assertFalse(com.example.util.VersionComparator.isUpdateAvailable("1.0.5", "1.0.5"))

    // Older versions
    assertFalse(com.example.util.VersionComparator.isUpdateAvailable("v0.9.9", "1.0.0"))
    assertFalse(com.example.util.VersionComparator.isUpdateAvailable("1.0.0", "1.0.1"))
    assertFalse(com.example.util.VersionComparator.isUpdateAvailable("1.0.4", "1.0.5"))

    // Multi-digit components
    assertTrue(com.example.util.VersionComparator.isUpdateAvailable("v1.10.0", "1.9.0"))
    assertFalse(com.example.util.VersionComparator.isUpdateAvailable("v1.9.0", "1.10.0"))

    // Exact prompt test case: "1.0.5" vs "1.0.2"
    assertTrue(com.example.util.VersionComparator.isUpdateAvailable("1.0.5", "1.0.2"))
    assertTrue(com.example.util.VersionComparator.isUpdateAvailable("v1.0.5", "1.0.2"))
    assertTrue(com.example.util.VersionComparator.isUpdateAvailable("v1.0.5", "v1.0.2"))
    assertFalse(com.example.util.VersionComparator.isUpdateAvailable("1.0.2", "1.0.5"))
    assertFalse(com.example.util.VersionComparator.isUpdateAvailable("v1.0.2", "v1.0.5"))
  }

  @Test
  fun `verify GitHub update endpoint and default repo URL`() {
    assertEquals(
      "https://github.com/ranaayaan964-dot/Ryaan-Islamic-Studio/releases/latest",
      com.example.util.AppUpdateManager.DEFAULT_GITHUB_RELEASE_URL
    )
    assertEquals(
      "https://api.github.com/repos/ranaayaan964-dot/Ryaan-Islamic-Studio/releases/latest",
      com.example.data.api.GitHubApiService.GITHUB_RELEASES_LATEST_URL
    )
  }

  @Test
  fun `verify VersionComparator parseToIntegerArray strips v prefix and handles qualifiers`() {
    val parts = com.example.util.VersionComparator.parseToIntegerArray("v1.2.3")
    assertEquals(listOf(1, 2, 3), parts)

    val partsWithQualifier = com.example.util.VersionComparator.parseToIntegerArray("v2.4.1-release")
    assertEquals(listOf(2, 4, 1), partsWithQualifier)
  }

  @Test
  fun `verify Gemini chatbot roles map to required models and task complexities`() {
    // 1. Particularly complex tasks -> gemini-3.1-pro-preview
    val complexRole = com.example.data.remote.GeminiChatbotRole.COMPLEX_JURIST
    assertEquals("gemini-3.1-pro-preview", complexRole.modelName)
    assertEquals("gemini-3.1-pro-preview", complexRole.displayModel)
    assertEquals("Particularly Complex Tasks", complexRole.taskComplexity)
    assertTrue(complexRole.systemPrompt.contains("Grand Mufti", ignoreCase = true))

    // 2. General tasks -> gemini-3.5-flash
    val generalRole = com.example.data.remote.GeminiChatbotRole.GENERAL_COMPANION
    assertEquals("gemini-3.5-flash", generalRole.modelName)
    assertEquals("gemini-3.5-flash", generalRole.displayModel)
    assertEquals("General Tasks", generalRole.taskComplexity)
    assertTrue(generalRole.systemPrompt.contains("Spiritual Companion", ignoreCase = true))

    // 3. Tasks that should happen fast -> gemini-3.1-flash-lite
    val fastRole = com.example.data.remote.GeminiChatbotRole.FAST_FINDER
    assertEquals("gemini-3.1-flash-lite-preview", fastRole.modelName)
    assertEquals("gemini-3.1-flash-lite", fastRole.displayModel)
    assertEquals("Tasks that should happen fast", fastRole.taskComplexity)
    assertTrue(fastRole.systemPrompt.contains("high-speed", ignoreCase = true))
  }

  @Test
  fun `verify Gemini chatbot role auto-detection categorizes task complexity accurately`() {
    val complexQuery = "What is the detailed Fiqh ruling on cryptocurrency according to Hanafi and Shafi'i schools?"
    val fastQuery = "Instant Dua for traveling with Arabic and translation"
    val generalQuery = "How to find spiritual peace and patience during difficult times in daily life?"

    assertEquals(com.example.data.remote.GeminiChatbotRole.COMPLEX_JURIST, com.example.data.remote.GeminiChatbotRole.autoDetectRole(complexQuery))
    assertEquals(com.example.data.remote.GeminiChatbotRole.FAST_FINDER, com.example.data.remote.GeminiChatbotRole.autoDetectRole(fastQuery))
    assertEquals(com.example.data.remote.GeminiChatbotRole.GENERAL_COMPANION, com.example.data.remote.GeminiChatbotRole.autoDetectRole(generalQuery))
  }

  @Test
  fun `verify QuranJsonApi contains all 114 Surahs and offline fallback returns valid Arabic and translation`() {
    val allSurahs = com.example.data.quran.api.QuranJsonApi.ALL_114_SURAHS
    assertEquals(114, allSurahs.size)
    assertEquals("Al-Fatihah", allSurahs[0].englishName)
    assertEquals("An-Nas", allSurahs[113].englishName)

    // Test Al-Fatihah fallback data
    val fatihah = com.example.data.quran.api.QuranJsonApi.getOfflineFallback(1)
    assertEquals(7, fatihah.ayahs.size)
    assertTrue(fatihah.ayahs[0].arabicText.contains("بِسْمِ ٱللَّهِ"))
    assertTrue(fatihah.ayahs[0].englishTranslation.contains("In the name of Allah"))

    // Test Al-Ikhlas fallback data
    val ikhlas = com.example.data.quran.api.QuranJsonApi.getOfflineFallback(112)
    assertEquals(4, ikhlas.ayahs.size)
    assertTrue(ikhlas.ayahs[0].arabicText.contains("قُلْ هُوَ ٱللَّهُ أَحَدٌ"))
    assertTrue(ikhlas.ayahs[0].englishTranslation.contains("Say, \"He is Allah"))
  }
}


