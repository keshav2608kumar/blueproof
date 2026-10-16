package com.blueproof.session

import com.blueproof.data.db.entity.SessionEntity
import com.blueproof.data.db.entity.StudentEntity
import com.blueproof.util.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class SessionValidatorTest {

    private lateinit var validator: SessionValidator

    @Before
    fun setUp() {
        validator = SessionValidator()
    }

    private fun createTestSession(
        sessionId: String = UUID.randomUUID().toString(),
        name: String = "CS101 Attendance",
        teacherName: String = "Dr. Smith",
        status: String = Constants.SESSION_STATUS_ACTIVE,
        rssiThreshold: Int = Constants.DEFAULT_RSSI_THRESHOLD,
        currentChallenge: String? = "a".repeat(64),
        challengeExpiresAt: Long? = System.currentTimeMillis() + Constants.CHALLENGE_VALIDITY_MS
    ): SessionEntity {
        return SessionEntity(
            sessionId = sessionId,
            name = name,
            teacherName = teacherName,
            status = status,
            rssiThreshold = rssiThreshold,
            currentChallenge = currentChallenge,
            challengeExpiresAt = challengeExpiresAt
        )
    }

    private fun createTestStudent(
        studentId: String = "STU1001",
        name: String = "Alice Smith",
        deviceId: String = "DEV-001",
        enrollmentKey: String = "b".repeat(64),
        isActive: Boolean = true
    ): StudentEntity {
        return StudentEntity(
            studentId = studentId,
            name = name,
            deviceId = deviceId,
            enrollmentKey = enrollmentKey,
            isActive = isActive
        )
    }

    @Test
    fun validCheckInIsAccepted() {
        val session = createTestSession()
        val student = createTestStudent()
        val timestamp = System.currentTimeMillis()
        val challenge = session.currentChallenge!!
        val hmac = ChallengeGenerator.computeHmac(
            challenge = challenge,
            studentId = student.studentId,
            timestamp = timestamp,
            key = student.enrollmentKey
        )
        val rssiSamples = listOf(-65, -67, -64, -68, -66)
        val rssiMedian = -66

        val result = validator.validateCheckIn(
            session = session,
            student = student,
            challenge = challenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = rssiMedian,
            rssiSamples = rssiSamples
        )

        assertTrue(result.isValid)
        assertEquals(Constants.STATUS_ACCEPTED, result.status)
        assertNull(result.reason)
    }

    @Test
    fun expiredSessionIsRejected() {
        val session = createTestSession(status = "EXPIRED")
        val student = createTestStudent()
        val timestamp = System.currentTimeMillis()
        val challenge = session.currentChallenge!!
        val hmac = ChallengeGenerator.computeHmac(challenge, student.studentId, timestamp, student.enrollmentKey)
        val rssiSamples = listOf(-65, -67, -64, -68, -66)

        val result = validator.validateCheckIn(
            session = session,
            student = student,
            challenge = challenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = -66,
            rssiSamples = rssiSamples
        )

        assertFalse(result.isValid)
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertNotNull(result.reason)
        assertFalse(validator.isSessionActive(session))
    }

    @Test
    fun endedSessionIsRejected() {
        val session = createTestSession(status = Constants.SESSION_STATUS_ENDED)
        val student = createTestStudent()
        val timestamp = System.currentTimeMillis()
        val challenge = session.currentChallenge!!
        val hmac = ChallengeGenerator.computeHmac(challenge, student.studentId, timestamp, student.enrollmentKey)
        val rssiSamples = listOf(-65, -67, -64, -68, -66)

        val result = validator.validateCheckIn(
            session = session,
            student = student,
            challenge = challenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = -66,
            rssiSamples = rssiSamples
        )

        assertFalse(result.isValid)
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertNotNull(result.reason)
        assertFalse(validator.isSessionActive(session))
    }

    @Test
    fun wrongChallengeIsRejected() {
        val session = createTestSession()
        val student = createTestStudent()
        val timestamp = System.currentTimeMillis()
        val submittedChallenge = "c".repeat(64)
        val hmac = ChallengeGenerator.computeHmac(submittedChallenge, student.studentId, timestamp, student.enrollmentKey)
        val rssiSamples = listOf(-65, -67, -64, -68, -66)

        val result = validator.validateCheckIn(
            session = session,
            student = student,
            challenge = submittedChallenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = -66,
            rssiSamples = rssiSamples
        )

        assertFalse(result.isValid)
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertNotNull(result.reason)
    }

    @Test
    fun expiredChallengeIsRejected() {
        val pastExpiry = System.currentTimeMillis() - 5000L
        val session = createTestSession(challengeExpiresAt = pastExpiry)
        val student = createTestStudent()
        val timestamp = System.currentTimeMillis()
        val challenge = session.currentChallenge!!
        val hmac = ChallengeGenerator.computeHmac(challenge, student.studentId, timestamp, student.enrollmentKey)
        val rssiSamples = listOf(-65, -67, -64, -68, -66)

        val result = validator.validateCheckIn(
            session = session,
            student = student,
            challenge = challenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = -66,
            rssiSamples = rssiSamples
        )

        assertFalse(result.isValid)
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertNotNull(result.reason)
        assertFalse(validator.isChallengeValid(session, challenge))
    }

    @Test
    fun invalidHmacIsRejected() {
        val session = createTestSession()
        val student = createTestStudent()
        val timestamp = System.currentTimeMillis()
        val challenge = session.currentChallenge!!
        val invalidHmac = "f".repeat(64)
        val rssiSamples = listOf(-65, -67, -64, -68, -66)

        val result = validator.validateCheckIn(
            session = session,
            student = student,
            challenge = challenge,
            timestamp = timestamp,
            hmac = invalidHmac,
            rssiMedian = -66,
            rssiSamples = rssiSamples
        )

        assertFalse(result.isValid)
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertNotNull(result.reason)
    }

    @Test
    fun weakRssiIsRejected() {
        val session = createTestSession(rssiThreshold = -75)
        val student = createTestStudent()
        val timestamp = System.currentTimeMillis()
        val challenge = session.currentChallenge!!
        val hmac = ChallengeGenerator.computeHmac(challenge, student.studentId, timestamp, student.enrollmentKey)
        val weakSamples = listOf(-85, -88, -82, -86, -84)
        val weakMedian = -85

        val result = validator.validateCheckIn(
            session = session,
            student = student,
            challenge = challenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = weakMedian,
            rssiSamples = weakSamples
        )

        assertFalse(result.isValid)
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertNotNull(result.reason)
        assertFalse(validator.isRssiAcceptable(weakMedian, session.rssiThreshold, weakSamples.size))
    }

    @Test
    fun insufficientRssiSamplesIsRejected() {
        val session = createTestSession()
        val student = createTestStudent()
        val timestamp = System.currentTimeMillis()
        val challenge = session.currentChallenge!!
        val hmac = ChallengeGenerator.computeHmac(challenge, student.studentId, timestamp, student.enrollmentKey)
        val fewSamples = listOf(-60, -62, -59) // 3 samples < MIN_RSSI_SAMPLES (5)

        val result = validator.validateCheckIn(
            session = session,
            student = student,
            challenge = challenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = -60,
            rssiSamples = fewSamples
        )

        assertFalse(result.isValid)
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertNotNull(result.reason)
        assertFalse(validator.isRssiAcceptable(-60, session.rssiThreshold, fewSamples.size))
    }

    @Test
    fun acceptableRssiIsAccepted() {
        val threshold = -75
        val samples = listOf(-70, -68, -72, -69, -71)
        val median = -70

        val acceptable = validator.isRssiAcceptable(median, threshold, samples.size)
        assertTrue(acceptable)

        val session = createTestSession(rssiThreshold = threshold)
        val student = createTestStudent()
        val timestamp = System.currentTimeMillis()
        val challenge = session.currentChallenge!!
        val hmac = ChallengeGenerator.computeHmac(challenge, student.studentId, timestamp, student.enrollmentKey)

        val result = validator.validateCheckIn(
            session = session,
            student = student,
            challenge = challenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = median,
            rssiSamples = samples
        )

        assertTrue(result.isValid)
        assertEquals(Constants.STATUS_ACCEPTED, result.status)
    }

    @Test
    fun deviceMismatchIsRejectedAsProxy() {
        val session = createTestSession()
        val student = createTestStudent(deviceId = "DEV-PHYSICAL-PHONE-A")
        val timestamp = System.currentTimeMillis()
        val challenge = session.currentChallenge!!
        val hmac = ChallengeGenerator.computeHmac(challenge, student.studentId, timestamp, student.enrollmentKey)
        val samples = listOf(-65, -67, -64, -68, -66)

        // Attempt submission from a different physical device
        val result = validator.validateCheckIn(
            session = session,
            student = student,
            challenge = challenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = -66,
            rssiSampleCount = samples.size,
            deviceId = "DEV-PHYSICAL-PHONE-B" // Different device!
        )

        assertFalse(result.isValid)
        assertEquals(Constants.STATUS_REJECTED, result.status)
        assertNotNull(result.reason)
        assertTrue(result.reason!!.contains("Device mismatch"))
    }

    @Test
    fun matchingDeviceIsAccepted() {
        val session = createTestSession()
        val student = createTestStudent(deviceId = "DEV-PHYSICAL-PHONE-A")
        val timestamp = System.currentTimeMillis()
        val challenge = session.currentChallenge!!
        val hmac = ChallengeGenerator.computeHmac(challenge, student.studentId, timestamp, student.enrollmentKey)
        val samples = listOf(-65, -67, -64, -68, -66)

        // Submission from the registered device
        val result = validator.validateCheckIn(
            session = session,
            student = student,
            challenge = challenge,
            timestamp = timestamp,
            hmac = hmac,
            rssiMedian = -66,
            rssiSampleCount = samples.size,
            deviceId = "DEV-PHYSICAL-PHONE-A"
        )

        assertTrue(result.isValid)
        assertEquals(Constants.STATUS_ACCEPTED, result.status)
        assertNull(result.reason)
    }
}
