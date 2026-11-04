package com.amasesoft.tabatatimer.ui.navigation

import com.amasesoft.tabatatimer.ui.editor.EditorMode
import kotlin.test.Test
import kotlin.test.assertEquals

class EnumNavTypeTest {

    @Test
    fun roundTripsEveryValue() {
        val type = enumNavType<EditorMode>()
        EditorMode.entries.forEach { assertEquals(it, type.parseValue(type.serializeAsValue(it))) }
    }
}
