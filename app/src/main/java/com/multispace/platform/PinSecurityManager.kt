package com.multispace.platform

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Handles cryptographic operations for local Space PIN & Pattern security.
 *
 * Employs standard PBKDF2WithHmacSHA256 key derivation with per-Space cryptographically
 * secure random salts, constant-time equality checks, versioned KDF parameters,
 * and transparent upgrade pathways.
 *
 * Plaintext PINs and Patterns are NEVER stored or logged.
 */
object PinSecurityManager {

  const val ITERATIONS_V1 = 120_000
  const val ITERATIONS_V2 = 250_000
  private const val KEY_LENGTH_BITS = 256
  private const val SALT_LENGTH_BYTES = 16

  private const val VERSION_PREFIX_V1 = "\$v1\$"
  private const val VERSION_PREFIX_V2 = "\$v2\$"

  /**
   * Generates a cryptographically secure random salt encoded in Base64.
   */
  fun generateSalt(): String {
    val random = SecureRandom()
    val salt = ByteArray(SALT_LENGTH_BYTES)
    random.nextBytes(salt)
    return Base64.getEncoder().encodeToString(salt)
  }

  /**
   * Derives a cryptographic hash of the credential using PBKDF2WithHmacSHA256 with V2 parameters.
   * Encodes the result with version metadata: "$v2$250000$<saltBase64>$<rawHashBase64>"
   */
  fun hashPin(pin: String, saltBase64: String): String {
    return hashPinWithIterations(pin, saltBase64, ITERATIONS_V2, VERSION_PREFIX_V2)
  }

  fun hashPinV1(pin: String, saltBase64: String): String {
    return hashPinWithIterations(pin, saltBase64, ITERATIONS_V1, VERSION_PREFIX_V1)
  }

  /**
   * Derives a hash with a specific iteration count and version prefix.
   */
  fun hashPinWithIterations(
    pin: String,
    saltBase64: String,
    iterations: Int,
    versionPrefix: String = VERSION_PREFIX_V2
  ): String {
    require(pin.isNotBlank()) { "Credential cannot be blank" }
    require(saltBase64.isNotBlank()) { "Salt cannot be blank" }

    val salt = Base64.getDecoder().decode(saltBase64)
    val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val hash = factory.generateSecret(spec).encoded
    val rawHashBase64 = Base64.getEncoder().encodeToString(hash)
    return "$versionPrefix$iterations\$$saltBase64\$$rawHashBase64"
  }

  /**
   * Derives legacy raw hash without prefix (for backwards compatibility / unit test matching).
   */
  fun hashPinRaw(pin: String, saltBase64: String, iterations: Int = ITERATIONS_V1): String {
    require(pin.isNotBlank()) { "Credential cannot be blank" }
    require(saltBase64.isNotBlank()) { "Salt cannot be blank" }

    val salt = Base64.getDecoder().decode(saltBase64)
    val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val hash = factory.generateSecret(spec).encoded
    return Base64.getEncoder().encodeToString(hash)
  }

  /**
   * Checks whether the stored hash is from an older KDF version or uses legacy parameters
   * and requires transparent upgrading upon successful authentication.
   */
  fun isUpgradeNeeded(storedHash: String?): Boolean {
    if (storedHash.isNullOrEmpty()) return false
    return !storedHash.startsWith(VERSION_PREFIX_V2)
  }

  data class VerificationResult(
    val isValid: Boolean,
    val needsUpgrade: Boolean,
    val upgradedHash: String? = null,
    val upgradedSalt: String? = null
  )

  /**
   * Verifies an entered credential against stored salt and expected hash.
   * Supports both legacy unadorned hashes (V1: 120,000 iterations) and versioned hashes ($v1$, $v2$).
   */
  fun verifyPin(enteredPin: String, saltBase64: String?, expectedHash: String?): Boolean {
    val result = verifyPinWithUpgradeCheck(enteredPin, saltBase64, expectedHash)
    return result.isValid
  }

