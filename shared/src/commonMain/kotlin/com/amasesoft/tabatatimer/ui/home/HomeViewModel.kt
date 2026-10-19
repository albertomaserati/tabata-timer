package com.amasesoft.tabatatimer.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amasesoft.tabatatimer.data.SettingsRepository
import com.amasesoft.tabatatimer.data.WorkoutRepository
import com.amasesoft.tabatatimer.domain.MAX_SAVED_WORKOUTS
import com.amasesoft.tabatatimer.domain.SavedWorkout
import com.amasesoft.tabatatimer.domain.WorkoutSort
import com.amasesoft.tabatatimer.domain.sortedFor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.IOException

@Immutable
data class WorkoutItem(
    val id: Long,
    val name: String,
    val description: String,
    val totalSeconds: Int,
    val sets: Int,
    val cycles: Int,
    val isAdvanced: Boolean,
    val usageCount: Int,
)

/** Result of reading the saved workouts. */
sealed interface WorkoutsLoad {
    data object Loading : WorkoutsLoad
    data class Loaded(val workouts: List<SavedWorkout>) : WorkoutsLoad
    data object Error : WorkoutsLoad
}

@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    /** The saved workouts could not be read: the list is unknown (not empty). */
    val loadError: Boolean = false,
    val workouts: List<WorkoutItem> = emptyList(),
    val sort: WorkoutSort = WorkoutSort.Custom,
    val isReordering: Boolean = false,
) {
    /** True only when the list is known: while loading or after a read error, the limit cannot be checked. */
    val isListAvailable: Boolean get() = !isLoading && !loadError

    val isAtLimit: Boolean get() = workouts.size >= MAX_SAVED_WORKOUTS
}

/**
 * MVVM: a list with independent actions (sort, delete, reorder). The sort criterion lives in
 * the persisted settings, so it is restored on next launch.
 */
class HomeViewModel(
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val isReordering = MutableStateFlow(false)

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    /** Each increment re-subscribes to the repository, i.e. re-reads the file. */
    private val reloadTrigger = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val workoutsLoad: Flow<WorkoutsLoad> = reloadTrigger.flatMapLatest {
        workoutRepository.workouts
            .map<List<SavedWorkout>, WorkoutsLoad> { WorkoutsLoad.Loaded(it) }
            .onStart { emit(WorkoutsLoad.Loading) }
            .catch { e -> if (e is IOException) emit(WorkoutsLoad.Error) else throw e }
    }

    val state: StateFlow<HomeUiState> = combine(
        workoutsLoad,
        settingsRepository.settings.map { it.workoutSort },
        isReordering,
    ) { load, sort, reordering ->
        when (load) {
            WorkoutsLoad.Loading -> HomeUiState(isLoading = true, sort = sort)
            WorkoutsLoad.Error -> HomeUiState(isLoading = false, loadError = true, sort = sort)
            is WorkoutsLoad.Loaded -> HomeUiState(
                isLoading = false,
                workouts = load.workouts.sortedFor(sort).map { it.toItem() },
                sort = sort,
                isReordering = reordering && sort == WorkoutSort.Custom,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** "Retry" after a read error. */
    fun reload() {
        isReordering.value = false
        reloadTrigger.update { it + 1 }
    }

    fun setSort(sort: WorkoutSort) {
        if (sort != WorkoutSort.Custom) isReordering.value = false
        write { settingsRepository.update { it.copy(workoutSort = sort) } }
    }

    /** Manual reordering only makes sense on the custom order, so it switches to it. */
    fun setReordering(enabled: Boolean) {
        if (enabled && state.value.sort != WorkoutSort.Custom) setSort(WorkoutSort.Custom)
        isReordering.value = enabled
    }

    fun move(id: Long, offset: Int) = write { workoutRepository.move(id, offset) }

    /** Moves to the top/bottom of the custom order, switching the list to it. */
    fun moveToEdge(id: Long, toTop: Boolean) {
        if (state.value.sort != WorkoutSort.Custom) setSort(WorkoutSort.Custom)
        write { workoutRepository.move(id, if (toTop) -MAX_SAVED_WORKOUTS else MAX_SAVED_WORKOUTS) }
    }

    fun delete(id: Long) = write { workoutRepository.delete(id) }

    private var startJob: Job? = null

    /** Ignores repeated taps while the previous start is still being resolved. */
    fun start(id: Long) {
        if (startJob?.isActive == true) return
        startJob = viewModelScope.launch {
            val workout = try {
                workoutRepository.get(id)
            } catch (_: IOException) {
                _events.send(HomeEvent.StorageError)
                null
            } ?: return@launch
            _events.send(HomeEvent.StartRun(workout))
        }
    }

    private fun write(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (_: IOException) {
                _events.send(HomeEvent.StorageError)
            }
        }
    }
}

sealed interface HomeEvent {
    data class StartRun(val workout: SavedWorkout) : HomeEvent
    data object StorageError : HomeEvent
}

private fun SavedWorkout.toItem() = WorkoutItem(
    id = id,
    name = name,
    description = description,
    totalSeconds = totalSeconds,
    sets = config.sets,
    cycles = config.plan.count,
    isAdvanced = config.isAdvanced,
    usageCount = usageCount,
)
