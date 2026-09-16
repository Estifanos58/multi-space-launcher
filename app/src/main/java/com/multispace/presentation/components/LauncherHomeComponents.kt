package com.multispace.presentation.components

import android.graphics.Bitmap
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.multispace.R
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.WallpaperCatalog
import com.multispace.presentation.DesktopCustomizationSheet
import com.multispace.presentation.DesktopInteractionController
import com.multispace.presentation.DragSource
import com.multispace.presentation.FolderDialog
import com.multispace.presentation.ImportLayoutDialog
import com.multispace.presentation.LauncherUiState
import com.multispace.presentation.LauncherWallpaperStyle
import com.multispace.presentation.SpaceUnlockDialog
import com.multispace.presentation.SpaceViewModel
import com.multispace.presentation.ThemedAppIcon
import com.multispace.presentation.UnifiedDragState
import com.multispace.ui.components.ModernEmptyState
import com.multispace.ui.components.ModernLoadingState
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.CrimsonNova
import com.multispace.ui.theme.ShapeRoundMd
import kotlin.math.roundToInt

@Composable
fun LauncherWallpaperLayer(
  wallpaperStyle: LauncherWallpaperStyle,
  modifier: Modifier = Modifier
) {
  when (wallpaperStyle.bgType) {
    Space.BACKGROUND_COLOR -> {
      if (wallpaperStyle.bgColor != null) {
        Box(
          modifier = modifier
            .fillMaxSize()
            .background(Color(wallpaperStyle.bgColor.toInt()))
        )
      } else {
        Image(
          painter = painterResource(id = R.drawable.img_wallpaper_aurora),
          contentDescription = "Space Wallpaper",
          contentScale = ContentScale.Crop,
          modifier = modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.20f))
        )
      }
    }
    Space.BACKGROUND_IMAGE -> {
      val presetRes = WallpaperCatalog.resolveDrawableRes(wallpaperStyle.bgImageUri)
      if (presetRes != null) {
        Image(
          painter = painterResource(id = presetRes),
          contentDescription = "Space Wallpaper",
          contentScale = if (wallpaperStyle.scaleMode == "crop") ContentScale.Crop else ContentScale.Fit,
          modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleX = wallpaperStyle.zoomLevel
              scaleY = wallpaperStyle.zoomLevel
              translationX = wallpaperStyle.offsetX
              translationY = wallpaperStyle.offsetY
            }
        )
        if (wallpaperStyle.dimLevel > 0f) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = wallpaperStyle.dimLevel))
          )
        }
      } else if (!wallpaperStyle.bgImageUri.isNullOrEmpty()) {
        val ctx = LocalContext.current
        AsyncImage(
          model = ImageRequest.Builder(ctx)
            .data(wallpaperStyle.bgImageUri)
            .crossfade(true)
            .build(),
          contentDescription = "Space Wallpaper",
          contentScale = if (wallpaperStyle.scaleMode == "crop") ContentScale.Crop else ContentScale.Fit,
          modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleX = wallpaperStyle.zoomLevel
              scaleY = wallpaperStyle.zoomLevel
              translationX = wallpaperStyle.offsetX
              translationY = wallpaperStyle.offsetY
            }
        )
        if (wallpaperStyle.dimLevel > 0f) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(Color.Black.copy(alpha = wallpaperStyle.dimLevel))
          )
        }
      } else {
        Image(
          painter = painterResource(id = R.drawable.img_wallpaper_aurora),
          contentDescription = "Space Wallpaper",
          contentScale = ContentScale.Crop,
          modifier = modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.20f))
        )
      }
    }
    else -> {
      Image(
        painter = painterResource(id = R.drawable.img_wallpaper_aurora),
        contentDescription = "Space Wallpaper",
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize()
      )
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.20f))
      )
    }
  }
}

