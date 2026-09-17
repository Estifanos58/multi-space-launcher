package com.multispace.presentation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.ImportReport
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceFolderItem

class DesktopInteractionController(
  private val spaceViewModel: SpaceViewModel,
  private val discoveryViewModel: AppDiscoveryViewModel
) {
  var showSpaceSwitcherMenu by mutableStateOf(false)
  var spaceToUnlockForSwitch by mutableStateOf<Space?>(null)
  var showUnlockForActiveSpace by mutableStateOf(false)
  var showDesktopCustomizationSheet by mutableStateOf(false)
  var activeDesktopPage by mutableIntStateOf(0)
  var showImportDialog by mutableStateOf(false)
  var importReport by mutableStateOf<ImportReport?>(null)
  var isImporting by mutableStateOf(false)
  var activeFolderInDialog by mutableStateOf<SpaceFolder?>(null)

  fun openFolder(folder: SpaceFolder, resolvedFolders: List<SpaceFolder>) {
    activeFolderInDialog = resolvedFolders.firstOrNull { it.id == folder.id } ?: folder
  }

  fun closeFolder() {
    activeFolderInDialog = null
  }

  fun dismissAllModals() {
    showSpaceSwitcherMenu = false
    spaceToUnlockForSwitch = null
    showUnlockForActiveSpace = false
    showDesktopCustomizationSheet = false
    showImportDialog = false
    activeFolderInDialog = null
  }

  fun renameFolder(folder: SpaceFolder, newName: String) {
    if (!folder.isMostUsedFolder) {
      spaceViewModel.renameFolder(folder.id, newName)
      activeFolderInDialog = folder.copy(name = newName)
    }
  }

  fun removeAppFromFolder(folder: SpaceFolder, item: SpaceFolderItem) {
    if (!folder.isMostUsedFolder) {
      spaceViewModel.removeAppFromFolder(folder.id, item.id)
      activeFolderInDialog = folder.copy(items = folder.items.filter { it.id != item.id })
    }
  }

  fun deleteFolder(folder: SpaceFolder) {
    if (!folder.isMostUsedFolder) {
      spaceViewModel.deleteFolder(folder.id)
    }
    activeFolderInDialog = null
  }

  fun removePlacement(placementId: String) {
    spaceViewModel.removePlacement(placementId)
  }

  fun createFolderFromApps(
    spaceId: String,
    targetPage: Int,
    targetPos: Int,
    sourceApp: DiscoveredApp,
    targetApp: DiscoveredApp,
    sourcePlacementId: String?,
    targetPlacementId: String?
  ) {
    spaceViewModel.createFolderFromApps(
      spaceId = spaceId,
      pageIndex = targetPage,
      positionIndex = targetPos,
      folderName = "Folder",
      sourceApp = sourceApp,
      targetApp = targetApp,
      sourcePlacementId = sourcePlacementId,
      targetPlacementId = targetPlacementId
    )
  }

  fun addAppToExistingFolder(folderId: String, app: DiscoveredApp, sourcePlacementId: String) {
    spaceViewModel.addAppToFolder(folderId, app, sourcePlacementId)
  }

  fun addAppToHome(spaceId: String, app: DiscoveredApp, page: Int = 0) {
    spaceViewModel.addAppToHome(spaceId, app, page)
  }

  fun movePlacement(spaceId: String, placementId: String, targetPage: Int, targetPos: Int, pageSize: Int) {
    spaceViewModel.moveAppToPage(spaceId, placementId, targetPage, targetPos, pageSize)
  }

  fun resizeWidget(placementId: String, spanX: Int, spanY: Int, pos: Int?) {
    spaceViewModel.updateWidgetSpan(placementId, spanX, spanY, pos)
  }

  fun openCustomization(page: Int) {
    activeDesktopPage = page
    showDesktopCustomizationSheet = true
  }

  fun closeCustomization() {
    showDesktopCustomizationSheet = false
  }

  fun updateWallpaperColor(spaceId: String, color: Long) {
    spaceViewModel.updateSpaceWallpaper(spaceId, Space.BACKGROUND_COLOR, color, null)
  }

  fun updateWallpaperPreset(spaceId: String, presetUri: String) {
    spaceViewModel.updateSpaceWallpaper(spaceId, Space.BACKGROUND_IMAGE, null, presetUri)
  }

  fun updateWallpaperUri(spaceId: String, uri: Uri) {
    spaceViewModel.updateSpaceWallpaper(spaceId, Space.BACKGROUND_IMAGE, null, uri.toString())
  }

  fun updateSpaceCustomization(
    spaceId: String,
    bgType: String,
    bgColor: Long?,
    bgUri: String?,
    cols: Int,
    size: String,
    showLabels: Boolean
  ) {
    spaceViewModel.updateSpaceCustomization(
      spaceId = spaceId,
      backgroundType = bgType,
      backgroundColor = bgColor,
      backgroundImageUri = bgUri,
      gridColumns = cols,
      iconSize = size,
      labelVisibility = showLabels
    )
  }

  fun reorderApp(spaceId: String, app: DiscoveredApp, direction: Int) {
    spaceViewModel.reorderSpaceApp(spaceId, app, direction)
  }

  fun sortAppsAlphabetically(spaceId: String, apps: List<DiscoveredApp>) {
    spaceViewModel.sortSpaceAppsAlphabetically(spaceId, apps)
  }

  fun addWidgetToHome(
    spaceId: String,
    pageIndex: Int,
    widgetType: String,
    spanX: Int,
    spanY: Int,
    appWidgetId: Int,
    packageName: String?,
    componentName: String?
  ) {
    showDesktopCustomizationSheet = false
    spaceViewModel.addWidgetToHome(
      spaceId = spaceId,
      pageIndex = pageIndex,
      widgetType = widgetType,
      spanX = spanX,
      spanY = spanY,
      appWidgetId = appWidgetId,
      packageName = packageName,
      componentName = componentName
    )
  }

  fun updateSpaceTheme(
    spaceId: String,
    appTheme: String,
    cols: Int,
    iconSize: String,
    showLabels: Boolean
  ) {
    spaceViewModel.updateSpaceTheme(
      spaceId = spaceId,
      appTheme = appTheme,
      gridColumns = cols,
      iconSize = iconSize,
      labelVisibility = showLabels
    )
  }

  fun addPage(spaceId: String) {
    spaceViewModel.addPage(spaceId)
  }

  fun deletePage(spaceId: String, pageIndex: Int) {
    spaceViewModel.deletePage(spaceId, pageIndex)
  }

  fun startImport(space: Space, allApps: List<DiscoveredApp>) {
    isImporting = true
    spaceViewModel.importCurrentHomeLayout(space.id, allApps) { report ->
      importReport = report
      isImporting = false
    }
  }

  fun dismissImportDialog() {
    showImportDialog = false
    importReport = null
    isImporting = false
  }

  fun openAppInfo(app: DiscoveredApp) = discoveryViewModel.openAppInfo(app)
  fun uninstallApp(app: DiscoveredApp) = discoveryViewModel.uninstallApp(app)
  fun forceStopApp(app: DiscoveredApp) = discoveryViewModel.forceStopApp(app)
}

@Composable
fun rememberDesktopInteractionController(
  spaceViewModel: SpaceViewModel,
  discoveryViewModel: AppDiscoveryViewModel
): DesktopInteractionController {
  return remember(spaceViewModel, discoveryViewModel) {
    DesktopInteractionController(spaceViewModel, discoveryViewModel)
  }
}
