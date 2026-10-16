package com.blueproof.util

import com.blueproof.data.db.entity.SessionEntity

object AttendanceCalculator {

    /**
     * Calculates the attendance percentage as a float between 0.0 and 100.0.
     * Safely handles total <= 0 by returning 0.0f.
     */
    fun attendancePercentage(accepted: Int, total: Int): Float {
        if (total <= 0) return 0f
        return (accepted.toFloat() / total.toFloat()) * 100f
    }

    /**
     * Calculates the attendance rate percentage based on a list of status strings.
     */
    fun attendanceRate(records: List<String>): Float {
        if (records.isEmpty()) return 0f
        val acceptedCount = records.count { it.equals(Constants.STATUS_ACCEPTED, ignoreCase = true) }
        return attendancePercentage(acceptedCount, records.size)
    }

    /**
     * Formats duration between start and end timestamps into a human readable string.
     * e.g. "45 min" or "1h 23m"
     */
    fun formatDuration(startTime: Long, endTime: Long?): String {
        val end = endTime ?: System.currentTimeMillis()
        val durationMs = (end - startTime).coerceAtLeast(0L)
        val totalMinutes = durationMs / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (hours > 0) {
            if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
        } else {
            "$minutes min"
        }
    }

    /**
     * Returns human-readable status for a session.
     */
    fun sessionStatus(
        startTime: Long,
        endTime: Long?,
        durationMinutes: Int,
        status: String
    ): String {
        if (status.equals(Constants.SESSION_STATUS_ENDED, ignoreCase = true)) {
            return "Ended"
        }
        if (status.equals(Constants.SESSION_STATUS_PAUSED, ignoreCase = true)) {
            return "Paused"
        }
        val now = endTime ?: System.currentTimeMillis()
        val sessionExpiryTime = startTime + (durationMinutes * 60 * 1000L)
        if (now > sessionExpiryTime) {
            return "Expired"
        }
        return "Active"
    }

    /**
     * Overload for SessionEntity.
     */
    fun sessionStatus(session: SessionEntity): String {
        return sessionStatus(
            startTime = session.startTime,
            endTime = session.endTime,
            durationMinutes = session.durationMinutes,
            status = session.status
        )
    }
}
