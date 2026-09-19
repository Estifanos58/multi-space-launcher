package com.multispace.presentation.drag

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.presentation.DragLifecycleState
import com.multispace.presentation.DragSource
import com.multispace.presentation.DragTargetZone
import com.multispace.presentation.UnifiedDragState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Direction for horizontal edge paging during drag-and-drop.
 */
enum class EdgePagingDirection {
  NONE,
  LEFT,
  RIGHT
}

/**
 * State of edge trigger to prevent rapid repetitive paging while finger stays at edge.
 */
enum class EdgeTriggerState {
  IDLE,
  ARMED,
  CONSUMED
}

/**
 * Reusable Drag & Drop Controller for Launcher Desktop & Catalog surfaces.
 *
 * Encapsulates:
 * 1. Authoritative drag lifecycle state machine (IDLE -> PRESSED_ACTION_VISIBLE -> DRAGGING -> DROP / CANCEL).
 * 2. Spatial coordinate resolution, slot bounds calculation, and multi-span footprint hit-testing.
 * 3. Pre-drag quick actions (App Info, Uninstall / Force Stop, Resize).
 * 4. Movement transition past drag slop into active dragging.
 * 5. Screen edge dwelling and automatic page navigation.
 * 6. Multi-target drop resolution: desktop grid reordering, folder creation, folder addition, dock bar drop, and trash bin drop.
 */
