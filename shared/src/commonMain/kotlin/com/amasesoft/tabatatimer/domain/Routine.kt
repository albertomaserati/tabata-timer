package com.amasesoft.tabatatimer.domain

import kotlinx.serialization.Serializable

/** Maximum number of gym routines ("schede"). */
const val MAX_ROUTINES = 20

/** Maximum number of exercises in a single routine. */
const val MAX_EXERCISES_PER_ROUTINE = 50

/**
 * A gym routine ("scheda"): a named, ordered list of exercises. Independent from the timer.
 *
 * Weights are stored as integers in **half kilograms** (`weightHalfKg = 25` is 12.5 kg): only
 * half-kilo steps are allowed, and integers avoid any floating point rounding.
 */
@Serializable
data class Routine(
    val id: Long,
    val name: String,
    val exercises: List<Exercise> = emptyList(),
)

/** What to do with the weight next time: [Increase] ("A", aumenta) or [Repeat] ("R", ripeti). */
@Serializable
enum class Progression { Repeat, Increase }

@Serializable
data class Exercise(
    val id: Long,
    val name: String,
    val description: String = "",
    val sets: Int = ExerciseLimits.DEFAULT_SETS,
    val reps: Int = ExerciseLimits.DEFAULT_REPS,
    /** Same weight for every set, in half kilograms; `null` = no weight (bodyweight exercise). */
    val weightHalfKg: Int? = null,
    val progression: Progression = Progression.Repeat,
    /** Weight added by "Apply" when [progression] is [Progression.Increase], in half kilograms. */
    val incrementHalfKg: Int = ExerciseLimits.DEFAULT_INCREMENT_HALF_KG,
) {
    /** Weight to use next time, shown only when there is something to apply. */
    val nextWeightHalfKg: Int?
        get() = if (progression == Progression.Increase && weightHalfKg != null) {
            (weightHalfKg + incrementHalfKg).coerceAtMost(ExerciseLimits.weightHalfKg.last)
        } else {
            null
        }

    /** Values clamped to the allowed ranges (input from the UI or from an edited file). */
    fun sanitized(): Exercise = copy(
        name = name.trim().take(ExerciseLimits.NAME_MAX_LENGTH),
        description = description.trim().take(ExerciseLimits.DESCRIPTION_MAX_LENGTH),
        sets = sets.coerceIn(ExerciseLimits.sets),
        reps = reps.coerceIn(ExerciseLimits.reps),
        weightHalfKg = weightHalfKg?.coerceIn(ExerciseLimits.weightHalfKg),
        incrementHalfKg = incrementHalfKg.coerceIn(ExerciseLimits.incrementHalfKg),
    )
}

object ExerciseLimits {
    const val NAME_MAX_LENGTH = 40
    const val DESCRIPTION_MAX_LENGTH = 200
    const val DEFAULT_SETS = 3
    const val DEFAULT_REPS = 10
    const val DEFAULT_INCREMENT_HALF_KG = 4 // 2 kg
    const val DEFAULT_WEIGHT_HALF_KG = 20 // 10 kg

    val sets = 1..20
    val reps = 1..100

    /** 0.5..500 kg. */
    val weightHalfKg = 1..1000

    /** 0.5..50 kg. */
    val incrementHalfKg = 1..100
}

/** Routine names: same rules as workout names (required, max length, unique). */
object RoutineLimits {
    const val NAME_MAX_LENGTH = 40
}

/** Why a name (routine or exercise) cannot be saved; shown inline under the field. */
enum class GymNameError { Empty, TooLong, Duplicate }

/** Local checks only; uniqueness is checked by the store, atomically with the write. */
fun validateGymName(name: String, maxLength: Int): GymNameError? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> GymNameError.Empty
        trimmed.length > maxLength -> GymNameError.TooLong
        else -> null
    }
}

/**
 * Formats a weight in half kilograms without unit: whole kilos without decimals ("14"),
 * otherwise with a single ".5" decimal using [decimalSeparator] ("12,5" in Italian).
 */
fun formatWeight(halfKg: Int, decimalSeparator: Char): String {
    val kilos = halfKg / 2
    return if (halfKg % 2 == 0) "$kilos" else "$kilos${decimalSeparator}5"
}
