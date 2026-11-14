package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.DailyPlan
import java.util.Calendar

object ReminderScheduler {
    const val CHANNEL_ID = "pushup_workout_reminders"
    const val EXTRA_DAY_OF_WEEK = "extra_day_of_week"
    const val EXTRA_DAILY_TARGET = "extra_daily_target"
    const val EXTRA_WORKOUT_TIME = "extra_workout_time"
    private const val REQUEST_CODE_BASE = 1000

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Training Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Scheduled reminders for your planned push-up training days."
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun syncReminders(
        context: Context,
        plans: List<DailyPlan>,
        notificationsEnabled: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        createNotificationChannel(context)

        // Cancel all 7 day slots first so stale alarms are cleared
        for (day in 1..7) {
            val cancelIntent = buildAlarmPendingIntent(
                context = context,
                dayOfWeek = day,
                dailyTarget = 0,
                workoutTime = "18:00",
                flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (cancelIntent != null) {
                alarmManager.cancel(cancelIntent)
                cancelIntent.cancel()
            }
        }

        if (!notificationsEnabled || !hasNotificationPermission(context)) {
            return
        }

        val now = System.currentTimeMillis()
        plans.filter { it.isEnabled }.forEach { plan ->
            val triggerAt = computeNextTriggerMillis(plan.dayOfWeek, plan.workoutTime, now)
            val pendingIntent = buildAlarmPendingIntent(
                context = context,
                dayOfWeek = plan.dayOfWeek,
                dailyTarget = plan.dailyTarget,
                workoutTime = plan.workoutTime,
                flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                try {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                } catch (_: SecurityException) {
                    // Fallback if OEM restricts alarm scheduling
                }
            }
        }
    }

    fun showReminderNotification(
        context: Context,
        dayOfWeek: Int,
        dailyTarget: Int,
        workoutTime: String
    ) {
        if (!hasNotificationPermission(context)) return
        createNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            dayOfWeek,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pushup)
            .setContentTitle("Time for Today's Push-Up Session")
            .setContentText("Target: $dailyTarget reps scheduled for $workoutTime. Tap to start!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(REQUEST_CODE_BASE + dayOfWeek, notification)
        } catch (_: SecurityException) {
            // Permission may have been revoked between check and notify
        }
    }

    fun sendTestNotification(context: Context): Boolean {
        if (!hasNotificationPermission(context)) return false
        createNotificationChannel(context)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            999,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pushup)
            .setContentTitle("Pushup Club Test Reminder")
            .setContentText("Notifications are working! Your scheduled training alerts will look like this.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(REQUEST_CODE_BASE + 99, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun parseTimeOrNull(timeStr: String): Pair<Int, Int>? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    fun formatTime(hour: Int, minute: Int): String {
        val h = hour.coerceIn(0, 23).toString().padStart(2, '0')
        val m = minute.coerceIn(0, 59).toString().padStart(2, '0')
        return "$h:$m"
    }

    fun computeNextTriggerMillis(
        dayOfWeek: Int,
        timeStr: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val (hour, minute) = parseTimeOrNull(timeStr) ?: (18 to 0)
        val targetCalendarDay = when (dayOfWeek) {
            1 -> Calendar.MONDAY
            2 -> Calendar.TUESDAY
            3 -> Calendar.WEDNESDAY
            4 -> Calendar.THURSDAY
            5 -> Calendar.FRIDAY
            6 -> Calendar.SATURDAY
            7 -> Calendar.SUNDAY
            else -> Calendar.MONDAY
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        var daysUntil = (targetCalendarDay - currentDay + 7) % 7
        if (daysUntil == 0 && cal.timeInMillis <= nowMillis + 1000L) {
            daysUntil = 7
        }
        cal.add(Calendar.DAY_OF_YEAR, daysUntil)
        return cal.timeInMillis
    }

    fun getNextScheduledReminderSummary(
        plans: List<DailyPlan>,
        notificationsEnabled: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ): String? {
        if (!notificationsEnabled) return null
        val enabledPlans = plans.filter { it.isEnabled }
        if (enabledPlans.isEmpty()) return null

        val nextPlan = enabledPlans.minByOrNull { plan ->
            computeNextTriggerMillis(plan.dayOfWeek, plan.workoutTime, nowMillis)
        } ?: return null

        val shortDay = nextPlan.dayName.take(3)
        return "$shortDay at ${nextPlan.workoutTime}"
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        dayOfWeek: Int,
        dailyTarget: Int,
        workoutTime: String,
        flags: Int
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_DAY_OF_WEEK, dayOfWeek)
            putExtra(EXTRA_DAILY_TARGET, dailyTarget)
            putExtra(EXTRA_WORKOUT_TIME, workoutTime)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_BASE + dayOfWeek,
            intent,
            flags
        )
    }
}
