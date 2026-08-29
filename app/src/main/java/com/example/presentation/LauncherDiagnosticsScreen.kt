package com.example.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diagnostics.AppLogger
import com.example.platform.HomePlatformManager
import com.example.ui.theme.DarkTerminalAccent
import com.example.ui.theme.DarkTerminalSurface
import com.example.ui.theme.DarkTerminalText
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurfaceContainer
import com.example.ui.theme.PrimaryContainerBadge
import com.example.ui.theme.PrimaryContainerLight
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleDark
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherDiagnosticsScreen(
  eventLogs: List<String>,
  onTriggerCheck: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var selectedTab by remember { mutableIntStateOf(0) }
  var homeState by remember { mutableStateOf(HomePlatformManager.checkHomeStatus(context)) }

  // Activity Result Launcher for Role Manager Request
  val roleRequestLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    AppLogger.i(
      AppLogger.Category.LAUNCHER,
      "RoleManager request returned with resultCode: ${result.resultCode}"
    )
    homeState = HomePlatformManager.checkHomeStatus(context)
    onTriggerCheck()
  }

  // Refresh status on composable launch
  LaunchedEffect(Unit) {
    homeState = HomePlatformManager.checkHomeStatus(context)
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(LightBackground)
      .testTag("phase1_root_scaffold"),
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryContainerLight),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Launcher Icon",
                tint = PrimaryPurpleDark,
                modifier = Modifier.size(22.dp)
              )
            }
            Column {
              Text(
                text = "Phase 1: Launcher Spike",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.testTag("app_title_text")
              )
              Text(
                text = "Home Role & Viability Validation",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = {
              homeState = HomePlatformManager.checkHomeStatus(context)
              onTriggerCheck()
            },
            modifier = Modifier.testTag("refresh_status_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh Status",
              tint = TextSecondary
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = LightBackground
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = LightSurfaceContainer,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("bottom_nav_bar")
      ) {
        NavigationBarItem(
          selected = selectedTab == 0,
          onClick = { selectedTab = 0 },
          icon = {
            Icon(
              imageVector = Icons.Default.Home,
              contentDescription = "Spike Overview"
            )
          },
          label = {
            Text(
              text = "Home",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Medium
            )
          },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = PrimaryContainerLight,
            selectedIconColor = PrimaryPurpleDark,
            selectedTextColor = TextPrimary,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
          ),
          modifier = Modifier.testTag("nav_item_home")
        )
        NavigationBarItem(
          selected = selectedTab == 1,
          onClick = { selectedTab = 1 },
          icon = {
            Icon(
              imageVector = Icons.Default.Description,
              contentDescription = "Gate 1 Checklist"
            )
          },
          label = {
            Text(
              text = "Gate 1",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Medium
            )
          },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = PrimaryContainerLight,
            selectedIconColor = PrimaryPurpleDark,
            selectedTextColor = TextPrimary,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
          ),
          modifier = Modifier.testTag("nav_item_gate1")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = {
            Icon(
              imageVector = Icons.Default.Terminal,
              contentDescription = "Live Telemetry"
            )
          },
          label = {
            Text(
              text = "Telemetry",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Medium
            )
          },
          colors = NavigationBarItemDefaults.colors(
            indicatorColor = PrimaryContainerLight,
            selectedIconColor = PrimaryPurpleDark,
            selectedTextColor = TextPrimary,
            unselectedIconColor = TextSecondary,
            unselectedTextColor = TextSecondary
          ),
          modifier = Modifier.testTag("nav_item_telemetry")
        )
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Spacer(modifier = Modifier.height(4.dp))
        HomeRoleStatusCard(
          homeState = homeState,
          onRequestRole = {
            val intent = HomePlatformManager.createRequestDefaultHomeIntent(context)
            try {
              roleRequestLauncher.launch(intent)
            } catch (e: Exception) {
              AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to launch Home request intent", e)
              context.startActivity(intent)
            }
          }
        )
      }

      item {
        SpikeExperimentActionsCard(
          onLaunchSettings = {
            val intent = HomePlatformManager.createTestExternalAppIntent()
            try {
              context.startActivity(intent)
            } catch (e: Exception) {
              AppLogger.e(AppLogger.Category.LAUNCH, "Failed to launch test external app", e)
            }
          },
          onCheckStatus = {
            homeState = HomePlatformManager.checkHomeStatus(context)
            onTriggerCheck()
          }
        )
      }

      item {
        GridGateSummarySection(isDefaultHome = homeState == HomePlatformManager.HomeRoleState.DEFAULT_HOME)
      }

      item {
        LiveTelemetrySection(eventLogs = eventLogs)
      }

      item {
        Phase1ScopeProtectionCard()
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun HomeRoleStatusCard(
  homeState: HomePlatformManager.HomeRoleState,
  onRequestRole: () -> Unit
) {
  val isDefault = homeState == HomePlatformManager.HomeRoleState.DEFAULT_HOME

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("home_role_status_card"),
    colors = CardDefaults.cardColors(
      containerColor = if (isDefault) PrimaryContainerLight else LightSurfaceContainer
    ),
    shape = RoundedCornerShape(28.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "HOME ROLE ELIGIBILITY",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = if (isDefault) PrimaryPurpleDark else TextSecondary,
          letterSpacing = 1.5.sp
        )
        Surface(
          shape = RoundedCornerShape(50),
          color = if (isDefault) PrimaryContainerBadge else Color(0xFFE0E0E0),
          modifier = Modifier.padding(vertical = 2.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isDefault) StatusGreen else StatusAmber)
            )
            Text(
              text = if (isDefault) "DEFAULT HOME" else "ELIGIBLE",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = if (isDefault) PrimaryPurpleDark else TextPrimary
            )
          }
        }
      }

      Text(
        text = if (isDefault) "Default Launcher Active" else "Home Intent Configured",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = if (isDefault) PrimaryPurpleDark else TextPrimary
      )

      Text(
        text = if (isDefault) {
          "Multi-Space Launcher is currently selected as the active Home app on this device."
        } else {
          "Intent filter categories HOME & DEFAULT declared. Tap below to request default launcher selection."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
      )

      if (!isDefault) {
        Spacer(modifier = Modifier.height(4.dp))
        Button(
          onClick = onRequestRole,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("request_home_role_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryPurple,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Launch,
            contentDescription = "Launch Home Selector",
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Set as Default Home Launcher",
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

@Composable
private fun SpikeExperimentActionsCard(
  onLaunchSettings: () -> Unit,
  onCheckStatus: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("spike_experiments_card"),
    colors = CardDefaults.cardColors(
      containerColor = LightSurfaceContainer
    ),
    shape = RoundedCornerShape(24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Text(
        text = "SPIKE EXPERIMENT CONTROLS",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        letterSpacing = 1.sp
      )

      Text(
        text = "To physically validate Gate 1 (Launcher Viability): launch an external app, press your device's Home key/gesture, and confirm immediate return to this launcher.",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onLaunchSettings,
          modifier = Modifier
            .weight(1f)
            .testTag("test_external_launch_button"),
          colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryContainerLight,
            contentColor = PrimaryPurpleDark
          ),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Open Settings",
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Test App Launch",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
          )
        }

        OutlinedButton(
          onClick = onCheckStatus,
          modifier = Modifier.testTag("verify_status_button"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Check Role Status",
            tint = PrimaryPurple,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Check Role",
            fontSize = 13.sp,
            color = PrimaryPurple,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }
  }
}

@Composable
private fun GridGateSummarySection(isDefaultHome: Boolean) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Gate 1 Checklist Card
    Card(
      modifier = Modifier
        .weight(1f)
        .testTag("gate1_checklist_card"),
      colors = CardDefaults.cardColors(
        containerColor = LightSurfaceContainer
      ),
      shape = RoundedCornerShape(24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Text(
          text = "GATE 1 CHECKLIST",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = TextSecondary,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          ChecklistItem(name = "Manifest HOME intent", isPassed = true)
          ChecklistItem(name = "launchMode singleTask", isPassed = true)
          ChecklistItem(name = "Home role eligibility", isPassed = true)
          ChecklistItem(name = "Role selected by user", isPassed = isDefaultHome)
          ChecklistItem(name = "Home button capture", isPassed = isDefaultHome)
          ChecklistItem(name = "Reboot persistence", isPassed = false)
        }
      }
    }

    // Protected Boundaries Card
    Card(
      modifier = Modifier
        .weight(1f)
        .testTag("boundaries_card"),
      colors = CardDefaults.cardColors(
        containerColor = LightSurfaceContainer
      ),
      shape = RoundedCornerShape(24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Text(
          text = "PHASE BOUNDARIES",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = TextSecondary,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          BoundaryRow(phase = "P1: Home Viability", status = "ACTIVE")
          BoundaryRow(phase = "P2: App Discovery", status = "NEXT")
          BoundaryRow(phase = "P3: App Launching", status = "WAIT")
          BoundaryRow(phase = "P5: Space Room DB", status = "WAIT")
          BoundaryRow(phase = "P8: Space PIN Auth", status = "WAIT")
        }
      }
    }
  }
}

@Composable
private fun ChecklistItem(name: String, isPassed: Boolean) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(if (isPassed) StatusGreen else TextMuted)
    )
    Text(
      text = name,
      style = MaterialTheme.typography.labelSmall,
      color = if (isPassed) TextPrimary else TextSecondary,
      fontWeight = if (isPassed) FontWeight.SemiBold else FontWeight.Normal,
      fontSize = 11.sp
    )
  }
}

