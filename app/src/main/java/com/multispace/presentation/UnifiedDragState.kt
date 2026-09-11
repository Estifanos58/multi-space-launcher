package com.multispace.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceDockItem
import com.multispace.domain.model.SpaceItemPlacement

enum class DragLifecycleState {
  IDLE,
  PRESSED_ACTION_VISIBLE,
  DRAGGING,
  DROP,
  CANCEL
}

enum class DragSource {
  LAYER1_DESKTOP,
  DOCK_BAR
}

enum class DragTargetZone {
  NONE,
  DESKTOP,
  DOCK_BAR,
  REMOVE_BIN
}

class UnifiedDragState {
  var lifecycleState by mutableStateOf(DragLifecycleState.IDLE)
  var isDragging by mutableStateOf(false)
  var dragSource by mutableStateOf(DragSource.LAYER1_DESKTOP)

  // Data being dragged
  var draggedPlacement by mutableStateOf<SpaceItemPlacement?>(null)
  var draggedDockItem by mutableStateOf<SpaceDockItem?>(null)
  var draggedApp by mutableStateOf<DiscoveredApp?>(null)

  // Pointer position in Root (window) coordinates & touch offset within icon
  var rootPointerPos by mutableStateOf(Offset.Zero)
  var touchOffsetInItem by mutableStateOf(Offset.Zero)

  // Active target zone
  var currentTargetZone by mutableStateOf(DragTargetZone.NONE)

  // Dock target slot (-1 for end of dock)
  var targetDockIndex by mutableIntStateOf(-1)
  var isOverDock by mutableStateOf(false)
  var isDockFull by mutableStateOf(false)

  // Desktop target slot
  var targetDesktopPage by mutableIntStateOf(0)
  var targetDesktopPosition by mutableIntStateOf(-1)

  // Trash / Removal Bin
  var isOverBin by mutableStateOf(false)

  // Coordinate tracking for cross-component hit testing
  var rootCoordinates by mutableStateOf<LayoutCoordinates?>(null)
  var layer1Coordinates by mutableStateOf<LayoutCoordinates?>(null)
  var dockCoordinates by mutableStateOf<LayoutCoordinates?>(null)
  var binBoundsInRoot by mutableStateOf<Rect?>(null)

  fun reset() {
    lifecycleState = DragLifecycleState.IDLE
    isDragging = false
    dragSource = DragSource.LAYER1_DESKTOP
    draggedPlacement = null
    draggedDockItem = null
    draggedApp = null
    rootPointerPos = Offset.Zero
    touchOffsetInItem = Offset.Zero
    currentTargetZone = DragTargetZone.NONE
    targetDockIndex = -1
    isOverDock = false
    isDockFull = false
    targetDesktopPosition = -1
    isOverBin = false
  }

  fun isPointerOverDock(rootPos: Offset): Boolean {
    val dockCoords = dockCoordinates ?: return false
    if (!dockCoords.isAttached) return false
    val bounds = dockCoords.boundsInRoot()
    // Generous top boundary for smooth snapping into the dock from above
    return rootPos.x >= bounds.left && rootPos.x <= bounds.right &&
        rootPos.y >= (bounds.top - 36f) && rootPos.y <= (bounds.bottom + 36f)
  }

  fun isPointerOverDesktop(rootPos: Offset): Boolean {
    val l1Coords = layer1Coordinates ?: return false
    if (!l1Coords.isAttached) return false
    val bounds = l1Coords.boundsInRoot()
    return bounds.contains(rootPos)
  }
}

fun LayoutCoordinates.localToRoot(offset: Offset): Offset =
  if (isAttached) positionInRoot() + offset else offset

fun LayoutCoordinates.rootToLocal(offset: Offset): Offset =
  if (isAttached) offset - positionInRoot() else offset

