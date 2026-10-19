package com.amasesoft.tabatatimer.domain

import kotlinx.serialization.Serializable

const val MAX_SAVED_WORKOUTS = 20

@Serializable
data class SavedWorkout(
    val id: Long,
    val name: String,
    val description: String = "",
    val config: WorkoutConfig,
    val usageCount: Int = 0,
    val createdAtEpochMillis: Long = 0,
) {
    val totalSeconds: Int get() = WorkoutPlanner.totalSeconds(config)
}

@Serializable
enum class WorkoutSort { Custom, Name, MostUsed, Newest, Oldest, Shortest, Longest }

/** Pure sorting used by the home screen; [WorkoutSort.Custom] keeps the stored (manual) order. */
fun List<SavedWorkout>.sortedFor(sort: WorkoutSort): List<SavedWorkout> = when (sort) {
    WorkoutSort.Custom -> this
    WorkoutSort.Name -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    WorkoutSort.MostUsed -> sortedWith(compareByDescending<SavedWorkout> { it.usageCount }.thenBy { it.name.lowercase() })
    WorkoutSort.Newest -> sortedByDescending { it.createdAtEpochMillis }
    WorkoutSort.Oldest -> sortedBy { it.createdAtEpochMillis }
    WorkoutSort.Shortest -> sortedWith(compareBy<SavedWorkout> { it.totalSeconds }.thenBy { it.name.lowercase() })
    WorkoutSort.Longest -> sortedWith(compareByDescending<SavedWorkout> { it.totalSeconds }.thenBy { it.name.lowercase() })
}
