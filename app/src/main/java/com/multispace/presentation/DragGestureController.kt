package com.multispace.presentation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Authoritative pointer and drag gesture controller that owns the full gesture lifecycle:
 * IDLE -> PRESSED_ACTION_VISIBLE / LONG_PRESS -> DRAGGING -> DROP / CANCEL
 *
 * Resolves gesture conflicts by maintaining a single pointer event stream:
 * 1. Short tap -> triggers item tap / launch without ever showing action menus or starting drag.
 * 2. Long press + release -> enters PRESSED_ACTION_VISIBLE and keeps quick-action menu visible on release; suppresses launch.
 * 3. Long press + movement -> transitions immediately from PRESSED_ACTION_VISIBLE to DRAGGING upon exceeding drag-slop;
 *    continuously updates pointer coordinates and performs DROP on release; suppresses launch.
 *
 * Reusable for Layer 1 (desktop placements) and Layer 2 (app catalog).
 */
class DragGestureController<T : Any>(
  var dragSlopPx: Float = 24f,
  var longPressTimeoutMs: Long = 400L,
  var unifiedDragState: UnifiedDragState? = null,
  var canDirectDrag: (T) -> Boolean = { false }
) {
  var lifecycleState by mutableStateOf(DragLifecycleState.IDLE)
  val isDragging: Boolean get() = lifecycleState == DragLifecycleState.DRAGGING

  var activeItem by mutableStateOf<T?>(null)
  var draggedItem by mutableStateOf<T?>(null)
  var activeActionItem by mutableStateOf<T?>(null)
  var pendingDragItem by mutableStateOf<T?>(null)

  var isPointerDown by mutableStateOf(false)
  var activePointerId: PointerId? = null
  var dragStartPos by mutableStateOf(Offset.Zero)
  var currentPointerPos by mutableStateOf(Offset.Zero)
  var touchOffsetWithinItem by mutableStateOf(Offset.Zero)
  var accumulatedDragDistance by mutableFloatStateOf(0f)
  var lastLongPressTimestamp by mutableLongStateOf(0L)
  var lastReleaseTimestamp by mutableLongStateOf(0L)

  // Callbacks
  var hitTest: ((Offset) -> T?)? = null
  var getItemBounds: ((T) -> Rect?)? = null
  var onItemTap: ((T) -> Unit)? = null
  var onEmptyTap: ((Offset) -> Unit)? = null
  var onEmptyLongPress: ((Offset) -> Unit)? = null
  var onLongPressAction: ((T, Offset) -> Unit)? = null
  var onDragStarted: ((T, Offset) -> Unit)? = null
  var onDragMoved: ((Offset) -> Unit)? = null
  var onDragDropped: ((T, Offset) -> Unit)? = null
  var onDragCancelled: (() -> Unit)? = null
  var onHapticFeedback: ((HapticFeedbackType) -> Unit)? = null

  fun handleDown(position: Offset, pointerId: PointerId? = null, time: Long = System.currentTimeMillis()) {
    isPointerDown = true
    activePointerId = pointerId
    dragStartPos = position
    currentPointerPos = position
    accumulatedDragDistance = 0f

    val resolved = hitTest?.invoke(position)
    activeItem = resolved

    // If actions were already visible and user touches outside the active action item, dismiss
    if (activeActionItem != null && resolved != activeActionItem) {
      dismissActions()
    }
  }

  fun handleTap(position: Offset) {
    isPointerDown = false
    activePointerId = null
    val item = activeItem ?: hitTest?.invoke(position)
    if (activeActionItem != null) {
      dismissActions()
    } else if (item != null) {
      onItemTap?.invoke(item)
    } else {
      onEmptyTap?.invoke(position)
    }
    reset()
  }

  fun handleLongPress(position: Offset, time: Long = System.currentTimeMillis()) {
    lastLongPressTimestamp = time
    val item = activeItem ?: hitTest?.invoke(position)

    if (item != null) {
      activeItem = item
      onHapticFeedback?.invoke(HapticFeedbackType.LongPress)
      if (canDirectDrag(item)) {
        // e.g. Folder directly enters DRAGGING
        lifecycleState = DragLifecycleState.DRAGGING
        draggedItem = item
        activeActionItem = null
        pendingDragItem = null
        unifiedDragState?.lifecycleState = DragLifecycleState.DRAGGING
        unifiedDragState?.isDragging = true
        onDragStarted?.invoke(item, position)
      } else {
        // App / Widget: enter PRESSED_ACTION_VISIBLE
        lifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
        activeActionItem = item
        pendingDragItem = item
        unifiedDragState?.lifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
        onLongPressAction?.invoke(item, position)
      }
    } else {
      lifecycleState = DragLifecycleState.IDLE
      unifiedDragState?.lifecycleState = DragLifecycleState.IDLE
      activeActionItem = null
      pendingDragItem = null
      onHapticFeedback?.invoke(HapticFeedbackType.LongPress)
      onEmptyLongPress?.invoke(position)
    }
  }

  fun handleMove(newPos: Offset) {
    if (!isPointerDown) return
    currentPointerPos = newPos
    val dist = (newPos - dragStartPos).getDistance()
    accumulatedDragDistance = dist

    if (lifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE && pendingDragItem != null) {
      if (dist >= dragSlopPx) {
        // Transition immediately from PRESSED_ACTION_VISIBLE to DRAGGING
        val itemToDrag = pendingDragItem!!
        activeActionItem = null
        pendingDragItem = null
        lifecycleState = DragLifecycleState.DRAGGING
        draggedItem = itemToDrag
        unifiedDragState?.lifecycleState = DragLifecycleState.DRAGGING
        unifiedDragState?.isDragging = true
        onHapticFeedback?.invoke(HapticFeedbackType.LongPress)
        onDragStarted?.invoke(itemToDrag, dragStartPos)
      }
    }

    if (lifecycleState == DragLifecycleState.DRAGGING) {
      onDragMoved?.invoke(newPos)
    }
  }

  fun handleReleaseWithoutDrag() {
    isPointerDown = false
    activePointerId = null
    // Release without movement: action menu remains visible, suppress launch
    pendingDragItem = null
  }

  fun handleDrop(dropPos: Offset) {
    isPointerDown = false
    activePointerId = null
    val item = draggedItem ?: activeItem
    if (lifecycleState == DragLifecycleState.DRAGGING && item != null) {
      lifecycleState = DragLifecycleState.DROP
      unifiedDragState?.lifecycleState = DragLifecycleState.DROP
      onDragDropped?.invoke(item, dropPos)
    }
    reset()
  }

  fun handleUp(position: Offset, time: Long = System.currentTimeMillis()) {
    isPointerDown = false
    activePointerId = null
    lastReleaseTimestamp = time
    when (lifecycleState) {
      DragLifecycleState.DRAGGING -> {
        handleDrop(position)
      }
      DragLifecycleState.PRESSED_ACTION_VISIBLE -> {
        handleReleaseWithoutDrag()
      }
      else -> {
        reset()
      }
    }
  }

  fun handleCancel() {
    isPointerDown = false
    activePointerId = null
    if (lifecycleState == DragLifecycleState.DRAGGING) {
      lifecycleState = DragLifecycleState.CANCEL
      unifiedDragState?.lifecycleState = DragLifecycleState.CANCEL
      onDragCancelled?.invoke()
    } else if (lifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE) {
      pendingDragItem = null
    }
    reset()
  }

  fun dismissActions() {
    activeActionItem = null
    pendingDragItem = null
    if (lifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE) {
      lifecycleState = DragLifecycleState.IDLE
      unifiedDragState?.lifecycleState = DragLifecycleState.IDLE
    }
  }

  fun reset() {
    lifecycleState = DragLifecycleState.IDLE
    draggedItem = null
    pendingDragItem = null
    accumulatedDragDistance = 0f
    isPointerDown = false
    activePointerId = null
  }
}

