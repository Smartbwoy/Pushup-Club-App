package com.example.data.adaptive

import android.content.Context
import com.example.data.model.TrainingProgram
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

data class AdaptedProgramInfo(
    val program: TrainingProgram,
    val multiplier: Float,
    val statusBadge: String? // e.g., "ADAPTED +10%", "EASED -10%"
)

class AdaptiveProgramManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _multipliers = MutableStateFlow(readAllMultipliers())
    val multipliers: StateFlow<Map<String, Float>> = _multipliers.asStateFlow()

    private fun readAllMultipliers(): Map<String, Float> {
        return TrainingProgram.programs.associate { program ->
            program.id to prefs.getFloat(keyFor(program.id), 1.0f).coerceIn(MIN_MULT, MAX_MULT)
        }
    }

    fun recordProgramOutcome(
        programId: String,
        targetTotalReps: Int,
        completedTotalReps: Int
    ) {
        if (targetTotalReps <= 0 || completedTotalReps <= 0) return
        val current = prefs.getFloat(keyFor(programId), 1.0f)
        val next = computeNextMultiplier(current, targetTotalReps, completedTotalReps)
        prefs.edit().putFloat(keyFor(programId), next).apply()
        _multipliers.value = readAllMultipliers()
    }

    fun getAllMultiplierMap(): Map<String, Float> = readAllMultipliers()

    fun restoreMultipliers(map: Map<String, Float>) {
        val editor = prefs.edit()
        map.forEach { (id, mult) ->
            editor.putFloat(keyFor(id), mult.coerceIn(MIN_MULT, MAX_MULT))
        }
        editor.apply()
        _multipliers.value = readAllMultipliers()
    }

    fun resetAll() {
        prefs.edit().clear().apply()
        _multipliers.value = readAllMultipliers()
    }

    companion object {
        private const val PREFS_NAME = "pushup_club_adaptive_programs"
        private const val MIN_MULT = 0.6f
        private const val MAX_MULT = 2.0f

        private fun keyFor(programId: String) = "mult_$programId"

        fun computeNextMultiplier(
            currentMultiplier: Float,
            targetTotalReps: Int,
            completedTotalReps: Int
        ): Float {
            return when {
                completedTotalReps >= targetTotalReps + 3 ->
                    (currentMultiplier + 0.10f).coerceAtMost(MAX_MULT)
                completedTotalReps < (targetTotalReps * 0.75f).roundToInt() ->
                    (currentMultiplier - 0.10f).coerceAtLeast(MIN_MULT)
                else -> currentMultiplier
            }
        }

        fun adaptProgram(program: TrainingProgram, multiplier: Float): AdaptedProgramInfo {
            val safeMult = multiplier.coerceIn(MIN_MULT, MAX_MULT)
            val pctDelta = ((safeMult - 1.0f) * 100f).roundToInt()
            val scaledSets = when {
                pctDelta == 0 -> program.sets
                pctDelta > 0 -> program.sets.map { base ->
                    kotlin.math.ceil((base * (100 + pctDelta) / 100.0) - 1e-6).toInt().coerceAtLeast(2)
                }
                else -> program.sets.map { base ->
                    kotlin.math.floor((base * (100 + pctDelta) / 100.0) + 1e-6).toInt().coerceAtLeast(2)
                }
            }

            val badge = when {
                pctDelta >= 5 -> "ADAPTED +$pctDelta%"
                pctDelta <= -5 -> "EASED $pctDelta%"
                else -> null
            }

            return AdaptedProgramInfo(
                program = program.copy(sets = scaledSets),
                multiplier = safeMult,
                statusBadge = badge
            )
        }
    }
}
