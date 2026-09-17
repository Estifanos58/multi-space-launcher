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
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.SpaceDockItem
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.appIdentity

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

fun LayoutCoordinates.rootToLocal(rootPosition: Offset): Offset {
  val rootOrigin = positionInRoot()
  return Offset(rootPosition.x - rootOrigin.x, rootPosition.y - rootOrigin.y)
}

enum class DragLifecycleState {
  IDLE,
  PRESSED_ACTION_VISIBLE,
  DRAGGING,
  DROP,
  CANCEL
}

/**
 * Unified, deterministic drag and drop state coordinator for MultiSpace Launcher.
 * Bridges Layer 1 curated desktop and bottom SpaceDockBar into a unified coordinate system
 * and formal interaction lifecycle.
 */
class UnifiedDragState(
  val eventTracer: com.multispace.presentation.events.LauncherEventTracer = com.multispace.presentation.events.DefaultLauncherEventTracer.Global
) {
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

  // Desktop drag and interaction lifecycle state
  var isDropping by mutableStateOf(false)
  var activeActionPlacement by mutableStateOf<SpaceItemPlacement?>(null)
  var pendingDragPlacement by mutableStateOf<SpaceItemPlacement?>(null)
  var localPointerPos by mutableStateOf(Offset.Zero)
  var touchOffsetWithinItem by mutableStateOf(Offset.Zero)
  var targetHoverPlacement by mutableStateOf<SpaceItemPlacement?>(null)
  var previewTargetSlot by mutableStateOf<Int?>(null)
  var accumulatedDragDistance by mutableStateOf(0f)

  /**
   * Formal deterministic interaction lifecycle:
   * Idle -> Pressing -> ContextMenu -> Dragging -> Dropping -> Idle
   */
  val formalInteractionState: LauncherInteractionState
    get() = when {
      lifecycleState == DragLifecycleState.DROP -> LauncherInteractionState.Dropping(
        identity = currentAppIdentity,
        target = resolveDropTarget(rootPointerPos)
      )
      isDragging -> LauncherInteractionState.Dragging(
        identity = currentAppIdentity,
        origin = currentDragOrigin,
        currentPosition = rootPointerPos,
        itemId = currentItemId,
        packageName = currentPackageName
      )
      lifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE -> LauncherInteractionState.ContextMenu(
        identity = currentAppIdentity,
        position = rootPointerPos,
        itemId = currentItemId
      )
      else -> LauncherInteractionState.Idle
    }

  /**
   * Maps current drag lifecycle and dragging state to the authoritative [LauncherInteractionState].
   * Retained for full backward-compatibility with existing tests.
   */
  val interactionState: LauncherInteractionState
    get() = when {
      isDragging -> LauncherInteractionState.DraggingApp(
        itemId = currentItemId,
        packageName = currentPackageName
      )
      lifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE -> LauncherInteractionState.LongPressingApp(
        itemId = currentItemId
      )
      else -> LauncherInteractionState.Idle
    }

  val currentItemId: String?
    get() = draggedPlacement?.id ?: draggedDockItem?.id

  val currentPackageName: String?
    get() = draggedPlacement?.packageName ?: draggedDockItem?.packageName ?: draggedApp?.packageName

  val currentAppIdentity: AppIdentity?
    get() = draggedApp?.appIdentity ?: draggedPlacement?.toAppIdentity() ?: draggedDockItem?.toAppIdentity()

  val currentDragOrigin: DragOrigin
    get() = when (dragSource) {
      DragSource.LAYER1_DESKTOP -> DragOrigin.Desktop(
        pageIndex = targetDesktopPage,
        positionIndex = targetDesktopPosition,
        placementId = draggedPlacement?.id
      )
      DragSource.DOCK_BAR -> DragOrigin.Dock(
        index = targetDockIndex,
        dockItemId = draggedDockItem?.id
      )
    }

  // Coordinate tracking for cross-component hit testing
  var rootCoordinates by mutableStateOf<LayoutCoordinates?>(null)
  var layer1Coordinates by mutableStateOf<LayoutCoordinates?>(null)
  var dockCoordinates by mutableStateOf<LayoutCoordinates?>(null)
  var binBoundsInRoot by mutableStateOf<Rect?>(null)

  fun reset() {
    if (lifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE) {
      eventTracer.record(com.multispace.presentation.events.LauncherEvent.ActionMenuDismissed())
    }
    lifecycleState = DragLifecycleState.IDLE
    isDragging = false
    dragSource = DragSource.LAYER1_DESKTOP
    draggedPlacement = null
    draggedDockItem = null
    draggedApp = null
    rootPointerPos = Offset.Zero
    localPointerPos = Offset.Zero
    touchOffsetInItem = Offset.Zero
    touchOffsetWithinItem = Offset.Zero
    currentTargetZone = DragTargetZone.NONE
    targetDockIndex = -1
    isOverDock = false
    isDockFull = false
    targetDesktopPosition = -1
    isOverBin = false
    activeActionPlacement = null
    pendingDragPlacement = null
    targetHoverPlacement = null
    previewTargetSlot = null
    isDropping = false
    accumulatedDragDistance = 0f
  }

  fun dismissActions() {
    activeActionPlacement = null
    pendingDragPlacement = null
    if (lifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE) {
      lifecycleState = DragLifecycleState.IDLE
      eventTracer.record(com.multispace.presentation.events.LauncherEvent.ActionMenuDismissed())
    }
  }

  fun startPressing(identity: AppIdentity, startPos: Offset, itemId: String? = null) {
    lifecycleState = DragLifecycleState.IDLE
    rootPointerPos = startPos
  }

  fun showContextMenu(identity: AppIdentity, pos: Offset, itemId: String? = null) {
    lifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
    rootPointerPos = pos
    eventTracer.record(
      com.multispace.presentation.events.LauncherEvent.ActionMenuOpened(
        identity = identity,
        position = pos,
        itemId = itemId
      )
    )
  }

  fun startItemLongPress(
    placement: SpaceItemPlacement,
    touchOffset: Offset = Offset.Zero,
    localOffset: Offset = Offset.Zero,
    rootOffset: Offset = Offset.Zero
  ) {
    lifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
    activeActionPlacement = placement
    pendingDragPlacement = placement
    draggedPlacement = placement
    touchOffsetInItem = touchOffset
    touchOffsetWithinItem = touchOffset
    if (localOffset != Offset.Zero) localPointerPos = localOffset
    if (rootOffset != Offset.Zero) rootPointerPos = rootOffset
    accumulatedDragDistance = 0f
    eventTracer.record(
      com.multispace.presentation.events.LauncherEvent.ActionMenuOpened(
        identity = placement.toAppIdentity(),
        position = if (rootOffset != Offset.Zero) rootOffset else rootPointerPos,
        itemId = placement.id
      )
    )
  }

  fun startDesktopDrag(
    placement: SpaceItemPlacement,
    app: DiscoveredApp? = null,
    pointerPos: Offset = Offset.Zero,
    touchOffset: Offset = Offset.Zero,
    localPos: Offset = Offset.Zero
  ) {
    lifecycleState = DragLifecycleState.DRAGGING
    isDragging = true
    isDropping = false
    activeActionPlacement = null
    pendingDragPlacement = null
    dragSource = DragSource.LAYER1_DESKTOP
    draggedPlacement = placement
    draggedApp = app
    rootPointerPos = pointerPos
    touchOffsetInItem = touchOffset
    touchOffsetWithinItem = touchOffset
    localPointerPos = if (localPos != Offset.Zero) localPos else pointerPos
    currentTargetZone = DragTargetZone.DESKTOP
    targetHoverPlacement = null
    previewTargetSlot = null
    isOverBin = false
    accumulatedDragDistance = 0f
    eventTracer.record(
      com.multispace.presentation.events.LauncherEvent.DragStarted(
        identity = currentAppIdentity,
        origin = currentDragOrigin,
        initialPosition = pointerPos,
        itemId = placement.id,
        packageName = placement.packageName
      )
    )
  }

  fun startDockDrag(
    dockItem: SpaceDockItem,
    app: DiscoveredApp? = null,
    pointerPos: Offset = Offset.Zero,
    touchOffset: Offset = Offset.Zero
  ) {
    lifecycleState = DragLifecycleState.DRAGGING
    isDragging = true
    dragSource = DragSource.DOCK_BAR
    draggedDockItem = dockItem
    draggedApp = app
    rootPointerPos = pointerPos
    touchOffsetInItem = touchOffset
    currentTargetZone = DragTargetZone.DOCK_BAR
    eventTracer.record(
      com.multispace.presentation.events.LauncherEvent.DragStarted(
        identity = currentAppIdentity,
        origin = currentDragOrigin,
        initialPosition = pointerPos,
        itemId = dockItem.id,
        packageName = dockItem.packageName
      )
    )
  }

  fun updatePointerPosition(pos: Offset, localPos: Offset = pos) {
    rootPointerPos = pos
    localPointerPos = localPos
    isOverDock = isPointerOverDock(pos)
    isOverBin = isPointerOverBin(pos)
    val prevZone = currentTargetZone
    currentTargetZone = when {
      isOverDock -> DragTargetZone.DOCK_BAR
      isPointerOverDesktop(pos) -> DragTargetZone.DESKTOP
      else -> DragTargetZone.NONE
    }
    if (prevZone != currentTargetZone) {
      eventTracer.record(
        com.multispace.presentation.events.LauncherEvent.DragTargetZoneChanged(
          previousZone = prevZone,
          newZone = currentTargetZone
        )
      )
    }
  }

  fun cancelDrag() {
    val identity = currentAppIdentity
    val origin = currentDragOrigin
    isDragging = false
    dragSource = DragSource.LAYER1_DESKTOP
    draggedPlacement = null
    draggedDockItem = null
    draggedApp = null
    rootPointerPos = Offset.Zero
    localPointerPos = Offset.Zero
    touchOffsetInItem = Offset.Zero
    touchOffsetWithinItem = Offset.Zero
    currentTargetZone = DragTargetZone.NONE
    targetDockIndex = -1
    isOverDock = false
    isDockFull = false
    targetDesktopPage = 0
    targetDesktopPosition = -1
    isOverBin = false
    activeActionPlacement = null
    pendingDragPlacement = null
    targetHoverPlacement = null
    previewTargetSlot = null
    isDropping = false
    accumulatedDragDistance = 0f
    lifecycleState = DragLifecycleState.CANCEL
    eventTracer.record(
      com.multispace.presentation.events.LauncherEvent.DragCancelled(
        identity = identity,
        origin = origin
      )
    )
  }

  fun finishDrop(): DropTarget {
    val target = resolveDropTarget(rootPointerPos)
    val identity = currentAppIdentity
    isDragging = false
    dragSource = DragSource.LAYER1_DESKTOP
    draggedPlacement = null
    draggedDockItem = null
    draggedApp = null
    rootPointerPos = Offset.Zero
    localPointerPos = Offset.Zero
    touchOffsetInItem = Offset.Zero
    touchOffsetWithinItem = Offset.Zero
    currentTargetZone = DragTargetZone.NONE
    targetDockIndex = -1
    isOverDock = false
    isDockFull = false
    targetDesktopPage = 0
    targetDesktopPosition = -1
    isOverBin = false
    activeActionPlacement = null
    pendingDragPlacement = null
    targetHoverPlacement = null
    previewTargetSlot = null
    isDropping = false
    accumulatedDragDistance = 0f
    lifecycleState = DragLifecycleState.DROP
    eventTracer.record(
      com.multispace.presentation.events.LauncherEvent.DragDropped(
        identity = identity,
        target = target
      )
    )
    return target
  }

  fun resolveDropTarget(rootPos: Offset): DropTarget {
    return when {
      isOverBin || isPointerOverBin(rootPos) -> DropTarget.Bin
      isOverDock || isPointerOverDock(rootPos) -> DropTarget.DockSlot(targetDockIndex)
      targetDesktopPosition >= 0 -> DropTarget.DesktopCell(targetDesktopPage, targetDesktopPosition)
      else -> DropTarget.None
    }
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

  fun isPointerOverBin(rootPos: Offset): Boolean {
    val binBounds = binBoundsInRoot ?: return false
    return binBounds.contains(rootPos)
  }

  fun toRootPosition(localOffset: Offset, coordinates: LayoutCoordinates): Offset {
    return if (coordinates.isAttached) coordinates.positionInRoot() + localOffset else localOffset
  }

  fun toLocalPosition(rootOffset: Offset, coordinates: LayoutCoordinates): Offset {
    return if (coordinates.isAttached) rootOffset - coordinates.positionInRoot() else rootOffset
  }
}

fun LayoutCoordinates.localToRoot(offset: Offset): Offset =
  if (isAttached) positionInRoot() + offset else offset

private fun SpaceItemPlacement.toAppIdentity(): AppIdentity? {
  val pkg = packageName ?: return null
  return AppIdentity(
    packageName = pkg,
    componentName = componentName ?: "",
    userHandleId = userHandleId
  )
}

private fun SpaceDockItem.toAppIdentity(): AppIdentity {
  return AppIdentity(
    packageName = packageName,
    componentName = componentName,
    userHandleId = userHandleId
  )
}
