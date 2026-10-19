package com.amasesoft.tabatatimer.ui.editor

import com.amasesoft.tabatatimer.data.FakeSettingsRepository
import com.amasesoft.tabatatimer.data.FakeWorkoutRepository
import com.amasesoft.tabatatimer.domain.CyclePlan
import kotlinx.coroutines.Dispatchers
import com.amasesoft.tabatatimer.data.FailingWorkoutRepository
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
class EditorViewModelTest {

    // Shared by Main (viewModelScope) and runTest, so collectors start eagerly.
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel() = EditorViewModel(EditorMode.New, null, FakeWorkoutRepository(), FakeSettingsRepository())

    @Test
    fun switchingToAdvancedExpandsSimpleValues() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.setUniformWork(30)
        vm.setUniformCount(3)
        vm.setAdvanced(true)
        val state = vm.state.value
        assertTrue(state.isAdvanced)
        assertEquals(listOf(30, 30, 30), state.cycles.map { it.workSeconds })
        assertEquals(3, state.cycles.map { it.key }.toSet().size)
    }

    @Test
    fun backToSimpleIsSilentWhenUniform() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.setAdvanced(true)
        val last = vm.state.value.cycles.last()
        vm.updateCycle(last.key) { it.copy(restSeconds = 0) } // last rest is irrelevant
        vm.setAdvanced(false)
        assertFalse(vm.state.value.isAdvanced)
        assertFalse(vm.state.value.showSimplifyConfirm)
    }

    @Test
    fun backToSimpleAsksConfirmationWhenCustomized() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.setAdvanced(true)
        val first = vm.state.value.cycles.first()
        vm.updateCycle(first.key) { it.copy(name = "Squat", workSeconds = 45) }
        vm.setAdvanced(false)
        assertTrue(vm.state.value.isAdvanced)
        assertTrue(vm.state.value.showSimplifyConfirm)
        vm.confirmSimplify()
        val state = vm.state.value
        assertFalse(state.isAdvanced)
        assertEquals(CyclePlan.Uniform(45, 10, 8), state.uniform)
    }

    @Test
    fun saveRequiresName() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.save()
        assertEquals(NameError.Empty, vm.state.value.nameError)
        vm.setName("Legs")
        assertNull(vm.state.value.nameError)
    }

    @Test
    fun quickSaveThenStartCarriesWorkoutId() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository()
        val settings = FakeSettingsRepository()
        val vm = EditorViewModel(EditorMode.Quick, null, repo, settings)
        val events = mutableListOf<EditorEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }

        vm.setUniformWork(45)
        vm.openSaveDialog()
        vm.setName("Legs")
        vm.save()
        assertEquals(EditorEvent.Saved, events.last())
        assertEquals(1L, vm.state.value.workoutId)
        assertFalse(vm.state.value.showSaveDialog)
        assertEquals("Legs", repo.store.value.workouts.single().name)

        vm.start()
        val start = events.last() as EditorEvent.StartRun
        assertEquals(1L, start.workoutId)
        assertEquals("Legs", start.title)
        assertEquals(45, (start.config.plan as CyclePlan.Uniform).workSeconds)
        assertEquals(start.config, settings.state.value.lastQuickConfig)
    }

    @Test
    fun saveFailureReportsErrorAndClearsSaving() = runTest(dispatcher) {
        val vm = EditorViewModel(EditorMode.New, null, FailingWorkoutRepository(), FakeSettingsRepository())
        val events = mutableListOf<EditorEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.setName("X")
        vm.save()
        assertEquals(listOf<EditorEvent>(EditorEvent.StorageError), events)
        assertFalse(vm.state.value.isSaving)
    }

    @Test
    fun leavingWithUnsavedChangesAsksConfirmation() = runTest(dispatcher) {
        val vm = newViewModel()
        val events = mutableListOf<EditorEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }

        assertFalse(vm.state.value.hasUnsavedChanges)
        vm.setName("Changed")
        assertTrue(vm.state.value.hasUnsavedChanges)
        vm.requestBack()
        assertTrue(vm.state.value.showDiscardConfirm)
        assertTrue(events.isEmpty())
        vm.confirmDiscard()
        assertEquals(listOf<EditorEvent>(EditorEvent.Close), events)
    }

    @Test
    fun quickModeNeverAsksOnLeave() = runTest(dispatcher) {
        val vm = EditorViewModel(EditorMode.Quick, null, FakeWorkoutRepository(), FakeSettingsRepository())
        vm.setUniformWork(50)
        assertFalse(vm.state.value.hasUnsavedChanges)
    }

    @Test
    fun duplicateNameShowsInlineErrorAndDoesNotSave() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository()
        repo.create("Legs", "", com.amasesoft.tabatatimer.domain.WorkoutConfig.Default)
        val vm = EditorViewModel(EditorMode.New, null, repo, FakeSettingsRepository())
        val events = mutableListOf<EditorEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }

        vm.setName("  lEGS ")
        vm.save()
        assertEquals(NameError.Duplicate, vm.state.value.nameError)
        assertFalse(vm.state.value.isSaving)
        assertTrue(events.isEmpty())
        assertEquals(1, repo.store.value.workouts.size)
        // Editing the name clears the error.
        vm.setName("Legs 2")
        assertNull(vm.state.value.nameError)
    }

    @Test
    fun quickSaveDialogStaysOpenOnDuplicateName() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository()
        repo.create("Legs", "", com.amasesoft.tabatatimer.domain.WorkoutConfig.Default)
        val vm = EditorViewModel(EditorMode.Quick, null, repo, FakeSettingsRepository())
        vm.openSaveDialog()
        vm.setName("legs")
        vm.save()
        assertTrue(vm.state.value.showSaveDialog)
        assertEquals(NameError.Duplicate, vm.state.value.nameError)
        assertNull(vm.state.value.workoutId)
    }

    @Test
    fun secondSaveInNewModeUpdatesInsteadOfDuplicating() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository()
        val vm = EditorViewModel(EditorMode.New, null, repo, FakeSettingsRepository())
        val events = mutableListOf<EditorEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }

        vm.setName("Legs")
        vm.save()
        assertEquals(1L, vm.state.value.workoutId)
        assertFalse(vm.state.value.hasUnsavedChanges)
        // The Close was dropped (e.g. app in background): the user taps Save again.
        vm.setDescription("updated")
        vm.save()
        assertEquals(1, repo.store.value.workouts.size)
        assertEquals("updated", repo.store.value.workouts.single().description)
        assertNull(vm.state.value.nameError)
        assertEquals(listOf<EditorEvent>(EditorEvent.Close, EditorEvent.Close), events)
    }
}
