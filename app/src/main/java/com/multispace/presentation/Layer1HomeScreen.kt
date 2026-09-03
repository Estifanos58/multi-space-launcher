package com.multispace.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.*
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.QuantumViolet
import com.multispace.ui.theme.ShapeRoundLg
import com.multispace.ui.theme.ShapeRoundMd
import com.multispace.ui.theme.ShapeRoundSm
import com.multispace.ui.theme.ShapeRoundXs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val EDGE_DWELL_DELAY_MS = 300L
private const val PAGE_TRANSITION_DURATION_MS = 280

private enum class EdgePagingDirection {
  NONE, LEFT, RIGHT
}

private enum class EdgeTriggerState {
  IDLE,
  ARMED,
  CONSUMED
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Layer1HomeScreen(
  space: Space,
  placements: List<SpaceItemPlacement>,
  folders: List<SpaceFolder>,
  allApps: List<DiscoveredApp>,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit,
  onOpenFolder: (SpaceFolder) -> Unit,
  onRemovePlacement: (String) -> Unit,
  onCreateFolderFromApps: (sourceApp: DiscoveredApp, targetApp: DiscoveredApp, sourcePlacementId: String?, targetPlacementId: String?) -> Unit,
  onAddAppToHome: (DiscoveredApp, Int) -> Unit,
  onMovePlacement: (placementId: String, targetPage: Int, targetPos: Int, pageSize: Int) -> Unit = { _, _, _, _ -> },
  modifier: Modifier = Modifier
) {
  BoxWithConstraints(
    modifier = modifier.fillMaxSize()
  ) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val appLookup = remember(allApps) {
      val map = mutableMapOf<String, DiscoveredApp>()
      for (app in allApps) {
        map["${app.packageName}/${app.activityName}"] = app
        map[app.packageName] = app
      }
      map
    }
    val folderLookup = remember(folders) {
      folders.associateBy { it.id }
    }

    // Grid sizing and dynamic row calculation based on full screen height
    val iconDp = when (space.iconSize) {
      Space.ICON_SIZE_SMALL -> 44.dp
      Space.ICON_SIZE_LARGE -> 62.dp
      else -> 52.dp
    }
    val iconSizeModifier = Modifier.size(iconDp)
    val labelHeight = if (space.labelVisibility) 20.dp else 0.dp
    val cellHeight = iconDp + labelHeight + 16.dp
    val appSpacing = 8.dp
    val gridHorizontalPadding = 16.dp
    val gridVerticalPadding = 8.dp
    val rowPitchDp = cellHeight + appSpacing
    val rowPitchPx = with(density) { rowPitchDp.toPx() }

    val cols = space.gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)
    val reservedVerticalSpace = 32.dp // Accommodate page vertical padding (16dp total) and potential page indicator dots
    val availableGridHeightDp = (maxHeight - reservedVerticalSpace).coerceAtLeast(cellHeight)
    val gridRows = ((availableGridHeightDp + appSpacing) / rowPitchDp).toInt().coerceAtLeast(1)
    val pageSize = (cols * gridRows).coerceAtLeast(1)

    // Ensure robust fallback placements if space has apps but no placements generated yet,
    // guaranteeing no apps are duplicated or lost in Layer 1.
    var localPlacements by remember(placements) { mutableStateOf(placements) }

    val effectivePlacements = remember(localPlacements, allApps, space, pageSize) {
      val existingPlacedApps = localPlacements.filter { it.itemType == SpaceItemPlacement.ITEM_TYPE_APP }
      val placedPkgSet = existingPlacedApps.mapNotNull { it.packageName }.toSet()

      val unplacedApps = allApps.distinctBy { it.packageName }.filter { app -> !placedPkgSet.contains(app.packageName) }

      val fullList = localPlacements.toMutableList()
      if (unplacedApps.isNotEmpty()) {
        val occupiedPerPage = mutableMapOf<Int, MutableSet<Int>>()
        for (p in fullList) {
          occupiedPerPage.getOrPut(p.pageIndex) { mutableSetOf() }.add(p.positionIndex)
        }

        var curPage = 0
        var curPos = 0
        for (app in unplacedApps) {
          var occupied = occupiedPerPage.getOrPut(curPage) { mutableSetOf() }
          while (occupied.contains(curPos) && curPos < pageSize) {
            curPos++
          }
          if (curPos >= pageSize) {
            curPage++
            curPos = 0
            occupied = occupiedPerPage.getOrPut(curPage) { mutableSetOf() }
            while (occupied.contains(curPos) && curPos < pageSize) {
              curPos++
            }
          }
          val newPlacement = SpaceItemPlacement(
            id = "virtual:${app.packageName}",
            spaceId = space.id,
            layer = SpaceItemPlacement.LAYER_HOME,
            pageIndex = curPage,
            positionIndex = curPos,
            itemType = SpaceItemPlacement.ITEM_TYPE_APP,
            packageName = app.packageName,
            componentName = app.activityName,
            userHandleId = app.userHandleId
          )
          fullList.add(newPlacement)
          occupied.add(curPos)
          curPos++
        }
      }
      fullList
    }

