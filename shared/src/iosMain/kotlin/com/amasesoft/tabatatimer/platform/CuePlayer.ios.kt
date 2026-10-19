package com.amasesoft.tabatatimer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AudioToolbox.AudioServicesPlaySystemSound

/**
 * Uses built-in system sounds (no bundled audio files). They follow the device
 * ringer/silent switch, like most system sounds on iOS.
 */
private object SystemSoundCuePlayer : CuePlayer {
    private const val TICK: UInt = 1057u      // "Tink"
    private const val PHASE: UInt = 1052u     // SIM toolkit general beep
    private const val FINISHED: UInt = 1025u  // "Fanfare"-like new mail chime

    override fun countdown() = AudioServicesPlaySystemSound(TICK)
    override fun phaseStart() = AudioServicesPlaySystemSound(PHASE)
    override fun finished() = AudioServicesPlaySystemSound(FINISHED)
}

@Composable
actual fun rememberCuePlayer(): CuePlayer = remember { SystemSoundCuePlayer }
