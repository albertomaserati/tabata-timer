package com.amasesoft.tabatatimer.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeightEntryTest {

    private fun WeightEntry.typeAll(vararg digits: Int): WeightEntry = digits.fold(this) { e, d -> e.type(d) }

    @Test
    fun prefilledValueIsShownAndReplacedByTheFirstDigit() {
        val entry = WeightEntry.of(25, ',')
        assertEquals("12,5", entry.display)
        assertEquals(25, entry.value)
        assertTrue(entry.isPristine)
        val typed = entry.type(8)
        assertEquals("8", typed.display)
        assertEquals(16, typed.value)
        assertFalse(typed.isPristine)
    }

    @Test
    fun halfKeyAddsAndRemovesTheHalfKilo() {
        val entry = WeightEntry.of(0, ',').typeAll(1, 4)
        assertEquals(28, entry.value)
        val half = entry.toggleHalf()
        assertEquals("14,5", half.display)
        assertEquals(29, half.value)
        assertEquals(28, half.toggleHalf().value)
    }

    @Test
    fun halfKeyEditsThePrefilledValue() {
        val half = WeightEntry.of(28, '.').toggleHalf()
        assertEquals("14.5", half.display)
        assertFalse(half.isPristine)
        assertEquals(28, WeightEntry.of(29, '.').toggleHalf().value)
    }

    @Test
    fun noOtherDecimalsCanBeTyped() {
        val half = WeightEntry.of(0, ',').typeAll(1, 2).toggleHalf()
        // Digits after the half are ignored: 12,5 cannot become 12,55 or 125,5.
        assertEquals(half, half.type(5))
        assertEquals("12,5", half.type(5).display)
    }

    @Test
    fun backspaceRemovesTheHalfFirst() {
        val half = WeightEntry.of(0, ',').typeAll(1, 2).toggleHalf()
        val noHalf = half.backspace()
        assertEquals("12", noHalf.display)
        assertEquals("1", noHalf.backspace().display)
        assertEquals("0", noHalf.backspace().backspace().display)
        assertEquals(0, noHalf.backspace().backspace().value)
    }

    @Test
    fun halfKiloAloneAndLeadingZeros() {
        val entry = WeightEntry.of(0, ',').typeAll(0, 0)
        assertEquals("0", entry.display)
        assertEquals("0,5", entry.toggleHalf().display)
        assertEquals(1, entry.toggleHalf().value)
    }

    @Test
    fun atMostThreeDigitsAndRangeValidation() {
        val entry = WeightEntry.of(0, ',').typeAll(5, 0, 0, 7)
        assertEquals("500", entry.display)
        assertTrue(entry.isValid(1..1000))
        assertFalse(entry.toggleHalf().isValid(1..1000)) // 500,5 kg
        assertFalse(WeightEntry.of(0, ',').isValid(1..1000)) // 0 kg
    }
}
