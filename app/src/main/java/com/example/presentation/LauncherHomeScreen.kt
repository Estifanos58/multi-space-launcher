package com.example.presentation

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DiscoveredApp
import com.example.platform.HomePlatformManager
import com.example.ui.theme.DarkTerminalAccent
import com.example.ui.theme.DarkTerminalSurface
import com.example.ui.theme.DarkTerminalText
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurfaceContainer
import com.example.ui.theme.LightSurfaceContainerHigh
import com.example.ui.theme.LightSurfaceContainerLow
import com.example.ui.theme.PrimaryContainerBadge
import com.example.ui.theme.PrimaryContainerLight
import com.example.ui.theme.PrimaryPurple
import com.example.ui.theme.PrimaryPurpleDark
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Phase 4 — Minimal Usable Launcher Home Screen.
 *
 * Provides a clean, functional Home experience:
 * - Simple, responsive 4-column application grid
 * - Application icons and labels loaded from LauncherApps discovery
 * - Direct tap-to-launch integration with verified AppLaunchManager
 * - Reactive catalog synchronization with dynamic package events
 * - Empty, loading, and error states
 * - Clean configuration / info entry point
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherHomeScreen(
  viewModel: AppDiscoveryViewModel,
  onNavigateDiagnostics: () -> Unit,
  spaceViewModel: SpaceViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  val activeSpace by spaceViewModel.activeSpace.collectAsState()
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  val snackbarHostState = remember { SnackbarHostState() }

  var showConfigSheet by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  LaunchedEffect(viewModel) {
    viewModel.userFeedback.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  LaunchedEffect(spaceViewModel) {
    spaceViewModel.userFeedback.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  Scaffold(
    modifier = modifier
      .background(LightBackground)
      .testTag("launcher_home_scaffold"),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PrimaryPurple),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "Home",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = TextPrimary
                )
                if (activeSpace != null) {
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryContainerLight
                  ) {
                    Text(
                      text = activeSpace?.name ?: "Default",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = PrimaryPurpleDark,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
              Text(
                text = "${uiState.filteredApps.size} apps available",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.loadApps() },
            modifier = Modifier.testTag("home_refresh_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh Applications",
              tint = PrimaryPurpleDark
            )
          }
          IconButton(
            onClick = { showConfigSheet = true },
            modifier = Modifier.testTag("home_config_button")
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Launcher Configuration",
              tint = PrimaryPurpleDark
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = LightBackground
        )
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp)
    ) {
      // Search Bar
      OutlinedTextField(
        value = uiState.searchQuery,
        onValueChange = { viewModel.onSearchQueryChanged(it) },
        placeholder = {
          Text(
            text = "Search apps...",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = PrimaryPurple
          )
        },
        trailingIcon = {
          if (uiState.searchQuery.isNotEmpty()) {
            IconButton(onClick = {
              viewModel.onSearchQueryChanged("")
              focusManager.clearFocus()
            }) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear search",
                tint = TextSecondary
              )
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = LightSurfaceContainerLow,
          unfocusedContainerColor = LightSurfaceContainerLow,
          focusedBorderColor = PrimaryPurple,
          unfocusedBorderColor = LightSurfaceContainerHigh
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("home_search_input")
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Category Filter Chips
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        FilterChip(
          selected = uiState.activeFilter == AppFilter.ALL,
          onClick = { viewModel.onFilterChanged(AppFilter.ALL) },
          label = { Text("All (${uiState.totalAppCount})", fontSize = 12.sp) },
          shape = RoundedCornerShape(12.dp),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryContainerBadge,
            selectedLabelColor = PrimaryPurpleDark
          ),
          modifier = Modifier.testTag("home_filter_all")
        )

        FilterChip(
          selected = uiState.activeFilter == AppFilter.USER_ONLY,
          onClick = { viewModel.onFilterChanged(AppFilter.USER_ONLY) },
          label = { Text("User (${uiState.userAppCount})", fontSize = 12.sp) },
          shape = RoundedCornerShape(12.dp),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryContainerBadge,
            selectedLabelColor = PrimaryPurpleDark
          ),
          modifier = Modifier.testTag("home_filter_user")
        )

        FilterChip(
          selected = uiState.activeFilter == AppFilter.SYSTEM_ONLY,
          onClick = { viewModel.onFilterChanged(AppFilter.SYSTEM_ONLY) },
          label = { Text("System (${uiState.systemAppCount})", fontSize = 12.sp) },
          shape = RoundedCornerShape(12.dp),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PrimaryContainerBadge,
            selectedLabelColor = PrimaryPurpleDark
          ),
          modifier = Modifier.testTag("home_filter_system")
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Main Content Area (Loading, Error, Empty, or App Grid)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        when {
          uiState.isLoading && uiState.allApps.isEmpty() -> {
            LoadingStateView(modifier = Modifier.align(Alignment.Center))
          }
          uiState.errorMessage != null && uiState.allApps.isEmpty() -> {
            ErrorStateView(
              errorMessage = uiState.errorMessage ?: "Unknown error",
              onRetry = { viewModel.loadApps() },
              modifier = Modifier.align(Alignment.Center)
            )
          }
          uiState.filteredApps.isEmpty() -> {
            EmptyStateView(
              searchQuery = uiState.searchQuery,
              onClearSearch = { viewModel.onSearchQueryChanged("") },
              onRefresh = { viewModel.loadApps() },
              modifier = Modifier.align(Alignment.Center)
            )
          }
          else -> {
            LauncherGridContent(
              apps = uiState.filteredApps,
              getIcon = { viewModel.getAppIcon(it) },
              onLaunchApp = { viewModel.launchApp(it) }
            )
          }
        }
      }
    }
  }

  // Launcher Configuration & Diagnostics Sheet
  if (showConfigSheet) {
    ModalBottomSheet(
      onDismissRequest = { showConfigSheet = false },
      sheetState = sheetState,
      containerColor = LightBackground,
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
      LauncherConfigSheetContent(
        uiState = uiState,
        spaceViewModel = spaceViewModel,
        onDismiss = { showConfigSheet = false },
        onOpenDiagnostics = {
          showConfigSheet = false
          onNavigateDiagnostics()
        },
        onRefreshCatalog = { viewModel.loadApps() }
      )
    }
  }
}

