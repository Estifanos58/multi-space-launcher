package com.multispace

import com.multispace.platform.RecentsController
import com.multispace.platform.RecentsInvocationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RecentsControllerTest {

  @Test
  fun testDefaultServiceState() {
    val isActive = RecentsController.isServiceActive.value
    // Default should be false when no service is registered
    assertFalse(isActive)
  }

  @Test
  fun testDiagnosticStateTracking() {
    val diagnostic = RecentsController.diagnosticInfo.value
    assertFalse(diagnostic.isServiceConnected)
    assertNull(diagnostic.lastResult)
    assertEquals(0, diagnostic.systemActionsCount)
  }
}
