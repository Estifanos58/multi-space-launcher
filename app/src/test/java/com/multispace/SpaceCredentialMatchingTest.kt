package com.multispace

import com.multispace.domain.model.Space
import com.multispace.platform.PinSecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceCredentialMatchingTest {

  @Test
  fun testPinHashingAndVerification() {
    val pin = "1234"
    val salt = PinSecurityManager.generateSalt()
    val hash = PinSecurityManager.hashPin(pin, salt)

    assertTrue("PIN should verify successfully with valid salt and hash", PinSecurityManager.verifyPin(pin, salt, hash))
    org.junit.Assert.assertFalse("Incorrect PIN should fail verification", PinSecurityManager.verifyPin("9999", salt, hash))
  }

  @Test
  fun testPatternHashingAndVerification() {
    val pattern = "PATTERN:3x3:0-1-2-5-8"
    val salt = PinSecurityManager.generateSalt()
    val hash = PinSecurityManager.hashPin(pattern, salt)

    assertTrue("Pattern should verify successfully with valid salt and hash", PinSecurityManager.verifyPin(pattern, salt, hash))
    org.junit.Assert.assertFalse("Incorrect pattern should fail verification", PinSecurityManager.verifyPin("PATTERN:3x3:0-1-2", salt, hash))
  }

  @Test
  fun testSpaceMatchingByCredential() {
    val saltPersonal = PinSecurityManager.generateSalt()
    val hashPersonal = PinSecurityManager.hashPin("1111", saltPersonal)
    val personalSpace = Space(
      id = "space_personal",
      name = "Personal",
      authPolicy = Space.AUTH_PIN,
      pinSalt = saltPersonal,
      pinHash = hashPersonal
    )

    val saltWork = PinSecurityManager.generateSalt()
    val hashWork = PinSecurityManager.hashPin("2222", saltWork)
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

    // Helper simulation of findSpaceMatchingCredential
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

    val matched1111 = findSpace("1111")
    assertNotNull(matched1111)
    assertEquals("Personal", matched1111?.name)

    val matched2222 = findSpace("2222")
    assertNotNull(matched2222)
    assertEquals("Work", matched2222?.name)

    val matchedPattern = findSpace("PATTERN:3x3:0-3-6-7-8")
    assertNotNull(matchedPattern)
    assertEquals("Secret", matchedPattern?.name)

    val invalidMatch = findSpace("9999")
    assertNull(invalidMatch)
  }
}
