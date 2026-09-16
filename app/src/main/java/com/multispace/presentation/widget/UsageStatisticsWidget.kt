package com.multispace.presentation.widget

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.AppLaunchCount
import com.multispace.domain.model.DailyUsage
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceUsageStats
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.ShapeRoundLg
import com.multispace.ui.theme.ShapeRoundSm

enum class UsageWidgetMode {
  TOP_APPS,
  WEEKLY_CHART
}

/**
 * Modern, intentional, and compact Material 3 Usage Statistics Widget for Layer 1.
 * Provides a seamless toggle between Top 3 Most Used Apps and a 7-day Weekly Bar Chart.
 */
@Composable
fun UsageStatisticsWidget(
  stats: SpaceUsageStats?,
  space: Space,
  spanX: Int = 2,
  spanY: Int = 2,
  getBitmap: ((DiscoveredApp) -> Bitmap?)? = null,
  onLaunchApp: ((DiscoveredApp) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  var viewMode by rememberSaveable(space.id) { mutableStateOf(UsageWidgetMode.TOP_APPS) }
  val isVeryCompact = spanY == 1

  val totalLaunches = stats?.totalLaunches ?: 0
  val launchesToday = stats?.launchesToday ?: 0
  val launches7d = stats?.launchesLast7Days ?: 0
  val topApps = stats?.topApps?.take(3) ?: emptyList()
  val weeklyLaunches = stats?.weeklyLaunches ?: emptyList()

  Box(
    modifier = modifier
      .fillMaxSize()
      .clip(ShapeRoundLg)
      .background(
        Brush.linearGradient(
          listOf(
            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f)
          )
        )
      )
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        shape = ShapeRoundLg
      )
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("widget_usage_stats_${space.id}")
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // 1. Header Row
      UsageWidgetHeader(
        spaceName = space.name,
        currentMode = viewMode,
        onToggleMode = {
          viewMode = if (viewMode == UsageWidgetMode.TOP_APPS) {
            UsageWidgetMode.WEEKLY_CHART
          } else {
            UsageWidgetMode.TOP_APPS
          }
        }
      )

      // 2. Main Animated Content
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        AnimatedContent(
          targetState = viewMode,
          transitionSpec = {
            if (targetState == UsageWidgetMode.WEEKLY_CHART) {
              (slideInHorizontally { it / 2 } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally { -it / 2 } + fadeOut(tween(180)))
            } else {
              (slideInHorizontally { -it / 2 } + fadeIn(tween(220))) togetherWith
                (slideOutHorizontally { it / 2 } + fadeOut(tween(180)))
            }
          },
          label = "UsageWidgetTransition"
        ) { mode ->
          when (mode) {
            UsageWidgetMode.TOP_APPS -> {
              TopAppsView(
                topApps = topApps,
                isVeryCompact = isVeryCompact,
                getBitmap = getBitmap,
                onLaunchApp = onLaunchApp
              )
            }
            UsageWidgetMode.WEEKLY_CHART -> {
              WeeklyChartView(
                weeklyLaunches = weeklyLaunches,
                isVeryCompact = isVeryCompact,
                totalWeekLaunches = launches7d
              )
            }
          }
        }
      }

      // 3. Compact Footer Summary
      UsageWidgetFooter(
        totalLaunches = totalLaunches,
        launchesToday = launchesToday,
        mostUsedApp = stats?.mostUsedApp?.label
      )
    }
  }
}

@Composable
private fun UsageWidgetHeader(
  spaceName: String,
  currentMode: UsageWidgetMode,
  onToggleMode: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    // Left: Insights icon + "Usage" + Space badge
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
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
          contentDescription = "Usage",
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(13.dp)
        )
      }

      Text(
        text = "Usage",
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.3.sp
        ),
        color = MaterialTheme.colorScheme.onSurface
      )

      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
      ) {
        Text(
          text = spaceName,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
      }
    }

    // Right: Compact Toggle Button
    IconButton(
      onClick = onToggleMode,
      modifier = Modifier
        .size(26.dp)
        .testTag("usage_widget_toggle_view")
    ) {
      Icon(
        imageVector = if (currentMode == UsageWidgetMode.TOP_APPS) {
          Icons.Default.BarChart
        } else {
          Icons.Default.FormatListNumbered
        },
        contentDescription = if (currentMode == UsageWidgetMode.TOP_APPS) {
          "Switch to Weekly Chart view"
        } else {
          "Switch to Top Apps view"
        },
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}

@Composable
private fun TopAppsView(
  topApps: List<AppLaunchCount>,
  isVeryCompact: Boolean,
  getBitmap: ((DiscoveredApp) -> Bitmap?)?,
  onLaunchApp: ((DiscoveredApp) -> Unit)?
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .testTag("usage_widget_top_apps_view"),
    contentAlignment = Alignment.Center
  ) {
    if (topApps.isEmpty()) {
      Text(
        text = "No launches recorded yet",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
      )
    } else {
      val maxLaunches = topApps.maxOfOrNull { it.launchCount }?.coerceAtLeast(1) ?: 1
      val displayCount = if (isVeryCompact) 1 else minOf(3, topApps.size)

      Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
      ) {
        for (i in 0 until displayCount) {
          val item = topApps[i]
          TopAppRow(
            rank = i + 1,
            item = item,
            maxLaunches = maxLaunches,
            getBitmap = getBitmap,
            onClick = { onLaunchApp?.invoke(item.app) },
            testTag = "usage_widget_app_row_$i"
          )
        }
      }
    }
  }
}

