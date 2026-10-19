package com.amasesoft.tabatatimer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkoutPlannerTest {

    private val simple = WorkoutConfig(
        prepareSeconds = 10,
        sets = 2,
        restBetweenSetsSeconds = 60,
        plan = CyclePlan.Uniform(workSeconds = 20, restSeconds = 10, count = 3),
    )

    @Test
    fun lastCycleOfEachSetIsWorkOnly() {
        val types = WorkoutPlanner.steps(simple).map { it.type }
        assertEquals(
            listOf(
                PhaseType.Prepare,
                PhaseType.Work, PhaseType.Rest, PhaseType.Work, PhaseType.Rest, PhaseType.Work,
                PhaseType.SetRest,
                PhaseType.Work, PhaseType.Rest, PhaseType.Work, PhaseType.Rest, PhaseType.Work,
            ),
            types,
        )
    }

    @Test
    fun totalDurationCountsEveryStep() {
        // 10 prepare + 2 x (3 x 20 work + 2 x 10 rest) + 60 between sets
        assertEquals(230, WorkoutPlanner.totalSeconds(simple))
    }

    @Test
    fun zeroDurationsAreSkipped() {
        val config = simple.copy(prepareSeconds = 0, restBetweenSetsSeconds = 0, plan = CyclePlan.Uniform(20, 0, 2))
        val types = WorkoutPlanner.steps(config).map { it.type }
        assertEquals(List(4) { PhaseType.Work }, types)
    }

    @Test
    fun noRestBetweenSetsAfterLastSet() {
        val steps = WorkoutPlanner.steps(simple.copy(sets = 1))
        assertTrue(steps.none { it.type == PhaseType.SetRest })
        assertEquals(PhaseType.Work, steps.last().type)
    }

    @Test
    fun stepsCarrySetCycleAndName() {
        val config = WorkoutConfig(
            prepareSeconds = 0,
            sets = 2,
            restBetweenSetsSeconds = 30,
            plan = CyclePlan.Custom(
                listOf(
                    Cycle(name = " Squat ", workSeconds = 40, restSeconds = 20),
                    Cycle(name = "Burpees", workSeconds = 30, restSeconds = 99),
                ),
            ),
        )
        val steps = WorkoutPlanner.steps(config)
        assertEquals(
            listOf(
                Step(PhaseType.Work, 40, 1, 1, "Squat"),
                Step(PhaseType.Rest, 20, 1, 1, "Squat"),
                Step(PhaseType.Work, 30, 1, 2, "Burpees"), // last cycle: rest 99 ignored
                Step(PhaseType.SetRest, 30, 1, 2),
                Step(PhaseType.Work, 40, 2, 1, "Squat"),
                Step(PhaseType.Rest, 20, 2, 1, "Squat"),
                Step(PhaseType.Work, 30, 2, 2, "Burpees"),
            ),
            steps,
        )
    }

    @Test
    fun uniformExpandsToCustomLosslessly() {
        val uniform = CyclePlan.Uniform(20, 10, 4)
        val custom = uniform.toCustom()
        assertTrue(custom.isUniform())
        assertEquals(uniform, custom.toUniform())
        assertEquals(
            WorkoutPlanner.steps(simple),
            WorkoutPlanner.steps(simple.copy(plan = (simple.plan as CyclePlan.Uniform).toCustom())),
        )
    }

    @Test
    fun uniformityIgnoresRestOfLastCycleButNotNames() {
        val base = listOf(Cycle(workSeconds = 20, restSeconds = 10), Cycle(workSeconds = 20, restSeconds = 0))
        assertTrue(CyclePlan.Custom(base).isUniform())
        assertFalse(CyclePlan.Custom(base.mapIndexed { i, c -> if (i == 0) c.copy(name = "A") else c }).isUniform())
        assertFalse(CyclePlan.Custom(base + Cycle(workSeconds = 25, restSeconds = 10)).isUniform())
    }

    @Test
    fun sanitizedClampsValues() {
        val config = WorkoutConfig(prepareSeconds = -5, sets = 0, plan = CyclePlan.Uniform(0, -1, 0)).sanitized()
        assertEquals(0, config.prepareSeconds)
        assertEquals(1, config.sets)
        assertEquals(CyclePlan.Uniform(1, 0, 1), config.plan)
        assertEquals(WorkoutConfig.DefaultPlan, WorkoutConfig(plan = CyclePlan.Custom(emptyList())).sanitized().plan)
    }

    @Test
    fun formatsSeconds() {
        assertEquals("0:05", formatSeconds(5))
        assertEquals("1:30", formatSeconds(90))
        assertEquals("1:01:01", formatSeconds(3661))
        assertEquals("0:00", formatSeconds(-3))
    }

    @Test
    fun singleSetSingleCycleWithoutPrepareIsOneWorkStep() {
        val config = WorkoutConfig(prepareSeconds = 0, sets = 1, restBetweenSetsSeconds = 60, plan = CyclePlan.Uniform(30, 15, 1))
        assertEquals(listOf(Step(PhaseType.Work, 30, 1, 1)), WorkoutPlanner.steps(config))
        assertEquals(30, WorkoutPlanner.totalSeconds(config))
        val timeline = WorkoutTimeline(WorkoutPlanner.steps(config))
        assertEquals(30, timeline.positionAt(kotlin.time.Duration.ZERO).displaySecondsInStep)
    }
}
