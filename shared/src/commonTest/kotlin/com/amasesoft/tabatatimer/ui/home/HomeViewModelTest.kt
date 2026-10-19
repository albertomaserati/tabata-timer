package com.amasesoft.tabatatimer.ui.home

import com.amasesoft.tabatatimer.data.FakeSettingsRepository
import com.amasesoft.tabatatimer.data.FakeWorkoutRepository
import com.amasesoft.tabatatimer.data.FailingWorkoutRepository
import com.amasesoft.tabatatimer.data.WorkoutStore
import com.amasesoft.tabatatimer.data.create
import com.amasesoft.tabatatimer.domain.CyclePlan
import com.amasesoft.tabatatimer.domain.WorkoutConfig
import com.amasesoft.tabatatimer.domain.WorkoutSort
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    // Shared by Main (viewModelScope) and runTest, so collectors start eagerly.
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val store = listOf("beta" to 3, "Alpha" to 1, "gamma" to 2).fold(WorkoutStore()) { s, (name, cycles) ->
        s.create(name, "", WorkoutConfig(prepareSeconds = 0, plan = CyclePlan.Uniform(10, 0, cycles)), 0).first
    }

    @Test
    fun sortIsPersistedAndApplied() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = HomeViewModel(FakeWorkoutRepository(store), settings)
        backgroundScope.launch { vm.state.collect {} }

        assertEquals(listOf("beta", "Alpha", "gamma"), vm.state.value.workouts.map { it.name })
        vm.setSort(WorkoutSort.Name)
        assertEquals(WorkoutSort.Name, settings.state.value.workoutSort)
        assertEquals(listOf("Alpha", "beta", "gamma"), vm.state.value.workouts.map { it.name })
        vm.setSort(WorkoutSort.Longest)
        assertEquals(listOf("beta", "gamma", "Alpha"), vm.state.value.workouts.map { it.name })
    }

    @Test
    fun reorderingForcesCustomSort() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = HomeViewModel(FakeWorkoutRepository(store), settings)
        backgroundScope.launch { vm.state.collect {} }
        vm.setSort(WorkoutSort.Name)

        vm.setReordering(true)
        assertEquals(WorkoutSort.Custom, vm.state.value.sort)
        assertTrue(vm.state.value.isReordering)
        vm.move(2, -1)
        assertEquals(listOf("Alpha", "beta", "gamma"), vm.state.value.workouts.map { it.name })
        // Choosing another sort leaves reorder mode.
        vm.setSort(WorkoutSort.Newest)
        assertFalse(vm.state.value.isReordering)
    }

    @Test
    fun moveToEdgeSwitchesToCustomOrder() = runTest(dispatcher) {
        val settings = FakeSettingsRepository()
        val vm = HomeViewModel(FakeWorkoutRepository(store), settings)
        backgroundScope.launch { vm.state.collect {} }
        vm.setSort(WorkoutSort.Name)
        vm.moveToEdge(3, toTop = true)
        assertEquals(WorkoutSort.Custom, vm.state.value.sort)
        assertEquals(listOf("gamma", "beta", "Alpha"), vm.state.value.workouts.map { it.name })
    }

    @Test
    fun writeFailureIsReported() = runTest(dispatcher) {
        val vm = HomeViewModel(FailingWorkoutRepository(), FakeSettingsRepository())
        val events = mutableListOf<HomeEvent>()
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.delete(1)
        assertEquals(listOf<HomeEvent>(HomeEvent.StorageError), events)
    }

    @Test
    fun readErrorIsAnErrorStateNotAnEmptyList() = runTest(dispatcher) {
        val vm = HomeViewModel(FailingWorkoutRepository(), FakeSettingsRepository())
        backgroundScope.launch { vm.state.collect {} }
        val state = vm.state.value
        assertTrue(state.loadError)
        assertFalse(state.isLoading)
        assertTrue(state.workouts.isEmpty())
        // Creating (FAB), sorting and reordering need a known list.
        assertFalse(state.isListAvailable)
        vm.setReordering(true)
        assertFalse(vm.state.value.isReordering)
    }

    @Test
    fun retryRereadsAndShowsTheList() = runTest(dispatcher) {
        val repo = FakeWorkoutRepository(store).apply { failReads = true }
        val vm = HomeViewModel(repo, FakeSettingsRepository())
        backgroundScope.launch { vm.state.collect {} }
        assertTrue(vm.state.value.loadError)

        vm.reload() // still failing
        assertTrue(vm.state.value.loadError)

        repo.failReads = false
        vm.reload()
        assertFalse(vm.state.value.loadError)
        assertTrue(vm.state.value.isListAvailable)
        assertEquals(listOf("beta", "Alpha", "gamma"), vm.state.value.workouts.map { it.name })
    }

    @Test
    fun startsInLoadingStateWithoutEmptyList() = runTest(dispatcher) {
        val vm = HomeViewModel(FakeWorkoutRepository(store), FakeSettingsRepository())
        // Before the first read completes nothing claims "no workouts" and nothing can be created.
        assertTrue(vm.state.value.isLoading)
        assertFalse(vm.state.value.isListAvailable)
    }
}
