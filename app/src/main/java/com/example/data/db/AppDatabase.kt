package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ChallengeGoal
import com.example.data.model.DailyPlan
import com.example.data.model.UserPreferences
import com.example.data.model.WorkoutSession

@Database(
    entities = [WorkoutSession::class, DailyPlan::class, ChallengeGoal::class, UserPreferences::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract val workoutDao: WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pushup_pro_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
