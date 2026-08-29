package com.multispace.domain.repository

import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceMembership
import kotlinx.coroutines.flow.Flow

/**
 * Domain Repository interface for Space management, Space persistence, and Space membership.
 */
interface SpaceRepository {
  val allSpacesFlow: Flow<List<Space>>
  val activeSpaceIdFlow: Flow<String?>
  val activeSpaceFlow: Flow<Space?>

  suspend fun ensureDefaultSpaceInitialized(): Result<Space>
  suspend fun getSpaceById(spaceId: String): Space?
  suspend fun setActiveSpaceId(spaceId: String): Result<Unit>
  suspend fun createSpace(name: String, layoutType: String = "GRID_4"): Result<Space>
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
    initialApps: List<DiscoveredApp> = emptyList()
  ): Result<Space>
  suspend fun renameSpace(spaceId: String, newName: String): Result<Unit>
  suspend fun deleteSpace(spaceId: String): Result<Unit>

  fun getMembershipsForSpaceFlow(spaceId: String): Flow<List<SpaceMembership>>
  suspend fun getMembershipsForSpace(spaceId: String): List<SpaceMembership>
  suspend fun addAppToSpace(spaceId: String, app: DiscoveredApp): Result<Unit>
  suspend fun removeAppFromSpace(spaceId: String, app: DiscoveredApp): Result<Unit>
  suspend fun isAppInSpace(spaceId: String, app: DiscoveredApp): Boolean

  suspend fun setSpacePin(spaceId: String, pin: String): Result<Unit>
  suspend fun changeSpacePin(spaceId: String, currentPin: String, newPin: String): Result<Unit>
  suspend fun disableSpacePin(spaceId: String, currentPin: String): Result<Unit>
  suspend fun verifySpacePin(spaceId: String, pin: String): Boolean

  suspend fun updateSpaceCustomization(
    spaceId: String,
    backgroundType: String,
    backgroundColor: Long?,
    backgroundImageUri: String?,
    gridColumns: Int,
    iconSize: String,
    labelVisibility: Boolean
  ): Result<Unit>

  suspend fun reorderSpaceApp(
    spaceId: String,
    app: DiscoveredApp,
    direction: Int
  ): Result<Unit>

  suspend fun reorderSpaceApps(
    spaceId: String,
    orderedApps: List<DiscoveredApp>
  ): Result<Unit>
}
