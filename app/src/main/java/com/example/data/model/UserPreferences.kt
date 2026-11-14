package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1, // Singleton row
    val weeklyTarget: Int = 300,
    val voiceCoachEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true
)
