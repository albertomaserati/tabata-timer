package com.amasesoft.tabatatimer.platform

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    actual val current: String
        @Composable get() = Locale.getDefault().toLanguageTag()

    /**
     * The target locale is resolved on every call: with `null` ("System") it is read from the
     * *system* resources, which our override never touches, so a device language change is
     * picked up (Android re-creates the Activity on that configuration change).
     *
     * The side effects run synchronously, because resources resolved in this same composition
     * pass must already see the new locale, but only when the locale actually differs.
     */
    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        val resources = LocalContext.current.resources
        val target = if (value == null) {
            Resources.getSystem().configuration.locales[0]
        } else {
            Locale.forLanguageTag(value)
        }
        if (Locale.getDefault() != target || configuration.locales[0] != target) {
            Locale.setDefault(target)
            configuration.setLocale(target)
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
        return LocalConfiguration.provides(configuration)
    }
}
