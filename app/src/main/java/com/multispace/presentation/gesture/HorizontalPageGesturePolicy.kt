package com.multispace.presentation.gesture

import com.multispace.presentation.DragLifecycleState
import com.multispace.presentation.LauncherInteractionState

/**
 * Policy governing horizontal desktop page swiping and scroll enablement.
 *
 * Ensures horizontal paging is active ONLY when:
 * 1. The launcher is in [LauncherInteractionState.Idle]
 * 2. No drag-and-drop operation is active or dropping
 * 3. Drag lifecycle state is [DragLifecycleState.IDLE]
 * 4. Layer 1 ↔ Layer 2 transition is not active
 */
object HorizontalPageGesturePolicy {

  /**
   * Determines if horizontal paging should be permitted for [HorizontalPager].
   */
  fun isScrollEnabled(
    interactionState: LauncherInteractionState,
    isDragging: Boolean,
    isDropping: Boolean,
    dragLifecycleState: DragLifecycleState = DragLifecycleState.IDLE
  ): Boolean {
    val stateAllows = interactionState is LauncherInteractionState.Idle
    val notInDrag = !isDragging && !isDropping && dragLifecycleState == DragLifecycleState.IDLE
    return stateAllows && notInDrag
  }
}
