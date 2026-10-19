package com.amasesoft.tabatatimer.platform

import androidx.compose.runtime.Composable

/**
 * Aligns the system bar icons with the in-app theme (which may differ from the system one).
 * No-op on iOS, where the status bar follows the hosting view controller.
 */
@Composable
expect fun SystemBarsAppearance(darkTheme: Boolean)
