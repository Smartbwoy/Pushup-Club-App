package com.example.data.tracking

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MilestoneSnapshot(
    val firstOpenTimestamp: Long = 0L,
    val setupStatus: String = "pending", // "pending", "completed", "skipped"
    val setupTimestamp: Long = 0L,
    val firstWorkoutStartedTimestamp: Long = 0L,
    val firstWorkoutSavedTimestamp: Long = 0L,
    val distinctDaysVisited: Int = 1,
    val totalWorkoutsSaved: Int = 0,
    val sharesInitiated: Int = 0
) {
    val returnVisitsCount: Int
        get() = (distinctDaysVisited - 1).coerceAtLeast(0)
}

/**
 * Privacy-friendly on-device funnel & milestone tracker to measure activation,
 * first workout saved, return visits, and sharing without external trackers.
 */
class MilestoneTracker(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _snapshot = MutableStateFlow(readSnapshot())
    val snapshot: StateFlow<MilestoneSnapshot> = _snapshot.asStateFlow()

    init {
        recordAppOpen()
    }

    private fun readSnapshot(): MilestoneSnapshot {
        val visitedDays = prefs.getStringSet(KEY_VISITED_DAYS, emptySet()) ?: emptySet()
        return MilestoneSnapshot(
            firstOpenTimestamp = prefs.getLong(KEY_FIRST_OPEN_TS, 0L),
            setupStatus = prefs.getString(KEY_SETUP_STATUS, "pending") ?: "pending",
            setupTimestamp = prefs.getLong(KEY_SETUP_TS, 0L),
            firstWorkoutStartedTimestamp = prefs.getLong(KEY_FIRST_WORKOUT_START_TS, 0L),
            firstWorkoutSavedTimestamp = prefs.getLong(KEY_FIRST_WORKOUT_SAVE_TS, 0L),
            distinctDaysVisited = visitedDays.size.coerceAtLeast(1),
            totalWorkoutsSaved = prefs.getInt(KEY_TOTAL_SAVED, 0),
            sharesInitiated = prefs.getInt(KEY_SHARES_INITIATED, 0)
        )
    }

    private fun recordAppOpen() {
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        if (!prefs.contains(KEY_FIRST_OPEN_TS)) {
            editor.putLong(KEY_FIRST_OPEN_TS, now)
            Log.i(TAG, "milestone=first_open ts=$now")
        }

        val todayKey = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(now))
        val existingDays = prefs.getStringSet(KEY_VISITED_DAYS, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (existingDays.add(todayKey)) {
            editor.putStringSet(KEY_VISITED_DAYS, existingDays)
            if (existingDays.size > 1) {
                Log.i(TAG, "milestone=return_visit day_count=${existingDays.size}")
            }
        }
        editor.apply()
        _snapshot.value = readSnapshot()
    }

    fun recordSetupCompleted() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_SETUP_STATUS, "completed")
            .putLong(KEY_SETUP_TS, now)
            .apply()
        Log.i(TAG, "milestone=setup_completed ts=$now")
        _snapshot.value = readSnapshot()
    }

    fun recordSetupSkipped() {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_SETUP_STATUS, "skipped")
            .putLong(KEY_SETUP_TS, now)
            .apply()
        Log.i(TAG, "milestone=setup_skipped ts=$now")
        _snapshot.value = readSnapshot()
    }

    fun recordWorkoutStarted() {
        if (!prefs.contains(KEY_FIRST_WORKOUT_START_TS)) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_FIRST_WORKOUT_START_TS, now).apply()
            Log.i(TAG, "milestone=first_workout_started ts=$now")
            _snapshot.value = readSnapshot()
        }
    }

    fun recordWorkoutSaved() {
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        if (!prefs.contains(KEY_FIRST_WORKOUT_SAVE_TS)) {
            editor.putLong(KEY_FIRST_WORKOUT_SAVE_TS, now)
            Log.i(TAG, "milestone=first_workout_saved ts=$now")
        }
        val nextCount = prefs.getInt(KEY_TOTAL_SAVED, 0) + 1
        editor.putInt(KEY_TOTAL_SAVED, nextCount).apply()
        _snapshot.value = readSnapshot()
    }

    fun recordShareInitiated() {
        val nextCount = prefs.getInt(KEY_SHARES_INITIATED, 0) + 1
        prefs.edit().putInt(KEY_SHARES_INITIATED, nextCount).apply()
        Log.i(TAG, "milestone=share_initiated count=$nextCount")
        _snapshot.value = readSnapshot()
    }

    companion object {
        private const val TAG = "PushUpMilestones"
        private const val PREFS_NAME = "pushup_pro_milestones"
        private const val KEY_FIRST_OPEN_TS = "first_open_ts"
        private const val KEY_SETUP_STATUS = "setup_status"
        private const val KEY_SETUP_TS = "setup_ts"
        private const val KEY_FIRST_WORKOUT_START_TS = "first_workout_start_ts"
        private const val KEY_FIRST_WORKOUT_SAVE_TS = "first_workout_save_ts"
        private const val KEY_VISITED_DAYS = "visited_days"
        private const val KEY_TOTAL_SAVED = "total_saved"
        private const val KEY_SHARES_INITIATED = "shares_initiated"
    }
}
