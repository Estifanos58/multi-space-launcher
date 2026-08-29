package com.multispace.presentation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.DiscoveredApp
import com.multispace.ui.theme.DarkTerminalAccent
import com.multispace.ui.theme.DarkTerminalSurface
import com.multispace.ui.theme.DarkTerminalText
import com.multispace.ui.theme.LightBackground
import com.multispace.ui.theme.LightSurfaceContainer
import com.multispace.ui.theme.LightSurfaceContainerHigh
import com.multispace.ui.theme.PrimaryContainerBadge
import com.multispace.ui.theme.PrimaryContainerLight
import com.multispace.ui.theme.PrimaryPurple
import com.multispace.ui.theme.PrimaryPurpleDark
import com.multispace.ui.theme.StatusGreen
import com.multispace.ui.theme.TextMuted
import com.multispace.ui.theme.TextPrimary
import com.multispace.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCatalogScreen(
  viewModel: AppDiscoveryViewModel,
  onNavigateDiagnostics: () -> Unit,
  onNavigateHome: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsState()
  var selectedTab by remember { mutableIntStateOf(1) } // Default to "Apps" tab
  var showSortMenu by remember { mutableStateOf(false) }
  val focusManager = LocalFocusManager.current
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(viewModel) {
    viewModel.userFeedback.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(LightBackground)
      .testTag("app_catalog_root_scaffold"),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(
            onClick = onNavigateHome,
            modifier = Modifier.testTag("nav_back_to_home")
          ) {
            Icon(
              imageVector = Icons.Default.Home,
              contentDescription = "Return to Home",
              tint = PrimaryPurpleDark
            )
          }
        },
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
                imageVector = Icons.Default.Apps,
                contentDescription = "Apps Discovery",
                tint = PrimaryPurpleDark,
                modifier = Modifier.size(22.dp)
              )
            }
            Column {
              Text(
                text = "Diagnostics & Discovery",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.testTag("app_title_text")
              )
              Text(
                text = "${uiState.totalAppCount} apps · Tap to launch",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = { viewModel.loadApps() },
            modifier = Modifier.testTag("refresh_discovery_button")
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Refresh Apps Scan",
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
          onClick = {
            selectedTab = 0
            onNavigateDiagnostics()
          },
          icon = {
            Icon(
              imageVector = Icons.Default.Home,
              contentDescription = "Home Spike"
            )
          },
          label = {
            Text(
              text = "Home Spike",
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
              imageVector = Icons.Default.Apps,
              contentDescription = "App Catalog"
            )
          },
          label = {
            Text(
              text = "App Catalog",
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
          modifier = Modifier.testTag("nav_item_catalog")
        )
        NavigationBarItem(
          selected = selectedTab == 2,
          onClick = { selectedTab = 2 },
          icon = {
            Icon(
              imageVector = Icons.Default.Terminal,
              contentDescription = "Telemetry"
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
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      if (selectedTab == 2) {
        // Full Telemetry View
        DiscoveryTelemetryView(uiState = uiState)
      } else {
        // Discovery Overview Header
        DiscoveryHeroCard(uiState = uiState)

        // Search & Controls Bar
        SearchAndControlsBar(
          searchQuery = uiState.searchQuery,
          onQueryChange = { viewModel.onSearchQueryChanged(it) },
          activeFilter = uiState.activeFilter,
          onFilterChange = { viewModel.onFilterChanged(it) },
          viewMode = uiState.viewMode,
          onViewModeChange = { viewModel.onViewModeChanged(it) },
          sortMode = uiState.sortMode,
          onSortModeChange = { viewModel.onSortModeChanged(it) },
          userCount = uiState.userAppCount,
          systemCount = uiState.systemAppCount,
          totalCount = uiState.totalAppCount,
          showSortMenu = showSortMenu,
          onToggleSortMenu = { showSortMenu = it },
          onDismissKeyboard = { focusManager.clearFocus() }
        )

        // Main App Grid / List Content
        if (uiState.isLoading) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              CircularProgressIndicator(
                color = PrimaryPurple,
                modifier = Modifier.size(32.dp)
              )
              Text(
                text = "Scanning device packages via LauncherApps...",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = TextSecondary
              )
            }
          }
        } else if (uiState.filteredApps.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "No apps found",
                tint = TextMuted,
                modifier = Modifier.size(48.dp)
              )
              Text(
                text = "No applications match \"${uiState.searchQuery}\"",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
              )
              Text(
                text = "Try adjusting your search query or filter.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
            }
          }
        } else {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
          ) {
            if (uiState.viewMode == AppViewMode.GRID) {
              AppGridContent(
                apps = uiState.filteredApps,
                getBitmap = { viewModel.getAppIconBitmap(it) },
                onLaunchApp = { viewModel.launchApp(it) }
              )
            } else {
              AppListContent(
                apps = uiState.filteredApps,
                getBitmap = { viewModel.getAppIconBitmap(it) },
                onLaunchApp = { viewModel.launchApp(it) }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun DiscoveryHeroCard(uiState: AppDiscoveryUiState) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("discovery_hero_card"),
    colors = CardDefaults.cardColors(
      containerColor = PrimaryContainerLight
    ),
    shape = RoundedCornerShape(24.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "LAUNCH & DISCOVERY ENGINE",
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
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(StatusGreen)
            )
            Text(
              text = "READY",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = PrimaryPurpleDark
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
      ) {
        Column {
          Text(
            text = "${uiState.totalAppCount} Launchable Apps",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryPurpleDark
          )
          Text(
            text = "Tap any application to launch via LauncherApps",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          CountBadge(label = "User", count = uiState.userAppCount)
          CountBadge(label = "System", count = uiState.systemAppCount)
        }
      }
    }
  }
}

@Composable
private fun CountBadge(label: String, count: Int) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = LightSurfaceContainer,
    modifier = Modifier.padding(vertical = 2.dp)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
      Text(
        text = "$count",
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontSize = 9.sp,
        color = TextSecondary
      )
    }
  }
}

