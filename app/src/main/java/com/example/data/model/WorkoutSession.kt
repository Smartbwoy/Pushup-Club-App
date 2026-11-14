package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val totalPushUps: Int,
    val setsCount: Int,
    val repsListString: String, // Comma separated reps, e.g. "10,15,10"
    val restTimeSeconds: Int,
    val programId: String? = null // e.g. "beginner", "military_prep", "challenge_100" or null
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
