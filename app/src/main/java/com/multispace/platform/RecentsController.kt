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
import kotlinx.coroutines.flow.update

enum class RecentsInvocationResult {
  SUCCESS,
  SERVICE_DISABLED,
  ACTION_UNAVAILABLE,
  ACTION_FAILED
}

data class RecentsDiagnosticInfo(
  val isEnabledInSettings: Boolean = false,
  val isServiceConnected: Boolean = false,
  val systemActionsCount: Int = 0,
  val isRecentsActionAvailable: Boolean? = null,
  val lastAttemptTimestamp: Long? = null,
  val lastResult: Boolean? = null,
  val lastInvocationResult: RecentsInvocationResult? = null,
  val lastFailureReason: String? = null,
  val availableActionNames: List<String> = emptyList(),
  val lastInvokedSource: String? = null
)

/**
 * Controller managing the optional Native Recents Bridge.
 * Connects Compose UI to the MultiSpaceAccessibilityService without leaking Service references.
 */
object RecentsController {

  private var activeServiceRef: WeakReference<MultiSpaceAccessibilityService>? = null

  private val _isServiceActive = MutableStateFlow(false)
  val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

  private val _diagnosticInfo = MutableStateFlow(RecentsDiagnosticInfo())
  val diagnosticInfo: StateFlow<RecentsDiagnosticInfo> = _diagnosticInfo.asStateFlow()

  fun registerService(service: MultiSpaceAccessibilityService) {
    activeServiceRef = WeakReference(service)
    _isServiceActive.value = true
    _diagnosticInfo.update {
      it.copy(isServiceConnected = true)
    }
    AppLogger.i(
      AppLogger.Category.RECENTS,
      "RECENTS_SERVICE_STATE: enabled=true bound=true serviceInstance=available"
    )
  }

  fun unregisterService(service: MultiSpaceAccessibilityService) {
    if (activeServiceRef?.get() == service) {
      activeServiceRef = null
      _isServiceActive.value = false
      _diagnosticInfo.update {
        it.copy(isServiceConnected = false)
      }
      AppLogger.i(
        AppLogger.Category.RECENTS,
        "RECENTS_SERVICE_STATE: enabled=unknown bound=false serviceInstance=unavailable"
      )
    }
  }

  fun updateSystemActionInfo(hasRecents: Boolean, actionCount: Int, actionNames: List<String>) {
    _diagnosticInfo.update {
      it.copy(
        isRecentsActionAvailable = hasRecents,
        systemActionsCount = actionCount,
        availableActionNames = actionNames
      )
    }
  }

  /**
   * Checks whether MultiSpaceAccessibilityService is currently enabled in Android system settings.
   */
  fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val activeInstance = activeServiceRef?.get()
    val isInstancePresent = activeInstance != null
    if (isInstancePresent) {
      _isServiceActive.value = true
      _diagnosticInfo.update { it.copy(isEnabledInSettings = true, isServiceConnected = true) }
      return true
    }

    val expectedComponentName = ComponentName(context, MultiSpaceAccessibilityService::class.java)
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    var isEnabled = false

    if (am != null) {
      val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
      isEnabled = enabledServices.any { service ->
        val serviceInfo = service.resolveInfo?.serviceInfo
        serviceInfo?.packageName == expectedComponentName.packageName &&
          serviceInfo?.name == expectedComponentName.className
      }
    }

    // Fallback check: Inspect Settings.Secure ENABLED_ACCESSIBILITY_SERVICES
    if (!isEnabled) {
      try {
        val enabledServicesSetting = Settings.Secure.getString(
          context.contentResolver,
          Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        isEnabled = enabledServicesSetting.contains(expectedComponentName.flattenToString()) ||
          enabledServicesSetting.contains("${context.packageName}/${MultiSpaceAccessibilityService::class.java.name}")
      } catch (e: Exception) {
        AppLogger.w(AppLogger.Category.RECENTS, "Failed to check secure accessibility setting: ${e.message}")
      }
    }

    _isServiceActive.value = isInstancePresent
    _diagnosticInfo.update {
      it.copy(
        isEnabledInSettings = isEnabled,
        isServiceConnected = isInstancePresent
      )
    }
    return isEnabled
  }

