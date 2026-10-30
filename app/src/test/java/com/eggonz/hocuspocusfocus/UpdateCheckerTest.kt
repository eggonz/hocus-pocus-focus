package com.eggonz.hocuspocusfocus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun higherVersionsAreNewer() {
        assertTrue(isNewerVersion("0.5.2", "0.5.1"))
        assertTrue(isNewerVersion("0.6.0", "0.5.9"))
        assertTrue(isNewerVersion("1.0.0", "0.9.9"))
    }

    @Test
    fun partsCompareAsNumbers() {
        assertTrue(isNewerVersion("0.10.0", "0.9.0"))
    }

    @Test
    fun sameOrLowerVersionsAreNotNewer() {
        assertFalse(isNewerVersion("0.5.1", "0.5.1"))
        assertFalse(isNewerVersion("0.5.0", "0.5.1"))
    }

    @Test
    fun missingPartsCountAsZero() {
        assertFalse(isNewerVersion("0.5", "0.5.0"))
        assertTrue(isNewerVersion("0.5.1", "0.5"))
    }

    @Test
    fun nonNumericVersionsAreNotNewer() {
        assertFalse(isNewerVersion("0.6.0-beta", "0.5.1"))
        assertFalse(isNewerVersion("0.6.0", "dev"))
    }
}
