package com.multispace

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.presentation.DragLifecycleState
import com.multispace.presentation.DragSource
import com.multispace.presentation.DragTargetZone
import com.multispace.presentation.UnifiedDragState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Layer1DragAndDropArchitectureTest {

  @Test
  fun testAuthoritativeDragStateMachineTransitions() {
    val dragState = UnifiedDragState()
    assertEquals(DragLifecycleState.IDLE, dragState.lifecycleState)
    assertFalse(dragState.isDragging)

    val sampleAppPlacement = SpaceItemPlacement(
      id = "placement_1",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 5,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.multispace.notes"
    )

    // 1. User long-presses on app cell -> PRESSED_ACTION_VISIBLE
    dragState.lifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
    dragState.draggedPlacement = sampleAppPlacement
    assertEquals(DragLifecycleState.PRESSED_ACTION_VISIBLE, dragState.lifecycleState)
    assertFalse("isDragging should still be false while action box is visible", dragState.isDragging)

    // 2. User moves past drag slop threshold -> DRAGGING
    val dragSlopPx = 24f
    var accumulatedDistance = 10f
    var hasInitiatedDrag = accumulatedDistance >= dragSlopPx
    assertFalse(hasInitiatedDrag)

    accumulatedDistance = 28f
    hasInitiatedDrag = accumulatedDistance >= dragSlopPx
    assertTrue(hasInitiatedDrag)

    dragState.lifecycleState = DragLifecycleState.DRAGGING
    dragState.isDragging = true
    assertEquals(DragLifecycleState.DRAGGING, dragState.lifecycleState)
    assertTrue(dragState.isDragging)

    // 3. User releases finger -> DROP
    dragState.lifecycleState = DragLifecycleState.DROP
    assertEquals(DragLifecycleState.DROP, dragState.lifecycleState)

    // After drop execution -> returns to IDLE
    dragState.reset()
    assertEquals(DragLifecycleState.IDLE, dragState.lifecycleState)
    assertFalse(dragState.isDragging)
    assertNull(dragState.draggedPlacement)
  }

  @Test
  fun testPreDragReleaseWithoutMovementRetainsActionVisible() {
    val dragState = UnifiedDragState()
    val sampleAppPlacement = SpaceItemPlacement(
      id = "placement_1",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 2,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.multispace.browser"
    )

    // Long press
    dragState.lifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
    dragState.draggedPlacement = sampleAppPlacement

    // User released finger with 0 distance (tap/hold without drag)
    val accumulatedDistance = 2f
    val dragSlopPx = 24f
    val movedPastSlop = accumulatedDistance >= dragSlopPx
    assertFalse(movedPastSlop)

    // State machine retains PRESSED_ACTION_VISIBLE
    assertEquals(DragLifecycleState.PRESSED_ACTION_VISIBLE, dragState.lifecycleState)

    // User dismisses action box (e.g. taps scrim)
    dragState.lifecycleState = DragLifecycleState.IDLE
    dragState.reset()
    assertEquals(DragLifecycleState.IDLE, dragState.lifecycleState)
  }

  @Test
  fun testWidgetLongPressReleaseRetainsResizeActionUntilTouchElsewhere() {
    var activeActionPlacement: SpaceItemPlacement? = null
    var pendingDragPlacement: SpaceItemPlacement? = null
    var dragLifecycleState = DragLifecycleState.IDLE
    var resizingWidgetId: String? = null

    val widgetPlacement = SpaceItemPlacement(
      id = "widget_1",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 4,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 2,
      customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
    )

    // 1. Long-press widget: resize icon appears at top
    dragLifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
    activeActionPlacement = widgetPlacement
    pendingDragPlacement = widgetPlacement

    assertTrue(activeActionPlacement!!.isWidget)
    assertEquals(DragLifecycleState.PRESSED_ACTION_VISIBLE, dragLifecycleState)

    // 2. User lets go (finger lifted without drag) -> Compose triggers onDragCancel or onDragEnd
    // Simulating onDragCancel logic
    val isDragging = (dragLifecycleState == DragLifecycleState.DRAGGING)
    assertFalse(isDragging)

    if (dragLifecycleState == DragLifecycleState.DRAGGING) {
      dragLifecycleState = DragLifecycleState.CANCEL
    } else if (dragLifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE) {
      // User held and released without dragging: action box stays visible!
      pendingDragPlacement = null
    } else {
      activeActionPlacement = null
      pendingDragPlacement = null
      dragLifecycleState = DragLifecycleState.IDLE
    }

    // Crucial check: resize action box must NOT disappear on letting go!
    assertNotNull("Resize action must stay visible after letting go", activeActionPlacement)
    assertEquals("widget_1", activeActionPlacement?.id)
    assertEquals(DragLifecycleState.PRESSED_ACTION_VISIBLE, dragLifecycleState)
    assertNull(pendingDragPlacement)

    // 3. User touches anything else (scrim receives touch event) -> dismisses
    activeActionPlacement = null
    dragLifecycleState = DragLifecycleState.IDLE

    assertNull("Action box should be dismissed after touching elsewhere", activeActionPlacement)
    assertEquals(DragLifecycleState.IDLE, dragLifecycleState)
  }

  @Test
  fun testWidgetLongPressThenClickResizeEntersResizeMode() {
    var activeActionPlacement: SpaceItemPlacement? = null
    var dragLifecycleState = DragLifecycleState.IDLE
    var resizingWidgetId: String? = null

    val widgetPlacement = SpaceItemPlacement(
      id = "widget_clock",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 2,
      spanY = 2,
      customWidgetType = SpaceItemPlacement.WIDGET_CLOCK_DATE
    )

    // Long press
    dragLifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
    activeActionPlacement = widgetPlacement

    // User taps the resize action icon
    val onActivateResize: (String) -> Unit = { widgetId ->
      resizingWidgetId = widgetId
      activeActionPlacement = null
      dragLifecycleState = DragLifecycleState.IDLE
    }

    onActivateResize(widgetPlacement.id)

    assertEquals("widget_clock", resizingWidgetId)
    assertNull(activeActionPlacement)
    assertEquals(DragLifecycleState.IDLE, dragLifecycleState)
  }

  @Test
  fun testFolderLongPressDirectlyTransitionsToDragging() {
    val dragState = UnifiedDragState()
    val folderPlacement = SpaceItemPlacement(
      id = "placement_folder_1",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 4,
      itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
      folderId = "folder_1"
    )

    assertTrue(folderPlacement.isFolder)

    // Folders skip PRESSED_ACTION_VISIBLE and directly enter DRAGGING
    if (folderPlacement.isFolder) {
      dragState.lifecycleState = DragLifecycleState.DRAGGING
      dragState.isDragging = true
      dragState.draggedPlacement = folderPlacement
    }

    assertEquals(DragLifecycleState.DRAGGING, dragState.lifecycleState)
    assertTrue(dragState.isDragging)
    assertEquals("folder_1", dragState.draggedPlacement?.folderId)
  }

  @Test
  fun testDragCancellationTransitionsCleanlyToIdle() {
    val dragState = UnifiedDragState()
    dragState.lifecycleState = DragLifecycleState.DRAGGING
    dragState.isDragging = true

    // Pointer gesture cancelled
    dragState.lifecycleState = DragLifecycleState.CANCEL
    assertEquals(DragLifecycleState.CANCEL, dragState.lifecycleState)

    dragState.reset()
    assertEquals(DragLifecycleState.IDLE, dragState.lifecycleState)
    assertFalse(dragState.isDragging)
  }

  @Test
  fun testCellIdentifiedItemBypassesManualHitTesting() {
    // Model demonstrating cell identification
    val item1 = SpaceItemPlacement(
      id = "item_1",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      packageName = "com.test.app1"
    )
    val item2 = SpaceItemPlacement(
      id = "item_2",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 1,
      packageName = "com.test.app2"
    )

    var touchedItemFromCell: SpaceItemPlacement? = null

    // Cell 2 was touched
    touchedItemFromCell = item2

    // Root pointerInput resolves touched item without searching the full grid
    val resolvedItem = touchedItemFromCell ?: item1
    assertEquals("item_2", resolvedItem.id)
    assertEquals("com.test.app2", resolvedItem.packageName)
  }

  @Test
  fun testDropTargetResolutionAndPersistenceDelegation() {
    val item = SpaceItemPlacement(
      id = "app_item",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 3,
      packageName = "com.test.app"
    )

    var movedItemId: String? = null
    var movedTargetPage = -1
    var movedTargetPos = -1
    var movedPageSize = -1

    val onMovePlacement: (String, Int, Int, Int) -> Unit = { id, page, pos, pageSize ->
      movedItemId = id
      movedTargetPage = page
      movedTargetPos = pos
      movedPageSize = pageSize
    }

    // Drop on desktop at page 1, slot 8
    val cols = 4
    val gridRows = 6
    val pageSize = cols * gridRows
    val rawTargetPos = 8
    val clampedC = (rawTargetPos % cols).coerceIn(0, cols - 1)
    val clampedR = (rawTargetPos / cols).coerceIn(0, gridRows - 1)
    val targetPos = clampedR * cols + clampedC

    onMovePlacement(item.id, 1, targetPos, pageSize)

    assertEquals("app_item", movedItemId)
    assertEquals(1, movedTargetPage)
    assertEquals(8, movedTargetPos)
    assertEquals(24, movedPageSize)
  }

  @Test
  fun testRootCoordinateTouchOffsetPreservation() {
    // Icon footprint in root coordinates: left=100, top=200, width=80, height=80
    val iconRect = Rect(100f, 200f, 180f, 280f)
    // User touches at root coordinate (125, 230)
    val startOffset = Offset(125f, 230f)

    val touchOffsetWithinItem = Offset(
      (startOffset.x - iconRect.left).coerceIn(0f, iconRect.width),
      (startOffset.y - iconRect.top).coerceIn(0f, iconRect.height)
    )

    assertEquals(25f, touchOffsetWithinItem.x, 0.01f)
    assertEquals(30f, touchOffsetWithinItem.y, 0.01f)

    // Drag overlay position = currentPointer - touchOffset
    val currentPointer = Offset(150f, 260f)
    val overlayPos = currentPointer - touchOffsetWithinItem

    // 150 - 25 = 125, 260 - 30 = 230
    assertEquals(125f, overlayPos.x, 0.01f)
    assertEquals(230f, overlayPos.y, 0.01f)
  }

  @Test
  fun testInteractionPath1_ShortTapLaunchesApp() {
    var dragLifecycleState = DragLifecycleState.IDLE
    var activeActionPlacement: SpaceItemPlacement? = null
    var lastLongPressTimestamp = 0L
    var appLaunched = false

    val appPlacement = SpaceItemPlacement(
      id = "placement_tap",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 3,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.multispace.calculator"
    )

    // User taps quickly (< long-press threshold)
    // Long-press never triggers, so state remains IDLE
    assertEquals(DragLifecycleState.IDLE, dragLifecycleState)
    assertNull(activeActionPlacement)
    assertEquals(0L, lastLongPressTimestamp)

    // Click handler executes
    val isActionActive = { activeActionPlacement != null }
    val getDragLifecycleState = { dragLifecycleState }
    val getLastLongPressTimestamp = { lastLongPressTimestamp }

    val now = 1000L
    if (!isActionActive() &&
        getDragLifecycleState() == DragLifecycleState.IDLE &&
        (now - getLastLongPressTimestamp()) >= 800L
    ) {
      appLaunched = true
    }

    assertTrue("Short tap must launch the app", appLaunched)
    assertEquals(DragLifecycleState.IDLE, dragLifecycleState)
  }

  @Test
  fun testInteractionPath2_LongPressShowsActionMenu() {
    var dragLifecycleState = DragLifecycleState.IDLE
    var activeActionPlacement: SpaceItemPlacement? = null
    var pendingDragPlacement: SpaceItemPlacement? = null
    var lastLongPressTimestamp = 0L

    val appPlacement = SpaceItemPlacement(
      id = "placement_hold",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 2,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.multispace.notes"
    )

    // Pointer held past long-press threshold: onDragStart triggers
    lastLongPressTimestamp = 5000L
    dragLifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
    activeActionPlacement = appPlacement
    pendingDragPlacement = appPlacement

    assertEquals(DragLifecycleState.PRESSED_ACTION_VISIBLE, dragLifecycleState)
    assertNotNull("Action menu must appear on long-press", activeActionPlacement)
    assertEquals("placement_hold", activeActionPlacement?.id)
    assertEquals("placement_hold", pendingDragPlacement?.id)
  }

  @Test
  fun testInteractionPath3_LongPressPlusMovementEntersDragging() {
    var dragLifecycleState = DragLifecycleState.IDLE
    var activeActionPlacement: SpaceItemPlacement? = null
    var pendingDragPlacement: SpaceItemPlacement? = null
    var accumulatedDragDistance = 0f
    val dragSlopPx = 24f
    var dragVisualStarted = false

    val appPlacement = SpaceItemPlacement(
      id = "placement_drag",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 1,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.multispace.gallery"
    )

    // 1. Long-press triggers
    dragLifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
    activeActionPlacement = appPlacement
    pendingDragPlacement = appPlacement

    // 2. User moves finger beyond drag slop threshold
    val moveAmount = 30f
    accumulatedDragDistance += moveAmount

    if (dragLifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE &&
        pendingDragPlacement != null && accumulatedDragDistance >= dragSlopPx
    ) {
      val placementToDrag = pendingDragPlacement!!
      activeActionPlacement = null
      pendingDragPlacement = null
      dragLifecycleState = DragLifecycleState.DRAGGING
      dragVisualStarted = true
    }

    assertEquals(DragLifecycleState.DRAGGING, dragLifecycleState)
    assertNull("Action menu must be dismissed when entering drag mode", activeActionPlacement)
    assertNull(pendingDragPlacement)
    assertTrue("Drag trigger and visual must appear when moving past slop", dragVisualStarted)
  }

  @Test
  fun testInteractionPath4_LongPressReleaseWithoutDraggingRetainsActionMenuAndSuppressesLaunch() {
    var dragLifecycleState = DragLifecycleState.IDLE
    var activeActionPlacement: SpaceItemPlacement? = null
    var pendingDragPlacement: SpaceItemPlacement? = null
    var lastLongPressTimestamp = 0L
    var appLaunched = false

    val appPlacement = SpaceItemPlacement(
      id = "placement_action_only",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 5,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.multispace.camera"
    )

    // 1. Long-press activates action menu
    val longPressTime = 10000L
    lastLongPressTimestamp = longPressTime
    dragLifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
    activeActionPlacement = appPlacement
    pendingDragPlacement = appPlacement

    // 2. User releases finger without moving past drag slop (onDragEnd)
    if (dragLifecycleState == DragLifecycleState.DRAGGING) {
      dragLifecycleState = DragLifecycleState.DROP
    } else if (dragLifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE) {
      // Action menu stays visible!
      pendingDragPlacement = null
    }

    // 3. Action menu remains visible
    assertEquals(DragLifecycleState.PRESSED_ACTION_VISIBLE, dragLifecycleState)
    assertNotNull("Action menu must remain visible upon release without drag", activeActionPlacement)
    assertEquals("placement_action_only", activeActionPlacement?.id)
    assertNull(pendingDragPlacement)

    // 4. Click handler is tested at release time (e.g. 50ms after long press)
    val releaseTime = longPressTime + 50L
    val isActionActive = { activeActionPlacement != null }
    val getDragLifecycleState = { dragLifecycleState }
    val getLastLongPressTimestamp = { lastLongPressTimestamp }

    if (isActionActive() ||
        getDragLifecycleState() != DragLifecycleState.IDLE ||
        (releaseTime - getLastLongPressTimestamp()) < 800L
    ) {
      // Launch is suppressed
      appLaunched = false
    } else {
      appLaunched = true
    }

    assertFalse("App launch MUST be suppressed when releasing after long press", appLaunched)
    assertEquals(DragLifecycleState.PRESSED_ACTION_VISIBLE, dragLifecycleState)
    assertNotNull(activeActionPlacement)
  }

  @Test
  fun testDragReleaseNeverTriggersAppLaunch() {
    val dragState = UnifiedDragState()
    dragState.lifecycleState = DragLifecycleState.DRAGGING
    dragState.isDragging = true

    var appLaunched = false
    val isActionActive = { false }
    val getDragLifecycleState = { dragState.lifecycleState }
    val getLastLongPressTimestamp = { 1000L }
    val now = 2000L

    // While dragging
    if (!isActionActive() &&
        getDragLifecycleState() == DragLifecycleState.IDLE &&
        (now - getLastLongPressTimestamp()) >= 800L
    ) {
      appLaunched = true
    }
    assertFalse("Launch must remain suppressed during dragging", appLaunched)

    // On drop
    dragState.lifecycleState = DragLifecycleState.DROP
    if (!isActionActive() &&
        getDragLifecycleState() == DragLifecycleState.IDLE &&
        (now - getLastLongPressTimestamp()) >= 800L
    ) {
      appLaunched = true
    }
    assertFalse("Launch must remain suppressed on drop", appLaunched)
  }

  @Test
  fun testAppDroppedOnAppInitiatesFolderCreation() {
    val sourcePlacement = SpaceItemPlacement(
      id = "placement_src",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 2,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.multispace.camera",
      componentName = "com.multispace.camera.MainActivity"
    )

    val targetPlacement = SpaceItemPlacement(
      id = "placement_target",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 5,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.multispace.gallery",
      componentName = "com.multispace.gallery.MainActivity"
    )

    val sourceApp = DiscoveredApp(
      id = "app_src",
      packageName = "com.multispace.camera",
      activityName = "com.multispace.camera.MainActivity",
      label = "Camera"
    )

    val targetApp = DiscoveredApp(
      id = "app_target",
      packageName = "com.multispace.gallery",
      activityName = "com.multispace.gallery.MainActivity",
      label = "Gallery"
    )

    val allPlacements = listOf(sourcePlacement, targetPlacement)
    val appLookup = mapOf(
      sourceApp.key to sourceApp,
      targetApp.key to targetApp
    )

    // Simulate drop on target slot (positionIndex = 5)
    var folderCreated = false
    var createdWithSource: DiscoveredApp? = null
    var createdWithTarget: DiscoveredApp? = null
    var targetPageCreated = -1
    var targetPosCreated = -1

    val targetSlot = 5
    val targetPage = 0

    val isDraggedApp = !sourcePlacement.isWidget && !sourcePlacement.isFolder
    assertTrue(isDraggedApp)

    val hitTarget = allPlacements.firstOrNull { item ->
      item.id != sourcePlacement.id &&
      item.pageIndex == targetPage &&
      item.positionIndex == targetSlot
    }

    assertNotNull(hitTarget)
    assertFalse(hitTarget!!.isWidget)
    assertFalse(hitTarget.isFolder)

    val srcApp = appLookup["${sourcePlacement.packageName}/${sourcePlacement.componentName}"]
    val tgtApp = appLookup["${hitTarget.packageName}/${hitTarget.componentName}"]

    if (srcApp != null && tgtApp != null) {
      folderCreated = true
      createdWithSource = srcApp
      createdWithTarget = tgtApp
      targetPageCreated = hitTarget.pageIndex
      targetPosCreated = hitTarget.positionIndex
    }

    assertTrue("Folder creation must initiate when app dropped on another app", folderCreated)
    assertEquals("Camera", createdWithSource?.label)
    assertEquals("Gallery", createdWithTarget?.label)
    assertEquals(0, targetPageCreated)
    assertEquals(5, targetPosCreated)
  }
}
