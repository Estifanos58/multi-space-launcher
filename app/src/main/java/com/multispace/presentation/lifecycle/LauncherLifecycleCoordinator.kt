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
import com.multispace.presentation.events.BackgroundReason
import com.multispace.presentation.events.DefaultLauncherEventTracer
import com.multispace.presentation.events.HomeTriggerSource
import com.multispace.presentation.events.LauncherEvent
import com.multispace.presentation.events.LauncherEventTracer
import com.multispace.presentation.events.RestorationReason
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Coordinates launcher activity lifecycle events, default home status monitoring,
 * screen-off broadcast detection, semantic event mapping, and diagnostic lifecycle logging.
 */
class LauncherLifecycleCoordinator(
  val eventTracer: LauncherEventTracer = DefaultLauncherEventTracer.Global,
  private val onScreenOff: () -> Unit = {},
  private val onHomeIntent: (source: HomeTriggerSource) -> Unit = {},
  private val onResetTransientState: () -> Unit = {},
  private val onRestored: (reason: RestorationReason) -> Unit = {}
) {

  constructor(
    onScreenOff: () -> Unit = {},
    onHomeIntent: () -> Unit
  ) : this(
    eventTracer = DefaultLauncherEventTracer.Global,
    onScreenOff = onScreenOff,
    onHomeIntent = { _ -> onHomeIntent() },
    onResetTransientState = {},
    onRestored = {}
  )

  companion object {
    const val MAX_EVENT_LOGS = 100

    fun isHomeIntent(intent: Intent?): Boolean {
      if (intent == null) return false
      return intent.hasCategory(Intent.CATEGORY_HOME) ||
        (intent.action == Intent.ACTION_MAIN && intent.categories?.contains(Intent.CATEGORY_HOME) == true)
    }
  }

  // Internal lifecycle and task state tracking
  var isActivityCreated: Boolean = false
    private set
  var isActivityStarted: Boolean = false
    private set
  var isActivityResumed: Boolean = false
    private set
  var hasBeenBackgrounded: Boolean = false
    private set
  var pendingHomeIntent: Boolean = false
    private set
  var lastDispatchedAppLaunch: LauncherEvent.AppLaunchDispatched? = null
    private set

  private val _isDefaultHomeState = mutableStateOf(false)
  val isDefaultHomeState: State<Boolean> = _isDefaultHomeState

  private val _eventLogs = mutableStateListOf<String>()
  val eventLogs: SnapshotStateList<String> = _eventLogs

  private var isReceiverRegistered = false

  private val screenOffReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
      if (intent?.action == Intent.ACTION_SCREEN_OFF) {
        AppLogger.i(AppLogger.Category.LIFECYCLE, "Screen turned off -> Notifying lifecycle coordinator")
        eventTracer.record(LauncherEvent.ScreenOffReceived())
        recordEvent("I/Lifecycle", "SCREEN_OFF_RECEIVED -> Securing Launcher")
        onResetTransientState()
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

  fun recordAppLaunch(packageName: String, activityName: String?, spaceId: String) {
    val event = LauncherEvent.AppLaunchDispatched(
      packageName = packageName,
      activityName = activityName,
      spaceId = spaceId
    )
    lastDispatchedAppLaunch = event
    eventTracer.record(event)
    eventTracer.record(LauncherEvent.TransientStateReset("appLaunch"))
    recordEvent("I/Launch", "APP_LAUNCH_DISPATCHED ($packageName, space=$spaceId)")
    onResetTransientState()
  }

  fun onCreate(activity: Activity, intent: Intent?) {
    isActivityCreated = true
    hasBeenBackgrounded = false
    logActivityDetails(activity, "onCreate", intent)
    eventTracer.record(
      LauncherEvent.ActivityCreated(
        taskId = activity.taskId,
        isTaskRoot = activity.isTaskRoot,
        action = intent?.action,
        categories = intent?.categories ?: emptySet(),
        flags = intent?.flags ?: 0
      )
    )
    refreshDefaultHomeStatus(activity)
    registerScreenOffReceiver(activity)

    if (isHomeIntent(intent)) {
      pendingHomeIntent = true
      eventTracer.record(
        LauncherEvent.HomeKeyDispatched(
          source = HomeTriggerSource.COLD_START,
          isDefaultHome = _isDefaultHomeState.value
        )
      )
      recordEvent("I/Launcher", "HOME_KEY_DISPATCHED (source=COLD_START)")
      onHomeIntent(HomeTriggerSource.COLD_START)
    }
  }

  fun onStart(activity: Activity, intent: Intent?) {
    isActivityStarted = true
    logActivityDetails(activity, "onStart", intent)
    eventTracer.record(
      LauncherEvent.ActivityStarted(
        taskId = activity.taskId,
        isTaskRoot = activity.isTaskRoot
      )
    )
    refreshDefaultHomeStatus(activity)
  }

  fun onResume(activity: Activity, intent: Intent?) {
    isActivityResumed = true
    logActivityDetails(activity, "onResume", intent)
    eventTracer.record(
      LauncherEvent.ActivityResumed(
        taskId = activity.taskId,
        isTaskRoot = activity.isTaskRoot
      )
    )
    refreshDefaultHomeStatus(activity)

    if (hasBeenBackgrounded) {
      val reason = if (pendingHomeIntent) {
        RestorationReason.HOME_INTENT
      } else {
        RestorationReason.RECENTS_OR_TASK_SWITCH
      }
      eventTracer.record(LauncherEvent.RestoredFromBackground(reason))
      recordEvent("I/Lifecycle", "RESTORED_FROM_BACKGROUND ($reason)")
      onRestored(reason)
      pendingHomeIntent = false
      hasBeenBackgrounded = false
    } else if (pendingHomeIntent) {
      eventTracer.record(LauncherEvent.RestoredFromBackground(RestorationReason.COLD_START))
      onRestored(RestorationReason.COLD_START)
      pendingHomeIntent = false
    }
  }

  fun onPause(activity: Activity, intent: Intent?) {
    isActivityResumed = false
    logActivityDetails(activity, "onPause", intent)
    eventTracer.record(
      LauncherEvent.ActivityPaused(
        taskId = activity.taskId,
        isTaskRoot = activity.isTaskRoot
      )
    )

    val now = System.currentTimeMillis()
    val launch = lastDispatchedAppLaunch
    val reason = if (launch != null && (now - launch.timestamp) < 3000L) {
      BackgroundReason.APP_LAUNCH
    } else {
      BackgroundReason.SYSTEM_NAVIGATION
    }
    eventTracer.record(LauncherEvent.Backgrounded(reason))
    eventTracer.record(LauncherEvent.TransientStateReset("onPause"))
    recordEvent("I/Lifecycle", "BACKGROUNDED ($reason) -> Resetting transient interaction state")
    onResetTransientState()
  }

  fun onStop(activity: Activity, intent: Intent?) {
    isActivityStarted = false
    hasBeenBackgrounded = true
    logActivityDetails(activity, "onStop", intent)
    eventTracer.record(
      LauncherEvent.ActivityStopped(
        taskId = activity.taskId,
        isTaskRoot = activity.isTaskRoot
      )
    )
    onResetTransientState()
  }

  fun onDestroy(activity: Activity) {
    isActivityCreated = false
    isActivityStarted = false
    isActivityResumed = false
    logActivityDetails(activity, "onDestroy", activity.intent)
    eventTracer.record(LauncherEvent.ActivityDestroyed(taskId = activity.taskId))
    unregisterScreenOffReceiver(activity)
  }

  fun onNewIntent(activity: Activity, intent: Intent?) {
    logActivityDetails(activity, "onNewIntent", intent)
    refreshDefaultHomeStatus(activity)

    val homeIntent = isHomeIntent(intent)
    eventTracer.record(
      LauncherEvent.NewIntentReceived(
        taskId = activity.taskId,
        action = intent?.action,
        categories = intent?.categories ?: emptySet(),
        flags = intent?.flags ?: 0,
        isHomeIntent = homeIntent
      )
    )

    AppLogger.i(
      AppLogger.Category.LAUNCHER,
      "MainActivity onNewIntent: isHomeIntent=$homeIntent, isDefault=${_isDefaultHomeState.value}"
    )
    recordEvent("I/Launcher", "onNewIntent: isHomeIntent=$homeIntent, isDefault=${_isDefaultHomeState.value}")

    if (homeIntent) {
      val source = if (hasBeenBackgrounded || !isActivityResumed) {
        HomeTriggerSource.NEW_INTENT_BACKGROUND
      } else {
        HomeTriggerSource.NEW_INTENT_FOREGROUND
      }
      pendingHomeIntent = true
      eventTracer.record(
        LauncherEvent.HomeKeyDispatched(
          source = source,
          isDefaultHome = _isDefaultHomeState.value
        )
      )
      recordEvent("I/Launcher", "HOME_KEY_DISPATCHED (source=$source)")
      onHomeIntent(source)
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
