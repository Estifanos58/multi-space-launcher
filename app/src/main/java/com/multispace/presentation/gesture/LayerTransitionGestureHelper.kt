package com.multispace.presentation.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Velocity
import com.multispace.presentation.LauncherInteractionState
import kotlin.math.abs

/**
 * Encapsulates Layer 1 ↔ Layer 2 vertical transition gesture logic and calculation helpers.
 *
 * Separates layer transition responsibilities:
 * - Empty space swipe detection on Layer 1 (with touchSlop and vertical/horizontal arbitration)
 * - Layer 2 header downward drag gestures
 * - Layer 2 nested scrolling coordination
 * - Settle target and progress velocity physics calculations
 */
object LayerTransitionGestureHelper {

  const val DEFAULT_FLING_THRESHOLD_PX = 500f
  const val TRANSITION_THRESHOLD_PROGRESS = 0.40f

  /**
   * Computes the target layer progress (0.0f for Layer 1, 1.0f for Layer 2)
   * based on current progress and release fling velocity.
   */
  fun calculateSettleTarget(
    currentProgress: Float,
    velocityY: Float,
    flingThresholdPx: Float = DEFAULT_FLING_THRESHOLD_PX
  ): Float {
    return when {
      velocityY < -flingThresholdPx && currentProgress > 0.02f -> 1.0f
      velocityY > flingThresholdPx && currentProgress < 0.98f -> 0.0f
      currentProgress >= TRANSITION_THRESHOLD_PROGRESS -> 1.0f
      else -> 0.0f
    }
  }

  /**
   * Computes normalized progress delta for an upward/downward finger displacement.
   * Upward movement (negative dragDeltaY in Compose) increases progress towards Layer 2.
   */
  fun calculateProgressDelta(dragDeltaY: Float, screenHeightPx: Float): Float {
    if (screenHeightPx <= 0f) return 0f
    return -dragDeltaY / screenHeightPx
  }

  /**
   * Computes normalized progress velocity for animation physics.
   */
  fun calculateProgressVelocity(velocityY: Float, screenHeightPx: Float): Float {
    if (screenHeightPx <= 0f) return 0f
    return -velocityY / screenHeightPx
  }

  /**
   * Creates a [NestedScrollConnection] coordinating scroll gestures between
   * Layer 2's scrollable list/grid and the layer transition progress.
   */
  fun createNestedScrollConnection(
    screenHeightPx: Float,
    getProgress: () -> Float,
    setProgress: (Float) -> Unit,
    isAtTop: () -> Boolean,
    onGestureActiveChanged: (Boolean) -> Unit,
    onSettle: (currentProgress: Float, velocityY: Float) -> Unit
  ): NestedScrollConnection {
    return object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val currentProgress = getProgress()
        // If Layer 2 is currently in transition (not fully resting at 1.0f), consume vertical deltas
        if (currentProgress in 0.001f..0.999f) {
          onGestureActiveChanged(true)
          val deltaProgress = -available.y / screenHeightPx
          val newProgress = (currentProgress + deltaProgress).coerceIn(0f, 1f)
          setProgress(newProgress)
          return Offset(0f, available.y)
        }

        // When scrolling down while at the top of Layer 2, start pulling Layer 2 closed towards Layer 1
        if (available.y > 0f && isAtTop() && currentProgress >= 0.999f) {
          onGestureActiveChanged(true)
          val deltaProgress = -available.y / screenHeightPx
          val newProgress = (currentProgress + deltaProgress).coerceIn(0f, 1f)
          setProgress(newProgress)
          return Offset(0f, available.y)
        }
        return Offset.Zero
      }

      override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return Offset.Zero
      }

      override suspend fun onPreFling(available: Velocity): Velocity {
        val currentProgress = getProgress()
        if (currentProgress in 0.001f..0.999f) {
          onSettle(currentProgress, available.y)
          return available
        }
        return Velocity.Zero
      }

      override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val currentProgress = getProgress()
        if (currentProgress < 0.999f) {
          onSettle(currentProgress, available.y)
          return available
        }
        return Velocity.Zero
      }
    }
  }
}

