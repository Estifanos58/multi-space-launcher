package com.multispace.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceUsageStats
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.ShapeRoundSm

/**
 * Polished Material 3 Usage Statistics Widget for Layer 1.
 * Reflects the active Space's usage metrics in a compact, responsive card.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UsageStatisticsWidget(
  stats: SpaceUsageStats?,
  space: Space,
  spanX: Int = 2,
  spanY: Int = 2,
  modifier: Modifier = Modifier
) {
  val totalLaunches = stats?.totalLaunches ?: 0
  val launchesToday = stats?.launchesToday ?: 0
  val launches7d = stats?.launchesLast7Days ?: 0
  val launches30d = stats?.launchesLast30Days ?: 0
  val uniqueApps = stats?.uniqueAppsCount ?: 0
  val topAppName = stats?.mostUsedApp?.label ?: "None"

  val isVeryCompact = spanY == 1

  Box(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing10)
      .testTag("widget_usage_stats_${space.id}")
  ) {
    if (isVeryCompact) {
      // 1-Row Compact layout
      Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
        ) {
          Box(
            modifier = Modifier
              .size(28.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Insights,
              contentDescription = "Usage",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
          }

          Column {
            Text(
              text = "Usage",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
              text = "$totalLaunches launches",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.testTag("usage_widget_total_launches")
            )
          }
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = "Today • $launchesToday",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("usage_widget_today")
          )
          Text(
            text = "Top: $topAppName",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("usage_widget_most_used_app")
          )
        }
      }
    } else {
      // Standard 2x2 or larger multi-row layout
      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Header Row: Title & Space Badge
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing6)
          ) {
            Box(
              modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Insights,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(13.dp)
              )
            }

            Text(
              text = "Usage",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Subtle Space Tag
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
          ) {
            Text(
              text = space.name,
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        // Primary Metric
        Column(modifier = Modifier.padding(vertical = 2.dp)) {
          Text(
            text = "$totalLaunches launches",
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag("usage_widget_total_launches")
          )
        }

        // Metrics Breakdown Row (Today • 7d • 30d • Apps)
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
          verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
        ) {
          UsageMetricItem(label = "Today", value = launchesToday.toString(), testTag = "usage_widget_today")
          UsageMetricItem(label = "7d", value = launches7d.toString(), testTag = "usage_widget_7d")
          UsageMetricItem(label = "30d", value = launches30d.toString(), testTag = "usage_widget_30d")
          UsageMetricItem(label = "Apps", value = uniqueApps.toString(), testTag = "usage_widget_unique_apps")
        }

        // Most Used App Footer
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeRoundSm)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = AppDimens.Spacing6, vertical = AppDimens.Spacing4),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Most used: ",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = topAppName,
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("usage_widget_most_used_app")
          )
        }
      }
    }
  }
}

@Composable
private fun UsageMetricItem(
  label: String,
  value: String,
  testTag: String
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(3.dp),
    modifier = Modifier.testTag(testTag)
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    )
    Text(
      text = "•",
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    )
    Text(
      text = value,
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      ),
      color = MaterialTheme.colorScheme.onSurface
    )
  }
}