@Composable
private fun SearchAndControlsBar(
  searchQuery: String,
  onQueryChange: (String) -> Unit,
  activeFilter: AppFilter,
  onFilterChange: (AppFilter) -> Unit,
  viewMode: AppViewMode,
  onViewModeChange: (AppViewMode) -> Unit,
  sortMode: AppSortMode,
  onSortModeChange: (AppSortMode) -> Unit,
  userCount: Int,
  systemCount: Int,
  totalCount: Int,
  showSortMenu: Boolean,
  onToggleSortMenu: (Boolean) -> Unit,
  onDismissKeyboard: () -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    // Search Box Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        modifier = Modifier
          .weight(1f)
          .testTag("app_search_text_field"),
        placeholder = {
          Text(
            text = "Search apps or packages...",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted
          )
        },
        leadingIcon = {
          Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        },
        trailingIcon = {
          if (searchQuery.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }) {
              Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear search",
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = LightSurfaceContainer,
          unfocusedContainerColor = LightSurfaceContainer,
          focusedBorderColor = PrimaryPurple,
          unfocusedBorderColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onDismissKeyboard() })
      )

      // View Mode Toggle (Grid / List)
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = LightSurfaceContainer,
        modifier = Modifier.height(56.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 4.dp)
        ) {
          IconButton(
            onClick = {
              onViewModeChange(
                if (viewMode == AppViewMode.GRID) AppViewMode.LIST else AppViewMode.GRID
              )
            },
            modifier = Modifier.testTag("toggle_view_mode_button")
          ) {
            Icon(
              imageVector = if (viewMode == AppViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
              contentDescription = "Toggle Grid/List",
              tint = PrimaryPurple
            )
          }

          Box {
            IconButton(
              onClick = { onToggleSortMenu(true) },
              modifier = Modifier.testTag("sort_menu_button")
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort Apps",
                tint = TextSecondary
              )
            }

            DropdownMenu(
              expanded = showSortMenu,
              onDismissRequest = { onToggleSortMenu(false) }
            ) {
              DropdownMenuItem(
                text = { Text("Alphabetical (A-Z)") },
                onClick = {
                  onSortModeChange(AppSortMode.NAME_ASC)
                  onToggleSortMenu(false)
                }
              )
              DropdownMenuItem(
                text = { Text("Alphabetical (Z-A)") },
                onClick = {
                  onSortModeChange(AppSortMode.NAME_DESC)
                  onToggleSortMenu(false)
                }
              )
              DropdownMenuItem(
                text = { Text("Recently Updated") },
                onClick = {
                  onSortModeChange(AppSortMode.RECENTLY_UPDATED)
                  onToggleSortMenu(false)
                }
              )
            }
          }
        }
      }
    }

    // Filter Chips Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      FilterChip(
        selected = activeFilter == AppFilter.ALL,
        onClick = { onFilterChange(AppFilter.ALL) },
        label = { Text("All ($totalCount)", fontSize = 12.sp) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = PrimaryContainerLight,
          selectedLabelColor = PrimaryPurpleDark,
          containerColor = LightSurfaceContainer,
          labelColor = TextSecondary
        ),
        border = null,
        modifier = Modifier.testTag("filter_all_chip")
      )

      FilterChip(
        selected = activeFilter == AppFilter.USER_ONLY,
        onClick = { onFilterChange(AppFilter.USER_ONLY) },
        label = { Text("User Apps ($userCount)", fontSize = 12.sp) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = PrimaryContainerLight,
          selectedLabelColor = PrimaryPurpleDark,
          containerColor = LightSurfaceContainer,
          labelColor = TextSecondary
        ),
        border = null,
        modifier = Modifier.testTag("filter_user_chip")
      )

      FilterChip(
        selected = activeFilter == AppFilter.SYSTEM_ONLY,
        onClick = { onFilterChange(AppFilter.SYSTEM_ONLY) },
        label = { Text("System ($systemCount)", fontSize = 12.sp) },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = PrimaryContainerLight,
          selectedLabelColor = PrimaryPurpleDark,
          containerColor = LightSurfaceContainer,
          labelColor = TextSecondary
        ),
        border = null,
        modifier = Modifier.testTag("filter_system_chip")
      )
    }
  }
}

