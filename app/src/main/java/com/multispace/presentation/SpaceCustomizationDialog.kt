package com.multispace.presentation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space

// Curated solid background color presets (ARGB Long values)
val PRESET_BACKGROUND_COLORS = listOf(
  0xFF0F172AL to "Midnight Navy",
  0xFF1E293BL to "Slate Charcoal",
  0xFF14532DL to "Forest Pine",
  0xFF312E81L to "Royal Indigo",
  0xFF7F1D1DL to "Crimson Rust",
  0xFF78350FL to "Sunset Amber",
  0xFF134E4AL to "Deep Teal",
  0xFF121212L to "Pure Dark",
  0xFF374151L to "Cool Gray",
  0xFF4C1D95L to "Deep Purple"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceCustomizationDialog(
  space: Space,
  spaceApps: List<DiscoveredApp> = emptyList(),
  onDismiss: () -> Unit,
  onSave: (
    backgroundType: String,
    backgroundColor: Long?,
    backgroundImageUri: String?,
    gridColumns: Int,
    iconSize: String,
    labelVisibility: Boolean
  ) -> Unit,
  onReorderApp: (DiscoveredApp, Int) -> Unit = { _, _ -> },
  onSortAlphabetically: () -> Unit = {}
) {
  val context = LocalContext.current

  var selectedBgType by remember { mutableStateOf(space.backgroundType) }
  var selectedBgColor by remember { mutableStateOf(space.backgroundColor ?: PRESET_BACKGROUND_COLORS.first().first) }
  var selectedImageUri by remember { mutableStateOf(space.backgroundImageUri) }
  var selectedGridColumns by remember { mutableIntStateOf(space.gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)) }
  var selectedIconSize by remember { mutableStateOf(space.iconSize) }
  var selectedLabelVisibility by remember { mutableStateOf(space.labelVisibility) }

  var activeTab by remember { mutableIntStateOf(0) } // 0: Background, 1: Layout & Grid, 2: App Ordering

  val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        context.contentResolver.takePersistableUriPermission(
          uri,
          Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
      } catch (_: Exception) {}
      selectedImageUri = uri.toString()
      selectedBgType = Space.BACKGROUND_IMAGE
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(max = 640.dp)
        .testTag("space_customization_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        // Header
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Customize Space",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = space.name,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close")
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs
        SecondaryTabRow(
          selectedTabIndex = activeTab,
          modifier = Modifier.fillMaxWidth()
        ) {
          Tab(
            selected = activeTab == 0,
            onClick = { activeTab = 0 },
            text = { Text("Background", maxLines = 1) },
            icon = { Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp)) }
          )
          Tab(
            selected = activeTab == 1,
            onClick = { activeTab = 1 },
            text = { Text("Grid & Icons", maxLines = 1) },
            icon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) }
          )
          Tab(
            selected = activeTab == 2,
            onClick = { activeTab = 2 },
            text = { Text("App Order", maxLines = 1) },
            icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(18.dp)) }
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          when (activeTab) {
            0 -> BackgroundCustomizationTab(
              selectedBgType = selectedBgType,
              selectedBgColor = selectedBgColor,
              selectedImageUri = selectedImageUri,
              onSelectBgType = { selectedBgType = it },
              onSelectBgColor = { selectedBgColor = it },
              onPickImage = { imagePickerLauncher.launch("image/*") },
              onClearImage = {
                selectedImageUri = null
                selectedBgType = Space.BACKGROUND_DEFAULT
              }
            )
            1 -> LayoutCustomizationTab(
              gridColumns = selectedGridColumns,
              iconSize = selectedIconSize,
              labelVisibility = selectedLabelVisibility,
              onGridColumnsChange = { selectedGridColumns = it },
              onIconSizeChange = { selectedIconSize = it },
              onLabelVisibilityChange = { selectedLabelVisibility = it }
            )
            2 -> AppOrderingTab(
              spaceApps = spaceApps,
              onReorder = onReorderApp,
              onSortAlphabetically = onSortAlphabetically
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions
        Row(
          horizontalArrangement = Arrangement.End,
          modifier = Modifier.fillMaxWidth()
        ) {
          TextButton(onClick = onDismiss) {
            Text("Cancel")
          }
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              onSave(
                selectedBgType,
                if (selectedBgType == Space.BACKGROUND_COLOR) selectedBgColor else null,
                if (selectedBgType == Space.BACKGROUND_IMAGE) selectedImageUri else null,
                selectedGridColumns,
                selectedIconSize,
                selectedLabelVisibility
              )
              onDismiss()
            },
            modifier = Modifier.testTag("save_customization_button")
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Apply Customization")
          }
        }
      }
    }
  }
}

