package com.amasesoft.tabatatimer.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A single work/rest cycle. [restSeconds] is ignored for the last cycle of each set
 * (the last cycle is "work only": what follows is either the rest between sets or the end).
 */
@Serializable
data class Cycle(
    val name: String = "",
    val workSeconds: Int,
    val restSeconds: Int,
)

/**
 * How the cycles of a set are described.
 *
 * - [Uniform] is the "simple" mode: N identical cycles.
 * - [Custom] is the "advanced" mode: every cycle has its own name and durations.
 *
 * Simple mode is just a compact special case of advanced mode: [Uniform.toCustom] expands it
 * losslessly, while [Custom.toUniform] is lossless only when [Custom.isUniform] is true.
 */
@Serializable
sealed interface CyclePlan {
    val count: Int

    /** The cycles of a single set, in order. */
    fun expand(): List<Cycle>

    @Serializable
    @SerialName("uniform")
    data class Uniform(
        val workSeconds: Int,
        val restSeconds: Int,
        override val count: Int,
    ) : CyclePlan {
        override fun expand(): List<Cycle> =
            List(count) { Cycle(workSeconds = workSeconds, restSeconds = restSeconds) }

        fun toCustom(): Custom = Custom(expand())
    }

    @Serializable
    @SerialName("custom")
    data class Custom(val cycles: List<Cycle>) : CyclePlan {
        override val count: Int get() = cycles.size

        override fun expand(): List<Cycle> = cycles

        /**
         * True when converting to [Uniform] loses nothing: no names, same work everywhere and
         * same rest everywhere except the last cycle, whose rest is never used.
         */
        fun isUniform(): Boolean {
            if (cycles.isEmpty()) return true
            val first = cycles.first()
            val restRelevant = cycles.dropLast(1)
            return cycles.all { it.name.isBlank() && it.workSeconds == first.workSeconds } &&
                restRelevant.all { it.restSeconds == first.restSeconds }
        }

        /** Uses the first cycle as template for every cycle. */
        fun toUniform(): Uniform {
            val first = cycles.firstOrNull() ?: return WorkoutConfig.DefaultPlan
            return Uniform(
                workSeconds = first.workSeconds,
                restSeconds = first.restSeconds,
                count = cycles.size,
            )
        }
    }
}

@Serializable
data class WorkoutConfig(
    val prepareSeconds: Int = 10,
    val sets: Int = 1,
    val restBetweenSetsSeconds: Int = 60,
    val plan: CyclePlan = DefaultPlan,
) {
    val isAdvanced: Boolean get() = plan is CyclePlan.Custom

    /** Clamps every value into [Limits], so that a config is always runnable. */
    fun sanitized(): WorkoutConfig = copy(
        prepareSeconds = prepareSeconds.coerceIn(Limits.prepareSeconds),
        sets = sets.coerceIn(Limits.sets),
        restBetweenSetsSeconds = restBetweenSetsSeconds.coerceIn(Limits.restSeconds),
        plan = when (plan) {
            is CyclePlan.Uniform -> plan.copy(
                workSeconds = plan.workSeconds.coerceIn(Limits.workSeconds),
                restSeconds = plan.restSeconds.coerceIn(Limits.restSeconds),
                count = plan.count.coerceIn(Limits.cycles),
            )

            is CyclePlan.Custom -> if (plan.cycles.isEmpty()) DefaultPlan else plan.copy(
                cycles = plan.cycles.take(Limits.cycles.last).map {
                    it.copy(
                        name = it.name.take(Limits.NAME_MAX_LENGTH),
                        workSeconds = it.workSeconds.coerceIn(Limits.workSeconds),
                        restSeconds = it.restSeconds.coerceIn(Limits.restSeconds),
                    )
                },
            )
        },
    )

    companion object {
        /** Classic Tabata: 8 x (20s work + 10s rest). */
        val DefaultPlan = CyclePlan.Uniform(workSeconds = 20, restSeconds = 10, count = 8)
        val Default = WorkoutConfig()
    }

    object Limits {
        val prepareSeconds = 0..600
        val workSeconds = 1..3600
        val restSeconds = 0..3600
        val cycles = 1..50
        val sets = 1..30
        const val NAME_MAX_LENGTH = 40
        const val DESCRIPTION_MAX_LENGTH = 200
    }
}
