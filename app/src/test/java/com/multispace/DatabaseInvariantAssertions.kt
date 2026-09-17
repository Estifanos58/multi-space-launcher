package com.multispace

import com.multispace.data.database.LauncherDatabase
import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.PlacementValidator
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.appIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

object DatabaseInvariantAssertions {

  suspend fun assertNoDuplicatePositions(
    database: LauncherDatabase,
    spaceId: String,
    layer: Int = SpaceItemPlacement.LAYER_HOME
  ) {
    val placements = database.spaceLayoutDao().getPlacementsForSpaceLayer(spaceId, layer)
    val positions = placements.map { it.pageIndex to it.positionIndex }
    val duplicates = positions.groupBy { it }.filter { it.value.size > 1 }.keys
    assertTrue("Found duplicate positions in space $spaceId layer $layer: $duplicates", duplicates.isEmpty())
  }

  suspend fun assertNoDuplicateAppIdentities(
    database: LauncherDatabase,
    spaceId: String,
    layer: Int = SpaceItemPlacement.LAYER_HOME
  ) {
    val placements = database.spaceLayoutDao().getPlacementsForSpaceLayer(spaceId, layer)
    val appPlacements = placements.filter { it.itemType == SpaceItemPlacement.ITEM_TYPE_APP && it.packageName != null }
    val identities = appPlacements.map { it.appIdentity }
    val duplicates = identities.groupBy { it }.filter { it.value.size > 1 }.keys
    assertTrue("Found duplicate app identities in space $spaceId layer $layer: $duplicates", duplicates.isEmpty())
  }

  suspend fun assertNoOrphanFolderPlacements(
    database: LauncherDatabase,
    spaceId: String
  ) {
    val placements = database.spaceLayoutDao().getAllPlacementsForSpace(spaceId)
    val folderPlacements = placements.filter { it.itemType == SpaceItemPlacement.ITEM_TYPE_FOLDER }
    val existingFolders = database.spaceLayoutDao().getFoldersForSpace(spaceId).map { it.id }.toSet()

    for (fp in folderPlacements) {
      assertNotNull("Folder placement ${fp.id} must have a non-null folderId", fp.folderId)
      assertTrue(
        "Folder placement ${fp.id} references folder ${fp.folderId} which does not exist in DB",
        existingFolders.contains(fp.folderId)
      )
    }
  }

  suspend fun assertNoOrphanFolders(
    database: LauncherDatabase,
    spaceId: String
  ) {
    val placements = database.spaceLayoutDao().getAllPlacementsForSpace(spaceId)
    val placedFolderIds = placements.mapNotNull { it.folderId }.toSet()
    val allFolders = database.spaceLayoutDao().getFoldersForSpace(spaceId)

    for (folder in allFolders) {
      assertTrue(
        "Folder ${folder.id} ('${folder.name}') exists in DB but has no placement on any page",
        placedFolderIds.contains(folder.id)
      )
    }
  }

  suspend fun assertDockPositionsSequential(
    database: LauncherDatabase,
    spaceId: String
  ) {
    val dockItems = database.spaceLayoutDao().getDockItemsForSpace(spaceId)
    for (i in dockItems.indices) {
      assertEquals(
        "Dock item at index $i must have orderIndex $i",
        i,
        dockItems[i].orderIndex
      )
    }
  }

  suspend fun assertValidMembershipOrdering(
    database: LauncherDatabase,
    spaceId: String
  ) {
    val memberships = database.spaceMembershipDao().getMembershipsForSpace(spaceId)
    val sorted = memberships.sortedBy { it.orderIndex }
    for (i in sorted.indices) {
      assertEquals(
        "Membership at sorted position $i must have orderIndex $i",
        i,
        sorted[i].orderIndex
      )
    }
  }

  suspend fun assertAllLayoutInvariants(
    database: LauncherDatabase,
    spaceId: String,
    gridCols: Int = 4,
    gridRows: Int = 5
  ) {
    assertNoDuplicatePositions(database, spaceId)
    assertNoDuplicateAppIdentities(database, spaceId)
    assertNoOrphanFolderPlacements(database, spaceId)
    assertDockPositionsSequential(database, spaceId)
    assertValidMembershipOrdering(database, spaceId)

    val placements = database.spaceLayoutDao().getPlacementsForSpaceLayer(spaceId, SpaceItemPlacement.LAYER_HOME)
    val domainPlacements = placements.map { it.toDomain() }
    val result = PlacementValidator.validatePlacements(domainPlacements, cols = gridCols, rows = gridRows)
    assertTrue("PlacementValidator detected invariant violations: ${result.issues}", result.isValid)
  }
}