@Composable
private fun AppGridContent(
  apps: List<DiscoveredApp>,
  getBitmap: (DiscoveredApp) -> Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit
) {
  LazyVerticalGrid(
    columns = GridCells.Fixed(4),
    contentPadding = PaddingValues(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier
      .fillMaxSize()
      .testTag("app_discovery_grid")
  ) {
    items(
      items = apps,
      key = { it.id },
      contentType = { "app_grid_item" }
    ) { app ->
      AppGridItem(
        app = app,
        getBitmap = getBitmap,
        onLaunch = { onLaunchApp(app) }
      )
    }
  }
}

@Composable
private fun AppGridItem(
  app: DiscoveredApp,
  getBitmap: (DiscoveredApp) -> Bitmap?,
  onLaunch: () -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable { onLaunch() }
      .padding(vertical = 6.dp, horizontal = 2.dp)
      .testTag("app_item_${app.packageName}")
  ) {
    Box(
      modifier = Modifier
        .size(52.dp)
        .clip(RoundedCornerShape(14.dp)),
      contentAlignment = Alignment.Center
    ) {
      AsyncAppIcon(
        app = app,
        getBitmap = getBitmap,
        contentDescription = app.label,
        modifier = Modifier.size(48.dp)
      )
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = app.label,
      style = MaterialTheme.typography.bodySmall,
      fontWeight = FontWeight.Medium,
      color = TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      textAlign = TextAlign.Center,
      fontSize = 11.sp
    )

    if (app.isSystemApp) {
      Text(
        text = "SYS",
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        color = TextMuted,
        fontSize = 8.sp
      )
    }
  }
}

