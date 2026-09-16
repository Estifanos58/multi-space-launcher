package com.multispace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.multispace.domain.model.*

class DockInteractionController(
  private val spaceViewModel: SpaceViewModel,
  private val getSpaceScopedApps: () -> List<DiscoveredApp>,
  private val getActivePlacements: () -> List<SpaceItemPlacement>
) {
  fun removeAppFromDock(spaceId: String, dockItemId: String) {
    spaceViewModel.removeAppFromDock(spaceId, dockItemId)
  }

  fun reorderDockItems(spaceId: String, reordered: List<SpaceDockItem>) {
    spaceViewModel.reorderDockItems(spaceId, reordered)
  }

  fun onDropFromDockToDesktop(
    space: Space,
    dockItem: SpaceDockItem,
    app: DiscoveredApp,
    targetPage: Int,
    targetPos: Int
  ) {
    val activePlacements = getActivePlacements()
    val spaceScopedApps = getSpaceScopedApps()

    val existing = activePlacements.firstOrNull {
      it.pageIndex == targetPage && it.positionIndex == targetPos
    }

    if (existing != null && !existing.isWidget && !existing.isFolder) {
      val targetApp: DiscoveredApp? = existing.appIdentity?.let { identity ->
        spaceScopedApps.firstOrNull { identity.matches(it.appIdentity) }
      } ?: spaceScopedApps.firstOrNull { it.packageName == existing.packageName }

      if (targetApp != null) {
        spaceViewModel.createFolderFromApps(
          spaceId = space.id,
          pageIndex = targetPage,
          positionIndex = targetPos,
          folderName = "Folder",
          sourceApp = app,
          targetApp = targetApp,
          sourcePlacementId = null,
          targetPlacementId = existing.id
        )
        spaceViewModel.removeAppFromDock(space.id, dockItem.id)
      } else {
        spaceViewModel.moveAppFromDockToHome(
          spaceId = space.id,
          dockItemId = dockItem.id,
          app = app,
          targetPage = targetPage,
          targetPosition = targetPos
        )
      }
    } else {
      spaceViewModel.moveAppFromDockToHome(
        spaceId = space.id,
        dockItemId = dockItem.id,
        app = app,
        targetPage = targetPage,
        targetPosition = targetPos
      )
    }
  }

  fun onDropItemToDock(
    space: Space,
    placement: SpaceItemPlacement,
    app: DiscoveredApp,
    targetDockIndex: Int
  ) {
    spaceViewModel.moveAppFromHomeToDock(
      spaceId = space.id,
      placementId = placement.id,
      app = app,
      targetDockIndex = targetDockIndex
    )
  }
}

@Composable
fun rememberDockInteractionController(
  spaceViewModel: SpaceViewModel,
  spaceScopedApps: List<DiscoveredApp>,
  activePlacements: List<SpaceItemPlacement>
): DockInteractionController {
  return remember(spaceViewModel, spaceScopedApps, activePlacements) {
    DockInteractionController(
      spaceViewModel = spaceViewModel,
      getSpaceScopedApps = { spaceScopedApps },
      getActivePlacements = { activePlacements }
    )
  }
}