/**
 * 4-Column Application Grid.
 */
@Composable
private fun LauncherGridContent(
  apps: List<DiscoveredApp>,
  getIcon: (DiscoveredApp) -> Drawable?,
  onLaunchApp: (DiscoveredApp) -> Unit
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(4),
    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    modifier = Modifier
      .fillMaxSize()
      .testTag("launcher_app_grid")
  ) {
    items(apps, key = { it.id }) { app ->
      LauncherGridItem(
        app = app,
        icon = getIcon(app),
        onLaunch = { onLaunchApp(app) }
      )
    }
  }
}

/**
 * Individual Application Icon + Label Grid Tile.
 */
@Composable
private fun LauncherGridItem(
  app: DiscoveredApp,
  icon: Drawable?,
  onLaunch: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onLaunch() }
      .padding(vertical = 8.dp, horizontal = 4.dp)
      .testTag("app_item_${app.packageName}")
  ) {
    Box(
      modifier = Modifier
        .size(56.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(LightSurfaceContainerHigh),
      contentAlignment = Alignment.Center
    ) {
      LauncherIconImage(
        drawable = icon,
        contentDescription = app.label,
        modifier = Modifier.size(46.dp)
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = app.label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = TextPrimary,
      textAlign = TextAlign.Center,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      lineHeight = 14.sp
    )
  }
}

/**
 * Loading State.
 */
@Composable
private fun LoadingStateView(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    CircularProgressIndicator(
      color = PrimaryPurple,
      strokeWidth = 3.dp,
      modifier = Modifier.size(40.dp)
    )
    Text(
      text = "Loading applications...",
      style = MaterialTheme.typography.bodyMedium,
      color = TextSecondary
    )
  }
}

/**
 * Empty State.
 */
@Composable
private fun EmptyStateView(
  searchQuery: String,
  onClearSearch: () -> Unit,
  onRefresh: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = LightSurfaceContainer)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(CircleShape)
          .background(PrimaryContainerLight),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Apps,
          contentDescription = "No Apps",
          tint = PrimaryPurpleDark,
          modifier = Modifier.size(28.dp)
        )
      }

      Text(
        text = if (searchQuery.isNotEmpty()) "No matches found" else "No applications found",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )

      Text(
        text = if (searchQuery.isNotEmpty()) {
          "No applications match \"$searchQuery\" in the current filter."
        } else {
          "No launchable applications were detected on this device profile."
        },
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(4.dp))

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (searchQuery.isNotEmpty()) {
          OutlinedButton(
            onClick = onClearSearch,
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Clear Search")
          }
        }
        Button(
          onClick = onRefresh,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
        ) {
          Text("Refresh")
        }
      }
    }
  }
}

