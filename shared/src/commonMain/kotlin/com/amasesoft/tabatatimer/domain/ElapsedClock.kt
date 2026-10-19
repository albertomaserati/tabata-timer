package com.amasesoft.tabatatimer.domain

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Pausable stopwatch based on a [TimeSource] (normally [TimeSource.Monotonic]).
 *
 * Elapsed time is always `offset + mark.elapsedNow()`, i.e. measured against a reference
 * point instead of summing `delay` ticks, so it never drifts.
 */
class ElapsedClock(private val timeSource: TimeSource = TimeSource.Monotonic) {

    private var offset: Duration = Duration.ZERO
    private var mark: TimeMark? = null

    val isRunning: Boolean get() = mark != null

    fun elapsed(): Duration = offset + (mark?.elapsedNow() ?: Duration.ZERO)

    fun start() {
        if (mark == null) mark = timeSource.markNow()
    }

    fun pause() {
        offset = elapsed()
        mark = null
    }

    /** Jumps to [to], keeping the running/paused status. */
    fun seek(to: Duration) {
        offset = to
        if (mark != null) mark = timeSource.markNow()
    }
}
