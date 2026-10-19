package com.amasesoft.tabatatimer.ui.editor

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amasesoft.tabatatimer.data.SaveResult
import com.amasesoft.tabatatimer.data.SettingsRepository
import com.amasesoft.tabatatimer.data.WorkoutRepository
import com.amasesoft.tabatatimer.domain.Cycle
import com.amasesoft.tabatatimer.domain.CyclePlan
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import com.amasesoft.tabatatimer.domain.WorkoutConfig.Limits
import com.amasesoft.tabatatimer.domain.WorkoutPlanner
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okio.IOException

/**
 * - [Quick]: configure and start immediately; saving is optional (dialog asks name/description).
 * - [New]/[Edit]: name, description and configuration of a saved workout.
 */
enum class EditorMode { Quick, New, Edit }

/** A cycle in the advanced editor; [key] is a stable identity for list animations. */
@Immutable
data class EditableCycle(
    val key: Long,
    val name: String,
    val workSeconds: Int,
    val restSeconds: Int,
) {
    fun toCycle() = Cycle(name = name.trim(), workSeconds = workSeconds, restSeconds = restSeconds)
}

/** What "saved" means for the unsaved-changes check. */
@Immutable
data class EditorSnapshot(val name: String, val description: String, val config: WorkoutConfig)

/** Why the name cannot be saved; shown inline under the name field. */
enum class NameError { Empty, Duplicate }

@Immutable
data class EditorUiState(
    val mode: EditorMode,
    val isLoading: Boolean = true,
    val workoutId: Long? = null,
    val name: String = "",
    val description: String = "",
    val prepareSeconds: Int = WorkoutConfig.Default.prepareSeconds,
    val sets: Int = WorkoutConfig.Default.sets,
    val restBetweenSetsSeconds: Int = WorkoutConfig.Default.restBetweenSetsSeconds,
    val isAdvanced: Boolean = false,
    /** Values of the simple mode (also kept while in advanced mode, unused there). */
    val uniform: CyclePlan.Uniform = WorkoutConfig.DefaultPlan,
    /** Cycles of the advanced mode. */
    val cycles: List<EditableCycle> = emptyList(),
    val showSimplifyConfirm: Boolean = false,
    val showSaveDialog: Boolean = false,
    val nameError: NameError? = null,
    val isSaving: Boolean = false,
    val showDiscardConfirm: Boolean = false,
    /** Content as loaded or last saved; null while loading. */
    val savedSnapshot: EditorSnapshot? = null,
) {
    fun snapshot() = EditorSnapshot(name.trim(), description.trim(), toConfig())

    /** Only New/Edit ask before leaving: a quick workout is meant to be thrown away. */
    val hasUnsavedChanges: Boolean
        get() = mode != EditorMode.Quick && savedSnapshot != null && savedSnapshot != snapshot()

    fun toConfig(): WorkoutConfig = WorkoutConfig(
        prepareSeconds = prepareSeconds,
        sets = sets,
        restBetweenSetsSeconds = restBetweenSetsSeconds,
        plan = if (isAdvanced) CyclePlan.Custom(cycles.map { it.toCycle() }) else uniform,
    )

    val totalSeconds: Int = WorkoutPlanner.totalSeconds(toConfig())
}

sealed interface EditorEvent {
    data object Close : EditorEvent
    data object Saved : EditorEvent
    data object LimitReached : EditorEvent
    data object StorageError : EditorEvent
    data class StartRun(val config: WorkoutConfig, val title: String?, val workoutId: Long?) : EditorEvent
}

/**
 * MVVM: the editor is a form with independent fields, so plain intent functions over one
 * immutable state are enough; the only interdependent rule (simple <-> advanced) is a small
 * explicit transition with a confirmation step.
 */
