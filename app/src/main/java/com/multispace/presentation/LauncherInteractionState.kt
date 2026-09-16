package com.multispace.presentation

import androidx.compose.ui.geometry.Offset
import com.multispace.domain.model.AppIdentity

/**
 * Origin of a drag operation across the launcher.
 */
sealed interface DragOrigin {
  data class Desktop(
    val pageIndex: Int,
    val positionIndex: Int,
    val placementId: String? = null
  ) : DragOrigin

  data class Dock(
    val index: Int,
    val dockItemId: String? = null
  ) : DragOrigin

  data object Library : DragOrigin
}

/**
 * Target drop zone destination for an item.
 */
sealed interface DropTarget {
  data class DesktopCell(val pageIndex: Int, val positionIndex: Int) : DropTarget
  data class DesktopFolder(val folderId: String) : DropTarget
  data class DockSlot(val index: Int) : DropTarget
  data object Bin : DropTarget
  data object None : DropTarget
}

/**
 * Explicit interaction-state model for mutually exclusive launcher interactions.
 *
 * Ensures that gesture handlers (layer transition, app dragging, long-press, horizontal paging)
 * have well-defined mutual exclusion and clear interaction ownership boundaries to prevent
 * conflicting gesture handlers from acting at the same time.
 *
 * Formal lifecycle:
 * Resting (Idle) -> Pressing -> ContextMenu -> Dragging -> Dropping -> Idle
 */
sealed class LauncherInteractionState {

  /**
   * The launcher is at rest with no active gestures or transitions.
   * All primary navigation (taps, horizontal paging, swipe transitions) can be initiated from this state.
   */
  object Idle : LauncherInteractionState() {
    override fun toString(): String = "Idle"
  }

  /**
   * An item has been touched/pressed and slop/duration is being measured.
   */
  data class Pressing(
    val identity: AppIdentity? = null,
    val startPosition: Offset = Offset.Zero,
    val itemId: String? = null
  ) : LauncherInteractionState()

  /**
   * Quick action / context menu is displayed for an item.
   */
  data class ContextMenu(
    val identity: AppIdentity? = null,
    val position: Offset = Offset.Zero,
    val itemId: String? = null
  ) : LauncherInteractionState()

  /**
   * An item or widget is currently being dragged across the screen.
   * All other interactions (page scrolling, layer transitions, dialog triggers) are suppressed.
   */
  data class Dragging(
    val identity: AppIdentity? = null,
    val origin: DragOrigin = DragOrigin.Desktop(0, 0),
    val currentPosition: Offset = Offset.Zero,
    val itemId: String? = null,
    val packageName: String? = null
  ) : LauncherInteractionState()

  /**
   * An item has been released and is dropping into its resolved target.
   */
  data class Dropping(
    val identity: AppIdentity? = null,
    val target: DropTarget = DropTarget.None
  ) : LauncherInteractionState()

  /**
   * An interactive or programmatic transition between Layer 1 (Curated Desktop)
   * and Layer 2 (App Library) is currently in progress.
   * Horizontal page swiping and app dragging are suppressed.
   */
  data class TransitioningLayer(
    val fromLayer: Int = 1
  ) : LauncherInteractionState()

  /**
   * Backward-compatible legacy class for existing tests.
   */
  data class DraggingApp(
    val itemId: String? = null,
    val packageName: String? = null
  ) : LauncherInteractionState()

  /**
   * Backward-compatible legacy class for existing tests.
   */
  data class LongPressingApp(
    val itemId: String? = null
  ) : LauncherInteractionState()

  val isIdle: Boolean get() = this is Idle
  val isDragging: Boolean get() = this is Dragging || this is DraggingApp
  val isLongPressing: Boolean get() = this is Pressing || this is ContextMenu || this is LongPressingApp
  val isTransitioningLayer: Boolean get() = this is TransitioningLayer
}

/**
 * Coordinator maintaining the current [LauncherInteractionState] and arbitrating
 * mutual exclusivity between concurrent gesture systems.
 */
