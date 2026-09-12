package com.multispace.diagnostics

import android.util.Log

/**
 * Diagnostics and logging utility for Multi-Space Launcher.
 * Aligned with the architectural logging categories defined in the Technical Architecture Blueprint.
 */
object AppLogger {
  private const val TAG_PREFIX = "MSLauncher"

  enum class Category(val tag: String) {
    LAUNCHER("LAUNCHER"),
    DISCOVERY("DISCOVERY"),
    LAUNCH("LAUNCH"),
    SPACE("SPACE"),
    PERSISTENCE("PERSISTENCE"),
    AUTH("AUTH"),
    LIFECYCLE("LIFECYCLE"),
    CUSTOMIZATION("CUSTOMIZATION"),
    RECOVERY("RECOVERY"),
    DIAGNOSTICS("DIAGNOSTICS")
  }

  fun d(category: Category, message: String) {
    try {
      Log.d("$TAG_PREFIX:${category.tag}", message)
    } catch (_: Throwable) {
      // Safe fallback in unit tests
    }
  }

  fun i(category: Category, message: String) {
    try {
      Log.i("$TAG_PREFIX:${category.tag}", message)
    } catch (_: Throwable) {
      // Safe fallback in unit tests
    }
  }

  fun w(category: Category, message: String, throwable: Throwable? = null) {
    try {
      if (throwable != null) {
        Log.w("$TAG_PREFIX:${category.tag}", message, throwable)
      } else {
        Log.w("$TAG_PREFIX:${category.tag}", message)
      }
    } catch (_: Throwable) {
      // Safe fallback in unit tests
    }
  }

  fun e(category: Category, message: String, throwable: Throwable? = null) {
    try {
      if (throwable != null) {
        Log.e("$TAG_PREFIX:${category.tag}", message, throwable)
      } else {
        Log.e("$TAG_PREFIX:${category.tag}", message)
      }
    } catch (_: Throwable) {
      // Safe fallback in unit tests
    }
  }
}