class EditorViewModel(
    private val mode: EditorMode,
    private val workoutId: Long?,
    private val workoutRepository: WorkoutRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState(mode = mode))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    private var nextKey = 0L

    init {
        viewModelScope.launch {
            try {
                loadInitial()
            } catch (_: IOException) {
                _events.send(EditorEvent.StorageError)
                // Never let an unreadable workout be overwritten with defaults.
                if (mode == EditorMode.Edit) _events.send(EditorEvent.Close)
                else load(WorkoutConfig.Default, name = "", description = "", id = null)
            }
        }
    }

    private suspend fun loadInitial() {
            when (mode) {
                EditorMode.Quick -> load(settingsRepository.settings.first().lastQuickConfig, name = "", description = "", id = null)
                EditorMode.New -> load(WorkoutConfig.Default, name = "", description = "", id = null)
                EditorMode.Edit -> {
                    val workout = workoutId?.let { workoutRepository.get(it) }
                    if (workout == null) {
                        _events.send(EditorEvent.Close)
                    } else {
                        load(workout.config, workout.name, workout.description, workout.id)
                    }
                }
            }
    }

    private fun load(config: WorkoutConfig, name: String, description: String, id: Long?) {
        val safe = config.sanitized()
        _state.value = EditorUiState(
            mode = mode,
            isLoading = false,
            workoutId = id,
            name = name,
            description = description,
            prepareSeconds = safe.prepareSeconds,
            sets = safe.sets,
            restBetweenSetsSeconds = safe.restBetweenSetsSeconds,
            isAdvanced = safe.plan is CyclePlan.Custom,
            uniform = when (val plan = safe.plan) {
                is CyclePlan.Uniform -> plan
                is CyclePlan.Custom -> plan.toUniform()
            },
            cycles = safe.plan.expand().map { it.toEditable() },
        ).let { it.copy(savedSnapshot = it.snapshot()) }
    }

    private fun Cycle.toEditable() = EditableCycle(nextKey++, name, workSeconds, restSeconds)

    // region Fields

    fun setName(value: String) = _state.update {
        it.copy(name = value.take(Limits.NAME_MAX_LENGTH), nameError = null)
    }

    fun setDescription(value: String) = _state.update {
        it.copy(description = value.take(Limits.DESCRIPTION_MAX_LENGTH))
    }

    fun setPrepare(value: Int) = _state.update { it.copy(prepareSeconds = value.coerceIn(Limits.prepareSeconds)) }
    fun setSets(value: Int) = _state.update { it.copy(sets = value.coerceIn(Limits.sets)) }
    fun setRestBetweenSets(value: Int) = _state.update { it.copy(restBetweenSetsSeconds = value.coerceIn(Limits.restSeconds)) }

    fun setUniformWork(value: Int) = _state.update { it.copy(uniform = it.uniform.copy(workSeconds = value.coerceIn(Limits.workSeconds))) }
    fun setUniformRest(value: Int) = _state.update { it.copy(uniform = it.uniform.copy(restSeconds = value.coerceIn(Limits.restSeconds))) }
    fun setUniformCount(value: Int) = _state.update { it.copy(uniform = it.uniform.copy(count = value.coerceIn(Limits.cycles))) }

    // endregion

    // region Simple <-> advanced

    /** Simple -> advanced is always lossless: the uniform cycles are expanded, prefilled. */
    fun setAdvanced(advanced: Boolean) {
        val current = _state.value
        if (advanced == current.isAdvanced) return
        if (advanced) {
            _state.value = current.copy(isAdvanced = true, cycles = current.uniform.expand().map { it.toEditable() })
        } else {
            val custom = CyclePlan.Custom(current.cycles.map { it.toCycle() })
            _state.value = if (custom.isUniform()) {
                current.copy(isAdvanced = false, uniform = custom.toUniform())
            } else {
                // Would lose names/durations: ask first.
                current.copy(showSimplifyConfirm = true)
            }
        }
    }

    fun confirmSimplify() = _state.update {
        it.copy(
            isAdvanced = false,
            showSimplifyConfirm = false,
            uniform = CyclePlan.Custom(it.cycles.map(EditableCycle::toCycle)).toUniform(),
        )
    }

    fun dismissSimplify() = _state.update { it.copy(showSimplifyConfirm = false) }

    // endregion

    // region Advanced cycles

    fun updateCycle(key: Long, transform: (EditableCycle) -> EditableCycle) = _state.update { state ->
        state.copy(cycles = state.cycles.map { cycle ->
            if (cycle.key != key) cycle else transform(cycle).let {
                it.copy(
                    name = it.name.take(Limits.NAME_MAX_LENGTH),
                    workSeconds = it.workSeconds.coerceIn(Limits.workSeconds),
                    restSeconds = it.restSeconds.coerceIn(Limits.restSeconds),
                )
            }
        })
    }

    fun addCycle() = _state.update { state ->
        if (state.cycles.size >= Limits.cycles.last) return@update state
        val template = state.cycles.lastOrNull()
        val new = EditableCycle(
            key = nextKey++,
            name = "",
            workSeconds = template?.workSeconds ?: state.uniform.workSeconds,
            restSeconds = template?.restSeconds ?: state.uniform.restSeconds,
        )
        state.copy(cycles = state.cycles + new)
    }

    fun duplicateCycle(key: Long) = _state.update { state ->
        val index = state.cycles.indexOfFirst { it.key == key }
        if (index < 0 || state.cycles.size >= Limits.cycles.last) return@update state
        val copy = state.cycles[index].copy(key = nextKey++)
        state.copy(cycles = state.cycles.toMutableList().apply { add(index + 1, copy) })
    }

    fun removeCycle(key: Long) = _state.update { state ->
        if (state.cycles.size <= Limits.cycles.first) state
        else state.copy(cycles = state.cycles.filterNot { it.key == key })
    }

    fun moveCycle(key: Long, offset: Int) = _state.update { state ->
        val from = state.cycles.indexOfFirst { it.key == key }
        val to = (from + offset).coerceIn(0, state.cycles.lastIndex)
        if (from < 0 || from == to) return@update state
        state.copy(cycles = state.cycles.toMutableList().apply { add(to, removeAt(from)) })
    }

    // endregion

    // region Save / start

    fun openSaveDialog() = _state.update { it.copy(showSaveDialog = true) }
    fun dismissSaveDialog() = _state.update { it.copy(showSaveDialog = false, nameError = null) }

    fun save() {
        val current = _state.value
        if (current.isSaving || current.isLoading) return
        if (current.name.isBlank()) {
            _state.value = current.copy(nameError = NameError.Empty)
            return
        }
        _state.value = current.copy(isSaving = true)
        viewModelScope.launch {
            val config = current.toConfig()
            val id = current.workoutId
            val result = try {
                if (id == null) {
                    workoutRepository.create(current.name, current.description, config)
                } else {
                    workoutRepository.update(id, current.name, current.description, config)
                }
            } catch (_: IOException) {
                null
            } finally {
                _state.update { it.copy(isSaving = false) }
            }
            when (result) {
                null -> _events.send(EditorEvent.StorageError)
                is SaveResult.Saved -> {
                    // Remember the id in every mode: a second "Save" (e.g. the Close was dropped
                    // because the app was in background) becomes an update, never a duplicate.
                    _state.update {
                        it.copy(workoutId = result.id, showSaveDialog = false, savedSnapshot = current.snapshot())
                    }
                    _events.send(if (mode == EditorMode.Quick) EditorEvent.Saved else EditorEvent.Close)
                }

                // Inline error on the name field (also inside the quick-save dialog, which stays open).
                SaveResult.NameTaken -> _state.update { it.copy(nameError = NameError.Duplicate) }

                SaveResult.LimitReached -> {
                    _state.update { it.copy(showSaveDialog = false) }
                    _events.send(EditorEvent.LimitReached)
                }

                SaveResult.NotFound -> _events.send(EditorEvent.Close)
            }
        }
    }

    /** Starts the current configuration without saving it (quick workout). */
    private var startJob: Job? = null

    fun start() {
        val current = _state.value
        if (current.isLoading || startJob?.isActive == true) return
        val config = current.toConfig().sanitized()
        startJob = viewModelScope.launch {
            try {
                settingsRepository.update { it.copy(lastQuickConfig = config) }
            } catch (_: IOException) {
                // Remembering the quick config is a convenience: start anyway.
            }
            _events.send(
                EditorEvent.StartRun(
                    config = config,
                    title = current.name.trim().ifEmpty { null },
                    // Counts as a use only if this quick workout has been saved.
                    workoutId = current.workoutId,
                ),
            )
        }
    }

    // endregion

    // region Leaving

    /** Back/up: asks before discarding unsaved changes of a saved workout. */
    fun requestBack() {
        if (_state.value.hasUnsavedChanges) {
            _state.update { it.copy(showDiscardConfirm = true) }
        } else {
            _events.trySend(EditorEvent.Close)
        }
    }

    fun confirmDiscard() {
        _state.update { it.copy(showDiscardConfirm = false) }
        _events.trySend(EditorEvent.Close)
    }

    fun dismissDiscard() = _state.update { it.copy(showDiscardConfirm = false) }

    // endregion
}
