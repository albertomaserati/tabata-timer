package com.amasesoft.tabatatimer.platform

import androidx.compose.runtime.Composable

/** Keeps the display awake while this composable is in the composition and [enabled] is true. */
@Composable
expect fun KeepScreenOn(enabled: Boolean)
