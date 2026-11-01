package com.amasesoft.tabatatimer.data

import com.amasesoft.tabatatimer.domain.Exercise
import com.amasesoft.tabatatimer.domain.SavedWorkout
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FakeWorkoutRepository(initial: WorkoutStore = WorkoutStore()) : WorkoutRepository {
    val store = MutableStateFlow(initial)

    /** When true, reading [workouts] fails like an unreadable file (checked at each collection). */
    var failReads = false

    override val workouts: Flow<List<SavedWorkout>> = flow {
        if (failReads) throw okio.IOException("unreadable")
        emitAll(store.map { it.workouts })
    }
    override suspend fun get(id: Long) = store.value.workouts.firstOrNull { it.id == id }

    override suspend fun create(name: String, description: String, config: WorkoutConfig): SaveResult {
        val (s, r) = store.value.create(name, description, config, 0)
        store.value = s
        return r
    }

    override suspend fun update(id: Long, name: String, description: String, config: WorkoutConfig): SaveResult {
        val (s, r) = store.value.update(id, name, description, config)
        store.value = s
        return r
    }

    override suspend fun delete(id: Long) { store.value = store.value.delete(id) }
    override suspend fun move(id: Long, offset: Int) { store.value = store.value.move(id, offset) }
    override suspend fun incrementUsage(id: Long) { store.value = store.value.incrementUsage(id) }
}

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    val state = MutableStateFlow(initial)
    override val settings: Flow<AppSettings> = state
    override suspend fun update(transform: (AppSettings) -> AppSettings) { state.value = transform(state.value) }
}

/** Every read and write fails, as with a broken disk. */
class FailingWorkoutRepository : WorkoutRepository {
    override val workouts: Flow<List<SavedWorkout>> = flow { throw okio.IOException("unreadable") }
    override suspend fun get(id: Long): SavedWorkout? = null
    override suspend fun create(name: String, description: String, config: WorkoutConfig): SaveResult = throw okio.IOException("disk full")
    override suspend fun update(id: Long, name: String, description: String, config: WorkoutConfig): SaveResult = throw okio.IOException("disk full")
    override suspend fun delete(id: Long) = throw okio.IOException("disk full")
    override suspend fun move(id: Long, offset: Int) = throw okio.IOException("disk full")
    override suspend fun incrementUsage(id: Long) = throw okio.IOException("disk full")
}

/** In-memory [GymRepository] running the real pure store operations. */
class FakeGymRepository(initial: GymStore = GymStore()) : GymRepository {
    val state = MutableStateFlow(initial)

    /** When true, reading [store] fails like an unreadable file (checked at each collection). */
    var failReads = false

    /** When true, every write fails like a full disk. */
    var failWrites = false

    override val store: Flow<GymStore> = flow {
        if (failReads) throw okio.IOException("unreadable")
        emitAll(state)
    }

    private fun <R> write(op: (GymStore) -> Pair<GymStore, R>): R {
        if (failWrites) throw okio.IOException("disk full")
        val (s, r) = op(state.value)
        state.value = s
        return r
    }

    private fun update(op: (GymStore) -> GymStore) = write { op(it) to Unit }

    override suspend fun exercise(id: Long) =
        if (failReads) throw okio.IOException("unreadable") else state.value.exercise(id)

    override suspend fun createRoutine(name: String) = write { it.createRoutine(name) }
    override suspend fun renameRoutine(id: Long, name: String) = write { it.renameRoutine(id, name) }
    override suspend fun duplicateRoutine(id: Long, copyWord: String) = write { it.duplicateRoutine(id, copyWord) }
    override suspend fun deleteRoutine(id: Long) = update { it.deleteRoutine(id) }
    override suspend fun moveRoutine(id: Long, offset: Int) = update { it.moveRoutine(id, offset) }
    override suspend fun selectRoutine(id: Long) = update { it.selectRoutine(id) }
    override suspend fun addExercise(routineId: Long, exercise: Exercise) = write { it.addExercise(routineId, exercise) }
    override suspend fun updateExercise(exercise: Exercise) = write { it.updateExercise(exercise) }
    override suspend fun deleteExercise(id: Long) = update { it.deleteExercise(id) }
    override suspend fun moveExercise(id: Long, offset: Int) = update { it.moveExercise(id, offset) }
    override suspend fun toggleProgression(id: Long) = update { it.toggleProgression(id) }
    override suspend fun applyIncrease(id: Long) = write { it.applyIncrease(id) }
    override suspend fun undoIncrease(applied: AppliedIncrease) = update { it.undoIncrease(applied) }
}
