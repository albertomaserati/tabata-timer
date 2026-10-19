package com.amasesoft.tabatatimer.data

import androidx.datastore.core.DataStore
import com.amasesoft.tabatatimer.domain.SavedWorkout
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Reads and writes may throw [okio.IOException] (disk full, I/O failure): callers handle it
 * and report it to the user. In particular [workouts] does *not* turn a read error into an
 * empty list, so "no workouts" and "cannot read workouts" stay distinguishable; a new
 * collection re-reads the file (retry).
 */
interface WorkoutRepository {
    /** Saved workouts in the user's custom order. */
    val workouts: Flow<List<SavedWorkout>>

    suspend fun get(id: Long): SavedWorkout?
    suspend fun create(name: String, description: String, config: WorkoutConfig): SaveResult
    suspend fun update(id: Long, name: String, description: String, config: WorkoutConfig): SaveResult
    suspend fun delete(id: Long)
    suspend fun move(id: Long, offset: Int)
    suspend fun incrementUsage(id: Long)
}

class DataStoreWorkoutRepository(
    private val dataStore: DataStore<WorkoutStore>,
    private val now: () -> Long = ::currentEpochMillis,
) : WorkoutRepository {

    override val workouts: Flow<List<SavedWorkout>> = dataStore.data.map { it.workouts }

    override suspend fun get(id: Long): SavedWorkout? =
        dataStore.data.first().workouts.firstOrNull { it.id == id }

    override suspend fun create(name: String, description: String, config: WorkoutConfig): SaveResult =
        updateWithResult { it.create(name, description, config, now()) }

    override suspend fun update(id: Long, name: String, description: String, config: WorkoutConfig): SaveResult =
        updateWithResult { it.update(id, name, description, config) }

    override suspend fun delete(id: Long) {
        dataStore.updateData { it.delete(id) }
    }

    override suspend fun move(id: Long, offset: Int) {
        dataStore.updateData { it.move(id, offset) }
    }

    override suspend fun incrementUsage(id: Long) {
        dataStore.updateData { it.incrementUsage(id) }
    }

    private suspend fun updateWithResult(op: (WorkoutStore) -> Pair<WorkoutStore, SaveResult>): SaveResult {
        var result: SaveResult = SaveResult.NotFound
        dataStore.updateData { store ->
            val (newStore, opResult) = op(store)
            result = opResult
            newStore
        }
        return result
    }
}

@OptIn(ExperimentalTime::class)
internal fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
