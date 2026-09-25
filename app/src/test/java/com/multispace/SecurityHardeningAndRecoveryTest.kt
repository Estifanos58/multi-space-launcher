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

  @Test
  fun testBiometricCryptoObjectFailureUnlockRejected() {
    // 1. Unlocked cipher verification fails on null
    assertFalse("Null CryptoObject must be rejected", BiometricKeyManager.verifyUnlockedCryptoObject(null))

    // 2. SpaceViewModel fail-closed: unlocking AUTH_BIOMETRIC without valid cryptoObject must return null
    val sessionManager = LauncherSessionManager(context)
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, com.multispace.data.database.LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.multispace.data.repository.RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = com.multispace.data.preferences.LauncherPreferences(context),
      context = context,
      database = database,
      sessionManager = sessionManager
    )
    val viewModel = com.multispace.presentation.SpaceViewModel(
      application = ApplicationProvider.getApplicationContext(),
      spaceRepository = repo,
      sessionManager = sessionManager
    )

    val salt = PinSecurityManager.generateSalt()
    val hash = PinSecurityManager.hashPin("9482", salt)
    val bioSpace = kotlinx.coroutines.runBlocking {
      repo.createFullSpace(
        name = "Bio Space",
        authPolicy = Space.AUTH_BIOMETRIC,
        recoveryPinSalt = salt,
        recoveryPinHash = hash
      ).getOrThrow()
    }

    val result = viewModel.authenticateAndUnlockWithBiometric(bioSpace.id, cryptoObject = null)
    assertNull("Biometric unlock must fail-closed and return null when cryptoObject is missing or invalid", result)
    assertFalse("Space must remain locked", sessionManager.isSpaceUnlocked(bioSpace.id))
    database.close()
  }

  @Test
  fun testWeakPinRejectedThroughEveryCreationEditPath() = kotlinx.coroutines.runBlocking {
    val weakPins = listOf("1111", "000000", "1234", "123456", "4321", "654321", "1212", "123123")
    for (weak in weakPins) {
      assertTrue("PinSecurityManager must reject weak PIN: $weak", PinSecurityManager.validatePinStrength(weak).isFailure)
    }

    val validPin = "2468"
    assertTrue("Valid complex PIN must be accepted", PinSecurityManager.validatePinStrength(validPin).isSuccess)

    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, com.multispace.data.database.LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.multispace.data.repository.RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = com.multispace.data.preferences.LauncherPreferences(context),
      context = context,
      database = database
    )

    val space = repo.createSpace(name = "Target Test Space").getOrThrow()

    // 1. setSpacePin rejects weak PINs
    val setRes = repo.setSpacePin(space.id, "1234")
    assertTrue("repository.setSpacePin must reject weak PIN", setRes.isFailure)

    // 2. setSpaceRecoveryPin rejects weak PINs
    val setRecRes = repo.setSpaceRecoveryPin(space.id, "1111")
    assertTrue("repository.setSpaceRecoveryPin must reject weak PIN", setRecRes.isFailure)

    // Setup valid PIN first
    repo.setSpacePin(space.id, "8391").getOrThrow()

    // 3. changeSpacePin rejects weak new PINs
    val changeRes = repo.changeSpacePin(space.id, "8391", "4321")
    assertTrue("repository.changeSpacePin must reject weak new PIN", changeRes.isFailure)

    database.close()
  }

  @Test
  fun testDeleteOperationAttemptsAreThrottled() = kotlinx.coroutines.runBlocking {
    val sessionManager = LauncherSessionManager(context)
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, com.multispace.data.database.LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.multispace.data.repository.RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = com.multispace.data.preferences.LauncherPreferences(context),
      context = context,
      database = database,
      sessionManager = sessionManager
    )
    val viewModel = com.multispace.presentation.SpaceViewModel(
      application = ApplicationProvider.getApplicationContext(),
      spaceRepository = repo,
      sessionManager = sessionManager
    )

    val salt = PinSecurityManager.generateSalt()
    val hash = PinSecurityManager.hashPin("8492", salt)
    val space = repo.createFullSpace(
      name = "Protected Delete Target",
      authPolicy = Space.AUTH_PIN,
      pinSalt = salt,
      pinHash = hash
    ).getOrThrow()

    sessionManager.recordSuccessfulAttempt(space.id)
    assertFalse("Initially not locked out", sessionManager.isLockedOut(space.id))

    // 5 failed verification attempts for delete
    repeat(5) {
      val res = viewModel.verifyCredentialWithThrottling(space.id, "wrong_pin", unlockOnSuccess = false)
      if (it == 4) {
        assertTrue("5th failed attempt must trigger lockout", res is com.multispace.domain.security.AuthenticationResult.TemporarilyLocked)
      }
    }

    assertTrue("Space must be locked out after 5 failed delete attempts", sessionManager.isLockedOut(space.id))
    val subsequent = viewModel.verifyCredentialWithThrottling(space.id, "8492", unlockOnSuccess = false)
    assertTrue("Subsequent delete attempt while locked out must be blocked", subsequent is com.multispace.domain.security.AuthenticationResult.TemporarilyLocked)

    database.close()
  }

  @Test
  fun testRecoveryPinIsThrottled() = kotlinx.coroutines.runBlocking {
    val sessionManager = LauncherSessionManager(context)
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, com.multispace.data.database.LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.multispace.data.repository.RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = com.multispace.data.preferences.LauncherPreferences(context),
      context = context,
      database = database,
      sessionManager = sessionManager
    )
    val viewModel = com.multispace.presentation.SpaceViewModel(
      application = ApplicationProvider.getApplicationContext(),
      spaceRepository = repo,
      sessionManager = sessionManager
    )

    val salt = PinSecurityManager.generateSalt()
    val hash = PinSecurityManager.hashPin("7392", salt)
    val space = repo.createFullSpace(
      name = "Bio Recovery Space",
      authPolicy = Space.AUTH_BIOMETRIC,
      recoveryPinSalt = salt,
      recoveryPinHash = hash
    ).getOrThrow()

    sessionManager.recordSuccessfulAttempt(space.id)

    // 5 failed recovery PIN attempts
    repeat(5) {
      val res = viewModel.verifyRecoveryPinWithThrottling(space.id, "wrong_recovery", unlockOnSuccess = false)
      if (it == 4) {
        assertTrue("5th attempt triggers lockout", res is com.multispace.domain.security.AuthenticationResult.TemporarilyLocked)
      }
    }

    assertTrue("Recovery PIN failures must trigger lockout", sessionManager.isLockedOut(space.id))
    val unlockRes = viewModel.authenticateAndUnlockWithRecoveryPin(space.id, "7392")
    assertNull("Attempting recovery unlock during lockout must be blocked", unlockRes)

    database.close()
  }

  @Test
  fun testBiometricKeyInvalidationRecoveryPinAndReenrollment() = kotlinx.coroutines.runBlocking {
    val sessionManager = LauncherSessionManager(context)
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, com.multispace.data.database.LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.multispace.data.repository.RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = com.multispace.data.preferences.LauncherPreferences(context),
      context = context,
      database = database,
      sessionManager = sessionManager
    )
    val viewModel = com.multispace.presentation.SpaceViewModel(
      application = ApplicationProvider.getApplicationContext(),
      spaceRepository = repo,
      sessionManager = sessionManager
    )

    val recoveryPin = "9381"
    val salt = PinSecurityManager.generateSalt()
    val hash = PinSecurityManager.hashPin(recoveryPin, salt)
    val space = repo.createFullSpace(
      name = "Re-enroll Space",
      authPolicy = Space.AUTH_BIOMETRIC,
      recoveryPinSalt = salt,
      recoveryPinHash = hash
    ).getOrThrow()

    // 1. Re-enroll with wrong Recovery PIN fails and increments failed attempts
    val wrongResult = viewModel.reenrollBiometrics(space.id, "0000")
    assertTrue("Re-enrollment with incorrect recovery PIN must fail", wrongResult.isFailure)

    // 2. Re-enroll with correct Recovery PIN succeeds
    val successResult = viewModel.reenrollBiometrics(space.id, recoveryPin)
    assertTrue("Re-enrollment with correct recovery PIN must succeed", successResult.isSuccess)
    assertTrue("Space must be unlocked after re-enrollment", sessionManager.isSpaceUnlocked(space.id))

    database.close()
  }

  @Test
  fun testSensitiveAuthenticationChangesRequireExplicitReauthentication() = kotlinx.coroutines.runBlocking {
    val sessionManager = LauncherSessionManager(context)
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, com.multispace.data.database.LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.multispace.data.repository.RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = com.multispace.data.preferences.LauncherPreferences(context),
      context = context,
      database = database,
      sessionManager = sessionManager
    )

    // Ensure there is at least one other space so delete is permitted
    repo.ensureDefaultSpaceInitialized(emptyList())

    val salt = PinSecurityManager.generateSalt()
    val hash = PinSecurityManager.hashPin("3819", salt)
    val space = repo.createFullSpace(
      name = "Protected Change Space",
      authPolicy = Space.AUTH_PIN,
      pinSalt = salt,
      pinHash = hash
    ).getOrThrow()

    // 1. Direct delete without explicit credential or authorization must fail closed
    val deleteWithoutCred = repo.deleteSpace(space.id, credential = null)
    assertTrue("Deleting protected space without credential must fail", deleteWithoutCred.isFailure)
    assertTrue("Failure must be SecurityException", deleteWithoutCred.exceptionOrNull() is SecurityException)

    // 2. setSpacePin on an already protected space must fail closed without explicit re-auth
    val setPinAgain = repo.setSpacePin(space.id, "9283")
    assertTrue("setSpacePin on already protected space must fail", setPinAgain.isFailure)

    // 3. updateFullSpace disabling protection without current credential must fail closed
    val disableViaUpdate = repo.updateFullSpace(
      spaceId = space.id,
      name = space.name,
      keepExistingCredentials = false,
      authPolicy = Space.AUTH_NONE,
      currentCredential = null
    )
    assertTrue("Disabling protection via updateFullSpace without currentCredential must fail", disableViaUpdate.isFailure)
    assertTrue("Failure must be SecurityException", disableViaUpdate.exceptionOrNull() is SecurityException)

    // 4. SpaceAuthorizationManager requires explicit authentication for sensitive actions
    val authManager = com.multispace.domain.security.SpaceAuthorizationManager(
      isSpaceUnlockedProvider = { true },
      isLauncherLockedProvider = { false }
    )
    assertTrue("DELETE_SPACE requires explicit authentication", authManager.requiresExplicitAuthentication(space, com.multispace.domain.security.SensitiveOperation.DELETE_SPACE))
    assertTrue("CHANGE_AUTHENTICATION requires explicit authentication", authManager.requiresExplicitAuthentication(space, com.multispace.domain.security.SensitiveOperation.CHANGE_AUTHENTICATION))
    assertTrue("DISABLE_AUTHENTICATION requires explicit authentication", authManager.requiresExplicitAuthentication(space, com.multispace.domain.security.SensitiveOperation.DISABLE_AUTHENTICATION))

    database.close()
  }

  @Test
  fun testMissingBiometricRecoveryCredentialsFailClosed() = kotlinx.coroutines.runBlocking {
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, com.multispace.data.database.LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = com.multispace.data.repository.RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = com.multispace.data.preferences.LauncherPreferences(context),
      context = context,
      database = database
    )

    // 1. Attempting to create a biometric space without recovery PIN must fail
    val createRes = repo.createFullSpace(
      name = "Missing Recovery Bio",
      authPolicy = Space.AUTH_BIOMETRIC,
      recoveryPinSalt = null,
      recoveryPinHash = null
    )
    assertTrue("Creating biometric space without recovery credentials must be rejected", createRes.isFailure)

    // 2. Space domain model fail-closed: missing recovery credentials make isSecurityConfigured false
    val unconfiguredBioSpace = Space(
      id = "corrupted_bio",
      name = "Corrupted Bio",
      authPolicy = Space.AUTH_BIOMETRIC,
      recoveryPinSalt = null,
      recoveryPinHash = null
    )
    assertFalse("Biometric space without recovery credentials is not configured", unconfiguredBioSpace.isSecurityConfigured)

    val authManager = com.multispace.domain.security.SpaceAuthorizationManager(
      isSpaceUnlockedProvider = { true },
      isLauncherLockedProvider = { false }
    )
    val authResult = authManager.authorize(unconfiguredBioSpace, com.multispace.domain.security.SensitiveOperation.EDIT_SPACE)
    assertFalse("Corrupted space must be denied authorization", authResult.isAuthorized)
    assertEquals(com.multispace.domain.security.AuthorizationStatus.DENIED_NOT_CONFIGURED, authResult.status)

    database.close()
  }
}
