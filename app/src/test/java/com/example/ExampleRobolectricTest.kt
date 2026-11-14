package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.challenge.ThirtyDayChallengeState
import com.example.data.model.DailyPlan
import com.example.data.model.UserPreferences
import com.example.data.model.WorkoutSession
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
  fun `backup export and parse roundtrips user data faithfully`() {
    val prefs = UserPreferences(
      weeklyTarget = 175,
      voiceCoachEnabled = false,
      notificationsEnabled = true,
      hasCompletedOnboarding = true,
      experienceLevel = "Intermediate"
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
    assertEquals(1, parsed.dailyPlans.size)
    assertEquals("07:30", parsed.dailyPlans[0].workoutTime)
    assertEquals(1, parsed.sessions.size)
    assertEquals(42, parsed.sessions[0].totalPushUps)
    assertEquals(18, parsed.challengeState.initialMaxReps)
    assertEquals(7, parsed.challengeState.completedDays)
    assertEquals(1.2f, parsed.adaptiveMultipliers["beginner"] ?: 0f, 0.01f)
    assertNull(BackupManager.parseBackupJson("{not-valid-backup"))
  }
}
