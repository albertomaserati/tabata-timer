package com.amasesoft.tabatatimer.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import com.amasesoft.tabatatimer.domain.formatWeight
import org.jetbrains.compose.resources.stringResource
import tabatatimer.shared.generated.resources.Res
import tabatatimer.shared.generated.resources.a11y_half_kilo
import tabatatimer.shared.generated.resources.decimal_separator
import tabatatimer.shared.generated.resources.unit_kg

/**
 * Weight formatting for the active **in-app** language: the decimal separator comes from the
 * string resources (",", "."), which follow `LocalAppLocale`, not only the system locale.
 */
@Immutable
class WeightFormat(val decimalSeparator: Char, private val unit: String, halfKeySpoken: String) {
    /** "12,5" */
    fun number(halfKg: Int): String = formatWeight(halfKg, decimalSeparator)

    /** "12,5 kg" */
    fun withUnit(halfKg: Int): String = "${number(halfKg)} $unit"

    /** Keypad key that adds/removes the half kilo. */
    val halfKey = ExtraKey.Half(label = "${decimalSeparator}5", spokenLabel = halfKeySpoken)
}

@Composable
fun rememberWeightFormat(): WeightFormat {
    val separator = stringResource(Res.string.decimal_separator).firstOrNull() ?: '.'
    val unit = stringResource(Res.string.unit_kg)
    val halfSpoken = stringResource(Res.string.a11y_half_kilo)
    return remember(separator, unit, halfSpoken) { WeightFormat(separator, unit, halfSpoken) }
}
