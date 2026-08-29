package com.multispace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.Space
import com.multispace.ui.theme.*

/**
 * Dedicated Full Screen / Page for crafting a Custom Grid Format.
 * Provides a live interactive Phone Mockup preview and fine-grained controls
 * for column counts, icon sizes, and label visibility.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomGridScreen(
  initialColumns: Int,
  initialIconSize: String,
  initialLabelVisibility: Boolean,
  onNavigateBack: () -> Unit,
  onApplyCustomGrid: (columns: Int, iconSize: String, showLabels: Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  var columns by remember { mutableIntStateOf(initialColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)) }
  var iconSize by remember { mutableStateOf(initialIconSize) }
  var showLabels by remember { mutableStateOf(initialLabelVisibility) }

  val previewColors = listOf(
    Color(0xFF3F51B5), Color(0xFF009688), Color(0xFFFF5722),
    Color(0xFF9C27B0), Color(0xFF4CAF50), Color(0xFFFF9800),
    Color(0xFF2196F3), Color(0xFFE91E63), Color(0xFF607D8B),
    Color(0xFF00BCD4), Color(0xFF8BC34A), Color(0xFF673AB7),
    Color(0xFF795548), Color(0xFFFFC107), Color(0xFF03A9F4),
    Color(0xFFCDDC39), Color(0xFF3F51B5), Color(0xFF009688)
  )

  val sampleLabels = listOf(
    "Browser", "Camera", "Chat", "Files", "Gallery", "Maps",
    "Music", "Notes", "Phone", "Radio", "Store", "Tasks",
    "Video", "Wallet", "Weather", "Clock", "Health", "Games"
  )

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = LightBackground,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(
              text = "Custom Grid Layout",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Configure app columns and icon proportions",
              style = MaterialTheme.typography.labelSmall,
              color = TextSecondary
            )
          }
        },
        navigationIcon = {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("btn_back_custom_grid")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary
            )
          }
        },
        actions = {
          TextButton(
            onClick = {
              columns = Space.DEFAULT_GRID_COLUMNS
              iconSize = Space.ICON_SIZE_MEDIUM
              showLabels = true
            }
          ) {
            Text("Reset", color = PrimaryPurpleDark, fontWeight = FontWeight.SemiBold)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
      )
    },
    bottomBar = {
      Surface(
        color = LightSurfaceContainer,
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Cancel")
          }
          Button(
            onClick = {
              onApplyCustomGrid(columns, iconSize, showLabels)
            },
            modifier = Modifier
              .weight(1.5f)
              .testTag("btn_save_custom_grid"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Apply Grid Layout", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Live Interactive Phone Mockup Preview Card
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "LIVE SCREEN PREVIEW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f),
                letterSpacing = 1.sp
              )
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = Color.White.copy(alpha = 0.15f)
            ) {
              Text(
                text = "$columns Columns · $iconSize",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Phone bezel frame container
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(240.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xFF0F172A))
              .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
              .padding(10.dp)
          ) {
            val previewIconDp = when (iconSize) {
              Space.ICON_SIZE_SMALL -> (28 - (columns - 3) * 2).coerceAtLeast(18).dp
              Space.ICON_SIZE_LARGE -> (44 - (columns - 3) * 2).coerceAtLeast(26).dp
              else -> (36 - (columns - 3) * 2).coerceAtLeast(22).dp
            }

            LazyVerticalGrid(
              columns = GridCells.Fixed(columns),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.fillMaxSize(),
              userScrollEnabled = false
            ) {
              val sampleCount = (columns * 3).coerceAtMost(18)
              items(sampleCount) { index ->
                val color = previewColors[index % previewColors.size]
                val label = sampleLabels[index % sampleLabels.size]

                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Box(
                    modifier = Modifier
                      .size(previewIconDp)
                      .clip(RoundedCornerShape(8.dp))
                      .background(color),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      imageVector = when (index % 5) {
                        0 -> Icons.Default.Apps
                        1 -> Icons.Default.Favorite
                        2 -> Icons.Default.Star
                        3 -> Icons.Default.Mail
                        else -> Icons.Default.Folder
                      },
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size((previewIconDp.value * 0.5f).dp)
                    )
                  }

                  if (showLabels) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = label,
                      color = Color.White.copy(alpha = 0.85f),
                      fontSize = when {
                        columns >= 6 -> 7.sp
                        columns >= 5 -> 8.sp
                        else -> 9.sp
                      },
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      textAlign = TextAlign.Center
                    )
                  }
                }
              }
            }
          }
        }
      }

      // 2. Column Count Controls Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Columns Count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                text = "Number of horizontal app slots per row",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
              )
            }
            Surface(
              shape = CircleShape,
              color = PrimaryContainerLight,
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "$columns",
                  color = PrimaryPurpleDark,
                  fontWeight = FontWeight.ExtraBold,
                  fontSize = 16.sp
                )
              }
            }
          }

          Slider(
            value = columns.toFloat(),
            onValueChange = { columns = it.toInt() },
            valueRange = Space.MIN_GRID_COLUMNS.toFloat()..Space.MAX_GRID_COLUMNS.toFloat(),
            steps = (Space.MAX_GRID_COLUMNS - Space.MIN_GRID_COLUMNS) - 1,
            colors = SliderDefaults.colors(
              thumbColor = PrimaryPurple,
              activeTrackColor = PrimaryPurpleDark
            ),
            modifier = Modifier.testTag("slider_custom_grid_columns")
          )

          // Quick selection chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            for (c in Space.MIN_GRID_COLUMNS..Space.MAX_GRID_COLUMNS) {
              val isSelected = columns == c
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) PrimaryPurple else LightSurfaceContainerHigh,
                modifier = Modifier
                  .weight(1f)
                  .clickable { columns = c }
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.padding(vertical = 8.dp)
                ) {
                  Text(
                    text = "$c",
                    color = if (isSelected) Color.White else TextPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                  )
                }
              }
            }
          }
        }
      }

      // 3. Icon Size Selection Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "App Icon Size",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val sizes = listOf(
              Space.ICON_SIZE_SMALL to ("Small" to "40dp (Dense)"),
              Space.ICON_SIZE_MEDIUM to ("Medium" to "48dp (Balanced)"),
              Space.ICON_SIZE_LARGE to ("Large" to "56dp (Roomy)")
            )

            sizes.forEach { (sizeKey, meta) ->
              val isSelected = iconSize == sizeKey
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) PrimaryContainerLight else LightSurfaceContainerHigh,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, PrimaryPurple) else null,
                modifier = Modifier
                  .weight(1f)
                  .clickable { iconSize = sizeKey }
                  .testTag("chip_icon_size_$sizeKey")
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Text(
                    text = meta.first,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) PrimaryPurpleDark else TextPrimary
                  )
                  Text(
                    text = meta.second,
                    fontSize = 10.sp,
                    color = if (isSelected) PrimaryPurpleDark else TextSecondary,
                    textAlign = TextAlign.Center
                  )
                }
              }
            }
          }
        }
      }

      // 4. App Name Label Visibility Card
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LightSurfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Show App Labels",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Display app name text beneath each application icon",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary
            )
          }

          Switch(
            checked = showLabels,
            onCheckedChange = { showLabels = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = PrimaryPurple
            ),
            modifier = Modifier.testTag("switch_show_labels")
          )
        }
      }
    }
  }
}
