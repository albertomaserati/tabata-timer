package com.amasesoft.tabatatimer.ui.run

import androidx.compose.runtime.Immutable
import com.amasesoft.tabatatimer.domain.PhaseType
import com.amasesoft.tabatatimer.domain.Step
import com.amasesoft.tabatatimer.domain.TimelinePosition
import com.amasesoft.tabatatimer.domain.WorkoutTimeline
import kotlin.time.Duration

// MVI contract of the run screen: one immutable state, user intents, internal actions,
// one-shot effects, and a pure reducer.

enum class RunStatus { Running, Paused, Finished }

@Immutable
data class RunUiState(
    val title: String?,
    val steps: List<Step>,
    val totalSets: Int,
    val totalCycles: Int,
    val totalDuration: Duration,
    val status: RunStatus,
    val position: TimelinePosition,
) {
    val currentStep: Step? get() = steps.getOrNull(position.stepIndex)
    val nextStep: Step? get() = if (position.isFinished) null else steps.getOrNull(position.stepIndex + 1)

    /** Progress of the current step, 0 = just started, 1 = done. */
    val stepProgress: Float
        get() {
            val total = position.elapsedInStep + position.remainingInStep
            return if (total == Duration.ZERO) 1f else (position.elapsedInStep / total).toFloat()
        }

    val totalProgress: Float
        get() = if (totalDuration == Duration.ZERO) 1f
        else (1f - (position.totalRemaining / totalDuration).toFloat()).coerceIn(0f, 1f)
}

/** What the user can do. */
sealed interface RunIntent {
    data object TogglePause : RunIntent

    /** Pause only if running (idempotent): used for the automatic pause on ON_STOP. */
    data object Pause : RunIntent
    data object Skip : RunIntent
    data object Stop : RunIntent
    data object Restart : RunIntent
}

/** Internal, time-stamped actions fed to the reducer (time comes from the clock, not the reducer). */
sealed interface RunAction {
    data class Tick(val elapsed: Duration) : RunAction
    data class Pause(val elapsed: Duration) : RunAction
    data object Resume : RunAction
    data class Seek(val elapsed: Duration) : RunAction
    data object Restart : RunAction
}

sealed interface RunEffect {
    data object CountdownCue : RunEffect
    data class PhaseStartCue(val type: PhaseType) : RunEffect
    data object FinishedCue : RunEffect
    data object Close : RunEffect
}

class RunReducer(private val timeline: WorkoutTimeline) {

    fun initialState(title: String?): RunUiState {
        val steps = timeline.steps
        return RunUiState(
            title = title,
            steps = steps,
            totalSets = steps.maxOfOrNull { it.setNumber } ?: 0,
            totalCycles = steps.maxOfOrNull { it.cycleNumber } ?: 0,
            totalDuration = timeline.totalDuration,
            status = if (steps.isEmpty()) RunStatus.Finished else RunStatus.Running,
            position = timeline.positionAt(Duration.ZERO),
        )
    }

    /** State after process death: never resumes by itself (paused, or finished). */
    fun restoredState(title: String?, elapsed: Duration, finished: Boolean): RunUiState {
        val initial = initialState(title)
        val position = timeline.positionAt(elapsed)
        val status = if (finished || position.isFinished || initial.steps.isEmpty()) RunStatus.Finished else RunStatus.Paused
        return initial.copy(status = status, position = position)
    }

    fun reduce(state: RunUiState, action: RunAction): Pair<RunUiState, List<RunEffect>> = when (action) {
        is RunAction.Tick ->
            if (state.status == RunStatus.Running) advance(state, action.elapsed) else state to emptyList()

        is RunAction.Pause ->
            if (state.status == RunStatus.Running) {
                state.copy(status = RunStatus.Paused, position = timeline.positionAt(action.elapsed)) to emptyList()
            } else {
                state to emptyList()
            }

        RunAction.Resume ->
            if (state.status == RunStatus.Paused) state.copy(status = RunStatus.Running) to emptyList()
            else state to emptyList()

        is RunAction.Seek -> when (state.status) {
            RunStatus.Finished -> state to emptyList()
            RunStatus.Running -> advance(state, action.elapsed)
            // Skipping while paused moves silently: no sound or haptic cue.
            RunStatus.Paused -> advance(state, action.elapsed).first to emptyList()
        }

        RunAction.Restart -> {
            val initial = initialState(state.title)
            initial to listOfNotNull(initial.currentStep?.let { RunEffect.PhaseStartCue(it.type) })
        }
    }

    private fun advance(state: RunUiState, elapsed: Duration): Pair<RunUiState, List<RunEffect>> {
        val old = state.position
        val new = timeline.positionAt(elapsed)
        return when {
            new.isFinished -> state.copy(status = RunStatus.Finished, position = new) to
                if (old.isFinished) emptyList() else listOf(RunEffect.FinishedCue)

            new.stepIndex != old.stepIndex -> state.copy(position = new) to
                listOf(RunEffect.PhaseStartCue(timeline.steps[new.stepIndex].type))

            new.displaySecondsInStep != old.displaySecondsInStep && new.displaySecondsInStep in 1..3 ->
                state.copy(position = new) to listOf(RunEffect.CountdownCue)

            else -> state.copy(position = new) to emptyList()
        }
    }
}