@Composable
fun LauncherLockedState(
  spaceName: String,
  wallpaperStyle: LauncherWallpaperStyle,
  onUnlockClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(AppDimens.Spacing32),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Surface(
      shape = CircleShape,
      color = wallpaperStyle.pillSurfaceColor,
      border = BorderStroke(AppDimens.BorderMedium, CrimsonNova.copy(alpha = 0.5f)),
      modifier = Modifier.size(76.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = null,
          tint = CrimsonNova,
          modifier = Modifier.size(AppDimens.IconHero)
        )
      }
    }
    Spacer(modifier = Modifier.height(AppDimens.Spacing16))
    Text(
      text = "$spaceName is Protected",
      style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      color = wallpaperStyle.headerContentColor
    )
    Spacer(modifier = Modifier.height(AppDimens.Spacing8))
    Text(
      text = "Enter your credential to access applications in this isolated workspace.",
      style = MaterialTheme.typography.bodyMedium,
      color = if (wallpaperStyle.isDarkTheme) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(AppDimens.Spacing24))
    Button(
      onClick = onUnlockClick,
      shape = ShapeRoundMd,
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      modifier = Modifier.height(AppDimens.ButtonHeight).testTag("btn_unlock_active_space")
    ) {
      Icon(
        imageVector = Icons.Default.Key,
        contentDescription = null,
        modifier = Modifier.size(AppDimens.IconSm)
      )
      Spacer(modifier = Modifier.width(AppDimens.Spacing8))
      Text(
        "Enter PIN / Credential",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
      )
    }
  }
}

@Composable
fun LauncherEmptySpaceState(
  spaceName: String,
  onConfigureClick: () -> Unit,
  onImportClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(AppDimens.Spacing32),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    ModernEmptyState(
      icon = Icons.Default.Apps,
      title = "No apps in $spaceName",
      description = "Assign apps to this workspace or import your existing home layout to get started."
    )
    Spacer(modifier = Modifier.height(AppDimens.Spacing16))
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)) {
      Button(
        onClick = onConfigureClick,
        shape = ShapeRoundMd,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier.height(AppDimens.ButtonHeightSm).testTag("btn_empty_space_configure")
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = null,
          modifier = Modifier.size(AppDimens.IconSm)
        )
        Spacer(modifier = Modifier.width(AppDimens.Spacing6))
        Text("Add Apps", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
      }

      OutlinedButton(
        onClick = onImportClick,
        shape = ShapeRoundMd,
        modifier = Modifier.height(AppDimens.ButtonHeightSm).testTag("btn_empty_space_import")
      ) {
        Icon(
          imageVector = Icons.Default.FileDownload,
          contentDescription = null,
          modifier = Modifier.size(AppDimens.IconSm)
        )
        Spacer(modifier = Modifier.width(AppDimens.Spacing6))
        Text("Import Layout", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
      }
    }
  }
}