@Composable
private fun TopAppRow(
  rank: Int,
  item: AppLaunchCount,
  maxLaunches: Int,
  getBitmap: ((DiscoveredApp) -> Bitmap?)?,
  onClick: () -> Unit,
  testTag: String
) {
  val iconBitmap = getBitmap?.invoke(item.app)
  val relativeFraction = (item.launchCount.toFloat() / maxLaunches).coerceIn(0.08f, 1f)

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .clickable { onClick() }
      .padding(vertical = 1.dp)
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // Rank
      Text(
        text = "$rank",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(10.dp)
      )

      // App Icon
      if (iconBitmap != null) {
        Image(
          bitmap = iconBitmap.asImageBitmap(),
          contentDescription = item.app.label,
          modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
        )
      } else {
        Box(
          modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = item.app.label.take(1).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
          )
        }
      }

      // App Name
      Text(
        text = item.app.label,
        style = MaterialTheme.typography.bodySmall.copy(
          fontWeight = FontWeight.Medium,
          fontSize = 12.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f)
      )

      // Launch Count
      Text(
        text = "${item.launchCount}",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        ),
        color = MaterialTheme.colorScheme.primary
      )
    }

    // Relative Usage Progress Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(3.dp)
        .padding(start = 36.dp, top = 1.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(fraction = relativeFraction)
          .fillMaxHeight()
          .clip(CircleShape)
          .background(
            if (rank == 1) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary
          )
      )
    }
  }
}

@Composable
private fun WeeklyChartView(
  weeklyLaunches: List<DailyUsage>,
  isVeryCompact: Boolean,
  totalWeekLaunches: Int
) {
  val maxCount = weeklyLaunches.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
  val totalWeekly = if (weeklyLaunches.isNotEmpty()) weeklyLaunches.sumOf { it.count } else totalWeekLaunches

  Column(
    modifier = Modifier
      .fillMaxSize()
      .testTag("usage_widget_chart_view"),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Chart Subtitle Row: "Last 7 days" + Total
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Last 7 days",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 11.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
      Text(
        text = "$totalWeekly launches",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.testTag("usage_widget_chart_total")
      )
    }

    // 7-day Bars Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .padding(vertical = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom
    ) {
      val days = if (weeklyLaunches.size == 7) {
        weeklyLaunches
      } else {
        listOf("M", "T", "W", "T", "F", "S", "S").mapIndexed { idx, label ->
          DailyUsage(dayLabel = label, dateMillis = 0L, count = 0, isToday = idx == 6)
        }
      }

      for (day in days) {
        val fraction = if (maxCount > 0) (day.count.toFloat() / maxCount).coerceIn(0f, 1f) else 0f
        val animatedFraction by animateFloatAsState(
          targetValue = fraction,
          animationSpec = tween(350),
          label = "barAnimation"
        )
        val isPeak = day.count == maxCount && day.count > 0

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.weight(1f)
        ) {
          // Value Indicator on Highest/Active bar
          if (!isVeryCompact) {
            Text(
              text = if (day.count > 0 && (isPeak || day.isToday)) "${day.count}" else "",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              ),
              color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1
            )
          }

          // Bar Graphic
          Box(
            modifier = Modifier
              .width(12.dp)
              .fillMaxHeight(fraction = if (isVeryCompact) 0.65f else 0.6f),
            contentAlignment = Alignment.BottomCenter
          ) {
            // Background track
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            )
            // Foreground bar
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction = animatedFraction.coerceAtLeast(0.06f))
                .clip(RoundedCornerShape(3.dp))
                .background(
                  when {
                    day.isToday -> MaterialTheme.colorScheme.primary
                    isPeak -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                  }
                )
            )
          }

          // Day Label
          Text(
            text = day.dayLabel.take(2),
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun UsageWidgetFooter(
  totalLaunches: Int,
  launchesToday: Int,
  mostUsedApp: String?
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(ShapeRoundSm)
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
      .padding(horizontal = 6.dp, vertical = 2.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = "$totalLaunches launches • $launchesToday today",
      style = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
      ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.testTag("usage_widget_total_launches")
    )

    if (!mostUsedApp.isNullOrEmpty() && mostUsedApp != "None") {
      Text(
        text = "Top: $mostUsedApp",
        style = MaterialTheme.typography.labelSmall.copy(
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.testTag("usage_widget_most_used_app")
      )
    }
  }
}

