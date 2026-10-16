package com.blueproof.session

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object ChallengeGenerator {

    private val secureRandom = SecureRandom()

    fun generateChallenge(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytesToHex(bytes)
    }

    fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }

    fun generateEnrollmentKey(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return bytesToHex(bytes)
    }

    fun isExpired(expiresAt: Long): Boolean {
        return System.currentTimeMillis() > expiresAt
    }

    fun computeHmac(
        challenge: String,
        studentId: String,
        timestamp: Long,
        key: String
    ): String {
        val keyBytes = hexToBytes(key)
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(keyBytes, "HmacSHA256")
        mac.init(secretKeySpec)

        val message = "$challenge:$studentId:$timestamp"
        val hmacBytes = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        return bytesToHex(hmacBytes)
    }

    fun verifyHmac(
        challenge: String,
        studentId: String,
        timestamp: Long,
        key: String,
        hmac: String
    ): Boolean {
        return try {
            val expectedHmac = computeHmac(challenge, studentId, timestamp, key)
            MessageDigest.isEqual(
                expectedHmac.lowercase().toByteArray(Charsets.UTF_8),
                hmac.lowercase().toByteArray(Charsets.UTF_8)
            )
        } catch (e: Exception) {
            false
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun hexToBytes(hex: String): ByteArray {
        val cleanHex = hex.trim()
        if (cleanHex.length % 2 != 0) {
            return cleanHex.toByteArray(Charsets.UTF_8)
        }
        return try {
            val result = ByteArray(cleanHex.length / 2)
            for (i in result.indices) {
                val index = i * 2
                result[i] = cleanHex.substring(index, index + 2).toInt(16).toByte()
            }
            result
        } catch (e: Exception) {
            cleanHex.toByteArray(Charsets.UTF_8)
        }
    }
}
