package com.amasesoft.tabatatimer.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkoutSortTest {

    private fun workout(id: Long, name: String, uses: Int, created: Long, cycles: Int) = SavedWorkout(
        id = id,
        name = name,
        config = WorkoutConfig(prepareSeconds = 0, plan = CyclePlan.Uniform(10, 0, cycles)),
        usageCount = uses,
        createdAtEpochMillis = created,
    )

    private val list = listOf(
        workout(1, "beta", uses = 5, created = 300, cycles = 3),
        workout(2, "Alpha", uses = 9, created = 100, cycles = 1),
        workout(3, "gamma", uses = 0, created = 200, cycles = 8),
    )

    private fun ids(sort: WorkoutSort) = list.sortedFor(sort).map { it.id }

    @Test
    fun sorts() {
        assertEquals(listOf(1L, 2L, 3L), ids(WorkoutSort.Custom))
        assertEquals(listOf(2L, 1L, 3L), ids(WorkoutSort.Name))
        assertEquals(listOf(2L, 1L, 3L), ids(WorkoutSort.MostUsed))
        assertEquals(listOf(1L, 3L, 2L), ids(WorkoutSort.Newest))
        assertEquals(listOf(2L, 3L, 1L), ids(WorkoutSort.Oldest))
        assertEquals(listOf(2L, 1L, 3L), ids(WorkoutSort.Shortest))
        assertEquals(listOf(3L, 1L, 2L), ids(WorkoutSort.Longest))
    }
}
