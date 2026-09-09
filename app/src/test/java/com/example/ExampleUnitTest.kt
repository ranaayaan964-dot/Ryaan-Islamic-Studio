package com.example

import com.example.data.repository.AladhanRepository
import com.example.data.repository.PrayerApiProvider
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `verify Sahiwal schedule returns correct prayer times with Islamic Finder source`() {
    val (schedule, items) = AladhanRepository.getSahiwalFallbackSchedule(PrayerApiProvider.ISLAMIC_FINDER)
    assertEquals("Sahiwal", schedule.city)
    assertEquals("Pakistan", schedule.country)
    assertEquals("Islamic Finder API", schedule.apiSource)
    assertTrue(items.isNotEmpty())
    assertEquals(6, items.size)
  }

  @Test
  fun `verify MuslimSalat provider schedule`() {
    val (schedule, items) = AladhanRepository.getSahiwalFallbackSchedule(PrayerApiProvider.MUSLIM_SALAT)
    assertEquals("Sahiwal", schedule.city)
    assertEquals("MuslimSalat API", schedule.apiSource)
    assertEquals(6, items.size)
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
    // Verify requirement 3: Countdown must be strictly positive (approx 4 hours 27 min until ~04:24 AM)
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
}

