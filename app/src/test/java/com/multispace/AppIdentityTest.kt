package com.multispace

import com.multispace.domain.model.AppIdentity
import com.multispace.domain.model.AppIdentityLookup
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.appIdentity
import com.multispace.domain.model.findMatching
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppIdentityTest {

  @Test
  fun testIdenticalPackageDifferentUserHandleIdAreDistinct() {
    val personalApp = AppIdentity(packageName = "com.work.slack", componentName = ".MainActivity", userHandleId = 0L)
    val workApp = AppIdentity(packageName = "com.work.slack", componentName = ".MainActivity", userHandleId = 10L)

    assertFalse("Personal and work profile instances must not be equal", personalApp == workApp)
    assertFalse("Personal and work profile instances must not match", personalApp.matches(workApp))
    assertTrue("Both should share package name", personalApp.matchesPackage(workApp.packageName))
  }

  @Test
  fun testIdenticalPackageDifferentComponentNameAreDistinct() {
    val dialerApp = AppIdentity(packageName = "com.google.android.dialer", componentName = ".DialtactsActivity", userHandleId = 0L)
    val contactsApp = AppIdentity(packageName = "com.google.android.dialer", componentName = ".ContactsActivity", userHandleId = 0L)

    assertFalse("Different components in same package must not be equal", dialerApp == contactsApp)
    assertFalse("Different components must not match when both specify componentName", dialerApp.matches(contactsApp))
    assertTrue("Both should share package name", dialerApp.matchesPackage(contactsApp.packageName))
  }

  @Test
  fun testAppIdentityLookupStrictMatch() {
    val app1 = DiscoveredApp(
      id = "com.google.android.chrome/.Main#0",
      packageName = "com.google.android.chrome",
      activityName = ".Main",
      label = "Chrome",
      userHandleId = 0L
    )
    val app2 = DiscoveredApp(
      id = "com.work.slack/.Main#10",
      packageName = "com.work.slack",
      activityName = ".Main",
      label = "Slack (Work)",
      userHandleId = 10L
    )

    val lookup = AppIdentityLookup(listOf(app1, app2))

    val found1 = lookup[AppIdentity("com.google.android.chrome", ".Main", 0L)]
    assertNotNull(found1)
    assertEquals(app1.id, found1?.id)

    val found2 = lookup[AppIdentity("com.work.slack", ".Main", 10L)]
    assertNotNull(found2)
    assertEquals(app2.id, found2?.id)
  }

  @Test
  fun testAppIdentityLookupEmptyComponentFallbackWithinSameProfile() {
    val app = DiscoveredApp(
      id = "com.spotify.music/.MainActivity#0",
      packageName = "com.spotify.music",
      activityName = ".MainActivity",
      label = "Spotify",
      userHandleId = 0L
    )

    val lookup = AppIdentityLookup(listOf(app))

    // Query with empty componentName within profile 0
    val queryIdentity = AppIdentity(packageName = "com.spotify.music", componentName = "", userHandleId = 0L)
    val found = lookup[queryIdentity]

    assertNotNull("Should resolve app when component is empty within same profile", found)
    assertEquals(app.id, found?.id)
  }

  @Test
  fun testAppIdentityLookupEnsuresCrossProfileLookupNeverHappens() {
    val workOnlyApp = DiscoveredApp(
      id = "com.work.teams/.Main#10",
      packageName = "com.work.teams",
      activityName = ".Main",
      label = "Teams (Work)",
      userHandleId = 10L
    )

    val lookup = AppIdentityLookup(listOf(workOnlyApp))

    // Personal profile query for the same package
    val personalQuery = AppIdentity(packageName = "com.work.teams", componentName = ".Main", userHandleId = 0L)
    val foundPersonal = lookup[personalQuery]

    assertNull("Personal profile query must NEVER resolve work profile app", foundPersonal)

    // Even with empty component, cross-profile resolution must never happen
    val emptyComponentPersonalQuery = AppIdentity(packageName = "com.work.teams", componentName = "", userHandleId = 0L)
    val foundEmptyPersonal = lookup[emptyComponentPersonalQuery]

    assertNull("Empty component personal query must NEVER cross profile boundary", foundEmptyPersonal)
  }

  @Test
  fun testAppIdentityLookupFindAnyInPackageExplicitFallback() {
    val personalApp = DiscoveredApp(
      id = "com.example.app/.Main#0",
      packageName = "com.example.app",
      activityName = ".Main",
      label = "App (Personal)",
      userHandleId = 0L
    )
    val workApp = DiscoveredApp(
      id = "com.example.app/.Main#10",
      packageName = "com.example.app",
      activityName = ".Main",
      label = "App (Work)",
      userHandleId = 10L
    )

    val lookup = AppIdentityLookup(listOf(personalApp, workApp))

    val anyPreferredWork = lookup.findAnyInPackage("com.example.app", preferredUserHandleId = 10L)
    assertEquals(workApp.id, anyPreferredWork?.id)

    val anyPreferredPersonal = lookup.findAnyInPackage("com.example.app", preferredUserHandleId = 0L)
    assertEquals(personalApp.id, anyPreferredPersonal?.id)

    val nonExistent = lookup.findAnyInPackage("com.nonexistent")
    assertNull(nonExistent)
  }

  @Test
  fun testCollectionFindMatchingDoesNotCrossProfiles() {
    val apps = listOf(
      DiscoveredApp(
        id = "com.work.crm/.Main#10",
        packageName = "com.work.crm",
        activityName = ".Main",
        label = "CRM (Work)",
        userHandleId = 10L
      )
    )

    val personalQuery = AppIdentity("com.work.crm", ".Main", userHandleId = 0L)
    val match = apps.findMatching(personalQuery)

    assertNull("findMatching must never cross profile boundary", match)
  }
}
