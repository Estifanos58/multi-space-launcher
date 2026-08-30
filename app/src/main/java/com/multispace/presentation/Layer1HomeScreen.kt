package com.multispace.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.*
import kotlinx.coroutines.launch

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
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  val appLookup = remember(allApps) {
    allApps.associateBy { "${it.packageName}/${it.activityName}" }
  }
  val folderLookup = remember(folders) {
    folders.associateBy { it.id }
  }

  // Active dragging state
  var draggedPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var targetHoverPlacement by remember { mutableStateOf<SpaceItemPlacement?>(null) }
  var isDragging by remember { mutableStateOf(false) }

  val maxPageInPlacements = placements.maxOfOrNull { it.pageIndex } ?: 0
  val pageCount = (maxPageInPlacements + 1).coerceAtLeast(1)

  val pagerState = rememberPagerState(initialPage = 0, pageCount = { pageCount })

  val iconSizeModifier = when (space.iconSize) {
    Space.ICON_SIZE_SMALL -> Modifier.size(44.dp)
    Space.ICON_SIZE_LARGE -> Modifier.size(62.dp)
    else -> Modifier.size(54.dp)
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Removal Bucket when dragging
      RemovalBucketBar(
        isVisible = isDragging && draggedPlacement != null,
        onRemoveClicked = {
          draggedPlacement?.let { onRemovePlacement(it.id) }
          draggedPlacement = null
          isDragging = false
        },
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(top = 8.dp)
      )

      // Main content: either Paged or Scrolling
      if (space.layer1DisplayMode == Space.DISPLAY_MODE_SCROLL) {
        // Vertical continuous scrolling layout
        LazyVerticalGrid(
          columns = GridCells.Fixed(space.gridColumns),
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("layer1_scroll_grid"),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
          items(placements, key = { it.id }) { placement ->
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
              onStartDrag = {
                draggedPlacement = placement
                isDragging = true
              },
              onEndDrag = { isDragging = false },
              isHovered = targetHoverPlacement?.id == placement.id
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
          val pagePlacements = placements.filter { it.pageIndex == page }.sortedBy { it.positionIndex }

          LazyVerticalGrid(
            columns = GridCells.Fixed(space.gridColumns),
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                onStartDrag = {
                  draggedPlacement = placement
                  isDragging = true
                },
                onEndDrag = { isDragging = false },
                isHovered = targetHoverPlacement?.id == placement.id
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
              .padding(vertical = 4.dp)
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
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
  onStartDrag: () -> Unit,
  onEndDrag: () -> Unit,
  isHovered: Boolean
) {
  var showItemMenu by remember { mutableStateOf(false) }

  if (placement.isFolder) {
    val folder = folderLookup[placement.folderId]
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .combinedClickable(
          onClick = { if (folder != null) onOpenFolder(folder) },
          onLongClick = { showItemMenu = true }
        )
        .padding(4.dp)
        .testTag("layer1_folder_${placement.folderId}")
    ) {
      // Folder Preview Icon (2x2 mini grid)
      Box(
        modifier = iconSizeModifier
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
          .padding(4.dp),
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
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
        }
      }

      if (space.labelVisibility) {
        Spacer(modifier = Modifier.height(4.dp))
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
    }
  } else {
    // App Item
    val key = "${placement.packageName}/${placement.componentName}"
    val app = appLookup[key] ?: allApps.firstOrNull { it.packageName == placement.packageName }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .combinedClickable(
          onClick = { if (app != null) onLaunchApp(app) },
          onLongClick = {
            onStartDrag()
            showItemMenu = true
          }
        )
        .padding(4.dp)
        .testTag("layer1_app_${placement.packageName}")
    ) {
      Box(
        modifier = iconSizeModifier
          .clip(RoundedCornerShape(14.dp))
          .background(MaterialTheme.colorScheme.surfaceVariant),
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
            color = MaterialTheme.colorScheme.primary,
            fontSize = 18.sp
          )
        }
      }

      if (space.labelVisibility) {
        Spacer(modifier = Modifier.height(4.dp))
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
        .clip(RoundedCornerShape(4.dp))
    )
  } else {
    Box(
      modifier = Modifier
        .size(16.dp)
        .clip(RoundedCornerShape(4.dp))
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
