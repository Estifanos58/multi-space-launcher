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
  spaceViewModel: SpaceViewModel,
  stateHolder: LauncherStateHolder = remember { LauncherStateHolder() }
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

  val defaultSurface = MaterialTheme.colorScheme.surface
  val defaultOutline = MaterialTheme.colorScheme.outlineVariant
  val defaultOnSurface = MaterialTheme.colorScheme.onSurface

  val wallpaperStyle = remember(activeSpace, defaultSurface, defaultOutline, defaultOnSurface) {
    LauncherWallpaperStyleResolver.resolveWallpaperStyle(
      activeSpace = activeSpace,
      defaultSurfaceColor = defaultSurface,
      defaultOutlineColor = defaultOutline,
      defaultOnSurfaceColor = defaultOnSurface
    )
  }

  val currentSpaceId = activeSpace?.id ?: Space.DEFAULT_SPACE_ID

  val spaceMostUsedApps by remember(currentSpaceId) {
    discoveryViewModel.getMostUsedAppsFlow(currentSpaceId)
  }.collectAsStateWithLifecycle(initialValue = emptyList())

  val spaceRecentApps by remember(currentSpaceId) {
    discoveryViewModel.getRecentAppsFlow(currentSpaceId)
  }.collectAsStateWithLifecycle(initialValue = emptyList())

  val spaceUsageStats by remember(currentSpaceId) {
    discoveryViewModel.getSpaceUsageStatsFlow(currentSpaceId)
  }.collectAsStateWithLifecycle(initialValue = null)

  return remember(
    discoveryUiState,
    activeSpace,
    allSpaces,
    activeMemberships,
    unlockedSpaceIds,
    activeLayerIndex,
    activePlacements,
    activeFolders,
    activeDockItems,
    spaceMostUsedApps,
    spaceRecentApps,
    spaceUsageStats,
    wallpaperStyle
  ) {
    stateHolder.deriveState(
      discoveryUiState = discoveryUiState,
      activeSpace = activeSpace,
      allSpaces = allSpaces,
      activeMemberships = activeMemberships,
      unlockedSpaceIds = unlockedSpaceIds,
      activeLayerIndex = activeLayerIndex,
      activePlacements = activePlacements,
      activeFolders = activeFolders,
      activeDockItems = activeDockItems,
      spaceMostUsedApps = spaceMostUsedApps,
      spaceRecentApps = spaceRecentApps,
      spaceUsageStats = spaceUsageStats,
      wallpaperStyle = wallpaperStyle
    )
  }
}
