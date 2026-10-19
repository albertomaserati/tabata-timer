package com.amasesoft.tabatatimer.domain

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Where a workout is at a given elapsed time. */
data class TimelinePosition(
    val stepIndex: Int,
    val elapsedInStep: Duration,
    val remainingInStep: Duration,
    val totalRemaining: Duration,
    val isFinished: Boolean,
) {
    /** Seconds shown to the user: rounded up, so a 20s step shows "20" right after it starts. */
    val displaySecondsInStep: Int
        get() = remainingInStep.inWholeMilliseconds.let { ((it + 999) / 1000).toInt() }

    val displayTotalSeconds: Int
        get() = totalRemaining.inWholeMilliseconds.let { ((it + 999) / 1000).toInt() }
}

/**
 * Maps an elapsed time (measured against a monotonic reference, see [ElapsedClock]) to the
 * current step. Being a pure function of the elapsed time, the timer cannot accumulate drift.
 */
class WorkoutTimeline(val steps: List<Step>) {

    private val starts: List<Duration> = steps.runningFold(Duration.ZERO) { acc, step ->
        acc + step.durationSeconds.seconds
    }

    val totalDuration: Duration = starts.last()

    fun startOf(stepIndex: Int): Duration = starts[stepIndex.coerceIn(0, steps.size)]

    fun positionAt(elapsed: Duration): TimelinePosition {
        if (steps.isEmpty() || elapsed >= totalDuration) {
            return TimelinePosition(
                stepIndex = (steps.size - 1).coerceAtLeast(0),
                elapsedInStep = steps.lastOrNull()?.durationSeconds?.seconds ?: Duration.ZERO,
                remainingInStep = Duration.ZERO,
                totalRemaining = Duration.ZERO,
                isFinished = true,
            )
        }
        val clamped = elapsed.coerceAtLeast(Duration.ZERO)
        // Last step whose start is <= elapsed.
        var index = 0
        while (index + 1 < steps.size && starts[index + 1] <= clamped) index++
        val inStep = clamped - starts[index]
        return TimelinePosition(
            stepIndex = index,
            elapsedInStep = inStep,
            remainingInStep = steps[index].durationSeconds.seconds - inStep,
            totalRemaining = totalDuration - clamped,
            isFinished = false,
        )
    }
}
