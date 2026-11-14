package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ChallengeGoal
import com.example.data.model.DailyPlan
import com.example.data.model.UserPreferences
import com.example.data.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // Workout Sessions
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession)

    @Delete
    suspend fun deleteSession(session: WorkoutSession)

    @Query("DELETE FROM workout_sessions")
    suspend fun clearAllSessions()

    // Daily Plans (Weekly Scheduler)
    @Query("SELECT * FROM daily_plans ORDER BY dayOfWeek ASC")
    fun getAllDailyPlans(): Flow<List<DailyPlan>>

    @Query("SELECT * FROM daily_plans ORDER BY dayOfWeek ASC")
    suspend fun getAllDailyPlansSync(): List<DailyPlan>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPlans(plans: List<DailyPlan>)

    @Update
    suspend fun updateDailyPlan(plan: DailyPlan)

    // User Preferences
    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    fun getUserPreferencesFlow(): Flow<UserPreferences?>

    @Query("SELECT * FROM user_preferences WHERE id = 1 LIMIT 1")
    suspend fun getUserPreferences(): UserPreferences?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPreferences(preferences: UserPreferences)

    // Challenge Goals
    @Query("SELECT * FROM challenge_goals")
    fun getAllChallengeGoals(): Flow<List<ChallengeGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallengeGoals(goals: List<ChallengeGoal>)

    @Update
    suspend fun updateChallengeGoal(goal: ChallengeGoal)
}
