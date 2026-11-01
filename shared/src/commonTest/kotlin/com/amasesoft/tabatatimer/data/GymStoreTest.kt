package com.amasesoft.tabatatimer.data

import com.amasesoft.tabatatimer.domain.Exercise
import com.amasesoft.tabatatimer.domain.ExerciseLimits
import com.amasesoft.tabatatimer.domain.MAX_EXERCISES_PER_ROUTINE
import com.amasesoft.tabatatimer.domain.MAX_ROUTINES
import com.amasesoft.tabatatimer.domain.Progression
import com.amasesoft.tabatatimer.domain.RoutineLimits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GymStoreTest {

    private fun GymStore.withRoutine(name: String): GymStore = createRoutine(name).first

    private fun GymStore.withExercise(routineId: Long, exercise: Exercise): GymStore = addExercise(routineId, exercise).first

    private fun exercise(name: String = "Panca", weight: Int? = 28, progression: Progression = Progression.Repeat, inc: Int = 4) =
        Exercise(id = 0, name = name, sets = 4, reps = 8, weightHalfKg = weight, progression = progression, incrementHalfKg = inc)

    // region Routines

    @Test
    fun createAssignsIdsTrimsAndSelects() {
        val (store, result) = GymStore().createRoutine("  Giorno A ")
        assertEquals(GymResult.Saved(1), result)
        assertEquals("Giorno A", store.routines.single().name)
        assertEquals(1L, store.selectedRoutineId)
        assertEquals(2L, store.nextId)
    }

    @Test
    fun namesAreUniqueIgnoringCaseAndSpaces() {
        val store = GymStore().withRoutine("Giorno A")
        assertEquals(GymResult.NameTaken, store.createRoutine(" giorno a  ").second)
        val withB = store.withRoutine("Giorno B")
        assertEquals(GymResult.NameTaken, withB.renameRoutine(2, "GIORNO A").second)
        // Renaming to itself (another case) is allowed.
        val (renamed, result) = withB.renameRoutine(1, "giorno A")
        assertEquals(GymResult.Saved(1), result)
        assertEquals("giorno A", renamed.routines.first().name)
    }

    @Test
    fun routineLimit() {
        val full = (1..MAX_ROUTINES).fold(GymStore()) { s, i -> s.withRoutine("R$i") }
        assertEquals(MAX_ROUTINES, full.routines.size)
        val (same, result) = full.createRoutine("Extra")
        assertEquals(GymResult.LimitReached, result)
        assertSame(full, same)
        assertEquals(GymResult.LimitReached, full.duplicateRoutine(1, "copia").second)
    }

    @Test
    fun duplicateCopiesExercisesWithNewIdsAndUniqueName() {
        val base = GymStore().withRoutine("Giorno A").withRoutine("Giorno B").let { s ->
            s.withExercise(1, exercise("Panca")).withExercise(1, exercise("Squat"))
        }
        val (copy1, r1) = base.duplicateRoutine(1, "copia")
        val newId = (r1 as GymResult.Saved).id
        // Inserted right after the original, and selected.
        assertEquals(listOf("Giorno A", "Giorno A (copia)", "Giorno B"), copy1.routines.map { it.name })
        assertEquals(newId, copy1.selectedRoutineId)
        val original = copy1.routines[0].exercises
        val copied = copy1.routines[1].exercises
        assertEquals(original.map { it.name }, copied.map { it.name })
        assertTrue(copied.map { it.id }.none { it in original.map(Exercise::id) })
        assertEquals((copy1.routines.flatMap { r -> r.exercises.map { it.id } + r.id }).toSet().size,
            copy1.routines.sumOf { it.exercises.size + 1 })

        val (copy2, _) = copy1.duplicateRoutine(1, "copia")
        assertTrue(copy2.routines.any { it.name == "Giorno A (copia 2)" })
    }

    @Test
    fun duplicateNameStaysWithinTheLimit() {
        val long = "x".repeat(RoutineLimits.NAME_MAX_LENGTH)
        val store = GymStore().withRoutine(long)
        val (copy, _) = store.duplicateRoutine(1, "copy")
        val name = copy.routines[1].name
        assertTrue(name.length <= RoutineLimits.NAME_MAX_LENGTH, name)
        assertTrue(name.endsWith(" (copy)"))
    }

    @Test
    fun deleteFixesTheSelection() {
        val store = GymStore().withRoutine("A").withRoutine("B").withRoutine("C").selectRoutine(2)
        // Deleting the selected one selects its neighbour (same position).
        assertEquals(3L, store.deleteRoutine(2).selectedRoutine?.id)
        // Deleting another one keeps the selection.
        assertEquals(2L, store.deleteRoutine(1).selectedRoutineId)
        // Deleting the last selected one selects the previous.
        assertEquals(2L, store.selectRoutine(3).deleteRoutine(3).selectedRoutine?.id)
        assertNull(GymStore().withRoutine("A").deleteRoutine(1).selectedRoutine)
    }

    @Test
    fun moveRoutineClamps() {
        val store = GymStore().withRoutine("A").withRoutine("B").withRoutine("C")
        assertEquals(listOf("B", "A", "C"), store.moveRoutine(2, -1).routines.map { it.name })
        assertEquals(listOf("B", "C", "A"), store.moveRoutine(1, 10).routines.map { it.name })
        assertSame(store, store.moveRoutine(1, -1))
    }

    @Test
    fun staleSelectionFallsBackToFirst() {
        val store = GymStore().withRoutine("A").withRoutine("B").copy(selectedRoutineId = 99)
        assertEquals(1L, store.selectedRoutine?.id)
        assertSame(store, store.selectRoutine(42))
    }

    // endregion

    // region Exercises

    @Test
    fun addExerciseSanitizesAndRespectsTheLimit() {
        val store = GymStore().withRoutine("A")
        val (added, result) = store.addExercise(1, exercise().copy(sets = 99, reps = 0, weightHalfKg = 5000, name = " Panca "))
        assertEquals(GymResult.Saved(2), result)
        val ex = added.routines.single().exercises.single()
        assertEquals("Panca", ex.name)
        assertEquals(ExerciseLimits.sets.last, ex.sets)
        assertEquals(ExerciseLimits.reps.first, ex.reps)
        assertEquals(ExerciseLimits.weightHalfKg.last, ex.weightHalfKg)

        assertEquals(GymResult.NotFound, store.addExercise(42, exercise()).second)

        val full = (1..MAX_EXERCISES_PER_ROUTINE).fold(store) { s, i -> s.withExercise(1, exercise("E$i")) }
        assertEquals(GymResult.LimitReached, full.addExercise(1, exercise()).second)
    }

    @Test
    fun updateMoveDeleteExercise() {
        val store = GymStore().withRoutine("A").withExercise(1, exercise("Panca")).withExercise(1, exercise("Squat"))
        val (updated, result) = store.updateExercise(store.exercise(2)!!.copy(name = "Panca piana", weightHalfKg = null))
        assertEquals(GymResult.Saved(2), result)
        assertNull(updated.exercise(2)!!.weightHalfKg)
        assertEquals(GymResult.NotFound, store.updateExercise(exercise().copy(id = 99)).second)

        assertEquals(listOf("Squat", "Panca"), store.moveExercise(3, -1).routines[0].exercises.map { it.name })
        assertEquals(listOf("Squat"), store.deleteExercise(2).routines[0].exercises.map { it.name })
    }

    @Test
    fun toggleProgression() {
        val store = GymStore().withRoutine("A").withExercise(1, exercise())
        val toggled = store.toggleProgression(2)
        assertEquals(Progression.Increase, toggled.exercise(2)!!.progression)
        assertEquals(Progression.Repeat, toggled.toggleProgression(2).exercise(2)!!.progression)
    }

    @Test
    fun applyIncreaseAddsTheIncrementAndGoesBackToRepeat() {
        val store = GymStore().withRoutine("A").withExercise(1, exercise(weight = 28, progression = Progression.Increase, inc = 4))
        val (applied, result) = store.applyIncrease(2)
        val ex = applied.exercise(2)!!
        assertEquals(32, ex.weightHalfKg) // 14 kg + 2 kg
        assertEquals(Progression.Repeat, ex.progression)
        assertEquals(AppliedIncrease(ProgressSnapshot(2, 28, Progression.Increase), 32), result)

        // A second tap has nothing to apply.
        val (again, none) = applied.applyIncrease(2)
        assertNull(none)
        assertSame(applied, again)

        // Undo restores weight and progression.
        val undone = applied.undoIncrease(result!!)
        assertEquals(store.exercise(2), undone.exercise(2))
    }

    @Test
    fun undoKeepsOtherEdits() {
        val store = GymStore().withRoutine("A").withExercise(1, exercise(weight = 28, progression = Progression.Increase))
        val (applied, result) = store.applyIncrease(2)
        val edited = applied.updateExercise(applied.exercise(2)!!.copy(reps = 12)).first
        val undone = edited.undoIncrease(result!!).exercise(2)!!
        assertEquals(12, undone.reps)
        assertEquals(28, undone.weightHalfKg)
        assertEquals(Progression.Increase, undone.progression)
    }

    @Test
    fun staleUndoChangesNothing() {
        val store = GymStore().withRoutine("A").withExercise(1, exercise(weight = 28, progression = Progression.Increase))
        val (applied, result) = store.applyIncrease(2)
        // Weight edited after Apply.
        val reweighed = applied.updateExercise(applied.exercise(2)!!.copy(weightHalfKg = 40)).first
        assertSame(reweighed, reweighed.undoIncrease(result!!))
        // Progression toggled back to Increase after Apply.
        val toggled = applied.toggleProgression(2)
        assertSame(toggled, toggled.undoIncrease(result))
        // Exercise deleted.
        val deleted = applied.deleteExercise(2)
        assertSame(deleted, deleted.undoIncrease(result))
    }

    @Test
    fun renameToSameNameWithDifferentCase() {
        val store = GymStore().withRoutine("panca").withRoutine("Squat")
        val (renamed, result) = store.renameRoutine(1, "Panca")
        assertEquals(GymResult.Saved(1), result)
        assertEquals(listOf("Panca", "Squat"), renamed.routines.map { it.name })
    }

    @Test
    fun duplicateAtTheLimit() {
        val full = (1..MAX_ROUTINES).fold(GymStore()) { s, i -> s.withRoutine("R$i") }
        val (same, result) = full.duplicateRoutine(1, "copia")
        assertEquals(GymResult.LimitReached, result)
        assertSame(full, same)
    }

    @Test
    fun applyIncreaseNeedsIncreaseAndAWeight() {
        val repeat = GymStore().withRoutine("A").withExercise(1, exercise(progression = Progression.Repeat))
        assertNull(repeat.applyIncrease(2).second)
        val bodyweight = GymStore().withRoutine("A").withExercise(1, exercise(weight = null, progression = Progression.Increase))
        assertNull(bodyweight.applyIncrease(2).second)
        assertNull(GymStore().applyIncrease(1).second)
    }

    @Test
    fun applyIncreaseIsCappedAtTheMaximum() {
        val max = ExerciseLimits.weightHalfKg.last
        val store = GymStore().withRoutine("A").withExercise(1, exercise(weight = max - 1, progression = Progression.Increase, inc = 20))
        assertEquals(max, store.applyIncrease(2).first.exercise(2)!!.weightHalfKg)
    }

    // endregion

    @Test
    fun decodesOlderOrPartialFiles() {
        val json = """{"routines":[{"id":3,"name":"A","exercises":[{"id":4,"name":"Panca"}]}],"unknown":1}"""
        val store = StorageJson.decodeFromString(GymStore.serializer(), json)
        val ex = store.routines.single().exercises.single()
        assertNull(ex.weightHalfKg)
        assertEquals(Progression.Repeat, ex.progression)
        assertEquals(ExerciseLimits.DEFAULT_INCREMENT_HALF_KG, ex.incrementHalfKg)
        assertEquals(3L, store.selectedRoutine?.id)
    }
}
