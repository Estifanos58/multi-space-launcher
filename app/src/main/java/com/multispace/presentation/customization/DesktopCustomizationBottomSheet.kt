package com.multispace.presentation.customization

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.domain.model.WallpaperCatalog
import com.multispace.domain.model.WallpaperImagePreset
import com.multispace.platform.AppWidgetGroup
import com.multispace.platform.AppWidgetHostManager
import com.multispace.platform.WidgetItemInfo
import com.multispace.presentation.PRESET_BACKGROUND_COLORS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopCustomizationBottomSheet(
  section: CustomizationSection,
  space: Space,
  spaceApps: List<DiscoveredApp>,
  placements: List<SpaceItemPlacement>,
  currentPageIndex: Int,
  onDismiss: () -> Unit,
  onNavigateToPage: (Int) -> Unit,
  onCreatePage: () -> Unit,
  onDeletePage: (Int) -> Unit,
  onApplyWallpaper: (type: String, color: Long?, uri: String?) -> Unit,
  onApplyTheme: (theme: String, iconSize: String, labelVisibility: Boolean, gridColumns: Int) -> Unit,
  onAddWidget: (WidgetItemInfo) -> Unit,
  modifier: Modifier = Modifier
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
  var selectedWidgetGroup by remember { mutableStateOf<AppWidgetGroup?>(null) }

  // Hierarchical back handling
  BackHandler(enabled = true) {
    if (selectedWidgetGroup != null) {
      selectedWidgetGroup = null
    } else {
      onDismiss()
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 6.dp,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.70f)
    ) {
      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        // Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = {
              if (selectedWidgetGroup != null) {
                selectedWidgetGroup = null
              } else {
                onDismiss()
              }
            },
            modifier = Modifier.testTag("customization_sheet_back_btn")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }

          val headerTitle = when (section) {
            CustomizationSection.WALLPAPER -> "Wallpapers"
            CustomizationSection.THEME -> "Space Theme"
            CustomizationSection.PAGE_CONTROL -> "Page Control"
            CustomizationSection.WIDGETS -> selectedWidgetGroup?.app?.label ?: "Widgets"
            CustomizationSection.NONE -> ""
          }

          Text(
            text = headerTitle,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 8.dp)
          )

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("customization_sheet_close_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        HorizontalDivider(
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
          thickness = 1.dp
        )

        // Content body based on active section
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          when (section) {
            CustomizationSection.WALLPAPER -> {
              WallpaperSectionContent(
                space = space,
                onApply = { type, color, uri ->
                  onApplyWallpaper(type, color, uri)
                  onDismiss()
                }
              )
            }
            CustomizationSection.THEME -> {
              ThemeSectionContent(
                space = space,
                onApply = { theme, iconSize, labelVisibility, gridColumns ->
                  onApplyTheme(theme, iconSize, labelVisibility, gridColumns)
                  onDismiss()
                }
              )
            }
            CustomizationSection.PAGE_CONTROL -> {
              PageControlSectionContent(
                space = space,
                placements = placements,
                currentPageIndex = currentPageIndex,
                onNavigateToPage = { page ->
                  onNavigateToPage(page)
                  onDismiss()
                },
                onCreatePage = onCreatePage,
                onDeletePage = onDeletePage
              )
            }
            CustomizationSection.WIDGETS -> {
              val currentGroup = selectedWidgetGroup
              if (currentGroup == null) {
                WidgetsListContent(
                  spaceApps = spaceApps,
                  onSelectGroup = { group -> selectedWidgetGroup = group }
                )
              } else {
                WidgetDetailsContent(
                  group = currentGroup,
                  onAddWidget = { widget ->
                    onAddWidget(widget)
                    onDismiss()
                  }
                )
              }
            }
            CustomizationSection.NONE -> {}
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// 1. WALLPAPER SECTION
// ---------------------------------------------------------------------------

@Composable
private fun WallpaperSectionContent(
  space: Space,
  onApply: (type: String, color: Long?, uri: String?) -> Unit
) {
  val context = LocalContext.current
  var selectedType by remember { mutableStateOf(space.homeWallpaperType) }
  var selectedColor by remember { mutableStateOf(space.homeWallpaperColor ?: PRESET_BACKGROUND_COLORS.first().first) }
  var selectedUri by remember { mutableStateOf(space.homeWallpaperImageUri ?: WallpaperCatalog.DEFAULT_WALLPAPER_URI) }

  val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      selectedUri = uri.toString()
      selectedType = Space.BACKGROUND_IMAGE
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    // Mode selector tabs
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      FilterChip(
        selected = selectedType == Space.BACKGROUND_IMAGE,
        onClick = { selectedType = Space.BACKGROUND_IMAGE },
        label = { Text("Curated Wallpapers") },
        modifier = Modifier.weight(1f)
      )
      FilterChip(
        selected = selectedType == Space.BACKGROUND_COLOR,
        onClick = { selectedType = Space.BACKGROUND_COLOR },
        label = { Text("Solid Color") },
        modifier = Modifier.weight(1f)
      )
    }

    if (selectedType == Space.BACKGROUND_IMAGE) {
      Text(
        text = "Curated Presets",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )

      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        items(WallpaperCatalog.PRESET_WALLPAPERS) { preset ->
          val isSelected = selectedUri == preset.uriString
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
              .width(110.dp)
              .clickable {
                selectedUri = preset.uriString
                selectedType = Space.BACKGROUND_IMAGE
              }
          ) {
            Box(
              modifier = Modifier
                .size(width = 110.dp, height = 160.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                  width = if (isSelected) 3.dp else 1.dp,
                  color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                  shape = RoundedCornerShape(16.dp)
                )
            ) {
              AsyncImage(
                model = preset.drawableRes,
                contentDescription = preset.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
              if (isSelected) {
                Box(
                  modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = preset.name,
              style = MaterialTheme.typography.bodySmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      OutlinedButton(
        onClick = { imagePickerLauncher.launch("image/*") },
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Choose Custom Image from Device")
      }
    } else {
      Text(
        text = "Color Palette",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
      )

      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        PRESET_BACKGROUND_COLORS.chunked(5).forEach { rowColors ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
          ) {
            rowColors.forEach { (colorVal, colorName) ->
              val isSelected = selectedColor == colorVal
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .clip(CircleShape)
                  .background(Color(colorVal))
                  .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape
                  )
                  .clickable {
                    selectedColor = colorVal
                    selectedType = Space.BACKGROUND_COLOR
                  },
                contentAlignment = Alignment.Center
              ) {
                if (isSelected) {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = colorName,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Button(
      onClick = { onApply(selectedType, selectedColor, selectedUri) },
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("apply_wallpaper_button")
    ) {
      Text("Apply Wallpaper to Space", fontWeight = FontWeight.Bold)
    }
  }
}

// ---------------------------------------------------------------------------
// 2. THEME SECTION
// ---------------------------------------------------------------------------

@Composable
private fun ThemeSectionContent(
  space: Space,
  onApply: (theme: String, iconSize: String, labelVisibility: Boolean, gridColumns: Int) -> Unit
) {
  var selectedTheme by remember { mutableStateOf(space.appTheme) }
  var selectedIconSize by remember { mutableStateOf(space.iconSize) }
  var selectedLabelVisibility by remember { mutableStateOf(space.labelVisibility) }
  var selectedGridColumns by remember { mutableIntStateOf(space.gridColumns) }

  val themes = listOf(
    Space.THEME_DEFAULT to "Modern Material",
    Space.THEME_PURPLE to "Quantum Violet",
    Space.THEME_DARK to "Obsidian Dark",
    Space.THEME_NEON to "Cyber Neon",
    Space.THEME_MINIMAL to "Monochrome Minimal",
    Space.THEME_EMERALD to "Emerald Core",
    Space.THEME_SUNSET to "Sunset Amber",
    Space.THEME_OCEAN to "Ocean Azure"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // Theme palette selection
    Text(
      text = "Color Theme",
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      themes.chunked(2).forEach { pair ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          pair.forEach { (id, name) ->
            val isSelected = selectedTheme == id
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              border = if (isSelected) border(1.5.dp, MaterialTheme.colorScheme.primary) else null,
              modifier = Modifier
                .weight(1f)
                .clickable { selectedTheme = id }
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                RadioButton(selected = isSelected, onClick = { selectedTheme = id })
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = name,
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
          if (pair.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }

    // Grid Columns
    Text(
      text = "Desktop Grid Columns",
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(3, 4, 5, 6).forEach { cols ->
        val isSelected = selectedGridColumns == cols
        FilterChip(
          selected = isSelected,
          onClick = { selectedGridColumns = cols },
          label = { Text("$cols Columns") },
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Icon Size
    Text(
      text = "App Icon Size",
      style = MaterialTheme.typography.labelLarge,
      fontWeight = FontWeight.SemiBold
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf(
        Space.ICON_SIZE_SMALL to "Small",
        Space.ICON_SIZE_MEDIUM to "Medium",
        Space.ICON_SIZE_LARGE to "Large"
      ).forEach { (sizeKey, label) ->
        val isSelected = selectedIconSize == sizeKey
        FilterChip(
          selected = isSelected,
          onClick = { selectedIconSize = sizeKey },
          label = { Text(label) },
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Label Visibility
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Show App Labels",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = "Display app names below desktop icons",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
      Switch(
        checked = selectedLabelVisibility,
        onCheckedChange = { selectedLabelVisibility = it }
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
      onClick = {
        onApply(selectedTheme, selectedIconSize, selectedLabelVisibility, selectedGridColumns)
      },
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("apply_theme_button")
    ) {
      Text("Apply Theme to Space", fontWeight = FontWeight.Bold)
    }
  }
}

// ---------------------------------------------------------------------------
// 3. PAGE CONTROL SECTION
// ---------------------------------------------------------------------------

@Composable
private fun PageControlSectionContent(
  space: Space,
  placements: List<SpaceItemPlacement>,
  currentPageIndex: Int,
  onNavigateToPage: (Int) -> Unit,
  onCreatePage: () -> Unit,
  onDeletePage: (Int) -> Unit
) {
  val homePlacements = placements.filter { it.layer == SpaceItemPlacement.LAYER_HOME }
  val maxPlacementPage = homePlacements.maxOfOrNull { it.pageIndex } ?: 0
  val totalPages = maxOf(space.pageCount, maxPlacementPage + 1)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "Desktop Pages",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "$totalPages of ${Space.MAX_PAGES} pages used",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      Button(
        onClick = onCreatePage,
        enabled = totalPages < Space.MAX_PAGES,
        modifier = Modifier.testTag("add_page_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add Page")
      }
    }

    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(totalPages) { pageIdx ->
        val isCurrent = pageIdx == currentPageIndex
        val isProtectedPage1 = pageIdx == 0
        val pageItemCount = homePlacements.count { it.pageIndex == pageIdx }

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
          border = if (isCurrent) border(2.dp, MaterialTheme.colorScheme.primary) else border(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToPage(pageIdx) }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(CircleShape)
                  .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "${pageIdx + 1}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
              }

              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "Page ${pageIdx + 1}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                  )
                  if (isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                      color = MaterialTheme.colorScheme.primary,
                      shape = RoundedCornerShape(8.dp)
                    ) {
                      Text(
                        text = "ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                }
                Text(
                  text = "$pageItemCount item${if (pageItemCount == 1) "" else "s"}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            if (isProtectedPage1) {
              Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(8.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "Protected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            } else {
              IconButton(
                onClick = { onDeletePage(pageIdx) },
                modifier = Modifier.testTag("delete_page_${pageIdx}_button")
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Delete Page ${pageIdx + 1}",
                  tint = MaterialTheme.colorScheme.error
                )
              }
            }
          }
        }
      }
    }
  }
}

// ---------------------------------------------------------------------------
// 4. WIDGETS SECTION
// ---------------------------------------------------------------------------

@Composable
private fun WidgetsListContent(
  spaceApps: List<DiscoveredApp>,
  onSelectGroup: (AppWidgetGroup) -> Unit
) {
  val context = LocalContext.current
  val groups = remember(spaceApps) {
    AppWidgetHostManager.getWidgetGroupsForApps(context, spaceApps)
  }

  if (groups.isEmpty()) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Widgets,
          contentDescription = null,
          modifier = Modifier.size(48.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "No Widgets Available",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Apps installed in this Space do not provide any desktop widgets.",
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(groups) { group ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectGroup(group) }
            .testTag("widget_app_group_${group.app.packageName}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
              modifier = Modifier.weight(1f)
            ) {
              group.widgets.firstOrNull()?.iconDrawable?.let { drawable ->
                val bitmap = remember(drawable) { drawable.toBitmap(96, 96) }
                Image(
                  bitmap = bitmap.asImageBitmap(),
                  contentDescription = group.app.label,
                  modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                )
              } ?: Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.Widgets,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }

              Column {
                Text(
                  text = group.app.label,
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = FontWeight.SemiBold,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                Text(
                  text = "${group.widgets.size} widget${if (group.widgets.size == 1) "" else "s"}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = "View widgets",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WidgetDetailsContent(
  group: AppWidgetGroup,
  onAddWidget: (WidgetItemInfo) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    items(group.widgets) { widget ->
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = border(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          // Widget visual preview
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
          ) {
            if (widget.previewDrawable != null) {
              val bitmap = remember(widget.previewDrawable) {
                widget.previewDrawable.toBitmap(
                  width = (widget.spanX * 120).coerceIn(160, 400),
                  height = (widget.spanY * 80).coerceIn(100, 260)
                )
              }
              Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = widget.label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                  .padding(8.dp)
                  .fillMaxSize()
              )
            } else if (widget.iconDrawable != null) {
              val bitmap = remember(widget.iconDrawable) { widget.iconDrawable.toBitmap(128, 128) }
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Image(
                  bitmap = bitmap.asImageBitmap(),
                  contentDescription = widget.label,
                  modifier = Modifier.size(54.dp)
                )
                Text(
                  text = "${widget.spanX} × ${widget.spanY} cells",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            } else {
              Icon(
                imageVector = Icons.Default.Widgets,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          // Widget description and size badge
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = widget.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "Grid size: ${widget.spanX} × ${widget.spanY} (Min: ${widget.minSpanX}×${widget.minSpanY})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Button(
              onClick = { onAddWidget(widget) },
              modifier = Modifier.testTag("add_widget_${widget.label}")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add to Desktop")
            }
          }
        }
      }
    }
  }
}

private fun border(width: androidx.compose.ui.unit.Dp, color: Color) =
  androidx.compose.foundation.BorderStroke(width, color)
