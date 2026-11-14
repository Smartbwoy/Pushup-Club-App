package com.example.data.repository

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
                // Prepopulate
                workoutDao.insertUserPreferences(UserPreferences())
                
                val plans = listOf(
                    DailyPlan(1, "Monday", true, "18:00", 50),
                    DailyPlan(2, "Tuesday", false, "18:00", 50),
                    DailyPlan(3, "Wednesday", true, "18:00", 75),
                    DailyPlan(4, "Thursday", false, "18:00", 50),
                    DailyPlan(5, "Friday", true, "18:00", 100),
                    DailyPlan(6, "Saturday", false, "10:00", 50),
                    DailyPlan(7, "Sunday", false, "10:00", 50)
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

    suspend fun updateChallengeGoal(goal: ChallengeGoal) {
        ensureInitialized()
        workoutDao.updateChallengeGoal(goal)
    }

    suspend fun recalculateProgress() {
        val sessions = workoutDao.getAllSessions().firstOrNull() ?: emptyList()
        val goals = workoutDao.getAllChallengeGoals().firstOrNull() ?: emptyList()
        
        val totalPushUps = sessions.sumOf { it.totalPushUps }
        val maxSingleSession = sessions.maxOfOrNull { it.totalPushUps } ?: 0
        val currentStreak = calculateStreak(sessions)

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

    fun calculateStreak(sessions: List<WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyyMMdd", java.util.Locale.US)
        
        val uniqueDays = sessions.map { sdf.format(java.util.Date(it.timestamp)) }
            .distinct()
            .sortedDescending()
        
        if (uniqueDays.isEmpty()) return 0
        
        val todayStr = sdf.format(java.util.Date())
        val yesterdayStr = sdf.format(java.util.Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))
        
        val lastActiveDay = uniqueDays.first()
        if (lastActiveDay != todayStr && lastActiveDay != yesterdayStr) {
            return 0
        }

        var streak = 0
        var currentCheckMillis = System.currentTimeMillis()
        var checkDayStr = lastActiveDay
        var checkIndex = 0
        
        while (checkIndex < uniqueDays.size) {
            val idx = uniqueDays.indexOf(checkDayStr)
            if (idx != -1) {
                streak++
                currentCheckMillis = sdf.parse(checkDayStr)?.time ?: currentCheckMillis
                checkDayStr = sdf.format(java.util.Date(currentCheckMillis - 24 * 60 * 60 * 1000))
                checkIndex++
            } else {
                break
            }
        }
        
        return streak
    }
}
