package com.multispace.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceDockItem
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.SpaceMembership
import com.multispace.domain.model.SpaceUsageStats

/**
 * Resolver for launcher wallpaper styling and contrast calculation.
 */
object LauncherWallpaperStyleResolver {
  fun resolveWallpaperStyle(
    activeSpace: Space?,
    defaultSurfaceColor: Color = Color(0xCC090B10),
    defaultOutlineColor: Color = Color(0x33A78BFA),
    defaultOnSurfaceColor: Color = Color.White
  ): LauncherWallpaperStyle {
    val currentBgType = activeSpace?.homeWallpaperType ?: activeSpace?.backgroundType ?: Space.BACKGROUND_DEFAULT
    val currentBgColor = activeSpace?.homeWallpaperColor ?: activeSpace?.backgroundColor
    val currentBgImageUri = activeSpace?.homeWallpaperImageUri ?: activeSpace?.backgroundImageUri
    val currentScaleMode = activeSpace?.homeWallpaperScaleMode ?: "crop"
    val currentZoomLevel = activeSpace?.homeWallpaperZoomLevel ?: 1.0f
    val currentDimLevel = activeSpace?.homeWallpaperDimLevel ?: 0.20f
    val currentOffsetX = activeSpace?.homeWallpaperOffsetX ?: 0.0f
    val currentOffsetY = activeSpace?.homeWallpaperOffsetY ?: 0.0f

    val isDarkThemeBackground = when (currentBgType) {
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

    val headerContentColor = if (isDarkThemeBackground) Color.White else defaultOnSurfaceColor
    val pillSurfaceColor = if (isDarkThemeBackground) {
      Color(0xCC090B10)
    } else {
      defaultSurfaceColor.copy(alpha = 0.85f)
    }
    val pillBorderColor = if (isDarkThemeBackground) {
      Color(0x33A78BFA)
    } else {
      defaultOutlineColor.copy(alpha = 0.6f)
    }

    return LauncherWallpaperStyle(
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
}

/**
 * State holder that coordinates non-UI state derivation for the launcher.
 * Facilitates fast, deterministic, JVM-executable unit testing of UI state derivation.
 */
class LauncherStateHolder(
  val appResolver: SpaceAppResolver = SpaceAppResolver
) {

  fun deriveState(
    discoveryUiState: AppDiscoveryUiState,
    activeSpace: Space?,
    allSpaces: List<Space>,
    activeMemberships: List<SpaceMembership>,
    unlockedSpaceIds: Set<String>,
    activeLayerIndex: Int,
    activePlacements: List<SpaceItemPlacement>,
    activeFolders: List<SpaceFolder>,
    activeDockItems: List<SpaceDockItem>,
    spaceMostUsedApps: List<DiscoveredApp>,
    spaceRecentApps: List<DiscoveredApp>,
    spaceUsageStats: SpaceUsageStats?,
    wallpaperStyle: LauncherWallpaperStyle
  ): LauncherUiState {
    val currentSpaceId = activeSpace?.id ?: Space.DEFAULT_SPACE_ID
    val currentSpace = activeSpace ?: Space.createDefault()
    val isCurrentSpaceUnlocked = if (activeSpace == null || !activeSpace.isProtected) {
      true
    } else {
      unlockedSpaceIds.contains(activeSpace.id)
    }

    val spaceScopedApps = appResolver.resolveSpaceScopedApps(
      allApps = discoveryUiState.allApps,
      activeMemberships = activeMemberships,
      isCurrentSpaceUnlocked = isCurrentSpaceUnlocked,
      activeSpace = activeSpace
    )

    val spaceScopedMostUsedAppsFull = appResolver.resolveSpaceScopedMostUsedAppsFull(
      spaceMostUsedApps = spaceMostUsedApps,
      spaceScopedApps = spaceScopedApps
    )

    val spaceScopedRecentApps = appResolver.resolveSpaceScopedRecentApps(
      spaceRecentApps = spaceRecentApps,
      spaceScopedApps = spaceScopedApps,
      maxCount = currentSpace.gridColumns
    )

    val spaceScopedMostUsedApps = spaceScopedMostUsedAppsFull.take(currentSpace.gridColumns)

    val resolvedActiveFolders = appResolver.resolveActiveFolders(
      activeFolders = activeFolders,
      dynamicMostUsedApps = spaceScopedMostUsedAppsFull,
      spaceId = currentSpaceId
    )

    val layer2CachedCatalog = appResolver.buildLayer2Catalog(spaceScopedApps)

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
}
