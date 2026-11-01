package com.amasesoft.tabatatimer.ui.routines

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amasesoft.tabatatimer.data.AppliedIncrease
import com.amasesoft.tabatatimer.data.GymRepository
import com.amasesoft.tabatatimer.data.GymStore
import com.amasesoft.tabatatimer.domain.Exercise
import com.amasesoft.tabatatimer.domain.MAX_EXERCISES_PER_ROUTINE
import com.amasesoft.tabatatimer.domain.MAX_ROUTINES
import com.amasesoft.tabatatimer.domain.Progression
import com.amasesoft.tabatatimer.domain.Routine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException

/** A routine in the selector / manage list. */
@Immutable
data class RoutineSummary(val id: Long, val name: String, val exerciseCount: Int)

@Immutable
data class ExerciseItem(
    val id: Long,
    val name: String,
    val description: String,
    val sets: Int,
    val reps: Int,
    val weightHalfKg: Int?,
    val progression: Progression,
    val incrementHalfKg: Int,
    /** Weight after "Apply"; `null` when there is nothing to apply (Repeat, or no weight). */
    val nextWeightHalfKg: Int?,
)

/** Result of reading the routines file. */
sealed interface GymLoad {
    data object Loading : GymLoad
    data class Loaded(val store: GymStore) : GymLoad
    data object Error : GymLoad
}

@Immutable
data class RoutinesUiState(
    val isLoading: Boolean = true,
    /** The file could not be read: the routines are unknown (not "none"). */
    val loadError: Boolean = false,
    val routines: List<RoutineSummary> = emptyList(),
    val selectedId: Long? = null,
    val exercises: List<ExerciseItem> = emptyList(),
    val nameDialog: RoutineNameDialog? = null,
) {
    /** Only a known list allows creating or changing anything. */
    val isListAvailable: Boolean get() = !isLoading && !loadError

    val selected: RoutineSummary? get() = routines.firstOrNull { it.id == selectedId }
    val canAddRoutine: Boolean get() = routines.size < MAX_ROUTINES
    val canAddExercise: Boolean get() = exercises.size < MAX_EXERCISES_PER_ROUTINE
}

/**
 * MVVM, like the Home: a list with independent actions (select, toggle, apply, move, delete).
 * Writes go to the repository, and the screen is rebuilt from the file content it emits.
 */
class RoutinesViewModel(private val repository: GymRepository) : ViewModel() {

    private val _events = Channel<RoutinesEvent>(Channel.BUFFERED)
    val events: Flow<RoutinesEvent> = _events.receiveAsFlow()

    private val nameDialog = RoutineNameDialogController(viewModelScope, repository) { _events.send(it) }

    /** Each increment re-subscribes to the repository, i.e. re-reads the file. */
    private val reloadTrigger = MutableStateFlow(0)

    /** Selection shown immediately, while it is being written. */
    private val pendingSelection = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val load: Flow<GymLoad> = reloadTrigger.flatMapLatest {
        repository.store
            .map<GymStore, GymLoad> { GymLoad.Loaded(it) }
            .onStart { emit(GymLoad.Loading) }
            .catch { e -> if (e is IOException) emit(GymLoad.Error) else throw e }
    }

    val state: StateFlow<RoutinesUiState> = combine(load, pendingSelection, nameDialog.state) { load, pending, dialog ->
        when (load) {
            GymLoad.Loading -> RoutinesUiState(isLoading = true)
            GymLoad.Error -> RoutinesUiState(isLoading = false, loadError = true)
            is GymLoad.Loaded -> {
                val store = load.store
                val selected = store.routines.firstOrNull { it.id == pending } ?: store.selectedRoutine
                RoutinesUiState(
                    isLoading = false,
                    routines = store.routines.map { it.toSummary() },
                    selectedId = selected?.id,
                    exercises = selected?.exercises.orEmpty().map { it.toItem() },
                    nameDialog = dialog,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutinesUiState())

    /** "Retry" after a read error. */
    fun reload() = reloadTrigger.update { it + 1 }

    /** Shows [id] and remembers it for the next launch. */
    fun select(id: Long) {
        if (id == state.value.selectedId) return
        pendingSelection.value = id
        viewModelScope.launch {
            try {
                repository.selectRoutine(id)
            } catch (_: IOException) {
                _events.send(RoutinesEvent.StorageError)
            } finally {
                // From now on the stored selection is the truth (or the old one, if the write failed).
                pendingSelection.compareAndSet(id, null)
            }
        }
    }

    // region Name dialog (create)

    fun openNewRoutine() {
        if (!state.value.isListAvailable) return
        if (state.value.canAddRoutine) {
            nameDialog.openNew()
        } else {
            _events.trySend(RoutinesEvent.RoutineLimitReached)
        }
    }

    fun setDialogName(value: String) = nameDialog.setText(value)
    fun dismissDialog() = nameDialog.dismiss()
    fun submitDialog() = nameDialog.submit()

    // endregion

    // region Exercises

    fun toggleProgression(exerciseId: Long) = write { repository.toggleProgression(exerciseId) }

    /** weight += increment and back to Repeat; the screen offers Undo. Repeated taps apply once. */
    fun applyIncrease(exerciseId: Long) = write {
        val applied = repository.applyIncrease(exerciseId) ?: return@write
        _events.send(RoutinesEvent.IncreaseApplied(applied))
    }

    fun undoIncrease(applied: AppliedIncrease) = write { repository.undoIncrease(applied) }

    fun moveExercise(exerciseId: Long, offset: Int) = write { repository.moveExercise(exerciseId, offset) }

    fun deleteExercise(exerciseId: Long) = write { repository.deleteExercise(exerciseId) }

    // endregion

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (_: IOException) {
                _events.send(RoutinesEvent.StorageError)
            }
        }
    }
}

internal fun Routine.toSummary() = RoutineSummary(id = id, name = name, exerciseCount = exercises.size)

internal fun Exercise.toItem() = ExerciseItem(
    id = id,
    name = name,
    description = description,
    sets = sets,
    reps = reps,
    weightHalfKg = weightHalfKg,
    progression = progression,
    incrementHalfKg = incrementHalfKg,
    nextWeightHalfKg = nextWeightHalfKg,
)
