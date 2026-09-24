package com.example.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.PrayerAlarmService
import com.example.data.model.PrayerTimeItem
import com.example.data.model.PrayerType
import com.example.data.repository.AladhanRepository
import com.example.data.repository.PrayerApiProvider
import com.example.service.CountdownState
import com.example.service.PrayerCountdownEngine
import com.example.util.PrayerCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

data class DashboardUiState(
    val isFriday: Boolean = false,
    val prayerItems: List<PrayerTimeItem> = emptyList(),
    val countdownState: CountdownState = CountdownState(),
    val activePrayerName: String = "Fajr",
    val hijriDate: String = "15 Ramadan 1447 AH",
    val gregorianDate: String = "Sunday, March 15, 2026"
)

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadPrayerTimes()
        startPeriodicUpdateLoop()
    }

    /**
     * MODULE 5: Dynamic Jumu'ah Override Engine
     */
    fun isFriday(): Boolean {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
    }

    fun getPrayerName(prayerIndex: Int, isFriday: Boolean = isFriday()): String {
        return PrayerCalculator.getPrayerName(prayerIndex, isFriday)
    }

    fun getDisplayNameForType(type: PrayerType, isFriday: Boolean = isFriday()): String {
        return if (type == PrayerType.DHUHR && isFriday) {
            "Jumu'ah"
        } else {
            type.displayName
        }
    }

    fun loadPrayerTimes() {
        viewModelScope.launch(Dispatchers.IO) {
            val friday = isFriday()
            val (schedule, items) = AladhanRepository.getSahiwalFallbackSchedule(PrayerApiProvider.ALADHAN_EXACT_COORDINATES)

            // Apply Jumu'ah override if Friday
            val adjustedItems = items.mapIndexed { index, item ->
                if (item.type == PrayerType.DHUHR && friday) {
                    item.copy() // Keep items synced
                } else {
                    item
                }
            }

            PrayerCountdownEngine.startEngine(adjustedItems, schedule)

            _uiState.value = _uiState.value.copy(
                isFriday = friday,
                prayerItems = adjustedItems,
                countdownState = PrayerCountdownEngine.countdownState.value,
                hijriDate = schedule.hijriDate,
                gregorianDate = schedule.gregorianDate
            )
        }
    }

    private fun startPeriodicUpdateLoop() {
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000L)
                val friday = isFriday()
                val currentCountdown = PrayerCountdownEngine.countdownState.value

                // If target prayer is Dhuhr on Friday, display Jumu'ah in countdown
                val displayTarget = if (currentCountdown.targetPrayerName.equals("Dhuhr", ignoreCase = true) && friday) {
                    "Jumu'ah"
                } else {
                    currentCountdown.targetPrayerName
                }

                _uiState.value = _uiState.value.copy(
                    isFriday = friday,
                    countdownState = currentCountdown.copy(targetPrayerName = displayTarget)
                )
            }
        }
    }

    fun triggerPrayerAlarm(context: Context, prayerIndex: Int) {
        val prayerName = getPrayerName(prayerIndex, isFriday())
        val prayerArabic = if (prayerName == "Jumu'ah") "صلاة الجمعة" else "الصلاة"
        val intent = android.content.Intent(context, PrayerAlarmService::class.java).apply {
            action = PrayerAlarmService.ACTION_START_AZAN
            putExtra(PrayerAlarmService.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmService.EXTRA_PRAYER_ARABIC, prayerArabic)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
