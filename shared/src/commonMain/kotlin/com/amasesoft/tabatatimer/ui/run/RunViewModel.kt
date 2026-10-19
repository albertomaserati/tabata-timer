package com.amasesoft.tabatatimer.ui.run

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amasesoft.tabatatimer.data.WorkoutRepository
import com.amasesoft.tabatatimer.domain.ElapsedClock
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import com.amasesoft.tabatatimer.domain.WorkoutPlanner
import com.amasesoft.tabatatimer.domain.WorkoutTimeline
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * MVI store for a running workout. The timer is a state machine (running/paused/finished,
 * step transitions, skip, restart), so all transitions go through the pure [RunReducer];
 * this class only owns the clock, the ticking coroutine and the effect channel.
 *
 * Elapsed time, the finished flag and whether the use was already counted are kept in
 * [savedStateHandle]: after process death the run is restored *paused* at the same point,
 * without replaying the start cue and without counting the use twice.
 *
 * @param workoutId saved workout to credit with one use, or null for a quick workout.
 */
class RunViewModel(
    config: WorkoutConfig,
    title: String?,
    workoutId: Long?,
    workoutRepository: WorkoutRepository,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    timeSource: TimeSource = TimeSource.Monotonic,
    private val tickInterval: Duration = 50.milliseconds,
) : ViewModel() {

    private val timeline = WorkoutTimeline(WorkoutPlanner.steps(config.sanitized()))
    private val reducer = RunReducer(timeline)
    private val clock = ElapsedClock(timeSource)

    private val _state: MutableStateFlow<RunUiState>
    val state: StateFlow<RunUiState>

    private val _effects = Channel<RunEffect>(Channel.BUFFERED)
    val effects: Flow<RunEffect> = _effects.receiveAsFlow()

    private var ticker: Job? = null
    private var closed = false

    init {
        val restoredMillis = savedStateHandle.get<Long>(KEY_ELAPSED_MS)
        _state = MutableStateFlow(
            if (restoredMillis == null) {
                reducer.initialState(title)
            } else {
                clock.seek(restoredMillis.milliseconds)
                reducer.restoredState(title, restoredMillis.milliseconds, savedStateHandle.get<Boolean>(KEY_FINISHED) == true)
            },
        )
        state = _state.asStateFlow()

        if (workoutId != null && savedStateHandle.get<Boolean>(KEY_USAGE_COUNTED) != true) {
            savedStateHandle[KEY_USAGE_COUNTED] = true
            viewModelScope.launch {
                try {
                    workoutRepository.incrementUsage(workoutId)
                } catch (_: IOException) {
                    // A missed usage count is not worth interrupting the workout.
                }
            }
        }
        if (restoredMillis == null && _state.value.status == RunStatus.Running) {
            _state.value.currentStep?.let { _effects.trySend(RunEffect.PhaseStartCue(it.type)) }
            startTicking()
        }
        persist()
    }

    fun onIntent(intent: RunIntent) {
        if (closed) return
        when (intent) {
            RunIntent.TogglePause -> when (_state.value.status) {
                RunStatus.Running -> pause()
                RunStatus.Paused -> {
                    dispatch(RunAction.Resume)
                    startTicking()
                }

                RunStatus.Finished -> Unit
            }

            RunIntent.Pause -> if (_state.value.status == RunStatus.Running) pause()

            RunIntent.Skip -> {
                if (_state.value.status == RunStatus.Finished) return
                val target = timeline.startOf(_state.value.position.stepIndex + 1)
                clock.seek(target)
                dispatch(RunAction.Seek(target))
            }

            RunIntent.Restart -> {
                clock.pause()
                clock.seek(Duration.ZERO)
                dispatch(RunAction.Restart)
                startTicking()
            }

            RunIntent.Stop -> {
                // Idempotent: a double tap must not close (pop) twice.
                closed = true
                ticker?.cancel()
                clock.pause()
                _effects.trySend(RunEffect.Close)
            }
        }
    }

    private fun pause() {
        clock.pause()
        ticker?.cancel()
        dispatch(RunAction.Pause(clock.elapsed()))
    }

    private fun startTicking() {
        ticker?.cancel()
        clock.start()
        ticker = viewModelScope.launch {
            while (isActive && _state.value.status == RunStatus.Running) {
                dispatch(RunAction.Tick(clock.elapsed()))
                delay(tickInterval)
            }
        }
    }

    private fun dispatch(action: RunAction) {
        val (newState, effects) = reducer.reduce(_state.value, action)
        _state.value = newState
        effects.forEach { _effects.trySend(it) }
        if (newState.status == RunStatus.Finished) clock.pause()
        persist()
    }

    private fun persist() {
        savedStateHandle[KEY_ELAPSED_MS] = clock.elapsed().inWholeMilliseconds
        savedStateHandle[KEY_FINISHED] = _state.value.status == RunStatus.Finished
    }

    private companion object {
        const val KEY_ELAPSED_MS = "run.elapsedMs"
        const val KEY_FINISHED = "run.finished"
        const val KEY_USAGE_COUNTED = "run.usageCounted"
    }
}
