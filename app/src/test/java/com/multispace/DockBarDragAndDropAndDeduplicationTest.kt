package com.multispace

import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceDockItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockBarDragAndDropAndDeduplicationTest {

  @Test
  fun testDockItemsDeduplicationByPackageName() {
    val rawDockItems = listOf(
      SpaceDockItem(id = "1", spaceId = "default", orderIndex = 0, packageName = "com.android.chrome", componentName = ".Main"),
      SpaceDockItem(id = "2", spaceId = "default", orderIndex = 1, packageName = "com.google.android.dialer", componentName = ".Dialer"),
      SpaceDockItem(id = "3", spaceId = "default", orderIndex = 2, packageName = "com.android.chrome", componentName = ".Main"), // Duplicate
      SpaceDockItem(id = "4", spaceId = "default", orderIndex = 3, packageName = "com.google.android.apps.messaging", componentName = ".Conversation"),
      SpaceDockItem(id = "5", spaceId = "default", orderIndex = 4, packageName = "com.google.android.dialer", componentName = ".Dialer") // Duplicate
    )

    val deduplicated = rawDockItems.distinctBy { it.packageName }

    assertEquals("Deduplicated dock should only contain 3 unique packages", 3, deduplicated.size)
    assertEquals("com.android.chrome", deduplicated[0].packageName)
    assertEquals("com.google.android.dialer", deduplicated[1].packageName)
    assertEquals("com.google.android.apps.messaging", deduplicated[2].packageName)
  }

  @Test
  fun testDockDragAndDropReordering() {
    val items = mutableListOf(
      SpaceDockItem(id = "1", spaceId = "default", orderIndex = 0, packageName = "com.app.a", componentName = ".A"),
      SpaceDockItem(id = "2", spaceId = "default", orderIndex = 1, packageName = "com.app.b", componentName = ".B"),
      SpaceDockItem(id = "3", spaceId = "default", orderIndex = 2, packageName = "com.app.c", componentName = ".C"),
      SpaceDockItem(id = "4", spaceId = "default", orderIndex = 3, packageName = "com.app.d", componentName = ".D")
    )

    // User drags item "com.app.a" from index 0 to index 2 (after com.app.c)
    val dragged = items.removeAt(0)
    items.add(2, dragged)

    val reordered = items.mapIndexed { idx, itm -> itm.copy(orderIndex = idx) }

    assertEquals("com.app.b", reordered[0].packageName)
    assertEquals("com.app.c", reordered[1].packageName)
    assertEquals("com.app.a", reordered[2].packageName)
    assertEquals("com.app.d", reordered[3].packageName)

    assertEquals(0, reordered[0].orderIndex)
    assertEquals(1, reordered[1].orderIndex)
    assertEquals(2, reordered[2].orderIndex)
    assertEquals(3, reordered[3].orderIndex)
  }

  @Test
  fun testCenterDrawerButtonSplit() {
    val dockItems = listOf(
      SpaceDockItem(id = "1", spaceId = "default", orderIndex = 0, packageName = "com.app.1", componentName = ".1"),
      SpaceDockItem(id = "2", spaceId = "default", orderIndex = 1, packageName = "com.app.2", componentName = ".2"),
      SpaceDockItem(id = "3", spaceId = "default", orderIndex = 2, packageName = "com.app.3", componentName = ".3"),
      SpaceDockItem(id = "4", spaceId = "default", orderIndex = 3, packageName = "com.app.4", componentName = ".4")
    )

    val splitIndex = dockItems.size / 2
    val leftItems = dockItems.take(splitIndex)
    val rightItems = dockItems.drop(splitIndex)

    assertEquals(2, leftItems.size)
    assertEquals(2, rightItems.size)
    assertEquals("com.app.1", leftItems[0].packageName)
    assertEquals("com.app.2", leftItems[1].packageName)
    assertEquals("com.app.3", rightItems[0].packageName)
    assertEquals("com.app.4", rightItems[1].packageName)
  }

  @Test
  fun testAddAppToDockPreventsDuplicates() {
    val current = mutableListOf(
      SpaceDockItem(id = "1", spaceId = "default", orderIndex = 0, packageName = "com.app.existing", componentName = ".Main")
    )

    val duplicateCandidate = DiscoveredApp(
      id = "com.app.existing/.Main/0",
      packageName = "com.app.existing",
      activityName = ".Main",
      label = "Existing App"
    )

    val alreadyExists = current.any { it.packageName == duplicateCandidate.packageName }
    assertTrue("Should detect existing package in dock", alreadyExists)
  }
}
