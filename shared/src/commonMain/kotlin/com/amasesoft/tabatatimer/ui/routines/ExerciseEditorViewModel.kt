package com.amasesoft.tabatatimer.ui.routines

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amasesoft.tabatatimer.data.GymRepository
import com.amasesoft.tabatatimer.data.GymResult
import com.amasesoft.tabatatimer.domain.Exercise
import com.amasesoft.tabatatimer.domain.ExerciseLimits
import com.amasesoft.tabatatimer.domain.GymNameError
import com.amasesoft.tabatatimer.domain.Progression
import com.amasesoft.tabatatimer.domain.validateGymName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException

/**
 * Form content. The weight is kept while "Use weight" is off ([useWeight] = false), so turning
 * it back on restores the last value instead of a default.
 */
@Immutable
data class ExerciseDraft(
    val name: String = "",
    val description: String = "",
    val sets: Int = ExerciseLimits.DEFAULT_SETS,
    val reps: Int = ExerciseLimits.DEFAULT_REPS,
    val useWeight: Boolean = true,
    val weightHalfKg: Int = ExerciseLimits.DEFAULT_WEIGHT_HALF_KG,
    val progression: Progression = Progression.Repeat,
    val incrementHalfKg: Int = ExerciseLimits.DEFAULT_INCREMENT_HALF_KG,
) {
    fun toExercise(id: Long): Exercise = Exercise(
        id = id,
        name = name.trim(),
        description = description.trim(),
        sets = sets,
        reps = reps,
        weightHalfKg = if (useWeight) weightHalfKg else null,
        progression = progression,
        incrementHalfKg = incrementHalfKg,
    )

    companion object {
        fun of(exercise: Exercise): ExerciseDraft = exercise.sanitized().let {
            ExerciseDraft(
                name = it.name,
                description = it.description,
                sets = it.sets,
                reps = it.reps,
                useWeight = it.weightHalfKg != null,
                weightHalfKg = it.weightHalfKg ?: ExerciseLimits.DEFAULT_WEIGHT_HALF_KG,
                progression = it.progression,
                incrementHalfKg = it.incrementHalfKg,
            )
        }
    }
}

@Immutable
data class ExerciseEditorUiState(
    val isNew: Boolean,
    val isLoading: Boolean = true,
    val draft: ExerciseDraft = ExerciseDraft(),
    val nameError: GymNameError? = null,
    val isSaving: Boolean = false,
    val showDiscardConfirm: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    /** Content as loaded; null while loading. */
    val savedDraft: ExerciseDraft? = null,
) {
    /** Compared as saved values: e.g. a weight hidden by "Use weight" off does not count. */
    val hasUnsavedChanges: Boolean
        get() = savedDraft != null && savedDraft.toExercise(0) != draft.toExercise(0)
}

sealed interface ExerciseEditorEvent {
    data object Close : ExerciseEditorEvent
    data object StorageError : ExerciseEditorEvent
    data object LimitReached : ExerciseEditorEvent
}

/**
 * MVVM: a form of independent fields, like the workout editor.
 *
 * @param exerciseId `null` for a new exercise, added at the end of [routineId].
 */