  /**
   * Verifies an entered credential and determines if a transparent KDF upgrade to V2 should occur.
   */
  fun verifyPinWithUpgradeCheck(
    enteredPin: String,
    saltBase64: String?,
    expectedHash: String?
  ): VerificationResult {
    if (saltBase64.isNullOrEmpty() || expectedHash.isNullOrEmpty() || enteredPin.isEmpty()) {
      return VerificationResult(isValid = false, needsUpgrade = false)
    }

    return try {
      when {
        // Version 2: $v2$<iterations>$<salt>$<rawHash>
        expectedHash.startsWith(VERSION_PREFIX_V2) -> {
          val parts = expectedHash.split("$")
          // parts[0] is "", parts[1] is "v2", parts[2] is iterations, parts[3] is salt, parts[4] is hash
          if (parts.size < 5) return VerificationResult(isValid = false, needsUpgrade = false)
          val iterations = parts[2].toIntOrNull() ?: ITERATIONS_V2
          val salt = parts[3]
          val expectedRaw = parts[4]
          val computedRaw = hashPinRaw(enteredPin, salt, iterations)
          val isValid = constantTimeEquals(computedRaw, expectedRaw)
          VerificationResult(isValid = isValid, needsUpgrade = false)
        }

        // Version 1: $v1$<iterations>$<salt>$<rawHash>
        expectedHash.startsWith(VERSION_PREFIX_V1) -> {
          val parts = expectedHash.split("$")
          if (parts.size < 5) return VerificationResult(isValid = false, needsUpgrade = false)
          val iterations = parts[2].toIntOrNull() ?: ITERATIONS_V1
          val salt = parts[3]
          val expectedRaw = parts[4]
          val computedRaw = hashPinRaw(enteredPin, salt, iterations)
          val isValid = constantTimeEquals(computedRaw, expectedRaw)
          if (isValid) {
            val newSalt = generateSalt()
            val newHash = hashPin(enteredPin, newSalt)
            VerificationResult(isValid = true, needsUpgrade = true, upgradedHash = newHash, upgradedSalt = newSalt)
          } else {
            VerificationResult(isValid = false, needsUpgrade = false)
          }
        }

        // Legacy unadorned Base64 hash (V1 120,000 iterations)
        else -> {
          val computedRaw = hashPinRaw(enteredPin, saltBase64, ITERATIONS_V1)
          val isValid = constantTimeEquals(computedRaw, expectedHash)
          if (isValid) {
            val newSalt = generateSalt()
            val newHash = hashPin(enteredPin, newSalt)
            VerificationResult(isValid = true, needsUpgrade = true, upgradedHash = newHash, upgradedSalt = newSalt)
          } else {
            VerificationResult(isValid = false, needsUpgrade = false)
          }
        }
      }
    } catch (e: Exception) {
      VerificationResult(isValid = false, needsUpgrade = false)
    }
  }

  private fun constantTimeEquals(a: String, b: String): Boolean {
    return try {
      val aBytes = Base64.getDecoder().decode(a)
      val bBytes = Base64.getDecoder().decode(b)
      MessageDigest.isEqual(aBytes, bBytes)
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Validates that the PIN conforms to basic length and numeric requirements (4 to 8 digits).
   */
  fun isValidPinFormat(pin: String): Boolean {
    return pin.length in 4..8 && pin.all { it.isDigit() }
  }

  /**
   * Validates PIN strength against weak, predictable, and trivial patterns.
   * Rejects:
   * - Identical digits (e.g. 1111, 000000)
   * - Purely sequential ascending (e.g. 1234, 123456) or descending (e.g. 4321, 654321)
   * - Simple repeating sequences (e.g. 1212, 123123)
   */
  fun validatePinStrength(pin: String): Result<Unit> {
    if (pin.length < 4) {
      return Result.failure(IllegalArgumentException("PIN must be at least 4 digits."))
    }
    if (pin.length > 8) {
      return Result.failure(IllegalArgumentException("PIN cannot exceed 8 digits."))
    }
    if (!pin.all { it.isDigit() }) {
      return Result.failure(IllegalArgumentException("PIN must contain only numeric digits."))
    }

    // Check all identical digits
    if (pin.all { it == pin[0] }) {
      return Result.failure(IllegalArgumentException("PIN cannot consist of all identical digits (e.g. '${pin.take(4)}')."))
    }

    // Check sequential ascending
    var isAscending = true
    for (i in 0 until pin.length - 1) {
      if (pin[i + 1] - pin[i] != 1) {
        isAscending = false
        break
      }
    }
    if (isAscending) {
      return Result.failure(IllegalArgumentException("PIN cannot be a sequential series (e.g. '1234')."))
    }

    // Check sequential descending
    var isDescending = true
    for (i in 0 until pin.length - 1) {
      if (pin[i] - pin[i + 1] != 1) {
        isDescending = false
        break
      }
    }
    if (isDescending) {
      return Result.failure(IllegalArgumentException("PIN cannot be a descending sequence (e.g. '4321')."))
    }

    // Check 2-digit repeated pattern (e.g. 1212, 121212)
    if (pin.length % 2 == 0) {
      val pair = pin.take(2)
      val repeated = pair.repeat(pin.length / 2)
      if (pin == repeated) {
        return Result.failure(IllegalArgumentException("PIN cannot be a repeating pattern (e.g. '$pair$pair')."))
      }
    }

    // Check 3-digit repeated pattern (e.g. 123123)
    if (pin.length % 3 == 0) {
      val triplet = pin.take(3)
      val repeated = triplet.repeat(pin.length / 3)
      if (pin == repeated) {
        return Result.failure(IllegalArgumentException("PIN cannot be a repeating sequence (e.g. '$triplet$triplet')."))
      }
    }

    return Result.success(Unit)
  }

  /**
   * Validates pattern format.
   */
  fun isValidPatternFormat(pattern: String): Boolean {
    return pattern.isNotBlank() && (pattern.startsWith("PATTERN:") || pattern.contains("-") || pattern.contains(",") || pattern.length >= 3)
  }

  /**
   * Generic verification for credentials (PIN or Pattern).
   */
  fun verifyCredential(enteredCredential: String, saltBase64: String?, expectedHash: String?): Boolean {
    return verifyPin(enteredCredential, saltBase64, expectedHash)
  }
}
