package com.example.data.cloud

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase
import com.example.data.repository.WorkoutRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.concurrent.TimeUnit

/**
 * Periodically syncs the user's rolling 7-day push-up count and lifetime stats to their clubs
 * so leaderboard totals stay accurate even when the user hasn't opened the Clubs tab today.
 */
class ClubSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val currentUser = Firebase.auth.currentUser ?: return Result.success()
        return try {
            val dao = AppDatabase.getInstance(applicationContext).workoutDao
            val workoutRepo = WorkoutRepository(dao)
            val sessions = workoutRepo.getAllSessionsSync()
            val plans = workoutRepo.getAllDailyPlansSync()

            val nicknameStore = ClubNicknameStore(applicationContext)
            val stats = ClubMemberStats(
                displayName = nicknameStore.resolveDisplayName(currentUser.uid),
                weeklyPushUps = WorkoutRepository.calculateWeeklyPushUps(sessions).toLong(),
                totalPushUps = sessions.sumOf { it.totalPushUps }.toLong(),
                streakDays = workoutRepo.calculateStreak(sessions, plans).toLong(),
                personalBest = (sessions.maxOfOrNull { it.totalPushUps } ?: 0).toLong()
            )

            val clubRepo = ClubRepository(applicationContext)
            clubRepo.syncStatsToUserAndClubs(stats).fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() }
            )
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "pushup_club_daily_leaderboard_sync"

        fun scheduleDailySync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<ClubSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