/**
 * Modifier for Layer 1 empty desktop space swipe detection with gesture arbitration.
 *
 * Checks whether the initial touch hits an item (app/widget). If it hits an item,
 * layer transition does not claim the gesture, letting item drag own it.
 * If initial touch is on empty space:
 * - Tracks movement until passing touch-slop.
 * - If vertical movement dominates (absY > absX): claims the gesture, consumes vertical deltas,
 *   and smoothly drives Layer 1 ↔ Layer 2 transition progress.
 * - If horizontal movement dominates (absX >= absY): aborts without consuming, allowing
 *   HorizontalPager to handle page swiping.
 */
fun Modifier.layerTransitionEmptySpaceSwipe(
  spaceId: String,
  isSwipeAllowed: Boolean,
  canTransition: () -> Boolean = { true },
  findItemAtOffset: (Offset) -> Any?,
  onSwipeStart: () -> Unit,
  onSwipeMove: (dragDeltaY: Float, change: PointerInputChange) -> Unit,
  onSwipeEnd: () -> Unit,
  onSwipeCancel: () -> Unit
): Modifier = this.pointerInput(spaceId, isSwipeAllowed) {
  if (!isSwipeAllowed) return@pointerInput
  val touchSlop = viewConfiguration.touchSlop

  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

    if (!canTransition()) {
      return@awaitEachGesture
    }

    // Arbitration: if initial touch hits an app or widget, do not intercept
    val initialHit = findItemAtOffset(down.position)
    if (initialHit != null) {
      return@awaitEachGesture
    }

    val pointerId = down.id
    var isDraggingLayer = false
    var previousY = down.position.y

    while (true) {
      val event = awaitPointerEvent(PointerEventPass.Initial)
      val change = event.changes.firstOrNull { it.id == pointerId } ?: break

      if (change.changedToUp()) {
        if (isDraggingLayer) {
          change.consume()
          onSwipeEnd()
        }
        break
      }

      if (!change.pressed) {
        if (isDraggingLayer) {
          onSwipeCancel()
        }
        break
      }

      if (!isDraggingLayer) {
        val totalDeltaX = change.position.x - down.position.x
        val totalDeltaY = change.position.y - down.position.y
        val absX = abs(totalDeltaX)
        val absY = abs(totalDeltaY)

        if (absX >= touchSlop || absY >= touchSlop) {
          if (absY > absX) {
            // Vertical dominance: claim for Layer transition
            isDraggingLayer = true
            onSwipeStart()
            val dragDeltaY = change.position.y - down.position.y
            previousY = change.position.y
            onSwipeMove(dragDeltaY, change)
            change.consume()
          } else {
            // Horizontal dominance: abort so HorizontalPager handles page swiping
            return@awaitEachGesture
          }
        }
      } else {
        val currentY = change.position.y
        val dragDeltaY = currentY - previousY
        previousY = currentY

        if (dragDeltaY != 0f) {
          onSwipeMove(dragDeltaY, change)
          change.consume()
        }
      }
    }
  }
}

/**
 * Modifier for Layer 2 header vertical drag gesture.
 */
fun Modifier.layer2HeaderDragGesture(
  enabled: Boolean = true,
  screenHeightPx: Float,
  onDragStart: () -> Unit,
  onDragDelta: (progressDelta: Float) -> Unit,
  onDragEnd: () -> Unit,
  onDragCancel: () -> Unit
): Modifier = if (!enabled) this else this.pointerInput(Unit) {
  detectVerticalDragGestures(
    onDragStart = { onDragStart() },
    onVerticalDrag = { change, dragAmount ->
      change.consume()
      val deltaProgress = LayerTransitionGestureHelper.calculateProgressDelta(dragAmount, screenHeightPx)
      onDragDelta(deltaProgress)
    },
    onDragEnd = { onDragEnd() },
    onDragCancel = { onDragCancel() }
  )
}
