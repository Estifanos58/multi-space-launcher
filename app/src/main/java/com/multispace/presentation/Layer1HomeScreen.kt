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
  onMovePlacement: (placementId: String, targetPage: Int, targetPos: Int) -> Unit = { _, _, _ -> },
  modifier: Modifier = Modifier
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

  // Ensure robust fallback placements if space has apps but no placements generated yet,
  // guaranteeing no apps are duplicated or lost in Layer 1.
  val effectivePlacements = remember(placements, allApps, space) {
    val cols = space.gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)
    val pageSize = (cols * 5).coerceAtLeast(1)

    val existingPlacedApps = placements.filter { it.itemType == SpaceItemPlacement.ITEM_TYPE_APP }
    val placedPkgSet = existingPlacedApps.mapNotNull { it.packageName }.toSet()

    val unplacedApps = allApps.distinctBy { it.packageName }.filter { app -> !placedPkgSet.contains(app.packageName) }

    val fullList = placements.toMutableList()
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
          id = "virtual_${app.packageName}_${curPage}_$curPos",
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
  var draggedPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var targetHoverPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var previewTargetSlot by remember { mutableStateOf<Int?>(null) }
  var isDragging by remember { mutableStateOf(false) }
  var currentPointerPos by remember { mutableStateOf(Offset.Zero) }
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

  // Edge paging state machine
  var pendingEdgeDirection by remember { mutableStateOf(EdgePagingDirection.NONE) }
  var isTransitioningPage by remember { mutableStateOf(false) }
  var edgeDwellJob by remember { mutableStateOf<Job?>(null) }

  val iconSizeModifier = when (space.iconSize) {
    Space.ICON_SIZE_SMALL -> Modifier.size(44.dp)
    Space.ICON_SIZE_LARGE -> Modifier.size(62.dp)
    else -> Modifier.size(52.dp)
  }

  val baseEdgeZonePx = with(density) { 80.dp.toPx() }
  val edgeZonePx = if (viewportWidth > 0f) {
    baseEdgeZonePx.coerceAtMost(viewportWidth * 0.22f)
  } else {
    baseEdgeZonePx
  }

  fun updatePreviewTargetSlot() {
    val cols = space.gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)
    val rows = 5
    val totalSlots = cols * rows

    // 1. Direct hit check on accurately measured slot bounds
    val directHit = slotBounds.entries.firstOrNull { (slot, rect) ->
      slot < totalSlots && rect.contains(currentPointerPos)
    }?.key

    if (directHit != null) {
      if (previewTargetSlot != directHit) {
        previewTargetSlot = directHit
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
      return
    }

    // 2. Proximity check to closest slot center
    if (slotBounds.isNotEmpty()) {
      val closest = slotBounds.entries
        .filter { it.key < totalSlots }
        .minByOrNull { (_, rect) ->
          val dx = rect.center.x - currentPointerPos.x
          val dy = rect.center.y - currentPointerPos.y
          dx * dx + dy * dy
        }?.key

      if (closest != null) {
        if (previewTargetSlot != closest) {
          previewTargetSlot = closest
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        return
      }
    }

    // 3. Fallback to page grid calculation
    val bounds = pageGridBounds
    if (bounds != null && bounds.width > 0f && bounds.height > 0f) {
      val colWidth = bounds.width / cols
      val rowHeight = bounds.height / rows
      val clampedX = currentPointerPos.x.coerceIn(bounds.left, bounds.right - 1f)
      val clampedY = currentPointerPos.y.coerceIn(bounds.top, bounds.bottom - 1f)
      val c = ((clampedX - bounds.left) / colWidth).toInt().coerceIn(0, cols - 1)
      val r = ((clampedY - bounds.top) / rowHeight).toInt().coerceIn(0, rows - 1)
      val calculated = (r * cols + c).coerceIn(0, totalSlots - 1)
      if (previewTargetSlot != calculated) {
        previewTargetSlot = calculated
      }
    } else {
      if (previewTargetSlot == null) {
        previewTargetSlot = 0
      }
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
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_START from=$fromPage to=$targetPage")
          pagerState.animateScrollToPage(targetPage, animationSpec = tween(PAGE_TRANSITION_DURATION_MS))
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_COMPLETE page=$targetPage")
        } finally {
          isTransitioningPage = false
          pendingEdgeDirection = EdgePagingDirection.NONE
          updatePreviewTargetSlot()
          if (isDragging && currentPointerPos.x in 0f..edgeZonePx && targetPage > 0) {
            pendingEdgeDirection = EdgePagingDirection.LEFT
            edgeDwellJob = coroutineScope.launch {
              delay(EDGE_DWELL_DELAY_MS)
              performPageTransition(EdgePagingDirection.LEFT)
            }
          }
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
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_START from=$fromPage to=$targetPage")
          pagerState.animateScrollToPage(targetPage, animationSpec = tween(PAGE_TRANSITION_DURATION_MS))
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_COMPLETE page=$targetPage")
        } finally {
          isTransitioningPage = false
          pendingEdgeDirection = EdgePagingDirection.NONE
          updatePreviewTargetSlot()
          if (isDragging && currentPointerPos.x in (viewportWidth - edgeZonePx)..viewportWidth) {
            pendingEdgeDirection = EdgePagingDirection.RIGHT
            edgeDwellJob = coroutineScope.launch {
              delay(EDGE_DWELL_DELAY_MS)
              performPageTransition(EdgePagingDirection.RIGHT)
            }
          }
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
    pendingEdgeDirection = EdgePagingDirection.NONE
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    updatePreviewTargetSlot()
    AppLogger.i(AppLogger.Category.LAUNCHER, "DRAG_START item=${placement.packageName ?: placement.id} page=${placement.pageIndex}")
  }

  fun handleDragMove(newPos: Offset) {
    currentPointerPos = newPos
    val overBin = binBounds?.contains(currentPointerPos) == true
    isOverBin = overBin

    if (overBin) {
      targetHoverPlacement = null
      previewTargetSlot = null
      if (pendingEdgeDirection != EdgePagingDirection.NONE) {
        pendingEdgeDirection = EdgePagingDirection.NONE
        edgeDwellJob?.cancel()
        edgeDwellJob = null
      }
      return
    }

    updatePreviewTargetSlot()

    // Edge auto-paging detection (horizontal pager mode only)
    if (space.layer1DisplayMode != Space.DISPLAY_MODE_SCROLL && viewportWidth > 0f && !isTransitioningPage && !pagerState.isScrollInProgress) {
      if (currentPointerPos.x in 0f..edgeZonePx) {
        if (pagerState.currentPage > 0) {
          if (pendingEdgeDirection != EdgePagingDirection.LEFT) {
            pendingEdgeDirection = EdgePagingDirection.LEFT
            AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_ENTER direction=LEFT page=${pagerState.currentPage}")
            edgeDwellJob?.cancel()
            edgeDwellJob = coroutineScope.launch {
              delay(EDGE_DWELL_DELAY_MS)
              performPageTransition(EdgePagingDirection.LEFT)
            }
          }
        } else {
          if (pendingEdgeDirection != EdgePagingDirection.NONE) {
            pendingEdgeDirection = EdgePagingDirection.NONE
            edgeDwellJob?.cancel()
            edgeDwellJob = null
          }
        }
      } else if (currentPointerPos.x in (viewportWidth - edgeZonePx)..viewportWidth) {
        if (pendingEdgeDirection != EdgePagingDirection.RIGHT) {
          pendingEdgeDirection = EdgePagingDirection.RIGHT
          AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_ENTER direction=RIGHT page=${pagerState.currentPage}")
          edgeDwellJob?.cancel()
          edgeDwellJob = coroutineScope.launch {
            delay(EDGE_DWELL_DELAY_MS)
            performPageTransition(EdgePagingDirection.RIGHT)
          }
        }
      } else {
        if (pendingEdgeDirection != EdgePagingDirection.NONE) {
          pendingEdgeDirection = EdgePagingDirection.NONE
          edgeDwellJob?.cancel()
          edgeDwellJob = null
        }
      }
    }
  }

  fun handleEndDrag() {
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    isTransitioningPage = false
    pendingEdgeDirection = EdgePagingDirection.NONE

    try {
      val dragged = draggedPlacement
      if (dragged != null) {
        if (isOverBin) {
          AppLogger.i(AppLogger.Category.LAUNCHER, "DROP removal item=${dragged.id}")
          onRemovePlacement(dragged.id)
        } else {
          val targetPage = pagerState.currentPage
          val targetPos = previewTargetSlot ?: dragged.positionIndex
          AppLogger.i(AppLogger.Category.LAUNCHER, "DROP item=${dragged.packageName ?: dragged.id} page=$targetPage pos=$targetPos")
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onMovePlacement(dragged.id, targetPage, targetPos)
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
    pendingEdgeDirection = EdgePagingDirection.NONE
    isDragging = false
    draggedPlacement = null
    previewTargetSlot = null
    targetHoverPlacement = null
    isOverBin = false
    extraPagesCount = 0
    AppLogger.i(AppLogger.Category.LAUNCHER, "DRAG_CANCEL")
  }

  Box(
    modifier = modifier
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
            val cols = space.gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)
            val rows = 5
            val totalSlots = cols * rows

            // Check if rootOffset is in any slot with an item
            val hitSlot = slotBounds.entries.firstOrNull { it.value.contains(rootOffset) }?.key
            if (hitSlot != null) {
              touchedPlacement = effectivePlacements.firstOrNull { it.pageIndex == activePage && it.positionIndex == hitSlot }
            }
            if (touchedPlacement == null) {
              val bounds = pageGridBounds
              if (bounds != null && bounds.contains(rootOffset)) {
                val colWidth = bounds.width / cols
                val rowHeight = bounds.height / rows
                val c = ((rootOffset.x - bounds.left) / colWidth).toInt().coerceIn(0, cols - 1)
                val r = ((rootOffset.y - bounds.top) / rowHeight).toInt().coerceIn(0, rows - 1)
                val touchedSlot = r * cols + c
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
        // Vertical continuous scrolling layout
        LazyVerticalGrid(
          columns = GridCells.Fixed(space.gridColumns),
          modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("layer1_scroll_grid"),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
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
          val cols = space.gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)
          val rows = 5

          val rawPagePlacements = effectivePlacements.filter { it.pageIndex == page }
          val otherPlacements = if (isDragging && draggedPlacement != null) {
            rawPagePlacements.filter { it.id != draggedPlacement!!.id }
          } else {
            rawPagePlacements
          }

          val previewSlotsMap = remember(otherPlacements, isDragging, isCurrentPage, isOverBin, previewTargetSlot) {
            if (isDragging && isCurrentPage && !isOverBin && previewTargetSlot != null && draggedPlacement != null) {
              val target = previewTargetSlot!!
              val dragged = draggedPlacement!!
              val occupying = otherPlacements.firstOrNull { it.positionIndex == target }
              val map = mutableMapOf<Int, SpaceItemPlacement>()
              for (p in otherPlacements) {
                if (occupying != null && p.id == occupying.id) {
                  if (dragged.pageIndex == page) {
                    map[dragged.positionIndex] = p.copy(positionIndex = dragged.positionIndex)
                  } else {
                    val occupied = otherPlacements.map { it.positionIndex }.toSet() + target
                    var free = 0
                    while (occupied.contains(free)) {
                      free++
                    }
                    map[free] = p.copy(positionIndex = free)
                  }
                } else {
                  map[p.positionIndex] = p
                }
              }
              map
            } else {
              otherPlacements.associateBy { it.positionIndex }
            }
          }

          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 8.dp, vertical = 4.dp)
              .onGloballyPositioned { coordinates ->
                if (isCurrentPage && rootCoordinates != null) {
                  val localOffset = rootCoordinates!!.localPositionOf(coordinates, Offset.Zero)
                  pageGridBounds = Rect(localOffset, coordinates.size.toSize())
                }
              }
          ) {
            for (r in 0 until rows) {
              Row(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                        appLookup = appLookup,
                        allApps = allApps,
                        getBitmap = getBitmap,
                        iconSizeModifier = iconSizeModifier
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
      if (pendingEdgeDirection == EdgePagingDirection.LEFT) {
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
      } else if (pendingEdgeDirection == EdgePagingDirection.RIGHT) {
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
        targetValue = 1.15f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dragScale"
      )

      Box(
        modifier = Modifier
          .offset {
            IntOffset(
              (currentPointerPos.x - 32.dp.toPx()).roundToInt(),
              (currentPointerPos.y - 32.dp.toPx()).roundToInt()
            )
          }
          .size(64.dp)
          .graphicsLayer {
            scaleX = dragScale
            scaleY = dragScale
            shadowElevation = 20.dp.toPx()
            shape = ShapeRoundMd
            clip = true
          }
          .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
          .border(2.dp, QuantumViolet, ShapeRoundMd)
          .zIndex(999f)
          .testTag("floating_dragged_item"),
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
          Text(
            text = app?.label?.take(1) ?: "?",
            fontWeight = FontWeight.Bold,
            color = QuantumViolet,
            fontSize = 20.sp
          )
        }
      }
    }
  }
}

@Composable
private fun DropTargetPreviewSlot(
  dragged: SpaceItemPlacement,
  appLookup: Map<String, DiscoveredApp>,
  allApps: List<DiscoveredApp>,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  iconSizeModifier: Modifier,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.14f,
    targetValue = 0.28f,
    animationSpec = infiniteRepeatable(
      animation = tween(700, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glowAlpha"
  )

  val key = "${dragged.packageName}/${dragged.componentName}"
  val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == dragged.packageName }
  val bitmap = app?.let { getBitmap(it) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(2.dp)
      .clip(ShapeRoundMd)
      .background(QuantumViolet.copy(alpha = glowAlpha))
      .border(AppDimens.BorderThick, QuantumViolet, ShapeRoundMd),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(2.dp)
    ) {
      Box(
        modifier = iconSizeModifier
          .clip(ShapeRoundMd),
        contentAlignment = Alignment.Center
      ) {
        if (dragged.isFolder) {
          Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = QuantumViolet.copy(alpha = 0.6f),
            modifier = Modifier.size(AppDimens.IconMd)
          )
        } else if (bitmap != null) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            alpha = 0.5f,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Text(
            text = app?.label?.take(1) ?: "?",
            fontWeight = FontWeight.Bold,
            color = QuantumViolet.copy(alpha = 0.7f),
            fontSize = 18.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = "Place here",
        style = MaterialTheme.typography.labelSmall,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = QuantumViolet,
        maxLines = 1
      )
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
    modifier = modifier
      .fillMaxSize()
      .padding(2.dp),
    contentAlignment = Alignment.Center
  ) {
    if (isDragging) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(ShapeRoundMd)
          .background(QuantumViolet.copy(alpha = 0.04f))
          .border(
            width = 1.dp,
            color = QuantumViolet.copy(alpha = 0.15f),
            shape = ShapeRoundMd
          ),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(QuantumViolet.copy(alpha = 0.25f))
        )
      }
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
      .fillMaxSize()
      .onGloballyPositioned { coordinates ->
        rootCoordinates?.let { root ->
          val localOffset = root.localPositionOf(coordinates, Offset.Zero)
          onPositioned(Rect(localOffset, coordinates.size.toSize()))
        } ?: onPositioned(coordinates.boundsInRoot())
      }
      .clip(ShapeRoundMd)
      .then(
        if (isTargetHover) {
          Modifier.border(AppDimens.BorderThick, QuantumViolet, ShapeRoundMd)
        } else {
          Modifier
        }
      )
      .graphicsLayer {
        alpha = if (isBeingDragged) 0.25f else 1.0f
        scaleX = if (isTargetHover) 1.08f else 1.0f
        scaleY = if (isTargetHover) 1.08f else 1.0f
      }
      .clickable {
        if (placement.isFolder) {
          if (folder != null) onOpenFolder(folder)
        } else {
          if (app != null) onLaunchApp(app)
        }
      }
      .padding(horizontal = 2.dp, vertical = 2.dp)
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
      // App Item
      Box(
        modifier = iconSizeModifier
          .clip(ShapeRoundMd)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
      ) {
        val bitmap = app?.let { getBitmap(it) }
        if (bitmap != null) {
          Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = app.label,
            modifier = Modifier.fillMaxSize()
          )
        } else {
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
