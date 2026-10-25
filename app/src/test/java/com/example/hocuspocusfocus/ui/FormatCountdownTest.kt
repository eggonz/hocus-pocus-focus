package com.example.hocuspocusfocus.ui

import com.example.hocuspocusfocus.FOCUS_DURATION_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatCountdownTest {

    @Test
    fun `shows the full duration until a whole second has passed`() {
        assertEquals("30:00", formatCountdown(FOCUS_DURATION_MILLIS))
        assertEquals("30:00", formatCountdown(FOCUS_DURATION_MILLIS - 999))
    }

    @Test
    fun `rounds partial seconds up`() {
        assertEquals("29:59", formatCountdown(FOCUS_DURATION_MILLIS - 1_000))
        assertEquals("29:59", formatCountdown(FOCUS_DURATION_MILLIS - 1_999))
        assertEquals("01:00", formatCountdown(59_001))
    }

    @Test
    fun `last second reads 00 01`() {
        assertEquals("00:01", formatCountdown(1_000))
        assertEquals("00:01", formatCountdown(1))
    }
}
