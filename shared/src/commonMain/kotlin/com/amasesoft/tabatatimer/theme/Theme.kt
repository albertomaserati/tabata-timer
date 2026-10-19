package com.amasesoft.tabatatimer.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.amasesoft.tabatatimer.domain.PhaseType

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = GreenGrey40,
    tertiary = Tiffany40,
    background = Color.White,
    onBackground = Color.DarkGray,
    primaryContainer = GreenGrey40,
    onPrimaryContainer = Color.White,
    secondaryContainer = GreenGrey90,
    onSecondaryContainer = GreenGrey10,
    onPrimary = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = Tiffany80,
    background = Color.DarkGray,
    onBackground = Color.White,
    primaryContainer = GreenGrey80,
    onPrimaryContainer = DarkGreen20,
    secondaryContainer = GreenGrey30,
    onSecondaryContainer = GreenGrey90,
    // Dark text on the light green primary for readable contrast.
    onPrimary = DarkGreen20
)

/** Accent color for each workout phase. */
@Immutable
data class PhaseColors(
    val prepare: Color,
    val work: Color,
    val rest: Color,
    val setRest: Color,
) {
    fun of(type: PhaseType): Color = when (type) {
        PhaseType.Prepare -> prepare
        PhaseType.Work -> work
        PhaseType.Rest -> rest
        PhaseType.SetRest -> setRest
    }
}

private val LightPhaseColors = PhaseColors(prepare = Amber40, work = Coral40, rest = Green40, setRest = Blue40)
private val DarkPhaseColors = PhaseColors(prepare = Amber80, work = Coral80, rest = Green80, setRest = Blue80)

val LocalPhaseColors = staticCompositionLocalOf { LightPhaseColors }

/** Theme extensions not covered by [MaterialTheme]. */
object AppThemeExtras {
    val phaseColors: PhaseColors
        @Composable get() = LocalPhaseColors.current
}

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val phaseColors = if (darkTheme) DarkPhaseColors else LightPhaseColors

    CompositionLocalProvider(LocalPhaseColors provides phaseColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
