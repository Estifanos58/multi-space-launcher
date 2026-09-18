package com.multispace.domain.model

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle
import com.multispace.data.entity.SpaceDockItemEntity
import com.multispace.data.entity.SpaceFolderItemEntity
import com.multispace.data.entity.SpaceItemPlacementEntity
import com.multispace.data.entity.SpaceMembershipEntity

/**
 * Common domain value object representing an application's unique identity
 * combining package name, component/activity name, and Android user handle ID.
 */
data class AppIdentity(
  val packageName: String,
  val componentName: String = "",
  val userHandleId: Long = 0L
) {

  /**
   * Compares equality with another [AppIdentity].
   * If either componentName is empty, matches on package and userHandleId.
   */
  fun matches(other: AppIdentity): Boolean {
    if (packageName != other.packageName) return false
    if (userHandleId != other.userHandleId) return false
    if (componentName.isNotEmpty() && other.componentName.isNotEmpty()) {
      return componentName == other.componentName
    }
    return true
  }

  /**
   * Matches whether this app identity belongs to [targetPackage].
   */
  fun matchesPackage(targetPackage: String): Boolean {
    return packageName == targetPackage
  }

  /**
   * Converts this identity to an Android [ComponentName].
   */
  fun toComponentName(): ComponentName = ComponentName(packageName, componentName)

  /**
   * Serializes this identity to a canonical string matching DiscoveredApp.id: "packageName/componentName#userHandleId".
   */
  fun toIdentifier(): String = "$packageName/$componentName#$userHandleId"

  companion object {
    fun fromDiscoveredApp(app: DiscoveredApp): AppIdentity =
      AppIdentity(app.packageName, app.activityName, app.userHandleId)

    fun fromMembership(membership: SpaceMembership): AppIdentity =
      AppIdentity(membership.packageName, membership.componentName, membership.userHandleId)

    fun fromPlacement(placement: SpaceItemPlacement): AppIdentity? =
      placement.packageName?.let { AppIdentity(it, placement.componentName ?: "", placement.userHandleId) }

    fun fromDockItem(dockItem: SpaceDockItem): AppIdentity =
      AppIdentity(dockItem.packageName, dockItem.componentName, dockItem.userHandleId)

    fun fromFolderItem(folderItem: SpaceFolderItem): AppIdentity =
      AppIdentity(folderItem.packageName, folderItem.componentName, folderItem.userHandleId)

    fun fromMembershipEntity(entity: SpaceMembershipEntity): AppIdentity =
      AppIdentity(entity.packageName, entity.componentName, entity.userHandleId)

    fun fromPlacementEntity(entity: SpaceItemPlacementEntity): AppIdentity? =
      entity.packageName?.let { AppIdentity(it, entity.componentName ?: "", entity.userHandleId) }

    fun fromDockItemEntity(entity: SpaceDockItemEntity): AppIdentity =
      AppIdentity(entity.packageName, entity.componentName, entity.userHandleId)

    fun fromFolderItemEntity(entity: SpaceFolderItemEntity): AppIdentity =
      AppIdentity(entity.packageName, entity.componentName, entity.userHandleId)

    fun fromLauncherActivityInfo(info: LauncherActivityInfo, userHandleId: Long): AppIdentity =
      AppIdentity(info.componentName.packageName, info.componentName.className, userHandleId)

    fun fromLauncherActivityInfo(info: LauncherActivityInfo, userHandle: UserHandle): AppIdentity =
      AppIdentity(
        info.componentName.packageName,
        info.componentName.className,
        com.multispace.platform.UserHandleHelper.getUserHandleId(null as android.content.Context?, userHandle)
      )

    /**
     * Parses an [AppIdentity] from a serialized string, placement ID, or virtual identifier.
     * Supports canonical and virtual formats:
     * - "virtual:pkg/comp#userHandleId"
     * - "virtual:pkg/comp/userHandleId"
     * - "virtual:pkg#userHandleId"
     * - "virtual_pkg_userHandleId"
     * - "pkg/comp#userHandleId" (canonical DiscoveredApp.id)
     * - "pkg/comp/userHandleId"
     * - "virtual:pkg"
     * - "pkg"
     */
    fun parseFromIdentifier(raw: String?): AppIdentity? {
      if (raw.isNullOrBlank()) return null
      val stripped = raw
        .removePrefix("virtual:")
        .removePrefix("fallback:")
        .removePrefix("virtual_")

      if (stripped.contains("#")) {
        val beforeHash = stripped.substringBefore("#")
        val userPart = stripped.substringAfter("#")
        val userId = userPart.toLongOrNull() ?: 0L
        return if (beforeHash.contains("/")) {
          val pkg = beforeHash.substringBefore("/")
          val comp = beforeHash.substringAfter("/")
          AppIdentity(packageName = pkg, componentName = comp, userHandleId = userId)
        } else {
          AppIdentity(packageName = beforeHash, componentName = "", userHandleId = userId)
        }
      }

      if (stripped.contains("/")) {
        val parts = stripped.split("/")
        if (parts.size >= 3) {
          val pkg = parts[0]
          val comp = parts[1]
          val userId = parts[2].toLongOrNull() ?: 0L
          return AppIdentity(packageName = pkg, componentName = comp, userHandleId = userId)
        } else if (parts.size == 2) {
          val pkg = parts[0]
          val secondPart = parts[1]
          val possibleUserId = secondPart.toLongOrNull()
          return if (possibleUserId != null) {
            AppIdentity(packageName = pkg, componentName = "", userHandleId = possibleUserId)
          } else {
            AppIdentity(packageName = pkg, componentName = secondPart, userHandleId = 0L)
          }
        }
      }

      if (raw.startsWith("virtual_")) {
        val lastUnderscore = stripped.lastIndexOf('_')
        if (lastUnderscore != -1) {
          val suffix = stripped.substring(lastUnderscore + 1)
          val userId = suffix.toLongOrNull()
          if (userId != null) {
            val pkg = stripped.substring(0, lastUnderscore)
            return AppIdentity(packageName = pkg, componentName = "", userHandleId = userId)
          }
        }
      }

      if (stripped.isNotEmpty()) {
        return AppIdentity(packageName = stripped, componentName = "", userHandleId = 0L)
      }
      return null
    }
  }
}

