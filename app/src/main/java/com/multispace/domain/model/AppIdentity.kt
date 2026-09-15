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
      AppIdentity(info.componentName.packageName, info.componentName.className, userHandle.hashCode().toLong())
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
 */
fun Collection<DiscoveredApp>.findMatching(identity: AppIdentity?): DiscoveredApp? {
  if (identity == null) return null
  for (app in this) {
    if (identity.matches(app.appIdentity)) return app
  }
  for (app in this) {
    if (identity.matchesPackage(app.packageName) && identity.userHandleId == app.userHandleId) return app
  }
  for (app in this) {
    if (identity.matchesPackage(app.packageName)) return app
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
 * Supports exact activity matching as well as fallback to package matching.
 */
class AppIdentityLookup(val apps: Collection<DiscoveredApp>) {
  private val byStrict: Map<AppIdentity, DiscoveredApp> = apps.associateBy { it.appIdentity }
  private val byComponent: Map<String, DiscoveredApp> = apps.associateBy { "${it.packageName}/${it.activityName}" }
  private val byPackageAndUser: Map<Pair<String, Long>, DiscoveredApp> = apps.associateBy { it.packageName to it.userHandleId }
  private val byPackage: Map<String, DiscoveredApp> = apps.associateBy { it.packageName }
  private val allList = apps.toList()

  operator fun get(identity: AppIdentity?): DiscoveredApp? {
    if (identity == null) return null
    byStrict[identity]?.let { return it }
    if (identity.componentName.isNotEmpty()) {
      byComponent["${identity.packageName}/${identity.componentName}"]?.let { return it }
    }
    byPackageAndUser[identity.packageName to identity.userHandleId]?.let { return it }
    byPackage[identity.packageName]?.let { return it }
    return allList.findMatching(identity)
  }

  operator fun get(placement: SpaceItemPlacement?): DiscoveredApp? = get(placement?.appIdentity)
  operator fun get(dockItem: SpaceDockItem?): DiscoveredApp? = get(dockItem?.appIdentity)
  operator fun get(folderItem: SpaceFolderItem?): DiscoveredApp? = get(folderItem?.appIdentity)
  operator fun get(membership: SpaceMembership?): DiscoveredApp? = get(membership?.appIdentity)
  operator fun get(key: String): DiscoveredApp? = byComponent[key] ?: byPackage[key]
}
