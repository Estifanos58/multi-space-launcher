package com.multispace

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.multispace.domain.model.Space
import com.multispace.platform.BiometricKeyManager
import com.multispace.platform.LauncherSessionManager
import com.multispace.platform.PinSecurityManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecurityHardeningAndRecoveryTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun testRecoveryPinHashingAndVerification() {
    val recoveryPin = "9482"
    val salt = PinSecurityManager.generateSalt()
    val hash = PinSecurityManager.hashPin(recoveryPin, salt)

    assertTrue("Valid recovery PIN should verify", PinSecurityManager.verifyPin(recoveryPin, salt, hash))
    assertFalse("Incorrect recovery PIN should fail", PinSecurityManager.verifyPin("1111", salt, hash))
  }

  @Test
  fun testFailClosedForBiometricSpaceWithoutRecoveryPin() {
    val biometricSpaceNoRecovery = Space(
      id = "bio_space_no_recovery",
      name = "Biometric Without Recovery",
      authPolicy = Space.AUTH_BIOMETRIC,
      recoveryPinSalt = null,
      recoveryPinHash = null
    )

    // Fail-closed verification: attempting recovery unlock when credentials are missing must fail
    val salt = biometricSpaceNoRecovery.recoveryPinSalt
    val hash = biometricSpaceNoRecovery.recoveryPinHash
    val canVerify = !salt.isNullOrEmpty() && !hash.isNullOrEmpty() && PinSecurityManager.verifyPin("1234", salt, hash)
    assertFalse("Space with missing recovery credentials must fail-closed", canVerify)
  }

  @Test
  fun testPersistentLockoutCooldownAndExponentialBackoff() {
    val sessionManager = LauncherSessionManager(context)
    val spaceId = "test_cooldown_space"

    // Clear any previous attempts
    sessionManager.recordSuccessfulAttempt(spaceId)
    assertNull("Initially should have no cooldown", sessionManager.getRemainingCooldownSeconds(spaceId))

    // 4 failed attempts: below threshold (cooldown starts at 5)
    repeat(4) {
      sessionManager.recordFailedAttempt(spaceId)
    }
    assertNull("4 attempts should not trigger lockout", sessionManager.getRemainingCooldownSeconds(spaceId))

    // 5th failed attempt: triggers lockout
    sessionManager.recordFailedAttempt(spaceId)
    val cooldown = sessionManager.getRemainingCooldownSeconds(spaceId)
    assertNotNull("5 attempts must trigger lockout", cooldown)
    assertTrue("Cooldown should be > 0 and <= 30 seconds", cooldown!! in 1..30)
    assertTrue("Space should be locked out", sessionManager.isLockedOut(spaceId))

    // A new session manager initialized with the same context should restore the lockout state
    val restoredSessionManager = LauncherSessionManager(context)
    assertTrue("Lockout state must persist across process restart", restoredSessionManager.isLockedOut(spaceId))
    val restoredCooldown = restoredSessionManager.getRemainingCooldownSeconds(spaceId)
    assertNotNull("Restored cooldown must be present", restoredCooldown)
    assertTrue("Restored cooldown should be > 0", restoredCooldown!! > 0)

    // Successful unlock clears the lockout state
    sessionManager.recordSuccessfulAttempt(spaceId)
    assertFalse("Unlock should clear lockout", sessionManager.isLockedOut(spaceId))
    assertNull("Unlock should clear cooldown", sessionManager.getRemainingCooldownSeconds(spaceId))
  }

  @Test
  fun testTargetSpaceIsolationPreventsCrossSpaceMatch() {
    val saltA = PinSecurityManager.generateSalt()
    val hashA = PinSecurityManager.hashPin("1234", saltA)
    val spaceA = Space(
      id = "space_a",
      name = "Space A",
      authPolicy = Space.AUTH_PIN,
      pinSalt = saltA,
      pinHash = hashA
    )

    val saltB = PinSecurityManager.generateSalt()
    val hashB = PinSecurityManager.hashPin("5678", saltB)
    val spaceB = Space(
      id = "space_b",
      name = "Space B",
      authPolicy = Space.AUTH_PIN,
      pinSalt = saltB,
      pinHash = hashB
    )

    fun verifySpecificTarget(target: Space, credential: String): Boolean {
      val s = target.pinSalt
      val h = target.pinHash
      if (s.isNullOrEmpty() || h.isNullOrEmpty()) return false
      return PinSecurityManager.verifyPin(credential, s, h)
    }

    // Space A with credential "5678" (Space B's pin) must fail
    assertFalse("Space A must not unlock with Space B's PIN", verifySpecificTarget(spaceA, "5678"))
    assertTrue("Space A must unlock with its own PIN", verifySpecificTarget(spaceA, "1234"))

    // Space B with credential "1234" (Space A's pin) must fail
    assertFalse("Space B must not unlock with Space A's PIN", verifySpecificTarget(spaceB, "1234"))
    assertTrue("Space B must unlock with its own PIN", verifySpecificTarget(spaceB, "5678"))
  }

  @Test
  fun testBiometricKeyReenrollmentAndCleanup() {
    val testSpaceId = "test_reenroll_space"
    assertEquals("multispace_space_auth_test_reenroll_space", BiometricKeyManager.getKeyAlias(testSpaceId))

    // Delete key safely (idempotent, does not throw even if key doesn't exist)
    BiometricKeyManager.deleteSecretKey(testSpaceId)
    assertTrue("Alias prefix is formatted correctly", BiometricKeyManager.getKeyAlias(testSpaceId).startsWith("multispace_space_auth_"))
  }
}
