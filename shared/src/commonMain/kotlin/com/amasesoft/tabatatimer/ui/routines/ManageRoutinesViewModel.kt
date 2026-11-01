package com.amasesoft.tabatatimer.ui.routines

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amasesoft.tabatatimer.data.GymRepository
import com.amasesoft.tabatatimer.data.GymResult
import com.amasesoft.tabatatimer.data.GymStore
import com.amasesoft.tabatatimer.domain.MAX_ROUTINES
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

@Immutable
data class ManageRoutinesUiState(
    val isLoading: Boolean = true,
    val loadError: Boolean = false,
    val routines: List<RoutineSummary> = emptyList(),
    val nameDialog: RoutineNameDialog? = null,
) {
    val isListAvailable: Boolean get() = !isLoading && !loadError
    val canAddRoutine: Boolean get() = routines.size < MAX_ROUTINES
}

/** MVVM: list of routines with rename / duplicate / delete / reorder / create. */
class ManageRoutinesViewModel(private val repository: GymRepository) : ViewModel() {

    private val _events = Channel<RoutinesEvent>(Channel.BUFFERED)
    val events: Flow<RoutinesEvent> = _events.receiveAsFlow()

    private val nameDialog = RoutineNameDialogController(viewModelScope, repository) { _events.send(it) }

    private val reloadTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val load: Flow<GymLoad> = reloadTrigger.flatMapLatest {
        repository.store
            .map<GymStore, GymLoad> { GymLoad.Loaded(it) }
            .onStart { emit(GymLoad.Loading) }
            .catch { e -> if (e is IOException) emit(GymLoad.Error) else throw e }
    }

    val state: StateFlow<ManageRoutinesUiState> = combine(load, nameDialog.state) { load, dialog ->
        when (load) {
            GymLoad.Loading -> ManageRoutinesUiState(isLoading = true)
            GymLoad.Error -> ManageRoutinesUiState(isLoading = false, loadError = true)
            is GymLoad.Loaded -> ManageRoutinesUiState(
                isLoading = false,
                routines = load.store.routines.map { it.toSummary() },
                nameDialog = dialog,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageRoutinesUiState())

    fun reload() = reloadTrigger.update { it + 1 }

    fun openNewRoutine() {
        if (!state.value.isListAvailable) return
        if (state.value.canAddRoutine) nameDialog.openNew() else _events.trySend(RoutinesEvent.RoutineLimitReached)
    }

    fun openRename(id: Long) {
        val routine = state.value.routines.firstOrNull { it.id == id } ?: return
        nameDialog.openRename(id, routine.name)
    }

    fun setDialogName(value: String) = nameDialog.setText(value)
    fun dismissDialog() = nameDialog.dismiss()
    fun submitDialog() = nameDialog.submit()

    /** @param copyWord localized "copy", used for the unique "<name> (copy)" name. */
    fun duplicate(id: Long, copyWord: String) = write {
        if (repository.duplicateRoutine(id, copyWord) == GymResult.LimitReached) {
            _events.send(RoutinesEvent.RoutineLimitReached)
        }
    }

    fun delete(id: Long) = write { repository.deleteRoutine(id) }

    fun move(id: Long, offset: Int) = write { repository.moveRoutine(id, offset) }

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
