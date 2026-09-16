package com.multispace.presentation

import android.graphics.Bitmap
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.multispace.domain.model.*

@Immutable
data class LauncherWallpaperStyle(
  val bgType: String = Space.BACKGROUND_DEFAULT,
  val bgColor: Long? = null,
  val bgImageUri: String? = null,
  val scaleMode: String = "crop",
  val zoomLevel: Float = 1.0f,
  val dimLevel: Float = 0.20f,
  val offsetX: Float = 0.0f,
  val offsetY: Float = 0.0f,
  val isDarkTheme: Boolean = false,
  val headerContentColor: Color = Color.White,
  val pillSurfaceColor: Color = Color(0xCC090B10),
  val pillBorderColor: Color = Color(0x33A78BFA)
)

@Immutable
data class LauncherUiState(
  val activeSpace: Space? = null,
  val currentSpace: Space = Space.createDefault(),
  val activeMemberships: List<SpaceMembership> = emptyList(),
  val allSpaces: List<Space> = emptyList(),
  val unlockedSpaceIds: Set<String> = emptySet(),
  val activeLayerIndex: Int = 1,
  val activePlacements: List<SpaceItemPlacement> = emptyList(),
  val activeFolders: List<SpaceFolder> = emptyList(),
  val resolvedActiveFolders: List<SpaceFolder> = emptyList(),
  val activeDockItems: List<SpaceDockItem> = emptyList(),
  val allApps: List<DiscoveredApp> = emptyList(),
  val spaceScopedApps: List<DiscoveredApp> = emptyList(),
  val spaceScopedRecentApps: List<DiscoveredApp> = emptyList(),
  val spaceScopedMostUsedApps: List<DiscoveredApp> = emptyList(),
  val spaceUsageStats: SpaceUsageStats? = null,
  val layer2CachedCatalog: Layer2CachedCatalog = Layer2CachedCatalog(),
  val isCurrentSpaceUnlocked: Boolean = true,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val wallpaperStyle: LauncherWallpaperStyle = LauncherWallpaperStyle()
)

