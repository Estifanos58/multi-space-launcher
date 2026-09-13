package com.multispace

import com.multispace.domain.model.Space
import com.multispace.presentation.DragLifecycleState
import com.multispace.presentation.UnifiedDragState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerTransitionLogicTest {

  @Test
  fun testSwipeAllowedInBothDockButtonAndSwipeUpModesWhenLayer2Enabled() {
    // Default space has ACCESS_MODE_DOCK_BUTTON and useLayer2 = true
    val defaultSpace = Space.createDefault()
    assertEquals(Space.ACCESS_MODE_DOCK_BUTTON, defaultSpace.layer2AccessMode)
    assertTrue(defaultSpace.useLayer2)

    val isSwipeAllowedDefault = defaultSpace.useLayer2
    assertTrue("Swipe must be allowed by default even with ACCESS_MODE_DOCK_BUTTON", isSwipeAllowedDefault)

    // SWIPE_UP mode with useLayer2 = true
    val swipeUpSpace = defaultSpace.copy(layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP)
    val isSwipeAllowedSwipeUp = swipeUpSpace.useLayer2
    assertTrue("Swipe must be allowed with ACCESS_MODE_SWIPE_UP", isSwipeAllowedSwipeUp)

    // When useLayer2 is disabled
    val disabledLayer2Space = defaultSpace.copy(useLayer2 = false)
    val isSwipeAllowedDisabled = disabledLayer2Space.useLayer2
    assertFalse("Swipe must be disabled when Layer 2 is turned off", isSwipeAllowedDisabled)
  }

  @Test
  fun testCanDragLayer1Conditions() {
    val dragState = UnifiedDragState()
    var isAnyDragActive = dragState.isDragging || dragState.lifecycleState != DragLifecycleState.IDLE
    assertFalse(isAnyDragActive)

    var layerTransitionProgress = 0.0f
    val isSwipeAllowed = true

    // Resting at Layer 1: can drag Layer 1
    var canDragLayer1 = layerTransitionProgress < 1f && !isAnyDragActive && isSwipeAllowed
    assertTrue("Should be able to drag Layer 1 when resting at 0f", canDragLayer1)

    // During active app/widget drag: Layer 1 dragging must be suppressed
    dragState.isDragging = true
    isAnyDragActive = dragState.isDragging || dragState.lifecycleState != DragLifecycleState.IDLE
    canDragLayer1 = layerTransitionProgress < 1f && !isAnyDragActive && isSwipeAllowed
    assertFalse("Dragging an app/widget must suppress Layer 1 transition drag", canDragLayer1)

    // Reset app drag
    dragState.isDragging = false
    isAnyDragActive = false

    // Fully on Layer 2 (progress = 1.0f): canDragLayer1 must be false
    layerTransitionProgress = 1.0f
    canDragLayer1 = layerTransitionProgress < 1f && !isAnyDragActive && isSwipeAllowed
    assertFalse("When fully on Layer 2, Layer 1 drag modifier should not be active", canDragLayer1)
  }

  @Test
  fun testContinuousFingerFollowingProgressDelta() {
    val screenHeightPx = 2000f
    var layerTransitionProgress = 0f

    // User drags upward by 200px (dragAmount = -200f in Compose coordinates)
    val dragAmountUp = -200f
    val progressDeltaUp = -dragAmountUp / screenHeightPx
    assertEquals(0.1f, progressDeltaUp, 0.0001f)

    layerTransitionProgress = (layerTransitionProgress + progressDeltaUp).coerceIn(0f, 1f)
    assertEquals(0.1f, layerTransitionProgress, 0.0001f)

    // User drags upward by another 800px
    val dragAmountUp2 = -800f
    val progressDeltaUp2 = -dragAmountUp2 / screenHeightPx
    layerTransitionProgress = (layerTransitionProgress + progressDeltaUp2).coerceIn(0f, 1f)
    assertEquals(0.5f, layerTransitionProgress, 0.0001f)

    // User drags downward by 200px while in progress (dragAmount = 200f)
    val dragAmountDown = 200f
    val progressDeltaDown = -dragAmountDown / screenHeightPx
    layerTransitionProgress = (layerTransitionProgress + progressDeltaDown).coerceIn(0f, 1f)
    assertEquals(0.4f, layerTransitionProgress, 0.0001f)
  }

  @Test
  fun testSettleTransitionFlingAndThresholdSettling() {
    val flingThresholdPx = 500f

    fun calculateTargetValue(currentProgress: Float, velocityY: Float): Float {
      return when {
        velocityY < -flingThresholdPx && currentProgress > 0.02f -> 1.0f
        velocityY > flingThresholdPx && currentProgress < 0.98f -> 0.0f
        currentProgress >= 0.60f -> 1.0f
        else -> 0.0f
      }
    }

    // Upward fast fling from low progress (e.g. flick up from 0.05f) -> settle to 1.0f (Layer 2)
    assertEquals(1.0f, calculateTargetValue(0.05f, -1200f), 0.001f)
    assertEquals(1.0f, calculateTargetValue(0.15f, -800f), 0.001f)

    // Downward fast fling from high progress (e.g. flick down from 0.95f) -> settle to 0.0f (Layer 1)
    assertEquals(0.0f, calculateTargetValue(0.95f, 1200f), 0.001f)
    assertEquals(0.0f, calculateTargetValue(0.85f, 800f), 0.001f)

    // Slow release at 0.7f (no fling) -> settle to 1.0f (Layer 2)
    assertEquals(1.0f, calculateTargetValue(0.7f, 0f), 0.001f)
    assertEquals(1.0f, calculateTargetValue(0.60f, 0f), 0.001f)

    // Slow release at 0.5f (no fling, pulled down past 40%) -> settle to 0.0f (Layer 1)
    assertEquals(0.0f, calculateTargetValue(0.5f, 0f), 0.001f)
    assertEquals(0.0f, calculateTargetValue(0.3f, 0f), 0.001f)
  }

  @Test
  fun testIsAtTopEvaluationForGridAndSectionList() {
    fun checkIsAtTop(
      isSectionedView: Boolean,
      gridCanScrollBackward: Boolean,
      gridIndex: Int,
      gridOffset: Int,
      listCanScrollBackward: Boolean,
      listIndex: Int,
      listOffset: Int
    ): Boolean {
      val isGridAtTop = !gridCanScrollBackward || (gridIndex == 0 && gridOffset <= 0)
      val isListAtTop = !listCanScrollBackward || (listIndex == 0 && listOffset <= 0)
      return if (isSectionedView) isListAtTop else isGridAtTop
    }

    // Grid view at top -> should be true even if section list has offset
    assertTrue(checkIsAtTop(
      isSectionedView = false,
      gridCanScrollBackward = false,
      gridIndex = 0,
      gridOffset = 0,
      listCanScrollBackward = true,
      listIndex = 5,
      listOffset = 100
    ))

    // Grid view scrolled down -> should be false
    assertFalse(checkIsAtTop(
      isSectionedView = false,
      gridCanScrollBackward = true,
      gridIndex = 2,
      gridOffset = 50,
      listCanScrollBackward = false,
      listIndex = 0,
      listOffset = 0
    ))

    // Section list view at top -> should be true even if grid has offset
    assertTrue(checkIsAtTop(
      isSectionedView = true,
      gridCanScrollBackward = true,
      gridIndex = 4,
      gridOffset = 80,
      listCanScrollBackward = false,
      listIndex = 0,
      listOffset = 0
    ))

    // Section list view scrolled down -> should be false
    assertFalse(checkIsAtTop(
      isSectionedView = true,
      gridCanScrollBackward = false,
      gridIndex = 0,
      gridOffset = 0,
      listCanScrollBackward = true,
      listIndex = 3,
      listOffset = 20
    ))
  }

  @Test
  fun testRestingDownwardDragIgnoredAtZeroProgress() {
    val layerTransitionProgress = 0f
    val dragAmountDown = 50f // user dragging downwards

    val shouldIgnore = layerTransitionProgress <= 0.001f && dragAmountDown > 0f
    assertTrue("Downward drag when resting at 0f must be ignored", shouldIgnore)

    val dragAmountUp = -50f // user dragging upwards
    val shouldIgnoreUp = layerTransitionProgress <= 0.001f && dragAmountUp > 0f
    assertFalse("Upward drag when resting at 0f must NOT be ignored", shouldIgnoreUp)
  }
}
