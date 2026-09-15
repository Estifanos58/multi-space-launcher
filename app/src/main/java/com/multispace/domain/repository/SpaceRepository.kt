package com.multispace.domain.repository

import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.PageTurnEffect
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceMembership
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository interface for Space management, Space persistence, and Space membership.
 */
interface SpaceRepository : SpaceMembershipRepository, PlacementRepository, FolderRepository, DockRepository {
  val allSpacesFlow: Flow<List<Space>>
  val activeSpaceIdFlow: Flow<String?>
  val activeSpaceFlow: Flow<Space?>
  val activeSpaceStateFlow: Flow<com.multispace.domain.model.ActiveSpaceState>

  suspend fun ensureDefaultSpaceInitialized(initialApps: List<DiscoveredApp> = emptyList()): Result<Space>
  suspend fun getSpaceById(spaceId: String): Space?
  suspend fun setActiveSpaceId(spaceId: String): Result<Unit>
  suspend fun createSpace(
    name: String,
    layoutType: String = "GRID_4",
    initialApps: List<DiscoveredApp> = emptyList()
  ): Result<Space>
  suspend fun createFullSpace(
    name: String,
    authPolicy: String = Space.AUTH_NONE,
    pinSalt: String? = null,
    pinHash: String? = null,
    patternRows: Int = Space.DEFAULT_PATTERN_ROWS,
    patternCols: Int = Space.DEFAULT_PATTERN_COLS,
    backgroundType: String = Space.BACKGROUND_DEFAULT,
    backgroundColor: Long? = null,
    backgroundImageUri: String? = null,
    homeWallpaperType: String = Space.BACKGROUND_DEFAULT,
    homeWallpaperColor: Long? = null,
    homeWallpaperImageUri: String? = null,
    phoneLockWallpaperType: String = Space.BACKGROUND_DEFAULT,
    phoneLockWallpaperColor: Long? = null,
    phoneLockWallpaperImageUri: String? = null,
    spaceLockWallpaperType: String = Space.BACKGROUND_DEFAULT,
    spaceLockWallpaperColor: Long? = null,
    spaceLockWallpaperImageUri: String? = null,
    appTheme: String = Space.THEME_DEFAULT,
    gridColumns: Int = Space.DEFAULT_GRID_COLUMNS,
    iconSize: String = Space.ICON_SIZE_MEDIUM,
    labelVisibility: Boolean = true,
    layer1DisplayMode: String = Space.DISPLAY_MODE_PAGE,
    layer2DisplayMode: String = Space.DISPLAY_MODE_SCROLL,
    layer2AccessMode: String = Space.ACCESS_MODE_DOCK_BUTTON,
    dockCapacity: Int = Space.DEFAULT_DOCK_CAPACITY,
    layoutPreset: String = Space.PRESET_DEFAULT,
    useLayer2: Boolean = true,
    homeWallpaperScaleMode: String = "crop",
    homeWallpaperZoomLevel: Float = 1.0f,
    homeWallpaperDimLevel: Float = 0.20f,
    homeWallpaperOffsetX: Float = 0.0f,
    homeWallpaperOffsetY: Float = 0.0f,
    phoneLockWallpaperScaleMode: String = "crop",
    phoneLockWallpaperZoomLevel: Float = 1.0f,
    phoneLockWallpaperDimLevel: Float = 0.20f,
    phoneLockWallpaperOffsetX: Float = 0.0f,
    phoneLockWallpaperOffsetY: Float = 0.0f,
    spaceLockWallpaperScaleMode: String = "crop",
    spaceLockWallpaperZoomLevel: Float = 1.0f,
    spaceLockWallpaperDimLevel: Float = 0.20f,
    spaceLockWallpaperOffsetX: Float = 0.0f,
    spaceLockWallpaperOffsetY: Float = 0.0f,
    pageTurnEffect: PageTurnEffect = PageTurnEffect.NORMAL,
    pageTurnDurationMs: Int = Space.DEFAULT_PAGE_TURN_DURATION_MS,
    pageTurnIntensity: Float = Space.DEFAULT_PAGE_TURN_INTENSITY,
    initialApps: List<DiscoveredApp> = emptyList()
  ): Result<Space>

