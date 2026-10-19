package com.amasesoft.tabatatimer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * In-app locale override, following the pattern documented by JetBrains for Compose
 * Multiplatform resources ("Locale override"): each platform pushes the chosen locale where
 * Compose resources read it from (default `Locale` + `Configuration` on Android,
 * the `AppleLanguages` user default on iOS).
 *
 * The UI subtree must be re-created when the value changes (see `key(language)` in `App`),
 * because resources cache the environment they resolved.
 */
expect object LocalAppLocale {
    val current: String @Composable get

    /** @param value BCP-47 language tag, or `null` to follow the system locale. */
    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}
