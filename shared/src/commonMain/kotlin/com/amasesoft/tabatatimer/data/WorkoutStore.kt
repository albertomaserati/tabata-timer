package com.amasesoft.tabatatimer.data

import com.amasesoft.tabatatimer.domain.MAX_SAVED_WORKOUTS
import com.amasesoft.tabatatimer.domain.SavedWorkout
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import kotlinx.serialization.Serializable

/**
 * Persisted document holding all saved workouts.
 *
 * The order of [workouts] is the user's custom (manual) order.
 * [schemaVersion] allows future migrations; new fields must have defaults so that older files
 * keep decoding (the JSON decoder ignores unknown keys and fills missing ones with defaults).
 */
@Serializable
data class WorkoutStore(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val nextId: Long = 1,
    val workouts: List<SavedWorkout> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

sealed interface SaveResult {
    data class Saved(val id: Long) : SaveResult
    data object LimitReached : SaveResult
    data object NotFound : SaveResult

    /** Another workout already uses this name (case and surrounding spaces ignored). */
    data object NameTaken : SaveResult
}

/** Names are unique ignoring case and leading/trailing spaces; [excludingId] is the workout being edited. */
internal fun WorkoutStore.isNameTaken(name: String, excludingId: Long? = null): Boolean {
    val normalized = name.trim()
    return workouts.any { it.id != excludingId && it.name.trim().equals(normalized, ignoreCase = true) }
}

// Pure operations, unit tested; the repository just runs them inside DataStore.updateData,
// so checks (limit, unique name) and the write are atomic.

internal fun WorkoutStore.create(
    name: String,
    description: String,
    config: WorkoutConfig,
    nowEpochMillis: Long,
): Pair<WorkoutStore, SaveResult> {
    if (workouts.size >= MAX_SAVED_WORKOUTS) return this to SaveResult.LimitReached
    if (isNameTaken(name)) return this to SaveResult.NameTaken
    val workout = SavedWorkout(
        id = nextId,
        name = name.trim(),
        description = description.trim(),
        config = config.sanitized(),
        createdAtEpochMillis = nowEpochMillis,
    )
    return copy(nextId = nextId + 1, workouts = workouts + workout) to SaveResult.Saved(workout.id)
}

internal fun WorkoutStore.update(
    id: Long,
    name: String,
    description: String,
    config: WorkoutConfig,
): Pair<WorkoutStore, SaveResult> {
    if (workouts.none { it.id == id }) return this to SaveResult.NotFound
    if (isNameTaken(name, excludingId = id)) return this to SaveResult.NameTaken
    val updated = workouts.map {
        if (it.id == id) {
            it.copy(name = name.trim(), description = description.trim(), config = config.sanitized())
        } else {
            it
        }
    }
    return copy(workouts = updated) to SaveResult.Saved(id)
}

internal fun WorkoutStore.delete(id: Long): WorkoutStore =
    copy(workouts = workouts.filterNot { it.id == id })

/** Moves a workout by [offset] positions in the custom order (negative = up). */
internal fun WorkoutStore.move(id: Long, offset: Int): WorkoutStore {
    val from = workouts.indexOfFirst { it.id == id }
    if (from < 0) return this
    val to = (from + offset).coerceIn(0, workouts.lastIndex)
    if (to == from) return this
    val list = workouts.toMutableList()
    list.add(to, list.removeAt(from))
    return copy(workouts = list)
}

internal fun WorkoutStore.incrementUsage(id: Long): WorkoutStore =
    copy(workouts = workouts.map { if (it.id == id) it.copy(usageCount = it.usageCount + 1) else it })
