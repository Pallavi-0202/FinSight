package com.example.finsight.auth

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Utility for hashing and verifying user passwords securely using PBKDF2WithHmacSHA256.
 */
object PasswordHasher {

    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256

    /**
     * Generates a random 16-byte salt represented as a Hex string.
     */
    fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.toHexString()
    }

    /**
     * Hashes the plain text password with the given salt Hex string.
     */
    fun hashPassword(password: String, saltHex: String): String {
        val saltBytes = saltHex.hexToByteArray()
        val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        return hashBytes.toHexString()
    }

    /**
     * Verifies if the candidate password matches the expected hash using constant-time comparison.
     */
    fun verifyPassword(password: String, saltHex: String, expectedHashHex: String): Boolean {
        val candidateHashHex = hashPassword(password, saltHex)
        return MessageDigest.isEqual(
            candidateHashHex.toByteArray(Charsets.UTF_8),
            expectedHashHex.toByteArray(Charsets.UTF_8)
        )
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }

    private fun String.hexToByteArray(): ByteArray {
        val result = ByteArray(length / 2)
        for (i in result.indices) {
            val index = i * 2
            result[i] = substring(index, index + 2).toInt(16).toByte()
        }
        return result
    }
}
