package com.multispace.presentation

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.multispace.domain.model.*
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.QuantumViolet
import com.multispace.ui.theme.ShapeRoundLg
import com.multispace.ui.theme.ShapeRoundMd
import com.multispace.ui.theme.ShapeRoundSm
import com.multispace.ui.theme.ShapeRoundXs
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

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

  val cellBounds = remember { mutableStateMapOf<String, Rect>() }

  val maxPageInPlacements = effectivePlacements.maxOfOrNull { it.pageIndex } ?: 0
  val pageCount = (maxPageInPlacements + 1).coerceAtLeast(1)

  val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

  val iconSizeModifier = when (space.iconSize) {
    Space.ICON_SIZE_SMALL -> Modifier.size(44.dp)
    Space.ICON_SIZE_LARGE -> Modifier.size(62.dp)
    else -> Modifier.size(52.dp)
  }

  fun handleStartDrag(placement: SpaceItemPlacement, startOffset: Offset) {
    draggedPlacement = placement
    isDragging = true
    val cell = cellBounds[placement.id]
    currentPointerPos = if (cell != null) cell.topLeft + startOffset else startOffset
    isOverBin = false
    targetHoverPlacement = null
  }

  fun handleDragDelta(delta: Offset) {
    currentPointerPos += delta
    val overBin = binBounds?.contains(currentPointerPos) == true
    isOverBin = overBin
    targetHoverPlacement = if (!overBin) {
      cellBounds.entries.firstOrNull { (id, rect) ->
        id != draggedPlacement?.id && rect.contains(currentPointerPos)
      }?.key?.let { id -> effectivePlacements.firstOrNull { it.id == id } }
    } else {
      null
    }
  }

  fun handleEndDrag() {
    try {
      val dragged = draggedPlacement
      if (dragged != null) {
        if (isOverBin) {
          onRemovePlacement(dragged.id)
        } else if (targetHoverPlacement != null) {
          val target = targetHoverPlacement!!
          if (!target.isFolder && !dragged.isFolder) {
            val srcApp = appLookup["${dragged.packageName}/${dragged.componentName}"]
              ?: allApps.firstOrNull { it.packageName == dragged.packageName }
            val tgtApp = appLookup["${target.packageName}/${target.componentName}"]
              ?: allApps.firstOrNull { it.packageName == target.packageName }
            if (srcApp != null && tgtApp != null) {
              onCreateFolderFromApps(srcApp, tgtApp, dragged.id, target.id)
            }
          } else {
            onMovePlacement(dragged.id, target.pageIndex, target.positionIndex)
          }
        }
      }
    } finally {
      isDragging = false
      draggedPlacement = null
      targetHoverPlacement = null
      isOverBin = false
    }
  }

  fun handleCancelDrag() {
    isDragging = false
    draggedPlacement = null
    targetHoverPlacement = null
    isOverBin = false
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Removal Bucket when dragging
      RemovalBucketBar(
        isVisible = isDragging && draggedPlacement != null,
        isHovered = isOverBin,
        onPositioned = { binBounds = it },
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(top = AppDimens.Spacing8)
      )

      // Main content: either Paged or Scrolling
      if (space.layer1DisplayMode == Space.DISPLAY_MODE_SCROLL) {
        // Vertical continuous scrolling layout
        LazyVerticalGrid(
          columns = GridCells.Fixed(space.gridColumns),
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing8)
            .testTag("layer1_scroll_grid"),
          horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
          verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
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
              onStartDrag = { offset -> handleStartDrag(placement, offset) },
              onDrag = { delta -> handleDragDelta(delta) },
              onEndDrag = { handleEndDrag() },
              onCancelDrag = { handleCancelDrag() },
              isBeingDragged = isDragging && draggedPlacement?.id == placement.id,
              isTargetHover = targetHoverPlacement?.id == placement.id
            )
          }
        }
      } else {
        // Horizontal paged layout (Default)
        HorizontalPager(
          state = pagerState,
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .testTag("layer1_horizontal_pager")
        ) { page ->
          val pagePlacements = effectivePlacements.filter { it.pageIndex == page }.sortedBy { it.positionIndex }

          LazyVerticalGrid(
            columns = GridCells.Fixed(space.gridColumns),
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing8),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
          ) {
            items(pagePlacements, key = { it.id }) { placement ->
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
                onStartDrag = { offset -> handleStartDrag(placement, offset) },
                onDrag = { delta -> handleDragDelta(delta) },
                onEndDrag = { handleEndDrag() },
                onCancelDrag = { handleCancelDrag() },
                isBeingDragged = isDragging && draggedPlacement?.id == placement.id,
                isTargetHover = targetHoverPlacement?.id == placement.id
              )
            }
          }
        }

        // Page Indicator Dots
        if (pageCount > 1) {
          PageIndicatorDots(
            pageCount = pageCount,
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

    // Floating dragged item follow overlay
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
          .size(60.dp)
          .shadow(AppDimens.ElevationHigh, ShapeRoundMd)
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
        alpha = if (isBeingDragged) 0.3f else 1.0f
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
