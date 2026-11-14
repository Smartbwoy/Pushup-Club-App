package com.example.data.backup

import com.example.data.challenge.ThirtyDayChallengeState
import com.example.data.model.DailyPlan
import com.example.data.model.UserPreferences
import com.example.data.model.WorkoutSession
import org.json.JSONArray
import org.json.JSONObject

data class BackupPayload(
    val formatVersion: Int,
    val exportedAt: Long,
    val preferences: UserPreferences,
    val dailyPlans: List<DailyPlan>,
    val sessions: List<WorkoutSession>,
    val challengeState: ThirtyDayChallengeState,
    val adaptiveMultipliers: Map<String, Float>
)

object BackupManager {
    private const val CURRENT_FORMAT_VERSION = 1

    fun exportToJson(
        preferences: UserPreferences,
        dailyPlans: List<DailyPlan>,
        sessions: List<WorkoutSession>,
        challengeState: ThirtyDayChallengeState,
        adaptiveMultipliers: Map<String, Float>,
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject().apply {
            put("app", "Pushup Club")
            put("formatVersion", CURRENT_FORMAT_VERSION)
            put("exportedAt", exportedAt)

            put("preferences", JSONObject().apply {
                put("weeklyTarget", preferences.weeklyTarget)
                put("voiceCoachEnabled", preferences.voiceCoachEnabled)
                put("notificationsEnabled", preferences.notificationsEnabled)
                put("hasCompletedOnboarding", preferences.hasCompletedOnboarding)
                put("experienceLevel", preferences.experienceLevel)
            })

            put("dailyPlans", JSONArray().apply {
                dailyPlans.forEach { plan ->
                    put(JSONObject().apply {
                        put("dayOfWeek", plan.dayOfWeek)
                        put("dayName", plan.dayName)
                        put("isEnabled", plan.isEnabled)
                        put("workoutTime", plan.workoutTime)
                        put("dailyTarget", plan.dailyTarget)
                    })
                }
            })

            put("sessions", JSONArray().apply {
                sessions.forEach { s ->
                    put(JSONObject().apply {
                        put("timestamp", s.timestamp)
                        put("totalPushUps", s.totalPushUps)
                        put("setsCount", s.setsCount)
                        put("repsListString", s.repsListString)
                        put("restTimeSeconds", s.restTimeSeconds)
                        if (s.programId != null) {
                            put("programId", s.programId)
                        }
                    })
                }
            })

            put("thirtyDayChallenge", JSONObject().apply {
                put("isStarted", challengeState.isStarted)
                put("initialMaxReps", challengeState.initialMaxReps)
                put("completedDays", challengeState.completedDays)
                put("finalMaxReps", challengeState.finalMaxReps)
            })

            put("adaptiveMultipliers", JSONObject().apply {
                adaptiveMultipliers.forEach { (k, v) ->
                    put(k, v.toDouble())
                }
            })
        }
        return root.toString(2)
    }

    fun parseBackupJson(jsonString: String): BackupPayload? {
        return try {
            val root = JSONObject(jsonString)
            if (!root.has("sessions") || !root.has("preferences")) return null

            val formatVersion = root.optInt("formatVersion", 1)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())

            val prefsObj = root.getJSONObject("preferences")
            val preferences = UserPreferences(
                id = 1,
                weeklyTarget = prefsObj.optInt("weeklyTarget", 100).coerceAtLeast(10),
                voiceCoachEnabled = prefsObj.optBoolean("voiceCoachEnabled", true),
                notificationsEnabled = prefsObj.optBoolean("notificationsEnabled", false),
                hasCompletedOnboarding = true,
                experienceLevel = prefsObj.optString("experienceLevel", "Beginner")
            )

            val plansArray = root.optJSONArray("dailyPlans") ?: JSONArray()
            val plans = mutableListOf<DailyPlan>()
            for (i in 0 until plansArray.length()) {
                val obj = plansArray.getJSONObject(i)
                val day = obj.optInt("dayOfWeek", -1)
                if (day in 1..7) {
                    plans.add(
                        DailyPlan(
                            dayOfWeek = day,
                            dayName = obj.optString("dayName", defaultDayName(day)),
                            isEnabled = obj.optBoolean("isEnabled", false),
                            workoutTime = obj.optString("workoutTime", "18:00"),
                            dailyTarget = obj.optInt("dailyTarget", 35).coerceAtLeast(1)
                        )
                    )
                }
            }

            val sessionsArray = root.getJSONArray("sessions")
            val sessions = mutableListOf<WorkoutSession>()
            for (i in 0 until sessionsArray.length()) {
                val obj = sessionsArray.getJSONObject(i)
                val total = obj.optInt("totalPushUps", 0)
                if (total > 0) {
                    val sets = obj.optInt("setsCount", 1).coerceAtLeast(1)
                    sessions.add(
                        WorkoutSession(
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            totalPushUps = total,
                            setsCount = sets,
                            repsListString = obj.optString("repsListString", total.toString()),
                            restTimeSeconds = obj.optInt("restTimeSeconds", 30).coerceAtLeast(5),
                            programId = if (obj.isNull("programId")) null else obj.optString("programId", "").ifEmpty { null }
                        )
                    )
                }
            }

            val challengeObj = root.optJSONObject("thirtyDayChallenge")
            val challengeState = if (challengeObj != null) {
                ThirtyDayChallengeState(
                    isStarted = challengeObj.optBoolean("isStarted", false),
                    initialMaxReps = challengeObj.optInt("initialMaxReps", 0),
                    completedDays = challengeObj.optInt("completedDays", 0).coerceIn(0, 30),
                    finalMaxReps = challengeObj.optInt("finalMaxReps", 0)
                )
            } else {
                ThirtyDayChallengeState()
            }

            val adaptiveObj = root.optJSONObject("adaptiveMultipliers")
            val adaptiveMap = mutableMapOf<String, Float>()
            if (adaptiveObj != null) {
                val keys = adaptiveObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    adaptiveMap[key] = adaptiveObj.optDouble(key, 1.0).toFloat()
                }
            }

            BackupPayload(
                formatVersion = formatVersion,
                exportedAt = exportedAt,
                preferences = preferences,
                dailyPlans = plans,
                sessions = sessions,
                challengeState = challengeState,
                adaptiveMultipliers = adaptiveMap
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun defaultDayName(dayOfWeek: Int): String = when (dayOfWeek) {
        1 -> "Monday"
        2 -> "Tuesday"
        3 -> "Wednesday"
        4 -> "Thursday"
        5 -> "Friday"
        6 -> "Saturday"
        7 -> "Sunday"
        else -> "Monday"
    }
}
