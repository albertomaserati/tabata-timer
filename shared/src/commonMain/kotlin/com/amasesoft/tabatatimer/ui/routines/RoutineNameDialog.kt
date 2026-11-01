package com.amasesoft.tabatatimer.ui.routines

import androidx.compose.runtime.Immutable
import com.amasesoft.tabatatimer.data.AppliedIncrease
import com.amasesoft.tabatatimer.data.GymRepository
import com.amasesoft.tabatatimer.data.GymResult
import com.amasesoft.tabatatimer.domain.GymNameError
import com.amasesoft.tabatatimer.domain.RoutineLimits
import com.amasesoft.tabatatimer.domain.validateGymName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException

/** What the name dialog does on confirm. */
sealed interface RoutineNameTarget {
    data object New : RoutineNameTarget
    data class Rename(val id: Long) : RoutineNameTarget
}

@Immutable
data class RoutineNameDialog(
    val target: RoutineNameTarget,
    val text: String,
    val error: GymNameError? = null,
    val isSaving: Boolean = false,
)

/** One-shot events shared by the routine screens. */
sealed interface RoutinesEvent {
    data object StorageError : RoutinesEvent
    data object RoutineLimitReached : RoutinesEvent
    data object ExerciseLimitReached : RoutinesEvent

    /** "Apply" done: the screen shows "Weight updated to ..." with Undo. */
    data class IncreaseApplied(val applied: AppliedIncrease) : RoutinesEvent
}

/**
 * Create/rename dialog logic, shared by the Routines tab and the Manage routines screen.
 * Local checks (empty, too long) are immediate; uniqueness is checked by the store, atomically
 * with the write, and shown inline like the workout editor.
 */
internal class RoutineNameDialogController(
    private val scope: CoroutineScope,
    private val repository: GymRepository,
    private val sendEvent: suspend (RoutinesEvent) -> Unit,
) {
    private val _state = MutableStateFlow<RoutineNameDialog?>(null)
    val state: StateFlow<RoutineNameDialog?> = _state.asStateFlow()

    fun openNew() {
        _state.value = RoutineNameDialog(RoutineNameTarget.New, text = "")
    }

    fun openRename(id: Long, currentName: String) {
        _state.value = RoutineNameDialog(RoutineNameTarget.Rename(id), text = currentName)
    }

    fun setText(value: String) = _state.update { it?.copy(text = value, error = null) }

    fun dismiss() {
        _state.value = null
    }

    fun submit() {
        val dialog = _state.value ?: return
        if (dialog.isSaving) return
        validateGymName(dialog.text, RoutineLimits.NAME_MAX_LENGTH)?.let { error ->
            _state.value = dialog.copy(error = error)
            return
        }
        _state.value = dialog.copy(isSaving = true)
        scope.launch {
            val result = try {
                when (val target = dialog.target) {
                    RoutineNameTarget.New -> repository.createRoutine(dialog.text)
                    is RoutineNameTarget.Rename -> repository.renameRoutine(target.id, dialog.text)
                }
            } catch (_: IOException) {
                null
            }
            when (result) {
                null -> {
                    _state.update { it?.copy(isSaving = false) }
                    sendEvent(RoutinesEvent.StorageError)
                }

                is GymResult.Saved, GymResult.NotFound -> _state.value = null
                GymResult.NameTaken -> _state.update { it?.copy(isSaving = false, error = GymNameError.Duplicate) }
                GymResult.LimitReached -> {
                    _state.value = null
                    sendEvent(RoutinesEvent.RoutineLimitReached)
                }
            }
        }
    }
}
