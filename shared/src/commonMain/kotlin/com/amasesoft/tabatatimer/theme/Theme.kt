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

// Colors aligned with the AM logo: teal primary, navy secondary, green tertiary.
private val LightColorScheme = lightColorScheme(
    primary = LogoTeal,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = LogoNavy,
    onSecondary = Color.White,
    secondaryContainer = Navy90,
    onSecondaryContainer = Navy10,
    tertiary = LogoGreen,
    onTertiary = Color.White,
    tertiaryContainer = Green90,
    onTertiaryContainer = Green10,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightBackground,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceContainerHighest,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = LogoTeal,
    onPrimaryContainer = Teal90,
    secondary = Navy80,
    onSecondary = Navy20,
    secondaryContainer = Navy30,
    onSecondaryContainer = Navy90,
    tertiary = Green80,
    onTertiary = Green10,
    tertiaryContainer = Green30,
    onTertiaryContainer = Green90,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceContainerHighest,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
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

private val LightPhaseColors = PhaseColors(prepare = Amber40, work = Coral40, rest = LogoGreen, setRest = Navy40)
private val DarkPhaseColors = PhaseColors(prepare = Amber80, work = Coral80, rest = Green80, setRest = Navy80)

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
