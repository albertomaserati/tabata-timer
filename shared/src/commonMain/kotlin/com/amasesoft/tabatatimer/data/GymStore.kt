package com.amasesoft.tabatatimer.data

import com.amasesoft.tabatatimer.domain.Exercise
import com.amasesoft.tabatatimer.domain.ExerciseLimits
import com.amasesoft.tabatatimer.domain.MAX_EXERCISES_PER_ROUTINE
import com.amasesoft.tabatatimer.domain.MAX_ROUTINES
import com.amasesoft.tabatatimer.domain.Progression
import com.amasesoft.tabatatimer.domain.Routine
import com.amasesoft.tabatatimer.domain.RoutineLimits
import kotlinx.serialization.Serializable

/**
 * Persisted document (`gym.json`) holding the gym routines, separate from the timer files.
 *
 * - The order of [routines] is the user's order (also the order of the selector).
 * - [nextId] is shared by routines and exercises, so exercise ids are unique in the whole file.
 * - [selectedRoutineId] is the last routine shown in the tab, restored on next launch. It lives
 *   here (not in `settings.json`) so that deleting a routine and fixing the selection is a
 *   single atomic write.
 * - [schemaVersion] allows future migrations; new fields must have defaults.
 */
@Serializable
data class GymStore(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val nextId: Long = 1,
    val routines: List<Routine> = emptyList(),
    val selectedRoutineId: Long? = null,
) {
    /** The selected routine, or the first one if the stored id is missing or stale. */
    val selectedRoutine: Routine?
        get() = routines.firstOrNull { it.id == selectedRoutineId } ?: routines.firstOrNull()

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

sealed interface GymResult {
    data class Saved(val id: Long) : GymResult
    data object LimitReached : GymResult
    data object NotFound : GymResult

    /** Another routine already uses this name (case and surrounding spaces ignored). */
    data object NameTaken : GymResult
}

/** Weight and progression of an exercise, e.g. before "Apply" (to undo it). */
data class ProgressSnapshot(val exerciseId: Long, val weightHalfKg: Int?, val progression: Progression)

/** Result of "Apply": the values before it (for undo) and the new weight (for the message). */
data class AppliedIncrease(val previous: ProgressSnapshot, val newWeightHalfKg: Int)

internal fun GymStore.isRoutineNameTaken(name: String, excludingId: Long? = null): Boolean {
    val normalized = name.trim()
    return routines.any { it.id != excludingId && it.name.trim().equals(normalized, ignoreCase = true) }
}

internal fun GymStore.routineOf(exerciseId: Long): Routine? =
    routines.firstOrNull { routine -> routine.exercises.any { it.id == exerciseId } }

internal fun GymStore.exercise(exerciseId: Long): Exercise? =
    routines.firstNotNullOfOrNull { routine -> routine.exercises.firstOrNull { it.id == exerciseId } }

// Pure operations, unit tested; the repository runs them inside DataStore.updateData, so the
// checks (limits, unique names) and the write are atomic.

// region Routines

/** Creates an empty routine and selects it. */
internal fun GymStore.createRoutine(name: String): Pair<GymStore, GymResult> {
    if (routines.size >= MAX_ROUTINES) return this to GymResult.LimitReached
    if (isRoutineNameTaken(name)) return this to GymResult.NameTaken
    val routine = Routine(id = nextId, name = name.trim().take(RoutineLimits.NAME_MAX_LENGTH))
    return copy(nextId = nextId + 1, routines = routines + routine, selectedRoutineId = routine.id) to
        GymResult.Saved(routine.id)
}

internal fun GymStore.renameRoutine(id: Long, name: String): Pair<GymStore, GymResult> {
    if (routines.none { it.id == id }) return this to GymResult.NotFound
    if (isRoutineNameTaken(name, excludingId = id)) return this to GymResult.NameTaken
    val trimmed = name.trim().take(RoutineLimits.NAME_MAX_LENGTH)
    return copy(routines = routines.map { if (it.id == id) it.copy(name = trimmed) else it }) to GymResult.Saved(id)
}

/**
 * Copies a routine (with new exercise ids) right after the original and selects it.
 * The copy is named "<name> (<copyWord>)", then "<name> (<copyWord> 2)", ... until unique;
 * the original name is shortened if needed to stay within the length limit.
 */
internal fun GymStore.duplicateRoutine(id: Long, copyWord: String): Pair<GymStore, GymResult> {
    val index = routines.indexOfFirst { it.id == id }
    if (index < 0) return this to GymResult.NotFound
    if (routines.size >= MAX_ROUTINES) return this to GymResult.LimitReached
    val original = routines[index]
    val name = uniqueCopyName(original.name.trim(), copyWord)
    var next = nextId
    val copy = Routine(
        id = next++,
        name = name,
        exercises = original.exercises.map { it.copy(id = next++) },
    )
    val list = routines.toMutableList().apply { add(index + 1, copy) }
    return copy(nextId = next, routines = list, selectedRoutineId = copy.id) to GymResult.Saved(copy.id)
}

internal fun GymStore.uniqueCopyName(base: String, copyWord: String): String {
    var n = 1
    while (true) {
        val suffix = if (n == 1) " ($copyWord)" else " ($copyWord $n)"
        val head = base.take((RoutineLimits.NAME_MAX_LENGTH - suffix.length).coerceAtLeast(1)).trimEnd()
        val candidate = head + suffix
        if (!isRoutineNameTaken(candidate)) return candidate
        n++
    }
}

/** Deletes a routine with its exercises; a deleted selection falls back to the neighbour. */
internal fun GymStore.deleteRoutine(id: Long): GymStore {
    val index = routines.indexOfFirst { it.id == id }
    if (index < 0) return this
    val remaining = routines.filterNot { it.id == id }
    val selection = if (selectedRoutine?.id == id) {
        remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.id
    } else {
        selectedRoutineId
    }
    return copy(routines = remaining, selectedRoutineId = selection)
}

/** Moves a routine by [offset] positions (negative = up). */
internal fun GymStore.moveRoutine(id: Long, offset: Int): GymStore {
    val moved = routines.moved(id, offset, Routine::id) ?: return this
    return copy(routines = moved)
}

internal fun GymStore.selectRoutine(id: Long): GymStore =
    if (routines.any { it.id == id }) copy(selectedRoutineId = id) else this

// endregion

// region Exercises

/** Adds an exercise (its id is assigned here) at the end of a routine. */
internal fun GymStore.addExercise(routineId: Long, exercise: Exercise): Pair<GymStore, GymResult> {
    val routine = routines.firstOrNull { it.id == routineId } ?: return this to GymResult.NotFound
    if (routine.exercises.size >= MAX_EXERCISES_PER_ROUTINE) return this to GymResult.LimitReached
    val added = exercise.copy(id = nextId).sanitized()
    return copy(nextId = nextId + 1, routines = routines.replace(routine.copy(exercises = routine.exercises + added))) to
        GymResult.Saved(added.id)
}

/** Replaces the exercise with the same id. */
internal fun GymStore.updateExercise(exercise: Exercise): Pair<GymStore, GymResult> {
    val routine = routineOf(exercise.id) ?: return this to GymResult.NotFound
    val clean = exercise.sanitized()
    val updated = routine.copy(exercises = routine.exercises.map { if (it.id == exercise.id) clean else it })
    return copy(routines = routines.replace(updated)) to GymResult.Saved(exercise.id)
}

internal fun GymStore.deleteExercise(exerciseId: Long): GymStore = mapExercises(exerciseId) { list ->
    list.filterNot { it.id == exerciseId }
}

internal fun GymStore.moveExercise(exerciseId: Long, offset: Int): GymStore = mapExercises(exerciseId) { list ->
    list.moved(exerciseId, offset, Exercise::id) ?: list
}

/** Switches Increase <-> Repeat. */
internal fun GymStore.toggleProgression(exerciseId: Long): GymStore = mapExercise(exerciseId) {
    it.copy(progression = if (it.progression == Progression.Increase) Progression.Repeat else Progression.Increase)
}

/**
 * "Apply": weight += increment (capped at the maximum) and progression back to Repeat.
 * Returns the previous values for undo, or `null` (store unchanged) if there is nothing to apply.
 */
internal fun GymStore.applyIncrease(exerciseId: Long): Pair<GymStore, AppliedIncrease?> {
    val exercise = exercise(exerciseId) ?: return this to null
    val next = exercise.nextWeightHalfKg ?: return this to null
    val applied = AppliedIncrease(ProgressSnapshot(exerciseId, exercise.weightHalfKg, exercise.progression), next)
    return mapExercise(exerciseId) { it.copy(weightHalfKg = next, progression = Progression.Repeat) } to applied
}

/**
 * Undo of [applyIncrease]: restores weight and progression only if the exercise is still in the
 * state "Apply" produced (new weight, Repeat). If it changed meanwhile (edited, toggled, applied
 * again) the undo is stale and the store is left unchanged. Other fields are never touched.
 */
internal fun GymStore.undoIncrease(applied: AppliedIncrease): GymStore {
    val previous = applied.previous
    val current = exercise(previous.exerciseId) ?: return this
    if (current.weightHalfKg != applied.newWeightHalfKg || current.progression != Progression.Repeat) return this
    return mapExercise(previous.exerciseId) {
        it.copy(
            weightHalfKg = previous.weightHalfKg?.coerceIn(ExerciseLimits.weightHalfKg),
            progression = previous.progression,
        )
    }
}

// endregion

private fun List<Routine>.replace(routine: Routine): List<Routine> = map { if (it.id == routine.id) routine else it }

private fun GymStore.mapExercises(exerciseId: Long, transform: (List<Exercise>) -> List<Exercise>): GymStore {
    val routine = routineOf(exerciseId) ?: return this
    return copy(routines = routines.replace(routine.copy(exercises = transform(routine.exercises))))
}

private fun GymStore.mapExercise(exerciseId: Long, transform: (Exercise) -> Exercise): GymStore =
    mapExercises(exerciseId) { list -> list.map { if (it.id == exerciseId) transform(it) else it } }

/** The list with the element [id] moved by [offset] (clamped), or `null` if nothing changes. */
private fun <T> List<T>.moved(id: Long, offset: Int, idOf: (T) -> Long): List<T>? {
    val from = indexOfFirst { idOf(it) == id }
    if (from < 0) return null
    val to = (from + offset).coerceIn(0, lastIndex)
    if (to == from) return null
    return toMutableList().apply { add(to, removeAt(from)) }
}