class ExerciseEditorViewModel(
    private val routineId: Long,
    private val exerciseId: Long?,
    private val repository: GymRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ExerciseEditorUiState(isNew = exerciseId == null))
    val state: StateFlow<ExerciseEditorUiState> = _state.asStateFlow()

    private val _events = Channel<ExerciseEditorEvent>(Channel.BUFFERED)
    val events: Flow<ExerciseEditorEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (exerciseId == null) {
                loaded(ExerciseDraft())
                return@launch
            }
            try {
                val exercise = repository.exercise(exerciseId)
                if (exercise == null) _events.send(ExerciseEditorEvent.Close) else loaded(ExerciseDraft.of(exercise))
            } catch (_: IOException) {
                // Never let an unreadable exercise be overwritten with defaults.
                _events.send(ExerciseEditorEvent.StorageError)
                _events.send(ExerciseEditorEvent.Close)
            }
        }
    }

    private fun loaded(draft: ExerciseDraft) {
        _state.update { it.copy(isLoading = false, draft = draft, savedDraft = draft) }
    }

    private fun edit(transform: (ExerciseDraft) -> ExerciseDraft) = _state.update { it.copy(draft = transform(it.draft)) }

    // region Fields

    /** Not truncated: a name over the limit shows an inline error (e.g. after a paste). */
    fun setName(value: String) = _state.update { it.copy(draft = it.draft.copy(name = value), nameError = null) }

    fun setDescription(value: String) = edit { it.copy(description = value.take(ExerciseLimits.DESCRIPTION_MAX_LENGTH)) }
    fun setSets(value: Int) = edit { it.copy(sets = value.coerceIn(ExerciseLimits.sets)) }
    fun setReps(value: Int) = edit { it.copy(reps = value.coerceIn(ExerciseLimits.reps)) }
    fun setUseWeight(value: Boolean) = edit { it.copy(useWeight = value) }
    fun setWeight(halfKg: Int) = edit { it.copy(weightHalfKg = halfKg.coerceIn(ExerciseLimits.weightHalfKg)) }
    fun setProgression(value: Progression) = edit { it.copy(progression = value) }
    fun setIncrement(halfKg: Int) = edit { it.copy(incrementHalfKg = halfKg.coerceIn(ExerciseLimits.incrementHalfKg)) }

    // endregion

    // region Save / delete

    fun save() {
        val current = _state.value
        if (current.isLoading || current.isSaving) return
        validateGymName(current.draft.name, ExerciseLimits.NAME_MAX_LENGTH)?.let { error ->
            _state.value = current.copy(nameError = error)
            return
        }
        _state.value = current.copy(isSaving = true)
        viewModelScope.launch {
            val result = try {
                if (exerciseId == null) {
                    repository.addExercise(routineId, current.draft.toExercise(0))
                } else {
                    repository.updateExercise(current.draft.toExercise(exerciseId))
                }
            } catch (_: IOException) {
                null
            }
            _state.update { it.copy(isSaving = false) }
            when (result) {
                null -> _events.send(ExerciseEditorEvent.StorageError)
                is GymResult.Saved -> {
                    _state.update { it.copy(savedDraft = current.draft) }
                    _events.send(ExerciseEditorEvent.Close)
                }

                GymResult.LimitReached -> _events.send(ExerciseEditorEvent.LimitReached)
                // The routine/exercise was deleted meanwhile; names of exercises need not be unique.
                GymResult.NotFound, GymResult.NameTaken -> _events.send(ExerciseEditorEvent.Close)
            }
        }
    }

    fun requestDelete() {
        if (exerciseId != null && !_state.value.isLoading) _state.update { it.copy(showDeleteConfirm = true) }
    }

    fun dismissDelete() = _state.update { it.copy(showDeleteConfirm = false) }

    fun confirmDelete() {
        val id = exerciseId ?: return
        _state.update { it.copy(showDeleteConfirm = false) }
        viewModelScope.launch {
            try {
                repository.deleteExercise(id)
                _events.send(ExerciseEditorEvent.Close)
            } catch (_: IOException) {
                _events.send(ExerciseEditorEvent.StorageError)
            }
        }
    }

    // endregion

    // region Leaving

    /** Back/up: asks before discarding unsaved changes. */
    fun requestBack() {
        if (_state.value.hasUnsavedChanges) {
            _state.update { it.copy(showDiscardConfirm = true) }
        } else {
            _events.trySend(ExerciseEditorEvent.Close)
        }
    }

    fun confirmDiscard() {
        _state.update { it.copy(showDiscardConfirm = false) }
        _events.trySend(ExerciseEditorEvent.Close)
    }

    fun dismissDiscard() = _state.update { it.copy(showDiscardConfirm = false) }

    // endregion
}
