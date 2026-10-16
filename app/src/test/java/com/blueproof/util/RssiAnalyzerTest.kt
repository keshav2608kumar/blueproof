package com.blueproof.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RssiAnalyzerTest {

    @Test
    fun medianOfOddSamples() {
        val oddSamples = listOf(-75, -60, -90, -55, -70)
        // Sorted: [-90, -75, -70, -60, -55], middle (index 2) is -70
        val median = RssiAnalyzer.medianRssi(oddSamples)
        assertEquals(-70, median)
    }

    @Test
    fun medianOfEvenSamples() {
        val evenSamples = listOf(-80, -65, -70, -60)
        // Sorted: [-80, -70, -65, -60], middle (index 2) is -65
        val median = RssiAnalyzer.medianRssi(evenSamples)
        assertEquals(-65, median)
    }

    @Test
    fun acceptableWhenAboveThreshold() {
        val samples = listOf(-68, -65, -62, -64, -66)
        val threshold = -75
        val acceptable = RssiAnalyzer.isAcceptable(samples, threshold, minSamples = 5)
        assertTrue(acceptable)
    }

    @Test
    fun rejectedWhenBelowThreshold() {
        val samples = listOf(-82, -85, -80, -79, -88)
        val threshold = -75
        val acceptable = RssiAnalyzer.isAcceptable(samples, threshold, minSamples = 5)
        assertFalse(acceptable)
    }

    @Test
    fun rejectedWhenTooFewSamples() {
        val samples = listOf(-60, -62, -61)
        val threshold = -75
        val acceptable = RssiAnalyzer.isAcceptable(samples, threshold, minSamples = 5)
        assertFalse(acceptable)
    }

    @Test
    fun signalStrengthCategories() {
        assertEquals("Excellent", RssiAnalyzer.signalStrength(-45))
        assertEquals("Good", RssiAnalyzer.signalStrength(-60))
        assertEquals("Fair", RssiAnalyzer.signalStrength(-70))
        assertEquals("Weak", RssiAnalyzer.signalStrength(-80))
        assertEquals("Very Weak", RssiAnalyzer.signalStrength(-95))
    }

    @Test
    fun signalPercentageBounds() {
        assertEquals(0.0f, RssiAnalyzer.signalPercentage(-100), 0.001f)
        assertEquals(1.0f, RssiAnalyzer.signalPercentage(0), 0.001f)
        assertEquals(0.5f, RssiAnalyzer.signalPercentage(-50), 0.001f)
        assertEquals(0.0f, RssiAnalyzer.signalPercentage(-120), 0.001f)
        assertEquals(1.0f, RssiAnalyzer.signalPercentage(10), 0.001f)
    }
}
