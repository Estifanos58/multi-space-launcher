package com.multispace.presentation

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.multispace.domain.model.AppUsageStats
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceUsageStats
import com.multispace.domain.model.appIdentity
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.ShapeRoundLg
import com.multispace.ui.theme.ShapeRoundMd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Clean Material 3 dialog presenting Space-specific application usage analytics.
 * Fully scoped to the active Space.
 */
@Composable
fun SpaceUsageStatsDialog(
  space: Space,
  discoveryViewModel: AppDiscoveryViewModel,
  getBitmap: (DiscoveredApp) -> Bitmap?,
  onDismiss: () -> Unit
) {
  var isLoading by remember { mutableStateOf(true) }
  var spaceStats by remember { mutableStateOf<SpaceUsageStats?>(null) }
  var mostUsedList by remember { mutableStateOf<List<DiscoveredApp>>(emptyList()) }
  var selectedAppStats by remember { mutableStateOf<AppUsageStats?>(null) }
  var expandedAppPackage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(space.id) {
    isLoading = true
    try {
      val stats = discoveryViewModel.getSpaceUsageStats(space.id)
      spaceStats = stats
      mostUsedList = discoveryViewModel.getMostUsedApps(space.id, discoveryViewModel.uiState.value.allApps, limit = 20)
    } finally {
      isLoading = false
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth(0.94f)
      .testTag("space_usage_stats_dialog"),
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Insights,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "${space.name} Analytics",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = "Space-isolated usage tracking",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(32.dp).testTag("stats_dialog_close_button")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            modifier = Modifier.size(18.dp)
          )
        }
      }
    },
    text = {
      if (isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
      } else {
        val stats = spaceStats
        Column(modifier = Modifier.fillMaxWidth()) {
          // 1. Overview summary metrics cards
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            StatMetricCard(
              title = "Total Launches",
              value = "${stats?.totalLaunches ?: 0}",
              modifier = Modifier
                .weight(1f)
                .testTag("stats_total_launches")
            )
            StatMetricCard(
              title = "Unique Apps",
              value = "${stats?.uniqueAppsCount ?: 0}",
              modifier = Modifier
                .weight(1f)
                .testTag("stats_unique_apps")
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            StatMetricCard(
              title = "Today",
              value = "${stats?.launchesToday ?: 0}",
              modifier = Modifier
                .weight(1f)
                .testTag("stats_launches_today")
            )
            StatMetricCard(
              title = "Last 7 Days",
              value = "${stats?.launchesLast7Days ?: 0}",
              modifier = Modifier
                .weight(1f)
                .testTag("stats_launches_7days")
            )
            StatMetricCard(
              title = "Last 30 Days",
              value = "${stats?.launchesLast30Days ?: 0}",
              modifier = Modifier
                .weight(1f)
                .testTag("stats_launches_30days")
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // 2. Most Used Apps Section
          Text(
            text = "Most Used in this Space",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(8.dp))

          if (mostUsedList.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "No apps launched in ${space.name} yet.\nLaunches in other spaces do not affect this space.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
              )
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
            ) {
              items(mostUsedList, key = { it.id }) { app ->
                val isExpanded = expandedAppPackage == app.packageName

                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clip(ShapeRoundMd)
                    .background(
                      if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
                    .clickable {
                      if (isExpanded) {
                        expandedAppPackage = null
                        selectedAppStats = null
                      } else {
                        expandedAppPackage = app.packageName
                        // Fetch per-app stats
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).run {
                          // Handled via LaunchedEffect below
                        }
                      }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("stats_app_item_${app.packageName}")
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    val bitmap = remember(app.id) { getBitmap(app) }
                    ThemedAppIcon(
                      app = app,
                      bitmap = bitmap,
                      appTheme = space.appTheme,
                      modifier = Modifier.size(32.dp),
                      fallbackText = app.label.take(1).uppercase()
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }

                    Icon(
                      imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                      contentDescription = null,
                      modifier = Modifier.size(20.dp),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  // Expanded Per-App Detail Stats
                  AnimatedVisibility(visible = isExpanded) {
                    AppDetailStatsView(
                      app = app,
                      spaceId = space.id,
                      discoveryViewModel = discoveryViewModel
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}

@Composable
private fun StatMetricCard(
  title: String,
  value: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.bodySmall,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
private fun AppDetailStatsView(
  app: DiscoveredApp,
  spaceId: String,
  discoveryViewModel: AppDiscoveryViewModel
) {
  var appStats by remember { mutableStateOf<AppUsageStats?>(null) }
  val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

  LaunchedEffect(app.id, spaceId) {
    appStats = discoveryViewModel.getAppUsageStats(spaceId, app.appIdentity, app)
  }

  val stats = appStats
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp, start = 4.dp, end = 4.dp)
  ) {
    HorizontalDivider(
      thickness = 0.5.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
    Spacer(modifier = Modifier.height(6.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "Total in ${spaceId}:",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = "${stats?.launchCount ?: 0} launches",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "Today / 7d / 30d:",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = "${stats?.launchesToday ?: 0} / ${stats?.launchesLast7Days ?: 0} / ${stats?.launchesLast30Days ?: 0}",
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    if (stats?.firstLaunchTimestamp != null && stats.firstLaunchTimestamp > 0) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "First Launched:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = dateFormat.format(Date(stats.firstLaunchTimestamp)),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }

    if (stats?.lastLaunchTimestamp != null && stats.lastLaunchTimestamp > 0) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Last Launched:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = dateFormat.format(Date(stats.lastLaunchTimestamp)),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}
