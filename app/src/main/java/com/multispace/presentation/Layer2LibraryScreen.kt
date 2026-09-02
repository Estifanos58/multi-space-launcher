package com.multispace.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.ui.components.ModernCard
import com.multispace.ui.components.ModernDialogContainer
import com.multispace.ui.components.ModernEmptyState
import com.multispace.ui.components.ModernSearchField
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.QuantumViolet
import com.multispace.ui.theme.ShapeRoundLg
import com.multispace.ui.theme.ShapeRoundMd
import com.multispace.ui.theme.ShapeRoundSm

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Layer2LibraryScreen(
  space: Space,
  spaceApps: List<DiscoveredApp>,
  getBitmap: (DiscoveredApp) -> android.graphics.Bitmap?,
  onLaunchApp: (DiscoveredApp) -> Unit,
  onAddToHome: (DiscoveredApp) -> Unit,
  onAddToDock: (DiscoveredApp) -> Unit,
  onAppInfo: (DiscoveredApp) -> Unit,
  onCloseLayer2: () -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedAppForMenu by remember { mutableStateOf<DiscoveredApp?>(null) }

  val filteredApps = remember(spaceApps, searchQuery) {
    if (searchQuery.isBlank()) {
      spaceApps
    } else {
      val q = searchQuery.trim().lowercase()
      spaceApps.filter {
        it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
      }
    }
  }

  val iconSizeModifier = when (space.iconSize) {
    Space.ICON_SIZE_SMALL -> Modifier.size(44.dp)
    Space.ICON_SIZE_LARGE -> Modifier.size(62.dp)
    else -> Modifier.size(52.dp)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
      .padding(top = AppDimens.Spacing8)
      .testTag("layer2_library_screen")
  ) {
    // Top Bar with Back Arrow and Modern Search Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing4),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
          AppDimens.BorderThin,
          MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.size(44.dp)
      ) {
        IconButton(
          onClick = onCloseLayer2,
          modifier = Modifier.fillMaxSize().testTag("layer2_back_button")
        ) {
          Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back to Home",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(AppDimens.IconSm)
          )
        }
      }

      Spacer(modifier = Modifier.width(AppDimens.Spacing10))

      ModernSearchField(
        query = searchQuery,
        onQueryChange = { searchQuery = it },
        placeholder = "Search ${space.name} library...",
        modifier = Modifier.weight(1f).testTag("layer2_search_input")
      )
    }

    Spacer(modifier = Modifier.height(AppDimens.Spacing12))

    // Apps Grid
    if (filteredApps.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        ModernEmptyState(
          icon = Icons.Default.Search,
          title = if (searchQuery.isBlank()) "No apps in this Space" else "No matching apps found",
          description = if (searchQuery.isBlank()) "Configure app memberships to see apps here." else "Try searching with a different keyword.",
          actionText = if (searchQuery.isNotBlank()) "Clear Search" else null,
          onActionClick = { searchQuery = "" }
        )
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Fixed(space.gridColumns),
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = AppDimens.Spacing16)
          .testTag("layer2_apps_grid"),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16),
        contentPadding = PaddingValues(bottom = AppDimens.Spacing24)
      ) {
        items(filteredApps, key = { "${it.packageName}/${it.activityName}" }) { app ->
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .fillMaxWidth()
              .clip(ShapeRoundMd)
              .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = { selectedAppForMenu = app }
              )
              .padding(AppDimens.Spacing4)
              .testTag("layer2_app_${app.packageName}")
          ) {
            Box(
              modifier = iconSizeModifier
                .clip(ShapeRoundMd)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
              contentAlignment = Alignment.Center
            ) {
              val bitmap = getBitmap(app)
              if (bitmap != null) {
                Image(
                  bitmap = bitmap.asImageBitmap(),
                  contentDescription = app.label,
                  modifier = Modifier.fillMaxSize()
                )
              } else {
                Text(
                  text = app.label.take(1).uppercase(),
                  fontWeight = FontWeight.Bold,
                  color = QuantumViolet,
                  fontSize = 18.sp
                )
              }
            }

            if (space.labelVisibility) {
              Spacer(modifier = Modifier.height(AppDimens.Spacing4))
              Text(
                text = app.label,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }
        }
      }
    }
  }

  // App Action Menu Modern Dialog
  if (selectedAppForMenu != null) {
    val app = selectedAppForMenu!!
    ModernDialogContainer(
      title = app.label,
      subtitle = app.packageName,
      confirmButtonText = null,
      dismissButtonText = "Close",
      onDismissRequest = { selectedAppForMenu = null }
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
      ) {
        ModernCard(
          onClick = {
            onLaunchApp(app)
            selectedAppForMenu = null
          },
          modifier = Modifier.fillMaxWidth(),
          shape = ShapeRoundMd
        ) {
          Row(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = QuantumViolet)
            Spacer(modifier = Modifier.width(AppDimens.Spacing12))
            Text(
              "Launch Application",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        ModernCard(
          onClick = {
            onAddToHome(app)
            selectedAppForMenu = null
          },
          modifier = Modifier.fillMaxWidth().testTag("menu_add_to_home"),
          shape = ShapeRoundMd
        ) {
          Row(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Home, contentDescription = null, tint = QuantumViolet)
            Spacer(modifier = Modifier.width(AppDimens.Spacing12))
            Text(
              "Add to Home Screen",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        ModernCard(
          onClick = {
            onAddToDock(app)
            selectedAppForMenu = null
          },
          modifier = Modifier.fillMaxWidth().testTag("menu_add_to_dock"),
          shape = ShapeRoundMd
        ) {
          Row(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Dock, contentDescription = null, tint = QuantumViolet)
            Spacer(modifier = Modifier.width(AppDimens.Spacing12))
            Text(
              "Add to Dock",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        ModernCard(
          onClick = {
            onAppInfo(app)
            selectedAppForMenu = null
          },
          modifier = Modifier.fillMaxWidth(),
          shape = ShapeRoundMd
        ) {
          Row(
            modifier = Modifier.padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(AppDimens.Spacing12))
            Text(
              "App Information",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }
}
