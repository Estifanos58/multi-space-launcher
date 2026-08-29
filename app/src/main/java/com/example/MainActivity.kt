package com.example

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.diagnostics.AppLogger
import com.example.platform.HomePlatformManager
import com.example.presentation.*
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

  private val discoveryViewModel: AppDiscoveryViewModel by viewModels()
  private val spaceViewModel: SpaceViewModel by viewModels()
  private val eventLogs = mutableStateListOf<String>()

  // Reactive state for default launcher status
  private val isDefaultHomeState = mutableStateOf(false)

  // Distinct modes:
  // "config": Dedicated Configuration / Space Management screen (shown when opening the app)
  // "home": Primary Multi-Space Launcher surface
  // "diagnostics": App catalog & telemetry view
  private var activeScreenState = mutableStateOf("config")

  private val requestHomeRoleLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { _ ->
    updateDefaultHomeStatus()
  }

  private fun recordEvent(tag: String, message: String) {
    val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    eventLogs.add("[$time] $tag: $message")
  }

  private fun updateDefaultHomeStatus() {
    val isDefault = HomePlatformManager.checkHomeStatus(this) == HomePlatformManager.HomeRoleState.DEFAULT_HOME
    isDefaultHomeState.value = isDefault
    AppLogger.d(AppLogger.Category.LAUNCHER, "Default Home status updated: $isDefault")
  }

  fun requestSetDefaultHome() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
          val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
          requestHomeRoleLauncher.launch(intent)
          return
        }
      }
      // Fallback to system Home settings
      HomePlatformManager.openDefaultHomeSettings(this)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to launch default Home request intent", e)
      HomePlatformManager.openDefaultHomeSettings(this)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppLogger.i(AppLogger.Category.LIFECYCLE, "MainActivity onCreate: Initializing Launcher")
    recordEvent("I/Lifecycle", "MainActivity onCreate (singleTask)")

    updateDefaultHomeStatus()

    // Distinguish between Android HOME intent vs opening the app normally:
    val isHomeIntent = intent?.hasCategory(Intent.CATEGORY_HOME) == true ||
      (intent?.action == Intent.ACTION_MAIN && intent?.categories?.contains(Intent.CATEGORY_HOME) == true)

    // When clicked/launched as an application, show the configuration page
    // When invoked via system Home button/intent while default launcher, show home surface
    activeScreenState.value = if (isHomeIntent && isDefaultHomeState.value) "home" else "config"

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        androidx.compose.material3.Surface(
          modifier = Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          val currentScreen by activeScreenState
          val isDefaultHome by isDefaultHomeState

          if (currentScreen == "diagnostics") {
            androidx.activity.compose.BackHandler {
              activeScreenState.value = "config"
            }
          }

          when (currentScreen) {
            "home" -> {
              LauncherHomeScreen(
                discoveryViewModel = discoveryViewModel,
                spaceViewModel = spaceViewModel,
                onLaunchApp = { app ->
                  discoveryViewModel.launchApp(app)
                },
                onOpenConfiguration = {
                  activeScreenState.value = "config"
                },
                modifier = Modifier.fillMaxSize()
              )
            }
            "config" -> {
              LauncherConfigurationScreen(
                spaceViewModel = spaceViewModel,
                discoveryViewModel = discoveryViewModel,
                isDefaultHome = isDefaultHome,
                onRequestSetDefaultHome = { requestSetDefaultHome() },
                onOpenDiagnostics = {
                  activeScreenState.value = "diagnostics"
                },
                onOpenHomeSurface = {
                  activeScreenState.value = "home"
                },
                modifier = Modifier.fillMaxSize()
              )
            }
            "diagnostics" -> {
              AppCatalogScreen(
                viewModel = discoveryViewModel,
                onNavigateDiagnostics = {
                  // Diagnostics telemetry
                },
                onNavigateHome = {
                  activeScreenState.value = "home"
                },
                modifier = Modifier.fillMaxSize()
              )
            }
            else -> {
              LauncherDiagnosticsScreen(
                eventLogs = eventLogs,
                onTriggerCheck = {
                  updateDefaultHomeStatus()
                  recordEvent("D/Launcher", "Manual Role Status Check: isDefault=${isDefaultHomeState.value}")
                },
                modifier = Modifier.fillMaxSize()
              )
            }
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    updateDefaultHomeStatus()

    val isHomeIntent = intent.hasCategory(Intent.CATEGORY_HOME) == true ||
      (intent.action == Intent.ACTION_MAIN && intent.categories?.contains(Intent.CATEGORY_HOME) == true)

    AppLogger.i(AppLogger.Category.LAUNCHER, "MainActivity onNewIntent: isHomeIntent=$isHomeIntent, isDefault=${isDefaultHomeState.value}")
    recordEvent("I/Launcher", "onNewIntent: isHomeIntent=$isHomeIntent, isDefault=${isDefaultHomeState.value}")

    if (isHomeIntent && isDefaultHomeState.value) {
      activeScreenState.value = "home"
    } else if (!isHomeIntent) {
      // User tapped the app icon in launcher / app drawer
      activeScreenState.value = "config"
    }
  }

  override fun onStart() {
    super.onStart()
    updateDefaultHomeStatus()
    spaceViewModel.ensureDefaultSpaceInitialized()
    discoveryViewModel.loadApps(isSilent = true)
    AppLogger.d(AppLogger.Category.LIFECYCLE, "MainActivity onStart")
    recordEvent("D/Lifecycle", "MainActivity onStart")
  }

  override fun onResume() {
    super.onResume()
    updateDefaultHomeStatus()
    discoveryViewModel.loadApps(isSilent = true)
    AppLogger.d(AppLogger.Category.LIFECYCLE, "MainActivity onResume (isDefault=${isDefaultHomeState.value})")
    recordEvent("D/Lifecycle", "MainActivity onResume (isDefault=${isDefaultHomeState.value})")
  }

  override fun onPause() {
    super.onPause()
    AppLogger.d(AppLogger.Category.LIFECYCLE, "MainActivity onPause")
    recordEvent("D/Lifecycle", "MainActivity onPause (navigating away)")
  }

  override fun onStop() {
    super.onStop()
    AppLogger.d(AppLogger.Category.LIFECYCLE, "MainActivity onStop")
    recordEvent("D/Lifecycle", "MainActivity onStop")
  }

  override fun onDestroy() {
    super.onDestroy()
    AppLogger.d(AppLogger.Category.LIFECYCLE, "MainActivity onDestroy")
    recordEvent("D/Lifecycle", "MainActivity onDestroy")
  }
}
