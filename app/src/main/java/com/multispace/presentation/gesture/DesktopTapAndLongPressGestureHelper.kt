package com.multispace.presentation.gesture

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Encapsulates Tap and Empty-Space Long-Press detection for the desktop.
 *
 * Separates tap and customization long-press responsibilities:
 * - Single-tap on an item: opens app or folder
 * - Single-tap on empty space: dismisses open action menus or widget resize bounding boxes
 * - Long-press on empty space: triggers haptic feedback and opens the Desktop Customization Sheet
 *
 * (Item long-press is handled separately by [AppDragDropGestureHelper] / [detectDragGesturesAfterLongPress]).
 */
object DesktopTapAndLongPressGestureHelper {

  /**
   * Modifier applying tap and empty-space long-press gesture detection.
   */
  fun createModifier(
    spaceId: String,
    enabled: Boolean = true,
    onTap: (Offset) -> Unit,
    onEmptyLongPress: (Offset) -> Unit
  ): Modifier {
    if (!enabled) return Modifier
    return Modifier.pointerInput(spaceId) {
      detectTapGestures(
        onTap = { offset ->
          onTap(offset)
        },
        onLongPress = { offset ->
          onEmptyLongPress(offset)
        }
      )
    }
  }
}

/**
 * Modifier extension applying tap and empty-space long press detection.
 */
fun Modifier.desktopTapAndEmptyLongPressGesture(
  spaceId: String,
  enabled: Boolean = true,
  onTap: (Offset) -> Unit,
  onEmptyLongPress: (Offset) -> Unit
): Modifier = this.then(
  DesktopTapAndLongPressGestureHelper.createModifier(
    spaceId = spaceId,
    enabled = enabled,
    onTap = onTap,
    onEmptyLongPress = onEmptyLongPress
  )
)
