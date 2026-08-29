package com.multispace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.ui.theme.DarkTerminalAccent
import com.multispace.ui.theme.DarkTerminalSurface
import com.multispace.ui.theme.DarkTerminalText
import com.multispace.ui.theme.LightBackground
import com.multispace.ui.theme.LightSurfaceContainer
import com.multispace.ui.theme.PrimaryContainerBadge
import com.multispace.ui.theme.PrimaryContainerLight
import com.multispace.ui.theme.PrimaryPurple
import com.multispace.ui.theme.PrimaryPurpleDark
import com.multispace.ui.theme.StatusGreen
import com.multispace.ui.theme.TextMuted
import com.multispace.ui.theme.TextPrimary
import com.multispace.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoundationOverviewScreen(modifier: Modifier = Modifier) {
  var selectedTab by remember { mutableIntStateOf(0) }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(LightBackground)
      .testTag("foundation_root_scaffold"),
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
              Box(
                modifier = Modifier
                  .size(width = 20.dp, height = 4.dp)
                  .clip(RoundedCornerShape(2.dp))
                  .background(PrimaryPurpleDark)
              )
            }
            Column {
              Text(
                text = "Phase 0: Foundation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.testTag("app_title_text")
              )
              Text(
                text = "Multi-Space Launcher",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = { /* Informational anchor */ },
            modifier = Modifier.testTag("more_options_button")
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = "Options Menu",
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
              contentDescription = "Home Overview"
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
              imageVector = Icons.Default.Folder,
              contentDescription = "Project Structure"
            )
          },
          label = {
            Text(
              text = "Structure",
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
          modifier = Modifier.testTag("nav_item_structure")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = {
            Icon(
              imageVector = Icons.Default.Terminal,
              contentDescription = "System Diagnostics"
            )
          },
          label = {
            Text(
              text = "Diagnostics",
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
          modifier = Modifier.testTag("nav_item_diagnostics")
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
        HeroStatusSection()
      }

      item {
        GridSummarySection()
      }

      item {
        SystemLogsTerminalSection()
      }

      item {
        Text(
          text = "FOUNDATION VERIFICATION",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = TextSecondary,
          letterSpacing = 1.sp,
          modifier = Modifier.padding(start = 4.dp, top = 6.dp)
        )
      }

      item {
        SpecificationItem(
          icon = Icons.Default.CheckCircle,
          title = "Build & Runtime Foundation",
          description = "Kotlin Android application compiling against API 36 with minSdk 28."
        )
      }

      item {
        SpecificationItem(
          icon = Icons.Default.Layers,
          title = "Single-Module Architecture",
          description = "Formalized package layout: presentation, domain, data, platform, diagnostics."
        )
      }

      item {
        SpecificationItem(
          icon = Icons.Default.Speed,
          title = "Compose UI & Diagnostics",
          description = "Jetpack Compose active with structured Android Logcat lifecycle tracing."
        )
      }

      item {
        SpecificationItem(
          icon = Icons.Default.Shield,
          title = "Scope Protection Active",
          description = "Phase 0 boundary preserved. Launcher role, Spaces, and Room deferred to planned phases."
        )
      }

      item {
        NextMilestoneCard()
        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun HeroStatusSection() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("status_banner_card"),
    colors = CardDefaults.cardColors(
      containerColor = PrimaryContainerLight
    ),
    shape = RoundedCornerShape(28.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(22.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "STATUS",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = PrimaryPurpleDark,
          letterSpacing = 1.5.sp
        )
        Surface(
          shape = RoundedCornerShape(50),
          color = PrimaryContainerBadge,
          modifier = Modifier.padding(vertical = 2.dp)
        ) {
          Text(
            text = "ACTIVE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryPurpleDark,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }

      Text(
        text = "Build Successful",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = PrimaryPurpleDark
      )

      Text(
        text = "v1-foundation-alpha · targetSdk 36 · minSdk 28",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
      )
    }
  }
}

@Composable
private fun GridSummarySection() {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Project Tree Card
    Card(
      modifier = Modifier
        .weight(1f)
        .testTag("project_tree_card"),
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
          text = "PROJECT TREE",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = TextSecondary,
          letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(
          verticalArrangement = Arrangement.spacedBy(5.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          TreeItem(prefix = "└─", name = "app/", indent = 0)
          TreeItem(prefix = "└─", name = "presentation/", indent = 1)
          TreeItem(prefix = "└─", name = "domain/", indent = 1)
          TreeItem(prefix = "└─", name = "data/", indent = 1)
          TreeItem(prefix = "└─", name = "platform/", indent = 1)
          TreeItem(prefix = "└─", name = "docs/", indent = 0)
        }
      }
    }

    // Continuity Status Card
    Card(
      modifier = Modifier
        .weight(1f)
        .testTag("continuity_card"),
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
          text = "CONTINUITY",
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
          ContinuityStatusRow(name = "CONSTITUTION", isVerified = true)
          ContinuityStatusRow(name = "PROJECT_STATE", isVerified = true)
          ContinuityStatusRow(name = "ARCHITECTURE", isVerified = true)
          ContinuityStatusRow(name = "DECISIONS", isVerified = true)
          ContinuityStatusRow(name = "HANDOFF", isVerified = true)
        }
      }
    }
  }
}

@Composable
private fun TreeItem(prefix: String, name: String, indent: Int) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(start = (indent * 12).dp)
  ) {
    Text(
      text = prefix,
      style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
      color = PrimaryPurple,
      fontWeight = FontWeight.Medium
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = name,
      style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
      color = TextPrimary,
      fontWeight = FontWeight.Normal
    )
  }
}

@Composable
private fun ContinuityStatusRow(name: String, isVerified: Boolean) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .size(7.dp)
        .clip(CircleShape)
        .background(if (isVerified) StatusGreen else TextMuted)
    )
    Text(
      text = name,
      style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
      color = if (isVerified) TextPrimary else TextSecondary,
      fontWeight = if (isVerified) FontWeight.SemiBold else FontWeight.Normal
    )
  }
}

@Composable
private fun SystemLogsTerminalSection() {
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
          text = "SYSTEM_LOGS",
          style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
          color = DarkTerminalText.copy(alpha = 0.7f),
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
        Text(
          text = "LOGCAT · ACTIVE",
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
        TerminalLine(text = "> D/Lifecycle: MainActivity initialized (onCreate)")
        TerminalLine(text = "> D/Compose: Phase0FoundationScreen rendered")
        TerminalLine(text = "> I/Phase0: Single-module foundation active")
        TerminalLine(text = "> D/Adb: Scope control enforced (Phases 1-11 protected)")
      }
    }
  }
}

@Composable
private fun TerminalLine(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
    color = DarkTerminalText.copy(alpha = 0.9f),
    fontSize = 11.sp,
    lineHeight = 16.sp
  )
}

@Composable
private fun SpecificationItem(
  icon: ImageVector,
  title: String,
  description: String
) {
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
          imageVector = icon,
          contentDescription = title,
          tint = PrimaryPurpleDark,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
          color = TextPrimary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
      }
    }
  }
}

@Composable
private fun NextMilestoneCard() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("next_milestone_card"),
    colors = CardDefaults.cardColors(
      containerColor = LightSurfaceContainer
    ),
    shape = RoundedCornerShape(20.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Text(
        text = "NEXT PLANNED MILESTONE",
        style = MaterialTheme.typography.labelSmall,
        color = PrimaryPurple,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Phase 1 — Launcher Viability Spike (Home role verification on physical test device).",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
      )
    }
  }
}

