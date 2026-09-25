package com.multispace

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.multispace.data.database.LauncherDatabase
import com.multispace.data.preferences.LauncherPreferences
import com.multispace.data.repository.RoomSpaceRepository
import com.multispace.domain.model.Space
import com.multispace.domain.security.AuthenticationMethod
import com.multispace.domain.security.AuthenticationResult
import com.multispace.domain.security.AuthorizationStatus
import com.multispace.domain.security.SensitiveOperation
import com.multispace.domain.security.SpaceAuthorizationManager
import com.multispace.platform.BiometricKeyManager
import com.multispace.platform.LauncherSessionManager
import com.multispace.platform.PinSecurityManager
import com.multispace.presentation.SpaceViewModel
import java.security.KeyStoreException
import kotlinx.coroutines.runBlocking
import org.junit.After
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
    BiometricKeyManager.testKeyGenerator = null
  }

  @After
  fun tearDown() {
    BiometricKeyManager.testKeyGenerator = null
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
  fun testWeakPinRejectionThroughCreateFullSpaceAndUpdateFullSpace() = runBlocking {
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = LauncherPreferences(context),
      context = context,
      database = database
    )

    // 1. Weak PINs rejected through createFullSpace
    val weakPins = listOf("1234", "1111", "1212", "4321", "123456", "000000", "123123")
    for (weak in weakPins) {
      val res = repo.createFullSpace(
        name = "Weak Space $weak",
        authPolicy = Space.AUTH_PIN,
        pin = weak
      )
      assertTrue("createFullSpace must reject weak PIN: $weak", res.isFailure)
    }

    // 2. Weak Recovery PIN rejected through createFullSpace
    for (weak in listOf("1234", "1111", "4321", "1212")) {
      val res = repo.createFullSpace(
        name = "Weak Bio Space $weak",
        authPolicy = Space.AUTH_BIOMETRIC,
        recoveryPin = weak
      )
      assertTrue("createFullSpace must reject weak Recovery PIN: $weak", res.isFailure)
    }

    // 3. Pre-hashed credential bypass attempt must be rejected
    val bypassRes = repo.createFullSpace(
      name = "Prehashed Bypass",
      authPolicy = Space.AUTH_PIN,
      pin = null,
      pinHash = "\$v2\$250000\$fakesalt\$fakehash"
    )
    assertTrue("createFullSpace must reject pre-hashed credentials without raw PIN", bypassRes.isFailure)

    // 4. Create a valid space
    val validSpace = repo.createFullSpace(
      name = "Valid Space",
      authPolicy = Space.AUTH_PIN,
      pin = "8392"
    ).getOrThrow()

    // 5. Weak PINs rejected through updateFullSpace
    for (weak in listOf("1234", "1111", "1212", "4321")) {
      val updateRes = repo.updateFullSpace(
        spaceId = validSpace.id,
        name = "Updated Weak",
        authPolicy = Space.AUTH_PIN,
        pin = weak,
        currentCredential = "8392"
      )
      assertTrue("updateFullSpace must reject weak PIN: $weak", updateRes.isFailure)
    }

    // 6. Weak Recovery PIN rejected through updateFullSpace
    val updateBioRes = repo.updateFullSpace(
      spaceId = validSpace.id,
      name = "Updated Weak Bio",
      authPolicy = Space.AUTH_BIOMETRIC,
      recoveryPin = "1234",
      currentCredential = "8392"
    )
    assertTrue("updateFullSpace must reject weak Recovery PIN", updateBioRes.isFailure)

    database.close()
  }

  @Test
  fun testThrottlingOfPinChangeAndProtectionDisable() = runBlocking {
    val sessionManager = LauncherSessionManager(context)
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = LauncherPreferences(context),
      context = context,
      database = database,
      sessionManager = sessionManager
    )

    val space = repo.createFullSpace(
      name = "Throttling Target Space",
      authPolicy = Space.AUTH_PIN,
      pin = "8392"
    ).getOrThrow()

    sessionManager.recordSuccessfulAttempt(space.id)
    assertFalse("Initially not locked out", sessionManager.isLockedOut(space.id))

    // 1. Throttling of PIN change attempts
    repeat(5) {
      val res = repo.changeSpacePin(space.id, currentPin = "wrong_pin", newPin = "9482")
      assertTrue("Wrong current PIN must fail", res.isFailure)
    }
    assertTrue("5 failed PIN change attempts must trigger lockout", sessionManager.isLockedOut(space.id))

    val subsequentChange = repo.changeSpacePin(space.id, currentPin = "8392", newPin = "9482")
    assertTrue("Subsequent PIN change attempt while locked out must be blocked", subsequentChange.isFailure)

    // Clear lockout
    sessionManager.recordSuccessfulAttempt(space.id)
    assertFalse("Lockout cleared", sessionManager.isLockedOut(space.id))

    // 2. Throttling of protection-disable attempts
    repeat(5) {
      val res = repo.disableSpaceProtection(space.id, currentCredential = "wrong_pin")
      assertTrue("Wrong current credential must fail disable", res.isFailure)
    }
    assertTrue("5 failed disable attempts must trigger lockout", sessionManager.isLockedOut(space.id))

    val subsequentDisable = repo.disableSpaceProtection(space.id, currentCredential = "8392")
    assertTrue("Subsequent disable attempt while locked out must be blocked", subsequentDisable.isFailure)

    database.close()
  }

  @Test
  fun testRecoveryPinAndReenrollmentThrottling() = runBlocking {
    val sessionManager = LauncherSessionManager(context)
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = LauncherPreferences(context),
      context = context,
      database = database,
      sessionManager = sessionManager
    )
    val viewModel = SpaceViewModel(
      application = ApplicationProvider.getApplicationContext(),
      spaceRepository = repo,
      sessionManager = sessionManager
    )

    val recoveryPin = "7392"
    val space = repo.createFullSpace(
      name = "Bio Recovery Space",
      authPolicy = Space.AUTH_BIOMETRIC,
      recoveryPin = recoveryPin
    ).getOrThrow()

    sessionManager.recordSuccessfulAttempt(space.id)

    // 1. 5 failed Recovery PIN verification attempts
    repeat(5) {
      val res = viewModel.verifyRecoveryPinWithThrottling(space.id, "wrong_recovery", unlockOnSuccess = false)
      if (it == 4) {
        assertTrue("5th attempt triggers lockout", res is AuthenticationResult.TemporarilyLocked)
      }
    }
    assertTrue("Recovery PIN failures must trigger lockout", sessionManager.isLockedOut(space.id))

    val unlockRes = viewModel.authenticateAndUnlockWithRecoveryPin(space.id, recoveryPin)
    assertNull("Attempting recovery unlock during lockout must be blocked", unlockRes)

    // Clear lockout
    sessionManager.recordSuccessfulAttempt(space.id)
    assertFalse("Lockout cleared", sessionManager.isLockedOut(space.id))

    // 2. Re-enrollment throttling with incorrect Recovery PIN
    repeat(5) {
      val res = viewModel.reenrollBiometrics(space.id, "wrong_rec")
      assertTrue("Re-enrollment with incorrect recovery PIN must fail", res.isFailure)
    }
    assertTrue("5 failed re-enrollment attempts must trigger lockout", sessionManager.isLockedOut(space.id))

    val subsequentReenroll = viewModel.reenrollBiometrics(space.id, recoveryPin)
    assertTrue("Subsequent re-enrollment while locked out must fail", subsequentReenroll.isFailure)

    database.close()
  }

  @Test
  fun testAndroidKeystoreFailureBiometricUnlockRejected() = runBlocking {
    val sessionManager = LauncherSessionManager(context)
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = LauncherPreferences(context),
      context = context,
      database = database,
      sessionManager = sessionManager
    )
    val viewModel = SpaceViewModel(
      application = ApplicationProvider.getApplicationContext(),
      spaceRepository = repo,
      sessionManager = sessionManager
    )

    val bioSpace = repo.createFullSpace(
      name = "Keystore Test Space",
      authPolicy = Space.AUTH_BIOMETRIC,
      recoveryPin = "9482"
    ).getOrThrow()

    // 1. Simulate Keystore hardware failure via test hook
    BiometricKeyManager.testKeyGenerator = {
      Result.failure(KeyStoreException("Keystore hardware initialization failed"))
    }

    val keyResult = BiometricKeyManager.getOrCreateSecretKey(bioSpace.id)
    assertTrue("Key generation must return failure on Keystore error", keyResult.isFailure)

    val cryptoInitResult = BiometricKeyManager.createCryptoObject(bioSpace.id)
    assertTrue("CryptoObject initialization must return Error", cryptoInitResult is BiometricKeyManager.CryptoInitResult.Error)

    // 2. Unlock attempt when Keystore fails must be rejected (fail closed)
    val unlockResult = viewModel.authenticateAndUnlockWithBiometric(bioSpace.id, cryptoObject = null)
    assertNull("Biometric unlock must fail-closed and return null", unlockResult)
    assertFalse("Space must remain locked", sessionManager.isSpaceUnlocked(bioSpace.id))

    database.close()
  }

  @Test
  fun testProtectedOperationsRequireExplicitAuthentication() = runBlocking {
    val sessionManager = LauncherSessionManager(context)
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = LauncherPreferences(context),
      context = context,
      database = database,
      sessionManager = sessionManager
    )

    // Ensure there is at least one other space so delete is permitted
    repo.ensureDefaultSpaceInitialized(emptyList())

    val space = repo.createFullSpace(
      name = "Protected Change Space",
      authPolicy = Space.AUTH_PIN,
      pin = "3819"
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

    // 4. Explicit authorization cannot be granted without successful authentication
    val invalidAuth = AuthenticationResult.InvalidCredential(space.id)
    val grantFailed = sessionManager.grantExplicitAuthorization(space.id, invalidAuth)
    assertFalse("grantExplicitAuthorization must reject InvalidCredential", grantFailed)
    assertFalse("Must not have explicit authorization", sessionManager.hasExplicitAuthorization(space.id))

    // 5. Granting with valid auth works and is single-use
    val validAuth = AuthenticationResult.Success(space.id, AuthenticationMethod.PIN)
    val grantSuccess = sessionManager.grantExplicitAuthorization(space.id, validAuth)
    assertTrue("grantExplicitAuthorization must accept Success", grantSuccess)
    assertTrue("Must have explicit authorization", sessionManager.hasExplicitAuthorization(space.id))

    // Consumed upon use
    val consumed = sessionManager.consumeExplicitAuthorization(space.id)
    assertTrue("consumeExplicitAuthorization must return true", consumed)
    assertFalse("After consumption, authorization must be gone", sessionManager.hasExplicitAuthorization(space.id))

    database.close()
  }

  @Test
  fun testMissingCredentialsFailClosed() = runBlocking {
    val database = androidx.room.Room.inMemoryDatabaseBuilder(context, LauncherDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = RoomSpaceRepository(
      spaceDao = database.spaceDao(),
      membershipDao = database.spaceMembershipDao(),
      layoutDao = database.spaceLayoutDao(),
      preferences = LauncherPreferences(context),
      context = context,
      database = database
    )

    // 1. Attempting to create a biometric space without recovery PIN must fail
    val createBioRes = repo.createFullSpace(
      name = "Missing Recovery Bio",
      authPolicy = Space.AUTH_BIOMETRIC,
      recoveryPin = null
    )
    assertTrue("Creating biometric space without recovery credentials must be rejected", createBioRes.isFailure)

    // 2. Attempting to create a PIN space without PIN must fail
    val createPinRes = repo.createFullSpace(
      name = "Missing PIN",
      authPolicy = Space.AUTH_PIN,
      pin = null
    )
    assertTrue("Creating PIN space without PIN must be rejected", createPinRes.isFailure)

    // 3. Space domain model fail-closed: missing recovery credentials make isSecurityConfigured false
    val unconfiguredBioSpace = Space(
      id = "corrupted_bio",
      name = "Corrupted Bio",
      authPolicy = Space.AUTH_BIOMETRIC,
      recoveryPinSalt = null,
      recoveryPinHash = null
    )
    assertFalse("Biometric space without recovery credentials is not configured", unconfiguredBioSpace.isSecurityConfigured)

    val authManager = SpaceAuthorizationManager(
      isSpaceUnlockedProvider = { true },
      isLauncherLockedProvider = { false }
    )
    val authResult = authManager.authorize(unconfiguredBioSpace, SensitiveOperation.EDIT_SPACE)
    assertFalse("Corrupted space must be denied authorization", authResult.isAuthorized)
    assertEquals(AuthorizationStatus.DENIED_NOT_CONFIGURED, authResult.status)

    database.close()
  }

  @Test
  fun testPersistentLockoutCooldownAndExponentialBackoff() {
    val sessionManager = LauncherSessionManager(context)
    val spaceId = "test_cooldown_space"

    sessionManager.recordSuccessfulAttempt(spaceId)
    assertNull("Initially should have no cooldown", sessionManager.getRemainingCooldownSeconds(spaceId))

    repeat(4) {
      sessionManager.recordFailedAttempt(spaceId)
    }
    assertNull("4 attempts should not trigger lockout", sessionManager.getRemainingCooldownSeconds(spaceId))

    sessionManager.recordFailedAttempt(spaceId)
    val cooldown = sessionManager.getRemainingCooldownSeconds(spaceId)
    assertNotNull("5 attempts must trigger lockout", cooldown)
    assertTrue("Cooldown should be > 0 and <= 30 seconds", cooldown!! in 1..30)
    assertTrue("Space should be locked out", sessionManager.isLockedOut(spaceId))

    val restoredSessionManager = LauncherSessionManager(context)
    assertTrue("Lockout state must persist across process restart", restoredSessionManager.isLockedOut(spaceId))
    val restoredCooldown = restoredSessionManager.getRemainingCooldownSeconds(spaceId)
    assertNotNull("Restored cooldown must be present", restoredCooldown)
    assertTrue("Restored cooldown should be > 0", restoredCooldown!! > 0)

    sessionManager.recordSuccessfulAttempt(spaceId)
    assertFalse("Unlock should clear lockout", sessionManager.isLockedOut(spaceId))
    assertNull("Unlock should clear cooldown", sessionManager.getRemainingCooldownSeconds(spaceId))
  }
}
