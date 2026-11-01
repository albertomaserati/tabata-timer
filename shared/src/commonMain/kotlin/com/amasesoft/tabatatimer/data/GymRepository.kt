package com.amasesoft.tabatatimer.data

import androidx.datastore.core.DataStore
import com.amasesoft.tabatatimer.domain.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Gym routines ("schede"), stored in their own file. Same contract as [WorkoutRepository]:
 * reads and writes may throw [okio.IOException]; [store] does *not* turn a read error into an
 * empty document, and a new collection re-reads the file (retry).
 */
interface GymRepository {
    val store: Flow<GymStore>

    suspend fun exercise(id: Long): Exercise?

    suspend fun createRoutine(name: String): GymResult
    suspend fun renameRoutine(id: Long, name: String): GymResult
    suspend fun duplicateRoutine(id: Long, copyWord: String): GymResult
    suspend fun deleteRoutine(id: Long)
    suspend fun moveRoutine(id: Long, offset: Int)
    suspend fun selectRoutine(id: Long)

    suspend fun addExercise(routineId: Long, exercise: Exercise): GymResult
    suspend fun updateExercise(exercise: Exercise): GymResult
    suspend fun deleteExercise(id: Long)
    suspend fun moveExercise(id: Long, offset: Int)
    suspend fun toggleProgression(id: Long)

    /** @return the new weight and the values for [undoIncrease]; `null` if nothing was applied. */
    suspend fun applyIncrease(id: Long): AppliedIncrease?
    /** Undo of [applyIncrease]; no effect if the exercise changed in the meantime. */
    suspend fun undoIncrease(applied: AppliedIncrease)
}

class DataStoreGymRepository(private val dataStore: DataStore<GymStore>) : GymRepository {

    override val store: Flow<GymStore> = dataStore.data

    override suspend fun exercise(id: Long): Exercise? = dataStore.data.first().exercise(id)

    override suspend fun createRoutine(name: String) = updateWithResult { it.createRoutine(name) }
    override suspend fun renameRoutine(id: Long, name: String) = updateWithResult { it.renameRoutine(id, name) }
    override suspend fun duplicateRoutine(id: Long, copyWord: String) =
        updateWithResult { it.duplicateRoutine(id, copyWord) }

    override suspend fun deleteRoutine(id: Long) { dataStore.updateData { it.deleteRoutine(id) } }
    override suspend fun moveRoutine(id: Long, offset: Int) { dataStore.updateData { it.moveRoutine(id, offset) } }
    override suspend fun selectRoutine(id: Long) { dataStore.updateData { it.selectRoutine(id) } }

    override suspend fun addExercise(routineId: Long, exercise: Exercise) =
        updateWithResult { it.addExercise(routineId, exercise) }

    override suspend fun updateExercise(exercise: Exercise) = updateWithResult { it.updateExercise(exercise) }

    override suspend fun deleteExercise(id: Long) { dataStore.updateData { it.deleteExercise(id) } }
    override suspend fun moveExercise(id: Long, offset: Int) { dataStore.updateData { it.moveExercise(id, offset) } }
    override suspend fun toggleProgression(id: Long) { dataStore.updateData { it.toggleProgression(id) } }

    override suspend fun applyIncrease(id: Long): AppliedIncrease? {
        var snapshot: AppliedIncrease? = null
        dataStore.updateData { store ->
            val (newStore, result) = store.applyIncrease(id)
            snapshot = result
            newStore
        }
        return snapshot
    }

    override suspend fun undoIncrease(applied: AppliedIncrease) {
        dataStore.updateData { it.undoIncrease(applied) }
    }

    private suspend fun updateWithResult(op: (GymStore) -> Pair<GymStore, GymResult>): GymResult {
        var result: GymResult = GymResult.NotFound
        dataStore.updateData { store ->
            val (newStore, opResult) = op(store)
            result = opResult
            newStore
        }
        return result
    }
}
