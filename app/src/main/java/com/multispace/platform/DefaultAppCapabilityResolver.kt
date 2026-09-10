package com.multispace.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.DiscoveredApp

/**
 * Resolves default application assignments for newly created Spaces and dynamic DockBar expansions.
 *
 * Utilizes Android Intent queries and semantic capability detection to find installed apps
 * without hardcoding OEM package names.
 */
object DefaultAppCapabilityResolver {

  enum class AppCapabilityType(val displayName: String) {
    BROWSER("Browser"),
    CAMERA("Camera"),
    PHONE("Phone"),
    MESSAGES("Messages"),
    CONTACTS("Contacts"),
    GALLERY("Gallery"),
    FILES("Files"),
    CLOCK("Clock"),
    CALCULATOR("Calculator"),
    CALENDAR("Calendar"),
    MAPS("Maps"),
    SETTINGS("Settings"),
    EMAIL("Email"),
    NOTES("Notes"),
    MEDIA("Media")
  }

  enum class SpaceCategory {
    PERSONAL,
    WORK,
    STUDY,
    DEFAULT
  }

  // Priority order for the 4 core dock apps on initial creation
  val CORE_DOCK_CAPABILITIES: List<AppCapabilityType> = listOf(
    AppCapabilityType.BROWSER,
    AppCapabilityType.CAMERA,
    AppCapabilityType.PHONE,
    AppCapabilityType.MESSAGES
  )

  // Everyday apps to fill newly available positions when DockBar capacity > 4
  val EXPANDED_DOCK_CAPABILITIES: List<AppCapabilityType> = listOf(
    AppCapabilityType.CONTACTS,
    AppCapabilityType.GALLERY,
    AppCapabilityType.FILES,
    AppCapabilityType.CLOCK,
    AppCapabilityType.CALCULATOR,
    AppCapabilityType.CALENDAR,
    AppCapabilityType.MAPS
  )

  // Curated subsets for Space types on Layer 1 Home Screen (8-12 apps)
  val PERSONAL_LAYER1_SUBSET: List<AppCapabilityType> = listOf(
    AppCapabilityType.CAMERA,
    AppCapabilityType.GALLERY,
    AppCapabilityType.MEDIA,
    AppCapabilityType.PHONE,
    AppCapabilityType.MESSAGES,
    AppCapabilityType.BROWSER,
    AppCapabilityType.CONTACTS,
    AppCapabilityType.CLOCK,
    AppCapabilityType.MAPS,
    AppCapabilityType.CALENDAR,
    AppCapabilityType.NOTES,
    AppCapabilityType.SETTINGS
  )

  val WORK_LAYER1_SUBSET: List<AppCapabilityType> = listOf(
    AppCapabilityType.EMAIL,
    AppCapabilityType.CALENDAR,
    AppCapabilityType.FILES,
    AppCapabilityType.NOTES,
    AppCapabilityType.CALCULATOR,
    AppCapabilityType.BROWSER,
    AppCapabilityType.CONTACTS,
    AppCapabilityType.MESSAGES,
    AppCapabilityType.PHONE,
    AppCapabilityType.CLOCK,
    AppCapabilityType.SETTINGS,
    AppCapabilityType.MAPS
  )

  val STUDY_LAYER1_SUBSET: List<AppCapabilityType> = listOf(
    AppCapabilityType.NOTES,
    AppCapabilityType.CALCULATOR,
    AppCapabilityType.CALENDAR,
    AppCapabilityType.FILES,
    AppCapabilityType.BROWSER,
    AppCapabilityType.CLOCK,
    AppCapabilityType.EMAIL,
    AppCapabilityType.CONTACTS,
    AppCapabilityType.MESSAGES,
    AppCapabilityType.SETTINGS,
    AppCapabilityType.MAPS,
    AppCapabilityType.CAMERA
  )

  val DEFAULT_LAYER1_SUBSET: List<AppCapabilityType> = listOf(
    AppCapabilityType.BROWSER,
    AppCapabilityType.CAMERA,
    AppCapabilityType.PHONE,
    AppCapabilityType.MESSAGES,
    AppCapabilityType.CONTACTS,
    AppCapabilityType.GALLERY,
    AppCapabilityType.FILES,
    AppCapabilityType.CLOCK,
    AppCapabilityType.CALCULATOR,
    AppCapabilityType.CALENDAR,
    AppCapabilityType.MAPS,
    AppCapabilityType.SETTINGS
  )