@Composable
private fun BoundaryRow(phase: String, status: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth()
  ) {
    Text(
      text = phase,
      style = MaterialTheme.typography.labelSmall,
      color = TextPrimary,
      fontSize = 11.sp
    )
    Text(
      text = status,
      style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
      color = when (status) {
        "ACTIVE" -> PrimaryPurple
        "NEXT" -> StatusAmber
        else -> TextMuted
      },
      fontWeight = FontWeight.Bold,
      fontSize = 10.sp
    )
  }
}

@Composable
private fun LiveTelemetrySection(eventLogs: List<String>) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("system_logs_card"),
    colors = CardDefaults.cardColors(
      containerColor = DarkTerminalSurface
    ),
    shape = RoundedCornerShape(24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "LIVE TELEMETRY LOGCAT",
          style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
          color = DarkTerminalText.copy(alpha = 0.7f),
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
        Text(
          text = "MSLauncher:*",
          style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
          color = DarkTerminalAccent,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (eventLogs.isEmpty()) {
          Text(
            text = "> No events captured yet. Interacting with Home button or app transitions will record telemetry.",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = DarkTerminalText.copy(alpha = 0.7f),
            fontSize = 11.sp
          )
        } else {
          eventLogs.takeLast(6).forEach { log ->
            Text(
              text = log,
              style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
              color = DarkTerminalText.copy(alpha = 0.9f),
              fontSize = 11.sp,
              lineHeight = 16.sp
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Phase1ScopeProtectionCard() {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    color = LightSurfaceContainer
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.Top
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(PrimaryContainerLight),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Shield,
          contentDescription = "Scope Protection",
          tint = PrimaryPurpleDark,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(
          text = "Scope Boundary Strict (Phase 1)",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Application discovery (LauncherApps), Room persistence, Space models, and PIN security remain strictly protected until Gate 1 physical validation passes.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
      }
    }
  }
}
