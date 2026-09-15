package com.multispace.presentation.gesture

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Encapsulates App and Item Drag & Drop gesture handling.
 *
 * Separates item long-press and drag-and-drop responsibilities:
 * - Detecting long press on placed items (apps, folders, widgets)
 * - Tracking accumulated movement against drag-slop threshold
 * - Transitioning into active drag and updating drop targets
 * - Completing drop or canceling drag
 * - Determining edge-paging trigger zones
 */
object AppDragDropGestureHelper {

  const val EDGE_PAGING_THRESHOLD_DP = 36f
  const val EDGE_PAGING_TRIGGER_DELAY_MS = 650L

  /**
   * Evaluates if pointer position is within the left or right edge-paging threshold.
   * Returns:
   * -1 for left edge paging (previous page)
   * +1 for right edge paging (next page)
   *  0 for neutral zone (no paging)
   */
  fun detectEdgePagingDirection(
    pointerX: Float,
    viewportWidth: Float,
    edgeThresholdPx: Float
  ): Int {
    if (viewportWidth <= 0f || edgeThresholdPx <= 0f) return 0
    return when {
      pointerX < edgeThresholdPx -> -1
      pointerX > viewportWidth - edgeThresholdPx -> 1
      else -> 0
    }
  }
}

/**
 * Modifier detecting long-press drag gestures specifically for placed items.
 */
fun Modifier.appDragGestures(
  spaceId: String,
  enabled: Boolean = true,
  onDragStart: (startOffset: Offset) -> Unit,
  onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
  onDragEnd: () -> Unit,
  onDragCancel: () -> Unit
): Modifier {
  if (!enabled) return this
  return this.pointerInput(spaceId) {
    detectDragGesturesAfterLongPress(
      onDragStart = { startOffset ->
        onDragStart(startOffset)
      },
      onDrag = { change, dragAmount ->
        onDrag(change, dragAmount)
      },
      onDragEnd = {
        onDragEnd()
      },
      onDragCancel = {
        onDragCancel()
      }
    )
  }
}