@Composable
fun rememberLauncherUiState(
  discoveryViewModel: AppDiscoveryViewModel,
  spaceViewModel: SpaceViewModel
): LauncherUiState {
  val discoveryUiState by discoveryViewModel.uiState.collectAsStateWithLifecycle()
  val activeSpace by spaceViewModel.activeSpace.collectAsStateWithLifecycle()
  val activeMemberships by spaceViewModel.activeMemberships.collectAsStateWithLifecycle()
  val allSpaces by spaceViewModel.allSpaces.collectAsStateWithLifecycle()
  val unlockedSpaceIds by spaceViewModel.unlockedSpaceIds.collectAsStateWithLifecycle()

  val activeLayerIndex by spaceViewModel.activeLayerIndex.collectAsStateWithLifecycle()
  val activePlacements by spaceViewModel.activePlacements.collectAsStateWithLifecycle()
  val activeFolders by spaceViewModel.activeFolders.collectAsStateWithLifecycle()
  val activeDockItems by spaceViewModel.activeDockItems.collectAsStateWithLifecycle()

  val isCurrentSpaceUnlocked = remember(activeSpace, unlockedSpaceIds) {
    spaceViewModel.isSpaceUnlocked(activeSpace)
  }

  // Automatic first-install configuration: ensure Default Space is configured as current Home page
  LaunchedEffect(discoveryUiState.allApps.isNotEmpty(), activeSpace?.id, activePlacements.isEmpty(), activeDockItems.isEmpty()) {
    if (discoveryUiState.allApps.isNotEmpty() &&
      (activeSpace?.id == Space.DEFAULT_SPACE_ID || activeSpace == null) &&
      activePlacements.isEmpty() &&
      activeDockItems.isEmpty()
    ) {
      val targetSpaceId = activeSpace?.id ?: Space.DEFAULT_SPACE_ID
      spaceViewModel.importCurrentHomeLayout(targetSpaceId, discoveryUiState.allApps)
    }
  }

  // Determine dynamic background styling and contrast
  val currentBgType = activeSpace?.homeWallpaperType ?: activeSpace?.backgroundType ?: Space.BACKGROUND_DEFAULT
  val currentBgColor = activeSpace?.homeWallpaperColor ?: activeSpace?.backgroundColor
  val currentBgImageUri = activeSpace?.homeWallpaperImageUri ?: activeSpace?.backgroundImageUri
  val currentScaleMode = activeSpace?.homeWallpaperScaleMode ?: "crop"
  val currentZoomLevel = activeSpace?.homeWallpaperZoomLevel ?: 1.0f
  val currentDimLevel = activeSpace?.homeWallpaperDimLevel ?: 0.20f
  val currentOffsetX = activeSpace?.homeWallpaperOffsetX ?: 0.0f
  val currentOffsetY = activeSpace?.homeWallpaperOffsetY ?: 0.0f

  val isDarkThemeBackground = remember(currentBgType, currentBgColor, currentBgImageUri) {
    when (currentBgType) {
      Space.BACKGROUND_COLOR -> {
        if (currentBgColor != null) {
          Color(currentBgColor.toInt()).luminance() < 0.45f
        } else {
          false
        }
      }
      Space.BACKGROUND_IMAGE -> !currentBgImageUri.isNullOrEmpty()
      else -> false
    }
  }

  val headerContentColor = if (isDarkThemeBackground) Color.White else MaterialTheme.colorScheme.onSurface
  val pillSurfaceColor = if (isDarkThemeBackground) {
    Color(0xCC090B10)
  } else {
    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
  }
  val pillBorderColor = if (isDarkThemeBackground) {
    Color(0x33A78BFA)
  } else {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
  }

  val wallpaperStyle = remember(
    currentBgType, currentBgColor, currentBgImageUri, currentScaleMode,
    currentZoomLevel, currentDimLevel, currentOffsetX, currentOffsetY,
    isDarkThemeBackground, headerContentColor, pillSurfaceColor, pillBorderColor
  ) {
    LauncherWallpaperStyle(
      bgType = currentBgType,
      bgColor = currentBgColor,
      bgImageUri = currentBgImageUri,
      scaleMode = currentScaleMode,
      zoomLevel = currentZoomLevel,
      dimLevel = currentDimLevel,
      offsetX = currentOffsetX,
      offsetY = currentOffsetY,
      isDarkTheme = isDarkThemeBackground,
      headerContentColor = headerContentColor,
      pillSurfaceColor = pillSurfaceColor,
      pillBorderColor = pillBorderColor
    )
  }

  // Resolve Space presentation: Project active Space's persisted memberships against current Android LauncherApps catalog
  val spaceScopedApps = remember(discoveryUiState.allApps, activeMemberships, isCurrentSpaceUnlocked, activeSpace) {
    if (!isCurrentSpaceUnlocked || discoveryUiState.allApps.isEmpty()) {
      emptyList()
    } else if (activeMemberships.isEmpty() && (activeSpace?.id == Space.DEFAULT_SPACE_ID || activeSpace == null)) {
      discoveryUiState.allApps
    } else if (activeMemberships.isEmpty()) {
      emptyList()
    } else {
      val lookup = AppIdentityLookup(discoveryUiState.allApps)
      val result = mutableListOf<DiscoveredApp>()
      val includedIdentities = mutableSetOf<AppIdentity>()

      for (membership in activeMemberships) {
        val matchedApp = lookup[membership.appIdentity]
        if (matchedApp != null && includedIdentities.add(matchedApp.appIdentity)) {
          result.add(matchedApp)
        }
      }
      result
    }
  }

  val currentSpaceId = activeSpace?.id ?: Space.DEFAULT_SPACE_ID
  val currentSpace = activeSpace ?: Space.createDefault()

  val spaceMostUsedApps by remember(currentSpaceId) {
    discoveryViewModel.getMostUsedAppsFlow(currentSpaceId)
  }.collectAsStateWithLifecycle(initialValue = emptyList())

  val spaceRecentApps by remember(currentSpaceId) {
    discoveryViewModel.getRecentAppsFlow(currentSpaceId)
  }.collectAsStateWithLifecycle(initialValue = emptyList())

  val spaceUsageStats by remember(currentSpaceId) {
    discoveryViewModel.getSpaceUsageStatsFlow(currentSpaceId)
  }.collectAsStateWithLifecycle(initialValue = null)

  val spaceScopedMostUsedAppsFull = remember(spaceMostUsedApps, spaceScopedApps) {
    val spaceLookup = AppIdentityLookup(spaceScopedApps)
    val seen = mutableSetOf<AppIdentity>()
    val resolved = mutableListOf<DiscoveredApp>()
    for (app in spaceMostUsedApps) {
      val inSpace = spaceLookup[app.appIdentity]
      if (inSpace != null && seen.add(inSpace.appIdentity)) {
        resolved.add(inSpace)
      }
    }
    resolved
  }

  val spaceScopedRecentApps = remember(spaceRecentApps, spaceScopedApps, currentSpace.gridColumns) {
    val spaceLookup = AppIdentityLookup(spaceScopedApps)
    val seen = mutableSetOf<AppIdentity>()
    val resolved = mutableListOf<DiscoveredApp>()
    for (app in spaceRecentApps) {
      val inSpace = spaceLookup[app.appIdentity]
      if (inSpace != null && seen.add(inSpace.appIdentity)) {
        resolved.add(inSpace)
        if (resolved.size >= currentSpace.gridColumns) break
      }
    }
    resolved
  }

  val spaceScopedMostUsedApps = remember(spaceScopedMostUsedAppsFull, currentSpace.gridColumns) {
    spaceScopedMostUsedAppsFull.take(currentSpace.gridColumns)
  }

  val resolvedActiveFolders = remember(activeFolders, spaceScopedMostUsedAppsFull, currentSpaceId) {
    activeFolders.map { folder ->
      if (folder.isMostUsedFolder) {
        val dynamicItems = spaceScopedMostUsedAppsFull.mapIndexed { index, app ->
          com.multispace.domain.model.SpaceFolderItem(
            id = "most_used_${currentSpaceId}_${app.packageName}_${app.userHandleId}",
            folderId = folder.id,
            packageName = app.packageName,
            componentName = app.activityName ?: "${app.packageName}.MainActivity",
            userHandleId = app.userHandleId,
            orderIndex = index
          )
        }
        folder.copy(name = SpaceFolder.MOST_USED_FOLDER_NAME, items = dynamicItems)
      } else {
        folder
      }
    }
  }

  val layer2CachedCatalog = remember(spaceScopedApps) {
    val sorted = spaceScopedApps.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    val grouped = linkedMapOf<Char, MutableList<DiscoveredApp>>()
    val letterToFirst = mutableMapOf<Char, Int>()

    sorted.forEachIndexed { index, app ->
      val cleanLabel = app.label.trim().trim('"', '\'', '(', '[', '{')
      val firstChar = cleanLabel.firstOrNull()?.uppercaseChar() ?: '#'
      val groupKey = if (firstChar in 'A'..'Z') firstChar else '#'
      grouped.getOrPut(groupKey) { mutableListOf() }.add(app)
      if (firstChar in 'A'..'Z' && !letterToFirst.containsKey(firstChar)) {
        letterToFirst[firstChar] = index
      }
    }

    val letterToSection = mutableMapOf<Char, Int>()
    grouped.keys.forEachIndexed { index, char ->
      letterToSection[char] = index
    }

    Layer2CachedCatalog(
      sortedApps = sorted,
      groupedApps = grouped,
      letterToSectionIndex = letterToSection,
      letterToFirstIndex = letterToFirst,
      activeLetters = letterToFirst.keys
    )
  }

  return LauncherUiState(
    activeSpace = activeSpace,
    currentSpace = currentSpace,
    activeMemberships = activeMemberships,
    allSpaces = allSpaces,
    unlockedSpaceIds = unlockedSpaceIds,
    activeLayerIndex = activeLayerIndex,
    activePlacements = activePlacements,
    activeFolders = activeFolders,
    resolvedActiveFolders = resolvedActiveFolders,
    activeDockItems = activeDockItems,
    allApps = discoveryUiState.allApps,
    spaceScopedApps = spaceScopedApps,
    spaceScopedRecentApps = spaceScopedRecentApps,
    spaceScopedMostUsedApps = spaceScopedMostUsedApps,
    spaceUsageStats = spaceUsageStats,
    layer2CachedCatalog = layer2CachedCatalog,
    isCurrentSpaceUnlocked = isCurrentSpaceUnlocked,
    isLoading = discoveryUiState.isLoading,
    errorMessage = discoveryUiState.errorMessage,
    wallpaperStyle = wallpaperStyle
  )
}