class DesktopDragManager(
  private val coroutineScope: CoroutineScope,
  var hapticFeedback: HapticFeedback? = null,
  var unifiedDragState: UnifiedDragState? = null,
  // Configuration
  var gridColumns: Int = 4,
  var gridRows: Int = 6,
  var isScrollMode: Boolean = false,
  var density: Density = Density(1f),
  // Edge dwell timing
  var edgeDwellDelayMs: Long = 650L,
  var pageTransitionDurationMs: Int = 300
) {
  // --- Observable UI States ---
  var lifecycleState by mutableStateOf(DragLifecycleState.IDLE)
  val isDragging: Boolean get() = lifecycleState == DragLifecycleState.DRAGGING

  var draggedPlacement by mutableStateOf<SpaceItemPlacement?>(null)
  var activeActionPlacement by mutableStateOf<SpaceItemPlacement?>(null)
  var pendingDragPlacement by mutableStateOf<SpaceItemPlacement?>(null)
  var pendingDragStartOffset by mutableStateOf(Offset.Zero)

  var currentPointerPos by mutableStateOf(Offset.Zero)
  var touchOffsetWithinItem by mutableStateOf(Offset.Zero)
  var previewTargetSlot by mutableStateOf<Int?>(null)
  var targetHoverPlacement by mutableStateOf<SpaceItemPlacement?>(null)
  var isOverBin by mutableStateOf(false)

  var extraPagesCount by mutableIntStateOf(0)
  var resizingWidgetId by mutableStateOf<String?>(null)
  var lastLongPressTimestamp by mutableStateOf(0L)

  // Item hit directly from cell touch
  var touchedItemFromCell: SpaceItemPlacement? = null
  var touchedItemRootOffset: Offset = Offset.Zero

  // --- Geometry & Spatial Layout ---
  var rootCoordinates: LayoutCoordinates? = null
  var pagerCoordinates: LayoutCoordinates? = null
  var pageGridBounds by mutableStateOf<Rect?>(null)
  var viewportWidth by mutableFloatStateOf(0f)
  var viewportHeight by mutableFloatStateOf(0f)

  val cellBounds = mutableStateMapOf<String, Rect>()
  val slotBounds = mutableStateMapOf<Int, Rect>()
  var binBounds: Rect? = null

  // Dynamic dimension metrics (in pixels)
  var cellWidthPx: Float = 0f
  var cellHeightPx: Float = 0f
  var appSpacingPx: Float = 0f
  var rowPitchPx: Float = 0f
  var dragSlopPx: Float = 24f
  var edgeZonePx: Float = 100f
  var actionBoxBounds by mutableStateOf<Rect?>(null)

  // --- Edge Paging Machine ---
  var activeEdgeZone by mutableStateOf(EdgePagingDirection.NONE)
  var edgeTriggerState by mutableStateOf(EdgeTriggerState.IDLE)
  var isTransitioningPage by mutableStateOf(false)
  private var edgeDwellJob: Job? = null

  // --- Callbacks registered by the hosting screen ---
  var onLaunchApp: ((DiscoveredApp) -> Unit)? = null
  var onOpenFolder: ((SpaceFolder) -> Unit)? = null
  var onOpenCustomization: ((pageIndex: Int) -> Unit)? = null
  var onMovePlacement: ((placementId: String, targetPage: Int, targetPosition: Int, pageSize: Int) -> Unit)? = null
  var onCreateFolderFromApps: ((sourceApp: DiscoveredApp, targetApp: DiscoveredApp, sourcePlacementId: String, targetPlacementId: String, pageIndex: Int, positionIndex: Int) -> Unit)? = null
  var onAddAppToExistingFolder: ((folderId: String, app: DiscoveredApp, sourcePlacementId: String) -> Unit)? = null
  var onRemovePlacement: ((placementId: String) -> Unit)? = null
  var onDropItemToDock: ((placement: SpaceItemPlacement, app: DiscoveredApp, targetDockSlot: Int) -> Unit)? = null
  var onPerformPageTransition: (suspend (direction: EdgePagingDirection, targetPage: Int) -> Unit)? = null

  // Data providers
  var effectivePlacementsProvider: () -> List<SpaceItemPlacement> = { emptyList() }
  var appLookupProvider: () -> Map<String, DiscoveredApp> = { emptyMap() }
  var folderLookupProvider: () -> Map<String, SpaceFolder> = { emptyMap() }
  var allAppsProvider: () -> List<DiscoveredApp> = { emptyList() }
  var currentPageProvider: () -> Int = { 0 }
  var totalPageCountProvider: () -> Int = { 1 }

  val pageSize: Int get() = gridColumns * gridRows

  fun performHaptic(type: HapticFeedbackType) {
    try {
      hapticFeedback?.performHapticFeedback(type)
    } catch (_: Exception) {}
  }

  fun isActionActive(): Boolean = activeActionPlacement != null

  fun onItemTouched(placement: SpaceItemPlacement, rootOffset: Offset) {
    touchedItemFromCell = placement
    touchedItemRootOffset = rootOffset
  }

  fun updatePageGridBounds() {
    val root = rootCoordinates
    val pager = pagerCoordinates
    if (root != null && pager != null && pager.isAttached && root.isAttached) {
      val localOffset = root.localPositionOf(pager, Offset.Zero)
      pageGridBounds = Rect(localOffset, pager.size.toSize())
    }
  }

  /**
   * Updates screen geometry metrics. Recalculates slot bounds mathematically.
   */
  fun updateGeometry(
    root: LayoutCoordinates?,
    pagerCoords: LayoutCoordinates?,
    viewportW: Float,
    viewportH: Float,
    gridHPadPx: Float,
    gridVPadPx: Float,
    cellWPx: Float,
    cellHPx: Float,
    spacingPx: Float,
    rowPPx: Float,
    edgeZPx: Float,
    slopPx: Float
  ) {
    rootCoordinates = root
    pagerCoordinates = pagerCoords
    viewportWidth = viewportW
    viewportHeight = viewportH
    cellWidthPx = cellWPx
    cellHeightPx = cellHPx
    appSpacingPx = spacingPx
    rowPitchPx = rowPPx
    edgeZonePx = edgeZPx
    dragSlopPx = slopPx

    if (root != null && pagerCoords != null && pagerCoords.isAttached && root.isAttached) {
      val localOffset = root.localPositionOf(pagerCoords, Offset.Zero)
      val pagerRect = Rect(localOffset, pagerCoords.size.toSize())
      val bounds = Rect(
        left = pagerRect.left + gridHPadPx,
        top = pagerRect.top + gridVPadPx,
        right = pagerRect.right - gridHPadPx,
        bottom = pagerRect.bottom - gridVPadPx
      )
      pageGridBounds = bounds

      val newBounds = mutableMapOf<Int, Rect>()
      for (r in 0 until gridRows) {
        for (c in 0 until gridColumns) {
          val left = bounds.left + c * (cellWPx + spacingPx)
          val top = bounds.top + r * rowPPx
          newBounds[r * gridColumns + c] = Rect(left, top, left + cellWPx, top + cellHPx)
        }
      }
      slotBounds.clear()
      slotBounds.putAll(newBounds)
    }
  }

  /**
   * Computes the bounding rect for a placement taking into account multi-cell widget spans.
   */
  fun getPlacementFootprintRect(item: SpaceItemPlacement): Rect? {
    val measured = cellBounds[item.id]
    val itemR = item.positionIndex / gridColumns
    val itemC = item.positionIndex % gridColumns
    val sX = if (item.isWidget) item.spanX.coerceIn(1, (gridColumns - itemC).coerceAtLeast(1)) else 1
    val sY = if (item.isWidget) item.spanY.coerceIn(1, (gridRows - itemR).coerceAtLeast(1)) else 1

    val tlSlot = item.positionIndex
    val brSlot = (itemR + sY - 1) * gridColumns + (itemC + sX - 1)
    val tlRect = slotBounds[tlSlot]
    val brRect = slotBounds[brSlot]
    val slotDerivedRect = if (tlRect != null && brRect != null) {
      Rect(
        left = minOf(tlRect.left, brRect.left),
        top = minOf(tlRect.top, brRect.top),
        right = maxOf(tlRect.right, brRect.right),
        bottom = maxOf(tlRect.bottom, brRect.bottom)
      )
    } else tlRect ?: brRect

    val pageBounds = pageGridBounds
    val mathRect = if (pageBounds != null && pageBounds.width > 0f) {
      val left = pageBounds.left + itemC * (cellWidthPx + appSpacingPx)
      val top = pageBounds.top + itemR * rowPitchPx
      val right = left + sX * cellWidthPx + (sX - 1) * appSpacingPx
      val bottom = top + sY * cellHeightPx + (sY - 1) * appSpacingPx
      Rect(left, top, right, bottom)
    } else null

    return when {
      measured != null && slotDerivedRect != null -> Rect(
        minOf(measured.left, slotDerivedRect.left),
        minOf(measured.top, slotDerivedRect.top),
        maxOf(measured.right, slotDerivedRect.right),
        maxOf(measured.bottom, slotDerivedRect.bottom)
      )
      measured != null -> measured
      slotDerivedRect != null -> slotDerivedRect
      else -> mathRect
    }
  }

  /**
   * Hit-tests an offset against all placements on the specified page.
   */
  fun findItemAtOffset(offset: Offset, page: Int): SpaceItemPlacement? {
    val pagePlacements = effectivePlacementsProvider().filter {
      if (isScrollMode) true else it.pageIndex == page
    }

    // PRIORITY 1: Physical footprint of widgets
    val hitWidget = pagePlacements.firstOrNull { item ->
      item.isWidget && getPlacementFootprintRect(item)?.contains(offset) == true
    }
    if (hitWidget != null) return hitWidget

    // PRIORITY 2: Physical footprint of apps and folders
    val hitItem = pagePlacements.firstOrNull { item ->
      !item.isWidget && getPlacementFootprintRect(item)?.contains(offset) == true
    }
    if (hitItem != null) return hitItem

    // PRIORITY 3: Slot-based hit testing
    val totalSlots = gridColumns * gridRows
    val hitSlot = slotBounds.entries.firstOrNull { (slot, rect) ->
      slot < totalSlots && rect.contains(offset)
    }?.key
    if (hitSlot != null) {
      val slotR = hitSlot / gridColumns
      val slotC = hitSlot % gridColumns

      val coveringWidget = pagePlacements.firstOrNull { item ->
        if (!item.isWidget) return@firstOrNull false
        val itemR = item.positionIndex / gridColumns
        val itemC = item.positionIndex % gridColumns
        val sX = item.spanX.coerceIn(1, gridColumns - itemC)
        val sY = item.spanY.coerceIn(1, gridRows - itemR)
        slotC in itemC until (itemC + sX) && slotR in itemR until (itemR + sY)
      }
      if (coveringWidget != null) return coveringWidget

      return pagePlacements.firstOrNull { it.positionIndex == hitSlot }
    }

    return null
  }

  /**
   * Resolves target slot index for a pointer position given span dimensions.
   */
  fun calculateSlotForPosition(pos: Offset, spanX: Int = 1, spanY: Int = 1): Int {
    val totalSlots = gridColumns * gridRows
    val directHit = slotBounds.entries.firstOrNull { (slot, rect) ->
      slot < totalSlots && rect.contains(pos)
    }?.key

    val rawSlot: Int = if (directHit != null) {
      directHit
    } else {
      val bounds = pageGridBounds
      if (bounds != null && bounds.width > 0f) {
        val colWidth = (bounds.width / gridColumns).coerceAtLeast(1f)
        val c = when {
          pos.x <= bounds.left -> 0
          pos.x >= bounds.right -> gridColumns - 1
          else -> ((pos.x - bounds.left) / colWidth).toInt().coerceIn(0, gridColumns - 1)
        }
        val r = when {
          pos.y <= bounds.top -> 0
          else -> {
            val relativeY = pos.y - bounds.top
            val pitch = if (rowPitchPx > 0f) rowPitchPx else 100f
            (relativeY / pitch).toInt().coerceIn(0, gridRows - 1)
          }
        }
        (r * gridColumns + c).coerceIn(0, totalSlots - 1)
      } else {
        val colWidth = if (viewportWidth > 0f) (viewportWidth / gridColumns).coerceAtLeast(1f) else 100f
        val pitch = if (rowPitchPx > 0f) rowPitchPx else 100f
        val c = (pos.x / colWidth).toInt().coerceIn(0, gridColumns - 1)
        val r = (pos.y / pitch).toInt().coerceIn(0, gridRows - 1)
        (r * gridColumns + c).coerceIn(0, totalSlots - 1)
      }
    }

    val rawC = rawSlot % gridColumns
    val rawR = rawSlot / gridColumns
    val clampedC = rawC.coerceIn(0, maxOf(0, gridColumns - spanX))
    val clampedR = rawR.coerceIn(0, maxOf(0, gridRows - spanY))
    return (clampedR * gridColumns + clampedC).coerceIn(0, totalSlots - 1)
  }

  fun updatePreviewTargetSlot() {
    if (!isDragging && unifiedDragState?.isDragging != true) {
      previewTargetSlot = null
      targetHoverPlacement = null
      return
    }

    val cols = gridColumns
    val rows = gridRows
    val isDraggedWidget = draggedPlacement?.isWidget == true

    val targetPointerPos = if (isDraggedWidget && draggedPlacement != null) {
      val widgetTopLeft = currentPointerPos - touchOffsetWithinItem
      Offset(
        widgetTopLeft.x + cellWidthPx / 2f,
        widgetTopLeft.y + cellHeightPx / 2f
      )
    } else {
      currentPointerPos
    }

    val draggedSpanX = if (isDraggedWidget) draggedPlacement!!.spanX.coerceIn(1, cols) else 1
    val draggedSpanY = if (isDraggedWidget) draggedPlacement!!.spanY.coerceIn(1, rows) else 1
    val candidateSlot = calculateSlotForPosition(targetPointerPos, draggedSpanX, draggedSpanY)

    if (previewTargetSlot != candidateSlot) {
      previewTargetSlot = candidateSlot
      AppLogger.i(
        AppLogger.Category.LAUNCHER,
        "PREVIEW_TARGET: pointerY=${currentPointerPos.y} previewTargetSlot=$candidateSlot targetPage=${currentPageProvider()} targetPos=$candidateSlot"
      )
      performHaptic(HapticFeedbackType.TextHandleMove)
    }

    val targetPage = currentPageProvider()
    val isApp = draggedPlacement?.let { !it.isWidget && !it.isFolder } == true
    if (isApp) {
      val hovered = findItemAtOffset(currentPointerPos, targetPage)?.takeIf { it.id != draggedPlacement?.id }
        ?: effectivePlacementsProvider().firstOrNull { item ->
          item.id != draggedPlacement?.id &&
          (isScrollMode || item.pageIndex == targetPage) &&
          (
            item.positionIndex == candidateSlot ||
            getPlacementFootprintRect(item)?.contains(currentPointerPos) == true ||
            cellBounds[item.id]?.contains(currentPointerPos) == true
          )
        }
      if (hovered != null && targetHoverPlacement?.id != hovered.id) {
        performHaptic(HapticFeedbackType.TextHandleMove)
      }
      targetHoverPlacement = hovered
    } else {
      targetHoverPlacement = null
    }
  }

  // --- Gesture Handlers ---

  fun onItemClicked(placement: SpaceItemPlacement) {
    if (isActionActive() || isDragging) return
    val now = android.os.SystemClock.uptimeMillis()
    if ((now - lastLongPressTimestamp) < 600L) return

    if (placement.isFolder) {
      val folder = folderLookupProvider()[placement.folderId]
      if (folder != null) onOpenFolder?.invoke(folder)
    } else {
      val key = "${placement.packageName}/${placement.componentName}"
      val app = appLookupProvider()[key] ?: allAppsProvider().firstOrNull { it.packageName == placement.packageName }
      if (app != null) onLaunchApp?.invoke(app)
    }
  }

  fun onLongPressItem(placement: SpaceItemPlacement, rootOffset: Offset) {
    lastLongPressTimestamp = android.os.SystemClock.uptimeMillis()
    performHaptic(HapticFeedbackType.LongPress)
    resizingWidgetId = null

    if (placement.isFolder) {
      activeActionPlacement = null
      pendingDragPlacement = null
      lifecycleState = DragLifecycleState.DRAGGING
      unifiedDragState?.lifecycleState = DragLifecycleState.DRAGGING
      startDrag(placement, rootOffset)
    } else {
      // Transition to PRESSED_ACTION_VISIBLE
      lifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
      unifiedDragState?.lifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
      activeActionPlacement = placement
      pendingDragPlacement = placement
      pendingDragStartOffset = rootOffset
    }
  }

  fun onLongPressDesktop(page: Int) {
    lastLongPressTimestamp = android.os.SystemClock.uptimeMillis()
    lifecycleState = DragLifecycleState.IDLE
    unifiedDragState?.lifecycleState = DragLifecycleState.IDLE
    activeActionPlacement = null
    pendingDragPlacement = null
    if (resizingWidgetId != null) {
      resizingWidgetId = null
    } else {
      performHaptic(HapticFeedbackType.LongPress)
      onOpenCustomization?.invoke(page)
    }
  }

  fun onLongPressReleaseWithoutDrag() {
    // User held and released without dragging past slop:
    // Action box stays visible! pendingDragPlacement is cleared so subsequent gestures don't drag.
    pendingDragPlacement = null
  }

  fun dismissAction() {
    activeActionPlacement = null
    pendingDragPlacement = null
    if (lifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE) {
      lifecycleState = DragLifecycleState.IDLE
      unifiedDragState?.lifecycleState = DragLifecycleState.IDLE
    }
  }

  fun startDrag(placement: SpaceItemPlacement, startOffset: Offset) {
    activeActionPlacement = null
    pendingDragPlacement = null
    lifecycleState = DragLifecycleState.DRAGGING
    draggedPlacement = placement
    currentPointerPos = startOffset
    isOverBin = false
    targetHoverPlacement = null
    activeEdgeZone = EdgePagingDirection.NONE
    edgeTriggerState = EdgeTriggerState.IDLE
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    previewTargetSlot = null

    val itemRect = getPlacementFootprintRect(placement) ?: cellBounds[placement.id] ?: slotBounds[placement.positionIndex]
    touchOffsetWithinItem = if (itemRect != null) {
      Offset(
        (startOffset.x - itemRect.left).coerceIn(0f, itemRect.width),
        (startOffset.y - itemRect.top).coerceIn(0f, itemRect.height)
      )
    } else {
      val sX = if (placement.isWidget) placement.spanX.coerceIn(1, gridColumns) else 1
      val sY = if (placement.isWidget) placement.spanY.coerceIn(1, gridRows) else 1
      Offset((cellWidthPx * sX) / 2f, (cellHeightPx * sY) / 2f)
    }

    val uds = unifiedDragState
    if (uds != null) {
      val app = appLookupProvider()["${placement.packageName}/${placement.componentName}"]
        ?: allAppsProvider().firstOrNull { it.packageName == placement.packageName }
      uds.lifecycleState = DragLifecycleState.DRAGGING
      uds.isDragging = true
      uds.dragSource = DragSource.LAYER1_DESKTOP
      uds.draggedPlacement = placement
      uds.draggedApp = app
      uds.touchOffsetInItem = touchOffsetWithinItem
      uds.rootPointerPos = rootCoordinates?.localToRoot(startOffset) ?: startOffset
      uds.currentTargetZone = DragTargetZone.DESKTOP
    }

    AppLogger.i(
      AppLogger.Category.LAUNCHER,
      "DRAG_START item=${placement.id} pkg=${placement.packageName ?: "folder"} page=${placement.pageIndex} slot=${placement.positionIndex}"
    )
  }

  fun onDragMove(newPos: Offset) {
    currentPointerPos = newPos
    val rootPos = rootCoordinates?.localToRoot(newPos) ?: newPos
    unifiedDragState?.rootPointerPos = rootPos

    val overBin = binBounds?.contains(currentPointerPos) == true
    isOverBin = overBin
    unifiedDragState?.isOverBin = overBin

    if (overBin) {
      unifiedDragState?.currentTargetZone = DragTargetZone.REMOVE_BIN
      targetHoverPlacement = null
      previewTargetSlot = null
      cancelEdgeDwell("over_bin")
      return
    }

    val isAppPlacement = draggedPlacement?.isWidget != true
    val isOverDock = isAppPlacement && (unifiedDragState?.isPointerOverDock(rootPos) == true)
    if (isOverDock) {
      unifiedDragState?.currentTargetZone = DragTargetZone.DOCK_BAR
      targetHoverPlacement = null
      previewTargetSlot = null
      cancelEdgeDwell("over_dock")
      return
    }

    unifiedDragState?.currentTargetZone = DragTargetZone.DESKTOP
    updatePreviewTargetSlot()

    // Edge paging state machine (horizontal pager mode)
    if (!isScrollMode && viewportWidth > 0f && !isTransitioningPage) {
      val inLeftEdge = currentPointerPos.x in 0f..edgeZonePx
      val inRightEdge = currentPointerPos.x in (viewportWidth - edgeZonePx)..viewportWidth
      val curPage = currentPageProvider()

      when {
        inLeftEdge -> {
          if (activeEdgeZone != EdgePagingDirection.LEFT) {
            activeEdgeZone = EdgePagingDirection.LEFT
            edgeDwellJob?.cancel()
            AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_ENTER direction=LEFT page=$curPage x=${currentPointerPos.x}")
            if (curPage > 0) {
              edgeTriggerState = EdgeTriggerState.ARMED
              edgeDwellJob = coroutineScope.launch {
                delay(edgeDwellDelayMs)
                triggerPageTransition(EdgePagingDirection.LEFT)
              }
            } else {
              edgeTriggerState = EdgeTriggerState.IDLE
              edgeDwellJob = null
            }
          }
        }
        inRightEdge -> {
          if (activeEdgeZone != EdgePagingDirection.RIGHT) {
            activeEdgeZone = EdgePagingDirection.RIGHT
            edgeDwellJob?.cancel()
            AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_ENTER direction=RIGHT page=$curPage x=${currentPointerPos.x}")
            edgeTriggerState = EdgeTriggerState.ARMED
            edgeDwellJob = coroutineScope.launch {
              delay(edgeDwellDelayMs)
              triggerPageTransition(EdgePagingDirection.RIGHT)
            }
          }
        }
        else -> {
          if (activeEdgeZone != EdgePagingDirection.NONE) {
            cancelEdgeDwell("returned_to_center")
          }
        }
      }
    }
  }

  private fun cancelEdgeDwell(reason: String) {
    if (activeEdgeZone != EdgePagingDirection.NONE) {
      AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_DWELL_CANCEL reason=$reason")
      edgeDwellJob?.cancel()
      edgeDwellJob = null
      activeEdgeZone = EdgePagingDirection.NONE
      edgeTriggerState = EdgeTriggerState.IDLE
    }
  }

  private suspend fun triggerPageTransition(direction: EdgePagingDirection) {
    if (!isDragging || isTransitioningPage) return
    val curPage = currentPageProvider()
    val totalPages = totalPageCountProvider()

    if (direction == EdgePagingDirection.LEFT && curPage > 0) {
      isTransitioningPage = true
      val targetPage = curPage - 1
      try {
        onPerformPageTransition?.invoke(direction, targetPage)
        performHaptic(HapticFeedbackType.TextHandleMove)
      } finally {
        isTransitioningPage = false
        edgeTriggerState = EdgeTriggerState.CONSUMED
        edgeDwellJob = null
        updatePreviewTargetSlot()
      }
    } else if (direction == EdgePagingDirection.RIGHT) {
      isTransitioningPage = true
      val targetPage = curPage + 1
      try {
        if (curPage >= totalPages - 1) {
          extraPagesCount++
        }
        onPerformPageTransition?.invoke(direction, targetPage)
        performHaptic(HapticFeedbackType.TextHandleMove)
      } finally {
        isTransitioningPage = false
        edgeTriggerState = EdgeTriggerState.CONSUMED
        edgeDwellJob = null
        updatePreviewTargetSlot()
      }
    }
  }

  fun endDrag() {
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    isTransitioningPage = false
    activeEdgeZone = EdgePagingDirection.NONE
    edgeTriggerState = EdgeTriggerState.IDLE

    try {
      val dragged = draggedPlacement
      if (dragged != null) {
        val targetZone = unifiedDragState?.currentTargetZone ?: if (isOverBin) DragTargetZone.REMOVE_BIN else DragTargetZone.DESKTOP
        when (targetZone) {
          DragTargetZone.REMOVE_BIN -> {
            AppLogger.i(AppLogger.Category.LAUNCHER, "DROP_REMOVE item=${dragged.id} pkg=${dragged.packageName}")
            onRemovePlacement?.invoke(dragged.id)
          }
          DragTargetZone.DOCK_BAR -> {
            if (!dragged.isWidget) {
              val app = appLookupProvider()["${dragged.packageName}/${dragged.componentName}"]
                ?: allAppsProvider().firstOrNull { it.packageName == dragged.packageName }
              if (app != null) {
                AppLogger.i(AppLogger.Category.LAUNCHER, "DROP_TO_DOCK item=${dragged.id} pkg=${dragged.packageName}")
                performHaptic(HapticFeedbackType.TextHandleMove)
                onDropItemToDock?.invoke(dragged, app, unifiedDragState?.targetDockIndex ?: -1)
              }
            }
          }
          DragTargetZone.DESKTOP, DragTargetZone.NONE -> {
            val targetPage = currentPageProvider()
            val cols = gridColumns
            val rows = gridRows
            val draggedSpanX = if (dragged.isWidget) dragged.spanX.coerceIn(1, cols) else 1
            val draggedSpanY = if (dragged.isWidget) dragged.spanY.coerceIn(1, rows) else 1
            val rawTargetPos = previewTargetSlot ?: dragged.positionIndex
            val rawC = rawTargetPos % cols
            val rawR = rawTargetPos / cols
            val clampedC = rawC.coerceIn(0, maxOf(0, cols - draggedSpanX))
            val clampedR = rawR.coerceIn(0, maxOf(0, rows - draggedSpanY))
            val targetPos = clampedR * cols + clampedC

            AppLogger.i(
              AppLogger.Category.LAUNCHER,
              "FINAL_DROP: previewTargetSlot=$previewTargetSlot targetPage=$targetPage targetPos=$targetPos draggedPlacement.positionIndex=${dragged.positionIndex}"
            )
            performHaptic(HapticFeedbackType.TextHandleMove)

            val isDraggedApp = !dragged.isWidget && !dragged.isFolder
            if (isDraggedApp) {
              val targetPlacement = targetHoverPlacement?.takeIf {
                it.id != dragged.id && (isScrollMode || it.pageIndex == targetPage)
              } ?: findItemAtOffset(currentPointerPos, targetPage)?.takeIf { it.id != dragged.id }
                ?: effectivePlacementsProvider().firstOrNull { item ->
                  item.id != dragged.id &&
                  (isScrollMode || item.pageIndex == targetPage) &&
                  item.positionIndex == targetPos
                }

              if (targetPlacement != null) {
                if (!targetPlacement.isWidget && !targetPlacement.isFolder) {
                  // Dropped directly on another app -> folder creation
                  val sourceApp = appLookupProvider()["${dragged.packageName}/${dragged.componentName}"]
                    ?: allAppsProvider().firstOrNull { it.packageName == dragged.packageName }
                  val targetApp = appLookupProvider()["${targetPlacement.packageName}/${targetPlacement.componentName}"]
                    ?: allAppsProvider().firstOrNull { it.packageName == targetPlacement.packageName }

                  if (sourceApp != null && targetApp != null) {
                    AppLogger.i(
                      AppLogger.Category.LAUNCHER,
                      "DROP_CREATE_FOLDER: source=${sourceApp.label} target=${targetApp.label} page=$targetPage pos=${targetPlacement.positionIndex}"
                    )
                    performHaptic(HapticFeedbackType.LongPress)
                    onCreateFolderFromApps?.invoke(
                      sourceApp,
                      targetApp,
                      dragged.id,
                      targetPlacement.id,
                      targetPlacement.pageIndex,
                      targetPlacement.positionIndex
                    )
                    return
                  }
                } else if (targetPlacement.isFolder && targetPlacement.folderId != null) {
                  // Dropped onto existing folder -> add to folder
                  val sourceApp = appLookupProvider()["${dragged.packageName}/${dragged.componentName}"]
                    ?: allAppsProvider().firstOrNull { it.packageName == dragged.packageName }
                  if (sourceApp != null) {
                    AppLogger.i(
                      AppLogger.Category.LAUNCHER,
                      "DROP_ADD_TO_FOLDER: source=${sourceApp.label} folderId=${targetPlacement.folderId}"
                    )
                    performHaptic(HapticFeedbackType.LongPress)
                    onAddAppToExistingFolder?.invoke(targetPlacement.folderId, sourceApp, dragged.id)
                    return
                  }
                }
              }
            }

            // Normal drop to position
            onMovePlacement?.invoke(dragged.id, targetPage, targetPos, pageSize)
          }
        }
      }
    } finally {
      lifecycleState = DragLifecycleState.IDLE
      draggedPlacement = null
      previewTargetSlot = null
      targetHoverPlacement = null
      isOverBin = false
      extraPagesCount = 0
      unifiedDragState?.reset()
    }
  }

  fun cancelDrag() {
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    isTransitioningPage = false
    activeEdgeZone = EdgePagingDirection.NONE
    edgeTriggerState = EdgeTriggerState.IDLE
    lifecycleState = DragLifecycleState.IDLE
    draggedPlacement = null
    previewTargetSlot = null
    targetHoverPlacement = null
    isOverBin = false
    extraPagesCount = 0
    unifiedDragState?.reset()
    AppLogger.i(AppLogger.Category.LAUNCHER, "DRAG_CANCEL")
  }
}

