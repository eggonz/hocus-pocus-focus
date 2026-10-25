package com.example.hocuspocusfocus.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatCountdownTest {

    @Test
    fun `shows the full duration until a whole second has passed`() {
        assertEquals("30:00", formatCountdown(THIRTY_MINUTES))
        assertEquals("30:00", formatCountdown(THIRTY_MINUTES - 999))
    }

    @Test
    fun `rounds partial seconds up`() {
        assertEquals("29:59", formatCountdown(THIRTY_MINUTES - 1_000))
        assertEquals("29:59", formatCountdown(THIRTY_MINUTES - 1_999))
        assertEquals("01:00", formatCountdown(59_001))
    }

    @Test
    fun `last second reads 00 01`() {
        assertEquals("00:01", formatCountdown(1_000))
        assertEquals("00:01", formatCountdown(1))
    }

    @Test
    fun `shows hours from an hour up`() {
        assertEquals("1:00:00", formatCountdown(3_600_000))
        assertEquals("1:00:00", formatCountdown(3_599_001))
        assertEquals("59:59", formatCountdown(3_599_000))
        assertEquals("3:00:00", formatCountdown(3 * 3_600_000L))
        assertEquals("1:29:59", formatCountdown(90 * 60_000L - 1_000))
    }

    @Test
    fun `formats picker durations`() {
        assertEquals("5 min", formatDuration(5))
        assertEquals("1 h", formatDuration(60))
        assertEquals("1.5 h", formatDuration(90))
        assertEquals("3 h", formatDuration(180))
    }

    private companion object {
        const val THIRTY_MINUTES = 30 * 60_000L
    }
}
