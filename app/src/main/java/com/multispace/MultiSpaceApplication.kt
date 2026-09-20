package com.multispace

import android.app.Application
import com.multispace.diagnostics.AppLogger

/**
 * Custom Application class for Multi-Space Launcher.
 * Hosts the application-scoped [AppContainer] to ensure unified services across activities.
 */
class MultiSpaceApplication : Application() {

  lateinit var container: AppContainer
    private set

  override fun onCreate() {
    super.onCreate()
    AppLogger.i(AppLogger.Category.LAUNCHER, "MultiSpaceApplication: Initializing shared application container")
    container = AppContainer(this)
  }
}
