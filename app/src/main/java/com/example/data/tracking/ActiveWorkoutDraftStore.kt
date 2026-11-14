package com.example.data.tracking

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ActiveWorkoutDraft(
    val workoutState: String, // "RUNNING" or "REST"
    val activeProgramId: String?,
    val activeSets: List<Int>,
    val completedReps: List<Int>,
    val currentSetIndex: Int,
    val currentRepCount: Int,
    val restEndTimeMillis: Long,
    val updatedAtMillis: Long
)

/**
 * Lightweight crash/process-kill persistence for an in-progress workout session
 * so Android killing the backgrounded app does not discard active reps or sets.
 */
class ActiveWorkoutDraftStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveDraft(draft: ActiveWorkoutDraft) {
        if (draft.workoutState != "RUNNING" && draft.workoutState != "REST") {
            clear()
            return
        }
        val json = JSONObject().apply {
            put("workoutState", draft.workoutState)
            put("activeProgramId", draft.activeProgramId ?: JSONObject.NULL)
            put("activeSets", JSONArray(draft.activeSets))
            put("completedReps", JSONArray(draft.completedReps))
            put("currentSetIndex", draft.currentSetIndex)
            put("currentRepCount", draft.currentRepCount)
            put("restEndTimeMillis", draft.restEndTimeMillis)
            put("updatedAtMillis", draft.updatedAtMillis)
        }.toString()
        prefs.edit().putString(KEY_DRAFT_JSON, json).apply()
    }

    fun loadValidDraft(nowMillis: Long = System.currentTimeMillis()): ActiveWorkoutDraft? {
        val raw = prefs.getString(KEY_DRAFT_JSON, null) ?: return null
        return try {
            val obj = JSONObject(raw)
            val updatedAt = obj.optLong("updatedAtMillis", 0L)
            if (nowMillis - updatedAt > MAX_DRAFT_AGE_MS || updatedAt <= 0L) {
                clear()
                return null
            }
            val state = obj.optString("workoutState", "")
            if (state != "RUNNING" && state != "REST") {
                clear()
                return null
            }
            val setsJson = obj.optJSONArray("activeSets") ?: return null
            val completedJson = obj.optJSONArray("completedReps") ?: return null
            val activeSets = List(setsJson.length()) { i -> setsJson.optInt(i, 10) }
            val completedReps = List(completedJson.length()) { i -> completedJson.optInt(i, 0) }
            if (activeSets.isEmpty()) return null

            val progId = if (obj.isNull("activeProgramId")) null else obj.optString("activeProgramId", "").ifBlank { null }
            ActiveWorkoutDraft(
                workoutState = state,
                activeProgramId = progId,
                activeSets = activeSets,
                completedReps = completedReps,
                currentSetIndex = obj.optInt("currentSetIndex", 0).coerceIn(0, activeSets.lastIndex),
                currentRepCount = obj.optInt("currentRepCount", 0).coerceAtLeast(0),
                restEndTimeMillis = obj.optLong("restEndTimeMillis", 0L),
                updatedAtMillis = updatedAt
            )
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_DRAFT_JSON).apply()
    }

    companion object {
        private const val PREFS_NAME = "pushup_active_workout_draft"
        private const val KEY_DRAFT_JSON = "active_draft_json"
        private const val MAX_DRAFT_AGE_MS = 3L * 60L * 60L * 1000L // 3 hours
    }
}
