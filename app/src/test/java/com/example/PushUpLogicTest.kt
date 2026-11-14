package com.example

import com.example.data.model.DailyPlan
import com.example.data.model.WorkoutSession
import com.example.data.repository.WorkoutRepository
import com.example.notifications.ReminderScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PushUpLogicTest {

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
}
