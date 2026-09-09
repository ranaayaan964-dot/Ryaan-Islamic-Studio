package com.example.service

import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.model.PrayerTimeItem
import com.example.data.model.PrayerType
import com.example.data.model.SahiwalPrayerSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

const val JAMAAT_GRACE_MILLIS = 45 * 60 * 1000L // 45 minutes

data class CountdownState(
    val formattedTime: String = "00:00:00",
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val remainingMillis: Long = 0L,
    val nextPrayer: PrayerTimeItem? = null,
    val isTomorrow: Boolean = false,
    val currentDeviceTime: String = "",
    val targetPrayerName: String = "Fajr",
    val targetPrayerArabic: String = "الفجر",
    val targetPrayerTimeFormatted: String = "04:24 AM",
    val tickCounter: Long = 0L,
    val isSimulatedMidnight: Boolean = false,
    val isPrayerInProgress: Boolean = false,
    val isSimulatedGracePeriod: Boolean = false
)

object PrayerCountdownEngine {

    val pkZone = TimeZone.getTimeZone("Asia/Karachi")

    private const val TAG = "PrayerCountdownEngine"
    private val timeDisplayFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).apply { timeZone = pkZone }
    private val time12Format = SimpleDateFormat("hh:mm a", Locale.ENGLISH).apply { timeZone = pkZone }

    private val _countdownState = MutableStateFlow(CountdownState())
    val countdownState = _countdownState.asStateFlow()

    private var activeTimer: CountDownTimer? = null
    private var currentPrayers: List<PrayerTimeItem> = emptyList()
    private var currentSchedule: SahiwalPrayerSchedule? = null
    private var tickCount: Long = 0L

    // Optional simulated time offset for testing
    private var simulatedTimeMillis: Long? = null

    // Dedicated Handler on Main Looper guaranteeing 1000ms continuous ticks
    private var mainHandler: Handler? = null

    private fun getOrCreateHandler(): Handler? {
        if (mainHandler == null) {
            mainHandler = try {
                Handler(Looper.getMainLooper())
            } catch (_: Throwable) {
                null
            }
        }
        return mainHandler
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            performTick()
            getOrCreateHandler()?.postDelayed(this, 1000L)
        }
    }

    private var isHandlerRunning = false

    @Synchronized
    fun startEngine(prayers: List<PrayerTimeItem>, schedule: SahiwalPrayerSchedule? = null) {
        currentPrayers = prayers
        if (schedule != null) currentSchedule = schedule
        performTick()
        startPeriodicHandler()
    }

    @Synchronized
    fun updatePrayers(prayers: List<PrayerTimeItem>, schedule: SahiwalPrayerSchedule? = null) {
        currentPrayers = prayers
        if (schedule != null) currentSchedule = schedule
        performTick()
    }

    private var isSimulatedGrace = false

    @Synchronized
    fun setSimulatedMidnight(enable: Boolean) {
        if (enable) {
            isSimulatedGrace = false
            // Simulate 11:57:00 PM today
            val cal = Calendar.getInstance(pkZone).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 57)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            simulatedTimeMillis = cal.timeInMillis
        } else {
            simulatedTimeMillis = null
            isSimulatedGrace = false
        }
        performTick()
    }

    @Synchronized
    fun setSimulatedTime(hour: Int, minute: Int, second: Int = 0) {
        val cal = Calendar.getInstance(pkZone).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
        }
        simulatedTimeMillis = cal.timeInMillis
        isSimulatedGrace = false
        performTick()
    }

    @Synchronized
    fun setSimulatedNight(enable: Boolean) {
        if (enable) {
            isSimulatedGrace = false
            // Simulate 9:23 PM (21:23:00) today
            val cal = Calendar.getInstance(pkZone).apply {
                set(Calendar.HOUR_OF_DAY, 21)
                set(Calendar.MINUTE, 23)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            simulatedTimeMillis = cal.timeInMillis
        } else {
            simulatedTimeMillis = null
            isSimulatedGrace = false
        }
        performTick()
    }

    @Synchronized
    fun setSimulatedGracePeriod(enable: Boolean) {
        if (enable) {
            isSimulatedGrace = true
            // Simulate 6 minutes after Dhuhr starts (e.g. 12:12 PM when Dhuhr is 12:06 PM)
            val dhuhrItem = currentPrayers.firstOrNull { it.type == PrayerType.DHUHR }
            val nowCal = Calendar.getInstance(pkZone)
            val dhuhrToday = (nowCal.clone() as Calendar).apply {
                if (dhuhrItem != null) {
                    val temp = Calendar.getInstance(pkZone).apply { timeInMillis = dhuhrItem.timeMillis }
                    set(Calendar.HOUR_OF_DAY, temp.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, temp.get(Calendar.MINUTE))
                } else {
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 6)
                }
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            simulatedTimeMillis = dhuhrToday.timeInMillis + (6 * 60 * 1000L)
        } else {
            simulatedTimeMillis = null
            isSimulatedGrace = false
        }
        performTick()
    }

    fun isSimulatedMidnightActive(): Boolean = simulatedTimeMillis != null && !isSimulatedGrace
    fun isSimulatedGracePeriodActive(): Boolean = simulatedTimeMillis != null && isSimulatedGrace

    private fun startPeriodicHandler() {
        if (!isHandlerRunning) {
            val handler = getOrCreateHandler()
            if (handler != null) {
                isHandlerRunning = true
                handler.removeCallbacks(tickRunnable)
                handler.post(tickRunnable)
            }
        }
    }

    /**
     * Internal container holding a prayer along with its calculated calendar for TODAY.
     */
    private data class TodayPrayer(
        val item: PrayerTimeItem,
        val todayCal: Calendar,
        val todayPrayerTime: Long
    )

    /**
     * Core Algorithm:
     * 1. Filter currentPrayers to get the 5 obligatory prayers (exclude SUNRISE).
     * 2. Extract HOUR_OF_DAY and MINUTE directly from each PrayerTimeItem's timeMillis using a Calendar,
     *    and apply them to a TODAY Calendar. Sort them chronologically.
     * 3. Apply a 45-minute Jamaat Grace Period (val JAMAAT_GRACE_MILLIS = 45 * 60 * 1000L).
     *    If nowMillis falls strictly within this 45-minute window for any prayer,
     *    set isPrayerInProgress = true and count down to the END of this grace period.
     * 4. If not in a grace period, find the first prayer where todayPrayerTime > nowMillis.
     * 5. MIDNIGHT ROLLOVER FIX: If no prayer is found (i.e., it is 9:23 PM, past Isha's grace period),
     *    the next prayer is explicitly FAJR of TOMORROW. Create a Calendar for Fajr,
     *    add exactly 1 day (add(Calendar.DAY_OF_YEAR, 1)), and set isTomorrow = true.
     * 6. Calculate the remaining HH:MM:SS from targetMillis - nowMillis and push the updated CountdownState.
     */
    @Synchronized
    fun performTick() {
        tickCount++

        // Device time or simulated time
        val nowCal = Calendar.getInstance(pkZone)
        val isSimulated = simulatedTimeMillis != null
        if (isSimulated) {
            // In simulation mode, advance 1 second per tick
            simulatedTimeMillis = (simulatedTimeMillis ?: nowCal.timeInMillis) + 1000L
            nowCal.timeInMillis = simulatedTimeMillis!!
        }
        val nowMillis = nowCal.timeInMillis
        val currentDeviceTimeStr = timeDisplayFormat.format(Date(nowMillis))

        // 1. Filter currentPrayers to get the 5 obligatory prayers (exclude SUNRISE)
        val nonSunrise = currentPrayers.filter { it.type != PrayerType.SUNRISE }
        val prayersToProcess = if (nonSunrise.isNotEmpty()) {
            nonSunrise
        } else {
            // Safe fallback timings if prayers haven't loaded yet
            val makeCal = { h: Int, m: Int ->
                (nowCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            listOf(
                PrayerTimeItem(PrayerType.FAJR, "04:24 AM", makeCal(4, 24)),
                PrayerTimeItem(PrayerType.DHUHR, "12:06 PM", makeCal(12, 6)),
                PrayerTimeItem(PrayerType.ASR, "04:38 PM", makeCal(16, 38)),
                PrayerTimeItem(PrayerType.MAGHRIB, "06:25 PM", makeCal(18, 25)),
                PrayerTimeItem(PrayerType.ISHA, "07:47 PM", makeCal(19, 47))
            )
        }

        // 2. Extract HOUR_OF_DAY and MINUTE directly from each PrayerTimeItem's timeMillis using a Calendar,
        // and apply them to a TODAY Calendar. Sort them chronologically.
        val scheduledPrayers: List<TodayPrayer> = prayersToProcess.map { item ->
            val srcCal = Calendar.getInstance(pkZone).apply { timeInMillis = item.timeMillis }
            val hour = srcCal.get(Calendar.HOUR_OF_DAY)
            val minute = srcCal.get(Calendar.MINUTE)

            val todayCal = (nowCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            TodayPrayer(
                item = item,
                todayCal = todayCal,
                todayPrayerTime = todayCal.timeInMillis
            )
        }.sortedBy { it.todayPrayerTime }

        // 3. Apply a 45-minute Jamaat Grace Period (val JAMAAT_GRACE_MILLIS = 45 * 60 * 1000L).
        // If nowMillis falls strictly within this 45-minute window for any prayer,
        // set isPrayerInProgress = true and count down to the END of this grace period.
        val activeGracePrayer = scheduledPrayers.lastOrNull { prayer ->
            val start = prayer.todayPrayerTime
            val end = start + JAMAAT_GRACE_MILLIS
            nowMillis in start until end
        } ?: run {
            // Check yesterday's Isha if shortly after midnight (e.g. 12:10 AM when Isha was 11:35 PM)
            val ishaPrayer = scheduledPrayers.firstOrNull { it.item.type == PrayerType.ISHA }
            if (ishaPrayer != null) {
                val yIshaCal = (ishaPrayer.todayCal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, -1)
                }
                val yStart = yIshaCal.timeInMillis
                val yEnd = yStart + JAMAAT_GRACE_MILLIS
                if (nowMillis in yStart until yEnd) {
                    TodayPrayer(ishaPrayer.item, yIshaCal, yStart)
                } else null
            } else null
        }

        if (activeGracePrayer != null) {
            val graceEndMillis = activeGracePrayer.todayPrayerTime + JAMAAT_GRACE_MILLIS
            val remainingMillis = (graceEndMillis - nowMillis).coerceAtLeast(0L)

            val hours = (remainingMillis / (1000 * 60 * 60)).toInt()
            val minutes = ((remainingMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
            val seconds = ((remainingMillis % (1000 * 60)) / 1000).toInt()
            val formattedTime = String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)

            val targetItem = activeGracePrayer.item
            val targetName = targetItem.type.displayName
            val targetArabic = targetItem.type.arabicName
            val targetTimeFormatted = targetItem.timeFormatted

            _countdownState.value = CountdownState(
                formattedTime = formattedTime,
                hours = hours,
                minutes = minutes,
                seconds = seconds,
                remainingMillis = remainingMillis,
                nextPrayer = targetItem.copy(
                    timeMillis = activeGracePrayer.todayPrayerTime,
                    isNext = false,
                    isPassed = false
                ),
                isTomorrow = false,
                currentDeviceTime = currentDeviceTimeStr,
                targetPrayerName = targetName,
                targetPrayerArabic = targetArabic,
                targetPrayerTimeFormatted = targetTimeFormatted,
                tickCounter = tickCount,
                isSimulatedMidnight = isSimulated && isSimulatedMidnightActive(),
                isPrayerInProgress = true,
                isSimulatedGracePeriod = isSimulatedGracePeriodActive()
            )
            return
        }

        // 4. If not in a grace period, find the first prayer where todayPrayerTime > nowMillis.
        val upcomingPrayer = scheduledPrayers.firstOrNull { it.todayPrayerTime > nowMillis }

        // 5. MIDNIGHT ROLLOVER FIX:
        // If no prayer is found (i.e., it is 9:23 PM, past Isha's grace period),
        // the next prayer is explicitly FAJR of TOMORROW. Create a Calendar for Fajr,
        // add exactly 1 day (add(Calendar.DAY_OF_YEAR, 1)), and set isTomorrow = true.
        val targetPrayerItem: PrayerTimeItem
        val targetCal: Calendar
        val isTomorrow: Boolean

        if (upcomingPrayer != null) {
            targetPrayerItem = upcomingPrayer.item
            targetCal = upcomingPrayer.todayCal
            isTomorrow = false
        } else {
            val fajr = scheduledPrayers.firstOrNull { it.item.type == PrayerType.FAJR } ?: scheduledPrayers.first()
            targetPrayerItem = fajr.item
            targetCal = (fajr.todayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, 1)
            }
            isTomorrow = true
        }

        // 6. Calculate the remaining HH:MM:SS from targetMillis - nowMillis and push the updated CountdownState.
        val targetMillis = targetCal.timeInMillis
        val remainingMillis = (targetMillis - nowMillis).coerceAtLeast(0L)

        val hours = (remainingMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((remainingMillis % (1000 * 60 * 60)) / (1000 * 60)).toInt()
        val seconds = ((remainingMillis % (1000 * 60)) / 1000).toInt()
        val formattedTime = String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)

        val targetName = targetPrayerItem.type.displayName
        val targetArabic = targetPrayerItem.type.arabicName
        val targetTimeFormatted = targetPrayerItem.timeFormatted

        _countdownState.value = CountdownState(
            formattedTime = formattedTime,
            hours = hours,
            minutes = minutes,
            seconds = seconds,
            remainingMillis = remainingMillis,
            nextPrayer = targetPrayerItem.copy(
                timeMillis = targetMillis,
                isNext = true,
                isPassed = false
            ),
            isTomorrow = isTomorrow,
            currentDeviceTime = currentDeviceTimeStr,
            targetPrayerName = targetName,
            targetPrayerArabic = targetArabic,
            targetPrayerTimeFormatted = targetTimeFormatted,
            tickCounter = tickCount,
            isSimulatedMidnight = isSimulated && isSimulatedMidnightActive(),
            isPrayerInProgress = false,
            isSimulatedGracePeriod = false
        )
    }

    fun stopEngine() {
        activeTimer?.cancel()
        activeTimer = null
        mainHandler?.removeCallbacks(tickRunnable)
        isHandlerRunning = false
    }
}
