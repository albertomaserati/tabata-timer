package com.amasesoft.tabatatimer

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.amasesoft.tabatatimer.data.ThemeMode
import com.amasesoft.tabatatimer.di.AppContainer
import com.amasesoft.tabatatimer.platform.LocalAppLocale
import com.amasesoft.tabatatimer.platform.SystemBarsAppearance
import com.amasesoft.tabatatimer.theme.AppTheme
import com.amasesoft.tabatatimer.ui.navigation.AppNavHost

/** Root composable rendered by both platforms. */
@Composable
fun App(container: AppContainer) {
    val settings by container.settings.collectAsStateWithLifecycle()
    // Settings are read once per process (a few ms): render nothing until then, to avoid a
    // flash of the wrong theme or language.
    val current = settings ?: return

    // Hoisted above key(): the back stack survives a language change.
    val navController = rememberNavController()

    val darkTheme = when (current.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    SystemBarsAppearance(darkTheme)

    val focusManager = LocalFocusManager.current

    CompositionLocalProvider(LocalAppLocale provides current.language.tag) {
        // Compose resources cache the resolved locale: re-create the UI when it changes.
        key(current.language) {
            AppTheme(darkTheme = darkTheme) {
                // iOS has no back button to close the keyboard: tapping outside a field closes it.
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppNavHost(
                        navController = navController,
                        container = container,
                        soundEnabled = current.soundEnabled,
                        tenSecondWarning = current.tenSecondWarning,
                    )
                }
            }
        }
    }
}