  /**
   * Invokes Android OS native Recent Apps / Overview screen using GLOBAL_ACTION_RECENTS.
   * Logs complete trace: RECENTS_UI_ACTION, RECENTS_SERVICE_STATE, RECENTS_SYSTEM_ACTIONS, RECENTS_GLOBAL_ACTION.
   */
  fun invokeNativeRecents(context: Context, source: String = "UI_BUTTON"): RecentsInvocationResult {
    val timestamp = System.currentTimeMillis()

    // 1. Log UI Action
    AppLogger.i(AppLogger.Category.RECENTS, "RECENTS_UI_ACTION: source=$source timestamp=$timestamp")

    // 2. Check and log Service State
    val service = activeServiceRef?.get()
    val isEnabledInSettings = isAccessibilityServiceEnabled(context)
    val isBound = service != null
    val serviceStatusStr = if (isBound) "available" else "unavailable"

    AppLogger.i(
      AppLogger.Category.RECENTS,
      "RECENTS_SERVICE_STATE: enabled=$isEnabledInSettings bound=$isBound serviceInstance=$serviceStatusStr"
    )

    if (service == null) {
      val failureReason = if (isEnabledInSettings) {
        "Service enabled in settings but not bound/connected by OS"
      } else {
        "Service is disabled in Android settings"
      }
      _diagnosticInfo.update {
        it.copy(
          isEnabledInSettings = isEnabledInSettings,
          isServiceConnected = false,
          lastAttemptTimestamp = timestamp,
          lastResult = false,
          lastInvocationResult = RecentsInvocationResult.SERVICE_DISABLED,
          lastFailureReason = failureReason,
          lastInvokedSource = source
        )
      }
      AppLogger.w(
        AppLogger.Category.RECENTS,
        "invokeNativeRecents aborted: $failureReason"
      )
      return RecentsInvocationResult.SERVICE_DISABLED
    }

    // 3. Inspect and log System Actions
    var hasRecents = true
    var systemActionCount = 0
    val actionNames = mutableListOf<String>()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      val systemActions = service.systemActions
      systemActionCount = systemActions.size
      hasRecents = systemActions.any { it.id == AccessibilityService.GLOBAL_ACTION_RECENTS }
      systemActions.forEach { action ->
        actionNames.add("ID=${action.id}: ${action.label ?: "Action"}")
      }
      AppLogger.i(
        AppLogger.Category.RECENTS,
        "RECENTS_SYSTEM_ACTIONS: count=$systemActionCount recentsAvailable=$hasRecents actions=[${actionNames.joinToString("; ")}]"
      )
    } else {
      AppLogger.i(
        AppLogger.Category.RECENTS,
        "RECENTS_SYSTEM_ACTIONS: getSystemActions requires API 30+ (current SDK=${Build.VERSION.SDK_INT})"
      )
    }

    // 4. Execute performGlobalAction(GLOBAL_ACTION_RECENTS)
    AppLogger.i(
      AppLogger.Category.RECENTS,
      "RECENTS_GLOBAL_ACTION: Calling performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)..."
    )
    val result = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    AppLogger.i(
      AppLogger.Category.RECENTS,
      "RECENTS_GLOBAL_ACTION: result=$result"
    )

    val invocationResult = if (result) {
      RecentsInvocationResult.SUCCESS
    } else {
      RecentsInvocationResult.ACTION_FAILED
    }

    _diagnosticInfo.update {
      it.copy(
        isEnabledInSettings = true,
        isServiceConnected = true,
        systemActionsCount = systemActionCount,
        isRecentsActionAvailable = hasRecents,
        lastAttemptTimestamp = timestamp,
        lastResult = result,
        lastInvocationResult = invocationResult,
        lastFailureReason = if (result) null else "performGlobalAction(GLOBAL_ACTION_RECENTS) returned false",
        availableActionNames = actionNames,
        lastInvokedSource = source
      )
    }

    return invocationResult
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

