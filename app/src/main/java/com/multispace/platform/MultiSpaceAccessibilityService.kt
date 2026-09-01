package com.multispace.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import com.multispace.diagnostics.AppLogger

/**
 * Minimal, zero-data AccessibilityService used exclusively to request
 * Android OS native Recent Apps / Overview screen via GLOBAL_ACTION_RECENTS.
 *
 * Privacy & Security Guarantees:
 * - Does NOT read screen contents or window hierarchy (canRetrieveWindowContent = false).
 * - Does NOT listen to or inspect AccessibilityEvents (eventTypes = 0).
 * - Does NOT log, capture, or transmit any user data.
 * - Single deterministic responsibility: performGlobalAction(GLOBAL_ACTION_RECENTS).
 * - NOT an accessibility tool (Play policy non-tool compliance).
 */
class MultiSpaceAccessibilityService : AccessibilityService() {

  override fun onServiceConnected() {
    super.onServiceConnected()
    AppLogger.i(AppLogger.Category.LAUNCHER, "MultiSpaceAccessibilityService -> onServiceConnected")
    RecentsController.registerService(this)

    // Ensure service configuration requests zero events and zero content inspection
    val info = AccessibilityServiceInfo().apply {
      eventTypes = 0 // Explicitly subscribe to zero events
      feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
      flags = 0
    }
    setServiceInfo(info)
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // Intentionally no-op: We do not process, inspect, or listen to events
  }

  override fun onInterrupt() {
    AppLogger.w(AppLogger.Category.LAUNCHER, "MultiSpaceAccessibilityService -> onInterrupt")
  }

  override fun onDestroy() {
    super.onDestroy()
    AppLogger.i(AppLogger.Category.LAUNCHER, "MultiSpaceAccessibilityService -> onDestroy")
    RecentsController.unregisterService(this)
  }
}