  suspend fun updateFullSpace(
    spaceId: String,
    name: String,
    authPolicy: String = Space.AUTH_NONE,
    pinSalt: String? = null,
    pinHash: String? = null,
    keepExistingCredentials: Boolean = false,
    patternRows: Int = Space.DEFAULT_PATTERN_ROWS,
    patternCols: Int = Space.DEFAULT_PATTERN_COLS,
    backgroundType: String = Space.BACKGROUND_DEFAULT,
    backgroundColor: Long? = null,
    backgroundImageUri: String? = null,
    homeWallpaperType: String = Space.BACKGROUND_DEFAULT,
    homeWallpaperColor: Long? = null,
    homeWallpaperImageUri: String? = null,
    phoneLockWallpaperType: String = Space.BACKGROUND_DEFAULT,
    phoneLockWallpaperColor: Long? = null,
    phoneLockWallpaperImageUri: String? = null,
    spaceLockWallpaperType: String = Space.BACKGROUND_DEFAULT,
    spaceLockWallpaperColor: Long? = null,
    spaceLockWallpaperImageUri: String? = null,
    appTheme: String = Space.THEME_DEFAULT,
    gridColumns: Int = Space.DEFAULT_GRID_COLUMNS,
    iconSize: String = Space.ICON_SIZE_MEDIUM,
    labelVisibility: Boolean = true,
    layer1DisplayMode: String = Space.DISPLAY_MODE_PAGE,
    layer2DisplayMode: String = Space.DISPLAY_MODE_SCROLL,
    layer2AccessMode: String = Space.ACCESS_MODE_DOCK_BUTTON,
    dockCapacity: Int = Space.DEFAULT_DOCK_CAPACITY,
    layoutPreset: String = Space.PRESET_DEFAULT,
    useLayer2: Boolean = true,
    homeWallpaperScaleMode: String = "crop",
    homeWallpaperZoomLevel: Float = 1.0f,
    homeWallpaperDimLevel: Float = 0.20f,
    homeWallpaperOffsetX: Float = 0.0f,
    homeWallpaperOffsetY: Float = 0.0f,
    phoneLockWallpaperScaleMode: String = "crop",
    phoneLockWallpaperZoomLevel: Float = 1.0f,
    phoneLockWallpaperDimLevel: Float = 0.20f,
    phoneLockWallpaperOffsetX: Float = 0.0f,
    phoneLockWallpaperOffsetY: Float = 0.0f,
    spaceLockWallpaperScaleMode: String = "crop",
    spaceLockWallpaperZoomLevel: Float = 1.0f,
    spaceLockWallpaperDimLevel: Float = 0.20f,
    spaceLockWallpaperOffsetX: Float = 0.0f,
    spaceLockWallpaperOffsetY: Float = 0.0f,
    pageTurnEffect: PageTurnEffect = PageTurnEffect.NORMAL,
    pageTurnDurationMs: Int = Space.DEFAULT_PAGE_TURN_DURATION_MS,
    pageTurnIntensity: Float = Space.DEFAULT_PAGE_TURN_INTENSITY,
    updatedApps: List<DiscoveredApp> = emptyList()
  ): Result<Space>
  suspend fun renameSpace(spaceId: String, newName: String): Result<Unit>
  suspend fun deleteSpace(spaceId: String): Result<Unit>
  suspend fun updatePageTurnSettings(
    spaceId: String,
    effect: PageTurnEffect,
    durationMs: Int = Space.DEFAULT_PAGE_TURN_DURATION_MS,
    intensity: Float = Space.DEFAULT_PAGE_TURN_INTENSITY
  ): Result<Unit>

  suspend fun setSpacePin(spaceId: String, pin: String): Result<Unit>
  suspend fun changeSpacePin(spaceId: String, currentPin: String, newPin: String): Result<Unit>
  suspend fun disableSpacePin(spaceId: String, currentPin: String): Result<Unit>
  suspend fun verifySpacePin(spaceId: String, pin: String): Boolean
  suspend fun findSpaceMatchingCredential(credential: String): Space?

  suspend fun updateSpaceCustomization(
    spaceId: String,
    backgroundType: String,
    backgroundColor: Long?,
    backgroundImageUri: String?,
    gridColumns: Int,
    iconSize: String,
    labelVisibility: Boolean
  ): Result<Unit>

  // --- Layout Configuration & Presets ---
  suspend fun updateSpaceLayoutSettings(
    spaceId: String,
    layer1DisplayMode: String,
    layer2DisplayMode: String,
    layer2AccessMode: String,
    dockCapacity: Int,
    gridColumns: Int
  ): Result<Unit>

  suspend fun applyLayoutPreset(spaceId: String, preset: com.multispace.domain.model.LayoutPreset, apps: List<DiscoveredApp>): Result<Unit>
  suspend fun importCurrentHomeLayout(spaceId: String, allInstalledApps: List<DiscoveredApp>): Result<com.multispace.domain.model.ImportReport>
  suspend fun cleanupUninstalledApp(packageName: String): Result<Unit>

  // --- Desktop Customization & Page Control ---
  suspend fun updateSpaceTheme(
    spaceId: String,
    appTheme: String,
    gridColumns: Int? = null,
    iconSize: String? = null,
    labelVisibility: Boolean? = null
  ): Result<Unit>
  suspend fun updateSpaceWallpaper(
    spaceId: String,
    wallpaperType: String,
    wallpaperColor: Long?,
    wallpaperImageUri: String?
  ): Result<Unit>
}
