package com.multispace

import com.multispace.presentation.DragLifecycleState
import com.multispace.presentation.LauncherInteractionCoordinator
import com.multispace.presentation.LauncherInteractionState
import com.multispace.presentation.gesture.HorizontalPageGesturePolicy
import com.multispace.presentation.gesture.LayerTransitionGestureHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherInteractionStateTest {

  @Test
  fun testCoordinatorEnforcesMutualExclusivityBetweenLayerTransitionAndDrag() {
    val coordinator = LauncherInteractionCoordinator()
    assertTrue("Coordinator starts in Idle", coordinator.currentState is LauncherInteractionState.Idle)
    assertTrue("Can transition layer when Idle", coordinator.canTransitionLayer())
    assertTrue("Can drag item when Idle", coordinator.canStartItemInteraction())

    // 1. Start layer transition
    val transitionStarted = coordinator.startTransition(fromLayer = 1)
    assertTrue("Transition should start successfully", transitionStarted)
    assertTrue("State is TransitioningLayer", coordinator.currentState is LauncherInteractionState.TransitioningLayer)

    // Invariant check: Cannot drag or long-press while layer transition is active
    assertFalse("Cannot drag while transitioning", coordinator.canStartItemInteraction())
    val dragAttempt = coordinator.startDraggingApp("item1", "com.example.app")
    assertFalse("Drag start must be rejected while transitioning", dragAttempt)

    // Complete transition
    coordinator.finishTransition()
    assertTrue("State returns to Idle after finishTransition", coordinator.currentState is LauncherInteractionState.Idle)

    // 2. Start app drag
    val dragStarted = coordinator.startDraggingApp("item1", "com.example.app")
    assertTrue("Drag should start successfully from Idle", dragStarted)
    assertTrue("State is DraggingApp", coordinator.currentState is LauncherInteractionState.DraggingApp)

    // Invariant check: Cannot transition layers while dragging an app
    assertFalse("Cannot transition layer while dragging app", coordinator.canTransitionLayer())
    val transitionAttempt = coordinator.startTransition(fromLayer = 1)
    assertFalse("Transition attempt must be rejected while dragging app", transitionAttempt)

    // End drag
    coordinator.endDragging()
    assertTrue("State returns to Idle after ending drag", coordinator.currentState is LauncherInteractionState.Idle)
    assertTrue("Can transition layer again after drag ends", coordinator.canTransitionLayer())
  }

  @Test
  fun testHorizontalPageGesturePolicyEnforcesExclusivity() {
    // 1. Idle state -> page scroll enabled
    val idleEnabled = HorizontalPageGesturePolicy.isScrollEnabled(
      interactionState = LauncherInteractionState.Idle,
      isDragging = false,
      isDropping = false,
      dragLifecycleState = DragLifecycleState.IDLE
    )
    assertTrue("Horizontal page swipe must be enabled when Idle and not dragging", idleEnabled)

    // 2. Dragging state -> page scroll disabled
    val draggingEnabled = HorizontalPageGesturePolicy.isScrollEnabled(
      interactionState = LauncherInteractionState.DraggingApp("item1", "pkg"),
      isDragging = true,
      isDropping = false,
      dragLifecycleState = DragLifecycleState.DRAGGING
    )
    assertFalse("Horizontal page swipe must be disabled while dragging", draggingEnabled)

    // 3. Layer transition state -> page scroll disabled
    val transitionEnabled = HorizontalPageGesturePolicy.isScrollEnabled(
      interactionState = LauncherInteractionState.TransitioningLayer(1),
      isDragging = false,
      isDropping = false,
      dragLifecycleState = DragLifecycleState.IDLE
    )
    assertFalse("Horizontal page swipe must be disabled during layer transition", transitionEnabled)

    // 4. Dropping animation in progress -> page scroll disabled
    val droppingEnabled = HorizontalPageGesturePolicy.isScrollEnabled(
      interactionState = LauncherInteractionState.Idle,
      isDragging = false,
      isDropping = true,
      dragLifecycleState = DragLifecycleState.IDLE
    )
    assertFalse("Horizontal page swipe must be disabled while item is dropping", droppingEnabled)
  }

  @Test
  fun testLayerTransitionSettlePhysicsCalculation() {
    // Upward fling (negative velocity) snaps to Layer 2 (1.0f)
    val upwardFlingTarget = LayerTransitionGestureHelper.calculateSettleTarget(
      currentProgress = 0.15f,
      velocityY = -800f,
      flingThresholdPx = 500f
    )
    assertEquals(1.0f, upwardFlingTarget, 0.001f)

    // Downward fling (positive velocity) snaps back to Layer 1 (0.0f)
    val downwardFlingTarget = LayerTransitionGestureHelper.calculateSettleTarget(
      currentProgress = 0.85f,
      velocityY = 800f,
      flingThresholdPx = 500f
    )
    assertEquals(0.0f, downwardFlingTarget, 0.001f)

    // Below threshold (0.40f) without velocity snaps to 0.0f
    val lowProgressNoVelocityTarget = LayerTransitionGestureHelper.calculateSettleTarget(
      currentProgress = 0.35f,
      velocityY = 0f,
      flingThresholdPx = 500f
    )
    assertEquals(0.0f, lowProgressNoVelocityTarget, 0.001f)

    // Above threshold (0.40f) without velocity snaps to 1.0f
    val highProgressNoVelocityTarget = LayerTransitionGestureHelper.calculateSettleTarget(
      currentProgress = 0.42f,
      velocityY = 0f,
      flingThresholdPx = 500f
    )
    assertEquals(1.0f, highProgressNoVelocityTarget, 0.001f)
  }

  @Test
  fun testProgressDeltaAndVelocityHelpers() {
    val screenHeightPx = 1000f

    // Upward drag of 200px (dragAmount = -200f in Compose) -> +0.20 progress
    val delta = LayerTransitionGestureHelper.calculateProgressDelta(
      dragDeltaY = -200f,
      screenHeightPx = screenHeightPx
    )
    assertEquals(0.20f, delta, 0.001f)

    // Upward fling with velocity -1500px/s -> +1.5 progress velocity
    val velocity = LayerTransitionGestureHelper.calculateProgressVelocity(
      velocityY = -1500f,
      screenHeightPx = screenHeightPx
    )
    assertEquals(1.5f, velocity, 0.001f)
  }
}
