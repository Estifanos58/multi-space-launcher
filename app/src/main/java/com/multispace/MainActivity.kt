package com.multispace

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.DiscoveredApp
import com.multispace.platform.HomePlatformManager
import com.multispace.presentation.*
import com.multispace.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : FragmentActivity() {

  private val discoveryViewModel: AppDiscoveryViewModel by viewModels()
  private val spaceViewModel: SpaceViewModel by viewModels()
  private val eventLogs = mutableStateListOf<String>()

  // Reactive state for default launcher status
  private val isDefaultHomeState = mutableStateOf(false)

  private val screenOffReceiver = object : android.content.BroadcastReceiver() {
    override fun onReceive(context: android.content.Context?, intent: Intent?) {
      if (intent?.action == Intent.ACTION_SCREEN_OFF) {
        AppLogger.i(AppLogger.Category.LIFECYCLE, "Screen turned off -> Locking Multi-Space phone lock")
        spaceViewModel.lockPhone()
      }
    }
  }

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

  private fun openConfigurationActivity() {
    AppLogger.i(AppLogger.Category.LAUNCHER, "MainActivity -> Launching ConfigurationActivity")
    val intent = Intent(this, ConfigurationActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
  }

  private fun logActivityDetails(event: String, intent: Intent?) {
    val action = intent?.action ?: "null"
    val categories = intent?.categories?.joinToString(",") ?: "none"
    val flags = intent?.flags?.let { "0x" + Integer.toHexString(it) } ?: "0x0"
    AppLogger.i(
      AppLogger.Category.LIFECYCLE,
      "MainActivity $event -> taskId=$taskId, isTaskRoot=$isTaskRoot, action=$action, categories=[$categories], flags=$flags"
    )
    recordEvent("I/Lifecycle", "$event (taskId=$taskId, root=$isTaskRoot, act=$action)")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    logActivityDetails("onCreate", intent)

    updateDefaultHomeStatus()
    spaceViewModel.ensureDefaultSpaceInitialized()

    try {
      val filter = android.content.IntentFilter(Intent.ACTION_SCREEN_OFF)
      registerReceiver(screenOffReceiver, filter)
    } catch (e: Exception) {
      AppLogger.w(AppLogger.Category.LIFECYCLE, "Could not register screenOffReceiver: ${e.message}")
    }

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        androidx.compose.material3.Surface(
          modifier = Modifier.fillMaxSize(),
          color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
          val isPhoneLocked by spaceViewModel.isPhoneLocked.collectAsState()

          if (isPhoneLocked) {
            MultiSpaceLockScreen(
              spaceViewModel = spaceViewModel,
              onUnlockSuccess = { _ ->
                // Phone unlocked, reveal active Space home surface
              },
              modifier = Modifier.fillMaxSize()
            )
          } else {
            LauncherHomeScreen(
              discoveryViewModel = discoveryViewModel,
              spaceViewModel = spaceViewModel,
              onLaunchApp = { app ->
                discoveryViewModel.launchApp(app)
              },
              onOpenConfiguration = {
                openConfigurationActivity()
              },
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    logActivityDetails("onNewIntent", intent)
    updateDefaultHomeStatus()

    val isHomeIntent = intent.hasCategory(Intent.CATEGORY_HOME) == true ||
      (intent.action == Intent.ACTION_MAIN && intent.categories?.contains(Intent.CATEGORY_HOME) == true)

    AppLogger.i(AppLogger.Category.LAUNCHER, "MainActivity onNewIntent: isHomeIntent=$isHomeIntent, isDefault=${isDefaultHomeState.value}")
    recordEvent("I/Launcher", "onNewIntent: isHomeIntent=$isHomeIntent, isDefault=${isDefaultHomeState.value}")

    // When the user presses Home or unlocks, always return to the active space Layer 1
    if (isHomeIntent) {
      spaceViewModel.setLayer(1)
    }
  }

  override fun onStart() {
    super.onStart()
    logActivityDetails("onStart", intent)
    updateDefaultHomeStatus()
    spaceViewModel.ensureDefaultSpaceInitialized()
    discoveryViewModel.loadApps(isSilent = true)
  }

  override fun onResume() {
    super.onResume()
    logActivityDetails("onResume", intent)
    updateDefaultHomeStatus()
    discoveryViewModel.loadApps(isSilent = true)
  }

  override fun onPause() {
    super.onPause()
    logActivityDetails("onPause", intent)
  }

  override fun onStop() {
    super.onStop()
    logActivityDetails("onStop", intent)
  }

  override fun onDestroy() {
    super.onDestroy()
    logActivityDetails("onDestroy", intent)
    try {
      unregisterReceiver(screenOffReceiver)
    } catch (e: Exception) {
      // Receiver may not be registered
    }
  }
}
