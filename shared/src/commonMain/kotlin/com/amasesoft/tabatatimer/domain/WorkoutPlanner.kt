package com.amasesoft.tabatatimer.domain

enum class PhaseType { Prepare, Work, Rest, SetRest }

/**
 * One timed activity of a running workout.
 *
 * @property setNumber 1-based set this step belongs to (or is about to start, for [PhaseType.Prepare]).
 * @property cycleNumber 1-based cycle this step belongs to.
 * @property cycleName name given to the cycle in advanced mode, blank otherwise.
 */
data class Step(
    val type: PhaseType,
    val durationSeconds: Int,
    val setNumber: Int,
    val cycleNumber: Int,
    val cycleName: String = "",
)

/** Expands a [WorkoutConfig] into the flat sequence of [Step]s that the timer will run. */
object WorkoutPlanner {

    fun steps(config: WorkoutConfig): List<Step> {
        val cycles = config.plan.expand()
        if (cycles.isEmpty() || config.sets <= 0) return emptyList()
        return buildList {
            if (config.prepareSeconds > 0) {
                add(Step(PhaseType.Prepare, config.prepareSeconds, setNumber = 1, cycleNumber = 1))
            }
            for (set in 1..config.sets) {
                cycles.forEachIndexed { index, cycle ->
                    val cycleNumber = index + 1
                    if (cycle.workSeconds > 0) {
                        add(Step(PhaseType.Work, cycle.workSeconds, set, cycleNumber, cycle.name.trim()))
                    }
                    // The last cycle of every set is work only.
                    val isLastCycle = index == cycles.lastIndex
                    if (!isLastCycle && cycle.restSeconds > 0) {
                        add(Step(PhaseType.Rest, cycle.restSeconds, set, cycleNumber, cycle.name.trim()))
                    }
                }
                if (set < config.sets && config.restBetweenSetsSeconds > 0) {
                    add(Step(PhaseType.SetRest, config.restBetweenSetsSeconds, set, cycles.size))
                }
            }
        }
    }

    fun totalSeconds(config: WorkoutConfig): Int = steps(config).sumOf { it.durationSeconds }
}
