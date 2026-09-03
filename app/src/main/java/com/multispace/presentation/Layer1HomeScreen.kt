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

private sealed interface GridDisplayItem {
  data class AppOrFolder(val placement: SpaceItemPlacement) : GridDisplayItem
  data object DropPlaceholder : GridDisplayItem
  data class DraggedAnchor(val placement: SpaceItemPlacement) : GridDisplayItem
}

private fun calculateTargetPosition(
  pointerPos: Offset,
  gridBounds: Rect?,
  columns: Int,
  itemCount: Int,
  density: Float
): Int {
  if (gridBounds == null || gridBounds.width <= 0f || gridBounds.height <= 0f) {
    return itemCount
  }
  if (itemCount <= 0) {
    return 0
  }

  val hSpacingPx = 8f * density
  val vSpacingPx = 16f * density

  val relX = (pointerPos.x - gridBounds.left).coerceIn(0f, gridBounds.width)
  val relY = (pointerPos.y - gridBounds.top).coerceAtLeast(0f)

  val colWidth = ((gridBounds.width - (columns - 1) * hSpacingPx) / columns).coerceAtLeast(1f)
  val colStep = colWidth + hSpacingPx

  val rowHeight = 84f * density
  val rowStep = rowHeight + vSpacingPx

  val col = (relX / colStep).toInt().coerceIn(0, columns - 1)
  val row = (relY / rowStep).toInt().coerceAtLeast(0)

  val calculatedIndex = row * columns + col
  return calculatedIndex.coerceIn(0, itemCount)
}

