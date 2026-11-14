package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WorkoutWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).workoutDao
                val repo = WorkoutRepository(dao)
                val sessions = repo.getAllSessionsSync()
                val plans = repo.getAllDailyPlansSync()
                val todayReps = WorkoutRepository.calculateTodayPushUps(sessions)
                val streak = repo.calculateStreak(sessions, plans)

                appWidgetIds.forEach { widgetId ->
                    val views = buildRemoteViews(context, todayReps, streak)
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_START_WORKOUT = "extra_start_workout_from_widget"

        fun updateAllWidgets(context: Context, todayReps: Int? = null, streak: Int? = null) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext) ?: return
            val componentName = ComponentName(appContext, WorkoutWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            if (ids.isEmpty()) return

            if (todayReps != null && streak != null) {
                ids.forEach { widgetId ->
                    manager.updateAppWidget(widgetId, buildRemoteViews(appContext, todayReps, streak))
                }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val dao = AppDatabase.getInstance(appContext).workoutDao
                    val repo = WorkoutRepository(dao)
                    val sessions = repo.getAllSessionsSync()
                    val plans = repo.getAllDailyPlansSync()
                    val computedToday = WorkoutRepository.calculateTodayPushUps(sessions)
                    val computedStreak = repo.calculateStreak(sessions, plans)
                    ids.forEach { widgetId ->
                        manager.updateAppWidget(
                            widgetId,
                            buildRemoteViews(appContext, computedToday, computedStreak)
                        )
                    }
                }
            }
        }

        private fun buildRemoteViews(
            context: Context,
            todayReps: Int,
            streak: Int
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_workout_summary)
            views.setTextViewText(R.id.widget_today_reps, todayReps.toString())
            views.setTextViewText(
                R.id.widget_streak_text,
                if (streak == 1) "1 day streak" else "$streak day streak"
            )

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                2001,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPendingIntent)

            val startIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_START_WORKOUT, true)
            }
            val startPendingIntent = PendingIntent.getActivity(
                context,
                2002,
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_start_button, startPendingIntent)

            return views
        }
    }
}