  // Active dragging state
  // Coordinate note: All coordinates (currentPointerPos, slotBounds, cellBounds, pageGridBounds, binBounds)
  // are measured in the root Box coordinate space to ensure single-source-of-truth geometry.
  var draggedPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var targetHoverPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var previewTargetSlot by remember { mutableStateOf<Int?>(null) }
  var isDragging by remember { mutableStateOf(false) }
  var currentPointerPos by remember { mutableStateOf(Offset.Zero) }
  var touchOffsetWithinItem by remember { mutableStateOf(Offset.Zero) }
  var binBounds by remember { mutableStateOf<Rect?>(null) }
  var isOverBin by remember { mutableStateOf(false) }

  // Root and page geometry measurements
  var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  var pageGridBounds by remember { mutableStateOf<Rect?>(null) }
  var viewportWidth by remember { mutableFloatStateOf(0f) }
  var viewportHeight by remember { mutableFloatStateOf(0f) }

  val cellBounds = remember { mutableStateMapOf<String, Rect>() }
  val slotBounds = remember { mutableStateMapOf<Int, Rect>() }

  // Page management with dynamic trailing page expansion
  val maxPageInPlacements = effectivePlacements.maxOfOrNull { it.pageIndex } ?: 0
  val basePageCount = (maxPageInPlacements + 1).coerceAtLeast(1)
  var extraPagesCount by remember { mutableIntStateOf(0) }
  val totalPageCount = (basePageCount + extraPagesCount).coerceAtLeast(1)