@Composable
private fun DropPlaceholderCell(
  space: Space,
  iconSizeModifier: Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "placeholder_glow")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 0.9f,
    animationSpec = infiniteRepeatable(
      animation = tween(850, easing = EaseInOutCubic),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .padding(AppDimens.Spacing4)
      .testTag("drop_placeholder_cell")
  ) {
    Box(
      modifier = iconSizeModifier
        .clip(ShapeRoundMd)
        .background(QuantumViolet.copy(alpha = 0.14f * pulseAlpha))
        .border(
          width = 2.dp,
          color = QuantumViolet.copy(alpha = pulseAlpha),
          shape = ShapeRoundMd
        ),
      contentAlignment = Alignment.Center
    ) {
      Box(
        modifier = Modifier
          .size(12.dp)
          .clip(CircleShape)
          .background(QuantumViolet.copy(alpha = pulseAlpha))
      )
    }

    if (space.labelVisibility) {
      Spacer(modifier = Modifier.height(AppDimens.Spacing4))
      Box(
        modifier = Modifier
          .height(14.dp)
          .width(44.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(QuantumViolet.copy(alpha = 0.14f * pulseAlpha))
      )
    }
  }
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
  // guaranteeing no apps are duplicated in Layer 1.
  val effectivePlacements = remember(placements, allApps, space) {
    val rawList = if (placements.isNotEmpty()) {
      placements
    } else if (allApps.isNotEmpty()) {
      val uniqueApps = allApps.distinctBy { it.packageName }
      val cols = space.gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)
      val pageSize = (cols * 5).coerceAtLeast(1)
      uniqueApps.mapIndexed { idx, app ->
        SpaceItemPlacement(
          id = "virtual_${app.packageName}_$idx",
          spaceId = space.id,
          layer = SpaceItemPlacement.LAYER_HOME,
          pageIndex = idx / pageSize,
          positionIndex = idx % pageSize,
          itemType = SpaceItemPlacement.ITEM_TYPE_APP,
          packageName = app.packageName,
          componentName = app.activityName,
          userHandleId = app.userHandleId
        )
      }
    } else {
      emptyList()
    }

    val seenPackages = mutableSetOf<String>()
    rawList.filter { placement ->
      if (placement.itemType == SpaceItemPlacement.ITEM_TYPE_APP) {
        val pkg = placement.packageName
        if (!pkg.isNullOrBlank()) {
          seenPackages.add(pkg)
        } else {
          true
        }
      } else {
        true
      }
    }
  }

  // Active dragging state
  var draggedPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var targetHoverPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var isDragging by remember { mutableStateOf(false) }
  var currentPointerPos by remember { mutableStateOf(Offset.Zero) }
  var binBounds by remember { mutableStateOf<Rect?>(null) }
  var isOverBin by remember { mutableStateOf(false) }
  var previewTargetPosition by remember { mutableIntStateOf(0) }

  // Viewport dimensions for edge zone calculation
  var viewportWidth by remember { mutableFloatStateOf(0f) }
  var viewportHeight by remember { mutableFloatStateOf(0f) }

  val cellBounds = remember { mutableStateMapOf<String, Rect>() }
  val pageGridBounds = remember { mutableStateMapOf<Int, Rect>() }

  // Page management with dynamic trailing page expansion
  var highestActivePage by remember { mutableIntStateOf(0) }
  val maxPageInPlacements = effectivePlacements.maxOfOrNull { it.pageIndex } ?: 0
  val basePageCount = maxOf(maxPageInPlacements + 1, highestActivePage + 1).coerceAtLeast(1)
  var extraPagesCount by remember { mutableIntStateOf(0) }
  val totalPageCount = (basePageCount + extraPagesCount).coerceAtLeast(1)

  val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPageCount })

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

  fun updatePreviewForPage(page: Int) {
    val gridBounds = pageGridBounds[page]
    val remainingCount = if (draggedPlacement?.pageIndex == page) {
      effectivePlacements.count { it.pageIndex == page && it.id != draggedPlacement?.id }
    } else {
      effectivePlacements.count { it.pageIndex == page }
    }
    previewTargetPosition = calculateTargetPosition(
      pointerPos = currentPointerPos,
      gridBounds = gridBounds,
      columns = space.gridColumns,
      itemCount = remainingCount,
      density = density.density
    )
  }

  fun performPageTransition(direction: EdgePagingDirection) {
    if (!isDragging || isTransitioningPage || pagerState.isScrollInProgress) return

    if (direction == EdgePagingDirection.LEFT && pagerState.currentPage > 0) {
      isTransitioningPage = true
      val fromPage = pagerState.currentPage
      val targetPage = fromPage - 1
      AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_START from=$fromPage to=$targetPage")
      coroutineScope.launch {
        try {
          pagerState.animateScrollToPage(targetPage, animationSpec = tween(PAGE_TRANSITION_DURATION_MS))
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_COMPLETE page=$targetPage")
          updatePreviewForPage(targetPage)
        } finally {
          isTransitioningPage = false
          pendingEdgeDirection = EdgePagingDirection.NONE
          // If finger remains in the left edge zone, schedule next transition
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
            highestActivePage = maxOf(highestActivePage, targetPage)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          }
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_START from=$fromPage to=$targetPage")
          pagerState.animateScrollToPage(targetPage, animationSpec = tween(PAGE_TRANSITION_DURATION_MS))
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_COMPLETE page=$targetPage")
          updatePreviewForPage(targetPage)
        } finally {
          isTransitioningPage = false
          pendingEdgeDirection = EdgePagingDirection.NONE
          // If finger remains in the right edge zone, schedule next transition
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
    val cell = cellBounds[placement.id]
    currentPointerPos = if (cell != null) cell.topLeft + startOffset else startOffset
    previewTargetPosition = placement.positionIndex
    isOverBin = false
    targetHoverPlacement = null
    pendingEdgeDirection = EdgePagingDirection.NONE
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    AppLogger.i(AppLogger.Category.LAUNCHER, "DRAG_START item=${placement.packageName ?: placement.id} page=${placement.pageIndex}")
  }

  fun handleDragDelta(delta: Offset) {
    currentPointerPos += delta
    val overBin = binBounds?.contains(currentPointerPos) == true
    isOverBin = overBin

    if (overBin) {
      targetHoverPlacement = null
      if (pendingEdgeDirection != EdgePagingDirection.NONE) {
        pendingEdgeDirection = EdgePagingDirection.NONE
        edgeDwellJob?.cancel()
        edgeDwellJob = null
      }
      return
    }

    val currentActivePage = pagerState.currentPage
    highestActivePage = maxOf(highestActivePage, currentActivePage)

    // Calculate live preview target position on current active page
    updatePreviewForPage(currentActivePage)

    // Hover target detection for folder creation
    targetHoverPlacement = cellBounds.entries.firstOrNull { (id, rect) ->
      if (id == draggedPlacement?.id) return@firstOrNull false
      val p = effectivePlacements.firstOrNull { it.id == id }
      p?.pageIndex == currentActivePage && rect.contains(currentPointerPos)
    }?.key?.let { id -> effectivePlacements.firstOrNull { it.id == id } }

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
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else if (targetHoverPlacement != null) {
          val target = targetHoverPlacement!!
          if (!target.isFolder && !dragged.isFolder) {
            val srcApp = appLookup["${dragged.packageName}/${dragged.componentName}"]
              ?: allApps.firstOrNull { it.packageName == dragged.packageName }
            val tgtApp = appLookup["${target.packageName}/${target.componentName}"]
              ?: allApps.firstOrNull { it.packageName == target.packageName }
            if (srcApp != null && tgtApp != null) {
              AppLogger.i(AppLogger.Category.LAUNCHER, "DROP create folder from ${srcApp.packageName} and ${tgtApp.packageName}")
              onCreateFolderFromApps(srcApp, tgtApp, dragged.id, target.id)
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
          } else {
            AppLogger.i(AppLogger.Category.LAUNCHER, "DROP hover move item=${dragged.id} page=${target.pageIndex} pos=${target.positionIndex}")
            onMovePlacement(dragged.id, target.pageIndex, target.positionIndex)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          }
        } else {
          // Drop onto empty space or target page
          val targetPage = if (space.layer1DisplayMode == Space.DISPLAY_MODE_SCROLL) 0 else pagerState.currentPage
          val targetPos = previewTargetPosition
          highestActivePage = maxOf(highestActivePage, targetPage)
          AppLogger.i(AppLogger.Category.LAUNCHER, "DROP item=${dragged.packageName ?: dragged.id} page=$targetPage pos=$targetPos")
          onMovePlacement(dragged.id, targetPage, targetPos)
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
      }
    } finally {
      isDragging = false
      draggedPlacement = null
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
    targetHoverPlacement = null
    isOverBin = false
    extraPagesCount = 0
    AppLogger.i(AppLogger.Category.LAUNCHER, "DRAG_CANCEL")
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .onGloballyPositioned { coordinates ->
        viewportWidth = coordinates.size.width.toFloat()
        viewportHeight = coordinates.size.height.toFloat()
      }
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Main content: either Paged or Scrolling
      if (space.layer1DisplayMode == Space.DISPLAY_MODE_SCROLL) {
        // Vertical continuous scrolling layout with live placement preview
        val isCurrentDrag = isDragging && !isOverBin
        val visiblePlacements = if (isDragging && draggedPlacement != null) {
          effectivePlacements.filter { it.id != draggedPlacement!!.id }
        } else {
          effectivePlacements
        }

        val displayItems = remember(effectivePlacements, isDragging, draggedPlacement, isCurrentDrag, previewTargetPosition) {
          if (!isDragging || draggedPlacement == null) {
            effectivePlacements.map { GridDisplayItem.AppOrFolder(it) }
          } else {
            val list = mutableListOf<GridDisplayItem>()
            val targetPos = previewTargetPosition.coerceIn(0, visiblePlacements.size)

            for (i in 0 until visiblePlacements.size) {
              if (isCurrentDrag && i == targetPos) {
                list.add(GridDisplayItem.DropPlaceholder)
              }
              list.add(GridDisplayItem.AppOrFolder(visiblePlacements[i]))
            }
            if (isCurrentDrag && targetPos >= visiblePlacements.size) {
              list.add(GridDisplayItem.DropPlaceholder)
            }
            if (draggedPlacement != null) {
              list.add(GridDisplayItem.DraggedAnchor(draggedPlacement!!))
            }
            list
          }
        }

        LazyVerticalGrid(
          columns = GridCells.Fixed(space.gridColumns),
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing8)
            .onGloballyPositioned { coords ->
              pageGridBounds[0] = coords.boundsInRoot()
            }
            .testTag("layer1_scroll_grid"),
          horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
          verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
        ) {
          items(
            items = displayItems,
            key = { item ->
              when (item) {
                is GridDisplayItem.AppOrFolder -> item.placement.id
                is GridDisplayItem.DropPlaceholder -> "__drag_drop_placeholder__"
                is GridDisplayItem.DraggedAnchor -> item.placement.id
              }
            }
          ) { item ->
            Box(modifier = Modifier.animateItem()) {
              when (item) {
                is GridDisplayItem.AppOrFolder -> {
                  Layer1ItemCell(
                    placement = item.placement,
                    space = space,
                    appLookup = appLookup,
                    folderLookup = folderLookup,
                    allApps = allApps,
                    iconSizeModifier = iconSizeModifier,
                    getBitmap = getBitmap,
                    onLaunchApp = onLaunchApp,
                    onOpenFolder = onOpenFolder,
                    onPositioned = { rect -> cellBounds[item.placement.id] = rect },
                    onStartDrag = { offset -> handleStartDrag(item.placement, offset) },
                    onDrag = { delta -> handleDragDelta(delta) },
                    onEndDrag = { handleEndDrag() },
                    onCancelDrag = { handleCancelDrag() },
                    isBeingDragged = false,
                    isTargetHover = targetHoverPlacement?.id == item.placement.id
                  )
                }
                is GridDisplayItem.DropPlaceholder -> {
                  DropPlaceholderCell(space = space, iconSizeModifier = iconSizeModifier)
                }
                is GridDisplayItem.DraggedAnchor -> {
                  Layer1ItemCell(
                    placement = item.placement,
                    space = space,
                    appLookup = appLookup,
                    folderLookup = folderLookup,
                    allApps = allApps,
                    iconSizeModifier = iconSizeModifier,
                    getBitmap = getBitmap,
                    onLaunchApp = onLaunchApp,
                    onOpenFolder = onOpenFolder,
                    onPositioned = { rect -> cellBounds[item.placement.id] = rect },
                    onStartDrag = { offset -> handleStartDrag(item.placement, offset) },
                    onDrag = { delta -> handleDragDelta(delta) },
                    onEndDrag = { handleEndDrag() },
                    onCancelDrag = { handleCancelDrag() },
                    isBeingDragged = true,
                    isTargetHover = false
                  )
                }
              }
            }
          }
        }
      } else {
        // Horizontal paged layout with edge-paging and dynamic trailing page support
        HorizontalPager(
          state = pagerState,
          userScrollEnabled = !isDragging,
          beyondViewportPageCount = totalPageCount.coerceIn(1, 10),
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .testTag("layer1_horizontal_pager")
        ) { page ->
          val pagePlacements = effectivePlacements.filter { it.pageIndex == page }.sortedBy { it.positionIndex }
          val isCurrentDragPage = isDragging && pagerState.currentPage == page && !isOverBin
          val isSourcePage = isDragging && draggedPlacement?.pageIndex == page
          val visiblePlacements = if (isSourcePage) {
            pagePlacements.filter { it.id != draggedPlacement?.id }
          } else {
            pagePlacements
          }

          val displayItems = remember(pagePlacements, isDragging, draggedPlacement, isCurrentDragPage, isSourcePage, previewTargetPosition) {
            if (!isDragging || draggedPlacement == null) {
              pagePlacements.map { GridDisplayItem.AppOrFolder(it) }
            } else {
              val list = mutableListOf<GridDisplayItem>()
              val targetPos = previewTargetPosition.coerceIn(0, visiblePlacements.size)

              for (i in 0 until visiblePlacements.size) {
                if (isCurrentDragPage && i == targetPos) {
                  list.add(GridDisplayItem.DropPlaceholder)
                }
                list.add(GridDisplayItem.AppOrFolder(visiblePlacements[i]))
              }
              if (isCurrentDragPage && targetPos >= visiblePlacements.size) {
                list.add(GridDisplayItem.DropPlaceholder)
              }
              // Retain source item's anchor in its origin page to preserve continuous pointer gesture
              if (isSourcePage && draggedPlacement != null) {
                list.add(GridDisplayItem.DraggedAnchor(draggedPlacement!!))
              }
              list
            }
          }

          if (displayItems.isEmpty()) {
            // Empty placeholder page for trailing pages when not actively dragged over
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing8),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Drop app here to place on Page ${page + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
              )
            }
          } else {
            LazyVerticalGrid(
              columns = GridCells.Fixed(space.gridColumns),
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing8)
                .onGloballyPositioned { coords ->
                  pageGridBounds[page] = coords.boundsInRoot()
                },
              horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
              verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
            ) {
              items(
                items = displayItems,
                key = { item ->
                  when (item) {
                    is GridDisplayItem.AppOrFolder -> item.placement.id
                    is GridDisplayItem.DropPlaceholder -> "__drag_drop_placeholder__"
                    is GridDisplayItem.DraggedAnchor -> item.placement.id
                  }
                }
              ) { item ->
                Box(modifier = Modifier.animateItem()) {
                  when (item) {
                    is GridDisplayItem.AppOrFolder -> {
                      Layer1ItemCell(
                        placement = item.placement,
                        space = space,
                        appLookup = appLookup,
                        folderLookup = folderLookup,
                        allApps = allApps,
                        iconSizeModifier = iconSizeModifier,
                        getBitmap = getBitmap,
                        onLaunchApp = onLaunchApp,
                        onOpenFolder = onOpenFolder,
                        onPositioned = { rect -> cellBounds[item.placement.id] = rect },
                        onStartDrag = { offset -> handleStartDrag(item.placement, offset) },
                        onDrag = { delta -> handleDragDelta(delta) },
                        onEndDrag = { handleEndDrag() },
                        onCancelDrag = { handleCancelDrag() },
                        isBeingDragged = false,
                        isTargetHover = targetHoverPlacement?.id == item.placement.id
                      )
                    }
                    is GridDisplayItem.DropPlaceholder -> {
                      DropPlaceholderCell(space = space, iconSizeModifier = iconSizeModifier)
                    }
                    is GridDisplayItem.DraggedAnchor -> {
                      Layer1ItemCell(
                        placement = item.placement,
                        space = space,
                        appLookup = appLookup,
                        folderLookup = folderLookup,
                        allApps = allApps,
                        iconSizeModifier = iconSizeModifier,
                        getBitmap = getBitmap,
                        onLaunchApp = onLaunchApp,
                        onOpenFolder = onOpenFolder,
                        onPositioned = { rect -> cellBounds[item.placement.id] = rect },
                        onStartDrag = { offset -> handleStartDrag(item.placement, offset) },
                        onDrag = { delta -> handleDragDelta(delta) },
                        onEndDrag = { handleEndDrag() },
                        onCancelDrag = { handleCancelDrag() },
                        isBeingDragged = true,
                        isTargetHover = false
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
      onPositioned = { binBounds = it },
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = AppDimens.Spacing8)
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

      Box(
        modifier = Modifier
          .offset {
            IntOffset(
              (currentPointerPos.x - 30.dp.toPx()).roundToInt(),
              (currentPointerPos.y - 30.dp.toPx()).roundToInt()
            )
          }
          .graphicsLayer {
            scaleX = 1.12f
            scaleY = 1.12f
            shadowElevation = 16.dp.toPx()
            shape = ShapeRoundMd
            clip = false
          }
          .size(60.dp)
          .clip(ShapeRoundMd)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .border(AppDimens.BorderThick, QuantumViolet, ShapeRoundMd)
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
  onStartDrag: (Offset) -> Unit,
  onDrag: (Offset) -> Unit,
  onEndDrag: () -> Unit,
  onCancelDrag: () -> Unit,
  isBeingDragged: Boolean,
  isTargetHover: Boolean
) {
  val key = "${placement.packageName}/${placement.componentName}"
  val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == placement.packageName }
  val folder = if (placement.isFolder) folderLookup[placement.folderId] else null

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .onGloballyPositioned { coordinates ->
        onPositioned(coordinates.boundsInRoot())
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
        alpha = if (isBeingDragged) 0f else 1.0f
        scaleX = if (isTargetHover) 1.08f else 1.0f
        scaleY = if (isTargetHover) 1.08f else 1.0f
      }
      .pointerInput(placement.id) {
        detectDragGesturesAfterLongPress(
          onDragStart = { offset -> onStartDrag(offset) },
          onDrag = { change, dragAmount ->
            change.consume()
            onDrag(dragAmount)
          },
          onDragEnd = { onEndDrag() },
          onDragCancel = { onCancelDrag() }
        )
      }
      .clickable {
        if (placement.isFolder) {
          if (folder != null) onOpenFolder(folder)
        } else {
          if (app != null) onLaunchApp(app)
        }
      }
      .padding(AppDimens.Spacing4)
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
