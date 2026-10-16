package com.blueproof.session

import com.blueproof.data.db.entity.SessionEntity
import com.blueproof.data.db.entity.StudentEntity
import com.blueproof.util.Constants

class SessionValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val status: String,
        val reason: String?
    )

    fun validateCheckIn(
        session: SessionEntity,
        student: StudentEntity?,
        challenge: String,
        timestamp: Long,
        hmac: String,
        rssiMedian: Int,
        rssiSamples: List<Int>
    ): ValidationResult = validateCheckIn(
        session = session,
        student = student,
        challenge = challenge,
        timestamp = timestamp,
        hmac = hmac,
        rssiMedian = rssiMedian,
        rssiSampleCount = rssiSamples.size
    )

    fun validateCheckIn(
        session: SessionEntity,
        student: StudentEntity?,
        challenge: String,
        timestamp: Long,
        hmac: String,
        rssiMedian: Int,
        rssiSampleCount: Int,
        deviceId: String = ""
    ): ValidationResult {
        if (!isSessionActive(session)) {
            return ValidationResult(false, Constants.STATUS_REJECTED, "Session is not active (status: ${session.status})")
        }
        if (!isChallengeValid(session, challenge)) {
            return ValidationResult(false, Constants.STATUS_REJECTED, "Invalid or expired challenge")
        }
        if (student == null) {
            return ValidationResult(false, Constants.STATUS_REJECTED, "Student not enrolled")
        }
        if (!student.isActive) {
            return ValidationResult(false, Constants.STATUS_REJECTED, "Student account is deactivated")
        }
        // Anti-Proxy: Verify that submission comes from the student's registered physical device
        if (student.deviceId.isNotBlank() && deviceId.isNotBlank() && student.deviceId != deviceId) {
            return ValidationResult(
                false,
                Constants.STATUS_REJECTED,
                "Device mismatch: Student ID ${student.studentId} is registered to another physical device"
            )
        }
        if (!ChallengeGenerator.verifyHmac(challenge, student.studentId, timestamp, student.enrollmentKey, hmac)) {
            return ValidationResult(false, Constants.STATUS_REJECTED, "Invalid authentication (HMAC mismatch)")
        }
        if (!isRssiAcceptable(rssiMedian, session.rssiThreshold, rssiSampleCount)) {
            val reason = if (rssiSampleCount < Constants.MIN_RSSI_SAMPLES) {
                "Insufficient proximity samples ($rssiSampleCount < ${Constants.MIN_RSSI_SAMPLES})"
            } else {
                "Signal too weak (RSSI $rssiMedian dBm < threshold ${session.rssiThreshold} dBm)"
            }
            return ValidationResult(false, Constants.STATUS_REJECTED, reason)
        }
        return ValidationResult(true, Constants.STATUS_ACCEPTED, null)
    }

    fun isSessionActive(session: SessionEntity): Boolean {
        return session.status == Constants.SESSION_STATUS_ACTIVE
    }

    fun isChallengeValid(session: SessionEntity, challenge: String): Boolean {
        val currentChallenge = session.currentChallenge ?: return false
        val expiresAt = session.challengeExpiresAt ?: return false
        return currentChallenge == challenge && !ChallengeGenerator.isExpired(expiresAt)
    }

    fun isRssiAcceptable(rssiMedian: Int, threshold: Int, sampleCount: Int): Boolean {
        return sampleCount >= Constants.MIN_RSSI_SAMPLES && rssiMedian >= threshold
    }
}
