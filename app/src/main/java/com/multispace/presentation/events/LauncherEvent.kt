package com.multispace.presentation.events

import androidx.compose.ui.geometry.Offset
import com.multispace.domain.model.AppIdentity
import com.multispace.presentation.DragOrigin
import com.multispace.presentation.DragTargetZone
import com.multispace.presentation.DropTarget
import com.multispace.presentation.LauncherInteractionState

/**
 * Structured event hierarchy for observable and deterministic tracing of launcher interactions.
 * Enables live diagnostics, automated testing, telemetry verification, and state transition monitoring.
 */
sealed interface LauncherEvent {
  val timestamp: Long

  data class StateChanged(
    val previousState: LauncherInteractionState,
    val newState: LauncherInteractionState,
    override val timestamp: Long = System.currentTimeMillis()
  ) : LauncherEvent

  data class LayerTransitionStarted(
    val fromLayer: Int,
    override val timestamp: Long = System.currentTimeMillis()
  ) : LauncherEvent

  data class LayerTransitionProgress(
    val progress: Float,
    override val timestamp: Long = System.currentTimeMillis()
  ) : LauncherEvent

  data class LayerTransitionSettled(
    val targetLayer: Int,
    val progress: Float,
    override val timestamp: Long = System.currentTimeMillis()
  ) : LauncherEvent

  data class DragStarted(
    val identity: AppIdentity?,
    val origin: DragOrigin,
    val initialPosition: Offset = Offset.Zero,
    val itemId: String? = null,
    val packageName: String? = null,
    override val timestamp: Long = System.currentTimeMillis()
  ) : LauncherEvent

  data class DragMoved(
    val position: Offset,
    val targetZone: DragTargetZone,
    override val timestamp: Long = System.currentTimeMillis()
  ) : LauncherEvent

  data class DragTargetZoneChanged(
    val previousZone: DragTargetZone,
    val newZone: DragTargetZone,
    override val timestamp: Long = System.currentTimeMillis()
  ) : LauncherEvent

  data class DragDropped(
    val identity: AppIdentity?,
    val target: DropTarget,
    override val timestamp: Long = System.currentTimeMillis()
  ) : LauncherEvent

  data class DragCancelled(
    val identity: AppIdentity?,
    val origin: DragOrigin,
    override val timestamp: Long = System.currentTimeMillis()
  ) : LauncherEvent

  data class ActionMenuOpened(
    val identity: AppIdentity?,
    val position: Offset = Offset.Zero,
    val itemId: String? = null,
    override val timestamp: Long = System.currentTimeMillis()
  ) : LauncherEvent

  data class ActionMenuDismissed(
    override val timestamp: Long = System.currentTimeMillis()
  ) : LauncherEvent

  // --- Semantic Launcher Events ---

  sealed interface Semantic : LauncherEvent

  data class HomeKeyDispatched(
    val source: HomeTriggerSource,
    val isDefaultHome: Boolean = false,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Semantic

  data class AppLaunchDispatched(
    val packageName: String,
    val activityName: String?,
    val spaceId: String,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Semantic

  data class RestoredFromBackground(
    val reason: RestorationReason,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Semantic

  data class Backgrounded(
    val reason: BackgroundReason,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Semantic

  data class TransientStateReset(
    val trigger: String,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Semantic

  data class ScreenOffReceived(
    override val timestamp: Long = System.currentTimeMillis()
  ) : Semantic

  // --- Activity Lifecycle Events ---

  sealed interface Lifecycle : LauncherEvent

  data class ActivityCreated(
    val taskId: Int,
    val isTaskRoot: Boolean,
    val action: String?,
    val categories: Set<String>,
    val flags: Int,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Lifecycle

  data class ActivityStarted(
    val taskId: Int,
    val isTaskRoot: Boolean,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Lifecycle

  data class ActivityResumed(
    val taskId: Int,
    val isTaskRoot: Boolean,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Lifecycle

  data class ActivityPaused(
    val taskId: Int,
    val isTaskRoot: Boolean,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Lifecycle

  data class ActivityStopped(
    val taskId: Int,
    val isTaskRoot: Boolean,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Lifecycle

  data class ActivityDestroyed(
    val taskId: Int,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Lifecycle

  data class NewIntentReceived(
    val taskId: Int,
    val action: String?,
    val categories: Set<String>,
    val flags: Int,
    val isHomeIntent: Boolean,
    override val timestamp: Long = System.currentTimeMillis()
  ) : Lifecycle
}

enum class HomeTriggerSource {
  COLD_START,
  NEW_INTENT_BACKGROUND,
  NEW_INTENT_FOREGROUND
}

enum class RestorationReason {
  HOME_INTENT,
  RECENTS_OR_TASK_SWITCH,
  COLD_START
}

enum class BackgroundReason {
  APP_LAUNCH,
  SYSTEM_NAVIGATION,
  SCREEN_OFF,
  UNKNOWN
}
