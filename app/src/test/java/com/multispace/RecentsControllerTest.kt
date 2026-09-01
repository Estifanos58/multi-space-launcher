package com.multispace

import android.provider.Settings
import com.multispace.platform.RecentsController
import com.multispace.platform.RecentsInvocationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RecentsControllerTest {

  @Test
  fun testAccessibilitySettingsIntent() {
    val intent = RecentsController.createAccessibilitySettingsIntent()
    assertNotNull(intent)
    assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, intent.action)
  }

  @Test
  fun testDefaultServiceState() {
    val isActive = RecentsController.isServiceActive.value
    // Default should be false when no service is registered
    assertEquals(false, isActive)
  }
}
