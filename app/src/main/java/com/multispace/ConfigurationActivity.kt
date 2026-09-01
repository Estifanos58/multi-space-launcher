package com.multispace

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
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.Space
import com.multispace.platform.HomePlatformManager
import com.multispace.presentation.*
import com.multispace.ui.theme.MyApplicationTheme

/**
 * Dedicated Configuration / Management Activity.
 * Launched when the user explicitly opens Multi-Space Launcher from their application drawer or an external launcher.
 * Runs in its own separate task affinity ("com.multispace.configuration") to prevent mixing with the Android HOME task.
 */
class ConfigurationActivity : ComponentActivity() {

  private val discoveryViewModel: AppDiscoveryViewModel by viewModels()
  private val spaceViewModel: SpaceViewModel by viewModels()
  private val eventLogs = mutableStateListOf<String>()

  private val isDefaultHomeState = mutableStateOf(false)
  private var currentConfigScreen = mutableStateOf("config")

  private val requestHomeRoleLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { _ ->
    updateDefaultHomeStatus()
  }

  private fun updateDefaultHomeStatus() {
    val isDefault = HomePlatformManager.checkHomeStatus(this) == HomePlatformManager.HomeRoleState.DEFAULT_HOME
    isDefaultHomeState.value = isDefault
    AppLogger.d(AppLogger.Category.LAUNCHER, "ConfigurationActivity - Default Home status: $isDefault")
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
      HomePlatformManager.openDefaultHomeSettings(this)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to launch default Home request intent", e)
      HomePlatformManager.openDefaultHomeSettings(this)
    }
  }

  private fun navigateToHomeSurface() {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_HOME)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(homeIntent)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppLogger.i(AppLogger.Category.LIFECYCLE, "ConfigurationActivity onCreate (Task ID: $taskId)")
    updateDefaultHomeStatus()
    spaceViewModel.ensureDefaultSpaceInitialized()
    discoveryViewModel.loadApps(isSilent = true)

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        androidx.compose.material3.Surface(
          modifier = Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          val currentScreen by currentConfigScreen
          val isDefaultHome by isDefaultHomeState
          var editingSpace by remember { mutableStateOf<Space?>(null) }

          if (currentScreen == "diagnostics" || currentScreen == "create_space") {
            androidx.activity.compose.BackHandler {
              editingSpace = null
              currentConfigScreen.value = "config"
            }
          }

          when (currentScreen) {
            "create_space" -> {
              val discoveryUiState by discoveryViewModel.uiState.collectAsState()
              CreateSpaceScreen(
                allApps = discoveryUiState.allApps,
                spaceViewModel = spaceViewModel,
                getBitmap = { app -> discoveryViewModel.getAppIconBitmap(app) },
                editingSpace = editingSpace,
                onNavigateBack = {
                  editingSpace = null
                  currentConfigScreen.value = "config"
                },
                onSpaceCreated = { _ ->
                  editingSpace = null
                  currentConfigScreen.value = "config"
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
                  currentConfigScreen.value = "diagnostics"
                },
                onOpenHomeSurface = {
                  navigateToHomeSurface()
                },
                onOpenCreateSpace = {
                  editingSpace = null
                  currentConfigScreen.value = "create_space"
                },
                onOpenEditSpace = { spaceToEdit ->
                  editingSpace = spaceToEdit
                  currentConfigScreen.value = "create_space"
                },
                modifier = Modifier.fillMaxSize()
              )
            }
            "diagnostics" -> {
              AppCatalogScreen(
                viewModel = discoveryViewModel,
                onNavigateDiagnostics = {
                  currentConfigScreen.value = "logs"
                },
                onNavigateHome = {
                  navigateToHomeSurface()
                },
                modifier = Modifier.fillMaxSize()
              )
            }
            else -> {
              LauncherDiagnosticsScreen(
                eventLogs = eventLogs,
                onTriggerCheck = {
                  updateDefaultHomeStatus()
                },
                modifier = Modifier.fillMaxSize()
              )
            }
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    updateDefaultHomeStatus()
    discoveryViewModel.loadApps(isSilent = true)
    AppLogger.d(AppLogger.Category.LIFECYCLE, "ConfigurationActivity onResume")
  }
}
