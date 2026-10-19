package com.amasesoft.tabatatimer.ui.run

import com.amasesoft.tabatatimer.domain.CyclePlan
import com.amasesoft.tabatatimer.domain.PhaseType
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import com.amasesoft.tabatatimer.domain.WorkoutPlanner
import com.amasesoft.tabatatimer.domain.WorkoutTimeline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class RunReducerTest {

    private val timeline = WorkoutTimeline(
        WorkoutPlanner.steps(WorkoutConfig(prepareSeconds = 5, sets = 1, plan = CyclePlan.Uniform(10, 5, 2))),
    ) // Prepare 5, Work 10, Rest 5, Work 10 = 30s
    private val reducer = RunReducer(timeline)

    /** Feeds ticks every 100ms up to [until] and collects all effects. */
    private fun run(until: Duration): Pair<RunUiState, List<RunEffect>> {
        var state = reducer.initialState("Test")
        val effects = mutableListOf<RunEffect>()
        var t = Duration.ZERO
        while (t <= until) {
            val (s, e) = reducer.reduce(state, RunAction.Tick(t))
            state = s
            effects += e
            t += 100.milliseconds
        }
        return state to effects
    }

    @Test
    fun emitsCountdownForLastThreeSecondsOfEachStepAndPhaseStarts() {
        val (state, effects) = run(until = 31.seconds)
        assertEquals(RunStatus.Finished, state.status)
        assertEquals(
            listOf(
                RunEffect.CountdownCue, RunEffect.CountdownCue, RunEffect.CountdownCue,
                RunEffect.PhaseStartCue(PhaseType.Work),
                RunEffect.CountdownCue, RunEffect.CountdownCue, RunEffect.CountdownCue,
                RunEffect.PhaseStartCue(PhaseType.Rest),
                RunEffect.CountdownCue, RunEffect.CountdownCue, RunEffect.CountdownCue,
                RunEffect.PhaseStartCue(PhaseType.Work),
                RunEffect.CountdownCue, RunEffect.CountdownCue, RunEffect.CountdownCue,
                RunEffect.FinishedCue,
            ),
            effects,
        )
    }

    @Test
    fun pauseFreezesStateAndIgnoresTicks() {
        var (state, _) = reducer.reduce(reducer.initialState(null), RunAction.Tick(2.seconds))
        state = reducer.reduce(state, RunAction.Pause(2.5.seconds)).first
        assertEquals(RunStatus.Paused, state.status)
        val afterTick = reducer.reduce(state, RunAction.Tick(20.seconds)).first
        assertEquals(state, afterTick)
        assertEquals(3, afterTick.position.displaySecondsInStep)
        assertEquals(RunStatus.Running, reducer.reduce(state, RunAction.Resume).first.status)
    }

    @Test
    fun seekToNextStepEmitsPhaseStart() {
        val (state, effects) = reducer.reduce(reducer.initialState(null), RunAction.Seek(timeline.startOf(1)))
        assertEquals(1, state.position.stepIndex)
        assertEquals(listOf<RunEffect>(RunEffect.PhaseStartCue(PhaseType.Work)), effects)
    }

    @Test
    fun derivedValues() {
        val state = reducer.reduce(reducer.initialState(null), RunAction.Tick(10.seconds)).first
        assertEquals(PhaseType.Work, state.currentStep?.type)
        assertEquals(PhaseType.Rest, state.nextStep?.type)
        assertEquals(0.5f, state.stepProgress)
        assertEquals(1, state.totalSets)
        assertEquals(2, state.totalCycles)
        assertTrue(state.totalProgress in 0.33f..0.34f)
    }

    @Test
    fun skipOnLastStepWhileRunningEmitsFinishedCue() {
        val running = reducer.reduce(reducer.initialState(null), RunAction.Seek(timeline.startOf(3))).first
        val (state, effects) = reducer.reduce(running, RunAction.Seek(timeline.startOf(4)))
        assertEquals(RunStatus.Finished, state.status)
        assertEquals(listOf<RunEffect>(RunEffect.FinishedCue), effects)
    }

    @Test
    fun skipWhilePausedIsSilent() {
        val paused = reducer.reduce(reducer.initialState(null), RunAction.Pause(1.seconds)).first
        val (state, effects) = reducer.reduce(paused, RunAction.Seek(timeline.startOf(1)))
        assertEquals(RunStatus.Paused, state.status)
        assertEquals(1, state.position.stepIndex)
        assertTrue(effects.isEmpty())
        // Skipping past the end while paused finishes silently too.
        val (finished, finishEffects) = reducer.reduce(state, RunAction.Seek(timeline.totalDuration))
        assertEquals(RunStatus.Finished, finished.status)
        assertTrue(finishEffects.isEmpty())
    }

    @Test
    fun restartFromFinished() {
        val finished = run(until = 31.seconds).first
        val (state, effects) = reducer.reduce(finished, RunAction.Restart)
        assertEquals(RunStatus.Running, state.status)
        assertEquals(0, state.position.stepIndex)
        assertEquals(listOf<RunEffect>(RunEffect.PhaseStartCue(PhaseType.Prepare)), effects)
    }

    @Test
    fun restoredStateIsPausedOrFinished() {
        val restored = reducer.restoredState("T", 12.seconds, finished = false)
        assertEquals(RunStatus.Paused, restored.status)
        assertEquals(1, restored.position.stepIndex)
        assertEquals(RunStatus.Finished, reducer.restoredState("T", 30.seconds, finished = false).status)
        assertEquals(RunStatus.Finished, reducer.restoredState("T", 1.seconds, finished = true).status)
    }
}
