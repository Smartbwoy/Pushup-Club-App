package com.example.data.challenge

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class ThirtyDayChallengeState(
    val isStarted: Boolean = false,
    val initialMaxReps: Int = 0,
    val completedDays: Int = 0, // 0..30
    val finalMaxReps: Int = 0
) {
    val currentDay: Int
        get() = (completedDays + 1).coerceAtMost(30)

    val isCompleted: Boolean
        get() = isStarted && completedDays >= 30
}

class ThirtyDayChallengeManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(readState())
    val state: StateFlow<ThirtyDayChallengeState> = _state.asStateFlow()

    private fun readState(): ThirtyDayChallengeState {
        return ThirtyDayChallengeState(
            isStarted = prefs.getBoolean(KEY_IS_STARTED, false),
            initialMaxReps = prefs.getInt(KEY_INITIAL_MAX, 0),
            completedDays = prefs.getInt(KEY_COMPLETED_DAYS, 0),
            finalMaxReps = prefs.getInt(KEY_FINAL_MAX, 0)
        )
    }

    fun startChallenge(initialMaxReps: Int) {
        val safeMax = initialMaxReps.coerceIn(1, 200)
        prefs.edit()
            .putBoolean(KEY_IS_STARTED, true)
            .putInt(KEY_INITIAL_MAX, safeMax)
            .putInt(KEY_COMPLETED_DAYS, 0)
            .putInt(KEY_FINAL_MAX, 0)
            .apply()
        _state.value = readState()
    }

    fun recordDayCompleted(dayNumber: Int, totalRepsDone: Int) {
        val current = readState()
        if (!current.isStarted) return
        val nextCompleted = maxOf(current.completedDays, dayNumber.coerceIn(1, 30))
        val editor = prefs.edit().putInt(KEY_COMPLETED_DAYS, nextCompleted)
        if (dayNumber >= 30 && totalRepsDone > 0) {
            editor.putInt(KEY_FINAL_MAX, totalRepsDone)
        }
        editor.apply()
        _state.value = readState()
    }

    fun recordFinalRetest(finalMaxReps: Int) {
        val safeMax = finalMaxReps.coerceIn(1, 300)
        prefs.edit()
            .putBoolean(KEY_IS_STARTED, true)
            .putInt(KEY_COMPLETED_DAYS, 30)
            .putInt(KEY_FINAL_MAX, safeMax)
            .apply()
        _state.value = readState()
    }

    fun restoreState(newState: ThirtyDayChallengeState) {
        prefs.edit()
            .putBoolean(KEY_IS_STARTED, newState.isStarted)
            .putInt(KEY_INITIAL_MAX, newState.initialMaxReps)
            .putInt(KEY_COMPLETED_DAYS, newState.completedDays.coerceIn(0, 30))
            .putInt(KEY_FINAL_MAX, newState.finalMaxReps)
            .apply()
        _state.value = readState()
    }

    fun resetChallenge() {
        prefs.edit().clear().apply()
        _state.value = ThirtyDayChallengeState()
    }

    companion object {
        private const val PREFS_NAME = "pushup_club_30day_challenge"
        private const val KEY_IS_STARTED = "is_started"
        private const val KEY_INITIAL_MAX = "initial_max_reps"
        private const val KEY_COMPLETED_DAYS = "completed_days"
        private const val KEY_FINAL_MAX = "final_max_reps"
        const val PROGRAM_ID_PREFIX = "challenge_30_day_"

        fun parseChallengeDay(programId: String?): Int? {
            if (programId == null || !programId.startsWith(PROGRAM_ID_PREFIX)) return null
            return programId.removePrefix(PROGRAM_ID_PREFIX).toIntOrNull()
        }

        /**
         * Builds a 4-set (or 5-set) daily workout scaled from [initialMaxReps] across 30 days.
         * Every 5th day is a lighter active-recovery day (65% volume) before ramping up again.
         */
        fun buildSetsForDay(initialMaxReps: Int, dayNumber: Int): List<Int> {
            val safeMax = initialMaxReps.coerceAtLeast(3)
            val clampedDay = dayNumber.coerceIn(1, 30)

            // Progress factor ramps from 1.0x on Day 1 to ~2.6x on Day 30
            val baseProgression = 1.0f + ((clampedDay - 1) / 29f) * 1.6f
            val isRecoveryDay = clampedDay % 5 == 0 && clampedDay < 30
            val multiplier = if (isRecoveryDay) baseProgression * 0.65f else baseProgression

            val dailyTotal = (safeMax * 2.2f * multiplier).roundToInt().coerceAtLeast(10)
            val setWeights = listOf(0.28f, 0.26f, 0.24f, 0.22f)
            return setWeights.map { weight ->
                (dailyTotal * weight).roundToInt().coerceAtLeast(2)
            }
        }
    }
}
