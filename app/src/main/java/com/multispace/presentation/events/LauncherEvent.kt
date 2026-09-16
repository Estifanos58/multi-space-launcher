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
}
