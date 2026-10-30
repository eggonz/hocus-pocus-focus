package com.example.hocuspocusfocus.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class IconPoolTest {
    @Test
    fun everyQuickRoutineIconHasALabel() {
        // The edit screen's picker describes its icons to accessibility services by label
        assertEquals(emptyList<IconPool>(), quickRoutineIcons.filter { it.label == null })
    }

    @Test
    fun quickIconsFillTheIconGridButOneCell() {
        // The icon grid is 5x5, with one cell left free
        val gridCells = 5 * 5 - 1
        assertEquals(gridCells, quickRoutineIcons.size)
        assertEquals(gridCells, quickTaskIcons.size)
    }
}
