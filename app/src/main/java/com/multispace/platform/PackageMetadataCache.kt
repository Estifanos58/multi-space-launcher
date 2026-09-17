package com.multispace.platform

import android.util.LruCache

/**
 * Cached package metadata to eliminate redundant PackageManager and DevicePolicyManager IPC queries.
 */
data class PackageMetadata(
  val versionName: String,
  val installTimeMillis: Long,
  val lastUpdateTimeMillis: Long,
  val isUninstallable: Boolean
)

class PackageMetadataCache(maxEntries: Int = 300) {

  private val cache = object : LruCache<String, PackageMetadata>(maxEntries) {}

  fun get(packageName: String, userHandleId: Long): PackageMetadata? {
    return cache.get(makeKey(packageName, userHandleId))
  }

  fun put(packageName: String, userHandleId: Long, metadata: PackageMetadata) {
    cache.put(makeKey(packageName, userHandleId), metadata)
  }

  fun evict(packageName: String, userHandleId: Long? = null) {
    if (userHandleId != null) {
      cache.remove(makeKey(packageName, userHandleId))
    } else {
      val prefix = "$packageName#"
      val keysToEvict = cache.snapshot().keys.filter { it.startsWith(prefix) }
      keysToEvict.forEach { cache.remove(it) }
    }
  }

  fun clear() {
    cache.evictAll()
  }

  fun size(): Int = cache.size()

  private fun makeKey(packageName: String, userHandleId: Long): String = "$packageName#$userHandleId"
}
