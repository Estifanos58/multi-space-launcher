package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.diagnostics.AppLogger
import com.example.presentation.AppCatalogScreen
import com.example.presentation.AppDiscoveryViewModel
import com.example.presentation.LauncherDiagnosticsScreen
import com.example.presentation.LauncherHomeScreen
import com.example.presentation.SpaceViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

  private val discoveryViewModel: AppDiscoveryViewModel by viewModels()
  private val spaceViewModel: SpaceViewModel by viewModels()
  private val eventLogs = mutableStateListOf<String>()
  private var activeScreenState = mutableStateOf("home")

  private fun recordEvent(tag: String, message: String) {
    val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
    eventLogs.add("[$time] $tag: $message")
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    AppLogger.i(AppLogger.Category.LIFECYCLE, "MainActivity onCreate: Initializing Launcher")
    recordEvent("I/Lifecycle", "MainActivity onCreate (singleTask)")
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val currentScreen by activeScreenState

        when (currentScreen) {
          "home" -> {
            LauncherHomeScreen(
              viewModel = discoveryViewModel,
              spaceViewModel = spaceViewModel,
              onNavigateDiagnostics = { activeScreenState.value = "discovery" },
              modifier = Modifier.fillMaxSize()
            )
          }
          "discovery" -> {
            AppCatalogScreen(
              viewModel = discoveryViewModel,
              onNavigateDiagnostics = { activeScreenState.value = "home_spike" },
              onNavigateHome = { activeScreenState.value = "home" },
              modifier = Modifier.fillMaxSize()
            )
          }
          else -> {
            LauncherDiagnosticsScreen(
              eventLogs = eventLogs,
              onTriggerCheck = {
                recordEvent("D/Launcher", "Manual Role Status Check Triggered")
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
    AppLogger.i(AppLogger.Category.LAUNCHER, "MainActivity onNewIntent: Home button / intent captured while active")
    recordEvent("I/Launcher", "onNewIntent: Home key captured (singleTask active)")
    // Return to the primary Home screen whenever Home intent is received
    activeScreenState.value = "home"
  }

  override fun onStart() {
    super.onStart()
    AppLogger.d(AppLogger.Category.LIFECYCLE, "MainActivity onStart")
    recordEvent("D/Lifecycle", "MainActivity onStart")
  }

  override fun onResume() {
    super.onResume()
    AppLogger.d(AppLogger.Category.LIFECYCLE, "MainActivity onResume")
    recordEvent("D/Lifecycle", "MainActivity onResume")
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
