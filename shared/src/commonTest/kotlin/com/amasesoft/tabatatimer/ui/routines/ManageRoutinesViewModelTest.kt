package com.amasesoft.tabatatimer.ui.routines

import com.amasesoft.tabatatimer.data.FakeGymRepository
import com.amasesoft.tabatatimer.data.GymStore
import com.amasesoft.tabatatimer.data.createRoutine
import com.amasesoft.tabatatimer.domain.GymNameError
import com.amasesoft.tabatatimer.domain.MAX_ROUTINES
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ManageRoutinesViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val store = GymStore().createRoutine("A").first.createRoutine("B").first

    @Test
    fun renameDuplicateMoveDelete() = runTest(dispatcher) {
        val vm = ManageRoutinesViewModel(FakeGymRepository(store))
        backgroundScope.launch { vm.state.collect {} }

        vm.openRename(1)
        assertEquals("A", vm.state.value.nameDialog?.text)
        vm.setDialogName("b")
        vm.submitDialog()
        assertEquals(GymNameError.Duplicate, vm.state.value.nameDialog?.error)
        vm.setDialogName("Giorno A")
        vm.submitDialog()
        assertNull(vm.state.value.nameDialog)

        vm.duplicate(1, "copia")
        assertEquals(listOf("Giorno A", "Giorno A (copia)", "B"), vm.state.value.routines.map { it.name })
        vm.move(2, -1)
        assertEquals(listOf("Giorno A", "B", "Giorno A (copia)"), vm.state.value.routines.map { it.name })
        vm.delete(1)
        assertEquals(listOf("B", "Giorno A (copia)"), vm.state.value.routines.map { it.name })
    }

    @Test
    fun duplicateAtTheLimitEmitsTheEvent() = runTest(dispatcher) {
        val full = (1..MAX_ROUTINES).fold(GymStore()) { s, i -> s.createRoutine("R$i").first }
        val repo = FakeGymRepository(full)
        val vm = ManageRoutinesViewModel(repo)
        val events = mutableListOf<RoutinesEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.duplicate(1, "copia")
        assertEquals(listOf<RoutinesEvent>(RoutinesEvent.RoutineLimitReached), events)
        assertEquals(MAX_ROUTINES, repo.state.value.routines.size)
    }

    @Test
    fun writeErrorIsReported() = runTest(dispatcher) {
        val vm = ManageRoutinesViewModel(FakeGymRepository(store).apply { failWrites = true })
        val events = mutableListOf<RoutinesEvent>()
        backgroundScope.launch { vm.state.collect {} }
        backgroundScope.launch { vm.events.collect { events += it } }
        vm.delete(1)
        vm.openNewRoutine()
        vm.setDialogName("C")
        vm.submitDialog()
        assertEquals(listOf<RoutinesEvent>(RoutinesEvent.StorageError, RoutinesEvent.StorageError), events)
        // The dialog stays open with the typed name, so the user can retry.
        assertEquals("C", vm.state.value.nameDialog?.text)
    }
}