/**
 * Intercepting pointer input loop for Launcher surfaces.
 *
 * Sequence:
 * 1. Press -> tracks down position and timestamp.
 * 2. If released or moved before long press timeout (400ms):
 *    - Released -> Tap (launches app or dismisses action overlay).
 *    - Moved past slop -> Swipe/Scroll (cancels tracking, yields to Pager/Vertical Swipe).
 * 3. If held for 400ms without moving -> Long Press triggered!
 *    - Transitions to PRESSED_ACTION_VISIBLE (quick action box shown).
 *    - Consumes subsequent moves in PointerEventPass.Initial so pagers/scrollables cannot steal the gesture.
 * 4. Move finger while in PRESSED_ACTION_VISIBLE ->
 *    - As soon as distance >= dragSlopPx -> transitions directly to DRAGGING!
 *    - App begins following finger smoothly.
 * 5. Release finger:
 *    - If DRAGGING -> commits drop position.
 *    - If PRESSED_ACTION_VISIBLE (finger lifted without moving past slop) -> action box stays visible, app is NOT launched.
 */
suspend fun PointerInputScope.detectDesktopGestures(
  controller: DesktopDragManager,
  longPressTimeoutMs: Long = 400L
) {
  awaitPointerEventScope {
    while (true) {
      // 1. Wait for initial press
      var downChange: PointerInputChange? = null
      while (downChange == null) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
          downChange = event.changes.firstOrNull { it.pressed }
        }
      }

      val pointerId = downChange.id
      val downPos = downChange.position
      val downTime = downChange.uptimeMillis

      // If an action box is currently visible on screen:
      val actionBounds = controller.actionBoxBounds
      if (controller.isActionActive()) {
        if (actionBounds != null && actionBounds.contains(downPos)) {
          // Pointer is inside the action box (user is tapping App Info / Uninstall)
          // Do NOT intercept; let the action buttons receive clicks in Main pass.
          do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
          } while (event.changes.any { it.pressed })
          continue
        } else {
          // Pointer is outside the action box: tap dismisses the action overlay
          controller.dismissAction()
          downChange.consume()
          do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
          } while (event.changes.any { it.pressed })
          continue
        }
      }

      // Clear any previous touched item cache
      controller.touchedItemFromCell = null

      var isLongPressTriggered = false
      var pointerReleasedBeforeLongPress = false
      var movedPastSlopBeforeLongPress = false

      try {
        withTimeout(longPressTimeoutMs) {
          while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
            val distance = (change.position - downPos).getDistance()

            if (!change.pressed) {
              pointerReleasedBeforeLongPress = true
              break
            }

            if (distance >= controller.dragSlopPx) {
              movedPastSlopBeforeLongPress = true
              break
            }
          }
        }
      } catch (_: TimeoutCancellationException) {
        // Finger was held still for longPressTimeoutMs: LONG PRESS TRIGGERED!
        isLongPressTriggered = true
      }

      if (pointerReleasedBeforeLongPress) {
        // Finger released before long press timeout: tap is handled by item clickable in Layer1ItemCell
        continue
      }

      if (movedPastSlopBeforeLongPress) {
        // User moved before long press: let horizontal pager or vertical swipe handle it
        do {
          val event = awaitPointerEvent(PointerEventPass.Initial)
        } while (event.changes.any { it.pressed })
        continue
      }

      if (isLongPressTriggered) {
        val currentPage = controller.currentPageProvider()
        val targetItem = controller.touchedItemFromCell ?: controller.findItemAtOffset(downPos, currentPage)

        // Long press event dispatch
        if (targetItem != null) {
          controller.onLongPressItem(targetItem, downPos)
        } else {
          controller.onLongPressDesktop(currentPage)
        }

        // The user's finger is STILL DOWN on the screen!
        // Now intercept and consume all subsequent pointer moves in PointerEventPass.Initial
        while (true) {
          val event = awaitPointerEvent(PointerEventPass.Initial)
          val change = event.changes.firstOrNull { it.id == pointerId } ?: break
          change.consume()

          if (!change.pressed) {
            // Finger lifted!
            if (controller.lifecycleState == DragLifecycleState.DRAGGING) {
              controller.endDrag()
            } else if (controller.lifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE) {
              controller.onLongPressReleaseWithoutDrag()
            }
            break
          }

          val currentPos = change.position
          val totalDist = (currentPos - downPos).getDistance()

          if (controller.lifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE && targetItem != null) {
            if (totalDist >= controller.dragSlopPx) {
              // Moved past slop: transition directly from PRESSED_ACTION_VISIBLE to DRAGGING!
              controller.startDrag(targetItem, downPos)
            }
          }

          if (controller.lifecycleState == DragLifecycleState.DRAGGING) {
            controller.onDragMove(currentPos)
          }
        }
      }
    }
  }
}
