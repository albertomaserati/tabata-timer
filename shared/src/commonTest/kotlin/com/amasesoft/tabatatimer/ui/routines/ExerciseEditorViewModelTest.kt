package com.amasesoft.tabatatimer.ui.routines

import com.amasesoft.tabatatimer.data.FakeGymRepository
import com.amasesoft.tabatatimer.data.GymStore
import com.amasesoft.tabatatimer.data.addExercise
import com.amasesoft.tabatatimer.data.createRoutine
import com.amasesoft.tabatatimer.data.exercise
import com.amasesoft.tabatatimer.domain.Exercise
import com.amasesoft.tabatatimer.domain.ExerciseLimits
import com.amasesoft.tabatatimer.domain.GymNameError
import com.amasesoft.tabatatimer.domain.MAX_EXERCISES_PER_ROUTINE
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseEditorViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    /** Routine 1 with exercise 2 (Panca 4x8, 14 kg). */
    private val store = GymStore().createRoutine("A").first
        .addExercise(1, Exercise(0, "Panca", sets = 4, reps = 8, weightHalfKg = 28)).first

    @Test
    fun newExerciseIsAddedToTheRoutine() = runTest(dispatcher) {
        val repo = FakeGymRepository(store)
        val vm = ExerciseEditorViewModel(routineId = 1, exerciseId = null, repository = repo)
        val events = mutableListOf<ExerciseEditorEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }

        vm.save()
        assertEquals(GymNameError.Empty, vm.state.value.nameError)
        vm.setName("x".repeat(ExerciseLimits.NAME_MAX_LENGTH + 1))
        vm.save()
        assertEquals(GymNameError.TooLong, vm.state.value.nameError)

        vm.setName("  Rematore ")
        vm.setSets(5)
        vm.setReps(12)
        vm.setWeight(25)
        vm.setProgression(Progression.Increase)
        vm.setIncrement(5)
        vm.save()
        assertEquals(listOf<ExerciseEditorEvent>(ExerciseEditorEvent.Close), events)
        val added = repo.state.value.routines.single().exercises.last()
        assertEquals(Exercise(3, "Rematore", "", 5, 12, 25, Progression.Increase, 5), added)
    }

    @Test
    fun weightCanBeRemovedAndIsRememberedWhileOff() = runTest(dispatcher) {
        val repo = FakeGymRepository(store)
        val vm = ExerciseEditorViewModel(1, 2, repo)
        assertTrue(vm.state.value.draft.useWeight)
        vm.setUseWeight(false)
        assertTrue(vm.state.value.hasUnsavedChanges)
        vm.setUseWeight(true)
        assertEquals(28, vm.state.value.draft.weightHalfKg)
        assertFalse(vm.state.value.hasUnsavedChanges)

        vm.setUseWeight(false)
        vm.save()
        assertNull(repo.state.value.exercise(2)!!.weightHalfKg)
    }

    @Test
    fun valuesAreClampedToTheRanges() = runTest(dispatcher) {
        val vm = ExerciseEditorViewModel(1, null, FakeGymRepository(store))
        vm.setSets(0)
        vm.setReps(1000)
        vm.setWeight(5000)
        vm.setIncrement(0)
        vm.setDescription("d".repeat(500))
        val draft = vm.state.value.draft
        assertEquals(ExerciseLimits.sets.first, draft.sets)
        assertEquals(ExerciseLimits.reps.last, draft.reps)
        assertEquals(ExerciseLimits.weightHalfKg.last, draft.weightHalfKg)
        assertEquals(ExerciseLimits.incrementHalfKg.first, draft.incrementHalfKg)
        assertEquals(ExerciseLimits.DESCRIPTION_MAX_LENGTH, draft.description.length)
    }

    @Test
    fun backAsksBeforeDiscardingChanges() = runTest(dispatcher) {
        val vm = ExerciseEditorViewModel(1, 2, FakeGymRepository(store))
        val events = mutableListOf<ExerciseEditorEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }
        assertEquals("Panca", vm.state.value.draft.name)
        vm.setReps(10)
        vm.requestBack()
        assertTrue(vm.state.value.showDiscardConfirm)
        assertTrue(events.isEmpty())
        vm.confirmDiscard()
        assertEquals(listOf<ExerciseEditorEvent>(ExerciseEditorEvent.Close), events)
    }

    @Test
    fun deleteAsksThenCloses() = runTest(dispatcher) {
        val repo = FakeGymRepository(store)
        val vm = ExerciseEditorViewModel(1, 2, repo)
        val events = mutableListOf<ExerciseEditorEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.requestDelete()
        assertTrue(vm.state.value.showDeleteConfirm)
        vm.confirmDelete()
        assertNull(repo.state.value.exercise(2))
        assertEquals(listOf<ExerciseEditorEvent>(ExerciseEditorEvent.Close), events)
    }

    @Test
    fun limitReachedKeepsTheEditorOpen() = runTest(dispatcher) {
        val full = (1..MAX_EXERCISES_PER_ROUTINE).fold(GymStore().createRoutine("A").first) { s, i ->
            s.addExercise(1, Exercise(0, "E$i")).first
        }
        val vm = ExerciseEditorViewModel(1, null, FakeGymRepository(full))
        val events = mutableListOf<ExerciseEditorEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.setName("Extra")
        vm.save()
        assertEquals(listOf<ExerciseEditorEvent>(ExerciseEditorEvent.LimitReached), events)
        assertTrue(vm.state.value.hasUnsavedChanges)
    }

    @Test
    fun unreadableExerciseIsNeverOverwritten() = runTest(dispatcher) {
        val repo = FakeGymRepository(store).apply { failReads = true }
        val events = mutableListOf<ExerciseEditorEvent>()
        val vm = ExerciseEditorViewModel(1, 2, repo)
        backgroundScope.launch { vm.events.collect { events += it } }
        assertEquals(listOf(ExerciseEditorEvent.StorageError, ExerciseEditorEvent.Close), events)
        assertTrue(vm.state.value.isLoading)
        vm.save() // ignored while not loaded
        assertEquals(28, store.exercise(2)!!.weightHalfKg)
    }

    @Test
    fun writeErrorIsReported() = runTest(dispatcher) {
        val repo = FakeGymRepository(store).apply { failWrites = true }
        val vm = ExerciseEditorViewModel(1, 2, repo)
        val events = mutableListOf<ExerciseEditorEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.setReps(12)
        vm.save()
        assertEquals(listOf<ExerciseEditorEvent>(ExerciseEditorEvent.StorageError), events)
        assertFalse(vm.state.value.isSaving)
    }
}