  /**
   * Resolves the Space category from its user-facing name and/or layout preset.
   */
  fun resolveSpaceCategory(name: String, preset: String = ""): SpaceCategory {
    val lowerName = name.lowercase().trim()
    val lowerPreset = preset.lowercase().trim()
    return when {
      lowerName.contains("work") || lowerPreset.contains("productivity") -> SpaceCategory.WORK
      lowerName.contains("study") || lowerName.contains("school") || lowerName.contains("learn") || lowerName.contains("focus") -> SpaceCategory.STUDY
      lowerName.contains("personal") || lowerName.contains("life") || lowerName.contains("home") -> SpaceCategory.PERSONAL
      else -> SpaceCategory.DEFAULT
    }
  }

  /**
   * Returns intent templates representing a capability.
   */
  private fun getIntentTemplatesForCapability(capability: AppCapabilityType): List<Intent> {
    return when (capability) {
      AppCapabilityType.BROWSER -> listOf(
        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
          addCategory(Intent.CATEGORY_BROWSABLE)
        },
        Intent(Intent.ACTION_WEB_SEARCH)
      )
      AppCapabilityType.CAMERA -> listOf(
        Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
      )
      AppCapabilityType.PHONE -> listOf(
        Intent(Intent.ACTION_DIAL, Uri.parse("tel:12345")),
        Intent(Intent.ACTION_DIAL)
      )
      AppCapabilityType.MESSAGES -> listOf(
        Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:12345")),
        Intent(Intent.ACTION_MAIN).apply {
          addCategory("android.intent.category.APP_MESSAGING")
        }
      )
      AppCapabilityType.CONTACTS -> listOf(
        Intent(Intent.ACTION_MAIN).apply {
          addCategory("android.intent.category.APP_CONTACTS")
        },
        Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI),
        Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
      )
      AppCapabilityType.GALLERY -> listOf(
        Intent(Intent.ACTION_MAIN).apply {
          addCategory("android.intent.category.APP_GALLERY")
        },
        Intent(Intent.ACTION_VIEW).apply {
          type = "image/*"
        }
      )
      AppCapabilityType.FILES -> listOf(
        Intent(Intent.ACTION_MAIN).apply {
          addCategory("android.intent.category.APP_FILES")
        },
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
          type = "*/*"
        },
        Intent(Intent.ACTION_GET_CONTENT).apply {
          type = "*/*"
        }
      )
      AppCapabilityType.CLOCK -> listOf(
        Intent(AlarmClock.ACTION_SHOW_ALARMS),
        Intent(Intent.ACTION_MAIN).apply {
          addCategory("android.intent.category.APP_CLOCK")
        }
      )
      AppCapabilityType.CALCULATOR -> listOf(
        Intent(Intent.ACTION_MAIN).apply {
          addCategory("android.intent.category.APP_CALCULATOR")
        }
      )
      AppCapabilityType.CALENDAR -> listOf(
        Intent(Intent.ACTION_MAIN).apply {
          addCategory("android.intent.category.APP_CALENDAR")
        },
        Intent(Intent.ACTION_VIEW, Uri.parse("content://com.android.calendar/time"))
      )
      AppCapabilityType.MAPS -> listOf(
        Intent(Intent.ACTION_MAIN).apply {
          addCategory("android.intent.category.APP_MAPS")
        },
        Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
      )
      AppCapabilityType.SETTINGS -> listOf(
        Intent(Settings.ACTION_SETTINGS)
      )
      AppCapabilityType.EMAIL -> listOf(
        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:test@example.com")),
        Intent(Intent.ACTION_MAIN).apply {
          addCategory("android.intent.category.APP_EMAIL")
        }
      )
      AppCapabilityType.NOTES -> listOf(
        Intent("android.intent.action.CREATE_NOTE"),
        Intent(Intent.ACTION_MAIN)
      )
      AppCapabilityType.MEDIA -> listOf(
        Intent(Intent.ACTION_MAIN).apply {
          addCategory("android.intent.category.APP_MUSIC")
        },
        Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
      )
    }
  }

  /**
   * Semantic keywords for fallback capability scoring when Intent resolution is inconclusive or offline.
   */
  private fun getKeywordsForCapability(capability: AppCapabilityType): List<String> {
    return when (capability) {
      AppCapabilityType.BROWSER -> listOf("browser", "chrome", "firefox", "opera", "edge", "safari", "web", "internet", "duckduckgo", "brave", "kiwi", "vivaldi")
      AppCapabilityType.CAMERA -> listOf("camera", "lens", "cam", "photo capture")
      AppCapabilityType.PHONE -> listOf("phone", "dialer", "call", "dialtacts")
      AppCapabilityType.MESSAGES -> listOf("message", "messaging", "sms", "mms", "chat", "conversation")
      AppCapabilityType.CONTACTS -> listOf("contact", "people", "addressbook")
      AppCapabilityType.GALLERY -> listOf("gallery", "photos", "photo", "pictures", "image")
      AppCapabilityType.FILES -> listOf("files", "documents", "filemanager", "file manager", "explorer", "nbu.files", "my files", "downloads")
      AppCapabilityType.CLOCK -> listOf("clock", "alarm", "deskclock", "timer", "stopwatch")
      AppCapabilityType.CALCULATOR -> listOf("calculator", "calc")
      AppCapabilityType.CALENDAR -> listOf("calendar", "agenda", "schedule")
      AppCapabilityType.MAPS -> listOf("maps", "map", "navigation", "navigator")
      AppCapabilityType.SETTINGS -> listOf("settings", "setting", "preferences", "config")
      AppCapabilityType.EMAIL -> listOf("mail", "email", "gmail", "outlook", "inbox", "exchange")
      AppCapabilityType.NOTES -> listOf("note", "notes", "keep", "memo", "notepad", "onenote", "evernote", "jots")
      AppCapabilityType.MEDIA -> listOf("music", "spotify", "media", "player", "audio", "sound", "podcast", "youtube music", "tracks")
    }
  }

  /**
   * Resolves the best matching installed app for a capability.
   */
  fun resolveAppForCapability(
    capability: AppCapabilityType,
    installedApps: List<DiscoveredApp>,
    context: Context? = null,
    excludedPackageNames: Set<String> = emptySet()
  ): DiscoveredApp? {
    val availableApps = installedApps.filterNot { excludedPackageNames.contains(it.packageName) }
    if (availableApps.isEmpty()) return null

    // 1. Android Intent Query Resolution (if Context is available)
    if (context != null) {
      try {
        val packageManager = context.packageManager
        val intents = getIntentTemplatesForCapability(capability)
        for (intent in intents) {
          val resolvedActivities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            .ifEmpty { packageManager.queryIntentActivities(intent, 0) }

          for (resolveInfo in resolvedActivities) {
            val pkg = resolveInfo.activityInfo?.packageName ?: continue
            val matched = availableApps.firstOrNull { it.packageName == pkg }
            if (matched != null) {
              AppLogger.d(AppLogger.Category.LAUNCHER, "Resolved ${capability.displayName} via Intent to ${matched.label} (${matched.packageName})")
              return matched
            }
          }
        }
      } catch (e: Exception) {
        AppLogger.w(AppLogger.Category.LAUNCHER, "Intent resolution failed for ${capability.displayName}: ${e.message}")
      }
    }

    // 2. Semantic Heuristic Scoring
    val keywords = getKeywordsForCapability(capability)
    var bestMatch: DiscoveredApp? = null
    var bestScore = 0

    for (app in availableApps) {
      val lowerLabel = app.label.lowercase()
      val lowerPkg = app.packageName.lowercase()
      val lowerAct = app.activityName.lowercase()

      var score = 0
      // Exact label match is highest priority (e.g. "Phone", "Calculator", "Camera")
      if (lowerLabel == capability.displayName.lowercase()) {
        score += 100
      }

      for (kw in keywords) {
        if (lowerLabel == kw) {
          score += 80
        } else if (lowerLabel.contains(kw)) {
          score += 40
        }
        if (lowerPkg.contains(kw)) {
          score += 25
        }
        if (lowerAct.contains(kw)) {
          score += 15
        }
      }

      // Bonus for system apps to avoid ad-supported fake clones taking priority
      if (app.isSystemApp && score > 0) {
        score += 10
      }

      if (score > bestScore) {
        bestScore = score
        bestMatch = app
      }
    }

    if (bestMatch != null && bestScore >= 25) {
      AppLogger.d(AppLogger.Category.LAUNCHER, "Resolved ${capability.displayName} via Heuristics to ${bestMatch.label} (${bestMatch.packageName}) score=$bestScore")
      return bestMatch
    }

    return null
  }

  /**
   * Initializes DockBar apps for a new Space or expands DockBar when capacity is increased.
   *
   * Priority on initial creation:
   * 1. Browser
   * 2. Camera
   * 3. Phone
   * 4. Messages
   *
   * If capacity > 4 (or when capacity increases later):
   * Keeps the existing / core apps first, and fills newly available positions with everyday apps:
   * Contacts → Gallery/Photos → Files → Clock → Calculator → Calendar → Maps
   */
  fun resolveDockApps(
    installedApps: List<DiscoveredApp>,
    dockCapacity: Int,
    context: Context? = null,
    existingDockApps: List<DiscoveredApp> = emptyList()
  ): List<DiscoveredApp> {
    val targetCapacity = dockCapacity.coerceIn(1, 10)
    val result = mutableListOf<DiscoveredApp>()
    val usedPackages = mutableSetOf<String>()

    // If existing dock apps are supplied (e.g. capacity expansion), preserve them first
    for (app in existingDockApps) {
      if (result.size < targetCapacity && !usedPackages.contains(app.packageName)) {
        result.add(app)
        usedPackages.add(app.packageName)
      }
    }

    // If new Space creation (no existing dock apps), resolve the core 4 apps first
    if (existingDockApps.isEmpty()) {
      for (capability in CORE_DOCK_CAPABILITIES) {
        if (result.size >= targetCapacity) break
        val app = resolveAppForCapability(capability, installedApps, context, usedPackages)
        if (app != null) {
          result.add(app)
          usedPackages.add(app.packageName)
        }
      }
    }

    // If capacity > 4 (or positions still unfilled), fill with expanded everyday apps
    if (result.size < targetCapacity) {
      for (capability in EXPANDED_DOCK_CAPABILITIES) {
        if (result.size >= targetCapacity) break
        val app = resolveAppForCapability(capability, installedApps, context, usedPackages)
        if (app != null) {
          result.add(app)
          usedPackages.add(app.packageName)
        }
      }
    }

    // If still unfilled (e.g. device has few standard apps), fill with remaining available installed apps
    if (result.size < targetCapacity) {
      for (app in installedApps) {
        if (result.size >= targetCapacity) break
        if (!usedPackages.contains(app.packageName)) {
          result.add(app)
          usedPackages.add(app.packageName)
        }
      }
    }

    return result
  }

  /**
   * Resolves Layer 1 curated apps for a newly created Space based on its Space type.
   *
   * Curated subsets:
   * - Personal: Phone, Messages, Browser, Camera, Gallery, Contacts, Clock, Maps, Calendar, Settings
   * - Work: Browser, Calendar, Contacts, Messages, Phone, Files, Calculator, Clock, Settings, Maps
   * - Study: Browser, Files, Calculator, Calendar, Clock, Settings, Contacts, Messages, Camera, Maps
   * - Default: Browser, Camera, Phone, Messages, Contacts, Gallery, Files, Clock, Calculator, Calendar, Maps, Settings
   *
   * Gracefully skips unavailable apps, guaranteeing no duplicates and contiguous placement.
   */
  fun resolveLayer1Apps(
    installedApps: List<DiscoveredApp>,
    spaceName: String,
    layoutPreset: String = "",
    context: Context? = null
  ): List<DiscoveredApp> {
    val category = resolveSpaceCategory(spaceName, layoutPreset)
    val subset = when (category) {
      SpaceCategory.PERSONAL -> PERSONAL_LAYER1_SUBSET
      SpaceCategory.WORK -> WORK_LAYER1_SUBSET
      SpaceCategory.STUDY -> STUDY_LAYER1_SUBSET
      SpaceCategory.DEFAULT -> DEFAULT_LAYER1_SUBSET
    }

    val result = mutableListOf<DiscoveredApp>()
    val usedPackages = mutableSetOf<String>()

    for (capability in subset) {
      val app = resolveAppForCapability(capability, installedApps, context, usedPackages)
      if (app != null) {
        result.add(app)
        usedPackages.add(app.packageName)
      }
    }

    // If device had very few apps matching the curated list, ensure at least some installed apps populate Layer 1
    if (result.size < 4 && installedApps.isNotEmpty()) {
      for (app in installedApps) {
        if (result.size >= 12) break
        if (!usedPackages.contains(app.packageName)) {
          result.add(app)
          usedPackages.add(app.packageName)
        }
      }
    }

    return result
  }
}
