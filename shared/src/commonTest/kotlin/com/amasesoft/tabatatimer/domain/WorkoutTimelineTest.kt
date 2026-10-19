package com.amasesoft.tabatatimer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class WorkoutTimelineTest {

    private val timeline = WorkoutTimeline(
        WorkoutPlanner.steps(
            WorkoutConfig(prepareSeconds = 5, sets = 1, plan = CyclePlan.Uniform(20, 10, 2)),
        ),
    ) // Prepare 5, Work 20, Rest 10, Work 20 = 55s

    @Test
    fun positionAtStart() {
        val p = timeline.positionAt(0.seconds)
        assertEquals(0, p.stepIndex)
        assertEquals(5, p.displaySecondsInStep)
        assertEquals(55, p.displayTotalSeconds)
        assertFalse(p.isFinished)
    }

    @Test
    fun displayRoundsUp() {
        val p = timeline.positionAt(100.milliseconds)
        assertEquals(5, p.displaySecondsInStep)
        assertEquals(4, timeline.positionAt(1.seconds).displaySecondsInStep)
    }

    @Test
    fun boundariesBelongToNextStep() {
        val p = timeline.positionAt(5.seconds)
        assertEquals(1, p.stepIndex)
        assertEquals(20, p.displaySecondsInStep)
        assertEquals(2, timeline.positionAt(25.seconds).stepIndex)
        assertEquals(3, timeline.positionAt(54.999.seconds).stepIndex)
    }

    @Test
    fun finishedAtTotal() {
        assertEquals(55.seconds, timeline.totalDuration)
        val p = timeline.positionAt(55.seconds)
        assertTrue(p.isFinished)
        assertEquals(0, p.displayTotalSeconds)
        assertTrue(timeline.positionAt(2000.seconds).isFinished)
    }

    @Test
    fun startOfSteps() {
        assertEquals(0.seconds, timeline.startOf(0))
        assertEquals(35.seconds, timeline.startOf(3))
        assertEquals(55.seconds, timeline.startOf(99))
    }

    @Test
    fun clockMeasuresAgainstReferenceAndPauses() {
        val source = TestTimeSource()
        val clock = ElapsedClock(source)
        clock.start()
        source += 5.seconds
        assertEquals(5.seconds, clock.elapsed())
        clock.pause()
        source += 10.seconds
        assertEquals(5.seconds, clock.elapsed())
        clock.start()
        source += 1.5.seconds
        assertEquals(6.5.seconds, clock.elapsed())
        clock.seek(20.seconds)
        source += 1.seconds
        assertEquals(21.seconds, clock.elapsed())
    }
}
