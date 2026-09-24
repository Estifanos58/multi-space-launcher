package com.multispace

import com.multispace.domain.model.Space
import com.multispace.domain.security.PatternSecurityHelper
import com.multispace.domain.security.SensitiveOperation
import com.multispace.domain.security.SpaceAuthorizationManager
import com.multispace.platform.PinSecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceCredentialMatchingTest {

  @Test
  fun testPinHashingAndVerification() {
    val pin = "8392"
    val salt = PinSecurityManager.generateSalt()
    val hash = PinSecurityManager.hashPin(pin, salt)

    assertTrue("PIN should verify successfully with valid salt and hash", PinSecurityManager.verifyPin(pin, salt, hash))
    assertFalse("Incorrect PIN should fail verification", PinSecurityManager.verifyPin("9999", salt, hash))
  }

  @Test
  fun testKdfVersioningAndTransparentUpgrade() {
    val pin = "7418"
    val legacySalt = PinSecurityManager.generateSalt()
    // Create legacy V1 hash (120,000 iterations)
    val legacyHash = PinSecurityManager.hashPinV1(pin, legacySalt)
    assertTrue("Hash should contain v1 marker", legacyHash.startsWith("\$v1\$120000\$"))

    // Verify with upgrade check
    val upgradeResult = PinSecurityManager.verifyPinWithUpgradeCheck(pin, legacySalt, legacyHash)
    assertTrue("Legacy hash must verify as valid", upgradeResult.isValid)
    assertTrue("Legacy hash must flag needsUpgrade = true", upgradeResult.needsUpgrade)
    assertNotNull("Upgraded hash must be generated", upgradeResult.upgradedHash)
    assertNotNull("Upgraded salt must be generated", upgradeResult.upgradedSalt)

    val upgradedHash = upgradeResult.upgradedHash!!
    val upgradedSalt = upgradeResult.upgradedSalt!!
    assertTrue("Upgraded hash must be V2 with 250k iterations", upgradedHash.startsWith("\$v2\$250000\$"))

    // Verify upgraded hash
    val subsequentResult = PinSecurityManager.verifyPinWithUpgradeCheck(pin, upgradedSalt, upgradedHash)
    assertTrue("Upgraded hash must verify as valid", subsequentResult.isValid)
    assertFalse("V2 hash must NOT flag needsUpgrade", subsequentResult.needsUpgrade)
  }

  @Test
  fun testPinStrengthValidation() {
    // Too short or long
    assertFalse("PIN too short should fail", PinSecurityManager.validatePinStrength("12").isSuccess)
    assertFalse("PIN too long should fail", PinSecurityManager.validatePinStrength("123456789").isSuccess)

    // Repeated digits
    assertFalse("PIN with repeated digits should fail", PinSecurityManager.validatePinStrength("1111").isSuccess)
    assertFalse("PIN with repeated digits should fail", PinSecurityManager.validatePinStrength("000000").isSuccess)

    // Sequential digits
    assertFalse("Ascending sequential PIN should fail", PinSecurityManager.validatePinStrength("1234").isSuccess)
    assertFalse("Descending sequential PIN should fail", PinSecurityManager.validatePinStrength("9876").isSuccess)

    // Strong non-sequential PINs
    assertTrue("Dispersed PIN should pass", PinSecurityManager.validatePinStrength("7418").isSuccess)
    assertTrue("Dispersed PIN should pass", PinSecurityManager.validatePinStrength("839201").isSuccess)
  }

  @Test
  fun testPatternSecurityIntermediateNodes() {
    // Diagonal across 3x3: 0 (top-left) to 8 (bottom-right) passes through 4 (center)
    val intermediates0to8 = PatternSecurityHelper.getIntermediateNodes(0, 8, 3, 3, emptySet())
    assertEquals(listOf(4), intermediates0to8)

    // If 4 was already visited, moving 0 to 8 should not duplicate 4
    val intermediatesVisited = PatternSecurityHelper.getIntermediateNodes(0, 8, 3, 3, setOf(4))
    assertTrue("Already visited node 4 should not be returned as intermediate", intermediatesVisited.isEmpty())

    // Adjacent nodes have no intermediates
    val intermediatesAdjacent = PatternSecurityHelper.getIntermediateNodes(0, 1, 3, 3, emptySet())
    assertTrue("Adjacent nodes have no intermediate", intermediatesAdjacent.isEmpty())

    // Canonical encoding and validation
    val pattern = PatternSecurityHelper.encodePattern(3, 3, listOf(0, 1, 2, 5, 8))
    assertEquals("PATTERN:3x3:0-1-2-5-8", pattern)
    assertTrue("Pattern format should be valid", PatternSecurityHelper.isValidPatternFormat(pattern, 3, 3))

    val parsed = PatternSecurityHelper.parsePattern(pattern, 3, 3)
    assertNotNull(parsed)
    assertEquals(listOf(0, 1, 2, 5, 8), parsed)
  }

  @Test
  fun testSpaceAuthorizationManagerPolicies() {
    var isUnlocked = false
    var isLauncherLocked = false

    val authManager = SpaceAuthorizationManager(
      isSpaceUnlockedProvider = { isUnlocked },
      isLauncherLockedProvider = { isLauncherLocked }
    )

    val protectedSpace = Space(
      id = "protected_space",
      name = "Protected Space",
      authPolicy = Space.AUTH_PIN
    )

    val publicSpace = Space(
      id = "public_space",
      name = "Public Space",
      authPolicy = Space.AUTH_NONE
    )

    // 1. When space is locked
    isUnlocked = false
    assertFalse("Cannot edit locked space", authManager.canPerform(protectedSpace, SensitiveOperation.EDIT_SPACE))
    assertFalse("Cannot delete locked space", authManager.canPerform(protectedSpace, SensitiveOperation.DELETE_SPACE))
    assertTrue("Public space operations are allowed", authManager.canPerform(publicSpace, SensitiveOperation.EDIT_SPACE))

    // 2. When space is unlocked
    isUnlocked = true
    assertTrue("Can edit when space is unlocked", authManager.canPerform(protectedSpace, SensitiveOperation.EDIT_SPACE))
    assertTrue("Can manage memberships when space is unlocked", authManager.canPerform(protectedSpace, SensitiveOperation.MANAGE_MEMBERSHIPS))

    // 3. High-security operations require explicit credential re-authentication even if unlocked
    assertTrue("Deleting protected space requires explicit re-auth", authManager.requiresExplicitAuthentication(protectedSpace, SensitiveOperation.DELETE_SPACE))
    assertTrue("Changing auth policy requires explicit re-auth", authManager.requiresExplicitAuthentication(protectedSpace, SensitiveOperation.CHANGE_AUTHENTICATION))
    assertFalse("Unlocking space does not require explicit re-auth once unlocked", authManager.requiresExplicitAuthentication(protectedSpace, SensitiveOperation.UNLOCK_SPACE))
  }

  @Test
  fun testSpaceMatchingByCredential() {
    val saltPersonal = PinSecurityManager.generateSalt()
    val hashPersonal = PinSecurityManager.hashPin("7418", saltPersonal)
    val personalSpace = Space(
      id = "space_personal",
      name = "Personal",
      authPolicy = Space.AUTH_PIN,
      pinSalt = saltPersonal,
      pinHash = hashPersonal
    )

    val saltWork = PinSecurityManager.generateSalt()
    val hashWork = PinSecurityManager.hashPin("8392", saltWork)
    val workSpace = Space(
      id = "space_work",
      name = "Work",
      authPolicy = Space.AUTH_PIN,
      pinSalt = saltWork,
      pinHash = hashWork
    )

    val saltSecret = PinSecurityManager.generateSalt()
    val hashSecret = PinSecurityManager.hashPin("PATTERN:3x3:0-3-6-7-8", saltSecret)
    val secretSpace = Space(
      id = "space_secret",
      name = "Secret",
      authPolicy = Space.AUTH_PATTERN,
      pinSalt = saltSecret,
      pinHash = hashSecret
    )

    val allSpaces = listOf(personalSpace, workSpace, secretSpace)

    fun findSpace(credential: String): Space? {
      for (space in allSpaces) {
        val s = space.pinSalt
        val h = space.pinHash
        if (!s.isNullOrEmpty() && !h.isNullOrEmpty()) {
          if (PinSecurityManager.verifyPin(credential, s, h)) {
            return space
          }
        }
      }
      return null
    }

    val matched7418 = findSpace("7418")
    assertNotNull(matched7418)
    assertEquals("Personal", matched7418?.name)

    val matched8392 = findSpace("8392")
    assertNotNull(matched8392)
    assertEquals("Work", matched8392?.name)

    val matchedPattern = findSpace("PATTERN:3x3:0-3-6-7-8")
    assertNotNull(matchedPattern)
    assertEquals("Secret", matchedPattern?.name)

    val invalidMatch = findSpace("9999")
    assertNull(invalidMatch)
  }
}
