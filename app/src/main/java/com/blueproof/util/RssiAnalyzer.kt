package com.blueproof.util

object RssiAnalyzer {

    /**
     * Sorts the sample RSSI list and returns the middle value.
     * Returns 0 if the list is empty.
     */
    fun medianRssi(samples: List<Int>): Int {
        if (samples.isEmpty()) return 0
        val sorted = samples.sorted()
        return sorted[sorted.size / 2]
    }

    /**
     * Determines whether the RSSI samples meet minimum sample count and median threshold requirements.
     */
    fun isAcceptable(
        samples: List<Int>,
        threshold: Int,
        minSamples: Int = Constants.MIN_RSSI_SAMPLES
    ): Boolean {
        if (samples.size < minSamples) return false
        return medianRssi(samples) >= threshold
    }

    /**
     * Returns qualitative signal strength description based on dBm RSSI.
     * > -50: "Excellent"
     * > -65: "Good"
     * > -75: "Fair"
     * > -85: "Weak"
     * <= -85: "Very Weak"
     */
    fun signalStrength(rssi: Int): String {
        return when {
            rssi > -50 -> "Excellent"
            rssi > -65 -> "Good"
            rssi > -75 -> "Fair"
            rssi > -85 -> "Weak"
            else -> "Very Weak"
        }
    }

    /**
     * Maps an RSSI range (-100 dBm to 0 dBm) to a normalized float between 0.0f and 1.0f clamped.
     */
    fun signalPercentage(rssi: Int): Float {
        val percentage = (rssi + 100).toFloat() / 100f
        return percentage.coerceIn(0f, 1f)
    }
}
