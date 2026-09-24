package com.multispace

import android.content.Context
import com.multispace.data.database.LauncherDatabase
import com.multispace.data.database.LaunchHistoryDatabase
import com.multispace.data.preferences.LauncherPreferences
import com.multispace.data.repository.*
import com.multispace.domain.repository.*
import com.multispace.platform.AppDiscoveryManager
import com.multispace.platform.AppLaunchManager
import com.multispace.platform.LauncherSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Lightweight application-scoped dependency container.
 *
 * Avoids heavyweight DI frameworks while guaranteeing that MainActivity,
 * ConfigurationActivity, and active ViewModels share singleton instances for:
 * - App discovery and package broadcast registration
 * - Space, placement, membership, folder, dock, and launch history repositories
 * - Transient launcher authentication / session lock state
 * - Safe lifecycle-detached launch resolution
 */
class AppContainer(context: Context) {
  val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  val preferences: LauncherPreferences = LauncherPreferences.getInstance(context)
  val database: LauncherDatabase = LauncherDatabase.getInstance(context)
  val launchHistoryDatabase: LaunchHistoryDatabase = LaunchHistoryDatabase.getInstance(context)

  val sessionManager: LauncherSessionManager = LauncherSessionManager(context)

  val spaceMembershipRepository: SpaceMembershipRepository = RoomSpaceMembershipRepository(
    database.spaceMembershipDao(),
    database
  )

  val placementRepository: PlacementRepository = RoomPlacementRepository(
    database.spaceDao(),
    database.spaceLayoutDao(),
    database.spaceMembershipDao(),
    context,
    database
  )

  val folderRepository: FolderRepository = RoomFolderRepository(
    database.spaceLayoutDao(),
    database
  )

  val dockRepository: DockRepository = RoomDockRepository(
    database.spaceDao(),
    database.spaceLayoutDao(),
    placementRepository,
    database
  )

  val spaceRepository: SpaceRepository = RoomSpaceRepository(
    spaceDao = database.spaceDao(),
    membershipDao = database.spaceMembershipDao(),
    layoutDao = database.spaceLayoutDao(),
    preferences = preferences,
    context = context,
    database = database,
    membershipRepository = spaceMembershipRepository,
    placementRepository = placementRepository,
    folderRepository = folderRepository,
    dockRepository = dockRepository
  )

  val launchHistoryRepository: LaunchHistoryRepository = RoomLaunchHistoryRepository(
    launchHistoryDao = launchHistoryDatabase.launchHistoryDao()
  )

  val discoveryManager: AppDiscoveryManager = AppDiscoveryManager(context)

  val appLaunchManager: AppLaunchManager = AppLaunchManager(
    context = context,
    historyRepository = launchHistoryRepository,
    spaceRepository = spaceRepository,
    applicationScope = applicationScope
  )
}
