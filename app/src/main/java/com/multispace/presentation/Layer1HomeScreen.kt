package com.multispace.presentation

import android.appwidget.AppWidgetHost
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInFull
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.presentation.gesture.*
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.*
import com.multispace.presentation.widget.DesktopWidgetView
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
  onCreateFolderFromApps: (sourceApp: DiscoveredApp, targetApp: DiscoveredApp, sourcePlacementId: String?, targetPlacementId: String?, targetPage: Int, targetPosition: Int) -> Unit = { _, _, _, _, _, _ -> },
  onAddAppToExistingFolder: (folderId: String, app: DiscoveredApp, sourcePlacementId: String) -> Unit = { _, _, _ -> },
  onAddAppToHome: (DiscoveredApp, Int) -> Unit,
  onMovePlacement: (placementId: String, targetPage: Int, targetPos: Int, pageSize: Int) -> Unit = { _, _, _, _ -> },
  onResizeWidget: (placementId: String, spanX: Int, spanY: Int, positionIndex: Int?) -> Unit = { _, _, _, _ -> },
  onOpenCustomization: (Int) -> Unit = {},
  onOpenAppInfo: (DiscoveredApp) -> Unit = {},
  onUninstallApp: (DiscoveredApp) -> Unit = {},
  onForceStopApp: (DiscoveredApp) -> Unit = {},
  appWidgetHost: AppWidgetHost? = null,
  unifiedDragState: UnifiedDragState? = null,
  onDropItemToDock: ((placement: SpaceItemPlacement, app: DiscoveredApp, targetDockIndex: Int) -> Unit)? = null,
  isSwipeAllowed: Boolean = false,
  onEmptySpaceSwipeStart: () -> Unit = {},
  onEmptySpaceSwipeMove: (dragAmount: Float, change: PointerInputChange) -> Unit = { _, _ -> },
  onEmptySpaceSwipeEnd: () -> Unit = {},
  onEmptySpaceSwipeCancel: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  BoxWithConstraints(
    modifier = modifier.fillMaxSize()
  ) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val appLookup = remember(allApps) {
      AppIdentityLookup(allApps)
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
    val availableGridWidthDp = (maxWidth - gridHorizontalPadding * 2).coerceAtLeast(100.dp)
    val cellWidth = (availableGridWidthDp - appSpacing * (cols - 1)) / cols

    // Authoritative Layer 1 placements:
    // When placements exist in Room, honor them directly (preserving preset curation, widgets, and user drag-and-drop moves).
    // Never auto-inject unplaced apps or dock apps onto Page 0 over widgets or curated empty space.
    // CRITICAL: Preset layouts place initial Page 0 apps at the bottom row (cols count),
    // and users can freely drag them anywhere on the page, BUT NO APP MAY EVER BE LAID ON A WIDGET!
    val effectivePlacements = remember(placements, allApps, space.id, pageSize, gridRows, cols) {
      val basePlacements = if (placements.isNotEmpty()) {
        val seenAppIdentities = mutableSetOf<AppIdentity>()
        val deduplicated = mutableListOf<SpaceItemPlacement>()
        for (p in placements) {
          if (p.isFolder || p.isWidget) {
            deduplicated.add(p)
          } else {
            val identity = p.appIdentity
            if (identity != null) {
              if (seenAppIdentities.add(identity)) {
                deduplicated.add(p)
              }
            } else {
              deduplicated.add(p)
            }
          }
        }
        deduplicated
      } else {
        // Fallback only if there are absolutely NO placements in Room yet.
        // Default layout: exactly cols apps on Page 0 at the bottom row (lastRow * cols + i)
        val lastRow = (gridRows - 1).coerceAtLeast(0)
        val distinctApps = allApps.distinctBy { it.appIdentity }
        val page0Count = minOf(cols, distinctApps.size)
        val fallbackList = mutableListOf<SpaceItemPlacement>()
        for (i in 0 until page0Count) {
          val app = distinctApps[i]
          fallbackList.add(
            SpaceItemPlacement(
              id = "fallback:${app.id}",
              spaceId = space.id,
              layer = SpaceItemPlacement.LAYER_HOME,
              pageIndex = 0,
              positionIndex = lastRow * cols + i,
              itemType = SpaceItemPlacement.ITEM_TYPE_APP,
              packageName = app.packageName,
              componentName = app.activityName,
              userHandleId = app.userHandleId
            )
          )
        }
        for (i in page0Count until distinctApps.size) {
          val app = distinctApps[i]
          val rem = i - page0Count
          fallbackList.add(
            SpaceItemPlacement(
              id = "fallback:${app.id}",
              spaceId = space.id,
              layer = SpaceItemPlacement.LAYER_HOME,
              pageIndex = 1 + (rem / pageSize),
              positionIndex = rem % pageSize,
              itemType = SpaceItemPlacement.ITEM_TYPE_APP,
              packageName = app.packageName,
              componentName = app.activityName,
              userHandleId = app.userHandleId
            )
          )
        }
        fallbackList
      }

      // CRITICAL GUARANTEE: NO APP MAY EVER BE LAID ON A WIDGET!
      val widgetSlotsByPage = mutableMapOf<Int, MutableSet<Int>>()
      for (p in basePlacements) {
        if (p.isWidget) {
          val r = (p.positionIndex / cols).coerceIn(0, gridRows - 1)
          val c = (p.positionIndex % cols).coerceIn(0, cols - 1)
          val sX = p.spanX.coerceIn(1, cols - c)
          val sY = p.spanY.coerceIn(1, gridRows - r)
          for (dr in 0 until sY) {
            for (dc in 0 until sX) {
              widgetSlotsByPage.getOrPut(p.pageIndex) { mutableSetOf() }.add((r + dr) * cols + (c + dc))
            }
          }
        }
      }

      val lastRow = (gridRows - 1).coerceAtLeast(0)
      val resolvedList = mutableListOf<SpaceItemPlacement>()
      val occupiedSlotsByPage = mutableMapOf<Int, MutableSet<Int>>()

      // 1. Keep all widgets and folders intact
      for (p in basePlacements) {
        if (p.isWidget || p.isFolder) {
          resolvedList.add(p)
          val r = (p.positionIndex / cols).coerceIn(0, gridRows - 1)
          val c = (p.positionIndex % cols).coerceIn(0, cols - 1)
          val sX = if (p.isWidget) p.spanX.coerceIn(1, cols - c) else 1
          val sY = if (p.isWidget) p.spanY.coerceIn(1, gridRows - r) else 1
          for (dr in 0 until sY) {
            for (dc in 0 until sX) {
              occupiedSlotsByPage.getOrPut(p.pageIndex) { mutableSetOf() }.add((r + dr) * cols + (c + dc))
            }
          }
        }
      }

      // 2. Validate and place apps: never allow an app on a widget
      val apps = basePlacements.filter { !it.isWidget && !it.isFolder }
      for (app in apps) {
        val page = app.pageIndex
        val pos = app.positionIndex
        val isCoveredByWidget = widgetSlotsByPage[page]?.contains(pos) == true
        val isSlotTaken = occupiedSlotsByPage[page]?.contains(pos) == true

        if (!isCoveredByWidget && !isSlotTaken) {
          // Valid placement: can be anywhere the user placed it, as long as it is not on a widget!
          resolvedList.add(app)
          occupiedSlotsByPage.getOrPut(page) { mutableSetOf() }.add(pos)
        } else {
          // Relocate app away from widget or collision
          var placed = false
          if (page == 0) {
            // If on Page 0: default to the bottom row first
            for (c in 0 until cols) {
              val candidatePos = lastRow * cols + c
              val onWidget = widgetSlotsByPage[0]?.contains(candidatePos) == true
              val taken = occupiedSlotsByPage[0]?.contains(candidatePos) == true
              if (!onWidget && !taken) {
                val relocated = app.copy(pageIndex = 0, positionIndex = candidatePos)
                resolvedList.add(relocated)
                occupiedSlotsByPage.getOrPut(0) { mutableSetOf() }.add(candidatePos)
                placed = true
                break
              }
            }
          }
          if (!placed) {
            // Find first available non-widget slot on Page 1 or beyond
            var searchPage = maxOf(1, page)
            var searchPos = 0
            while (!placed) {
              val onWidget = widgetSlotsByPage[searchPage]?.contains(searchPos) == true
              val taken = occupiedSlotsByPage[searchPage]?.contains(searchPos) == true
              if (!onWidget && !taken) {
                val relocated = app.copy(pageIndex = searchPage, positionIndex = searchPos)
                resolvedList.add(relocated)
                occupiedSlotsByPage.getOrPut(searchPage) { mutableSetOf() }.add(searchPos)
                placed = true
              } else {
                searchPos++
                if (searchPos >= pageSize) {
                  searchPage++
                  searchPos = 0
                }
              }
            }
          }
        }
      }

      val validationReport = PlacementValidator.validatePlacements(resolvedList, cols = cols, rows = gridRows)
      if (validationReport.hasIssues) {
        AppLogger.w(
          AppLogger.Category.LAUNCHER,
          "Placement validation detected ${validationReport.issues.size} issues on desktop layout for space '${space.name}': ${validationReport.issues.take(3)}"
        )
      }

      resolvedList
    }

  val dragSlopPx = with(density) { 8.dp.toPx() }
  var dragLifecycleState by remember { mutableStateOf(DragLifecycleState.IDLE) }
  var isDragging by remember { mutableStateOf(false) }
  var isDropping by remember { mutableStateOf(false) }
  var activeActionPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var draggedPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var pendingDragPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var currentPointerPos by remember { mutableStateOf(Offset.Zero) }
  var touchOffsetWithinItem by remember { mutableStateOf(Offset.Zero) }
  var accumulatedDragDistance by remember { mutableFloatStateOf(0f) }
  var lastLongPressTimestamp by remember { mutableLongStateOf(0L) }

  val launcherInteractionState by remember {
    derivedStateOf {
      when {
        isDragging -> LauncherInteractionState.DraggingApp(draggedPlacement?.id, draggedPlacement?.packageName)
        dragLifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE -> LauncherInteractionState.LongPressingApp(activeActionPlacement?.id)
        else -> LauncherInteractionState.Idle
      }
    }
  }

  val currentPlacements by rememberUpdatedState(effectivePlacements)
  val currentSpace by rememberUpdatedState(space)
  val currentAllApps by rememberUpdatedState(allApps)
  val currentAppLookup by rememberUpdatedState(appLookup)
  val currentFolderLookup by rememberUpdatedState(folderLookup)
  val currentOnLaunchApp by rememberUpdatedState(onLaunchApp)
  val currentOnOpenFolder by rememberUpdatedState(onOpenFolder)
  val currentOnOpenCustomization by rememberUpdatedState(onOpenCustomization)

  fun dismissActions() {
    activeActionPlacement = null
    pendingDragPlacement = null
    dragLifecycleState = DragLifecycleState.IDLE
  }

  var resizingWidgetId by remember { mutableStateOf<String?>(null) }
  var targetHoverPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var previewTargetSlot by remember { mutableStateOf<Int?>(null) }
  var binBounds by remember { mutableStateOf<Rect?>(null) }
  var isOverBin by remember { mutableStateOf(false) }

  // Root and page geometry measurements
  var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  var pagerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
  var pageGridBounds by remember { mutableStateOf<Rect?>(null) }
  var viewportWidth by remember { mutableFloatStateOf(0f) }
  var viewportHeight by remember { mutableFloatStateOf(0f) }

  val cellBounds = remember { mutableStateMapOf<String, Rect>() }
  val slotBounds = remember { mutableStateMapOf<Int, Rect>() }

  fun updatePageGridBounds() {
    val root = rootCoordinates
    val pagerCoords = pagerCoordinates
    if (root != null && pagerCoords != null && pagerCoords.isAttached && root.isAttached) {
      val localOffset = root.localPositionOf(pagerCoords, Offset.Zero)
      val pagerRect = Rect(localOffset, pagerCoords.size.toSize())
      val hPadPx = with(density) { gridHorizontalPadding.toPx() }
      val vPadPx = with(density) { gridVerticalPadding.toPx() }
      val bounds = Rect(
        left = pagerRect.left + hPadPx,
        top = pagerRect.top + vPadPx,
        right = pagerRect.right - hPadPx,
        bottom = pagerRect.bottom - vPadPx
      )
      pageGridBounds = bounds

      // Mathematically calculate slot bounds for the active grid page without per-cell measurement overhead
      val cellWPx = with(density) { cellWidth.toPx() }
      val cellHPx = with(density) { cellHeight.toPx() }
      val spacingPx = with(density) { appSpacing.toPx() }
      val newBounds = mutableMapOf<Int, Rect>()
      for (r in 0 until gridRows) {
        for (c in 0 until cols) {
          val left = bounds.left + c * (cellWPx + spacingPx)
          val top = bounds.top + r * rowPitchPx
          newBounds[r * cols + c] = Rect(left, top, left + cellWPx, top + cellHPx)
        }
      }
      slotBounds.clear()
      slotBounds.putAll(newBounds)
    }
  }

  fun getPlacementFootprintRect(item: SpaceItemPlacement): Rect? {
    val measured = cellBounds[item.id]
    val itemR = item.positionIndex / cols
    val itemC = item.positionIndex % cols
    val sX = if (item.isWidget) item.spanX.coerceIn(1, (cols - itemC).coerceAtLeast(1)) else 1
    val sY = if (item.isWidget) item.spanY.coerceIn(1, (gridRows - itemR).coerceAtLeast(1)) else 1

    val tlSlot = item.positionIndex
    val brSlot = (itemR + sY - 1) * cols + (itemC + sX - 1)
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
    val cellWPx = with(density) { cellWidth.toPx() }
    val cellHPx = with(density) { cellHeight.toPx() }
    val spacingPx = with(density) { appSpacing.toPx() }
    val mathRect = if (pageBounds != null && pageBounds.width > 0f) {
      val left = pageBounds.left + itemC * (cellWPx + spacingPx)
      val top = pageBounds.top + itemR * rowPitchPx
      val right = left + sX * cellWPx + (sX - 1) * spacingPx
      val bottom = top + sY * cellHPx + (sY - 1) * spacingPx
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

  fun findItemAtOffset(offset: Offset, page: Int): SpaceItemPlacement? {
    val pagePlacements = currentPlacements.filter {
      if (currentSpace.layer1DisplayMode == Space.DISPLAY_MODE_SCROLL) true else it.pageIndex == page
    }

    // PRIORITY 1: Physical footprint of widgets (ensuring multi-cell bounds participate fully)
    val hitWidget = pagePlacements.firstOrNull { item ->
      item.isWidget && getPlacementFootprintRect(item)?.contains(offset) == true
    }
    if (hitWidget != null) return hitWidget

    // PRIORITY 2: Physical footprint of apps and folders
    val hitItem = pagePlacements.firstOrNull { item ->
      !item.isWidget && getPlacementFootprintRect(item)?.contains(offset) == true
    }
    if (hitItem != null) return hitItem

    // PRIORITY 3: Slot-based hit testing (checking all slots covered by widgets or apps)
    val totalSlots = cols * gridRows
    val hitSlot = slotBounds.entries.firstOrNull { (slot, rect) ->
      slot < totalSlots && rect.contains(offset)
    }?.key
    if (hitSlot != null) {
      val slotR = hitSlot / cols
      val slotC = hitSlot % cols

      // Check if any widget covers this slot
      val coveringWidget = pagePlacements.firstOrNull { item ->
        if (!item.isWidget) return@firstOrNull false
        val itemR = item.positionIndex / cols
        val itemC = item.positionIndex % cols
        val sX = item.spanX.coerceIn(1, (cols - itemC).coerceAtLeast(1))
        val sY = item.spanY.coerceIn(1, (gridRows - itemR).coerceAtLeast(1))
        slotC in itemC until (itemC + sX) && slotR in itemR until (itemR + sY)
      }
      if (coveringWidget != null) return coveringWidget

      // Check app/folder on this slot
      val slotItem = pagePlacements.firstOrNull { item ->
        item.positionIndex == hitSlot
      }
      if (slotItem != null) return slotItem
    }

    // PRIORITY 4: Mathematical grid cell resolution if slotBounds missed
    val pageBounds = pageGridBounds
    if (pageBounds != null && pageBounds.width > 0f) {
      val cellWPx = with(density) { cellWidth.toPx() }
      val spacingPx = with(density) { appSpacing.toPx() }
      val colPitchPx = cellWPx + spacingPx
      val relX = offset.x - pageBounds.left
      val relY = offset.y - pageBounds.top
      if (relX >= 0f && relY >= 0f) {
        val calcC = (relX / colPitchPx).toInt().coerceIn(0, cols - 1)
        val calcR = (relY / rowPitchPx).toInt().coerceIn(0, gridRows - 1)

        val widgetAtGrid = pagePlacements.firstOrNull { item ->
          if (!item.isWidget) return@firstOrNull false
          val itemR = item.positionIndex / cols
          val itemC = item.positionIndex % cols
          val sX = item.spanX.coerceIn(1, (cols - itemC).coerceAtLeast(1))
          val sY = item.spanY.coerceIn(1, (gridRows - itemR).coerceAtLeast(1))
          calcC in itemC until (itemC + sX) && calcR in itemR until (itemR + sY)
        }
        if (widgetAtGrid != null) return widgetAtGrid

        val itemAtGrid = pagePlacements.firstOrNull { item ->
          item.positionIndex == (calcR * cols + calcC)
        }
        if (itemAtGrid != null) return itemAtGrid
      }
    }

    // PRIORITY 5: Touch slop margin tolerance (10dp) around widgets first, then apps
    val touchMargin = with(density) { 10.dp.toPx() }
    val nearWidget = pagePlacements.firstOrNull { item ->
      if (!item.isWidget) return@firstOrNull false
      val rect = getPlacementFootprintRect(item) ?: return@firstOrNull false
      Rect(rect.left - touchMargin, rect.top - touchMargin, rect.right + touchMargin, rect.bottom + touchMargin).contains(offset)
    }
    if (nearWidget != null) return nearWidget

    val nearItem = pagePlacements.firstOrNull { item ->
      val rect = getPlacementFootprintRect(item) ?: return@firstOrNull false
      Rect(rect.left - touchMargin, rect.top - touchMargin, rect.right + touchMargin, rect.bottom + touchMargin).contains(offset)
    }
    if (nearItem != null) return nearItem

    // ONLY when no widget or item was touched: returns null, indicating genuine empty desktop area!
    return null
  }

  // Page management with dynamic trailing page expansion
  val maxPageInPlacements = effectivePlacements.maxOfOrNull { it.pageIndex } ?: 0
  val basePageCount = maxOf(space.pageCount, (maxPageInPlacements + 1).coerceAtLeast(1))
  var extraPagesCount by remember { mutableIntStateOf(0) }
  val totalPageCount = (basePageCount + extraPagesCount).coerceAtLeast(1)

  val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPageCount })

  LaunchedEffect(pagerState.currentPage) {
    updatePageGridBounds()
  }

  LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
    if (pagerState.isScrollInProgress) {
      dismissActions()
    }
  }
  LaunchedEffect(space.id) {
    dismissActions()
    resizingWidgetId = null
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

  fun calculateSlotForPosition(pointerPos: Offset, spanX: Int = 1, spanY: Int = 1): Int {
    val totalSlots = cols * gridRows
    if (viewportWidth <= 0f || viewportHeight <= 0f) return 0

    // Check direct hit on measured slot bounds first
    val directHit = slotBounds.entries.firstOrNull { (slot, rect) ->
      slot < totalSlots && rect.contains(pointerPos)
    }?.key

    val rawSlot: Int = if (directHit != null) {
      directHit
    } else {
      val bounds = pageGridBounds
      if (bounds != null && bounds.width > 0f) {
        val colWidth = (bounds.width / cols).coerceAtLeast(1f)
        val c = when {
          pointerPos.x <= bounds.left -> 0
          pointerPos.x >= bounds.right -> cols - 1
          else -> ((pointerPos.x - bounds.left) / colWidth).toInt().coerceIn(0, cols - 1)
        }

        val r = when {
          pointerPos.y <= bounds.top -> 0
          else -> {
            val relativeY = pointerPos.y - bounds.top
            (relativeY / rowPitchPx).toInt().coerceIn(0, gridRows - 1)
          }
        }

        (r * cols + c).coerceIn(0, totalSlots - 1)
      } else {
        val colWidth = (viewportWidth / cols).coerceAtLeast(1f)
        val c = (pointerPos.x / colWidth).toInt().coerceIn(0, cols - 1)
        val r = (pointerPos.y / rowPitchPx).toInt().coerceIn(0, gridRows - 1)
        (r * cols + c).coerceIn(0, totalSlots - 1)
      }
    }

    val rawC = rawSlot % cols
    val rawR = rawSlot / cols
    val clampedC = rawC.coerceIn(0, maxOf(0, cols - spanX))
    val clampedR = rawR.coerceIn(0, maxOf(0, gridRows - spanY))
    return (clampedR * cols + clampedC).coerceIn(0, totalSlots - 1)
  }

  fun updatePreviewTargetSlot(pointerPos: Offset = currentPointerPos) {
    if (viewportWidth <= 0f || viewportHeight <= 0f) return

    val item = draggedPlacement
    val cellWPx = with(density) { cellWidth.toPx() }
    val cellHPx = with(density) { cellHeight.toPx() }
    val touchOffset = touchOffsetWithinItem
    val targetPointerPos = if (item?.isWidget == true) {
      val widgetTopLeft = pointerPos - touchOffset
      Offset(
        widgetTopLeft.x + cellWPx / 2f,
        widgetTopLeft.y + cellHPx / 2f
      )
    } else {
      pointerPos
    }

    val draggedSpanX = if (item?.isWidget == true) item.spanX.coerceIn(1, cols) else 1
    val draggedSpanY = if (item?.isWidget == true) item.spanY.coerceIn(1, gridRows) else 1
    val targetPage = pagerState.currentPage
    val isApp = item?.let { !it.isWidget && !it.isFolder } == true
    val candidateSlot = calculateSlotForPosition(targetPointerPos, draggedSpanX, draggedSpanY)

    // Check if candidateSlot is covered by a widget on targetPage
    val isCandidateOverWidget = if (isApp) {
      effectivePlacements.any { other ->
        if (other.isWidget && (space.layer1DisplayMode == Space.DISPLAY_MODE_SCROLL || other.pageIndex == targetPage)) {
          val r = (other.positionIndex / cols).coerceIn(0, gridRows - 1)
          val c = (other.positionIndex % cols).coerceIn(0, cols - 1)
          val sX = other.spanX.coerceIn(1, cols - c)
          val sY = other.spanY.coerceIn(1, gridRows - r)
          val targetR = candidateSlot / cols
          val targetC = candidateSlot % cols
          targetC in c until (c + sX) && targetR in r until (r + sY)
        } else false
      }
    } else false

    val validCandidateSlot = if (isCandidateOverWidget) null else candidateSlot

    if (previewTargetSlot != validCandidateSlot) {
      previewTargetSlot = validCandidateSlot
      AppLogger.i(
        AppLogger.Category.LAUNCHER,
        "PREVIEW_TARGET: pointerPos=$pointerPos previewTargetSlot=$validCandidateSlot targetPage=$targetPage targetPos=$validCandidateSlot gridRows=$gridRows pageSize=$pageSize isCandidateOverWidget=$isCandidateOverWidget"
      )
      if (validCandidateSlot != null) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
    }

    if (isApp) {
      val hovered = (findItemAtOffset(pointerPos, targetPage)?.takeIf { it.id != item?.id && !it.isWidget }
        ?: effectivePlacements.firstOrNull { other ->
          other.id != item?.id && !other.isWidget &&
          (space.layer1DisplayMode == Space.DISPLAY_MODE_SCROLL || other.pageIndex == targetPage) &&
          (
            getPlacementFootprintRect(other)?.contains(pointerPos) == true ||
            cellBounds[other.id]?.contains(pointerPos) == true
          )
        })
      if (hovered != null && targetHoverPlacement?.id != hovered.id) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      }
      targetHoverPlacement = hovered
    } else {
      targetHoverPlacement = null
    }
  }

  // Cross-component drag listener: highlight desktop slots when dragging app from DockBar
  LaunchedEffect(unifiedDragState?.rootPointerPos, unifiedDragState?.currentTargetZone) {
    val uds = unifiedDragState
    if (uds != null && uds.isDragging && uds.dragSource == DragSource.DOCK_BAR) {
      if (uds.currentTargetZone == DragTargetZone.DESKTOP) {
        val coords = rootCoordinates
        if (coords != null && coords.isAttached) {
          val localPos = coords.rootToLocal(uds.rootPointerPos)
          val candidateSlot = calculateSlotForPosition(localPos, 1, 1)
          if (previewTargetSlot != candidateSlot) {
            previewTargetSlot = candidateSlot
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          }
          uds.targetDesktopPage = pagerState.currentPage
          uds.targetDesktopPosition = candidateSlot
        }
      } else {
        if (previewTargetSlot != null) {
          previewTargetSlot = null
        }
      }
    }
  }

  var handleDragMoveRef: ((Offset) -> Unit)? = null

  fun performPageTransition(direction: EdgePagingDirection) {
    if (!isDragging || isDropping || isTransitioningPage || pagerState.isScrollInProgress) return

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
          edgeDwellJob?.cancel()
          edgeDwellJob = null
          updatePreviewTargetSlot()
          // Allow continuous edge dwell to trigger next page transition after a 550ms cooldown
          edgeDwellJob = coroutineScope.launch {
            delay(550L)
            edgeTriggerState = EdgeTriggerState.IDLE
            activeEdgeZone = EdgePagingDirection.NONE
            if (isDragging && !isDropping) {
              handleDragMoveRef?.invoke(currentPointerPos)
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
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_START from=$fromPage to=$targetPage direction=RIGHT")
          pagerState.animateScrollToPage(targetPage, animationSpec = tween(PAGE_TRANSITION_DURATION_MS))
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          AppLogger.i(AppLogger.Category.LAUNCHER, "PAGE_TRANSITION_COMPLETE page=$targetPage")
        } finally {
          isTransitioningPage = false
          edgeDwellJob?.cancel()
          edgeDwellJob = null
          updatePreviewTargetSlot()
          // Allow continuous edge dwell to trigger next page transition after a 550ms cooldown
          edgeDwellJob = coroutineScope.launch {
            delay(550L)
            edgeTriggerState = EdgeTriggerState.IDLE
            activeEdgeZone = EdgePagingDirection.NONE
            if (isDragging && !isDropping) {
              handleDragMoveRef?.invoke(currentPointerPos)
            }
          }
        }
      }
    }
  }

  fun handleStartDrag(placement: SpaceItemPlacement, startOffset: Offset) {
    dragLifecycleState = DragLifecycleState.DRAGGING
    isDragging = true
    isDropping = false
    draggedPlacement = placement
    currentPointerPos = startOffset
    isOverBin = false
    targetHoverPlacement = null
    activeEdgeZone = EdgePagingDirection.NONE
    edgeTriggerState = EdgeTriggerState.IDLE
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    previewTargetSlot = null

    // Preserve finger-to-icon offset to eliminate visual jumping when drag starts
    val itemRect = getPlacementFootprintRect(placement) ?: cellBounds[placement.id] ?: slotBounds[placement.positionIndex]
    val computedTouchOffset = if (itemRect != null) {
      Offset(
        (startOffset.x - itemRect.left).coerceIn(0f, itemRect.width),
        (startOffset.y - itemRect.top).coerceIn(0f, itemRect.height)
      )
    } else {
      val sX = if (placement.isWidget) placement.spanX.coerceIn(1, cols) else 1
      val sY = if (placement.isWidget) placement.spanY.coerceIn(1, gridRows) else 1
      with(density) { Offset((cellWidth * sX).toPx() / 2f, (cellHeight * sY).toPx() / 2f) }
    }
    touchOffsetWithinItem = computedTouchOffset

    if (unifiedDragState != null) {
      val app = appLookup[placement]
      unifiedDragState.lifecycleState = DragLifecycleState.DRAGGING
      unifiedDragState.isDragging = true
      unifiedDragState.dragSource = DragSource.LAYER1_DESKTOP
      unifiedDragState.draggedPlacement = placement
      unifiedDragState.draggedApp = app
      unifiedDragState.touchOffsetInItem = computedTouchOffset
      unifiedDragState.rootPointerPos = rootCoordinates?.localToRoot(startOffset) ?: startOffset
      unifiedDragState.currentTargetZone = DragTargetZone.DESKTOP
    }

    AppLogger.i(
      AppLogger.Category.LAUNCHER,
      "DRAG_START item=${placement.id} pkg=${placement.packageName ?: "folder"} page=${placement.pageIndex} slot=${placement.positionIndex} touchOffset=$computedTouchOffset"
    )
    currentPointerPos = startOffset
    updatePreviewTargetSlot(startOffset)
  }

  fun handleDragMove(newPos: Offset) {
    currentPointerPos = newPos
    val rootPos = rootCoordinates?.localToRoot(newPos) ?: newPos
    if (unifiedDragState != null) {
      unifiedDragState.rootPointerPos = rootPos
    }

    val overBin = binBounds?.contains(newPos) == true
    isOverBin = overBin
    if (unifiedDragState != null) {
      unifiedDragState.isOverBin = overBin
    }

    if (overBin) {
      if (unifiedDragState != null) {
        unifiedDragState.currentTargetZone = DragTargetZone.REMOVE_BIN
      }
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

    // Check if dragging over the DockBar (only valid for apps, not widgets)
    val isAppPlacement = draggedPlacement?.isWidget != true
    val isOverDock = isAppPlacement && (unifiedDragState?.isPointerOverDock(rootPos) == true)
    if (isOverDock) {
      unifiedDragState?.currentTargetZone = DragTargetZone.DOCK_BAR
      targetHoverPlacement = null
      previewTargetSlot = null
      if (activeEdgeZone != EdgePagingDirection.NONE) {
        edgeDwellJob?.cancel()
        edgeDwellJob = null
        activeEdgeZone = EdgePagingDirection.NONE
        edgeTriggerState = EdgeTriggerState.IDLE
      }
      return
    }

    if (unifiedDragState != null) {
      unifiedDragState.currentTargetZone = DragTargetZone.DESKTOP
    }

    updatePreviewTargetSlot(newPos)

    // Robust edge paging state machine (horizontal pager mode only)
    if (space.layer1DisplayMode != Space.DISPLAY_MODE_SCROLL && viewportWidth > 0f && !isTransitioningPage && !pagerState.isScrollInProgress) {
      val inLeftEdge = newPos.x <= edgeZonePx
      val inRightEdge = newPos.x >= (viewportWidth - edgeZonePx)

      when {
        inLeftEdge -> {
          if (activeEdgeZone != EdgePagingDirection.LEFT) {
            // Newly entered left edge zone
            activeEdgeZone = EdgePagingDirection.LEFT
            edgeDwellJob?.cancel()
            AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_ENTER direction=LEFT page=${pagerState.currentPage} x=${newPos.x}")
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
            AppLogger.i(AppLogger.Category.LAUNCHER, "EDGE_ENTER direction=RIGHT page=${pagerState.currentPage} x=${newPos.x}")
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
  handleDragMoveRef = { handleDragMove(it) }

  fun cleanupDragState() {
    isDropping = false
    isDragging = false
    draggedPlacement = null
    pendingDragPlacement = null
    dragLifecycleState = DragLifecycleState.IDLE
    previewTargetSlot = null
    targetHoverPlacement = null
    isOverBin = false
    extraPagesCount = 0
    accumulatedDragDistance = 0f
    unifiedDragState?.reset()
  }

  fun handleEndDrag(draggedOverride: SpaceItemPlacement? = null, dropPosOverride: Offset? = null) {
    if (isDropping) return
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    isTransitioningPage = false
    activeEdgeZone = EdgePagingDirection.NONE
    edgeTriggerState = EdgeTriggerState.IDLE

    val dragged = draggedOverride ?: draggedPlacement
    if (dragged == null) {
      cleanupDragState()
      return
    }

    val dropPos = dropPosOverride ?: currentPointerPos
    val targetZone = unifiedDragState?.currentTargetZone ?: if (isOverBin) DragTargetZone.REMOVE_BIN else DragTargetZone.DESKTOP
    when (targetZone) {
      DragTargetZone.REMOVE_BIN -> {
        AppLogger.i(AppLogger.Category.LAUNCHER, "DROP_REMOVE item=${dragged.id} pkg=${dragged.packageName}")
        onRemovePlacement(dragged.id)
        cleanupDragState()
      }
      DragTargetZone.DOCK_BAR -> {
        if (!dragged.isWidget) {
          val app = appLookup[dragged]
          if (app != null) {
            AppLogger.i(AppLogger.Category.LAUNCHER, "DROP_TO_DOCK item=${dragged.id} pkg=${dragged.packageName} slot=${unifiedDragState?.targetDockIndex}")
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onDropItemToDock?.invoke(dragged, app, unifiedDragState?.targetDockIndex ?: -1)
          }
        }
        cleanupDragState()
      }
      DragTargetZone.DESKTOP, DragTargetZone.NONE -> {
        val targetPage = pagerState.currentPage
        val draggedSpanX = if (dragged.isWidget) dragged.spanX.coerceIn(1, cols) else 1
        val draggedSpanY = if (dragged.isWidget) dragged.spanY.coerceIn(1, gridRows) else 1

        val cellWPx = with(density) { cellWidth.toPx() }
        val cellHPx = with(density) { cellHeight.toPx() }
        val touchOffset = touchOffsetWithinItem
        val targetPointerPos = if (dragged.isWidget) {
          val widgetTopLeft = dropPos - touchOffset
          Offset(widgetTopLeft.x + cellWPx / 2f, widgetTopLeft.y + cellHPx / 2f)
        } else {
          dropPos
        }

        val slotAtDrop = calculateSlotForPosition(targetPointerPos, draggedSpanX, draggedSpanY)
        val rawTargetPos = previewTargetSlot ?: slotAtDrop
        val rawC = rawTargetPos % cols
        val rawR = rawTargetPos / cols
        val clampedC = rawC.coerceIn(0, maxOf(0, cols - draggedSpanX))
        val clampedR = rawR.coerceIn(0, maxOf(0, gridRows - draggedSpanY))
        val targetPos = clampedR * cols + clampedC

        val isDraggedApp = !dragged.isWidget && !dragged.isFolder
        val isTargetOverWidget = if (isDraggedApp) {
          effectivePlacements.any { other ->
            if (other.isWidget && (space.layer1DisplayMode == Space.DISPLAY_MODE_SCROLL || other.pageIndex == targetPage)) {
              val r = (other.positionIndex / cols).coerceIn(0, gridRows - 1)
              val c = (other.positionIndex % cols).coerceIn(0, cols - 1)
              val sX = other.spanX.coerceIn(1, cols - c)
              val sY = other.spanY.coerceIn(1, gridRows - r)
              val targetR = targetPos / cols
              val targetC = targetPos % cols
              targetC in c until (c + sX) && targetR in r until (r + sY)
            } else false
          }
        } else false

        if (isDraggedApp && isTargetOverWidget) {
          AppLogger.w(
            AppLogger.Category.LAUNCHER,
            "REJECT_DROP: App ${dragged.packageName} cannot be dropped onto widget at slot $targetPos on page $targetPage"
          )
          cleanupDragState()
          return
        }

        AppLogger.i(
          AppLogger.Category.LAUNCHER,
          "FINAL_DROP: dropPos=$dropPos previewTargetSlot=$previewTargetSlot slotAtDrop=$slotAtDrop targetPage=$targetPage targetPos=$targetPos gridRows=$gridRows pageSize=$pageSize dragged.pageIndex=${dragged.pageIndex} dragged.positionIndex=${dragged.positionIndex}"
        )
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

        if (isDraggedApp && targetHoverPlacement != null) {
          val targetPlacement = targetHoverPlacement!!.takeIf {
            it.id != dragged.id && (space.layer1DisplayMode == Space.DISPLAY_MODE_SCROLL || it.pageIndex == targetPage)
          }

          if (targetPlacement != null) {
            if (!targetPlacement.isWidget && !targetPlacement.isFolder) {
              // Dropped directly on another app -> initiate folder creation containing both apps!
              val sourceApp = appLookup[dragged]
              val targetApp = appLookup[targetPlacement]

              if (sourceApp != null && targetApp != null) {
                AppLogger.i(
                  AppLogger.Category.LAUNCHER,
                  "DROP_CREATE_FOLDER: source=${sourceApp.label} (${dragged.id}) target=${targetApp.label} (${targetPlacement.id}) page=$targetPage pos=${targetPlacement.positionIndex}"
                )
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCreateFolderFromApps(
                  sourceApp,
                  targetApp,
                  dragged.id,
                  targetPlacement.id,
                  targetPlacement.pageIndex,
                  targetPlacement.positionIndex
                )
                cleanupDragState()
                return
              }
            } else if (targetPlacement.isFolder && targetPlacement.folderId != null) {
              // Dropped directly onto an existing folder -> add to folder!
              val sourceApp = appLookup[dragged]
              if (sourceApp != null) {
                AppLogger.i(
                  AppLogger.Category.LAUNCHER,
                  "DROP_ADD_TO_FOLDER: source=${sourceApp.label} folderId=${targetPlacement.folderId}"
                )
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onAddAppToExistingFolder(targetPlacement.folderId, sourceApp, dragged.id)
                cleanupDragState()
                return
              }
            }
          }
        }

        // Animate smooth drop glide to destination slot
        val targetRect = slotBounds[targetPos]
        if (targetRect != null && draggedOverride == null) {
          isDropping = true
          val targetDest = Offset(targetRect.left + touchOffset.x, targetRect.top + touchOffset.y)
          val startPos = currentPointerPos
          coroutineScope.launch {
            try {
              val anim = Animatable(0f)
              anim.animateTo(1f, animationSpec = tween(120, easing = FastOutSlowInEasing)) {
                currentPointerPos = Offset(
                  startPos.x + (targetDest.x - startPos.x) * value,
                  startPos.y + (targetDest.y - startPos.y) * value
                )
              }
              onMovePlacement(dragged.id, targetPage, targetPos, pageSize)
            } finally {
              cleanupDragState()
            }
          }
        } else {
          onMovePlacement(dragged.id, targetPage, targetPos, pageSize)
          cleanupDragState()
        }
      }
    }
  }

  fun handleCancelDrag() {
    edgeDwellJob?.cancel()
    edgeDwellJob = null
    isTransitioningPage = false
    activeEdgeZone = EdgePagingDirection.NONE
    edgeTriggerState = EdgeTriggerState.IDLE
    cleanupDragState()
    AppLogger.i(AppLogger.Category.LAUNCHER, "DRAG_CANCEL")
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .onGloballyPositioned { coordinates ->
        rootCoordinates = coordinates
        viewportWidth = coordinates.size.width.toFloat()
        viewportHeight = coordinates.size.height.toFloat()
        unifiedDragState?.layer1Coordinates = coordinates
        updatePageGridBounds()
      }
      .layerTransitionEmptySpaceSwipe(
        spaceId = space.id,
        isSwipeAllowed = isSwipeAllowed,
        canTransition = { !isDragging && !isDropping && dragLifecycleState == DragLifecycleState.IDLE },
        findItemAtOffset = { offset -> findItemAtOffset(offset, pagerState.currentPage) },
        onSwipeStart = onEmptySpaceSwipeStart,
        onSwipeMove = onEmptySpaceSwipeMove,
        onSwipeEnd = onEmptySpaceSwipeEnd,
        onSwipeCancel = onEmptySpaceSwipeCancel
      )
      .desktopTapAndEmptyLongPressGesture(
        spaceId = space.id,
        enabled = true,
        onTap = { offset ->
          if (isDropping) return@desktopTapAndEmptyLongPressGesture
          if (activeActionPlacement != null) {
            dismissActions()
          } else {
            val hitItem = findItemAtOffset(offset, pagerState.currentPage)
            if (hitItem != null) {
              if (hitItem.isFolder) {
                val folder = currentFolderLookup[hitItem.folderId]
                if (folder != null) currentOnOpenFolder(folder)
              } else if (!hitItem.isWidget) {
                val app = currentAppLookup[hitItem]
                if (app != null) currentOnLaunchApp(app)
              }
            } else {
              if (resizingWidgetId != null) {
                resizingWidgetId = null
              }
            }
          }
        },
        onEmptyLongPress = { offset ->
          if (isDropping) return@desktopTapAndEmptyLongPressGesture
          val hitItem = findItemAtOffset(offset, pagerState.currentPage)
          if (hitItem == null) {
            if (resizingWidgetId != null) {
              resizingWidgetId = null
            } else {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              currentOnOpenCustomization(pagerState.currentPage)
            }
          }
        }
      )
      .appDragGestures(
        spaceId = space.id,
        enabled = true,
        onDragStart = { startOffset ->
          if (isDropping) return@appDragGestures
          val hitItem = findItemAtOffset(startOffset, pagerState.currentPage)
          if (hitItem != null) {
            resizingWidgetId = null
            if (hitItem.isFolder) {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              handleStartDrag(hitItem, startOffset)
            } else {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              lastLongPressTimestamp = System.currentTimeMillis()
              dragLifecycleState = DragLifecycleState.PRESSED_ACTION_VISIBLE
              activeActionPlacement = hitItem
              pendingDragPlacement = hitItem
              accumulatedDragDistance = 0f
            }
          }
        },
        onDrag = { change, dragAmount ->
          if (isDropping) return@appDragGestures
          change.consume()
          accumulatedDragDistance += dragAmount.getDistance()
          if (dragLifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE && pendingDragPlacement != null) {
            if (accumulatedDragDistance >= dragSlopPx) {
              val itemToDrag = pendingDragPlacement!!
              activeActionPlacement = null
              pendingDragPlacement = null
              handleStartDrag(itemToDrag, change.position)
            }
          } else if (isDragging) {
            handleDragMove(change.position)
          }
        },
        onDragEnd = {
          if (isDropping) return@appDragGestures
          if (isDragging) {
            handleEndDrag()
          } else if (dragLifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE) {
            pendingDragPlacement = null
          } else {
            handleCancelDrag()
          }
        },
        onDragCancel = {
          if (isDropping) return@appDragGestures
          if (isDragging) {
            handleCancelDrag()
          } else if (dragLifecycleState == DragLifecycleState.PRESSED_ACTION_VISIBLE) {
            pendingDragPlacement = null
          } else {
            handleCancelDrag()
          }
        }
      )
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
          items(
            items = effectivePlacements,
            key = { it.id },
            span = { placement ->
              if (placement.isWidget) {
                GridItemSpan(placement.spanX.coerceIn(1, space.gridColumns))
              } else {
                GridItemSpan(1)
              }
            }
          ) { placement ->
            val isResizing = (resizingWidgetId == placement.id)
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
              onRemovePlacement = onRemovePlacement,
              appWidgetHost = appWidgetHost,
              onPositioned = { rect -> cellBounds[placement.id] = rect },
              isBeingDragged = isDragging && draggedPlacement?.id == placement.id,
              isTargetHover = targetHoverPlacement?.id == placement.id,
              rootCoordinates = rootCoordinates,
              isResizeMode = isResizing,
              onLongClick = null,
              onResizeChange = { newSpanX, newSpanY ->
                val clampedSpanX = newSpanX.coerceIn(1, space.gridColumns)
                val clampedSpanY = newSpanY.coerceIn(1, 6)
                onResizeWidget(placement.id, clampedSpanX, clampedSpanY, null)
              },
              onFinishResize = {
                resizingWidgetId = null
              },
              maxSpanX = space.gridColumns,
              maxSpanY = 6
            )
          }
        }
      } else {
        // Horizontal paged layout (Default)
        HorizontalPager(
          state = pagerState,
          userScrollEnabled = HorizontalPageGesturePolicy.isScrollEnabled(
            interactionState = launcherInteractionState,
            isDragging = isDragging,
            isDropping = isDropping,
            dragLifecycleState = dragLifecycleState
          ),
          modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .testTag("layer1_horizontal_pager")
            .onGloballyPositioned { pagerCoords ->
              pagerCoordinates = pagerCoords
              updatePageGridBounds()
            }
        ) { page ->
          val isCurrentPage = page == pagerState.currentPage

          val rawPagePlacements = effectivePlacements.filter { it.pageIndex == page }
          val otherPlacements = if (isDragging && draggedPlacement != null) {
            val dragged = draggedPlacement!!
            rawPagePlacements.filter { p ->
              p.id != dragged.id && (dragged.packageName == null || p.packageName != dragged.packageName)
            }
          } else {
            rawPagePlacements
          }

          val previewSlotsMap = remember(otherPlacements) {
            otherPlacements.associateBy { it.positionIndex }
          }

          // Visual page turn presentation container (pure visual transform layer)
          Box(
            modifier = Modifier
              .fillMaxSize()
              .pageTurnEffect(
                pagerState = pagerState,
                page = page,
                effect = space.pageTurnEffect,
                intensity = space.pageTurnIntensity
              )
          ) {
            BoxWithConstraints(
              modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = gridHorizontalPadding, vertical = gridVerticalPadding)
            ) {
              val availableWidth = maxWidth
              val cellWidth = (availableWidth - appSpacing * (cols - 1)) / cols
              val rowPitch = cellHeight + appSpacing

              // Slots covered by placed widgets or items
              val coveredSlots = remember(otherPlacements, cols, gridRows) {
                val set = mutableSetOf<Int>()
                for (item in otherPlacements) {
                  val r = (item.positionIndex / cols).coerceIn(0, (gridRows - 1).coerceAtLeast(0))
                  val c = (item.positionIndex % cols).coerceIn(0, (cols - 1).coerceAtLeast(0))
                  val sX = if (item.isWidget) item.spanX.coerceIn(1, cols - c) else 1
                  val sY = if (item.isWidget) item.spanY.coerceIn(1, gridRows - r) else 1
                  for (dr in 0 until sY) {
                    for (dc in 0 until sX) {
                      set.add((r + dr) * cols + (c + dc))
                    }
                  }
                }
                set
              }

              val draggedSpanX = if (draggedPlacement?.isWidget == true) draggedPlacement!!.spanX.coerceIn(1, cols) else 1
              val draggedSpanY = if (draggedPlacement?.isWidget == true) draggedPlacement!!.spanY.coerceIn(1, gridRows) else 1

              // 1. Slot grid placeholders (handles empty clicks, slotBounds measurement, and drop landing)
              for (r in 0 until gridRows) {
                for (c in 0 until cols) {
                  val slotIndex = r * cols + c
                  val leftDp = (cellWidth + appSpacing) * c
                  val topDp = rowPitch * r
                  val isPreviewTarget = if (isDragging && isCurrentPage && !isOverBin && previewTargetSlot != null) {
                    val targetSlot = previewTargetSlot!!
                    val tR = targetSlot / cols
                    val tC = targetSlot % cols
                    r in tR until (tR + draggedSpanY) && c in tC until (tC + draggedSpanX)
                  } else false
                  val isCovered = coveredSlots.contains(slotIndex)

                  Box(
                    modifier = Modifier
                      .offset(x = leftDp, y = topDp)
                      .size(width = cellWidth, height = cellHeight),
                    contentAlignment = Alignment.Center
                  ) {
                    if (isPreviewTarget && !isCovered) {
                      DropTargetLandingSlot(iconSizeModifier = iconSizeModifier)
                    } else if (!isCovered) {
                      EmptyGridCell(
                        slotIndex = slotIndex,
                        isDragging = isDragging && !isOverBin
                      )
                    }
                  }
                }
              }

              // 2. Placed items: apps, folders, and multi-span widgets
              for (item in otherPlacements) {
                val r = (item.positionIndex / cols).coerceIn(0, (gridRows - 1).coerceAtLeast(0))
                val c = (item.positionIndex % cols).coerceIn(0, (cols - 1).coerceAtLeast(0))
                val sX = if (item.isWidget) item.spanX.coerceIn(1, cols - c) else 1
                val sY = if (item.isWidget) item.spanY.coerceIn(1, gridRows - r) else 1
                val leftDp = (cellWidth + appSpacing) * c
                val topDp = rowPitch * r
                val widthDp = cellWidth * sX + appSpacing * (sX - 1)
                val heightDp = cellHeight * sY + appSpacing * (sY - 1)
                val isResizing = (resizingWidgetId == item.id)

                Box(
                  modifier = Modifier
                    .offset(x = leftDp, y = topDp)
                    .size(width = widthDp, height = heightDp)
                    .zIndex(if (isResizing) 50f else 1f),
                  contentAlignment = Alignment.Center
                ) {
                  val isPreviewTarget = if (isDragging && isCurrentPage && !isOverBin && previewTargetSlot != null) {
                    val targetSlot = previewTargetSlot!!
                    val tR = targetSlot / cols
                    val tC = targetSlot % cols
                    val overlapX = maxOf(tC, c) < minOf(tC + draggedSpanX, c + sX)
                    val overlapY = maxOf(tR, r) < minOf(tR + draggedSpanY, r + sY)
                    overlapX && overlapY
                  } else false
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
                    onRemovePlacement = onRemovePlacement,
                    appWidgetHost = appWidgetHost,
                    onPositioned = { rect -> cellBounds[item.id] = rect },
                    isBeingDragged = isDragging && (draggedPlacement?.id == item.id || (draggedPlacement?.packageName != null && item.packageName == draggedPlacement?.packageName)),
                    isTargetHover = isPreviewTarget || (targetHoverPlacement?.id == item.id),
                    rootCoordinates = rootCoordinates,
                    isResizeMode = isResizing,
                    onLongClick = null,
                    onResizeChange = { newSpanX, newSpanY ->
                      val clampedSpanX = newSpanX.coerceIn(1, cols)
                      val clampedSpanY = newSpanY.coerceIn(1, gridRows)
                      val curR = item.positionIndex / cols
                      val curC = item.positionIndex % cols
                      val newC = if (curC + clampedSpanX > cols) (cols - clampedSpanX).coerceAtLeast(0) else curC
                      val newR = if (curR + clampedSpanY > gridRows) (gridRows - clampedSpanY).coerceAtLeast(0) else curR
                      val newPos = newR * cols + newC
                      onResizeWidget(item.id, clampedSpanX, clampedSpanY, newPos)
                    },
                    onFinishResize = {
                      resizingWidgetId = null
                    },
                    maxSpanX = cols,
                    maxSpanY = gridRows
                  )
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
              coroutineScope.launch {
                pagerState.animateScrollToPage(page, animationSpec = tween(space.pageTurnDurationMs))
              }
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
    if (isDragging && space.layer1DisplayMode != Space.DISPLAY_MODE_SCROLL && !isOverBin && !isDropping) {
      if (activeEdgeZone == EdgePagingDirection.LEFT && edgeTriggerState == EdgeTriggerState.ARMED) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .width(36.dp)
            .align(Alignment.CenterStart)
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  QuantumViolet.copy(alpha = 0.4f),
                  Color.Transparent
                )
              )
            )
            .zIndex(100f),
          contentAlignment = Alignment.CenterStart
        ) {
          Box(
            modifier = Modifier
              .padding(start = 6.dp)
              .size(30.dp)
              .clip(CircleShape)
              .background(QuantumViolet.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Previous Page",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      } else if (activeEdgeZone == EdgePagingDirection.RIGHT && edgeTriggerState == EdgeTriggerState.ARMED) {
        Box(
          modifier = Modifier
            .fillMaxHeight()
            .width(36.dp)
            .align(Alignment.CenterEnd)
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  Color.Transparent,
                  QuantumViolet.copy(alpha = 0.4f)
                )
              )
            )
            .zIndex(100f),
          contentAlignment = Alignment.CenterEnd
        ) {
          Box(
            modifier = Modifier
              .padding(end = 6.dp)
              .size(30.dp)
              .clip(CircleShape)
              .background(QuantumViolet.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "Next Page",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }

    // Floating dragged item follow overlay (rendered in root coordinate space)
    if ((isDragging || isDropping) && draggedPlacement != null) {
      val dragged = draggedPlacement!!
      val app = appLookup[dragged]
      val bitmap = remember(app?.id) { app?.let { getBitmap(it) } }

      var lastPointerX by remember { mutableFloatStateOf(currentPointerPos.x) }
      var targetTilt by remember { mutableFloatStateOf(0f) }
      LaunchedEffect(currentPointerPos.x) {
        val dx = currentPointerPos.x - lastPointerX
        lastPointerX = currentPointerPos.x
        targetTilt = (dx * 0.16f).coerceIn(-6f, 6f)
      }

      val targetScale = when {
        isDropping -> 1.0f
        isOverBin -> 0.82f
        targetHoverPlacement != null -> 1.16f
        else -> 1.12f
      }
      val dragScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dragScale"
      )
      val dragTilt by animateFloatAsState(
        targetValue = if (isDropping) 0f else targetTilt,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dragTilt"
      )
      val dragElevation by animateDpAsState(
        targetValue = if (isDropping) 2.dp else if (isOverBin) 8.dp else 22.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "dragElevation"
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
            rotationZ = dragTilt
            shadowElevation = dragElevation.toPx()
            alpha = if (isOverBin) 0.75f else 1.0f
          }
          .zIndex(999f)
          .testTag("floating_dragged_item"),
        contentAlignment = Alignment.Center
      ) {
        if (dragged.isWidget) {
          val sX = dragged.spanX.coerceIn(1, cols)
          val sY = dragged.spanY.coerceIn(1, gridRows)
          val widthDp = cellWidth * sX + appSpacing * (sX - 1)
          val heightDp = cellHeight * sY + appSpacing * (sY - 1)
          Box(
            modifier = Modifier
              .size(width = widthDp, height = heightDp)
              .clip(ShapeRoundLg)
              .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
            contentAlignment = Alignment.Center
          ) {
            DesktopWidgetView(
              placement = dragged,
              space = space,
              onRemove = null,
              appWidgetHost = appWidgetHost,
              isResizeMode = false
            )
          }
        } else {
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
              } else {
                ThemedAppIcon(
                  app = app,
                  bitmap = bitmap,
                  appTheme = space.appTheme,
                  modifier = Modifier.fillMaxSize(),
                  fallbackText = app?.label?.take(1) ?: dragged.packageName?.take(1)?.uppercase()
                )
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

    // Tap-away dismiss scrim for pre-drag action overlay (only shown when finger has been released without dragging)
    if (activeActionPlacement != null && pendingDragPlacement == null && !isDragging) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(activeActionPlacement) {
            detectTapGestures(
              onPress = {
                dismissActions()
              }
            )
          }
          .zIndex(800f)
      )
    }

    // Pre-drag floating action box overlay (rendered above the app/widget when action is active and not dragging)
    if (activeActionPlacement != null && !isDragging) {
      val targetPlacement = activeActionPlacement!!
      val targetRect = getPlacementFootprintRect(targetPlacement)
        ?: cellBounds[targetPlacement.id]
        ?: slotBounds[targetPlacement.positionIndex]
      val app = appLookup[targetPlacement]

      PreDragActionBoxOverlay(
        placement = targetPlacement,
        app = app,
        targetRect = targetRect,
        viewportWidth = viewportWidth,
        density = density,
        onOpenAppInfo = { appToOpen ->
          try {
            onOpenAppInfo(appToOpen)
          } catch (e: Exception) {
            AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to open App Info for ${appToOpen.packageName}", e)
          }
          dismissActions()
        },
        onUninstallApp = { appToUninstall ->
          try {
            onUninstallApp(appToUninstall)
          } catch (e: Exception) {
            com.multispace.platform.PackageActionHelper.launchUninstallConfirmation(context, appToUninstall.packageName)
          }
          dismissActions()
        },
        onForceStopApp = { appToForceStop ->
          try {
            onForceStopApp(appToForceStop)
          } catch (e: Exception) {
            com.multispace.platform.PackageActionHelper.forceStopPackage(context, appToForceStop.packageName)
          }
          dismissActions()
        },
        onActivateResize = { widgetId ->
          resizingWidgetId = widgetId
          dismissActions()
        },
        onDismiss = {
          dismissActions()
        },
        modifier = Modifier.zIndex(850f)
      )
    }
  }
}

@Composable
private fun PreDragActionBoxOverlay(
  placement: SpaceItemPlacement,
  app: DiscoveredApp?,
  targetRect: Rect?,
  viewportWidth: Float,
  density: androidx.compose.ui.unit.Density,
  onOpenAppInfo: (DiscoveredApp) -> Unit,
  onUninstallApp: (DiscoveredApp) -> Unit,
  onForceStopApp: (DiscoveredApp) -> Unit,
  onActivateResize: (String) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val pkgName = placement.packageName ?: ""
  val isUninstallable = remember(app, pkgName) {
    com.multispace.platform.PackageActionHelper.isPackageUninstallable(
      context = context,
      packageName = pkgName,
      fallbackUninstallable = app?.isUninstallable ?: true
    )
  }

  val boxWidthDp = if (placement.isWidget) 44.dp else 92.dp
  val boxHeightDp = 44.dp
  val boxWidthPx = with(density) { boxWidthDp.toPx() }
  val boxHeightPx = with(density) { boxHeightDp.toPx() }
  val gapPx = with(density) { 8.dp.toPx() }
  val minMarginPx = with(density) { 16.dp.toPx() }

  val targetCenterX = targetRect?.center?.x ?: (if (viewportWidth > 0f) viewportWidth / 2f else minMarginPx + boxWidthPx)
  val targetTop = targetRect?.top ?: (minMarginPx + boxHeightPx + gapPx)
  val targetBottom = targetRect?.bottom ?: targetTop

  val idealTopY = targetTop - gapPx - boxHeightPx
  val topY = if (idealTopY >= minMarginPx) idealTopY else (targetBottom + gapPx)
  val leftX = (targetCenterX - (boxWidthPx / 2f)).coerceIn(
    minMarginPx,
    (viewportWidth - boxWidthPx - minMarginPx).coerceAtLeast(minMarginPx)
  )

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.95f),
    tonalElevation = 6.dp,
    shadowElevation = 8.dp,
    border = BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    ),
    modifier = modifier
      .offset { IntOffset(leftX.roundToInt(), topY.roundToInt()) }
      .size(boxWidthDp, boxHeightDp)
      .testTag(if (placement.isWidget) "widget_action_box" else "app_action_box")
  ) {
    if (placement.isWidget) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        IconButton(
          onClick = {
            onActivateResize(placement.id)
            onDismiss()
          },
          modifier = Modifier
            .fillMaxSize()
            .testTag("btn_widget_resize_action")
        ) {
          Icon(
            imageVector = Icons.Default.OpenInFull,
            contentDescription = "Resize Widget",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    } else {
      val targetApp = app ?: DiscoveredApp(
        id = placement.id,
        packageName = pkgName,
        activityName = placement.componentName ?: "",
        label = pkgName
      )

      Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        IconButton(
          onClick = {
            onOpenAppInfo(targetApp)
            onDismiss()
          },
          modifier = Modifier
            .size(40.dp)
            .testTag("btn_app_info_action")
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "App Info",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
        }

        Box(
          modifier = Modifier
            .width(1.dp)
            .height(20.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        )

        if (isUninstallable) {
          IconButton(
            onClick = {
              onUninstallApp(targetApp)
              onDismiss()
            },
            modifier = Modifier
              .size(40.dp)
              .testTag("btn_app_uninstall_action")
          ) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = "Uninstall App",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp)
            )
          }
        } else {
          IconButton(
            onClick = {
              onForceStopApp(targetApp)
              onDismiss()
            },
            modifier = Modifier
              .size(40.dp)
              .testTag("btn_app_force_stop_action")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Force Stop",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DropTargetLandingSlot(
  iconSizeModifier: Modifier,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "slotGlow")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.16f,
    targetValue = 0.32f,
    animationSpec = infiniteRepeatable(
      animation = tween(750, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(750, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.graphicsLayer {
        scaleX = pulseScale
        scaleY = pulseScale
      }
    ) {
      Box(
        modifier = iconSizeModifier
          .clip(ShapeRoundMd)
          .background(QuantumViolet.copy(alpha = pulseAlpha))
          .border(
            BorderStroke(1.75.dp, QuantumViolet.copy(alpha = 0.8f)),
            ShapeRoundMd
          )
      )
      Spacer(modifier = Modifier.height(AppDimens.Spacing4))
      Box(
        modifier = Modifier
          .width(36.dp)
          .height(6.dp)
          .clip(CircleShape)
          .background(QuantumViolet.copy(alpha = 0.35f))
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
  appLookup: AppIdentityLookup,
  folderLookup: Map<String, SpaceFolder>,
  allApps: List<DiscoveredApp>,
  iconSizeModifier: Modifier,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit = {},
  onOpenFolder: (SpaceFolder) -> Unit = {},
  onRemovePlacement: (String) -> Unit = {},
  appWidgetHost: AppWidgetHost? = null,
  onPositioned: (Rect) -> Unit,
  isBeingDragged: Boolean,
  isTargetHover: Boolean,
  rootCoordinates: LayoutCoordinates?,
  isResizeMode: Boolean = false,
  onLongClick: (() -> Unit)? = null,
  onResizeChange: ((newSpanX: Int, newSpanY: Int) -> Unit)? = null,
  onFinishResize: (() -> Unit)? = null,
  maxSpanX: Int = 4,
  maxSpanY: Int = 5,
  onItemTouched: ((SpaceItemPlacement, Offset) -> Unit)? = null,
  isActionActive: () -> Boolean = { false },
  getDragLifecycleState: () -> DragLifecycleState = { DragLifecycleState.IDLE },
  getLastLongPressTimestamp: () -> Long = { 0L }
) {
  val app = appLookup[placement]
  val folder = if (placement.isFolder) folderLookup[placement.folderId] else null
  var cellCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

  val hoverScale by animateFloatAsState(
    targetValue = if (isTargetHover) 1.12f else 1.0f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
    label = "cellHoverScale"
  )
  val cellAlpha by animateFloatAsState(
    targetValue = if (isBeingDragged) 0.0f else 1.0f,
    animationSpec = tween(120),
    label = "cellAlpha"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .fillMaxSize()
      .onGloballyPositioned { coordinates ->
        cellCoordinates = coordinates
        val root = rootCoordinates
        if (root != null && coordinates.isAttached && root.isAttached) {
          val localOffset = root.localPositionOf(coordinates, Offset.Zero)
          onPositioned(Rect(localOffset, coordinates.size.toSize()))
        }
      }
      .graphicsLayer {
        alpha = cellAlpha
        scaleX = hoverScale
        scaleY = hoverScale
      }
      .testTag(if (placement.isFolder) "layer1_folder_${placement.folderId}" else if (placement.isWidget) "layer1_widget_${placement.id}" else "layer1_app_${placement.packageName}")
  ) {
    if (placement.isWidget) {
      DesktopWidgetView(
        placement = placement,
        space = space,
        onRemove = { onRemovePlacement(placement.id) },
        appWidgetHost = appWidgetHost,
        isResizeMode = isResizeMode,
        onLongClick = onLongClick,
        onResizeChange = onResizeChange,
        onFinishResize = onFinishResize,
        maxSpanX = maxSpanX,
        maxSpanY = maxSpanY,
        modifier = Modifier.fillMaxSize()
      )
    } else if (placement.isFolder) {
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
                MiniAppIcon(item = item, appLookup = appLookup, allApps = allApps, appTheme = space.appTheme, getBitmap = getBitmap)
              }
            }
            if (previewItems.size > 2) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
              ) {
                previewItems.drop(2).take(2).forEach { item ->
                  MiniAppIcon(item = item, appLookup = appLookup, allApps = allApps, appTheme = space.appTheme, getBitmap = getBitmap)
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
      // App Item - themed according to space.appTheme
      val bitmap = remember(app?.id) { app?.let { getBitmap(it) } }
      Box(contentAlignment = Alignment.Center) {
        ThemedAppIcon(
          app = app,
          bitmap = bitmap,
          appTheme = space.appTheme,
          modifier = iconSizeModifier,
          fallbackText = app?.label?.take(1) ?: placement.packageName?.take(1)?.uppercase()
        )
        if (isTargetHover && !placement.isWidget && !placement.isFolder) {
          Box(
            modifier = iconSizeModifier
              .clip(ShapeRoundMd)
              .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
              .border(2.dp, MaterialTheme.colorScheme.primary, ShapeRoundMd)
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
  appLookup: AppIdentityLookup,
  allApps: List<DiscoveredApp>,
  appTheme: String,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?
) {
  val app = appLookup[item]
  val bitmap = remember(app?.id) { app?.let { getBitmap(it) } }

  ThemedMiniAppIcon(
    app = app,
    bitmap = bitmap,
    appTheme = appTheme,
    modifier = Modifier.size(16.dp),
    fallbackText = app?.label?.take(1) ?: item.packageName.take(1).uppercase()
  )
}
