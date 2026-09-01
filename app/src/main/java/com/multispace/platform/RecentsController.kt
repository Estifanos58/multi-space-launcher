package com.multispace.platform

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.multispace.diagnostics.AppLogger
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RecentsInvocationResult {
  SUCCESS,
  SERVICE_DISABLED,
  ACTION_UNAVAILABLE,
  ACTION_FAILED
}

/**
 * Controller managing the optional Native Recents Bridge.
 * Connects Compose UI to the MultiSpaceAccessibilityService without leaking Service references.
 */
object RecentsController {

  private var activeServiceRef: WeakReference<MultiSpaceAccessibilityService>? = null

  private val _isServiceActive = MutableStateFlow(false)
  val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

  fun registerService(service: MultiSpaceAccessibilityService) {
    activeServiceRef = WeakReference(service)
    _isServiceActive.value = true
    AppLogger.i(AppLogger.Category.LAUNCHER, "RecentsController -> Service registered and active")
  }

  fun unregisterService(service: MultiSpaceAccessibilityService) {
    if (activeServiceRef?.get() == service) {
      activeServiceRef = null
      _isServiceActive.value = false
      AppLogger.i(AppLogger.Category.LAUNCHER, "RecentsController -> Service unregistered")
    }
  }

  /**
   * Checks whether MultiSpaceAccessibilityService is currently enabled in Android system settings.
   */
  fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val activeInstance = activeServiceRef?.get()
    if (activeInstance != null) {
      _isServiceActive.value = true
      return true
    }

    val expectedComponentName = ComponentName(context, MultiSpaceAccessibilityService::class.java)
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    if (am != null) {
      val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
      val isEnabled = enabledServices.any { service ->
        val serviceInfo = service.resolveInfo?.serviceInfo
        serviceInfo?.packageName == expectedComponentName.packageName &&
          serviceInfo?.name == expectedComponentName.className
      }
      if (isEnabled) {
        _isServiceActive.value = true
        return true
      }
    }

    // Fallback: Inspect Settings.Secure ENABLED_ACCESSIBILITY_SERVICES
    return try {
      val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
      ) ?: ""
      val isMatched = enabledServicesSetting.contains(expectedComponentName.flattenToString()) ||
        enabledServicesSetting.contains("${context.packageName}/${MultiSpaceAccessibilityService::class.java.name}")
      _isServiceActive.value = isMatched
      isMatched
    } catch (e: Exception) {
      AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to check secure accessibility setting: ${e.message}")
      _isServiceActive.value = false
      false
    }
  }

  /**
   * Invokes Android OS native Recent Apps / Overview screen using GLOBAL_ACTION_RECENTS.
   * Returns a deterministic result enum.
   */
  fun invokeNativeRecents(context: Context): RecentsInvocationResult {
    val service = activeServiceRef?.get()
    if (service == null) {
      val isEnabled = isAccessibilityServiceEnabled(context)
      AppLogger.w(
        AppLogger.Category.LAUNCHER,
        "RecentsController.invokeNativeRecents: Service inactive (enabledInSettings=$isEnabled)"
      )
      return RecentsInvocationResult.SERVICE_DISABLED
    }

    // Check system actions list on Android 11+ (API 30+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val systemActions = service.systemActions
      val hasRecents = systemActions.any { it.id == AccessibilityService.GLOBAL_ACTION_RECENTS }
      AppLogger.d(
        AppLogger.Category.LAUNCHER,
        "RecentsController.invokeNativeRecents: SystemActions count=${systemActions.size}, hasRecents=$hasRecents"
      )
    }

    AppLogger.i(AppLogger.Category.LAUNCHER, "RecentsController -> Executing performGlobalAction(GLOBAL_ACTION_RECENTS)")
    val result = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    AppLogger.i(AppLogger.Category.LAUNCHER, "RecentsController -> Result = $result")

    return if (result) {
      RecentsInvocationResult.SUCCESS
    } else {
      RecentsInvocationResult.ACTION_FAILED
    }
  }

  /**
   * Creates an Intent to navigate the user directly to the Android System Accessibility Settings.
   */
  fun createAccessibilitySettingsIntent(): Intent {
    return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
  }
}