  val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPageCount })

  LaunchedEffect(pagerState.currentPage) {
    slotBounds.clear()
  }

  // Edge paging state machine (IDLE -> ARMED -> CONSUMED)
  var edgeTriggerState by remember { mutableStateOf(EdgeTriggerState.IDLE) }
  var activeEdgeZone by remember { mutableStateOf(EdgePagingDirection.NONE) }
  var isTransitioningPage by remember { mutableStateOf(false) }
  var edgeDwellJob by remember { mutableStateOf<Job?>(null) }

  val baseEdgeZonePx = with(density) { 80.dp.toPx() }
  val edgeZonePx = if (viewportWidth > 0f) {
    baseEdgeZonePx.coerceAtMost(viewportWidth * 0.22f)
  } else {
    baseEdgeZonePx
  }

  fun updatePreviewTargetSlot() {
    val totalSlots = cols * gridRows
    if (viewportWidth <= 0f || viewportHeight <= 0f) return

    // Check direct hit on measured slot bounds first
    val directHit = slotBounds.entries.firstOrNull { (slot, rect) ->
      slot < totalSlots && rect.contains(currentPointerPos)
    }?.key

    val candidateSlot: Int = if (directHit != null) {
      directHit
    } else {
      val bounds = pageGridBounds
      if (bounds != null && bounds.width > 0f) {
        val colWidth = (bounds.width / cols).coerceAtLeast(1f)
        val c = when {
          currentPointerPos.x <= bounds.left -> 0
          currentPointerPos.x >= bounds.right -> cols - 1
          else -> ((currentPointerPos.x - bounds.left) / colWidth).toInt().coerceIn(0, cols - 1)
        }

        val r = when {
          currentPointerPos.y <= bounds.top -> 0
          else -> {
            val relativeY = currentPointerPos.y - bounds.top
            (relativeY / rowPitchPx).toInt().coerceIn(0, gridRows - 1)
          }
        }

        (r * cols + c).coerceIn(0, totalSlots - 1)
      } else {
        val colWidth = (viewportWidth / cols).coerceAtLeast(1f)
        val c = (currentPointerPos.x / colWidth).toInt().coerceIn(0, cols - 1)
        val r = (currentPointerPos.y / rowPitchPx).toInt().coerceIn(0, gridRows - 1)
        (r * cols + c).coerceIn(0, totalSlots - 1)
      }
    }

    if (previewTargetSlot != candidateSlot) {
      previewTargetSlot = candidateSlot
      AppLogger.i(
        AppLogger.Category.LAUNCHER,
        "PREVIEW_TARGET: pointerY=${currentPointerPos.y} previewTargetSlot=$candidateSlot targetPage=${pagerState.currentPage} targetPos=$candidateSlot gridRows=$gridRows pageSize=$pageSize draggedPlacement.pageIndex=${draggedPlacement?.pageIndex} draggedPlacement.positionIndex=${draggedPlacement?.positionIndex}"
      )
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
  }

  fun performPageTransition(direction: EdgePagingDirection) {
    if (!isDragging || isTransitioningPage || pagerState.isScrollInProgress) return

    if (direction == EdgePagingDirection.LEFT && pagerState.currentPage > 0) {
      isTransitioningPage = true
      val fromPage = pagerState.currentPage
      val targetPage = fromPage - 1
      coroutineScope.launch {
        try {
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_START from=$fromPage to=$targetPage direction=LEFT")
          pagerState.animateScrollToPage(targetPage, animationSpec = tween(PAGE_TRANSITION_DURATION_MS))
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_COMPLETE page=$targetPage")
        } finally {
          isTransitioningPage = false
          // Edge trigger becomes CONSUMED: finger staying at edge will not trigger another transition
          edgeTriggerState = EdgeTriggerState.CONSUMED
          AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_TRIGGER_CONSUMED direction=LEFT page=$targetPage")
          edgeDwellJob = null
          updatePreviewTargetSlot()
        }
      }
    } else if (direction == EdgePagingDirection.RIGHT) {
      isTransitioningPage = true
      val fromPage = pagerState.currentPage
      val targetPage = fromPage + 1
      coroutineScope.launch {
        try {
          if (fromPage >= totalPageCount - 1) {
            AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_CREATE index=$targetPage")
            extraPagesCount++
          }
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_START from=$fromPage to=$targetPage direction=RIGHT")
          pagerState.animateScrollToPage(targetPage, animationSpec = tween(PAGE_TRANSITION_DURATION_MS))
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_COMPLETE page=$targetPage")
        } finally {
          isTransitioningPage = false
          // Edge trigger becomes CONSUMED: finger staying at edge will not trigger another transition or page creation
          edgeTriggerState = EdgeTriggerState.CONSUMED
          AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_TRIGGER_CONSUMED direction=RIGHT page=$targetPage")
          edgeDwellJob = null
          updatePreviewTargetSlot()
        }
      }
    }
  }

  fun handleStartDrag(placement: SpaceItemPlacement, startOffset: Offset) {
    draggedPlacement = placement
    isDragging = true
    currentPointerPos = startOffset
    isOverBin = false
    targetHoverPlacement = null
    activeEdgeZone = EdgePagingDirection.NONE
    edgeTriggerState = EdgeTriggerState.IDLE
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    previewTargetSlot = placement.positionIndex

    // Preserve finger-to-icon offset to eliminate visual jumping when drag starts
    val itemRect = cellBounds[placement.id] ?: slotBounds[placement.positionIndex]
    touchOffsetWithinItem = if (itemRect != null) {
      Offset(
        (startOffset.x - itemRect.left).coerceIn(0f, itemRect.width),
        (startOffset.y - itemRect.top).coerceIn(0f, itemRect.height)
      )
    } else {
      with(density) { Offset(iconDp.toPx() / 2f, iconDp.toPx() / 2f) }
    }

    AppLogger.i(
      AppLogger.Category.LAUNCHER,
      "DRAG_START item=${placement.id} pkg=${placement.packageName ?: "folder"} page=${placement.pageIndex} slot=${placement.positionIndex} touchOffset=$touchOffsetWithinItem"
    )
  }

  fun handleDragMove(newPos: Offset) {
    currentPointerPos = newPos
    val overBin = binBounds?.contains(currentPointerPos) == true
    isOverBin = overBin

    if (overBin) {
      targetHoverPlacement = null
      previewTargetSlot = null
      if (activeEdgeZone != EdgePagingDirection.NONE) {
        AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_DWELL_CANCEL direction=$activeEdgeZone reason=over_bin")
        edgeDwellJob?.cancel()
        edgeDwellJob = null
        activeEdgeZone = EdgePagingDirection.NONE
        edgeTriggerState = EdgeTriggerState.IDLE
      }
      return
    }

    updatePreviewTargetSlot()

    // Robust edge paging state machine (horizontal pager mode only)
    if (space.layer1DisplayMode != Space.DISPLAY_MODE_SCROLL && viewportWidth > 0f && !isTransitioningPage && !pagerState.isScrollInProgress) {
      val inLeftEdge = currentPointerPos.x in 0f..edgeZonePx
      val inRightEdge = currentPointerPos.x in (viewportWidth - edgeZonePx)..viewportWidth

      when {
        inLeftEdge -> {
          if (activeEdgeZone != EdgePagingDirection.LEFT) {
            // Newly entered left edge zone
            activeEdgeZone = EdgePagingDirection.LEFT
            edgeDwellJob?.cancel()
            AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_ENTER direction=LEFT page=${pagerState.currentPage} x=${currentPointerPos.x}")
            if (pagerState.currentPage > 0) {
              edgeTriggerState = EdgeTriggerState.ARMED
              AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_DWELL_START direction=LEFT page=${pagerState.currentPage} delayMs=$EDGE_DWELL_DELAY_MS")
              edgeDwellJob = coroutineScope.launch {
                delay(EDGE_DWELL_DELAY_MS)
                performPageTransition(EdgePagingDirection.LEFT)
              }
            } else {
              edgeTriggerState = EdgeTriggerState.IDLE
              edgeDwellJob = null
            }
          } else {
            // Already in left edge zone: if CONSUMED, do nothing!
            if (edgeTriggerState == EdgeTriggerState.IDLE && pagerState.currentPage > 0) {
              edgeTriggerState = EdgeTriggerState.ARMED
              AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_DWELL_START direction=LEFT page=${pagerState.currentPage} delayMs=$EDGE_DWELL_DELAY_MS")
              edgeDwellJob = coroutineScope.launch {
                delay(EDGE_DWELL_DELAY_MS)
                performPageTransition(EdgePagingDirection.LEFT)
              }
            }
          }
        }
        inRightEdge -> {
          if (activeEdgeZone != EdgePagingDirection.RIGHT) {
            // Newly entered right edge zone
            activeEdgeZone = EdgePagingDirection.RIGHT
            edgeDwellJob?.cancel()
            AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_ENTER direction=RIGHT page=${pagerState.currentPage} x=${currentPointerPos.x}")
            edgeTriggerState = EdgeTriggerState.ARMED
            AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_DWELL_START direction=RIGHT page=${pagerState.currentPage} delayMs=$EDGE_DWELL_DELAY_MS")
            edgeDwellJob = coroutineScope.launch {
              delay(EDGE_DWELL_DELAY_MS)
              performPageTransition(EdgePagingDirection.RIGHT)
            }
          } else {
            // Already in right edge zone: if CONSUMED, do nothing!
            if (edgeTriggerState == EdgeTriggerState.IDLE) {
              edgeTriggerState = EdgeTriggerState.ARMED
              AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_DWELL_START direction=RIGHT page=${pagerState.currentPage} delayMs=$EDGE_DWELL_DELAY_MS")
              edgeDwellJob = coroutineScope.launch {
                delay(EDGE_DWELL_DELAY_MS)
                performPageTransition(EdgePagingDirection.RIGHT)
              }
            }
          }
        }
        else -> {
          // In central area (outside both edge zones): reset trigger state
          if (activeEdgeZone != EdgePagingDirection.NONE) {
            AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_TRIGGER_RESET previousZone=$activeEdgeZone")
            edgeDwellJob?.cancel()
            edgeDwellJob = null
            activeEdgeZone = EdgePagingDirection.NONE
            edgeTriggerState = EdgeTriggerState.IDLE
          }
        }
      }
    }
  }

  fun handleEndDrag() {
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    isTransitioningPage = false
    activeEdgeZone = EdgePagingDirection.NONE
    edgeTriggerState = EdgeTriggerState.IDLE

    try {
      val dragged = draggedPlacement
      if (dragged != null) {
        if (isOverBin) {
          AppLogger.i(AppLogger.Category.LAUNCHER, "DROP_REMOVE item=${dragged.id} pkg=${dragged.packageName}")
          localPlacements = effectivePlacements.filter { it.id != dragged.id }
          onRemovePlacement(dragged.id)
        } else {
          val targetPage = pagerState.currentPage
          val targetPos = previewTargetSlot ?: dragged.positionIndex
          AppLogger.i(
            AppLogger.Category.LAUNCHER,
            "FINAL_DROP: pointerY=${currentPointerPos.y} previewTargetSlot=$previewTargetSlot targetPage=$targetPage targetPos=$targetPos gridRows=$gridRows pageSize=$pageSize draggedPlacement.pageIndex=${dragged.pageIndex} draggedPlacement.positionIndex=${dragged.positionIndex}"
          )
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

          // Optimistically update local placements with cascading ripple logic
          val updatedList = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
            allCurrentPlacements = effectivePlacements,
            itemToInsert = dragged,
            targetPage = targetPage,
            targetPosition = targetPos,
            pageSize = pageSize
          )
          localPlacements = updatedList

          onMovePlacement(dragged.id, targetPage, targetPos, pageSize)
        }
      }
    } finally {
      isDragging = false
      draggedPlacement = null
      previewTargetSlot = null
      targetHoverPlacement = null
      isOverBin = false
      extraPagesCount = 0
    }
  }

  fun handleCancelDrag() {
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    isTransitioningPage = false
    activeEdgeZone = EdgePagingDirection.NONE
    edgeTriggerState = EdgeTriggerState.IDLE
    isDragging = false
    draggedPlacement = null
    previewTargetSlot = null
    targetHoverPlacement = null
    isOverBin = false
    extraPagesCount = 0
    AppLogger.i(AppLogger.Category.LAUNCHER, "DRAG_CANCEL")
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .onGloballyPositioned { coordinates ->
        rootCoordinates = coordinates
        viewportWidth = coordinates.size.width.toFloat()
        viewportHeight = coordinates.size.height.toFloat()
      }
      .pointerInput(Unit) {
        detectDragGesturesAfterLongPress(
          onDragStart = { rootOffset ->
            val activePage = pagerState.currentPage
            var touchedPlacement: SpaceItemPlacement? = null
            val totalSlots = cols * gridRows

            // Check if rootOffset is in any slot with an item
            val hitSlot = slotBounds.entries.firstOrNull { it.key < totalSlots && it.value.contains(rootOffset) }?.key
            if (hitSlot != null) {
              touchedPlacement = effectivePlacements.firstOrNull { it.pageIndex == activePage && it.positionIndex == hitSlot }
            }
            if (touchedPlacement == null) {
              val bounds = pageGridBounds
              if (bounds != null && bounds.width > 0f) {
                val colWidth = (bounds.width / cols).coerceAtLeast(1f)
                val c = ((rootOffset.x - bounds.left) / colWidth).toInt().coerceIn(0, cols - 1)
                val r = ((rootOffset.y - bounds.top) / rowPitchPx).toInt().coerceIn(0, gridRows - 1)
                val touchedSlot = (r * cols + c).coerceIn(0, totalSlots - 1)
                touchedPlacement = effectivePlacements.firstOrNull { it.pageIndex == activePage && it.positionIndex == touchedSlot }
              }
            }
            if (touchedPlacement == null) {
              val expandedThreshold = with(density) { 16.dp.toPx() }
              touchedPlacement = cellBounds.entries.firstOrNull { (id, rect) ->
                val p = effectivePlacements.firstOrNull { it.id == id }
                p?.pageIndex == activePage && Rect(
                  rect.left - expandedThreshold,
                  rect.top - expandedThreshold,
                  rect.right + expandedThreshold,
                  rect.bottom + expandedThreshold
                ).contains(rootOffset)
              }?.key?.let { id -> effectivePlacements.firstOrNull { it.id == id } }
            }
            if (touchedPlacement != null) {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              handleStartDrag(touchedPlacement, rootOffset)
            }
          },
          onDrag = { change, _ ->
            change.consume()
            handleDragMove(change.position)
          },
          onDragEnd = { handleEndDrag() },
          onDragCancel = { handleCancelDrag() }
        )
      }
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Main content: either Paged or Scrolling
      if (space.layer1DisplayMode == Space.DISPLAY_MODE_SCROLL) {
        val scrollHorizontalPadding = 16.dp
        val appSpacing = 8.dp

        // Vertical continuous scrolling layout
        LazyVerticalGrid(
          columns = GridCells.Fixed(space.gridColumns),
          modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .padding(horizontal = scrollHorizontalPadding, vertical = 8.dp)
            .testTag("layer1_scroll_grid"),
          horizontalArrangement = Arrangement.spacedBy(appSpacing),
          verticalArrangement = Arrangement.spacedBy(appSpacing)
        ) {
          items(effectivePlacements, key = { it.id }) { placement ->
            Layer1ItemCell(
              placement = placement,
              space = space,
              appLookup = appLookup,
              folderLookup = folderLookup,
              allApps = allApps,
              iconSizeModifier = iconSizeModifier,
              getBitmap = getBitmap,
              onLaunchApp = onLaunchApp,
              onOpenFolder = onOpenFolder,
              onPositioned = { rect -> cellBounds[placement.id] = rect },
              isBeingDragged = isDragging && draggedPlacement?.id == placement.id,
              isTargetHover = targetHoverPlacement?.id == placement.id,
              rootCoordinates = rootCoordinates
            )
          }
        }
      } else {
        // Horizontal paged layout (Default)
        HorizontalPager(
          state = pagerState,
          userScrollEnabled = !isDragging,
          modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .testTag("layer1_horizontal_pager")
        ) { page ->
          val isCurrentPage = page == pagerState.currentPage

          val rawPagePlacements = effectivePlacements.filter { it.pageIndex == page }
          val otherPlacements = if (isDragging && draggedPlacement != null) {
            rawPagePlacements.filter { it.id != draggedPlacement!!.id }
          } else {
            rawPagePlacements
          }

          val previewSlotsMap = remember(effectivePlacements, otherPlacements, isDragging, isCurrentPage, isOverBin, previewTargetSlot, draggedPlacement, pageSize, gridRows) {
            if (isDragging && isCurrentPage && !isOverBin && previewTargetSlot != null && draggedPlacement != null) {
              val target = previewTargetSlot!!
              val dragged = draggedPlacement!!
              val allExceptDragged = effectivePlacements.filter { it.id != dragged.id }
              val cascaded = PlacementCascadeHelper.cascadeInsert(
                existingPlacements = allExceptDragged,
                itemToInsert = dragged,
                targetPage = page,
                targetPosition = target,
                pageSize = pageSize
              )
              // Only items belonging to current page, excluding the dragged item itself (rendered as DropTargetPreviewSlot)
              val shiftedOnPage = cascaded.filter { it.id != dragged.id && it.pageIndex == page }.associateBy { it.positionIndex }
              val shiftedIds = cascaded.map { it.id }.toSet()
              val map = mutableMapOf<Int, SpaceItemPlacement>()
              for (p in otherPlacements) {
                if (!shiftedIds.contains(p.id)) {
                  map[p.positionIndex] = p
                }
              }
              for ((pos, p) in shiftedOnPage) {
                map[pos] = p
              }
              map
            } else {
              otherPlacements.associateBy { it.positionIndex }
            }
          }

          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = gridHorizontalPadding, vertical = gridVerticalPadding)
              .onGloballyPositioned { coordinates ->
                if (isCurrentPage && rootCoordinates != null) {
                  val localOffset = rootCoordinates!!.localPositionOf(coordinates, Offset.Zero)
                  pageGridBounds = Rect(localOffset, coordinates.size.toSize())
                }
              },
            verticalArrangement = Arrangement.spacedBy(appSpacing)
          ) {
            for (r in 0 until gridRows) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(cellHeight),
                horizontalArrangement = Arrangement.spacedBy(appSpacing)
              ) {
                for (c in 0 until cols) {
                  val slotIndex = r * cols + c
                  val item = previewSlotsMap[slotIndex]
                  val isPreviewTarget = isDragging && isCurrentPage && !isOverBin && previewTargetSlot == slotIndex

                  Box(
                    modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                      .onGloballyPositioned { coords ->
                        if (isCurrentPage && coords.isAttached && rootCoordinates != null) {
                          val localOffset = rootCoordinates!!.localPositionOf(coords, Offset.Zero)
                          slotBounds[slotIndex] = Rect(localOffset, coords.size.toSize())
                        }
                      },
                    contentAlignment = Alignment.Center
                  ) {
                    if (isPreviewTarget && draggedPlacement != null) {
                      DropTargetPreviewSlot(
                        dragged = draggedPlacement!!,
                        space = space,
                        appLookup = appLookup,
                        folderLookup = folderLookup,
                        allApps = allApps,
                        iconSizeModifier = iconSizeModifier,
                        getBitmap = getBitmap
                      )
                    } else if (item != null) {
                      Layer1ItemCell(
                        placement = item,
                        space = space,
                        appLookup = appLookup,
                        folderLookup = folderLookup,
                        allApps = allApps,
                        iconSizeModifier = iconSizeModifier,
                        getBitmap = getBitmap,
                        onLaunchApp = onLaunchApp,
                        onOpenFolder = onOpenFolder,
                        onPositioned = { rect -> cellBounds[item.id] = rect },
                        isBeingDragged = isDragging && draggedPlacement?.id == item.id,
                        isTargetHover = false,
                        rootCoordinates = rootCoordinates
                      )
                    } else {
                      EmptyGridCell(
                        slotIndex = slotIndex,
                        isDragging = isDragging && !isOverBin
                      )
                    }
                  }
                }
              }
            }
          }
        }

        // Page Indicator Dots
        if (totalPageCount > 1) {
          PageIndicatorDots(
            pageCount = totalPageCount,
            currentPage = pagerState.currentPage,
            onDotClick = { page ->
              coroutineScope.launch { pagerState.animateScrollToPage(page) }
            },
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .padding(vertical = AppDimens.Spacing4)
          )
        }
      }
    }

    // Top Removal Bucket overlay (floats on top of apps without shifting grid layout)
    RemovalBucketBar(
      isVisible = isDragging && draggedPlacement != null,
      isHovered = isOverBin,
      onPositioned = { /* Coordinates measured accurately below */ },
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = AppDimens.Spacing8)
        .onGloballyPositioned { coordinates ->
          rootCoordinates?.let { root ->
            val localOffset = root.localPositionOf(coordinates, Offset.Zero)
            binBounds = Rect(localOffset, coordinates.size.toSize())
          }
        }
        .zIndex(50f)
    )

    // Subtle edge auto-paging activation indicator cues
    if (isDragging && space.layer1DisplayMode != Space.DISPLAY_MODE_SCROLL && !isOverBin) {
      if (activeEdgeZone == EdgePagingDirection.LEFT && edgeTriggerState == EdgeTriggerState.ARMED) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .width(28.dp)
            .align(Alignment.CenterStart)
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  QuantumViolet.copy(alpha = 0.35f),
                  Color.Transparent
                )
              )
            )
            .zIndex(100f)
        )
      } else if (activeEdgeZone == EdgePagingDirection.RIGHT && edgeTriggerState == EdgeTriggerState.ARMED) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .width(28.dp)
            .align(Alignment.CenterEnd)
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  Color.Transparent,
                  QuantumViolet.copy(alpha = 0.35f)
                )
              )
            )
            .zIndex(100f)
        )
      }
    }

    // Floating dragged item follow overlay (rendered in root coordinate space)
    if (isDragging && draggedPlacement != null) {
      val dragged = draggedPlacement!!
      val app = appLookup["${dragged.packageName}/${dragged.componentName}"]
        ?: allApps.firstOrNull { it.packageName == dragged.packageName }
      val bitmap = app?.let { getBitmap(it) }

      val dragScale by animateFloatAsState(
        targetValue = 1.08f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dragScale"
      )

      Box(
        modifier = Modifier
          .offset {
            IntOffset(
              (currentPointerPos.x - touchOffsetWithinItem.x).roundToInt(),
              (currentPointerPos.y - touchOffsetWithinItem.y).roundToInt()
            )
          }
          .wrapContentSize()
          .graphicsLayer {
            scaleX = dragScale
            scaleY = dragScale
            shadowElevation = 16.dp.toPx()
          }
          .zIndex(999f)
          .testTag("floating_dragged_item"),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Box(
            modifier = iconSizeModifier,
            contentAlignment = Alignment.Center
          ) {
            if (dragged.isFolder) {
              Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Dragging Folder",
                tint = QuantumViolet,
                modifier = Modifier.size(AppDimens.IconLg)
              )
            } else if (bitmap != null) {
              Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
              )
            } else {
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .clip(ShapeRoundMd)
                  .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = app?.label?.take(1) ?: "?",
                  fontWeight = FontWeight.Bold,
                  color = QuantumViolet,
                  fontSize = 20.sp
                )
              }
            }
          }
          if (space.labelVisibility && app != null) {
            Spacer(modifier = Modifier.height(AppDimens.Spacing4))
            Text(
              text = app.label,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 11.sp
            )
          }
        }
      }
    }
  }
}
}

