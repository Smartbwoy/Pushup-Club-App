package com.example

import com.example.data.adaptive.AdaptiveProgramManager
import com.example.data.challenge.ThirtyDayChallengeManager
import com.example.data.db.WorkoutDao
import com.example.data.model.ChallengeGoal
import com.example.data.model.DailyPlan
import com.example.data.model.TrainingProgram
import com.example.data.model.UserPreferences
import com.example.data.model.WorkoutSession
import com.example.data.repository.WorkoutRepository
import com.example.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PushUpLogicTest {

    private class FakeWorkoutDao : WorkoutDao {
        override fun getAllSessions(): Flow<List<WorkoutSession>> = emptyFlow()
        override suspend fun getAllSessionsSync(): List<WorkoutSession> = emptyList()
        override suspend fun insertSession(session: WorkoutSession) {}
        override suspend fun insertSessions(sessions: List<WorkoutSession>) {}
        override suspend fun deleteSession(session: WorkoutSession) {}
        override suspend fun clearAllSessions() {}
        override fun getAllDailyPlans(): Flow<List<DailyPlan>> = emptyFlow()
        override suspend fun getAllDailyPlansSync(): List<DailyPlan> = emptyList()
        override suspend fun insertDailyPlans(plans: List<DailyPlan>) {}
        override suspend fun updateDailyPlan(plan: DailyPlan) {}
        override fun getUserPreferencesFlow(): Flow<UserPreferences?> = emptyFlow()
        override suspend fun getUserPreferences(): UserPreferences? = null
        override suspend fun insertUserPreferences(preferences: UserPreferences) {}
        override fun getAllChallengeGoals(): Flow<List<ChallengeGoal>> = emptyFlow()
        override suspend fun insertChallengeGoals(goals: List<ChallengeGoal>) {}
        override suspend fun updateChallengeGoal(goal: ChallengeGoal) {}
    }

    @Test
    fun `splitRepsAcrossSets preserves remainder so sum matches total`() {
        val split17Over3 = WorkoutRepository.splitRepsAcrossSets(17, 3)
        assertEquals(listOf(6, 6, 5), split17Over3)
        assertEquals(17, split17Over3.sum())

        val split10Over3 = WorkoutRepository.splitRepsAcrossSets(10, 3)
        assertEquals(listOf(4, 3, 3), split10Over3)
        assertEquals(10, split10Over3.sum())

        val split2Over5 = WorkoutRepository.splitRepsAcrossSets(2, 5)
        assertEquals(listOf(1, 1), split2Over5)
        assertEquals(2, split2Over5.sum())
    }

    @Test
    fun `calculateWeeklyPushUps sums only last 7 calendar days`() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 14, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = nowCal.timeInMillis
        val dayMs = 24L * 60 * 60 * 1000

        val sessions = listOf(
            WorkoutSession(totalPushUps = 25, setsCount = 3, repsListString = "9,8,8", restTimeSeconds = 30, timestamp = now),
            WorkoutSession(totalPushUps = 30, setsCount = 3, repsListString = "10,10,10", restTimeSeconds = 30, timestamp = now - 6 * dayMs),
            WorkoutSession(totalPushUps = 100, setsCount = 4, repsListString = "25,25,25,25", restTimeSeconds = 30, timestamp = now - 8 * dayMs),
            WorkoutSession(totalPushUps = 200, setsCount = 5, repsListString = "40,40,40,40,40", restTimeSeconds = 30, timestamp = now - 30 * dayMs)
        )

        val weeklyTotal = WorkoutRepository.calculateWeeklyPushUps(sessions, now)
        assertEquals(55, weeklyTotal)
    }

    @Test
    fun `calculateStreak preserves streak across scheduled rest days and breaks on missed scheduled day`() {
        val repo = WorkoutRepository(FakeWorkoutDao())
        val monWedFriPlan = listOf(
            DailyPlan(1, "Monday", true, "18:00", 35),
            DailyPlan(2, "Tuesday", false, "18:00", 35),
            DailyPlan(3, "Wednesday", true, "18:00", 35),
            DailyPlan(4, "Thursday", false, "18:00", 35),
            DailyPlan(5, "Friday", true, "18:00", 30),
            DailyPlan(6, "Saturday", false, "10:00", 30),
            DailyPlan(7, "Sunday", false, "10:00", 30)
        )

        // Reference: Wednesday Nov 11, 2026 (before today's workout)
        val wedRef = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 11, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val monNov9 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 9, 18, 0, 0)
        }.timeInMillis
        val friNov6 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 6, 18, 0, 0)
        }.timeInMillis

        // Trained Friday + Monday, skipped Sat/Sun/Tue (rest days): streak stays 2!
        assertEquals(
            2,
            repo.calculateStreak(
                sessions = listOf(
                    WorkoutSession(totalPushUps = 35, setsCount = 3, repsListString = "12,12,11", restTimeSeconds = 30, timestamp = monNov9),
                    WorkoutSession(totalPushUps = 30, setsCount = 3, repsListString = "10,10,10", restTimeSeconds = 30, timestamp = friNov6)
                ),
                plans = monWedFriPlan,
                referenceTimeMillis = wedRef
            )
        )

        // Reference: Thursday Nov 12, 2026 — missed Wednesday Nov 11 (scheduled day) -> streak resets to 0
        val thuRef = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals(
            0,
            repo.calculateStreak(
                sessions = listOf(
                    WorkoutSession(totalPushUps = 35, setsCount = 3, repsListString = "12,12,11", restTimeSeconds = 30, timestamp = monNov9)
                ),
                plans = monWedFriPlan,
                referenceTimeMillis = thuRef
            )
        )
    }

    @Test
    fun `reminder time parsing and scheduling compute future trigger`() {
        assertEquals(18 to 30, ReminderScheduler.parseTimeOrNull("18:30"))
        assertNull(ReminderScheduler.parseTimeOrNull("25:99"))
        assertNull(ReminderScheduler.parseTimeOrNull("invalid"))

        val now = System.currentTimeMillis()
        val nextTrigger = ReminderScheduler.computeNextTriggerMillis(
            dayOfWeek = 1,
            timeStr = "18:00",
            nowMillis = now
        )
        assertTrue(nextTrigger > now)

        val plans = listOf(
            DailyPlan(1, "Monday", true, "18:00", 50),
            DailyPlan(2, "Tuesday", false, "18:00", 50)
        )
        val summary = ReminderScheduler.getNextScheduledReminderSummary(plans, notificationsEnabled = true, nowMillis = now)
        assertNotNull(summary)
        assertEquals("Mon at 18:00", summary)
        assertNull(ReminderScheduler.getNextScheduledReminderSummary(plans, notificationsEnabled = false, nowMillis = now))
    }

    @Test
    fun `thirty day challenge scales from baseline with recovery days`() {
        val day1 = ThirtyDayChallengeManager.buildSetsForDay(initialMaxReps = 10, dayNumber = 1)
        val day5Recovery = ThirtyDayChallengeManager.buildSetsForDay(initialMaxReps = 10, dayNumber = 5)
        val day4 = ThirtyDayChallengeManager.buildSetsForDay(initialMaxReps = 10, dayNumber = 4)
        val day30 = ThirtyDayChallengeManager.buildSetsForDay(initialMaxReps = 10, dayNumber = 30)

        assertTrue(day30.sum() > day1.sum() * 2)
        assertTrue(day5Recovery.sum() < day4.sum())
        assertEquals(14, ThirtyDayChallengeManager.parseChallengeDay("${ThirtyDayChallengeManager.PROGRAM_ID_PREFIX}14"))
        assertNull(ThirtyDayChallengeManager.parseChallengeDay("beginner"))
    }

    @Test
    fun `adaptive program increases or eases targets based on performance`() {
        val base = TrainingProgram.programs.first() // 5,5,4,4,3 -> 21
        val nextUp = AdaptiveProgramManager.computeNextMultiplier(1.0f, targetTotalReps = 21, completedTotalReps = 26)
        assertEquals(1.1f, nextUp, 0.01f)

        val adaptedUp = AdaptiveProgramManager.adaptProgram(base, nextUp)
        assertEquals("ADAPTED +10%", adaptedUp.statusBadge)
        assertTrue(adaptedUp.program.sets.sum() > base.sets.sum())

        val nextDown = AdaptiveProgramManager.computeNextMultiplier(1.0f, targetTotalReps = 21, completedTotalReps = 10)
        assertEquals(0.9f, nextDown, 0.01f)

        val adaptedDown = AdaptiveProgramManager.adaptProgram(base, nextDown)
        assertEquals("EASED -10%", adaptedDown.statusBadge)
        assertTrue(adaptedDown.program.sets.sum() < base.sets.sum())
    }

    @Test
    fun `detectPreferredTrainingTime finds most common workout hour and today reps`() {
        fun sessionAtHour(dayOffset: Int, hour: Int, reps: Int): WorkoutSession {
            val cal = Calendar.getInstance().apply {
                set(2026, Calendar.NOVEMBER, 14, hour, 15, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, -dayOffset)
            }
            return WorkoutSession(
                timestamp = cal.timeInMillis,
                totalPushUps = reps,
                setsCount = 2,
                repsListString = "$reps",
                restTimeSeconds = 30
            )
        }

        // Fewer than 3 sessions returns null
        assertNull(WorkoutRepository.detectPreferredTrainingTime(listOf(sessionAtHour(0, 7, 20))))

        val list = listOf(
            sessionAtHour(0, 7, 25),
            sessionAtHour(1, 7, 30),
            sessionAtHour(2, 18, 20),
            sessionAtHour(3, 7, 35)
        )
        assertEquals("07:00", WorkoutRepository.detectPreferredTrainingTime(list))

        val refToday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 14, 20, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(25, WorkoutRepository.calculateTodayPushUps(list, refToday))
    }

    @Test
    fun `optical rep detector calibrates then counts dip without double counting`() {
        val detector = com.example.camera.OpticalRepDetector(cooldownMs = 550L, calibrationFramesRequired = 8)
        var t = 1000L

        // 8 bright ambient calibration frames (~180 luma); frames 1..7 are calibrating and frame 8 completes calibration
        repeat(8) { idx ->
            val res = detector.processFrameLuminance(180f, t)
            if (idx < 7) {
                assertTrue(res.isCalibrating)
                assertEquals(false, res.calibrationJustCompleted)
            } else {
                assertEquals(false, res.isCalibrating)
                assertTrue(res.calibrationJustCompleted)
            }
            assertEquals(false, res.repDetected)
            t += 50L
        }

        // Staying bright at top position does not count
        val topRes = detector.processFrameLuminance(178f, t)
        assertEquals(false, topRes.isCalibrating)
        assertEquals(false, topRes.repDetected)
        t += 100L

        // Drop down (chest shadows camera -> ~80 luma): counts rep #1
        val downRes1 = detector.processFrameLuminance(80f, t)
        assertEquals(true, downRes1.isDownPosition)
        assertEquals(true, downRes1.repDetected)
        t += 100L

        // Staying down at bottom does not double-count even after cooldown passes
        t += 700L
        val holdDownRes = detector.processFrameLuminance(75f, t)
        assertEquals(true, holdDownRes.isDownPosition)
        assertEquals(false, holdDownRes.repDetected)

        // Rise back up to top position (~178 luma) -> resets hysteresis
        t += 100L
        val recoverRes = detector.processFrameLuminance(178f, t)
        assertEquals(false, recoverRes.isDownPosition)
        assertEquals(false, recoverRes.repDetected)

        // Verify fast dip inside cooldown (<550ms) is ignored
        val fastDetector = com.example.camera.OpticalRepDetector(cooldownMs = 550L, calibrationFramesRequired = 8)
        var ft = 0L
        repeat(8) {
            fastDetector.processFrameLuminance(180f, ft)
            ft += 30L
        }
        assertEquals(true, fastDetector.processFrameLuminance(80f, 300L).repDetected)
        fastDetector.processFrameLuminance(180f, 400L)
        assertEquals(false, fastDetector.processFrameLuminance(80f, 600L).repDetected)
        fastDetector.processFrameLuminance(180f, 700L)
        assertEquals(true, fastDetector.processFrameLuminance(80f, 1000L).repDetected)
    }

    @Test
    fun `club leaderboard sorts descending by weekly reps then total reps`() {
        val club = com.example.data.cloud.Club(
            id = "ABC234",
            name = "Evening Crew",
            joinCode = "ABC234",
            ownerId = "u1",
            memberUids = listOf("u1", "u2", "u3"),
            memberStats = mapOf(
                "u1" to com.example.data.cloud.ClubMemberStats("Alice", weeklyPushUps = 120, totalPushUps = 500, streakDays = 3, personalBest = 30),
                "u2" to com.example.data.cloud.ClubMemberStats("Bob", weeklyPushUps = 150, totalPushUps = 400, streakDays = 5, personalBest = 35),
                "u3" to com.example.data.cloud.ClubMemberStats("Cara", weeklyPushUps = 120, totalPushUps = 650, streakDays = 6, personalBest = 40)
            )
        )
        val rows = com.example.ui.ClubViewModel.buildLeaderboardRows(club, currentUserId = "u3")
        assertEquals(listOf("Bob", "Cara", "Alice"), rows.map { it.displayName })
        assertEquals(true, rows[1].isCurrentUser)
        assertEquals(true, rows[2].isOwner)
    }

    @Test
    fun `optical rep detector supports setup countdown and flags too dark lighting`() {
        val detector = com.example.camera.OpticalRepDetector(
            cooldownMs = 550L,
            calibrationFramesRequired = 4,
            warmupMs = 2000L,
            lowLightThreshold = 28f
        )

        // Warmup frames at 0ms and 1100ms return countdown seconds without calibrating yet
        val warm1 = detector.processFrameLuminance(160f, 0L)
        assertTrue(warm1.isWarmingUp)
        assertEquals(2, warm1.countdownSecondsLeft)
        assertEquals(false, warm1.repDetected)

        val warm2 = detector.processFrameLuminance(160f, 1100L)
        assertTrue(warm2.isWarmingUp)
        assertEquals(1, warm2.countdownSecondsLeft)

        // Warmup completes at 2000ms -> 4 calibration frames finish at 4th frame
        repeat(3) { idx ->
            val cal = detector.processFrameLuminance(18f, 2000L + idx * 50L)
            assertEquals(false, cal.isWarmingUp)
            assertTrue(cal.isCalibrating)
            assertEquals(false, cal.calibrationJustCompleted)
        }
        val calDone = detector.processFrameLuminance(18f, 2200L)
        assertEquals(false, calDone.isCalibrating)
        assertTrue(calDone.calibrationJustCompleted)
        assertTrue(calDone.isTooDark)
    }
}
