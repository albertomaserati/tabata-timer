package com.amasesoft.tabatatimer.platform

import androidx.compose.runtime.Composable

/** Short audio cues played during a workout. */
interface CuePlayer {
    /** Short beep for the last 3 seconds of an activity. */
    fun countdown()

    /** Longer beep when a new activity starts. */
    fun phaseStart()

    /** Workout completed. */
    fun finished()
}

/** Platform cue player bound to the composition lifecycle (resources are released on dispose). */
@Composable
expect fun rememberCuePlayer(): CuePlayer