@Composable
private fun DropTargetPreviewSlot(
  dragged: SpaceItemPlacement,
  space: Space,
  appLookup: Map<String, DiscoveredApp>,
  folderLookup: Map<String, SpaceFolder>,
  allApps: List<DiscoveredApp>,
  iconSizeModifier: Modifier,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  modifier: Modifier = Modifier
) {
  val folder = if (dragged.isFolder && dragged.folderId != null) folderLookup[dragged.folderId] else null
  val key = "${dragged.packageName}/${dragged.componentName}"
  val app = if (!dragged.isFolder) appLookup[key] ?: allApps.firstOrNull { it.packageName == dragged.packageName } else null
  val bitmap = app?.let { getBitmap(it) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.graphicsLayer { alpha = 0.48f }
    ) {
      Box(
        modifier = iconSizeModifier
          .clip(ShapeRoundMd)
          .background(QuantumViolet.copy(alpha = 0.14f))
          .border(BorderStroke(1.5.dp, QuantumViolet.copy(alpha = 0.55f)), ShapeRoundMd),
        contentAlignment = Alignment.Center
      ) {
        if (dragged.isFolder) {
          Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = QuantumViolet,
            modifier = Modifier.size(AppDimens.IconMd)
          )
        } else if (bitmap != null) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Text(
            text = app?.label?.take(1) ?: "?",
            fontWeight = FontWeight.Bold,
            color = QuantumViolet,
            fontSize = 18.sp
          )
        }
      }
      if (space.labelVisibility) {
        val label = if (dragged.isFolder) folder?.name ?: "Folder" else app?.label
        if (!label.isNullOrEmpty()) {
          Spacer(modifier = Modifier.height(AppDimens.Spacing4))
          Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp
          )
        }
      }
    }
  }
}

