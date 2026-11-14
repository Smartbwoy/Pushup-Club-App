package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1, // Singleton row
    val weeklyTarget: Int = 100,
    val voiceCoachEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val experienceLevel: String = "Beginner", // "Beginner", "Intermediate", "Advanced"
    val preferredCounterMode: String = COUNTER_MODE_TAP // "TAP", "PROXIMITY", "CAMERA"
) {
    companion object {
        const val COUNTER_MODE_TAP = "TAP"
        const val COUNTER_MODE_PROXIMITY = "PROXIMITY"
        const val COUNTER_MODE_CAMERA = "CAMERA"
    }
}
