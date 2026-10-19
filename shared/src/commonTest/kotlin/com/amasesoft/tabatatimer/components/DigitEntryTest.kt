package com.amasesoft.tabatatimer.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DigitEntryTest {

    private fun DigitEntry.typeAll(vararg digits: Int): DigitEntry = digits.fold(this) { e, d -> e.type(d) }

    @Test
    fun durationStartsFromCurrentValue() {
        val entry = DurationEntry.of(90)
        assertTrue(entry.isPristine)
        assertEquals("1:30", entry.display)
        assertEquals(90, entry.value)
        assertEquals("0:20", DurationEntry.of(20).display)
        assertEquals("0:00", DurationEntry.of(0).display)
        assertEquals(0, DurationEntry.of(0).value)
    }

    @Test
    fun firstDigitReplacesCurrentValue() {
        val entry = DurationEntry.of(90).type(5)
        assertFalse(entry.isPristine)
        assertEquals("0:05", entry.display)
        assertEquals(5, entry.value)
    }

    @Test
    fun digitsFillFromTheRight() {
        val entry = DurationEntry.of(20).typeAll(1, 3, 0)
        assertEquals("1:30", entry.display)
        assertEquals(90, entry.value)
    }

    @Test
    fun overflowingSecondsAreNormalized() {
        val entry = DurationEntry.of(20).typeAll(9, 0)
        assertEquals("0:90", entry.display) // shown as typed
        assertEquals(90, entry.value) // = 1:30
        assertEquals(DurationEntry.format(90), "1:30")
    }

    @Test
    fun leadingZerosAreIgnoredAndLengthIsCapped() {
        val entry = DurationEntry.of(20).typeAll(0, 0, 1, 2, 3, 4, 5)
        assertEquals("12:34", entry.display)
        assertEquals(12 * 60 + 34, entry.value)
    }

    @Test
    fun backspaceRemovesLastDigit() {
        assertEquals("0:13", DurationEntry.of(20).typeAll(1, 3, 0).backspace().display)
        // On the prefilled value, backspace edits it instead of replacing.
        val edited = DurationEntry.of(90).backspace()
        assertFalse(edited.isPristine)
        assertEquals("0:13", edited.display)
        assertEquals(0, DurationEntry.of(5).backspace().value)
    }

    @Test
    fun durationValidationAgainstRange() {
        assertTrue(DurationEntry.of(20).typeAll(4, 5).isValid(1..3600))
        assertFalse(DurationEntry.of(20).backspace().backspace().isValid(1..3600)) // empty
        assertFalse(DurationEntry.of(20).typeAll(9, 9, 5, 9).isValid(1..3600)) // 99:59
        assertFalse(DurationEntry.of(20).type(0).isValid(1..3600))
        assertTrue(DurationEntry.of(20).typeAll(6, 0, 0, 0).isValid(1..3600)) // 60:00
    }

    @Test
    fun countEntry() {
        val range = 1..50
        val entry = CountEntry.of(8, range)
        assertTrue(entry.isPristine)
        assertEquals("8", entry.display)
        val typed = entry.typeAll(1, 2, 3) // max 2 digits for 50
        assertEquals(12, typed.value)
        assertTrue(typed.isValid(range))
        assertFalse(entry.typeAll(7, 5).isValid(range))
        assertFalse(entry.type(0).isValid(range)) // leading zero ignored -> empty
        assertEquals("0", entry.type(0).display)
        assertEquals(1, entry.typeAll(1, 2).backspace().value)
    }

    @Test
    fun zeroIsConfirmableWhereTheRangeAllowsIt() {
        val restRange = 0..3600
        // Prefilled 0 (e.g. a rest of 0s) is valid as-is.
        assertTrue(DurationEntry.of(0).isValid(restRange))
        // Typing 0 or 00, or deleting every digit, gives 0:00.
        assertEquals(0, DurationEntry.of(30).type(0).value)
        assertEquals(0, DurationEntry.of(30).type(0).type(0).value)
        assertTrue(DurationEntry.of(30).type(0).isValid(restRange))
        assertTrue(DurationEntry.of(5).backspace().isValid(restRange))
        assertEquals("0:00", DurationEntry.of(5).backspace().display)
    }

    @Test
    fun zeroIsInvalidWhenTheMinimumIsAboveIt() {
        val workRange = 1..3600
        assertFalse(DurationEntry.of(0).isValid(workRange))
        assertFalse(DurationEntry.of(20).type(0).isValid(workRange))
        val countRange = 1..50
        assertEquals(0, CountEntry.of(8, countRange).backspace().value)
        assertFalse(CountEntry.of(8, countRange).backspace().isValid(countRange))
        assertTrue(CountEntry.of(8, 0..10).type(0).isValid(0..10))
    }
}
