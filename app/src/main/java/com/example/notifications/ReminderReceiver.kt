package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dayOfWeek = intent.getIntExtra(ReminderScheduler.EXTRA_DAY_OF_WEEK, -1)
        if (dayOfWeek !in 1..7) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).workoutDao
                val repository = WorkoutRepository(dao)
                val prefs = repository.getUserPreferencesSync()
                val plans = repository.getAllDailyPlansSync()
                val sessions = repository.getAllSessionsSync()
                val matchingPlan = plans.firstOrNull { it.dayOfWeek == dayOfWeek }
                val alreadyTrainedToday = WorkoutRepository.calculateTodayPushUps(sessions) > 0

                if (prefs?.notificationsEnabled == true && matchingPlan?.isEnabled == true && !alreadyTrainedToday) {
                    ReminderScheduler.showReminderNotification(
                        context = context,
                        dayOfWeek = dayOfWeek,
                        dailyTarget = matchingPlan.dailyTarget,
                        workoutTime = matchingPlan.workoutTime
                    )
                }

                // Reschedule next week's reminders
                ReminderScheduler.syncReminders(
                    context = context,
                    plans = plans,
                    notificationsEnabled = prefs?.notificationsEnabled == true
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
