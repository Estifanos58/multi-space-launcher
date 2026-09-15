package com.multispace

import android.content.Intent
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
import com.multispace.presentation.*
import com.multispace.presentation.lifecycle.LauncherLifecycleCoordinator
import com.multispace.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

  private val discoveryViewModel: AppDiscoveryViewModel by viewModels()
  private val spaceViewModel: SpaceViewModel by viewModels()

  private val lifecycleCoordinator by lazy {
    LauncherLifecycleCoordinator(
      onScreenOff = {
        spaceViewModel.lockPhone()
      },
      onHomeIntent = {
        spaceViewModel.setLayer(1)
      }
    )
  }

  private val requestHomeRoleLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { _ ->
    lifecycleCoordinator.refreshDefaultHomeStatus(this)
  }

  fun requestSetDefaultHome() {
    lifecycleCoordinator.requestSetDefaultHome(this, requestHomeRoleLauncher)
  }

  private fun openConfigurationActivity() {
    AppLogger.i(AppLogger.Category.LAUNCHER, "MainActivity -> Launching ConfigurationActivity")
    val intent = Intent(this, ConfigurationActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    lifecycleCoordinator.onCreate(this, intent)

    spaceViewModel.ensureDefaultSpaceInitialized()

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
    lifecycleCoordinator.onNewIntent(this, intent)
  }

  override fun onStart() {
    super.onStart()
    lifecycleCoordinator.onStart(this, intent)
  }

  override fun onResume() {
    super.onResume()
    lifecycleCoordinator.onResume(this, intent)
  }

  override fun onPause() {
    super.onPause()
    lifecycleCoordinator.onPause(this, intent)
  }

  override fun onStop() {
    super.onStop()
    lifecycleCoordinator.onStop(this, intent)
  }

  override fun onDestroy() {
    super.onDestroy()
    lifecycleCoordinator.onDestroy(this)
  }
}
