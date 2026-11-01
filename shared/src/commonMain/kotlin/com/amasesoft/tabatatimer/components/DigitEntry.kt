package com.amasesoft.tabatatimer.components

import androidx.compose.runtime.Immutable
import com.amasesoft.tabatatimer.domain.formatSeconds
import com.amasesoft.tabatatimer.domain.formatWeight

/**
 * Pure state of the digit-by-digit entry used by the value dialogs (no Compose, unit tested).
 *
 * The entry starts from the current value, shown as-is ([isPristine]): the first typed digit
 * replaces it, and backspace edits it.
 */
@Immutable
sealed interface DigitEntry {
    val digits: String
    val isPristine: Boolean

    /**
     * Value in the field's unit. No digits means 0 (so 0 can be confirmed where the range
     * allows it, e.g. prepare or rest); [isValid] decides against the range.
     */
    val value: Int

    /** Text shown in the dialog while typing. */
    val display: String

    fun type(digit: Int): DigitEntry
    fun backspace(): DigitEntry

    /** The ",5" key; only [WeightEntry] supports it, other entries ignore it. */
    fun toggleHalf(): DigitEntry = this

    fun isValid(range: IntRange): Boolean = value in range
}

/**
 * Timer-style duration entry (as in Google Clock): digits fill `m:ss` from the right.
 * Typing 1, 3, 0 gives `1:30`; typing 9, 0 gives `0:90`, normalized to 90s (`1:30`).
 * Up to [MAX_DIGITS] digits (`mm:ss`).
 */
@Immutable
data class DurationEntry(
    override val digits: String,
    override val isPristine: Boolean = false,
) : DigitEntry {

    override val value: Int
        get() {
            val padded = digits.padStart(MAX_DIGITS, '0')
            val minutes = padded.substring(0, MAX_DIGITS - 2).toInt()
            val seconds = padded.substring(MAX_DIGITS - 2).toInt()
            return minutes * 60 + seconds
        }

    /** Raw digits as `m:ss` (not normalized, e.g. `0:90`), so typing is predictable. */
    override val display: String
        get() {
            val padded = digits.padStart(3, '0')
            val minutes = padded.dropLast(2).toInt()
            return "$minutes:${padded.takeLast(2)}"
        }

    override fun type(digit: Int): DurationEntry {
        require(digit in 0..9)
        val base = if (isPristine) "" else digits
        if (base.length >= MAX_DIGITS) return copy(isPristine = false)
        // No leading zeros: they would not change the value and would eat the digit budget.
        val next = if (base.isEmpty() && digit == 0) "" else base + digit
        return DurationEntry(next)
    }

    override fun backspace(): DurationEntry = DurationEntry(digits.dropLast(1))

    companion object {
        const val MAX_DIGITS = 4

        /** Digits of a value: 90s -> "130" (1:30), 20s -> "20", 0s -> "" (= 0:00). */
        fun of(seconds: Int): DurationEntry {
            val safe = seconds.coerceIn(0, 99 * 60 + 59)
            val raw = "${safe / 60}${(safe % 60).toString().padStart(2, '0')}".trimStart('0')
            return DurationEntry(raw, isPristine = true)
        }

        /** Normalized `m:ss` text of a value, for messages and presets. */
        fun format(seconds: Int): String = formatSeconds(seconds)
    }
}

/** Plain integer entry for counters; at most as many digits as the range maximum. */
@Immutable
data class CountEntry(
    override val digits: String,
    val maxDigits: Int,
    override val isPristine: Boolean = false,
) : DigitEntry {

    override val value: Int get() = digits.toIntOrNull() ?: 0

    override val display: String get() = digits.ifEmpty { "0" }

    override fun type(digit: Int): CountEntry {
        require(digit in 0..9)
        val base = if (isPristine) "" else digits
        if (base.length >= maxDigits) return copy(isPristine = false)
        val next = if (base.isEmpty() && digit == 0) "" else base + digit
        return copy(digits = next, isPristine = false)
    }

    override fun backspace(): CountEntry = copy(digits = digits.dropLast(1), isPristine = false)

    companion object {
        fun of(value: Int, range: IntRange): CountEntry = CountEntry(
            digits = value.coerceAtLeast(0).toString().trimStart('0'),
            maxDigits = range.last.toString().length,
            isPristine = true,
        )
    }
}

/**
 * Weight entry in **half kilograms**: digits are whole kilos, and the ",5" key ([toggleHalf])
 * adds or removes the half kilo. No other decimals can be typed: while the half is on, digits
 * are ignored (as on a calculator after the only allowed decimal); backspace removes the half
 * first, then the digits.
 *
 * [value] is in half kilograms: "12,5" -> 25. [decimalSeparator] follows the in-app language.
 */
@Immutable
data class WeightEntry(
    override val digits: String,
    val half: Boolean,
    val decimalSeparator: Char,
    override val isPristine: Boolean = false,
) : DigitEntry {

    override val value: Int get() = (digits.toIntOrNull() ?: 0) * 2 + if (half) 1 else 0

    override val display: String get() = digits.ifEmpty { "0" } + if (half) "${decimalSeparator}5" else ""

    override fun type(digit: Int): WeightEntry {
        require(digit in 0..9)
        if (isPristine) return WeightEntry("", half = false, decimalSeparator).type(digit)
        if (half || digits.length >= MAX_DIGITS) return this
        val next = if (digits.isEmpty() && digit == 0) "" else digits + digit
        return copy(digits = next)
    }

    override fun backspace(): WeightEntry =
        if (half) copy(half = false, isPristine = false) else copy(digits = digits.dropLast(1), isPristine = false)

    /** On the prefilled value it edits it (14 -> 14,5) instead of replacing it. */
    override fun toggleHalf(): WeightEntry = copy(half = !half, isPristine = false)

    companion object {
        /** Whole kilos up to 999 (the ranges stop at 500 kg). */
        const val MAX_DIGITS = 3

        fun of(halfKg: Int, decimalSeparator: Char): WeightEntry {
            val safe = halfKg.coerceIn(0, MAX_HALF_KG)
            return WeightEntry(
                digits = (safe / 2).toString().trimStart('0'),
                half = safe % 2 == 1,
                decimalSeparator = decimalSeparator,
                isPristine = true,
            )
        }

        private const val MAX_HALF_KG = 999 * 2 + 1

        /** Text of a value without unit, for messages and presets. */
        fun format(halfKg: Int, decimalSeparator: Char): String = formatWeight(halfKg, decimalSeparator)
    }
}
