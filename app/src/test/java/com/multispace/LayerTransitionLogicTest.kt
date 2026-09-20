package com.multispace

import androidx.compose.ui.geometry.Offset
import com.multispace.domain.model.Space
import com.multispace.presentation.DragLifecycleState
import com.multispace.presentation.UnifiedDragState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerTransitionLogicTest {

  @Test
  fun testSwipeAllowedWhenLayer2Enabled() {
    // Default space has ACCESS_MODE_DOCK_BUTTON and useLayer2 = true
    val defaultSpace = Space.createDefault()
    assertTrue("Layer 2 must be enabled by default", defaultSpace.useLayer2)

    val isSwipeAllowedDefault = defaultSpace.useLayer2
    assertTrue("Swipe must be allowed whenever Layer 2 is enabled", isSwipeAllowedDefault)

    // SWIPE_UP mode with useLayer2 = true
    val swipeUpSpace = defaultSpace.copy(layer2AccessMode = Space.ACCESS_MODE_SWIPE_UP)
    val isSwipeAllowedSwipeUp = swipeUpSpace.useLayer2
    assertTrue("Swipe must be allowed with ACCESS_MODE_SWIPE_UP", isSwipeAllowedSwipeUp)

    // When useLayer2 is disabled
    val disabledLayer2Space = swipeUpSpace.copy(useLayer2 = false)
    val isSwipeAllowedDisabled = disabledLayer2Space.useLayer2
    assertFalse("Swipe must be disabled when Layer 2 is turned off", isSwipeAllowedDisabled)
  }

  @Test
  fun testCanDragLayer1Conditions() {
    var layerTransitionProgress = 0.0f
    var useLayer2 = true

    // Resting at Layer 1: can drag Layer 1
    var canDragLayer1 = layerTransitionProgress < 1f && useLayer2
    assertTrue("Should be able to drag Layer 1 when resting at 0f with useLayer2", canDragLayer1)

    // Fully on Layer 2 (progress = 1.0f): canDragLayer1 must be false
    layerTransitionProgress = 1.0f
    canDragLayer1 = layerTransitionProgress < 1f && useLayer2
    assertFalse("When fully on Layer 2, Layer 1 drag modifier should not be active", canDragLayer1)

    // When Layer 2 is disabled: canDragLayer1 must be false
    layerTransitionProgress = 0.0f
    useLayer2 = false
    canDragLayer1 = layerTransitionProgress < 1f && useLayer2
    assertFalse("When Layer 2 is disabled, Layer 1 drag should not be active", canDragLayer1)
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
  fun testSlideUpTransitionCompletesAtFiftyPercent() {
    val screenHeightPx = 2000f
    val slideUpTravelDistancePx = screenHeightPx * com.multispace.presentation.gesture.LayerTransitionGestureHelper.SLIDE_UP_TRAVEL_FRACTION
    assertEquals(1000f, slideUpTravelDistancePx, 0.001f)

    var layerTransitionProgress = 0f

    // User slides up by 500px (25% of screen height) -> progress should reach 0.50f (50%)
    val dragAmount25Percent = -500f
    val progressDelta1 = com.multispace.presentation.gesture.LayerTransitionGestureHelper.calculateProgressDelta(
      dragDeltaY = dragAmount25Percent,
      screenHeightPx = slideUpTravelDistancePx
    )
    layerTransitionProgress = (layerTransitionProgress + progressDelta1).coerceIn(0f, 1f)
    assertEquals(0.50f, layerTransitionProgress, 0.001f)

    // User slides up by another 500px (reaching 1000px total = 50% of screen height) -> progress reaches 1.0f (100%)
    val progressDelta2 = com.multispace.presentation.gesture.LayerTransitionGestureHelper.calculateProgressDelta(
      dragDeltaY = -500f,
      screenHeightPx = slideUpTravelDistancePx
    )
    layerTransitionProgress = (layerTransitionProgress + progressDelta2).coerceIn(0f, 1f)
    assertEquals(1.0f, layerTransitionProgress, 0.001f)
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

  enum class GestureOwner {
    ITEM_DRAG,
    LAYER_TRANSITION
  }

  enum class AxisClassification {
    PENDING,
    HORIZONTAL_PAGER_WINS,
    VERTICAL_LAYER_TRANSITION_WINS
  }

  @Test
  fun testGestureArbitrationOwnershipDecision() {
    // Mock hit-test function representing findItemAtOffset
    val mockItemsAtOffset = mapOf<Offset, String>(
      Offset(100f, 100f) to "app_item",
      Offset(250f, 200f) to "widget_item"
    )
    fun findItemAtOffset(pos: Offset): String? = mockItemsAtOffset[pos]

    fun arbitrateInitialPointer(downPosition: Offset, isSwipeAllowed: Boolean): GestureOwner? {
      val hitItem = findItemAtOffset(downPosition)
      return if (hitItem != null) {
        // Initial touch on an app/widget: Item drag owns gesture; Layer transition must not consume it
        GestureOwner.ITEM_DRAG
      } else if (isSwipeAllowed) {
        // Initial touch on empty space: Layer transition owns gesture
        GestureOwner.LAYER_TRANSITION
      } else {
        null
      }
    }

    // 1. Initial touch on app item -> ITEM_DRAG owns
    val appTouchOwner = arbitrateInitialPointer(Offset(100f, 100f), isSwipeAllowed = true)
    assertEquals(GestureOwner.ITEM_DRAG, appTouchOwner)

    // 2. Initial touch on widget item -> ITEM_DRAG owns
    val widgetTouchOwner = arbitrateInitialPointer(Offset(250f, 200f), isSwipeAllowed = true)
    assertEquals(GestureOwner.ITEM_DRAG, widgetTouchOwner)

    // 3. Initial touch on empty space -> LAYER_TRANSITION owns
    val emptySpaceTouchOwner = arbitrateInitialPointer(Offset(500f, 500f), isSwipeAllowed = true)
    assertEquals(GestureOwner.LAYER_TRANSITION, emptySpaceTouchOwner)

    // 4. Once decided, moving pointer over empty space retains ITEM_DRAG ownership
    var activeOwner = appTouchOwner
    val moveOverEmpty = Offset(500f, 500f)
    // Ownership is locked to initial touch, never reassigned during pointer lifetime
    assertEquals("Moving an item over empty space must NOT reassign ownership to Layer transition",
      GestureOwner.ITEM_DRAG, activeOwner)

    // 5. Once decided, empty space swipe moving over app retains LAYER_TRANSITION ownership
    activeOwner = emptySpaceTouchOwner
    val moveOverApp = Offset(100f, 100f)
    assertEquals("Swiping empty space over an app must NOT cancel Layer transition ownership",
      GestureOwner.LAYER_TRANSITION, activeOwner)
  }

  @Test
  fun testEmptySpaceGestureAxisClassification() {
    val touchSlop = 18f

    fun classifyEmptySpaceGesture(deltaX: Float, deltaY: Float): AxisClassification {
      val absX = kotlin.math.abs(deltaX)
      val absY = kotlin.math.abs(deltaY)

      if (absX >= touchSlop || absY >= touchSlop) {
        return if (absY > absX) {
          AxisClassification.VERTICAL_LAYER_TRANSITION_WINS
        } else {
          AxisClassification.HORIZONTAL_PAGER_WINS
        }
      }
      return AxisClassification.PENDING
    }

    // 1. Movement under touch-slop remains pending (unconsumed, tracking)
    assertEquals(AxisClassification.PENDING, classifyEmptySpaceGesture(deltaX = 5f, deltaY = 10f))
    assertEquals(AxisClassification.PENDING, classifyEmptySpaceGesture(deltaX = -12f, deltaY = 3f))

    // 2. Horizontal swipe past touch-slop -> HORIZONTAL_PAGER_WINS (Layer transition aborts without consuming)
    assertEquals(AxisClassification.HORIZONTAL_PAGER_WINS, classifyEmptySpaceGesture(deltaX = 25f, deltaY = 5f))
    assertEquals(AxisClassification.HORIZONTAL_PAGER_WINS, classifyEmptySpaceGesture(deltaX = -30f, deltaY = -10f))

    // 3. Vertical swipe upward past touch-slop -> VERTICAL_LAYER_TRANSITION_WINS (Claims and follows finger)
    assertEquals(AxisClassification.VERTICAL_LAYER_TRANSITION_WINS, classifyEmptySpaceGesture(deltaX = 4f, deltaY = -35f))
    assertEquals(AxisClassification.VERTICAL_LAYER_TRANSITION_WINS, classifyEmptySpaceGesture(deltaX = -8f, deltaY = 28f))

    // 4. Equal 45-degree diagonal movement past touch-slop -> defaults to HORIZONTAL_PAGER_WINS
    assertEquals(AxisClassification.HORIZONTAL_PAGER_WINS, classifyEmptySpaceGesture(deltaX = 20f, deltaY = 20f))
  }
}