/**
 * Error State.
 */
@Composable
private fun ErrorStateView(
  errorMessage: String,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = LightSurfaceContainer)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Box(
        modifier = Modifier
          .size(52.dp)
          .clip(CircleShape)
          .background(Color(0xFFFFEBEE)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.ErrorOutline,
          contentDescription = "Error",
          tint = Color(0xFFC62828),
          modifier = Modifier.size(28.dp)
        )
      }

      Text(
        text = "Discovery Issue",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )

      Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        textAlign = TextAlign.Center
      )

      Button(
        onClick = onRetry,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
      ) {
        Text("Retry Scan")
      }
    }
  }
}

/**
 * Launcher Configuration & System Info Bottom Sheet.
 */
@Composable
private fun LauncherConfigSheetContent(
  uiState: AppDiscoveryUiState,
  spaceViewModel: SpaceViewModel,
  onDismiss: () -> Unit,
  onOpenDiagnostics: () -> Unit,
  onRefreshCatalog: () -> Unit
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()
  var homeStatus by remember {
    mutableStateOf(HomePlatformManager.checkHomeStatus(context))
  }

  val defaultHomeLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) {
    homeStatus = HomePlatformManager.checkHomeStatus(context)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(scrollState)
      .padding(horizontal = 24.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Launcher Configuration",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Text(
          text = "Multi-Space Launcher · V1 Minimal Home",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )
      }
    }

    HorizontalDivider(color = LightSurfaceContainerHigh)

    // Space Domain & Persistence Section
    SpaceManagementSection(
      spaceViewModel = spaceViewModel,
      allApps = uiState.allApps
    )

    // Default Home Role Card
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = LightSurfaceContainer,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (homeStatus == HomePlatformManager.HomeRoleState.DEFAULT_HOME) PrimaryContainerLight else Color(0xFFFFF3E0)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (homeStatus == HomePlatformManager.HomeRoleState.DEFAULT_HOME) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = "Home Status",
            tint = if (homeStatus == HomePlatformManager.HomeRoleState.DEFAULT_HOME) StatusGreen else Color(0xFFE65100),
            modifier = Modifier.size(24.dp)
          )
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (homeStatus == HomePlatformManager.HomeRoleState.DEFAULT_HOME) "Active Default Home" else "Not Default Home",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = if (homeStatus == HomePlatformManager.HomeRoleState.DEFAULT_HOME) {
              "Multi-Space Launcher handles Home key events."
            } else {
              "Set as default to capture Home button presses."
            },
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
        }

        if (homeStatus != HomePlatformManager.HomeRoleState.DEFAULT_HOME) {
          Button(
            onClick = {
              val intent = HomePlatformManager.createRequestDefaultHomeIntent(context)
              defaultHomeLauncher.launch(intent)
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
          ) {
            Text("Set Default", fontSize = 12.sp)
          }
        }
      }
    }

    // Catalog Statistics
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = LightSurfaceContainer,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text(
          text = "CATALOG STATISTICS",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = PrimaryPurpleDark
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Total Applications", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
          Text("${uiState.totalAppCount}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("User Installed", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
          Text("${uiState.userAppCount}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("System Pre-installed", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
          Text("${uiState.systemAppCount}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text("Package Events", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
          Text(uiState.recentPackageEvent, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
        }
      }
    }

    // Engineering Diagnostics Entry Action
    OutlinedButton(
      onClick = onOpenDiagnostics,
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(14.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Open Engineering Diagnostics & Telemetry")
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = "Diagnostics",
          modifier = Modifier.size(18.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

/**
 * Renders the application icon from Drawable.
 */
@Composable
private fun LauncherIconImage(
  drawable: Drawable?,
  contentDescription: String,
  modifier: Modifier = Modifier
) {
  if (drawable != null) {
    val bitmap = remember(drawable) {
      drawableToBitmap(drawable)
    }
    if (bitmap != null) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier
      )
    } else {
      FallbackIcon(contentDescription, modifier)
    }
  } else {
    FallbackIcon(contentDescription, modifier)
  }
}

@Composable
private fun FallbackIcon(contentDescription: String, modifier: Modifier) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(PrimaryContainerLight),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = Icons.Default.Apps,
      contentDescription = contentDescription,
      tint = PrimaryPurpleDark,
      modifier = Modifier.size(24.dp)
    )
  }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap? {
  return try {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
      return drawable.bitmap
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    bitmap
  } catch (e: Exception) {
    null
  }
}
