package com.multispace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

class LauncherGestureCoordinator(
  val unifiedDragState: UnifiedDragState,
  val transitionController: LayerTransitionController,
  val interactionCoordinator: LauncherInteractionCoordinator = LauncherInteractionCoordinator()
) {
  val isAnyDragActive: Boolean
    get() = unifiedDragState.isDragging || unifiedDragState.lifecycleState != DragLifecycleState.IDLE

  val currentInteractionState: LauncherInteractionState
    get() = when {
      transitionController.isGestureActive || (transitionController.layerTransitionProgress > 0f && transitionController.layerTransitionProgress < 1f) ->
        LauncherInteractionState.TransitioningLayer(if (transitionController.layerTransitionProgress >= 0.5f) 2 else 1)
      unifiedDragState.isDragging -> unifiedDragState.formalInteractionState
      unifiedDragState.lifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE -> unifiedDragState.formalInteractionState
      else -> LauncherInteractionState.Idle
    }

  fun canDragLayer1(useLayer2: Boolean): Boolean {
    return useLayer2 && transitionController.layerTransitionProgress < 1f && !isAnyDragActive
  }

  fun canPageHorizontally(): Boolean {
    return !isAnyDragActive && !transitionController.isGestureActive && transitionController.layerTransitionProgress <= 0.001f
  }

  fun canLaunchApp(): Boolean {
    return !isAnyDragActive && !transitionController.isGestureActive &&
      (transitionController.layerTransitionProgress <= 0.001f || transitionController.layerTransitionProgress >= 0.999f)
  }
}

@Composable
fun rememberLauncherGestureCoordinator(
  unifiedDragState: UnifiedDragState,
  transitionController: LayerTransitionController
): LauncherGestureCoordinator {
  return remember(unifiedDragState, transitionController) {
    LauncherGestureCoordinator(
      unifiedDragState = unifiedDragState,
      transitionController = transitionController
    )
  }
}
