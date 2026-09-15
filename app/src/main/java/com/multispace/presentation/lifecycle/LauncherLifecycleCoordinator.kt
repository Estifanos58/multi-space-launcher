package com.multispace.presentation.lifecycle

import android.app.Activity
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.multispace.diagnostics.AppLogger
import com.multispace.platform.HomePlatformManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Coordinates launcher activity lifecycle events, default home status monitoring,
 * screen-off broadcast detection, and diagnostic lifecycle logging.
 */
class LauncherLifecycleCoordinator(
  private val onScreenOff: () -> Unit = {},
  private val onHomeIntent: () -> Unit = {}
) {

  companion object {
    const val MAX_EVENT_LOGS = 100

    fun isHomeIntent(intent: Intent?): Boolean {
      if (intent == null) return false
      return intent.hasCategory(Intent.CATEGORY_HOME) ||
        (intent.action == Intent.ACTION_MAIN && intent.categories?.contains(Intent.CATEGORY_HOME) == true)
    }
  }

  private val _isDefaultHomeState = mutableStateOf(false)
  val isDefaultHomeState: State<Boolean> = _isDefaultHomeState

  private val _eventLogs = mutableStateListOf<String>()
  val eventLogs: SnapshotStateList<String> = _eventLogs

  private var isReceiverRegistered = false

  private val screenOffReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == Intent.ACTION_SCREEN_OFF) {
        AppLogger.i(AppLogger.Category.LIFECYCLE, "Screen turned off -> Notifying lifecycle coordinator")
        onScreenOff()
      }
    }
  }

  fun recordEvent(tag: String, message: String) {
    val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    if (_eventLogs.size >= MAX_EVENT_LOGS) {
      _eventLogs.removeAt(0)
    }
    _eventLogs.add("[$time] $tag: $message")
  }

  fun refreshDefaultHomeStatus(context: Context) {
    val isDefault = HomePlatformManager.checkHomeStatus(context) == HomePlatformManager.HomeRoleState.DEFAULT_HOME
    _isDefaultHomeState.value = isDefault
    AppLogger.d(AppLogger.Category.LAUNCHER, "Default Home status updated: $isDefault")
  }

  fun requestSetDefaultHome(activity: Activity, requestRoleLauncher: ActivityResultLauncher<Intent>? = null) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = activity.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
          val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
          if (requestRoleLauncher != null) {
            requestRoleLauncher.launch(intent)
          } else {
            activity.startActivity(intent)
          }
          return
        }
      }
      HomePlatformManager.openDefaultHomeSettings(activity)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to launch default Home request intent", e)
      HomePlatformManager.openDefaultHomeSettings(activity)
    }
  }

  fun onCreate(activity: Activity, intent: Intent?) {
    logActivityDetails(activity, "onCreate", intent)
    refreshDefaultHomeStatus(activity)
    registerScreenOffReceiver(activity)
  }

  fun onStart(activity: Activity, intent: Intent?) {
    logActivityDetails(activity, "onStart", intent)
    refreshDefaultHomeStatus(activity)
  }

  fun onResume(activity: Activity, intent: Intent?) {
    logActivityDetails(activity, "onResume", intent)
    refreshDefaultHomeStatus(activity)
  }

  fun onPause(activity: Activity, intent: Intent?) {
    logActivityDetails(activity, "onPause", intent)
  }

  fun onStop(activity: Activity, intent: Intent?) {
    logActivityDetails(activity, "onStop", intent)
  }

  fun onDestroy(activity: Activity) {
    logActivityDetails(activity, "onDestroy", activity.intent)
    unregisterScreenOffReceiver(activity)
  }

  fun onNewIntent(activity: Activity, intent: Intent?) {
    logActivityDetails(activity, "onNewIntent", intent)
    refreshDefaultHomeStatus(activity)

    val homeIntent = isHomeIntent(intent)
    AppLogger.i(
      AppLogger.Category.LAUNCHER,
      "MainActivity onNewIntent: isHomeIntent=$homeIntent, isDefault=${_isDefaultHomeState.value}"
    )
    recordEvent("I/Launcher", "onNewIntent: isHomeIntent=$homeIntent, isDefault=${_isDefaultHomeState.value}")

    if (homeIntent) {
      onHomeIntent()
    }
  }

  fun registerScreenOffReceiver(context: Context) {
    if (!isReceiverRegistered) {
      try {
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        context.registerReceiver(screenOffReceiver, filter)
        isReceiverRegistered = true
      } catch (e: Exception) {
        AppLogger.w(AppLogger.Category.LIFECYCLE, "Could not register screenOffReceiver: ${e.message}")
      }
    }
  }

  fun unregisterScreenOffReceiver(context: Context) {
    if (isReceiverRegistered) {
      try {
        context.unregisterReceiver(screenOffReceiver)
      } catch (e: Exception) {
        AppLogger.w(AppLogger.Category.LIFECYCLE, "Error unregistering screenOffReceiver: ${e.message}")
      } finally {
        isReceiverRegistered = false
      }
    }
  }

  private fun logActivityDetails(activity: Activity, event: String, intent: Intent?) {
    val action = intent?.action ?: "null"
    val categories = intent?.categories?.joinToString(",") ?: "none"
    val flags = intent?.flags?.let { "0x" + Integer.toHexString(it) } ?: "0x0"
    AppLogger.i(
      AppLogger.Category.LIFECYCLE,
      "MainActivity $event -> taskId=${activity.taskId}, isTaskRoot=${activity.isTaskRoot}, action=$action, categories=[$categories], flags=$flags"
    )
    recordEvent("I/Lifecycle", "$event (taskId=${activity.taskId}, root=${activity.isTaskRoot}, act=$action)")
  }
}
