package com.blueproof.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceCalculatorTest {

    @Test
    fun percentageWithZeroTotal() {
        val result = AttendanceCalculator.attendancePercentage(accepted = 0, total = 0)
        assertEquals(0f, result, 0.001f)

        val negativeResult = AttendanceCalculator.attendancePercentage(accepted = 5, total = -1)
        assertEquals(0f, negativeResult, 0.001f)
    }

    @Test
    fun percentageNormal() {
        val result1 = AttendanceCalculator.attendancePercentage(accepted = 18, total = 20)
        assertEquals(90.0f, result1, 0.001f)

        val result2 = AttendanceCalculator.attendancePercentage(accepted = 25, total = 50)
        assertEquals(50.0f, result2, 0.001f)
    }

    @Test
    fun attendanceRateCalculation() {
        val records = listOf(
            Constants.STATUS_ACCEPTED,
            Constants.STATUS_ACCEPTED,
            Constants.STATUS_REJECTED,
            Constants.STATUS_ACCEPTED,
            Constants.STATUS_PENDING_REVIEW
        )
        // 3 accepted out of 5 = 60.0%
        val rate = AttendanceCalculator.attendanceRate(records)
        assertEquals(60.0f, rate, 0.001f)

        val emptyRate = AttendanceCalculator.attendanceRate(emptyList())
        assertEquals(0.0f, emptyRate, 0.001f)
    }

    @Test
    fun formatDurationMinutes() {
        val startTime = 1_000_000L
        val endTime = startTime + (45 * 60 * 1000L) // 45 minutes
        val formatted = AttendanceCalculator.formatDuration(startTime, endTime)
        assertEquals("45 min", formatted)
    }

    @Test
    fun formatDurationHours() {
        val startTime = 1_000_000L
        val endTime = startTime + (83 * 60 * 1000L) // 1h 23m
        val formatted = AttendanceCalculator.formatDuration(startTime, endTime)
        assertEquals("1h 23m", formatted)

        val roundHourEndTime = startTime + (120 * 60 * 1000L) // 2h 0m -> "2h"
        val formattedRoundHour = AttendanceCalculator.formatDuration(startTime, roundHourEndTime)
        assertEquals("2h", formattedRoundHour)
    }

    @Test
    fun sessionStatusTest() {
        val startTime = 1000L
        val statusEnded = AttendanceCalculator.sessionStatus(
            startTime = startTime,
            endTime = 5000L,
            durationMinutes = 15,
            status = Constants.SESSION_STATUS_ENDED
        )
        assertEquals("Ended", statusEnded)

        val statusPaused = AttendanceCalculator.sessionStatus(
            startTime = startTime,
            endTime = 5000L,
            durationMinutes = 15,
            status = Constants.SESSION_STATUS_PAUSED
        )
        assertEquals("Paused", statusPaused)

        val statusActive = AttendanceCalculator.sessionStatus(
            startTime = System.currentTimeMillis() - 60_000L,
            endTime = null,
            durationMinutes = 15,
            status = Constants.SESSION_STATUS_ACTIVE
        )
        assertEquals("Active", statusActive)
    }
}
