package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.challenge.ThirtyDayChallengeState
import com.example.data.model.DailyPlan
import com.example.data.model.UserPreferences
import com.example.data.model.WorkoutSession
import com.example.data.tracking.ActiveWorkoutDraft
import com.example.data.tracking.ActiveWorkoutDraftStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pushup Club", appName)
    }

    @Test
    fun `backup export and parse roundtrips user data including preferred counter mode`() {
        val prefs = UserPreferences(
            weeklyTarget = 175,
            voiceCoachEnabled = false,
            notificationsEnabled = true,
            hasCompletedOnboarding = true,
            experienceLevel = "Intermediate",
            preferredCounterMode = UserPreferences.COUNTER_MODE_CAMERA
        )
        val plans = listOf(DailyPlan(1, "Monday", true, "07:30", 50))
        val sessions = listOf(
            WorkoutSession(
                timestamp = 1700000000000L,
                totalPushUps = 42,
                setsCount = 3,
                repsListString = "15,14,13",
                restTimeSeconds = 30,
                programId = "beginner"
            )
        )
        val challenge = ThirtyDayChallengeState(
            isStarted = true,
            initialMaxReps = 18,
            completedDays = 7,
            finalMaxReps = 0
        )
        val multipliers = mapOf("beginner" to 1.2f)

        val json = BackupManager.exportToJson(prefs, plans, sessions, challenge, multipliers)
        val parsed = BackupManager.parseBackupJson(json)
        assertNotNull(parsed)
        assertEquals(175, parsed!!.preferences.weeklyTarget)
        assertEquals("Intermediate", parsed.preferences.experienceLevel)
        assertEquals(UserPreferences.COUNTER_MODE_CAMERA, parsed.preferences.preferredCounterMode)
        assertEquals(1, parsed.dailyPlans.size)
        assertEquals("07:30", parsed.dailyPlans[0].workoutTime)
        assertEquals(1, parsed.sessions.size)
        assertEquals(42, parsed.sessions[0].totalPushUps)
        assertEquals(18, parsed.challengeState.initialMaxReps)
        assertEquals(7, parsed.challengeState.completedDays)
        assertEquals(1.2f, parsed.adaptiveMultipliers["beginner"] ?: 0f, 0.01f)
        assertNull(BackupManager.parseBackupJson("{not-valid-backup"))
    }

    @Test
    fun `active workout draft store persists and expires active workout state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = ActiveWorkoutDraftStore(context)
        store.clear()

        val now = 1_700_000_000_000L
        val draft = ActiveWorkoutDraft(
            workoutState = "RUNNING",
            activeProgramId = "beginner",
            activeSets = listOf(5, 5, 4),
            completedReps = listOf(5, 3, 0),
            currentSetIndex = 1,
            currentRepCount = 3,
            restEndTimeMillis = 0L,
            updatedAtMillis = now
        )
        store.saveDraft(draft)

        val restored = store.loadValidDraft(now + 60_000L)
        assertNotNull(restored)
        assertEquals("RUNNING", restored!!.workoutState)
        assertEquals("beginner", restored.activeProgramId)
        assertEquals(listOf(5, 3, 0), restored.completedReps)
        assertEquals(1, restored.currentSetIndex)
        assertEquals(3, restored.currentRepCount)

        // Expired after 4 hours -> returns null cleanly
        assertNull(store.loadValidDraft(now + 4L * 60L * 60L * 1000L))
    }
}