@Composable
private fun BackgroundCustomizationTab(
  selectedBgType: String,
  selectedBgColor: Long,
  selectedImageUri: String?,
  onSelectBgType: (String) -> Unit,
  onSelectBgColor: (Long) -> Unit,
  onPickImage: () -> Unit,
  onClearImage: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    Text(
      text = "Background Style",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Background Type Selector
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      FilterChip(
        selected = selectedBgType == Space.BACKGROUND_DEFAULT,
        onClick = { onSelectBgType(Space.BACKGROUND_DEFAULT) },
        label = { Text("Default") },
        leadingIcon = if (selectedBgType == Space.BACKGROUND_DEFAULT) {
          { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        modifier = Modifier.weight(1f)
      )
      FilterChip(
        selected = selectedBgType == Space.BACKGROUND_COLOR,
        onClick = { onSelectBgType(Space.BACKGROUND_COLOR) },
        label = { Text("Solid Color") },
        leadingIcon = if (selectedBgType == Space.BACKGROUND_COLOR) {
          { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        modifier = Modifier.weight(1.1f)
      )
      FilterChip(
        selected = selectedBgType == Space.BACKGROUND_IMAGE,
        onClick = { onSelectBgType(Space.BACKGROUND_IMAGE) },
        label = { Text("Photo") },
        leadingIcon = if (selectedBgType == Space.BACKGROUND_IMAGE) {
          { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
        } else null,
        modifier = Modifier.weight(0.9f)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    when (selectedBgType) {
      Space.BACKGROUND_DEFAULT -> {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Icon(
              imageVector = Icons.Default.BrightnessAuto,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "System Theme Background",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Uses the standard launcher dark/light color scheme.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
      Space.BACKGROUND_COLOR -> {
        Text(
          text = "Choose Palette Color",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          PRESET_BACKGROUND_COLORS.chunked(5).forEach { rowColors ->
            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              rowColors.forEach { (colorValue, name) ->
                val isSelected = selectedBgColor == colorValue
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(colorValue))
                    .border(
                      width = if (isSelected) 3.dp else 1.dp,
                      color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.3f),
                      shape = CircleShape
                    )
                    .clickable { onSelectBgColor(colorValue) }
                ) {
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = name,
                      tint = Color.White,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected color preview
        val currentColorName = PRESET_BACKGROUND_COLORS.firstOrNull { it.first == selectedBgColor }?.second ?: "Custom"
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = Color(selectedBgColor),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
          ) {
            Text(
              text = "Selected: $currentColorName",
              color = Color.White,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
      Space.BACKGROUND_IMAGE -> {
        Text(
          text = "Custom Image Wallpaper",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (!selectedImageUri.isNullOrEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
          ) {
            AsyncImage(
              model = selectedImageUri,
              contentDescription = "Wallpaper Preview",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
            IconButton(
              onClick = onClearImage,
              modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .size(32.dp)
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Remove Photo", tint = Color.White, modifier = Modifier.size(16.dp))
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
        }

        OutlinedButton(
          onClick = onPickImage,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(if (selectedImageUri.isNullOrEmpty()) "Pick Image from Device" else "Change Image")
        }
      }
    }
  }
}

@Composable
private fun LayoutCustomizationTab(
  gridColumns: Int,
  iconSize: String,
  labelVisibility: Boolean,
  onGridColumnsChange: (Int) -> Unit,
  onIconSizeChange: (String) -> Unit,
  onLabelVisibilityChange: (Boolean) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
  ) {
    // Grid Columns
    Text(
      text = "Grid Columns",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold
    )
    Text(
      text = "Number of app columns on the Home screen",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      listOf(3, 4, 5, 6).forEach { cols ->
        FilterChip(
          selected = gridColumns == cols,
          onClick = { onGridColumnsChange(cols) },
          label = { Text("$cols Cols") },
          leadingIcon = if (gridColumns == cols) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
          } else null,
          modifier = Modifier.weight(1f)
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Icon Size
    Text(
      text = "Icon Size",
      style = MaterialTheme.typography.titleSmall,
      fontWeight = FontWeight.SemiBold
    )
    Text(
      text = "Scaling of app icons in the grid",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      listOf(
        Space.ICON_SIZE_SMALL to "Small",
        Space.ICON_SIZE_MEDIUM to "Medium",
        Space.ICON_SIZE_LARGE to "Large"
      ).forEach { (sizeKey, label) ->
        FilterChip(
          selected = iconSize == sizeKey,
          onClick = { onIconSizeChange(sizeKey) },
          label = { Text(label) },
          leadingIcon = if (iconSize == sizeKey) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
          } else null,
          modifier = Modifier.weight(1f)
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // App Labels Toggle
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Show App Labels",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Display text names below icons on Home",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = labelVisibility,
          onCheckedChange = onLabelVisibilityChange
        )
      }
    }
  }
}

@Composable
private fun AppOrderingTab(
  spaceApps: List<DiscoveredApp>,
  onReorder: (DiscoveredApp, Int) -> Unit,
  onSortAlphabetically: () -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = "App Order (${spaceApps.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )
      if (spaceApps.size > 1) {
        TextButton(onClick = onSortAlphabetically) {
          Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Sort A-Z")
        }
      }
    }

    if (spaceApps.isEmpty()) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp)
      ) {
        Text(
          text = "No applications assigned to this Space yet.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        itemsIndexed(spaceApps) { index, app ->
          Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(
                text = "${index + 1}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(24.dp)
              )
              Text(
                text = app.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
              )
              IconButton(
                onClick = { onReorder(app, -1) },
                enabled = index > 0,
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", modifier = Modifier.size(20.dp))
              }
              IconButton(
                onClick = { onReorder(app, 1) },
                enabled = index < spaceApps.size - 1,
                modifier = Modifier.size(32.dp)
              ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", modifier = Modifier.size(20.dp))
              }
            }
          }
        }
      }
    }
  }
}
