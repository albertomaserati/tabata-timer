package com.amasesoft.tabatatimer.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

actual object LocalAppLocale {
    private const val LANG_KEY = "AppleLanguages"
    private val LocalAppLocale = staticCompositionLocalOf { preferredLanguage() }

    /** Last override written in this process; [NOT_APPLIED] until the first call. */
    private var lastApplied: String? = NOT_APPLIED
    private const val NOT_APPLIED = "\u0000"

    private fun preferredLanguage(): String = NSLocale.preferredLanguages.firstOrNull() as? String ?: "en"

    actual val current: String
        @Composable get() = LocalAppLocale.current

    /**
     * `AppleLanguages` in the app's user defaults overrides the language list Compose resources
     * read (`NSLocale.preferredLanguages`); removing it ("System") makes the device languages
     * visible again. The defaults are written only when the requested value changes.
     */
    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (lastApplied != value) {
            val defaults = NSUserDefaults.standardUserDefaults
            if (value == null) defaults.removeObjectForKey(LANG_KEY) else defaults.setObject(listOf(value), LANG_KEY)
            lastApplied = value
        }
        return LocalAppLocale.provides(value ?: preferredLanguage())
    }
}
