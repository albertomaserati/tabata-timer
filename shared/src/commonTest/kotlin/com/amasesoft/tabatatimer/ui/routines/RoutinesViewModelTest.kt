package com.amasesoft.tabatatimer.ui.routines

import com.amasesoft.tabatatimer.data.FakeGymRepository
import com.amasesoft.tabatatimer.data.GymStore
import com.amasesoft.tabatatimer.data.addExercise
import com.amasesoft.tabatatimer.data.createRoutine
import com.amasesoft.tabatatimer.data.exercise
import com.amasesoft.tabatatimer.domain.Exercise
import com.amasesoft.tabatatimer.domain.GymNameError
import com.amasesoft.tabatatimer.domain.MAX_ROUTINES
import com.amasesoft.tabatatimer.domain.Progression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesViewModelTest {

    // Shared by Main (viewModelScope) and runTest, so collectors start eagerly.
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** Routines "A" (id 1, exercises 3 and 4) and "B" (id 2, exercise 5); B selected. */
    private val store = GymStore()
        .createRoutine("A").first
        .createRoutine("B").first
        .addExercise(1, Exercise(0, "Panca", weightHalfKg = 28, progression = Progression.Increase, incrementHalfKg = 4)).first
        .addExercise(1, Exercise(0, "Trazioni", weightHalfKg = null, progression = Progression.Increase)).first
        .addExercise(2, Exercise(0, "Squat", weightHalfKg = 80)).first

    @Test
    fun restoresTheLastSelectedRoutine() = runTest(dispatcher) {
        val vm = RoutinesViewModel(FakeGymRepository(store))
        backgroundScope.launch { vm.state.collect {} }
        assertEquals(2L, vm.state.value.selectedId)
        assertEquals(listOf("Squat"), vm.state.value.exercises.map { it.name })
    }

    @Test
    fun selectionIsShownAndPersisted() = runTest(dispatcher) {
        val repo = FakeGymRepository(store)
        val vm = RoutinesViewModel(repo)
        backgroundScope.launch { vm.state.collect {} }
        vm.select(1)
        assertEquals(1L, vm.state.value.selectedId)
        assertEquals(listOf("Panca", "Trazioni"), vm.state.value.exercises.map { it.name })
        assertEquals(1L, repo.state.value.selectedRoutineId)

        // A new ViewModel (next launch) restores it.
        val next = RoutinesViewModel(repo)
        backgroundScope.launch { next.state.collect {} }
        assertEquals(1L, next.state.value.selectedId)
    }

    @Test
    fun nextWeightOnlyForIncreaseWithAWeight() = runTest(dispatcher) {
        val vm = RoutinesViewModel(FakeGymRepository(store.copy(selectedRoutineId = 1)))
        backgroundScope.launch { vm.state.collect {} }
        val (panca, trazioni) = vm.state.value.exercises
        assertEquals(32, panca.nextWeightHalfKg)
        assertNull(trazioni.nextWeightHalfKg) // bodyweight: badge only
    }

    @Test
    fun applyThenUndo() = runTest(dispatcher) {
        val repo = FakeGymRepository(store.copy(selectedRoutineId = 1))
        val vm = RoutinesViewModel(repo)
        val events = mutableListOf<RoutinesEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.events.collect { events += it } }

        vm.applyIncrease(3)
        val panca = vm.state.value.exercises.first()
        assertEquals(32, panca.weightHalfKg)
        assertEquals(Progression.Repeat, panca.progression)
        assertNull(panca.nextWeightHalfKg)
        val applied = assertIs<RoutinesEvent.IncreaseApplied>(events.single()).applied
        assertEquals(32, applied.newWeightHalfKg)

        // A double tap applies only once.
        vm.applyIncrease(3)
        assertEquals(32, repo.state.value.exercise(3)!!.weightHalfKg)
        assertEquals(1, events.size)

        vm.undoIncrease(applied)
        val restored = vm.state.value.exercises.first()
        assertEquals(28, restored.weightHalfKg)
        assertEquals(Progression.Increase, restored.progression)
    }

    @Test
    fun undoAfterAChangeDoesNothing() = runTest(dispatcher) {
        val vm = RoutinesViewModel(FakeGymRepository(store.copy(selectedRoutineId = 1)))
        val events = mutableListOf<RoutinesEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.applyIncrease(3)
        val applied = assertIs<RoutinesEvent.IncreaseApplied>(events.single()).applied
        vm.toggleProgression(3) // back to Increase, at the new weight
        vm.undoIncrease(applied)
        val panca = vm.state.value.exercises.first()
        assertEquals(32, panca.weightHalfKg)
        assertEquals(Progression.Increase, panca.progression)
    }

    @Test
    fun toggleMoveAndDelete() = runTest(dispatcher) {
        val vm = RoutinesViewModel(FakeGymRepository(store.copy(selectedRoutineId = 1)))
        backgroundScope.launch { vm.state.collect {} }
        vm.toggleProgression(3)
        assertEquals(Progression.Repeat, vm.state.value.exercises.first().progression)
        vm.moveExercise(4, -1)
        assertEquals(listOf("Trazioni", "Panca"), vm.state.value.exercises.map { it.name })
        vm.deleteExercise(4)
        assertEquals(listOf("Panca"), vm.state.value.exercises.map { it.name })
    }

    @Test
    fun createRoutineFromTheEmptyState() = runTest(dispatcher) {
        val vm = RoutinesViewModel(FakeGymRepository())
        backgroundScope.launch { vm.state.collect {} }
        assertTrue(vm.state.value.isListAvailable)
        assertTrue(vm.state.value.routines.isEmpty())

        vm.openNewRoutine()
        vm.submitDialog()
        assertEquals(GymNameError.Empty, vm.state.value.nameDialog?.error)
        vm.setDialogName("x".repeat(41))
        vm.submitDialog()
        assertEquals(GymNameError.TooLong, vm.state.value.nameDialog?.error)
        vm.setDialogName("Giorno A")
        assertNull(vm.state.value.nameDialog?.error)
        vm.submitDialog()
        assertNull(vm.state.value.nameDialog)
        assertEquals("Giorno A", vm.state.value.selected?.name)

        vm.openNewRoutine()
        vm.setDialogName(" giorno a ")
        vm.submitDialog()
        assertEquals(GymNameError.Duplicate, vm.state.value.nameDialog?.error)
    }

    @Test
    fun routineLimitIsExplained() = runTest(dispatcher) {
        val full = (1..MAX_ROUTINES).fold(GymStore()) { s, i -> s.createRoutine("R$i").first }
        val vm = RoutinesViewModel(FakeGymRepository(full))
        val events = mutableListOf<RoutinesEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.events.collect { events += it } }
        assertFalse(vm.state.value.canAddRoutine)
        vm.openNewRoutine()
        assertNull(vm.state.value.nameDialog)
        assertEquals(listOf<RoutinesEvent>(RoutinesEvent.RoutineLimitReached), events)
    }

    @Test
    fun readErrorThenRetry() = runTest(dispatcher) {
        val repo = FakeGymRepository(store).apply { failReads = true }
        val vm = RoutinesViewModel(repo)
        backgroundScope.launch { vm.state.collect {} }
        assertTrue(vm.state.value.loadError)
        assertFalse(vm.state.value.isListAvailable)
        assertTrue(vm.state.value.routines.isEmpty())

        repo.failReads = false
        vm.reload()
        assertFalse(vm.state.value.loadError)
        assertEquals(2, vm.state.value.routines.size)
    }

    @Test
    fun writeErrorIsReportedAndSelectionReverts() = runTest(dispatcher) {
        val repo = FakeGymRepository(store).apply { failWrites = true }
        val vm = RoutinesViewModel(repo)
        val events = mutableListOf<RoutinesEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.applyIncrease(3)
        vm.select(1)
        assertEquals(listOf<RoutinesEvent>(RoutinesEvent.StorageError, RoutinesEvent.StorageError), events)
        assertEquals(2L, vm.state.value.selectedId)
    }
}
