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
  fun testNormalAppTapAllowsLaunchInteraction() {
    val coordinator = LauncherInteractionCoordinator()
    assertTrue("Initial state must be Idle", coordinator.currentState is LauncherInteractionState.Idle)
    assertTrue("Normal app tap must allow launch when Idle", coordinator.canLaunchApp())
    assertTrue("Item interaction is allowed when Idle", coordinator.canStartItemInteraction())
    assertTrue("Horizontal paging is allowed when Idle", coordinator.canPageHorizontally())
    assertTrue("Layer transition is allowed when Idle", coordinator.canTransitionLayer())
  }

  @Test
  fun testLongPressEntersActionStateAndSuppressesLaunch() {
    val coordinator = LauncherInteractionCoordinator()
    val longPressAccepted = coordinator.toLongPressing("app-123")
    assertTrue("Long press should be accepted from Idle", longPressAccepted)
    assertTrue("State should be LongPressingApp", coordinator.currentState is LauncherInteractionState.LongPressingApp)
    assertEquals("app-123", (coordinator.currentState as LauncherInteractionState.LongPressingApp).itemId)

    // Invariant: launch must be suppressed in action state
    assertFalse("Normal app tap-to-launch must be suppressed while long pressing", coordinator.canLaunchApp())
    // However, dragging from long-press remains allowed
    assertTrue("Item interaction remains allowed from LongPressingApp", coordinator.canStartItemInteraction())
  }

  @Test
  fun testLongPressPlusMovementPastSlopTransitionsToDragging() {
    val coordinator = LauncherInteractionCoordinator()
    coordinator.toLongPressing("app-123")

    // Movement past slop transitions to DraggingApp
    val dragTransitionAccepted = coordinator.toDragging("app-123", "com.example.notes")
    assertTrue("Transition from long-press to dragging should succeed", dragTransitionAccepted)
    assertTrue("State should be DraggingApp", coordinator.currentState is LauncherInteractionState.DraggingApp)
    val draggingState = coordinator.currentState as LauncherInteractionState.DraggingApp
    assertEquals("app-123", draggingState.itemId)
    assertEquals("com.example.notes", draggingState.packageName)

    // Launch remains suppressed while dragging
    assertFalse("Tap-to-launch must be suppressed while dragging", coordinator.canLaunchApp())
  }

  @Test
  fun testDraggingBlocksLayerTransition() {
    val coordinator = LauncherInteractionCoordinator()
    coordinator.startDraggingApp("app-123", "com.example.notes")
    assertTrue("Must be in DraggingApp state", coordinator.currentState is LauncherInteractionState.DraggingApp)

    // Invariant: layer transition must be blocked while dragging
    assertFalse("Layer transition must be blocked while dragging", coordinator.canTransitionLayer())
    val transitionAttempt = coordinator.startTransition(fromLayer = 1)
    assertFalse("Transition attempt must be rejected while dragging", transitionAttempt)
    assertTrue("State must remain DraggingApp", coordinator.currentState is LauncherInteractionState.DraggingApp)
  }

  @Test
  fun testLayerTransitionBlocksAppDrag() {
    val coordinator = LauncherInteractionCoordinator()
    val transitionStarted = coordinator.startTransition(fromLayer = 1)
    assertTrue(transitionStarted)
    assertTrue("Must be in TransitioningLayer state", coordinator.currentState is LauncherInteractionState.TransitioningLayer)

    // Invariant: app drag and long-press must be blocked during layer transition
    assertFalse("Item interaction must be blocked during layer transition", coordinator.canStartItemInteraction())
    val dragAttempt = coordinator.startDraggingApp("app-123", "com.example.notes")
    assertFalse("App drag attempt must be blocked during layer transition", dragAttempt)

    val longPressAttempt = coordinator.toLongPressing("app-123")
    assertFalse("Long press attempt must be blocked during layer transition", longPressAttempt)

    assertTrue("State must remain TransitioningLayer", coordinator.currentState is LauncherInteractionState.TransitioningLayer)
  }

  @Test
  fun testLayerTransitionBlocksHorizontalPageInteraction() {
    val coordinator = LauncherInteractionCoordinator()
    coordinator.startTransition(fromLayer = 1)
    assertTrue(coordinator.currentState is LauncherInteractionState.TransitioningLayer)

    // Invariant: horizontal paging must be blocked during layer transition
    assertFalse("Horizontal paging must be blocked on coordinator", coordinator.canPageHorizontally())
    val isScrollEnabled = HorizontalPageGesturePolicy.isScrollEnabled(
      interactionState = coordinator.currentState,
      isDragging = false,
      isDropping = false,
      dragLifecycleState = DragLifecycleState.IDLE
    )
    assertFalse("Horizontal page scrolling must be disabled via policy during layer transition", isScrollEnabled)
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

  @Test
  fun testFormalInteractionStateLifecycleTransitions() {
    val coordinator = LauncherInteractionCoordinator()
    assertTrue("Starts in Idle", coordinator.currentState is LauncherInteractionState.Idle)

    val identity = com.multispace.domain.model.AppIdentity("com.example.test", ".MainActivity", 0L)
    val startPos = androidx.compose.ui.geometry.Offset(100f, 200f)

    // 1. Idle -> Pressing
    val pressed = coordinator.toPressing(identity, startPos, "item_1")
    assertTrue("Should transition to Pressing", pressed)
    assertTrue("Current state is Pressing", coordinator.currentState is LauncherInteractionState.Pressing)
    val pressingState = coordinator.currentState as LauncherInteractionState.Pressing
    assertEquals(identity, pressingState.identity)
    assertEquals(startPos, pressingState.startPosition)
    assertEquals("item_1", pressingState.itemId)

    // 2. Pressing -> ContextMenu
    val contextMenu = coordinator.toContextMenu(identity, startPos, "item_1")
    assertTrue("Should transition to ContextMenu", contextMenu)
    assertTrue("Current state is ContextMenu", coordinator.currentState is LauncherInteractionState.ContextMenu)
    val menuState = coordinator.currentState as LauncherInteractionState.ContextMenu
    assertEquals(identity, menuState.identity)
    assertEquals(startPos, menuState.position)

    // 3. ContextMenu -> Dragging
    val dragOrigin = com.multispace.presentation.DragOrigin.Desktop(0, 3, "item_1")
    val dragPos = androidx.compose.ui.geometry.Offset(150f, 300f)
    val dragging = coordinator.toDragging(identity, dragOrigin, dragPos, "item_1", "com.example.test")
    assertTrue("Should transition to Dragging", dragging)
    assertTrue("Current state is Dragging", coordinator.currentState is LauncherInteractionState.Dragging)
    val dragState = coordinator.currentState as LauncherInteractionState.Dragging
    assertEquals(identity, dragState.identity)
    assertEquals(dragOrigin, dragState.origin)
    assertEquals(dragPos, dragState.currentPosition)

    // 4. Dragging -> Dropping
    val dropTarget = com.multispace.presentation.DropTarget.DockSlot(2)
    val dropping = coordinator.toDropping(identity, dropTarget)
    assertTrue("Should transition to Dropping", dropping)
    assertTrue("Current state is Dropping", coordinator.currentState is LauncherInteractionState.Dropping)
    val dropState = coordinator.currentState as LauncherInteractionState.Dropping
    assertEquals(identity, dropState.identity)
    assertEquals(dropTarget, dropState.target)

    // 5. Dropping -> Idle
    coordinator.toIdle()
    assertTrue("Returns to Idle", coordinator.currentState is LauncherInteractionState.Idle)
  }

  @Test
  fun testUnifiedDragStateFormalLifecycleAndTargetResolution() {
    val dragState = com.multispace.presentation.UnifiedDragState()
    assertEquals(LauncherInteractionState.Idle, dragState.formalInteractionState)

    val placement = com.multispace.domain.model.SpaceItemPlacement(
      id = "p1",
      spaceId = "space1",
      layer = 1,
      pageIndex = 0,
      positionIndex = 2,
      itemType = com.multispace.domain.model.SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.test.app",
      componentName = ".Main",
      userHandleId = 0L
    )

    // 1. Long press item -> ContextMenu
    dragState.startItemLongPress(placement, androidx.compose.ui.geometry.Offset(10f, 10f))
    assertTrue("Formal state is ContextMenu", dragState.formalInteractionState is LauncherInteractionState.ContextMenu)

    // 2. Start drag -> Dragging
    dragState.startDesktopDrag(placement, pointerPos = androidx.compose.ui.geometry.Offset(100f, 200f))
    assertTrue("Formal state is Dragging", dragState.formalInteractionState is LauncherInteractionState.Dragging)
    val dragging = dragState.formalInteractionState as LauncherInteractionState.Dragging
    assertEquals("p1", dragging.itemId)
    assertEquals("com.test.app", dragging.packageName)

    // Target resolution: Desktop Cell
    dragState.targetDesktopPage = 0
    dragState.targetDesktopPosition = 4
    val desktopTarget = dragState.resolveDropTarget(androidx.compose.ui.geometry.Offset(100f, 200f))
    assertEquals(com.multispace.presentation.DropTarget.DesktopCell(0, 4), desktopTarget)

    // Target resolution: Dock Slot
    dragState.isOverDock = true
    dragState.targetDockIndex = 1
    val dockTarget = dragState.resolveDropTarget(androidx.compose.ui.geometry.Offset(100f, 200f))
    assertEquals(com.multispace.presentation.DropTarget.DockSlot(1), dockTarget)

    // Target resolution: Bin
    dragState.isOverBin = true
    val binTarget = dragState.resolveDropTarget(androidx.compose.ui.geometry.Offset(100f, 200f))
    assertEquals(com.multispace.presentation.DropTarget.Bin, binTarget)

    // 3. Drop
    val finalTarget = dragState.finishDrop()
    assertEquals(com.multispace.presentation.DropTarget.Bin, finalTarget)
    assertTrue("Formal state is Dropping", dragState.formalInteractionState is LauncherInteractionState.Dropping)

    // Reset to Idle
    dragState.reset()
    assertEquals(LauncherInteractionState.Idle, dragState.formalInteractionState)
  }
}
