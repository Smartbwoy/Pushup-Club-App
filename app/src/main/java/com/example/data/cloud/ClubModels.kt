package com.example.data.cloud

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class ClubMemberStats(
    val displayName: String = "",
    val weeklyPushUps: Long = 0L,
    val totalPushUps: Long = 0L,
    val streakDays: Long = 0L,
    val personalBest: Long = 0L
) {
    fun toPayloadMap(): Map<String, Any> = mapOf(
        "displayName" to displayName.trim().ifEmpty { "Athlete" }.take(30),
        "weeklyPushUps" to weeklyPushUps.coerceIn(0L, 100_000L),
        "totalPushUps" to totalPushUps.coerceIn(0L, 10_000_000L),
        "streakDays" to streakDays.coerceIn(0L, 3660L),
        "personalBest" to personalBest.coerceIn(0L, 50_000L)
    )
}

@IgnoreExtraProperties
data class Club(
    @DocumentId val id: String = "",
    val name: String = "",
    val joinCode: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val memberStats: Map<String, ClubMemberStats> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ClubLeaderboardRow(
    val uid: String,
    val displayName: String,
    val weeklyPushUps: Int,
    val totalPushUps: Int,
    val streakDays: Int,
    val personalBest: Int,
    val isCurrentUser: Boolean,
    val isOwner: Boolean
)
