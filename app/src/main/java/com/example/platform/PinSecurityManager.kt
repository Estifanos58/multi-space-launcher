package com.example.platform

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Handles cryptographic operations for local Space PIN security.
 *
 * Employs standard PBKDF2WithHmacSHA256 key derivation with per-Space cryptographically
 * secure random salts and constant-time equality checks.
 *
 * Plaintext PINs are NEVER stored or logged.
 */
object PinSecurityManager {

  private const val ITERATION_COUNT = 10_000
  private const val KEY_LENGTH_BITS = 256
  private const val SALT_LENGTH_BYTES = 16

  /**
   * Generates a cryptographically secure random salt encoded in Base64.
   */
  fun generateSalt(): String {
    val random = SecureRandom()
    val salt = ByteArray(SALT_LENGTH_BYTES)
    random.nextBytes(salt)
    return Base64.encodeToString(salt, Base64.NO_WRAP)
  }

  /**
   * Derives a cryptographic hash of the numeric PIN using PBKDF2WithHmacSHA256.
   *
   * @param pin Plaintext numeric PIN provided by the user.
   * @param saltBase64 Base64-encoded salt unique to this Space.
   * @return Base64-encoded derived hash.
   */
  fun hashPin(pin: String, saltBase64: String): String {
    require(pin.isNotBlank()) { "PIN cannot be blank" }
    require(saltBase64.isNotBlank()) { "Salt cannot be blank" }

    val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
    val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val hash = factory.generateSecret(spec).encoded
    return Base64.encodeToString(hash, Base64.NO_WRAP)
  }

  /**
   * Verifies an entered PIN against a stored salt and expected hash using constant-time comparison.
   */
  fun verifyPin(enteredPin: String, saltBase64: String?, expectedHashBase64: String?): Boolean {
    if (saltBase64.isNullOrEmpty() || expectedHashBase64.isNullOrEmpty()) {
      return false
    }
    return try {
      val computedHash = hashPin(enteredPin, saltBase64)
      val computedBytes = Base64.decode(computedHash, Base64.NO_WRAP)
      val expectedBytes = Base64.decode(expectedHashBase64, Base64.NO_WRAP)
      MessageDigest.isEqual(computedBytes, expectedBytes)
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Validates that the PIN conforms to the numeric PIN requirements (4 to 8 digits).
   */
  fun isValidPinFormat(pin: String): Boolean {
    return pin.length in 4..8 && pin.all { it.isDigit() }
  }
}
