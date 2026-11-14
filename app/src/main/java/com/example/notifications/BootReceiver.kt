package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.db.AppDatabase
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_TIME_CHANGED
        ) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).workoutDao
                val repository = WorkoutRepository(dao)
                val prefs = repository.getUserPreferencesSync()
                val plans = repository.getAllDailyPlansSync()
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
