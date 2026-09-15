package com.multispace.presentation

/**
 * Explicit interaction-state model for mutually exclusive launcher interactions.
 *
 * Ensures that gesture handlers (layer transition, app dragging, long-press, horizontal paging)
 * have well-defined mutual exclusion and clear interaction ownership boundaries to prevent
 * conflicting gesture handlers from acting at the same time.
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
   * An app or widget is currently being dragged across the screen.
   * All other interactions (page scrolling, layer transitions, dialog triggers) are suppressed.
   */
  data class DraggingApp(
    val itemId: String? = null,
    val packageName: String? = null
  ) : LauncherInteractionState()

  /**
   * A touch has triggered a long-press on an item, displaying quick actions or measuring drag slop.
   * Suppresses normal tap launches and layer transitions.
   */
  data class LongPressingApp(
    val itemId: String? = null
  ) : LauncherInteractionState()

  /**
   * An interactive or programmatic transition between Layer 1 (Curated Desktop)
   * and Layer 2 (App Library) is currently in progress.
   * Horizontal page swiping and app dragging are suppressed.
   */
  data class TransitioningLayer(
    val fromLayer: Int = 1
  ) : LauncherInteractionState()

  val isIdle: Boolean get() = this is Idle
  val isDragging: Boolean get() = this is DraggingApp
  val isLongPressing: Boolean get() = this is LongPressingApp
  val isTransitioningLayer: Boolean get() = this is TransitioningLayer
}

/**
 * Coordinator maintaining the current [LauncherInteractionState] and arbitrating
 * mutual exclusivity between concurrent gesture systems.
 */
class LauncherInteractionCoordinator(
  initialState: LauncherInteractionState = LauncherInteractionState.Idle
) {
  var currentState: LauncherInteractionState = initialState
    private set

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
    return currentState is LauncherInteractionState.Idle || currentState is LauncherInteractionState.LongPressingApp
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

  fun toLongPressing(itemId: String? = null): Boolean {
    if (canStartItemInteraction()) {
      currentState = LauncherInteractionState.LongPressingApp(itemId)
      return true
    }
    return false
  }

  fun toDragging(itemId: String? = null, packageName: String? = null): Boolean {
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
  fun startDraggingApp(itemId: String? = null, packageName: String? = null): Boolean = toDragging(itemId, packageName)
  fun endDragging() = toIdle()
}
