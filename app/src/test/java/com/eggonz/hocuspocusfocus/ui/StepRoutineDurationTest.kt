package com.eggonz.hocuspocusfocus.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class StepRoutineDurationTest {

    @Test
    fun `adds and subtracts minutes`() {
        assertEquals(65, stepRoutineDuration(60, 5))
        assertEquals(30, stepRoutineDuration(60, -30))
    }

    @Test
    fun `stays between 5 minutes and 3 hours`() {
        assertEquals(5, stepRoutineDuration(20, -30))
        assertEquals(3 * 60, stepRoutineDuration(170, 30))
    }
}
