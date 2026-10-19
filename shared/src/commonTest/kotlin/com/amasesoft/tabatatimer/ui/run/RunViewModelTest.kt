package com.amasesoft.tabatatimer.ui.run

import androidx.lifecycle.SavedStateHandle
import com.amasesoft.tabatatimer.data.FakeWorkoutRepository
import com.amasesoft.tabatatimer.data.WorkoutStore
import com.amasesoft.tabatatimer.data.create
import com.amasesoft.tabatatimer.domain.CyclePlan
import com.amasesoft.tabatatimer.domain.PhaseType
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RunViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val config = WorkoutConfig(prepareSeconds = 3, sets = 2, restBetweenSetsSeconds = 5, plan = CyclePlan.Uniform(4, 2, 2))
    // 3 + 2 x (4 + 2 + 4) + 5 = 28s

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.newViewModel(
        repo: FakeWorkoutRepository = FakeWorkoutRepository(),
        id: Long? = null,
        handle: SavedStateHandle = SavedStateHandle(),
    ) = RunViewModel(config, "T", id, repo, handle, timeSource = testScheduler.timeSource)

    private fun TestScope.collectEffects(vm: RunViewModel): MutableList<RunEffect> {
        val effects = mutableListOf<RunEffect>()
        backgroundScope.launch { vm.effects.collect { effects += it } }
        return effects
    }

    private fun savedWorkoutRepo() = FakeWorkoutRepository(WorkoutStore().create("W", "", config, 0).first)

    @Test
    fun finishesExactlyAtTotalDurationWithoutDrift() = runTest(dispatcher) {
        val vm = newViewModel()
        advanceTimeBy(27_900)
        runCurrent()
        assertEquals(RunStatus.Running, vm.state.value.status)
        assertEquals(1, vm.state.value.position.displayTotalSeconds)
        advanceTimeBy(200)
        runCurrent()
        assertEquals(RunStatus.Finished, vm.state.value.status)
    }

    @Test
    fun pauseStopsTimeAndResumeContinues() = runTest(dispatcher) {
        val vm = newViewModel()
        advanceTimeBy(2_000)
        runCurrent()
        vm.onIntent(RunIntent.TogglePause)
        val paused = vm.state.value
        assertEquals(RunStatus.Paused, paused.status)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(paused, vm.state.value)
        vm.onIntent(RunIntent.TogglePause)
        advanceTimeBy(1_500)
        runCurrent()
        assertEquals(RunStatus.Running, vm.state.value.status)
        // 2s + 1.5s of the 28s workout elapsed.
        assertEquals(25, vm.state.value.position.displayTotalSeconds)
        vm.onIntent(RunIntent.Stop)
    }

    @Test
    fun autoPauseIsIdempotentAndNeverResumes() = runTest(dispatcher) {
        val vm = newViewModel()
        advanceTimeBy(1_000)
        runCurrent()
        vm.onIntent(RunIntent.Pause)
        val paused = vm.state.value
        assertEquals(RunStatus.Paused, paused.status)
        vm.onIntent(RunIntent.Pause)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(paused, vm.state.value)
    }

    @Test
    fun skipJumpsToNextStep() = runTest(dispatcher) {
        val vm = newViewModel()
        runCurrent()
        vm.onIntent(RunIntent.Skip)
        assertEquals(1, vm.state.value.position.stepIndex)
        assertEquals(25, vm.state.value.position.displayTotalSeconds)
        vm.onIntent(RunIntent.Stop)
    }

    @Test
    fun skipWhilePausedEmitsNoCue() = runTest(dispatcher) {
        val vm = newViewModel()
        val effects = collectEffects(vm)
        runCurrent()
        vm.onIntent(RunIntent.TogglePause)
        runCurrent()
        effects.clear()
        vm.onIntent(RunIntent.Skip)
        runCurrent()
        assertEquals(1, vm.state.value.position.stepIndex)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun restartFromFinishedRunsAgain() = runTest(dispatcher) {
        val vm = newViewModel()
        val effects = collectEffects(vm)
        advanceTimeBy(29_000)
        runCurrent()
        assertEquals(RunStatus.Finished, vm.state.value.status)
        effects.clear()
        vm.onIntent(RunIntent.Restart)
        runCurrent()
        assertEquals(RunStatus.Running, vm.state.value.status)
        assertEquals(listOf<RunEffect>(RunEffect.PhaseStartCue(PhaseType.Prepare)), effects)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(27, vm.state.value.position.displayTotalSeconds)
        vm.onIntent(RunIntent.Stop)
    }

    @Test
    fun stopIsIdempotent() = runTest(dispatcher) {
        val vm = newViewModel()
        val effects = collectEffects(vm)
        runCurrent()
        vm.onIntent(RunIntent.Stop)
        vm.onIntent(RunIntent.Stop)
        runCurrent()
        assertEquals(1, effects.count { it == RunEffect.Close })
    }

    @Test
    fun startingSavedWorkoutIncrementsUsage() = runTest(dispatcher) {
        val repo = savedWorkoutRepo()
        val vm = newViewModel(repo, id = 1)
        runCurrent()
        assertEquals(1, repo.store.value.workouts.single().usageCount)
        vm.onIntent(RunIntent.Stop)
    }

    @Test
    fun restoresFromSavedStatePausedWithoutRecountingOrCue() = runTest(dispatcher) {
        val repo = savedWorkoutRepo()
        val handle = SavedStateHandle()
        val first = newViewModel(repo, id = 1, handle = handle)
        advanceTimeBy(5_000)
        runCurrent()
        first.onIntent(RunIntent.Pause) // what ON_STOP does before the process is killed
        runCurrent()

        // "Process death": a new ViewModel with the same saved state.
        val restored = newViewModel(repo, id = 1, handle = handle)
        val effects = collectEffects(restored)
        advanceTimeBy(3_000)
        runCurrent()
        assertEquals(RunStatus.Paused, restored.state.value.status)
        assertEquals(23, restored.state.value.position.displayTotalSeconds)
        assertTrue(effects.isEmpty())
        assertEquals(1, repo.store.value.workouts.single().usageCount)

        restored.onIntent(RunIntent.TogglePause)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(22, restored.state.value.position.displayTotalSeconds)
        restored.onIntent(RunIntent.Stop)
    }
}