@Composable
fun LauncherFloatingDragOverlay(
  unifiedDragState: UnifiedDragState,
  appTheme: String,
  getBitmap: (DiscoveredApp) -> Bitmap?
) {
  if (unifiedDragState.isDragging && unifiedDragState.dragSource == DragSource.DOCK_BAR && unifiedDragState.draggedApp != null) {
    val app = unifiedDragState.draggedApp!!
    val bitmap = getBitmap(app)
    val dragScale by animateFloatAsState(
      targetValue = if (unifiedDragState.isOverBin) 0.85f else 1.10f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
      label = "unifiedDragScale"
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .zIndex(9999f)
    ) {
      Box(
        modifier = Modifier
          .offset {
            val rootPos = unifiedDragState.rootPointerPos
            val touchOffset = unifiedDragState.touchOffsetInItem
            IntOffset(
              x = (rootPos.x - touchOffset.x).roundToInt(),
              y = (rootPos.y - touchOffset.y).roundToInt()
            )
          }
          .scale(dragScale)
          .shadow(16.dp, CircleShape)
          .size(56.dp)
          .background(
            if (unifiedDragState.isOverBin) CrimsonNova.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        ThemedAppIcon(
          app = app,
          bitmap = bitmap,
          appTheme = appTheme,
          modifier = Modifier.size(46.dp)
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherDialogsHost(
  desktopController: DesktopInteractionController,
  uiState: LauncherUiState,
  spaceViewModel: SpaceViewModel,
  getBitmap: (DiscoveredApp) -> Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit
) {
  // Active space unlock dialog
  if (desktopController.showUnlockForActiveSpace && uiState.activeSpace != null) {
    SpaceUnlockDialog(
      space = uiState.activeSpace,
      onDismiss = { desktopController.showUnlockForActiveSpace = false },
      onUnlockSuccess = {
        desktopController.showUnlockForActiveSpace = false
      },
      spaceViewModel = spaceViewModel
    )
  }

  // Target space unlock dialog for switching
  desktopController.spaceToUnlockForSwitch?.let { space ->
    SpaceUnlockDialog(
      space = space,
      onDismiss = { desktopController.spaceToUnlockForSwitch = null },
      onUnlockSuccess = {
        spaceViewModel.selectActiveSpace(space.id)
        desktopController.spaceToUnlockForSwitch = null
      },
      spaceViewModel = spaceViewModel
    )
  }

  // Folder Dialog
  if (desktopController.activeFolderInDialog != null) {
    val folderId = desktopController.activeFolderInDialog!!.id
    val folder = if (desktopController.activeFolderInDialog!!.isMostUsedFolder) {
      uiState.resolvedActiveFolders.firstOrNull { it.id == folderId } ?: desktopController.activeFolderInDialog!!
    } else {
      desktopController.activeFolderInDialog!!
    }
    FolderDialog(
      folder = folder,
      allApps = uiState.spaceScopedApps,
      getBitmap = getBitmap,
      onLaunchApp = onLaunchApp,
      onRenameFolder = { newName -> desktopController.renameFolder(folder, newName) },
      onRemoveItem = { item -> desktopController.removeAppFromFolder(folder, item) },
      onDeleteFolder = { desktopController.deleteFolder(folder) },
      onDismiss = { desktopController.closeFolder() }
    )
  }

  // Import Layout Dialog
  if (desktopController.showImportDialog && uiState.activeSpace != null) {
    ImportLayoutDialog(
      report = desktopController.importReport,
      isImporting = desktopController.isImporting,
      onStartImport = {
        desktopController.startImport(uiState.activeSpace, uiState.allApps)
      },
      onDismiss = { desktopController.dismissImportDialog() }
    )
  }

  // Desktop Customization Sheet (Triggered on long-pressing empty desktop)
  if (desktopController.showDesktopCustomizationSheet && uiState.activeSpace != null) {
    val space = uiState.activeSpace
    DesktopCustomizationSheet(
      space = space,
      placements = uiState.activePlacements,
      currentPage = desktopController.activeDesktopPage,
      totalPageCount = maxOf(space.pageCount, (uiState.activePlacements.maxOfOrNull { it.pageIndex } ?: 0) + 1),
      spaceApps = uiState.spaceScopedApps,
      onDismiss = { desktopController.closeCustomization() },
      onSelectWallpaperColor = { color -> desktopController.updateWallpaperColor(space.id, color) },
      onSelectWallpaperPreset = { presetUri -> desktopController.updateWallpaperPreset(space.id, presetUri) },
      onSelectWallpaperUri = { uri -> desktopController.updateWallpaperUri(space.id, uri) },
      onOpenWallpaperEditor = {},
      onUpdateSpaceCustomization = { bgType, bgColor, bgUri, cols, size, showLabels ->
        desktopController.updateSpaceCustomization(space.id, bgType, bgColor, bgUri, cols, size, showLabels)
      },
      onReorderApp = { app, direction -> desktopController.reorderApp(space.id, app, direction) },
      onSortAlphabetically = { desktopController.sortAppsAlphabetically(space.id, uiState.spaceScopedApps) },
      onAddWidget = { pageIndex, widgetType, spanX, spanY, appWidgetId, pkg, comp ->
        desktopController.addWidgetToHome(space.id, pageIndex, widgetType, spanX, spanY, appWidgetId, pkg, comp)
      },
      onUpdateTheme = { appTheme, cols, iconSize, showLabels ->
        desktopController.updateSpaceTheme(space.id, appTheme, cols, iconSize, showLabels)
      },
      onAddPage = { desktopController.addPage(space.id) },
      onDeletePage = { pageIndex -> desktopController.deletePage(space.id, pageIndex) },
      onScrollToPage = {}
    )
  }
}
