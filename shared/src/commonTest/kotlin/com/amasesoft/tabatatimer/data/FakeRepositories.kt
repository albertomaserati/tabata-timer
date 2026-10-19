package com.amasesoft.tabatatimer.data

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