@Composable
private fun AppListContent(
  apps: List<DiscoveredApp>,
  getBitmap: (DiscoveredApp) -> Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit
) {
  LazyColumn(
    contentPadding = PaddingValues(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier
      .fillMaxSize()
      .testTag("app_discovery_list")
  ) {
    items(
      items = apps,
      key = { it.id },
      contentType = { "app_list_item" }
    ) { app ->
      AppListItem(
        app = app,
        getBitmap = getBitmap,
        onLaunch = { onLaunchApp(app) }
      )
    }
  }
}

@Composable
private fun AppListItem(
  app: DiscoveredApp,
  getBitmap: (DiscoveredApp) -> Bitmap?,
  onLaunch: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = LightSurfaceContainer,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable { onLaunch() }
      .testTag("app_item_${app.packageName}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncAppIcon(
        app = app,
        getBitmap = getBitmap,
        contentDescription = app.label,
        modifier = Modifier.size(44.dp)
      )

      Spacer(modifier = Modifier.width(14.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = app.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          if (app.isSystemApp) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = LightSurfaceContainerHigh
            ) {
              Text(
                text = "SYSTEM",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                fontSize = 8.sp,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }

        Text(
          text = app.packageName,
          style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
          fontSize = 10.sp,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }

      if (app.versionName.isNotEmpty()) {
        Text(
          text = "v${app.versionName}",
          style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
          fontSize = 9.sp,
          color = TextMuted
        )
      }
    }
  }
}

@Composable
private fun DiscoveryTelemetryView(uiState: AppDiscoveryUiState) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Launch Diagnostics Terminal Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkTerminalSurface),
        shape = RoundedCornerShape(24.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "LAUNCH_DIAGNOSTICS",
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
              color = DarkTerminalText.copy(alpha = 0.7f),
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "LauncherApps.startMainActivity",
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
              color = DarkTerminalAccent,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "> Recent Launch: ${uiState.recentLaunchLog}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = DarkTerminalAccent,
            fontSize = 11.sp
          )

          if (uiState.launchHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "--- Recent Launch History ---",
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
              color = DarkTerminalText.copy(alpha = 0.5f),
              fontSize = 9.sp
            )
            uiState.launchHistory.take(5).forEach { historyEntry ->
              Text(
                text = "> $historyEntry",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = DarkTerminalText,
                fontSize = 10.sp
              )
            }
          }
        }
      }
    }

    // Package Change Monitor Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkTerminalSurface),
        shape = RoundedCornerShape(24.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "PACKAGE_MONITOR",
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
              color = DarkTerminalText.copy(alpha = 0.7f),
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = "LauncherApps.Callback",
              style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
              color = DarkTerminalAccent,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "> Status: Dynamic package change callback active",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = DarkTerminalText,
            fontSize = 11.sp
          )
          Text(
            text = "> Total indexed: ${uiState.totalAppCount} apps (${uiState.userAppCount} user, ${uiState.systemAppCount} system)",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = DarkTerminalText,
            fontSize = 11.sp
          )
          Text(
            text = "> Latest Event: ${uiState.recentPackageEvent}",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = DarkTerminalAccent,
            fontSize = 11.sp
          )
        }
      }
    }

    // Acceptance Criteria Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LightSurfaceContainer),
        shape = RoundedCornerShape(20.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "PHASE 3 ACCEPTANCE CRITERIA",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryPurple,
            letterSpacing = 1.sp
          )
          Text(
            text = "1. Discovered launcher items initiate launch via LauncherApps.startMainActivity.\n2. UserHandle resolved at launch time from UserManager.\n3. Component verified against current Android state before launch.\n4. Graceful handling of unavailable/uninstalled/stale apps without crashing.\n5. Home key returns cleanly to Multi-Space Launcher.\n6. Live launch telemetry recorded in diagnostics log.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            lineHeight = 18.sp
          )
        }
      }
    }
  }
}

@Composable
fun AsyncAppIcon(
  app: DiscoveredApp,
  getBitmap: (DiscoveredApp) -> Bitmap?,
  contentDescription: String,
  modifier: Modifier = Modifier
) {
  var bitmap by remember(app.id) { mutableStateOf(getBitmap(app)) }

  LaunchedEffect(app.id) {
    if (bitmap == null) {
      withContext(Dispatchers.IO) {
        val loaded = getBitmap(app)
        withContext(Dispatchers.Main) {
          bitmap = loaded
        }
      }
    }
  }

  AppIconImage(
    bitmap = bitmap,
    contentDescription = contentDescription,
    modifier = modifier
  )
}

@Composable
fun AppIconImage(
  bitmap: Bitmap?,
  contentDescription: String,
  modifier: Modifier = Modifier
) {
  if (bitmap != null) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    Image(
      bitmap = imageBitmap,
      contentDescription = contentDescription,
      modifier = modifier
    )
  } else {
    FallbackAppIcon(contentDescription, modifier)
  }
}

@Composable
fun AppIconImage(
  drawable: Drawable?,
  contentDescription: String,
  modifier: Modifier = Modifier
) {
  if (drawable != null) {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
      AppIconImage(
        bitmap = drawable.bitmap,
        contentDescription = contentDescription,
        modifier = modifier
      )
    } else {
      val bitmap = remember(drawable) {
        drawableToBitmap(drawable)
      }
      AppIconImage(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier
      )
    }
  } else {
    FallbackAppIcon(contentDescription, modifier)
  }
}

@Composable
private fun FallbackAppIcon(contentDescription: String, modifier: Modifier) {
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

