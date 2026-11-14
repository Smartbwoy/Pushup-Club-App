package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_plans")
data class DailyPlan(
    @PrimaryKey val dayOfWeek: Int, // 1 = Monday, 2 = Tuesday, ..., 7 = Sunday
    val dayName: String, // "Monday", "Tuesday", etc.
    val isEnabled: Boolean = false,
    val workoutTime: String = "18:00", // "HH:mm" formatted
    val dailyTarget: Int = 50
)