class LauncherInteractionCoordinator(
  initialState: LauncherInteractionState = LauncherInteractionState.Idle,
  val eventTracer: com.multispace.presentation.events.LauncherEventTracer = com.multispace.presentation.events.DefaultLauncherEventTracer.Global
) {
  var currentState: LauncherInteractionState = initialState
    private set(value) {
      val prev = field
      if (prev != value) {
        field = value
        eventTracer.record(com.multispace.presentation.events.LauncherEvent.StateChanged(prev, value))
      }
    }

  /**
   * Returns whether a layer transition can be initiated from the current state.
   */
  fun canTransitionLayer(): Boolean {
    return currentState is LauncherInteractionState.Idle || currentState is LauncherInteractionState.TransitioningLayer
  }

  /**
   * Returns whether an item drag or long-press can be initiated from the current state.
   */
  fun canStartItemInteraction(): Boolean {
    return currentState is LauncherInteractionState.Idle ||
      currentState is LauncherInteractionState.LongPressingApp ||
      currentState is LauncherInteractionState.Pressing ||
      currentState is LauncherInteractionState.ContextMenu
  }

  /**
   * Returns whether horizontal desktop paging is permitted in the current state.
   */
  fun canPageHorizontally(): Boolean {
    return currentState is LauncherInteractionState.Idle
  }

  /**
   * Returns whether a normal app tap-to-launch is permitted in the current state.
   * Suppressed when in long-press action state, dragging, or during layer transition.
   */
  fun canLaunchApp(): Boolean {
    return currentState is LauncherInteractionState.Idle
  }

  fun toIdle() {
    currentState = LauncherInteractionState.Idle
  }

  fun toPressing(identity: AppIdentity? = null, startPosition: Offset = Offset.Zero, itemId: String? = null): Boolean {
    if (canStartItemInteraction()) {
      currentState = LauncherInteractionState.Pressing(identity, startPosition, itemId)
      return true
    }
    return false
  }

  fun toContextMenu(identity: AppIdentity? = null, position: Offset = Offset.Zero, itemId: String? = null): Boolean {
    if (canStartItemInteraction()) {
      currentState = LauncherInteractionState.ContextMenu(identity, position, itemId)
      return true
    }
    return false
  }

  fun toDragging(
    itemId: String?,
    packageName: String? = null
  ): Boolean {
    if (canStartItemInteraction()) {
      currentState = LauncherInteractionState.DraggingApp(itemId, packageName)
      return true
    }
    return false
  }

  fun toDragging(
    identity: AppIdentity?,
    origin: DragOrigin = DragOrigin.Desktop(0, 0),
    currentPosition: Offset = Offset.Zero,
    itemId: String? = null,
    packageName: String? = null
  ): Boolean {
    if (canStartItemInteraction()) {
      currentState = LauncherInteractionState.Dragging(identity, origin, currentPosition, itemId, packageName)
      return true
    }
    return false
  }

  fun toDropping(identity: AppIdentity? = null, target: DropTarget = DropTarget.None): Boolean {
    if (currentState.isDragging) {
      currentState = LauncherInteractionState.Dropping(identity, target)
      return true
    }
    return false
  }

  fun toLongPressing(itemId: String? = null): Boolean {
    if (canStartItemInteraction()) {
      currentState = LauncherInteractionState.LongPressingApp(itemId)
      return true
    }
    return false
  }

  fun toDraggingLegacy(itemId: String? = null, packageName: String? = null): Boolean {
    if (canStartItemInteraction()) {
      currentState = LauncherInteractionState.DraggingApp(itemId, packageName)
      return true
    }
    return false
  }

  fun toTransitioningLayer(fromLayer: Int = 1): Boolean {
    if (canTransitionLayer()) {
      currentState = LauncherInteractionState.TransitioningLayer(fromLayer)
      return true
    }
    return false
  }

  fun startTransition(fromLayer: Int = 1): Boolean = toTransitioningLayer(fromLayer)
  fun finishTransition() = toIdle()
  fun startDraggingApp(itemId: String? = null, packageName: String? = null): Boolean = toDraggingLegacy(itemId, packageName)
  fun endDragging() = toIdle()
}