@Composable
private fun EmptyGridCell(
  slotIndex: Int,
  isDragging: Boolean,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    if (isDragging) {
      Box(
        modifier = Modifier
          .size(6.dp)
          .clip(CircleShape)
          .background(QuantumViolet.copy(alpha = 0.25f))
      )
    }
  }
}

@Composable
private fun Layer1ItemCell(
  placement: SpaceItemPlacement,
  space: Space,
  appLookup: Map<String, DiscoveredApp>,
  folderLookup: Map<String, SpaceFolder>,
  allApps: List<DiscoveredApp>,
  iconSizeModifier: Modifier,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit,
  onOpenFolder: (SpaceFolder) -> Unit,
  onPositioned: (Rect) -> Unit,
  isBeingDragged: Boolean,
  isTargetHover: Boolean,
  rootCoordinates: LayoutCoordinates?
) {
  val key = "${placement.packageName}/${placement.componentName}"
  val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == placement.packageName }
  val folder = if (placement.isFolder) folderLookup[placement.folderId] else null

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .wrapContentSize()
      .onGloballyPositioned { coordinates ->
        rootCoordinates?.let { root ->
          val localOffset = root.localPositionOf(coordinates, Offset.Zero)
          onPositioned(Rect(localOffset, coordinates.size.toSize()))
        } ?: onPositioned(coordinates.boundsInRoot())
      }
      .graphicsLayer {
        alpha = if (isBeingDragged) 0.0f else 1.0f
        scaleX = if (isTargetHover) 1.08f else 1.0f
        scaleY = if (isTargetHover) 1.08f else 1.0f
      }
      .clickable(enabled = !isBeingDragged) {
        if (placement.isFolder) {
          if (folder != null) onOpenFolder(folder)
        } else {
          if (app != null) onLaunchApp(app)
        }
      }
      .testTag(if (placement.isFolder) "layer1_folder_${placement.folderId}" else "layer1_app_${placement.packageName}")
  ) {
    if (placement.isFolder) {
      // Folder Preview Icon (2x2 mini grid)
      Box(
        modifier = iconSizeModifier
          .clip(ShapeRoundMd)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f))
          .border(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant, ShapeRoundMd)
          .padding(AppDimens.Spacing4),
        contentAlignment = Alignment.Center
      ) {
        val previewItems = folder?.items?.take(4) ?: emptyList()
        if (previewItems.isNotEmpty()) {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceEvenly
            ) {
              previewItems.take(2).forEach { item ->
                MiniAppIcon(item = item, appLookup = appLookup, allApps = allApps, getBitmap = getBitmap)
              }
            }
            if (previewItems.size > 2) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
              ) {
                previewItems.drop(2).take(2).forEach { item ->
                  MiniAppIcon(item = item, appLookup = appLookup, allApps = allApps, getBitmap = getBitmap)
                }
              }
            }
          }
        } else {
          Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = "Folder",
            tint = QuantumViolet,
            modifier = Modifier.size(AppDimens.IconMd)
          )
        }
      }

      if (space.labelVisibility) {
        Spacer(modifier = Modifier.height(AppDimens.Spacing4))
        Text(
          text = folder?.name ?: "Folder",
          style = MaterialTheme.typography.bodySmall,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    } else {
      // App Item - apps only take the space their icon takes! No border padding, no card background!
      val bitmap = app?.let { getBitmap(it) }
      if (bitmap != null) {
        Image(
          bitmap = bitmap.asImageBitmap(),
          contentDescription = app.label,
          modifier = iconSizeModifier
        )
      } else {
        Box(
          modifier = iconSizeModifier
            .clip(ShapeRoundMd)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = app?.label?.take(1) ?: placement.packageName?.take(1)?.uppercase() ?: "?",
            fontWeight = FontWeight.Bold,
            color = QuantumViolet,
            fontSize = 18.sp
          )
        }
      }

      if (space.labelVisibility) {
        Spacer(modifier = Modifier.height(AppDimens.Spacing4))
        Text(
          text = app?.label ?: placement.packageName?.substringAfterLast('.') ?: "",
          style = MaterialTheme.typography.bodySmall,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun MiniAppIcon(
  item: SpaceFolderItem,
  appLookup: Map<String, DiscoveredApp>,
  allApps: List<DiscoveredApp>,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?
) {
  val key = "${item.packageName}/${item.componentName}"
  val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == item.packageName }
  val bitmap = app?.let { getBitmap(it) }

  if (bitmap != null) {
    Image(
      bitmap = bitmap.asImageBitmap(),
      contentDescription = app.label,
      modifier = Modifier
        .size(16.dp)
        .clip(ShapeRoundXs)
    )
  } else {
    Box(
      modifier = Modifier
        .size(16.dp)
        .clip(ShapeRoundXs)
        .background(MaterialTheme.colorScheme.primaryContainer),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = app?.label?.take(1) ?: item.packageName.take(1).uppercase(),
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer
      )
    }
  }
}
