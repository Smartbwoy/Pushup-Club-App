package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenge_goals")
data class ChallengeGoal(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val targetValue: Int,
    val currentValue: Int,
    val isCompleted: Boolean = false,
    val category: String, // "BEGINNER", "INTERMEDIATE", "ADVANCED", "ACHIEVEMENT"
    val type: String, // "TOTAL_PUSHUPS", "STREAK", "SINGLE_SESSION", "DAYS_CHALLENGE"
    val iconName: String // string descriptor for icons
)
