package com.multispace

import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Layer2AlphabetFastScrollTest {

  private fun createApp(label: String, pkg: String): DiscoveredApp {
    return DiscoveredApp(
      id = "$pkg/.MainActivity#0",
      packageName = pkg,
      activityName = ".MainActivity",
      label = label,
      userHandleId = 0L,
      isSystemApp = false
    )
  }

  private fun buildLetterIndexMap(apps: List<DiscoveredApp>): Map<Char, Int> {
    val map = mutableMapOf<Char, Int>()
    apps.forEachIndexed { index, app ->
      val cleanLabel = app.label.trim().trim('"', '\'', '(', '[', '{')
      val firstChar = cleanLabel.firstOrNull()?.uppercaseChar()
      if (firstChar != null && firstChar in 'A'..'Z') {
        if (!map.containsKey(firstChar)) {
          map[firstChar] = index
        }
      }
    }
    return map
  }

  private fun findLetterAtFraction(fraction: Float, alphabet: List<Char>): Char {
    val clamped = fraction.coerceIn(0f, 0.999f)
    val index = (clamped * alphabet.size).toInt().coerceIn(0, alphabet.lastIndex)
    return alphabet[index]
  }

  @Test
  fun testAlphabetIndexMapsToFirstAppIndex() {
    val apps = listOf(
      createApp("Alarm Clock", "com.android.deskclock"),
      createApp("Amazon Shopping", "com.amazon.mShop"),
      createApp("Calendar", "com.google.android.calendar"),
      createApp("Camera", "com.android.camera2"),
      createApp("Chrome", "com.android.chrome"),
      createApp("Files", "com.google.android.documentsui"),
      createApp("YouTube", "com.google.android.youtube")
    )

    val map = buildLetterIndexMap(apps)

    // 'A' points to index 0 ("Alarm Clock")
    assertEquals(0, map['A'])
    // 'C' points to index 2 ("Calendar"), not index 3 ("Camera")
    assertEquals(2, map['C'])
    // 'F' points to index 5 ("Files")
    assertEquals(5, map['F'])
    // 'Y' points to index 6 ("YouTube")
    assertEquals(6, map['Y'])

    // 'B', 'D', 'E', 'Z' have no matching apps
    assertNull(map['B'])
    assertNull(map['D'])
    assertNull(map['E'])
    assertNull(map['Z'])
  }

  @Test
  fun testActiveAndDisabledLetters() {
    val apps = listOf(
      createApp("Calculator", "com.google.android.calculator"),
      createApp("Settings", "com.android.settings")
    )

    val map = buildLetterIndexMap(apps)
    val activeLetters = map.keys

    assertTrue("C should be an active letter", activeLetters.contains('C'))
    assertTrue("S should be an active letter", activeLetters.contains('S'))
    assertFalse("A should be disabled/inactive", activeLetters.contains('A'))
    assertFalse("B should be disabled/inactive", activeLetters.contains('B'))
    assertFalse("Z should be disabled/inactive", activeLetters.contains('Z'))
  }

  @Test
  fun testCaseInsensitiveAndPunctuationTrimming() {
    val apps = listOf(
      createApp("  \"authenticator\"", "com.google.android.apps.authenticator2"),
      createApp("(Beta) App", "com.beta.app"),
      createApp("gmail", "com.google.android.gm")
    )

    val map = buildLetterIndexMap(apps)

    assertEquals("Should map lowercase or quoted 'authenticator' to A", 0, map['A'])
    assertEquals("Should map '(Beta)' to B after trimming leading parenthesis", 1, map['B'])
    assertEquals("Should map lowercase 'gmail' to G", 2, map['G'])
  }

  @Test
  fun testContinuousDragMapsFractionToLetters() {
    val alphabet = ('A'..'Z').toList()

    assertEquals('A', findLetterAtFraction(0.0f, alphabet))
    assertEquals('A', findLetterAtFraction(0.02f, alphabet))
    assertEquals('B', findLetterAtFraction(0.05f, alphabet))
    assertEquals('M', findLetterAtFraction(0.48f, alphabet))
    assertEquals('Z', findLetterAtFraction(0.98f, alphabet))
    assertEquals('Z', findLetterAtFraction(1.0f, alphabet))
  }

  @Test
  fun testLayer2VerticalModeCondition() {
    val verticalSpace = Space(
      id = "s1",
      name = "Work Space",
      layer2DisplayMode = Space.DISPLAY_MODE_SCROLL
    )

    val horizontalSpace = Space(
      id = "s2",
      name = "Personal Space",
      layer2DisplayMode = Space.DISPLAY_MODE_PAGE
    )

    val isVertical1 = verticalSpace.layer2DisplayMode != Space.DISPLAY_MODE_PAGE
    val isVertical2 = horizontalSpace.layer2DisplayMode != Space.DISPLAY_MODE_PAGE

    assertTrue("DISPLAY_MODE_SCROLL must be identified as vertical mode", isVertical1)
    assertFalse("DISPLAY_MODE_PAGE must NOT be identified as vertical mode", isVertical2)
  }

  @Test
  fun testSearchFilteringPreservesOrderAndUpdatesLetters() {
    val apps = listOf(
      createApp("Chrome", "com.android.chrome"),
      createApp("Clock", "com.android.deskclock"),
      createApp("Contacts", "com.google.android.contacts"),
      createApp("Gmail", "com.google.android.gm"),
      createApp("Google Maps", "com.google.android.apps.maps")
    )

    // Unfiltered
    val allMap = buildLetterIndexMap(apps)
    assertEquals(2, allMap.keys.size) // 'C' and 'G'
    assertEquals(0, allMap['C'])
    assertEquals(3, allMap['G'])

    // Filtered by query "maps"
    val query = "maps"
    val filtered = apps.filter { it.label.lowercase().contains(query) }
    val filteredMap = buildLetterIndexMap(filtered)

    assertEquals(1, filtered.size)
    assertEquals("Google Maps", filtered[0].label)
    assertFalse("C should no longer be active when query filters out C apps", filteredMap.containsKey('C'))
    assertTrue("G should be active for Google Maps", filteredMap.containsKey('G'))
    assertEquals(0, filteredMap['G'])
  }
}
