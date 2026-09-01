package com.multispace.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.multispace.diagnostics.AppLogger

/**
 * Minimal, zero-data AccessibilityService used exclusively to request
 * Android OS native Recent Apps / Overview screen via GLOBAL_ACTION_RECENTS.
 *
 * Privacy & Security Guarantees:
 * - Does NOT read screen contents or window hierarchy (canRetrieveWindowContent = false).
 * - Does NOT listen to or inspect AccessibilityEvents (eventTypes ignored).
 * - Does NOT log, capture, or transmit any user data.
 * - Single deterministic responsibility: performGlobalAction(GLOBAL_ACTION_RECENTS).
 * - NOT an accessibility tool (Play policy non-tool compliance).
 */
class MultiSpaceAccessibilityService : AccessibilityService() {

  override fun onServiceConnected() {
    super.onServiceConnected()
    AppLogger.i(AppLogger.Category.RECENTS, "onServiceConnected -> AccessibilityService connected and bound")
    RecentsController.registerService(this)

    // Inspect available system actions on Android 11+ (API 30+)
    var hasRecents = false
    var actionCount = 0
    val actionNames = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val actions = systemActions
      actionCount = actions.size
      hasRecents = actions.any { it.id == GLOBAL_ACTION_RECENTS }
      actions.forEach { action ->
        actionNames.add("ID=${action.id}: ${action.label ?: "Action"}")
      }
      AppLogger.i(
        AppLogger.Category.RECENTS,
        "RECENTS_SYSTEM_ACTIONS: count=$actionCount recentsAvailable=$hasRecents actions=[${actionNames.joinToString("; ")}]"
      )
    }

    RecentsController.updateSystemActionInfo(
      hasRecents = hasRecents,
      actionCount = actionCount,
      actionNames = actionNames
    )

    AppLogger.i(
      AppLogger.Category.RECENTS,
      "RECENTS_SERVICE_STATE: enabled=true bound=true serviceInstance=available"
    )
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // Intentionally no-op: zero event processing
  }

  override fun onInterrupt() {
    AppLogger.w(AppLogger.Category.RECENTS, "RECENTS_SERVICE_STATE: onInterrupt called")
  }

  override fun onDestroy() {
    super.onDestroy()
    AppLogger.i(AppLogger.Category.RECENTS, "onDestroy -> RECENTS_SERVICE_STATE: service destroyed, serviceInstance=unavailable")
    RecentsController.unregisterService(this)
  }
}

