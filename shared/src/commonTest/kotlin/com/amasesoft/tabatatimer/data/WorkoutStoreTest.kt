package com.amasesoft.tabatatimer.data

import com.amasesoft.tabatatimer.domain.CyclePlan
import com.amasesoft.tabatatimer.domain.MAX_SAVED_WORKOUTS
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WorkoutStoreTest {

    private fun storeWith(n: Int): WorkoutStore = (1..n).fold(WorkoutStore()) { store, i ->
        store.create("W$i", "", WorkoutConfig.Default, nowEpochMillis = i.toLong()).first
    }

    @Test
    fun createAssignsIncreasingIdsAndTrims() {
        val (store, result) = WorkoutStore().create("  Legs ", " desc ", WorkoutConfig.Default, 42)
        assertEquals(SaveResult.Saved(1), result)
        assertEquals("Legs", store.workouts.single().name)
        assertEquals("desc", store.workouts.single().description)
        assertEquals(42, store.workouts.single().createdAtEpochMillis)
        assertEquals(2, store.nextId)
    }

    @Test
    fun limitIsEnforced() {
        val full = storeWith(MAX_SAVED_WORKOUTS)
        val (after, result) = full.create("extra", "", WorkoutConfig.Default, 0)
        assertIs<SaveResult.LimitReached>(result)
        assertEquals(full, after)
        // Updating is still allowed at the limit.
        assertIs<SaveResult.Saved>(full.update(1, "renamed", "", WorkoutConfig.Default).second)
    }

    @Test
    fun idsAreNotReusedAfterDelete() {
        val store = storeWith(2).delete(2)
        val (_, result) = store.create("new", "", WorkoutConfig.Default, 0)
        assertEquals(SaveResult.Saved(3), result)
    }

    @Test
    fun updateUnknownIdIsNotFound() {
        assertIs<SaveResult.NotFound>(storeWith(1).update(99, "x", "", WorkoutConfig.Default).second)
    }

    @Test
    fun moveReordersAndClamps() {
        val store = storeWith(3)
        assertEquals(listOf(2L, 1L, 3L), store.move(2, -1).workouts.map { it.id })
        assertEquals(listOf(2L, 3L, 1L), store.move(1, 5).workouts.map { it.id })
        assertEquals(store, store.move(1, -1))
    }

    @Test
    fun incrementUsage() {
        val store = storeWith(2).incrementUsage(2).incrementUsage(2)
        assertEquals(listOf(0, 2), store.workouts.map { it.usageCount })
    }

    @Test
    fun jsonRoundTripAndUnknownKeys() {
        val store = storeWith(1).update(
            1, "Adv", "", WorkoutConfig(plan = CyclePlan.Custom(listOf(com.amasesoft.tabatatimer.domain.Cycle("A", 10, 5)))),
        ).first
        val json = StorageJson.encodeToString(WorkoutStore.serializer(), store)
        assertEquals(store, StorageJson.decodeFromString(WorkoutStore.serializer(), json))
        // Forward compatibility: unknown fields from a newer version are ignored.
        val withExtra = json.replaceFirst("{", "{\"futureField\":true,")
        assertEquals(store, StorageJson.decodeFromString(WorkoutStore.serializer(), withExtra))
    }

    @Test
    fun createRejectsDuplicateNameIgnoringCaseAndSpaces() {
        val store = storeWith(2) // W1, W2
        val (after, result) = store.create("  w1 ", "", WorkoutConfig.Default, 0)
        assertIs<SaveResult.NameTaken>(result)
        assertEquals(store, after)
    }

    @Test
    fun updateRejectsNameOfAnotherWorkout() {
        val store = storeWith(2)
        val (after, result) = store.update(2, "W1", "", WorkoutConfig.Default)
        assertIs<SaveResult.NameTaken>(result)
        assertEquals(store, after)
    }

    @Test
    fun updateMayKeepOrRecaseItsOwnName() {
        val store = storeWith(2)
        assertEquals(SaveResult.Saved(1), store.update(1, "W1", "same", WorkoutConfig.Default).second)
        val (renamed, result) = store.update(1, " w1 ", "", WorkoutConfig.Default)
        assertEquals(SaveResult.Saved(1), result)
        assertEquals("w1", renamed.workouts.first().name)
    }
}
