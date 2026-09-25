package com.multispace

import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceFolder
import com.multispace.domain.model.SpaceFolderItem
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.SpaceMembership
import com.multispace.presentation.AppDiscoveryUiState
import com.multispace.presentation.LauncherStateHolder
import com.multispace.presentation.LauncherWallpaperStyleResolver
import com.multispace.presentation.SpaceAppResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceAppResolverAndStateHolderTest {

  private fun createApp(
    packageName: String,
    label: String,
    userHandleId: Long = 0L,
    activityName: String = "$packageName.MainActivity"
  ): DiscoveredApp {
    return DiscoveredApp(
      id = "$packageName/$userHandleId",
      packageName = packageName,
      activityName = activityName,
      label = label,
      userHandleId = userHandleId
    )
  }

  @Test
  fun testResolveSpaceScopedApps_emptyAppsReturnsEmpty() {
    val result = SpaceAppResolver.resolveSpaceScopedApps(
      allApps = emptyList(),
      activeMemberships = emptyList(),
      isCurrentSpaceUnlocked = true,
      activeSpace = Space.createDefault()
    )
    assertTrue(result.isEmpty())
  }

  @Test
  fun testResolveSpaceScopedApps_lockedSpaceReturnsEmpty() {
    val apps = listOf(createApp("com.app.one", "One"))
    val result = SpaceAppResolver.resolveSpaceScopedApps(
      allApps = apps,
      activeMemberships = emptyList(),
      isCurrentSpaceUnlocked = false,
      activeSpace = Space.createDefault()
    )
    assertTrue(result.isEmpty())
  }

  @Test
  fun testResolveSpaceScopedApps_defaultSpaceWithNoMembershipsReturnsAllApps() {
    val apps = listOf(
      createApp("com.app.one", "One"),
      createApp("com.app.two", "Two")
    )
    val result = SpaceAppResolver.resolveSpaceScopedApps(
      allApps = apps,
      activeMemberships = emptyList(),
      isCurrentSpaceUnlocked = true,
      activeSpace = Space.createDefault()
    )
    assertEquals(2, result.size)
    assertEquals("com.app.one", result[0].packageName)
    assertEquals("com.app.two", result[1].packageName)
  }

  @Test
  fun testResolveSpaceScopedApps_customSpaceWithNoMembershipsReturnsEmpty() {
    val apps = listOf(createApp("com.app.one", "One"))
    val customSpace = Space(id = "work_space", name = "Work")
    val result = SpaceAppResolver.resolveSpaceScopedApps(
      allApps = apps,
      activeMemberships = emptyList(),
      isCurrentSpaceUnlocked = true,
      activeSpace = customSpace
    )
    assertTrue(result.isEmpty())
  }

  @Test
  fun testResolveSpaceScopedApps_respectsUserHandleIdInMemberships() {
    val appPersonal = createApp("com.app.chat", "Chat", userHandleId = 0)
    val appWork = createApp("com.app.chat", "Chat Work", userHandleId = 10)
    val apps = listOf(appPersonal, appWork)

    val customSpace = Space(id = "work_space", name = "Work")
    val memberships = listOf(
      SpaceMembership(
        spaceId = "work_space",
        packageName = "com.app.chat",
        componentName = "com.app.chat.MainActivity",
        userHandleId = 10L
      )
    )

    val result = SpaceAppResolver.resolveSpaceScopedApps(
      allApps = apps,
      activeMemberships = memberships,
      isCurrentSpaceUnlocked = true,
      activeSpace = customSpace
    )

    assertEquals(1, result.size)
    assertEquals(10L, result[0].userHandleId)
    assertEquals("Chat Work", result[0].label)
  }

  @Test
  fun testResolveSpaceScopedApps_deduplicatesIdentities() {
    val app = createApp("com.app.one", "One")
    val customSpace = Space(id = "space_1", name = "Space 1")
    val memberships = listOf(
      SpaceMembership(
        spaceId = "space_1",
        packageName = "com.app.one",
        componentName = "com.app.one.MainActivity",
        userHandleId = 0L
      ),
      SpaceMembership(
        spaceId = "space_1",
        packageName = "com.app.one",
        componentName = "com.app.one.MainActivity",
        userHandleId = 0L
      )
    )

    val result = SpaceAppResolver.resolveSpaceScopedApps(
      allApps = listOf(app),
      activeMemberships = memberships,
      isCurrentSpaceUnlocked = true,
      activeSpace = customSpace
    )

    assertEquals(1, result.size)
  }

  @Test
  fun testResolveSpaceScopedMostUsedApps_onlyIncludesAppsInSpace() {
    val app1 = createApp("com.app.one", "One")
    val app2 = createApp("com.app.two", "Two")
    val app3 = createApp("com.app.three", "Three")

    val spaceScoped = listOf(app1, app2)
    val globalMostUsed = listOf(app3, app2, app1)

    val resolved = SpaceAppResolver.resolveSpaceScopedMostUsedAppsFull(
      spaceMostUsedApps = globalMostUsed,
      spaceScopedApps = spaceScoped
    )

    assertEquals(2, resolved.size)
    assertEquals("com.app.two", resolved[0].packageName)
    assertEquals("com.app.one", resolved[1].packageName)
  }

  @Test
  fun testResolveSpaceScopedRecentApps_limitsToMaxCount() {
    val apps = (1..10).map { createApp("com.app.$it", "App $it") }
    val resolved = SpaceAppResolver.resolveSpaceScopedRecentApps(
      spaceRecentApps = apps,
      spaceScopedApps = apps,
      maxCount = 4
    )
    assertEquals(4, resolved.size)
  }

  @Test
  fun testResolveActiveFolders_populatesDynamicItemsForMostUsedFolder() {
    val mostUsedApps = listOf(
      createApp("com.app.top1", "Top 1"),
      createApp("com.app.top2", "Top 2")
    )

    val normalFolder = SpaceFolder(id = "folder_normal", spaceId = "space_1", name = "Games")
    val mostUsedFolder = SpaceFolder(
      id = SpaceFolder.getMostUsedFolderId("space_1"),
      spaceId = "space_1",
      name = SpaceFolder.MOST_USED_FOLDER_NAME
    )

    val resolved = SpaceAppResolver.resolveActiveFolders(
      activeFolders = listOf(normalFolder, mostUsedFolder),
      dynamicMostUsedApps = mostUsedApps,
      spaceId = "space_1"
    )

    assertEquals(2, resolved.size)
    assertEquals("Games", resolved[0].name)
    assertTrue(resolved[0].items.isEmpty())

    val muResult = resolved[1]
    assertEquals(SpaceFolder.MOST_USED_FOLDER_NAME, muResult.name)
    assertEquals(2, muResult.items.size)
    assertEquals("com.app.top1", muResult.items[0].packageName)
    assertEquals("com.app.top2", muResult.items[1].packageName)
    assertEquals(0, muResult.items[0].orderIndex)
    assertEquals(1, muResult.items[1].orderIndex)
  }

  @Test
  fun testBuildLayer2Catalog_sortsAndGroupsCorrectly() {
    val apps = listOf(
      createApp("com.app.z", "Zebra"),
      createApp("com.app.a", "Apple"),
      createApp("com.app.num", "123 Numbers"),
      createApp("com.app.a2", "Avocado")
    )

    val catalog = SpaceAppResolver.buildLayer2Catalog(apps)

    assertEquals(4, catalog.sortedApps.size)
    assertEquals("123 Numbers", catalog.sortedApps[0].label)
    assertEquals("Apple", catalog.sortedApps[1].label)
    assertEquals("Avocado", catalog.sortedApps[2].label)
    assertEquals("Zebra", catalog.sortedApps[3].label)

    assertTrue(catalog.activeLetters.contains('A'))
    assertTrue(catalog.activeLetters.contains('Z'))
    assertEquals(1, catalog.letterToFirstIndex['A'])
    assertEquals(3, catalog.letterToFirstIndex['Z'])
  }

  @Test
  fun testLauncherStateHolder_deriveStateProducesValidUiState() {
    val stateHolder = LauncherStateHolder()
    val apps = listOf(createApp("com.app.one", "One"))
    val defaultSpace = Space.createDefault()

    val wallpaperStyle = LauncherWallpaperStyleResolver.resolveWallpaperStyle(defaultSpace)

    val uiState = stateHolder.deriveState(
      discoveryUiState = AppDiscoveryUiState(allApps = apps),
      activeSpace = defaultSpace,
      allSpaces = listOf(defaultSpace),
      activeMemberships = emptyList(),
      unlockedSpaceIds = emptySet(),
      activeLayerIndex = 1,
      activePlacements = emptyList(),
      activeFolders = emptyList(),
      activeDockItems = emptyList(),
      spaceMostUsedApps = emptyList(),
      spaceRecentApps = emptyList(),
      spaceUsageStats = null,
      wallpaperStyle = wallpaperStyle
    )

    assertEquals(defaultSpace, uiState.activeSpace)
    assertEquals(1, uiState.spaceScopedApps.size)
    assertTrue(uiState.isCurrentSpaceUnlocked)
    assertFalse(uiState.isLoading)
  }

  @Test
  fun testMostUsedFolder_hiddenWhenEmpty() {
    val stateHolder = LauncherStateHolder()
    val apps = listOf(createApp("com.app.one", "One"))
    val defaultSpace = Space.createDefault()
    val wallpaperStyle = LauncherWallpaperStyleResolver.resolveWallpaperStyle(defaultSpace)

    val mostUsedFolder = SpaceFolder(
      id = SpaceFolder.getMostUsedFolderId(defaultSpace.id),
      spaceId = defaultSpace.id,
      name = SpaceFolder.MOST_USED_FOLDER_NAME
    )
    val mostUsedPlacement = SpaceItemPlacement(
      id = SpaceFolder.getMostUsedPlacementId(defaultSpace.id),
      spaceId = defaultSpace.id,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 3,
      itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
      folderId = mostUsedFolder.id
    )
    val normalAppPlacement = SpaceItemPlacement(
      id = "p_app_one",
      spaceId = defaultSpace.id,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.app.one",
      componentName = "com.app.one.MainActivity"
    )

    // With 0 most used apps:
    val uiState = stateHolder.deriveState(
      discoveryUiState = AppDiscoveryUiState(allApps = apps),
      activeSpace = defaultSpace,
      allSpaces = listOf(defaultSpace),
      activeMemberships = emptyList(),
      unlockedSpaceIds = emptySet(),
      activeLayerIndex = 1,
      activePlacements = listOf(normalAppPlacement, mostUsedPlacement),
      activeFolders = listOf(mostUsedFolder),
      activeDockItems = emptyList(),
      spaceMostUsedApps = emptyList(),
      spaceRecentApps = emptyList(),
      spaceUsageStats = null,
      wallpaperStyle = wallpaperStyle
    )

    // Most used folder MUST NOT be visible when empty!
    assertEquals(1, uiState.activePlacements.size)
    assertEquals("p_app_one", uiState.activePlacements[0].id)
    assertFalse(uiState.activePlacements.any { it.folderId == mostUsedFolder.id })
    assertTrue(uiState.resolvedActiveFolders.none { it.isMostUsedFolder })
  }

  @Test
  fun testMostUsedFolder_visibleWhenNotEmpty() {
    val stateHolder = LauncherStateHolder()
    val app1 = createApp("com.app.one", "One")
    val defaultSpace = Space.createDefault()
    val wallpaperStyle = LauncherWallpaperStyleResolver.resolveWallpaperStyle(defaultSpace)

    val mostUsedFolder = SpaceFolder(
      id = SpaceFolder.getMostUsedFolderId(defaultSpace.id),
      spaceId = defaultSpace.id,
      name = SpaceFolder.MOST_USED_FOLDER_NAME
    )
    val mostUsedPlacement = SpaceItemPlacement(
      id = SpaceFolder.getMostUsedPlacementId(defaultSpace.id),
      spaceId = defaultSpace.id,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 3,
      itemType = SpaceItemPlacement.ITEM_TYPE_FOLDER,
      folderId = mostUsedFolder.id
    )
    val normalAppPlacement = SpaceItemPlacement(
      id = "p_app_one",
      spaceId = defaultSpace.id,
      layer = SpaceItemPlacement.LAYER_HOME,
      pageIndex = 0,
      positionIndex = 0,
      itemType = SpaceItemPlacement.ITEM_TYPE_APP,
      packageName = "com.app.one",
      componentName = "com.app.one.MainActivity"
    )

    // With 1 most used app:
    val uiState = stateHolder.deriveState(
      discoveryUiState = AppDiscoveryUiState(allApps = listOf(app1)),
      activeSpace = defaultSpace,
      allSpaces = listOf(defaultSpace),
      activeMemberships = emptyList(),
      unlockedSpaceIds = emptySet(),
      activeLayerIndex = 1,
      activePlacements = listOf(normalAppPlacement, mostUsedPlacement),
      activeFolders = listOf(mostUsedFolder),
      activeDockItems = emptyList(),
      spaceMostUsedApps = listOf(app1),
      spaceRecentApps = emptyList(),
      spaceUsageStats = null,
      wallpaperStyle = wallpaperStyle
    )

    // Most used folder MUST be visible when it has one or more apps!
    assertEquals(2, uiState.activePlacements.size)
    assertTrue(uiState.activePlacements.any { it.folderId == mostUsedFolder.id })
    val resolvedMu = uiState.resolvedActiveFolders.firstOrNull { it.isMostUsedFolder }
    assertNotNull(resolvedMu)
    assertEquals(1, resolvedMu?.items?.size)
    assertEquals("com.app.one", resolvedMu?.items?.get(0)?.packageName)
  }
}
