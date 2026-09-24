package com.multispace.domain.security

import com.multispace.domain.model.Space

/**
 * Supported authentication methods for Spaces.
 */
enum class AuthenticationMethod {
  NONE,
  PIN,
  PATTERN,
  BIOMETRIC;

  companion object {
    fun fromAuthPolicy(policy: String): AuthenticationMethod {
      return when (policy.uppercase()) {
        Space.AUTH_PIN -> PIN
        Space.AUTH_PATTERN -> PATTERN
        Space.AUTH_BIOMETRIC -> BIOMETRIC
        else -> NONE
      }
    }
  }
}

/**
 * Explicit sensitive operations that require authorization.
 */
enum class SensitiveOperation {
  UNLOCK_SPACE,
  EDIT_SPACE,
  DELETE_SPACE,
  CHANGE_AUTHENTICATION,
  DISABLE_AUTHENTICATION,
  MANAGE_MEMBERSHIPS,
  CHANGE_SECURITY_SETTINGS
}

/**
 * Result of attempting space credential or biometric authentication.
 */
sealed interface AuthenticationResult {
  /**
   * Authentication succeeded.
   */
  data class Success(
    val spaceId: String,
    val method: AuthenticationMethod,
    val upgradedKdf: Boolean = false
  ) : AuthenticationResult

  /**
   * The provided credential (PIN, Pattern, Recovery PIN) was incorrect.
   */
  data class InvalidCredential(
    val spaceId: String,
    val attemptsRemaining: Int? = null,
    val cooldownSeconds: Long? = null
  ) : AuthenticationResult

  /**
   * Authentication is throttled due to multiple failed attempts.
   */
  data class TemporarilyLocked(
    val spaceId: String,
    val remainingSeconds: Long
  ) : AuthenticationResult

  /**
   * The space is not properly configured (e.g. missing hash/salt or corrupt pattern).
   * Fails closed to protect data.
   */
  data class NotConfigured(
    val spaceId: String,
    val reason: String
  ) : AuthenticationResult

  /**
   * The requested operation is not allowed.
   */
  data class NotAllowed(
    val reason: String
  ) : AuthenticationResult

  /**
   * Strong hardware biometrics are not available or not enrolled.
   */
  data class BiometricUnavailable(
    val reason: String
  ) : AuthenticationResult

  /**
   * The Keystore key for biometric authentication has been invalidated (e.g. new fingerprints enrolled).
   * Recovery PIN is required to unlock and re-enroll.
   */
  data class BiometricKeyInvalidated(
    val spaceId: String
  ) : AuthenticationResult

  /**
   * Unexpected error during authentication.
   */
  data class Failure(
    val message: String,
    val cause: Throwable? = null
  ) : AuthenticationResult
}

/**
 * Represents an authenticated session for a specific Space with monotonic expiration.
 */
data class AuthenticatedSession(
  val spaceId: String,
  val authenticatedAtElapsedMs: Long,
  val expiresAtElapsedMs: Long
) {
  fun isExpired(currentElapsedMs: Long): Boolean {
    return currentElapsedMs >= expiresAtElapsedMs
  }
}
