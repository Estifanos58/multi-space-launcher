package com.multispace.domain.model

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
