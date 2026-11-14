package com.example.data.repository

import com.example.data.backup.BackupPayload
import com.example.data.db.WorkoutDao
import com.example.data.model.ChallengeGoal
import com.example.data.model.DailyPlan
import com.example.data.model.UserPreferences
import com.example.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WorkoutRepository(private val workoutDao: WorkoutDao) {
    private val initMutex = Mutex()
    private var isInitialized = false

    val allSessions: Flow<List<WorkoutSession>> = workoutDao.getAllSessions()
    val allDailyPlans: Flow<List<DailyPlan>> = workoutDao.getAllDailyPlans()
    val allChallengeGoals: Flow<List<ChallengeGoal>> = workoutDao.getAllChallengeGoals()
    val userPreferencesFlow: Flow<UserPreferences?> = workoutDao.getUserPreferencesFlow()

    suspend fun ensureInitialized() {
        if (isInitialized) return
        initMutex.withLock {
            if (isInitialized) return

            val preferences = workoutDao.getUserPreferences()
            if (preferences == null) {
                // Prepopulate with beginner-friendly defaults; onboarding lets users customize or skip
                workoutDao.insertUserPreferences(
                    UserPreferences(
                        weeklyTarget = 100,
                        voiceCoachEnabled = true,
                        notificationsEnabled = false,
                        hasCompletedOnboarding = false,
                        experienceLevel = "Beginner"
                    )
                )

                val plans = listOf(
                    DailyPlan(1, "Monday", true, "18:00", 35),
                    DailyPlan(2, "Tuesday", false, "18:00", 35),
                    DailyPlan(3, "Wednesday", true, "18:00", 35),
                    DailyPlan(4, "Thursday", false, "18:00", 35),
                    DailyPlan(5, "Friday", true, "18:00", 30),
                    DailyPlan(6, "Saturday", false, "10:00", 30),
                    DailyPlan(7, "Sunday", false, "10:00", 30)
                )
                workoutDao.insertDailyPlans(plans)

                val goals = listOf(
                    ChallengeGoal("goal_total_min", "Quick Start: 10 Push-ups", "Log a single session with at least 10 push-ups", 10, 0, false, "BEGINNER", "SINGLE_SESSION", "star"),
                    ChallengeGoal("goal_total_100", "Centurion: 100 Total Push-ups", "Reach a total of 100 cumulative push-ups", 100, 0, false, "BEGINNER", "TOTAL_PUSHUPS", "fitness_center"),
                    ChallengeGoal("goal_streak_3", "Dynamic Trio: 3-Day Streak", "Achieve 3 consecutive active check-ins", 3, 0, false, "BEGINNER", "STREAK", "local_fire_department"),

                    ChallengeGoal("goal_single_50", "Half Century: 50 Reps", "Complete a session containing 50 push-ups", 50, 0, false, "INTERMEDIATE", "SINGLE_SESSION", "star_half"),
                    ChallengeGoal("goal_total_1000", "Iron Milestone: 1,000 Push-ups", "Reach 1,000 total push-ups done", 1000, 0, false, "INTERMEDIATE", "TOTAL_PUSHUPS", "workspace_premium"),
                    ChallengeGoal("goal_streak_7", "Consistency King: 7-Day Streak", "Keep the fire alive for 7 days consecutive", 7, 0, false, "INTERMEDIATE", "STREAK", "local_fire_department"),

                    ChallengeGoal("goal_single_100", "Century Club: 100 Reps", "Complete 100 push-ups in a single session", 100, 0, false, "ADVANCED", "SINGLE_SESSION", "military_tech"),
                    ChallengeGoal("goal_total_5000", "Push-up Titan: 5,000 Total", "Achieve a total of 5,000 lifetime push-ups", 5000, 0, false, "ADVANCED", "TOTAL_PUSHUPS", "emoji_events"),
                    ChallengeGoal("goal_streak_30", "Unstoppable: 30-Day Challenge", "Maintain a workout log streak for 30 consecutive days", 30, 0, false, "ADVANCED", "STREAK", "trending_up")
                )
                workoutDao.insertChallengeGoals(goals)
            }
            isInitialized = true
        }
    }

    suspend fun getAllDailyPlansSync(): List<DailyPlan> {
        ensureInitialized()
        return workoutDao.getAllDailyPlansSync()
    }

    suspend fun getAllSessionsSync(): List<WorkoutSession> {
        ensureInitialized()
        return workoutDao.getAllSessionsSync()
    }

    suspend fun getUserPreferencesSync(): UserPreferences? {
        ensureInitialized()
        return workoutDao.getUserPreferences()
    }

    suspend fun restoreFromBackup(payload: BackupPayload) {
        ensureInitialized()
        workoutDao.insertUserPreferences(payload.preferences.copy(id = 1, hasCompletedOnboarding = true))
        if (payload.dailyPlans.isNotEmpty()) {
            workoutDao.insertDailyPlans(payload.dailyPlans)
        }
        workoutDao.clearAllSessions()
        if (payload.sessions.isNotEmpty()) {
            workoutDao.insertSessions(payload.sessions)
        }
        recalculateProgress()
    }

    suspend fun applyTimeToEnabledDays(timeStr: String) {
        ensureInitialized()
        val updated = workoutDao.getAllDailyPlansSync().map { plan ->
            if (plan.isEnabled) plan.copy(workoutTime = timeStr) else plan
        }
        workoutDao.insertDailyPlans(updated)
    }

    suspend fun logSession(session: WorkoutSession) {
        ensureInitialized()
        workoutDao.insertSession(session)
        recalculateProgress()
    }

    suspend fun deleteSession(session: WorkoutSession) {
        ensureInitialized()
        workoutDao.deleteSession(session)
        recalculateProgress()
    }

    suspend fun clearAllData() {
        ensureInitialized()
        workoutDao.clearAllSessions()
        val currentGoals = workoutDao.getAllChallengeGoals().firstOrNull() ?: emptyList()
        val resetGoals = currentGoals.map { it.copy(currentValue = 0, isCompleted = false) }
        workoutDao.insertChallengeGoals(resetGoals)
    }

    suspend fun updateDailyPlan(plan: DailyPlan) {
        ensureInitialized()
        workoutDao.updateDailyPlan(plan)
    }

    suspend fun updateUserPreferences(prefs: UserPreferences) {
        ensureInitialized()
        workoutDao.insertUserPreferences(prefs)
    }

    suspend fun applyOnboardingSetup(
        experienceLevel: String,
        weeklyTarget: Int,
        enabledDays: Set<Int>
    ) {
        ensureInitialized()
        val current = workoutDao.getUserPreferences() ?: UserPreferences()
        workoutDao.insertUserPreferences(
            current.copy(
                weeklyTarget = weeklyTarget.coerceAtLeast(10),
                experienceLevel = experienceLevel,
                hasCompletedOnboarding = true
            )
        )

        val existingPlans = workoutDao.getAllDailyPlansSync()
        val activeDayCount = enabledDays.size.coerceAtLeast(1)
        val perDayTargets = splitRepsAcrossSets(weeklyTarget.coerceAtLeast(10), activeDayCount)
        var targetIdx = 0

        val updatedPlans = existingPlans.map { plan ->
            val enabled = plan.dayOfWeek in enabledDays
            if (enabled) {
                val daily = perDayTargets.getOrElse(targetIdx) { plan.dailyTarget }
                targetIdx++
                plan.copy(isEnabled = true, dailyTarget = daily)
            } else {
                plan.copy(isEnabled = false)
            }
        }
        if (updatedPlans.isNotEmpty()) {
            workoutDao.insertDailyPlans(updatedPlans)
        }
    }

    suspend fun markOnboardingCompleted() {
        ensureInitialized()
        val current = workoutDao.getUserPreferences() ?: UserPreferences()
        workoutDao.insertUserPreferences(current.copy(hasCompletedOnboarding = true))
    }

    suspend fun updateChallengeGoal(goal: ChallengeGoal) {
        ensureInitialized()
        workoutDao.updateChallengeGoal(goal)
    }

    suspend fun recalculateProgress() {
        val sessions = workoutDao.getAllSessions().firstOrNull() ?: emptyList()
        val goals = workoutDao.getAllChallengeGoals().firstOrNull() ?: emptyList()
        val plans = workoutDao.getAllDailyPlansSync()

        val totalPushUps = sessions.sumOf { it.totalPushUps }
        val maxSingleSession = sessions.maxOfOrNull { it.totalPushUps } ?: 0
        val currentStreak = calculateStreak(sessions, plans)

        val updatedGoals = goals.map { goal ->
            val newValue = when (goal.type) {
                "TOTAL_PUSHUPS" -> totalPushUps
                "SINGLE_SESSION" -> maxSingleSession
                "STREAK" -> currentStreak
                else -> goal.currentValue
            }
            val completed = newValue >= goal.targetValue
            goal.copy(currentValue = newValue, isCompleted = completed)
        }

        workoutDao.insertChallengeGoals(updatedGoals)
    }

    /**
     * Calculates the current unbroken workout streak using DST-safe Calendar arithmetic.
     * When [plans] has active days configured (e.g. Mon/Wed/Fri), scheduled rest days
     * do not break the streak; missing a scheduled training day breaks it.
     */
    fun calculateStreak(
        sessions: List<WorkoutSession>,
        plans: List<DailyPlan> = emptyList(),
        referenceTimeMillis: Long = System.currentTimeMillis()
    ): Int {
        if (sessions.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)

        val loggedDayKeys = sessions.map { sdf.format(Date(it.timestamp)) }.toSet()
        val earliestDayTime = sessions.minOf { it.timestamp }
        val earliestCal = Calendar.getInstance().apply {
            timeInMillis = earliestDayTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val scheduledCalendarDays = plans
            .filter { it.isEnabled }
            .map { planDayToCalendarDay(it.dayOfWeek) }
            .toSet()

        val checkCal = Calendar.getInstance().apply {
            timeInMillis = referenceTimeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var streak = 0
        var seenFirstWorkout = false
        var safetyDays = 0

        while (checkCal.timeInMillis >= earliestCal.timeInMillis && safetyDays < 3660) {
            val dayKey = sdf.format(checkCal.time)
            val isToday = safetyDays == 0
            val calDayOfWeek = checkCal.get(Calendar.DAY_OF_WEEK)
            val isScheduledDay = scheduledCalendarDays.isEmpty() || (calDayOfWeek in scheduledCalendarDays)

            if (dayKey in loggedDayKeys) {
                streak++
                seenFirstWorkout = true
            } else if (!seenFirstWorkout) {
                // Grace for today if user hasn't trained yet today, and for unscheduled rest days
                if (!isToday && isScheduledDay) {
                    return 0
                }
            } else {
                // Already in an active streak: only a missed scheduled day breaks it
                if (isScheduledDay) {
                    break
                }
            }

            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            safetyDays++
        }

        return streak
    }

    private fun planDayToCalendarDay(planDayOfWeek: Int): Int {
        return when (planDayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }
    }

    companion object {
        /**
         * Sums push-ups from the last 7 calendar days (today and the preceding 6 days),
         * matching the HistoryScreen 7-day chart window.
         */
        fun calculateWeeklyPushUps(
            sessions: List<WorkoutSession>,
            referenceTimeMillis: Long = System.currentTimeMillis()
        ): Int {
            if (sessions.isEmpty()) return 0
            val keySdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val validDayKeys = (0..6).map { offset ->
                val cal = Calendar.getInstance().apply {
                    timeInMillis = referenceTimeMillis
                    add(Calendar.DAY_OF_YEAR, -offset)
                }
                keySdf.format(cal.time)
            }.toSet()

            return sessions
                .filter { keySdf.format(Date(it.timestamp)) in validDayKeys }
                .sumOf { it.totalPushUps }
        }

        fun calculateTodayPushUps(
            sessions: List<WorkoutSession>,
            referenceTimeMillis: Long = System.currentTimeMillis()
        ): Int {
            if (sessions.isEmpty()) return 0
            val keySdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val todayKey = keySdf.format(Date(referenceTimeMillis))
            return sessions
                .filter { keySdf.format(Date(it.timestamp)) == todayKey }
                .sumOf { it.totalPushUps }
        }

        /**
         * Learns the user's most frequent training hour once at least 3 sessions are logged.
         * Returns formatted "HH:00" string or null if not enough history.
         */
        fun detectPreferredTrainingTime(sessions: List<WorkoutSession>): String? {
            if (sessions.size < 3) return null
            val hourCounts = sessions.groupingBy { session ->
                Calendar.getInstance().apply { timeInMillis = session.timestamp }
                    .get(Calendar.HOUR_OF_DAY)
            }.eachCount()

            val topHour = hourCounts.maxByOrNull { it.value }?.key ?: return null
            return "${topHour.toString().padStart(2, '0')}:00"
        }

        /**
         * Evenly distributes [totalReps] across [setsCount] sets while preserving any remainder
         * across the first sets so the sum of returned set counts always equals [totalReps].
         * Example: splitRepsAcrossSets(17, 3) -> [6, 6, 5].
         */
        fun splitRepsAcrossSets(totalReps: Int, setsCount: Int): List<Int> {
            if (totalReps <= 0) return emptyList()
            val safeSets = setsCount.coerceIn(1, totalReps)
            val base = totalReps / safeSets
            val remainder = totalReps % safeSets
            return List(safeSets) { index ->
                base + if (index < remainder) 1 else 0
            }
        }
    }
}
