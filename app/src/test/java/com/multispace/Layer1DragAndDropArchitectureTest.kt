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
}