val DiscoveredApp.appIdentity: AppIdentity
  get() = AppIdentity.fromDiscoveredApp(this)

val SpaceMembership.appIdentity: AppIdentity
  get() = AppIdentity.fromMembership(this)

val SpaceItemPlacement.appIdentity: AppIdentity?
  get() = AppIdentity.fromPlacement(this)

val SpaceDockItem.appIdentity: AppIdentity
  get() = AppIdentity.fromDockItem(this)

val SpaceFolderItem.appIdentity: AppIdentity
  get() = AppIdentity.fromFolderItem(this)

val SpaceMembershipEntity.appIdentity: AppIdentity
  get() = AppIdentity.fromMembershipEntity(this)

val SpaceItemPlacementEntity.appIdentity: AppIdentity?
  get() = AppIdentity.fromPlacementEntity(this)

val SpaceDockItemEntity.appIdentity: AppIdentity
  get() = AppIdentity.fromDockItemEntity(this)

val SpaceFolderItemEntity.appIdentity: AppIdentity
  get() = AppIdentity.fromFolderItemEntity(this)

/**
 * Extension on collection of DiscoveredApp to find a match for a given [AppIdentity].
 * Guarantees cross-profile identity safety: never returns an app from a different user profile.
 */
fun Collection<DiscoveredApp>.findMatching(identity: AppIdentity?): DiscoveredApp? {
  if (identity == null) return null
  // 1. Strict exact match
  for (app in this) {
    if (identity.matches(app.appIdentity)) return app
  }
  // 2. Fallback strictly within the SAME user handle / profile
  for (app in this) {
    if (identity.matchesPackage(app.packageName) && identity.userHandleId == app.userHandleId) {
      return app
    }
  }
  return null
}

fun Collection<DiscoveredApp>.findMatching(placement: SpaceItemPlacement): DiscoveredApp? =
  findMatching(placement.appIdentity)

fun Collection<DiscoveredApp>.findMatching(dockItem: SpaceDockItem): DiscoveredApp? =
  findMatching(dockItem.appIdentity)

fun Collection<DiscoveredApp>.findMatching(folderItem: SpaceFolderItem): DiscoveredApp? =
  findMatching(folderItem.appIdentity)

fun Collection<DiscoveredApp>.findMatching(membership: SpaceMembership): DiscoveredApp? =
  findMatching(membership.appIdentity)

/**
 * Fast lookup helper that resolves [DiscoveredApp] instances by their canonical [AppIdentity].
 *
 * Strict identity: (packageName, componentName, userHandleId) identifies a specific app instance.
 * Cross-profile lookups are strictly prohibited.
 */
class AppIdentityLookup(val apps: Collection<DiscoveredApp>) {
  // 1. Strict exact identity map: AppIdentity -> DiscoveredApp
  private val byStrict: Map<AppIdentity, DiscoveredApp> = apps.associateBy { it.appIdentity }

  // 2. Candidates grouped by (packageName, userHandleId):
  private val byPackageAndUser: Map<Pair<String, Long>, List<DiscoveredApp>> =
    apps.groupBy { it.packageName to it.userHandleId }

  // 3. Candidates grouped by package only (for explicit package-level operations):
  private val byPackage: Map<String, List<DiscoveredApp>> =
    apps.groupBy { it.packageName }

  /**
   * Resolves a [DiscoveredApp] by strict [AppIdentity].
   * - 1. Exact match on (packageName, componentName, userHandleId).
   * - 2. If componentName is empty or component changed, resolves within the SAME (packageName, userHandleId) profile.
   * - NEVER falls back across user handles or profile boundaries.
   */
  operator fun get(identity: AppIdentity?): DiscoveredApp? {
    if (identity == null) return null
    byStrict[identity]?.let { return it }

    val profileCandidates = byPackageAndUser[identity.packageName to identity.userHandleId]
    if (!profileCandidates.isNullOrEmpty()) {
      if (identity.componentName.isNotEmpty()) {
        val matchingComponent = profileCandidates.firstOrNull { it.activityName == identity.componentName }
        if (matchingComponent != null) return matchingComponent
      }
      return profileCandidates.first()
    }
    return null
  }

  /**
   * Explicit package-level fallback when domain logic specifically requests any matching app from this package.
   * If [preferredUserHandleId] is provided, prefers candidate from that profile.
   * Never called silently by [get].
   */
  fun findAnyInPackage(packageName: String, preferredUserHandleId: Long? = null): DiscoveredApp? {
    val candidates = byPackage[packageName] ?: return null
    if (preferredUserHandleId != null) {
      val inProfile = candidates.firstOrNull { it.userHandleId == preferredUserHandleId }
      if (inProfile != null) return inProfile
    }
    return candidates.firstOrNull()
  }

  operator fun get(placement: SpaceItemPlacement?): DiscoveredApp? = get(placement?.appIdentity)
  operator fun get(dockItem: SpaceDockItem?): DiscoveredApp? = get(dockItem?.appIdentity)
  operator fun get(folderItem: SpaceFolderItem?): DiscoveredApp? = get(folderItem?.appIdentity)
  operator fun get(membership: SpaceMembership?): DiscoveredApp? = get(membership?.appIdentity)
}
