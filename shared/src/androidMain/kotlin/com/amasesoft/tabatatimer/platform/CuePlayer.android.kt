package com.amasesoft.tabatatimer.platform

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember

private class ToneCuePlayer : CuePlayer {
    // ToneGenerator needs no permission; creation can fail on devices without audio.
    private val tone: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 90) }.getOrNull()

    override fun countdown() {
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }

    override fun phaseStart() {
        tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
    }

    override fun finished() {
        tone?.startTone(ToneGenerator.TONE_CDMA_CONFIRM, 800)
    }

    fun release() {
        tone?.release()
    }
}

@Composable
actual fun rememberCuePlayer(): CuePlayer {
    val player = remember { ToneCuePlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
