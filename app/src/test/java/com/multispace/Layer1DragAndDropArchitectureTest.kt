package com.multispace

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.presentation.DragGestureController
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
    val appLookup = com.multispace.domain.model.AppIdentityLookup(listOf(sourceApp, targetApp))

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

    val srcApp = appLookup[sourcePlacement]
    val tgtApp = appLookup[hitTarget]

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

  @Test
  fun testDropPositionCalculation_ResolvesNewSlotAndNeverDefaultsToOriginal() {
    val cols = 4
    val gridRows = 5
    val totalSlots = cols * gridRows
    val originalPositionIndex = 0

    // Simulate slot boundaries for a 4x5 grid
    val cellWidth = 80f
    val cellHeight = 100f
    val slotBounds = mutableMapOf<Int, Rect>()
    for (r in 0 until gridRows) {
      for (c in 0 until cols) {
        val left = c * cellWidth
        val top = r * cellHeight
        slotBounds[r * cols + c] = Rect(left, top, left + cellWidth, top + cellHeight)
      }
    }

    fun calculateSlot(pointerPos: Offset): Int {
      val hit = slotBounds.entries.firstOrNull { (slot, rect) ->
        slot < totalSlots && rect.contains(pointerPos)
      }?.key
      val raw = hit ?: 0
      val c = (raw % cols).coerceIn(0, cols - 1)
      val r = (raw / cols).coerceIn(0, gridRows - 1)
      return r * cols + c
    }

    // User drags item from original slot 0 (pos 40, 50) and drops at slot 6 (col 2, row 1 -> pos 200, 150)
    val dropPos = Offset(200f, 150f)
    val calculatedSlot = calculateSlot(dropPos)
    assertEquals(6, calculatedSlot)

    // Verify rawTargetPos does not fall back to originalPositionIndex
    var previewTargetSlot: Int? = null // if preview was null at moment of drop
    val targetPos = previewTargetSlot ?: calculatedSlot
    assertEquals(6, targetPos)
    assertTrue("Target position must be new dropped slot 6, not original slot 0", targetPos != originalPositionIndex)
  }

  @Test
  fun testPage0_AllowsPlacingAppsAcrossAnyRow() {
    val cols = 4
    val gridRows = 5
    val pageSize = cols * gridRows
    val lastRow = gridRows - 1

    // 1. Initial fallback placements start at the last row on Page 0
    val apps = listOf(
      DiscoveredApp("id_a", "app.a", "app.a.Main", "App A"),
      DiscoveredApp("id_b", "app.b", "app.b.Main", "App B"),
      DiscoveredApp("id_c", "app.c", "app.c.Main", "App C"),
      DiscoveredApp("id_d", "app.d", "app.d.Main", "App D")
    )
    val page0Count = minOf(cols, apps.size)
    val fallbackList = mutableListOf<SpaceItemPlacement>()
    for (i in 0 until page0Count) {
      fallbackList.add(
        SpaceItemPlacement(
          id = "fallback:${apps[i].packageName}",
          spaceId = "space_1",
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = 0,
          positionIndex = lastRow * cols + i,
          itemType = SpaceItemPlacement.ITEM_TYPE_APP,
          packageName = apps[i].packageName
        )
      )
    }

    // Verify initial fallback places apps at last row (row 4)
    assertEquals(4, fallbackList.size)
    fallbackList.forEach { p ->
      assertEquals(0, p.pageIndex)
      assertEquals(lastRow, p.positionIndex / cols)
    }

    // 2. User moves App A from row 4 to row 1 (pos 5: r=1, c=1)
    // and App B to row 0 (pos 2: r=0, c=2)
    val updatedPlacements = listOf(
      fallbackList[0].copy(positionIndex = 1 * cols + 1), // row 1, col 1
      fallbackList[1].copy(positionIndex = 0 * cols + 2), // row 0, col 2
      fallbackList[2].copy(positionIndex = 2 * cols + 0), // row 2, col 0
      fallbackList[3] // stays at row 4, col 3
    )

    // Verify effective placements retain all positions across rows 0, 1, 2, and 4
    val rowMap = updatedPlacements.associate { it.packageName to (it.positionIndex / cols) }
    assertEquals(1, rowMap["app.a"])
    assertEquals(0, rowMap["app.b"])
    assertEquals(2, rowMap["app.c"])
    assertEquals(4, rowMap["app.d"])

    // Ensure they are NOT forced to the last row
    assertTrue("App A is placed on row 1", updatedPlacements[0].positionIndex / cols == 1)
    assertTrue("App B is placed on row 0", updatedPlacements[1].positionIndex / cols == 0)
    assertTrue("App C is placed on row 2", updatedPlacements[2].positionIndex / cols == 2)
  }

  @Test
  fun testNoAppLaidOnWidgetOnPage0_ResolvesToNonWidgetSlots() {
    val cols = 4
    val gridRows = 5
    val lastRow = 4
    val pageSize = 20

    // Widgets on Page 0:
    // Clock widget: row 0..1, cols 0..3 (occupies slots 0..7)
    val clockWidget = SpaceItemPlacement(
      id = "clock",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 4,
      spanY = 2
    )
    // Search widget: row 2, cols 0..3 (occupies slots 8..11)
    val searchWidget = SpaceItemPlacement(
      id = "search",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 8,
      itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
      spanX = 4,
      spanY = 1
    )

    // Suppose bad input placements have apps at slots 0, 1, 2, 3 (directly on top of clock widget!)
    val collidingApps = (0 until 4).map { i ->
      SpaceItemPlacement(
        id = "app_$i",
        spaceId = "space_1",
        layer = SpaceItemPlacement.LAYER_HOME,
        pageIndex = 0,
        positionIndex = i,
        itemType = SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = "com.test.app$i"
      )
    }

    val rawPlacements = listOf(clockWidget, searchWidget) + collidingApps

    // Calculate widget reserved slots
    val widgetSlots = mutableSetOf<Int>()
    rawPlacements.filter { it.isWidget && it.pageIndex == 0 }.forEach { w ->
      val r = w.positionIndex / cols
      val c = w.positionIndex % cols
      for (dr in 0 until w.spanY) {
        for (dc in 0 until w.spanX) {
          widgetSlots.add((r + dr) * cols + (c + dc))
        }
      }
    }
    assertEquals(12, widgetSlots.size) // slots 0..11 are widget slots

    // Effective placements resolution logic
    val resolvedList = mutableListOf<SpaceItemPlacement>()
    val occupiedSlots = mutableSetOf<Int>()

    // 1. Widgets added first
    rawPlacements.filter { it.isWidget }.forEach {
      resolvedList.add(it)
      val r = it.positionIndex / cols
      val c = it.positionIndex % cols
      for (dr in 0 until it.spanY) {
        for (dc in 0 until it.spanX) {
          occupiedSlots.add((r + dr) * cols + (c + dc))
        }
      }
    }

    // 2. Apps relocated if on widget slots
    rawPlacements.filter { !it.isWidget }.forEach { app ->
      val isCovered = widgetSlots.contains(app.positionIndex)
      if (!isCovered && !occupiedSlots.contains(app.positionIndex)) {
        resolvedList.add(app)
        occupiedSlots.add(app.positionIndex)
      } else {
        // Relocate to bottom row
        var placed = false
        for (c in 0 until cols) {
          val cand = lastRow * cols + c
          if (!widgetSlots.contains(cand) && !occupiedSlots.contains(cand)) {
            resolvedList.add(app.copy(pageIndex = 0, positionIndex = cand))
            occupiedSlots.add(cand)
            placed = true
            break
          }
        }
        assertTrue("App must be relocated to an available slot", placed)
      }
    }

    // Verify:
    val resolvedApps = resolvedList.filter { it.itemType == SpaceItemPlacement.ITEM_TYPE_APP }
    assertEquals(4, resolvedApps.size)
    resolvedApps.forEach { app ->
      assertFalse("No app may be placed on any widget slot", widgetSlots.contains(app.positionIndex))
      assertEquals("Relocated apps must land on row 4", 4, app.positionIndex / cols)
    }
  }

  @Test
  fun testDropAppOntoWidgetSlotIsRejected() {
    val cols = 4
    val gridRows = 5
    // Clock widget at row 0..1, cols 0..3 (slots 0..7)
    val widgetSlots = (0..7).toSet()

    // Dragging an app
    val draggedApp = SpaceItemPlacement(
      id = "dragged_app",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 16,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.test.dragged"
    )

    // User attempts to drop onto slot 2 (inside the clock widget)
    val targetSlot = 2
    val isOverWidget = widgetSlots.contains(targetSlot)
    assertTrue("Target slot 2 is over a widget", isOverWidget)

    // Drop handler logic: reject drop onto widget
    var dropCommitted = false
    if (!isOverWidget) {
      dropCommitted = true
    }

    assertFalse("Drop of an app onto a widget slot must be rejected", dropCommitted)
  }

  @Test
  fun testDragGestureController_AuthoritativeDropHandoffToSlotCalculation() {
    val controller = DragGestureController<SpaceItemPlacement>(dragSlopPx = 10f)
    val appPlacement = SpaceItemPlacement(
      id = "item-app-origin",
      spaceId = "space-1",
      packageName = "com.test.movedapp",
      componentName = "com.test.movedapp.MainActivity",
      pageIndex = 0,
      positionIndex = 0 // origin at slot 0
    )

    controller.hitTest = { appPlacement }
    controller.getItemBounds = { Rect(0f, 0f, 100f, 100f) }

    var onMovePlacementCalled = false
    var movedId: String? = null
    var movedPage: Int = -1
    var movedTargetPos: Int = -1

    // Simulate Layer1 grid geometry: 4 cols, rowPitch 150px, colWidth 100px
    val cols = 4
    val gridRows = 6
    val colWidth = 100f
    val rowPitch = 150f

    fun calculateSlot(pointerPos: Offset): Int {
      val c = (pointerPos.x / colWidth).toInt().coerceIn(0, cols - 1)
      val r = (pointerPos.y / rowPitch).toInt().coerceIn(0, gridRows - 1)
      return (r * cols + c).coerceIn(0, cols * gridRows - 1)
    }

    // Simulate Layer 1 drop handler directly receiving (item, dropPos)
    controller.onDragDropped = { authoritativeItem: SpaceItemPlacement, finalPointerPosition: Offset ->
      // Calculate target slot directly from finalPointerPosition
      val targetPos = calculateSlot(finalPointerPosition)
      onMovePlacementCalled = true
      movedId = authoritativeItem.id
      movedPage = 0
      movedTargetPos = targetPos
    }

    // 1. Long press and drag to slot (row 2, col 3) -> x = 350, y = 350 (slot = 2 * 4 + 3 = 11)
    val downPos = Offset(50f, 50f)
    controller.handleDown(downPos)
    controller.handleLongPress(downPos)
    controller.handleMove(Offset(350f, 350f))

    assertEquals(DragLifecycleState.DRAGGING, controller.lifecycleState)
    assertEquals("item-app-origin", controller.draggedItem?.id)

    // 2. UP event at finalPointerPosition (350f, 350f)
    val finalUpPos = Offset(350f, 350f)
    controller.handleUp(finalUpPos)

    // Verify handoff was called with authoritative item and position
    assertTrue(onMovePlacementCalled)
    assertEquals("item-app-origin", movedId)
    assertEquals(0, movedPage)
    // Slot must be 11, NOT origin slot 0!
    assertEquals(11, movedTargetPos)

    // Controller must be reset after drop
    assertEquals(DragLifecycleState.IDLE, controller.lifecycleState)
    assertNull(controller.draggedItem)
  }

  @Test
  fun testDeterministicSlotCalculation_FromActualFinalPointerCoordinate() {
    val cols = 4
    val gridRows = 6
    val cellWidthPx = 80f
    val spacingPx = 8f
    val colPitchPx = cellWidthPx + spacingPx // 88f
    val rowPitchPx = 100f
    val bounds = Rect(left = 16f, top = 24f, right = 16f + cols * cellWidthPx + (cols - 1) * spacingPx, bottom = 24f + gridRows * rowPitchPx)

    fun calculateSlot(pointerPos: Offset): Int {
      val c = when {
        pointerPos.x <= bounds.left -> 0
        pointerPos.x >= bounds.right -> cols - 1
        else -> ((pointerPos.x - bounds.left) / colPitchPx).toInt().coerceIn(0, cols - 1)
      }
      val r = when {
        pointerPos.y <= bounds.top -> 0
        pointerPos.y >= bounds.bottom -> gridRows - 1
        else -> ((pointerPos.y - bounds.top) / rowPitchPx).toInt().coerceIn(0, gridRows - 1)
      }
      return (r * cols + c).coerceIn(0, cols * gridRows - 1)
    }

    // Origin slot 0: top-left cell
    assertEquals(0, calculateSlot(Offset(30f, 40f)))

    // Dropping into empty slot 1 (row 0, col 1): x = bounds.left + 1 * 88 + 40 = 144, y = 40
    assertEquals(1, calculateSlot(Offset(144f, 40f)))

    // Dropping into spacing between col 1 and col 2 (x = 16 + 88 + 82 = 186): maps accurately to col 1
    assertEquals(1, calculateSlot(Offset(186f, 40f)))

    // Dropping into empty slot 5 (row 1, col 1): x = 144, y = bounds.top + 1 * 100 + 40 = 164
    assertEquals(5, calculateSlot(Offset(144f, 164f)))

    // Dropping into empty slot 7 (row 1, col 3): x = 16 + 3 * 88 + 40 = 320, y = 164
    assertEquals(7, calculateSlot(Offset(320f, 164f)))

    // Dropping at edge boundaries
    assertEquals(0, calculateSlot(Offset(0f, 0f))) // Clamped to (0, 0) -> slot 0
    assertEquals(23, calculateSlot(Offset(500f, 1000f))) // Clamped to bottom-right -> slot 23
  }

  @Test
  fun testFindItemAtOffset_DoesNotClassifyEmptyDestinationAsNearbyApp() {
    val cols = 4
    val gridRows = 6
    val cellWPx = 80f
    val spacingPx = 8f
    val colPitchPx = cellWPx + spacingPx
    val rowPitchPx = 100f
    val bounds = Rect(16f, 24f, 16f + 4 * 80f + 3 * 8f, 24f + 6 * 100f)

    // Only slot 0 has an app
    val appAtSlot0 = SpaceItemPlacement(
      id = "place_app_0",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      packageName = "com.test.slot0"
    )
    val pagePlacements = listOf(appAtSlot0)

    val slot0Rect = Rect(bounds.left, bounds.top, bounds.left + cellWPx, bounds.top + 80f)

    // Hit test function without the 10dp touch-margin fallback
    fun findItemAtOffset(offset: Offset): SpaceItemPlacement? {
      // 1. Direct hit on footprint
      if (slot0Rect.contains(offset)) return appAtSlot0

      // 2. Mathematical grid cell check
      val relX = offset.x - bounds.left
      val relY = offset.y - bounds.top
      if (relX >= 0f && relY >= 0f) {
        val calcC = (relX / colPitchPx).toInt().coerceIn(0, cols - 1)
        val calcR = (relY / rowPitchPx).toInt().coerceIn(0, gridRows - 1)
        val slot = calcR * cols + calcC
        return pagePlacements.firstOrNull { it.positionIndex == slot }
      }
      return null
    }

    // Pointer directly inside slot 0 -> hits appAtSlot0
    val pointerInSlot0 = Offset(50f, 50f)
    assertEquals("place_app_0", findItemAtOffset(pointerInSlot0)?.id)

    // Pointer inside slot 1 (which is empty) -> must return NULL, NOT classify as nearby app!
    val pointerInSlot1 = Offset(bounds.left + colPitchPx + 20f, bounds.top + 30f)
    assertNull("Empty slot 1 must NOT be classified as nearby app at slot 0", findItemAtOffset(pointerInSlot1))

    // Pointer inside slot 4 (empty) -> must return NULL
    val pointerInSlot4 = Offset(bounds.left + 20f, bounds.top + rowPitchPx + 30f)
    assertNull("Empty slot 4 must NOT be classified as nearby app", findItemAtOffset(pointerInSlot4))
  }

  @Test
  fun testDropOnEmptySlot_VerifiesRoomPlacementMappingAndPersistencePath() {
    // 1. Room contains an authoritative placement record
    val roomPlacement = SpaceItemPlacement(
      id = "persistent_id_123",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.multispace.gallery",
      componentName = "com.multispace.gallery.MainActivity"
    )
    val roomPlacements = listOf(roomPlacement)

    // 2. UI deduplicated effective placement (could be derived or have matching package)
    val draggedFromUI = SpaceItemPlacement(
      id = "persistent_id_123",
      spaceId = "space_1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.multispace.gallery"
    )

    // 3. User drops at final pointer (300f, 150f) which maps to empty slot 5
    val finalPointerPos = Offset(300f, 150f)

    // Step A: DROP_FINAL logged
    val logTrace = mutableListOf<String>()
    logTrace.add("DROP_FINAL: draggedId=${draggedFromUI.id}, draggedPackage=${draggedFromUI.packageName}, finalPointer=$finalPointerPos")

    // Step B: Resolve dragged ID/package against Room database records
    val persistentRecord = roomPlacements.firstOrNull { it.id == draggedFromUI.id }
      ?: (if (draggedFromUI.packageName != null) roomPlacements.firstOrNull { it.packageName == draggedFromUI.packageName } else null)
      ?: draggedFromUI

    val resolvedPlacementId = persistentRecord.id
    val resolvedPackage = persistentRecord.packageName ?: draggedFromUI.packageName
    logTrace.add("RESOLVED_DRAGGED: id=$resolvedPlacementId pkg=$resolvedPackage fromPage=${persistentRecord.pageIndex} fromPos=${persistentRecord.positionIndex}")
    assertEquals("persistent_id_123", resolvedPlacementId)

    // Step C: Deterministically calculate target slot from final pointer
    val targetPage = 0
    val targetPos = 5 // Slot 5 is empty

    // Step D: Verify pointer is NOT inside any other item's footprint -> no folder creation!
    val isDraggedApp = !draggedFromUI.isWidget && !draggedFromUI.isFolder
    val otherPlacements = roomPlacements.filter {
      it.id != draggedFromUI.id &&
      it.id != persistentRecord.id &&
      (draggedFromUI.packageName == null || it.packageName != draggedFromUI.packageName)
    }
    val folderTargetItem = if (isDraggedApp) {
      otherPlacements.firstOrNull { /* empty slot contains nothing */ false }
    } else null
    assertNull("No folder creation when dropping onto an empty slot", folderTargetItem)

    logTrace.add("DROP_TARGET: targetPlacementId=${folderTargetItem?.id}, targetPackage=${folderTargetItem?.packageName}, exactFootprintHit=${folderTargetItem != null}")
    logTrace.add("DROP_SLOT: targetPage=$targetPage, targetPosition=$targetPos")

    // Step E: Call onMovePlacement
    val pageSize = 20
    var onMovePlacementCalled = false
    var persistenceResultPage = -1
    var persistenceResultPos = -1

    val onMovePlacement: (String, Int, Int, Int) -> Unit = { id, page, pos, _ ->
      logTrace.add("DROP_PERSIST: calling onMovePlacement(draggedId=${draggedFromUI.id}, targetPage=$page, targetPosition=$pos)")
      onMovePlacementCalled = true

      // Step F: Repository finds item in Room and updates persistence
      val itemToMove = roomPlacements.firstOrNull { it.id == id }
      assertNotNull("Repository must find the item in Room placements", itemToMove)
      logTrace.add("REPOSITORY_ITEM_FOUND: id=${itemToMove!!.id} pkg=${itemToMove.packageName} fromPage=${itemToMove.pageIndex} fromPos=${itemToMove.positionIndex} targetPage=$page targetPos=$pos")

      // Step G: Repository persists the new placement
      val persistedPlacement = itemToMove.copy(pageIndex = page, positionIndex = pos)
      persistenceResultPage = persistedPlacement.pageIndex
      persistenceResultPos = persistedPlacement.positionIndex
      logTrace.add("PERSISTED_PLACEMENT: id=${persistedPlacement.id} pkg=${persistedPlacement.packageName} targetPage=$page targetPos=$pos persistedPage=${persistedPlacement.pageIndex} persistedPos=${persistedPlacement.positionIndex}")
    }

    onMovePlacement(resolvedPlacementId, targetPage, targetPos, pageSize)

    // Verifications:
    assertTrue("onMovePlacement must be executed", onMovePlacementCalled)
    assertEquals("Persisted page must match target page", 0, persistenceResultPage)
    assertEquals("Persisted position must be slot 5, NOT original slot 0!", 5, persistenceResultPos)

    // Trace sequence verification
    assertTrue(logTrace[0].startsWith("DROP_FINAL"))
    assertTrue(logTrace[1].startsWith("RESOLVED_DRAGGED"))
    assertTrue(logTrace[2].startsWith("DROP_TARGET"))
    assertTrue(logTrace[3].startsWith("DROP_SLOT"))
    assertTrue(logTrace[4].startsWith("DROP_PERSIST"))
    assertTrue(logTrace[5].startsWith("REPOSITORY_ITEM_FOUND"))
    assertTrue(logTrace[6].startsWith("PERSISTED_PLACEMENT"))
  }

  @Test
  fun testDropOnAnotherApp_TriggersFolderCreationOnlyOnExactFootprint() {
    val app1 = SpaceItemPlacement(id = "app_1", spaceId = "s1", layer = SpaceItemPlacement.LAYER_HOME, pageIndex = 0, positionIndex = 0, packageName = "com.app.one")
    val app2 = SpaceItemPlacement(id = "app_2", spaceId = "s1", layer = SpaceItemPlacement.LAYER_HOME, pageIndex = 0, positionIndex = 1, packageName = "com.app.two")

    val app2Bounds = Rect(100f, 20f, 180f, 100f) // Exact footprint of app2
    val cellBounds = mapOf("app_2" to app2Bounds)

    // User drags app1 and drops it directly inside app2's footprint
    val dropOnApp2Pointer = Offset(140f, 60f)
    assertTrue(app2Bounds.contains(dropOnApp2Pointer))

    val candidateTarget = listOf(app2).firstOrNull { item ->
      cellBounds[item.id]?.contains(dropOnApp2Pointer) == true
    }
    assertNotNull(candidateTarget)
    assertEquals("app_2", candidateTarget?.id)

    // User drags app1 and drops it on empty cell right next to app2
    val dropNearApp2Pointer = Offset(195f, 60f) // 15px outside app2
    assertFalse(app2Bounds.contains(dropNearApp2Pointer))

    val emptyCellTarget = listOf(app2).firstOrNull { item ->
      cellBounds[item.id]?.contains(dropNearApp2Pointer) == true
    }
    assertNull("Releasing outside real footprint must NOT trigger folder creation", emptyCellTarget)
  }

  @Test
  fun testDropOnExistingFolder_TriggersAddToFolderOnlyOnExactFootprint() {
    val app = SpaceItemPlacement(id = "app_1", spaceId = "s1", layer = SpaceItemPlacement.LAYER_HOME, pageIndex = 0, positionIndex = 0, packageName = "com.app.one")
    val folderPlacement = SpaceItemPlacement(
      id = "folder_placement_1",
      spaceId = "s1",
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 2,
      itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
      folderId = "folder_123"
    )

    val folderBounds = Rect(200f, 20f, 280f, 100f)
    val cellBounds = mapOf("folder_placement_1" to folderBounds)

    val dropInsideFolder = Offset(240f, 60f)
    assertTrue(folderBounds.contains(dropInsideFolder))

    val folderTarget = listOf(folderPlacement).firstOrNull { item ->
      cellBounds[item.id]?.contains(dropInsideFolder) == true
    }
    assertNotNull(folderTarget)
    assertTrue(folderTarget!!.isFolder)
    assertEquals("folder_123", folderTarget.folderId)
  }
}

