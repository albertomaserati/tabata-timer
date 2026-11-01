package com.amasesoft.tabatatimer.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RoutineTest {

    @Test
    fun formatsWholeAndHalfKilos() {
        assertEquals("14", formatWeight(28, ','))
        assertEquals("12,5", formatWeight(25, ','))
        assertEquals("12.5", formatWeight(25, '.'))
        assertEquals("0,5", formatWeight(1, ','))
        assertEquals("500", formatWeight(1000, '.'))
    }

    @Test
    fun nextWeightOnlyWhenIncreasingAWeight() {
        val ex = Exercise(id = 1, name = "Panca", weightHalfKg = 28, progression = Progression.Increase, incrementHalfKg = 5)
        assertEquals(33, ex.nextWeightHalfKg) // 14 + 2.5 = 16.5 kg
        assertNull(ex.copy(progression = Progression.Repeat).nextWeightHalfKg)
        assertNull(ex.copy(weightHalfKg = null).nextWeightHalfKg)
    }

    @Test
    fun nameValidation() {
        assertEquals(GymNameError.Empty, validateGymName("   ", 40))
        assertEquals(GymNameError.TooLong, validateGymName("x".repeat(41), 40))
        assertNull(validateGymName("  ${"x".repeat(40)}  ", 40))
    }
}