fun <T : Any> Modifier.dragGestureHandler(
  controller: DragGestureController<T>
): Modifier = this.pointerInput(controller) {
  detectAuthoritativeDragGestures(controller)
}

suspend fun <T : Any> PointerInputScope.detectAuthoritativeDragGestures(
  controller: DragGestureController<T>
) {
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false)
    val pointerId = down.id
    val downPos = down.position
    controller.handleDown(downPos, pointerId)

    var longPressTriggered = false
    var pointerUpBeforeLongPress = false
    var movedPastSlopBeforeLongPress = false

    try {
      withTimeout(controller.longPressTimeoutMs) {
        while (true) {
          val event = awaitPointerEvent(PointerEventPass.Main)
          val change = event.changes.firstOrNull { it.id == pointerId } ?: break
          if (change.changedToUp()) {
            pointerUpBeforeLongPress = true
            change.consume()
            break
          }
          if (change.isConsumed) {
            break
          }
          val distance = (change.position - downPos).getDistance()
          if (distance > viewConfiguration.touchSlop) {
            movedPastSlopBeforeLongPress = true
            break
          }
        }
      }
    } catch (_: PointerEventTimeoutCancellationException) {
      longPressTriggered = true
    }

    if (pointerUpBeforeLongPress) {
      controller.handleTap(downPos)
      return@awaitEachGesture
    }

    if (movedPastSlopBeforeLongPress) {
      controller.handleCancel()
      return@awaitEachGesture
    }

    if (!longPressTriggered) {
      controller.handleCancel()
      return@awaitEachGesture
    }

    // Long press triggered!
    controller.handleLongPress(downPos)

    // The SAME pointer is now owned authoritative until UP/CANCEL
    while (true) {
      val event = awaitPointerEvent(PointerEventPass.Initial)
      val change = event.changes.firstOrNull { it.id == pointerId } ?: break
      change.consume()

      if (change.changedToUp()) {
        controller.handleUp(change.position)
        break
      }

      if (!change.pressed) {
        controller.handleCancel()
        break
      }

      controller.handleMove(change.position)
    }
  }
}
