package com.blueproof.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChallengeGeneratorTest {

    @Test
    fun challengeIs64HexChars() {
        val challenge = ChallengeGenerator.generateChallenge()
        assertEquals(64, challenge.length)
        assertTrue(challenge.matches(Regex("^[0-9a-fA-F]{64}$")))
    }

    @Test
    fun challengesAreUnique() {
        val challenges = (1..100).map { ChallengeGenerator.generateChallenge() }.toSet()
        assertEquals(100, challenges.size)
    }

    @Test
    fun sessionIdIsValidUuid() {
        val sessionId = ChallengeGenerator.generateSessionId()
        val parsedUuid = UUID.fromString(sessionId)
        assertNotNull(parsedUuid)
        assertEquals(sessionId, parsedUuid.toString())
    }

    @Test
    fun enrollmentKeyIs64HexChars() {
        val key = ChallengeGenerator.generateEnrollmentKey()
        assertEquals(64, key.length)
        assertTrue(key.matches(Regex("^[0-9a-fA-F]{64}$")))
    }

    @Test
    fun hmacIsConsistent() {
        val challenge = ChallengeGenerator.generateChallenge()
        val key = ChallengeGenerator.generateEnrollmentKey()
        val studentId = "STU101"
        val timestamp = System.currentTimeMillis()

        val hmac1 = ChallengeGenerator.computeHmac(challenge, studentId, timestamp, key)
        val hmac2 = ChallengeGenerator.computeHmac(challenge, studentId, timestamp, key)

        assertEquals(hmac1, hmac2)
        assertEquals(64, hmac1.length)
    }

    @Test
    fun hmacDiffersWithDifferentKey() {
        val challenge = ChallengeGenerator.generateChallenge()
        val key1 = ChallengeGenerator.generateEnrollmentKey()
        val key2 = ChallengeGenerator.generateEnrollmentKey()
        val studentId = "STU101"
        val timestamp = System.currentTimeMillis()

        val hmac1 = ChallengeGenerator.computeHmac(challenge, studentId, timestamp, key1)
        val hmac2 = ChallengeGenerator.computeHmac(challenge, studentId, timestamp, key2)

        assertNotEquals(hmac1, hmac2)
    }

    @Test
    fun verifyHmacReturnsTrueForValid() {
        val challenge = ChallengeGenerator.generateChallenge()
        val key = ChallengeGenerator.generateEnrollmentKey()
        val studentId = "STU101"
        val timestamp = System.currentTimeMillis()

        val hmac = ChallengeGenerator.computeHmac(challenge, studentId, timestamp, key)
        val isValid = ChallengeGenerator.verifyHmac(challenge, studentId, timestamp, key, hmac)

        assertTrue(isValid)
    }

    @Test
    fun verifyHmacReturnsFalseForInvalid() {
        val challenge = ChallengeGenerator.generateChallenge()
        val key = ChallengeGenerator.generateEnrollmentKey()
        val studentId = "STU101"
        val timestamp = System.currentTimeMillis()

        val bogusHmac = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        val isValid = ChallengeGenerator.verifyHmac(challenge, studentId, timestamp, key, bogusHmac)

        assertFalse(isValid)
    }

    @Test
    fun isExpiredReturnsTrueForPastTime() {
        val pastTimestamp = System.currentTimeMillis() - 5000L
        assertTrue(ChallengeGenerator.isExpired(pastTimestamp))
    }

    @Test
    fun isExpiredReturnsFalseForFutureTime() {
        val futureTimestamp = System.currentTimeMillis() + 60_000L
        assertFalse(ChallengeGenerator.isExpired(futureTimestamp))
    }
}
