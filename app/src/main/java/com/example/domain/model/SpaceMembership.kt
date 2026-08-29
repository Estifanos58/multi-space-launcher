package com.example.domain.model

/**
 * Represents an association between an application and a specific Space.
 *
 * @property spaceId Stable identifier of the target Space.
 * @property packageName Application package name.
 * @property componentName Explicit launch activity component name.
 * @property userHandleId Android UserProfile identifier.
 * @property orderIndex Sort order position within the Space.
 * @property addedAt Timestamp when membership was added.
 */
data class SpaceMembership(
  val spaceId: String,
  val packageName: String,
  val componentName: String,
  val userHandleId: Long = 0L,
  val orderIndex: Int = 0,
  val addedAt: Long = System.currentTimeMillis()
) {
  val compositeKey: String
    get() = "${spaceId}#${packageName}#${componentName}#${userHandleId}"
}
