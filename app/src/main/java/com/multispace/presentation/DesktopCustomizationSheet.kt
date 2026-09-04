package com.multispace.presentation

import android.appwidget.AppWidgetManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceItemPlacement
import com.multispace.ui.theme.AmberPulse
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.CyberCyan
import com.multispace.ui.theme.EmeraldCore
import com.multispace.ui.theme.QuantumViolet
import com.multispace.ui.theme.ShapeRoundLg
import com.multispace.ui.theme.ShapeRoundMd
import com.multispace.ui.theme.ShapeRoundSm

enum class CustomizationSection {
  ROOT,
  WALLPAPERS,
  WIDGETS,
  THEME,
  PAGE_CONTROL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopCustomizationSheet(
  space: Space,
  placements: List<SpaceItemPlacement>,
  currentPage: Int,
  totalPageCount: Int,
  onDismiss: () -> Unit,
  onSelectWallpaperColor: (Long) -> Unit,
  onSelectWallpaperPreset: (String) -> Unit,
  onSelectWallpaperUri: (Uri) -> Unit,
  onOpenWallpaperEditor: () -> Unit,
  onAddWidget: (pageIndex: Int, widgetType: String, spanX: Int, spanY: Int, appWidgetId: Int, pkg: String?, comp: String?) -> Unit,
  onUpdateTheme: (appTheme: String, cols: Int, iconSize: String, showLabels: Boolean) -> Unit,
  onAddPage: () -> Unit,
  onDeletePage: (pageIndex: Int) -> Unit,
  onScrollToPage: (pageIndex: Int) -> Unit,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  var currentSection by remember { mutableStateOf(CustomizationSection.ROOT) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("desktop_customization_sheet")
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.70f)
        .padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing8)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Header with title and back navigation
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AppDimens.Spacing12),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
          ) {
            if (currentSection != CustomizationSection.ROOT) {
              IconButton(
                onClick = { currentSection = CustomizationSection.ROOT },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("btn_customization_back")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Back",
                  tint = MaterialTheme.colorScheme.onSurface
                )
              }
            }
            Text(
              text = when (currentSection) {
                CustomizationSection.ROOT -> "Desktop Customization"
                CustomizationSection.WALLPAPERS -> "Wallpapers"
                CustomizationSection.WIDGETS -> "Widgets"
                CustomizationSection.THEME -> "Theme & Layout"
                CustomizationSection.PAGE_CONTROL -> "Page Control"
              },
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(36.dp)
              .testTag("btn_close_customization")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(AppDimens.Spacing16))

        // Animated Content for Root and Sub-sections
        AnimatedContent(
          targetState = currentSection,
          transitionSpec = {
            if (targetState != CustomizationSection.ROOT) {
              (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
            } else {
              (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
            }
          },
          label = "customization_section"
        ) { section ->
          when (section) {
            CustomizationSection.ROOT -> {
              CustomizationRootMenu(
                onNavigate = { currentSection = it },
                space = space,
                totalPageCount = totalPageCount
              )
            }
            CustomizationSection.WALLPAPERS -> {
              WallpapersSubscreen(
                space = space,
                onSelectColor = onSelectWallpaperColor,
                onSelectPreset = onSelectWallpaperPreset,
                onSelectUri = onSelectWallpaperUri,
                onOpenEditor = onOpenWallpaperEditor
              )
            }
            CustomizationSection.WIDGETS -> {
              WidgetsSubscreen(
                space = space,
                currentPage = currentPage,
                totalPageCount = totalPageCount,
                onAddWidget = onAddWidget
              )
            }
            CustomizationSection.THEME -> {
              ThemeSubscreen(
                space = space,
                onUpdateTheme = onUpdateTheme
              )
            }
            CustomizationSection.PAGE_CONTROL -> {
              PageControlSubscreen(
                space = space,
                placements = placements,
                currentPage = currentPage,
                totalPageCount = totalPageCount,
                onAddPage = onAddPage,
                onDeletePage = onDeletePage,
                onScrollToPage = onScrollToPage
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CustomizationRootMenu(
  onNavigate: (CustomizationSection) -> Unit,
  space: Space,
  totalPageCount: Int
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
  ) {
    CustomizationCategoryTile(
      title = "Wallpapers",
      subtitle = "Solid colors, curated themes, or custom photos",
      icon = Icons.Default.Wallpaper,
      badge = if (space.homeWallpaperType == Space.BACKGROUND_DEFAULT) "Default" else "Custom",
      iconTint = QuantumViolet,
      onClick = { onNavigate(CustomizationSection.WALLPAPERS) },
      testTag = "tile_customization_wallpapers"
    )

    CustomizationCategoryTile(
      title = "Widgets",
      subtitle = "Clock, search bar, calendar, notes, battery, and system widgets",
      icon = Icons.Default.Widgets,
      badge = "Add Items",
      iconTint = CyberCyan,
      onClick = { onNavigate(CustomizationSection.WIDGETS) },
      testTag = "tile_customization_widgets"
    )

    CustomizationCategoryTile(
      title = "Theme",
      subtitle = "Palette, grid columns (${space.gridColumns}x), icon size, and label visibility",
      icon = Icons.Default.Palette,
      badge = space.appTheme,
      iconTint = EmeraldCore,
      onClick = { onNavigate(CustomizationSection.THEME) },
      testTag = "tile_customization_theme"
    )

    CustomizationCategoryTile(
      title = "Page Control",
      subtitle = "Create, delete, or reorder desktop pages (Page 1 is permanent)",
      icon = Icons.Default.Layers,
      badge = "$totalPageCount ${if (totalPageCount == 1) "Page" else "Pages"}",
      iconTint = AmberPulse,
      onClick = { onNavigate(CustomizationSection.PAGE_CONTROL) },
      testTag = "tile_customization_page_control"
    )
  }
}

@Composable
private fun CustomizationCategoryTile(
  title: String,
  subtitle: String,
  icon: ImageVector,
  badge: String,
  iconTint: Color,
  onClick: () -> Unit,
  testTag: String
) {
  Card(
    onClick = onClick,
    shape = ShapeRoundMd,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .testTag(testTag)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(AppDimens.Spacing16),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(ShapeRoundMd)
            .background(iconTint.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
          )
        }

        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
          ) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Box(
              modifier = Modifier
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.2f))
                .padding(horizontal = AppDimens.Spacing8, vertical = 2.dp)
            ) {
              Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = iconTint,
                fontSize = 10.sp
              )
            }
          }
          Spacer(modifier = Modifier.height(AppDimens.Spacing4))
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "Open",
        tint = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

// --- WALLPAPERS SUB-SCREEN ---

@Composable
private fun WallpapersSubscreen(
  space: Space,
  onSelectColor: (Long) -> Unit,
  onSelectPreset: (String) -> Unit,
  onSelectUri: (Uri) -> Unit,
  onOpenEditor: () -> Unit
) {
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      onSelectUri(uri)
    }
  }

  val presetColors = listOf(
    0xFF12141C to "Midnight Dark",
    0xFF1E1B4B to "Deep Indigo",
    0xFF0F172A to "Slate Navy",
    0xFF1C1917 to "Warm Onyx",
    0xFF14532D to "Forest Green",
    0xFF701A75 to "Crimson Plum",
    0xFF312E81 to "Royal Violet",
    0xFF083344 to "Ocean Deep"
  )

  val presetThemes = listOf(
    "Deep Space" to "android.resource://com.example/drawable/wp_space",
    "Cyberpunk Glow" to "android.resource://com.example/drawable/wp_cyber",
    "Aurora Borealis" to "android.resource://com.example/drawable/wp_aurora",
    "Minimalist Charcoal" to "android.resource://com.example/drawable/wp_minimal"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
  ) {
    // Action Row: Photo Picker & Advanced Wallpaper Editor
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
      Button(
        onClick = {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        shape = ShapeRoundMd,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .testTag("btn_wallpaper_photo_picker")
      ) {
        Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(AppDimens.Spacing8))
        Text("Pick Photo", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
      }

      OutlinedButton(
        onClick = onOpenEditor,
        shape = ShapeRoundMd,
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .testTag("btn_open_wallpaper_editor")
      ) {
        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(AppDimens.Spacing8))
        Text("Fine Tune", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
      }
    }

    // Color Swatches
    Text(
      text = "Solid Colors",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
      presetColors.forEach { (colorLong, label) ->
        val isSelected = space.homeWallpaperType == Space.BACKGROUND_COLOR && space.homeWallpaperColor == colorLong
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.clickable { onSelectColor(colorLong) }
        ) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .background(Color(colorLong))
              .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            if (isSelected) {
              Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
          }
          Spacer(modifier = Modifier.height(AppDimens.Spacing4))
          Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            maxLines = 1
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(AppDimens.Spacing8))

    // Preset Styles
    Text(
      text = "Presets",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
      presetThemes.forEach { (name, uriStr) ->
        val isSelected = space.homeWallpaperImageUri == uriStr
        OutlinedCard(
          onClick = { onSelectPreset(uriStr) },
          shape = ShapeRoundMd,
          modifier = Modifier
            .width(130.dp)
            .height(84.dp)
            .testTag("preset_$name"),
          colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerHigh
          ),
          border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
          )
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(AppDimens.Spacing12),
            contentAlignment = Alignment.BottomStart
          ) {
            Column {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.height(AppDimens.Spacing6))
              Text(
                text = name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}

// --- WIDGETS SUB-SCREEN ---

@Composable
private fun WidgetsSubscreen(
  space: Space,
  currentPage: Int,
  totalPageCount: Int,
  onAddWidget: (pageIndex: Int, widgetType: String, spanX: Int, spanY: Int, appWidgetId: Int, pkg: String?, comp: String?) -> Unit
) {
  val context = LocalContext.current
  var selectedTargetPage by remember { mutableIntStateOf(currentPage) }

  val companionWidgets = listOf(
    Triple(
      SpaceItemPlacement.WIDGET_CLOCK_DATE,
      "Digital Clock & Date",
      "Large elegant time and current date display (2x1)"
    ) to (2 to 1),
    Triple(
      SpaceItemPlacement.WIDGET_QUICK_SEARCH,
      "Quick Search Bar",
      "Web and app launcher search pill (4x1)"
    ) to (4 to 1),
    Triple(
      SpaceItemPlacement.WIDGET_CALENDAR,
      "Calendar Card",
      "Date, day of week, and month indicator (2x2)"
    ) to (2 to 2),
    Triple(
      SpaceItemPlacement.WIDGET_BATTERY_STATUS,
      "Battery Status",
      "Live battery percentage and charging status (2x1)"
    ) to (2 to 1),
    Triple(
      SpaceItemPlacement.WIDGET_QUICK_NOTES,
      "Quick Notes",
      "Compact notepad for desktop reminders (4x2)"
    ) to (4 to 2)
  )

  val appWidgetManager = remember { AppWidgetManager.getInstance(context) }
  val installedProviders = remember {
    try {
      appWidgetManager.installedProviders.take(10)
    } catch (e: Exception) {
      emptyList()
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
  ) {
    // Page Target Selection
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = "Add to Desktop Page:",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing6)) {
        for (p in 0 until totalPageCount) {
          FilterChip(
            selected = selectedTargetPage == p,
            onClick = { selectedTargetPage = p },
            label = { Text("Page ${p + 1}") },
            shape = ShapeRoundSm,
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primary,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            )
          )
        }
      }
    }

    Text(
      text = "Launcher Companion Widgets",
      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.primary
    )

    companionWidgets.forEach { (item, size) ->
      val (type, title, description) = item
      val (spanX, spanY) = size

      Card(
        shape = ShapeRoundMd,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            onAddWidget(selectedTargetPage, type, spanX, spanY, -1, null, null)
          }
          .testTag("widget_item_$type")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimens.Spacing12),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(ShapeRoundMd)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = when (type) {
                  SpaceItemPlacement.WIDGET_CLOCK_DATE -> Icons.Default.Schedule
                  SpaceItemPlacement.WIDGET_QUICK_SEARCH -> Icons.Default.Search
                  SpaceItemPlacement.WIDGET_CALENDAR -> Icons.Default.CalendarToday
                  SpaceItemPlacement.WIDGET_BATTERY_STATUS -> Icons.Default.AutoAwesome
                  else -> Icons.Default.Notes
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
              )
            }

            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing6)
              ) {
                Text(
                  text = title,
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${spanX}x$spanY",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
              )
            }
          }

          FilledTonalButton(
            onClick = {
              onAddWidget(selectedTargetPage, type, spanX, spanY, -1, null, null)
            },
            shape = ShapeRoundSm,
            contentPadding = PaddingValues(horizontal = AppDimens.Spacing12, vertical = 4.dp),
            modifier = Modifier.height(34.dp).testTag("btn_add_widget_$type")
          ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
          }
        }
      }
    }

    if (installedProviders.isNotEmpty()) {
      Spacer(modifier = Modifier.height(AppDimens.Spacing8))
      Text(
        text = "System Widgets",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary
      )

      installedProviders.forEach { provider ->
        Card(
          shape = ShapeRoundMd,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              onAddWidget(
                selectedTargetPage,
                "SYSTEM_WIDGET",
                2,
                2,
                -1,
                provider.provider.packageName,
                provider.provider.className
              )
            }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(AppDimens.Spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = provider.loadLabel(context.packageManager),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = provider.provider.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
              )
            }

            OutlinedButton(
              onClick = {
                onAddWidget(
                  selectedTargetPage,
                  "SYSTEM_WIDGET",
                  2,
                  2,
                  -1,
                  provider.provider.packageName,
                  provider.provider.className
                )
              },
              shape = ShapeRoundSm,
              contentPadding = PaddingValues(horizontal = AppDimens.Spacing12, vertical = 4.dp),
              modifier = Modifier.height(34.dp)
            ) {
              Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Add", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }
  }
}

// --- THEME SUB-SCREEN ---

@Composable
private fun ThemeSubscreen(
  space: Space,
  onUpdateTheme: (appTheme: String, cols: Int, iconSize: String, showLabels: Boolean) -> Unit
) {
  var selectedTheme by remember { mutableStateOf(space.appTheme) }
  var selectedColumns by remember { mutableIntStateOf(space.gridColumns) }
  var selectedIconSize by remember { mutableStateOf(space.iconSize) }
  var showLabels by remember { mutableStateOf(space.labelVisibility) }

  val themePresets = listOf(
    "DEFAULT" to "Modern Teal",
    "PURPLE" to "Midnight Violet",
    "DARK" to "Deep Slate",
    "NEON" to "Cyber Neon",
    "MINIMAL" to "Pure Minimal",
    "EMERALD" to "Forest Emerald",
    "SUNSET" to "Crimson Sunset",
    "OCEAN" to "Pacific Blue"
  )

  val columnsList = listOf(3, 4, 5, 6)
  val iconSizes = listOf(
    Space.ICON_SIZE_SMALL to "Small",
    Space.ICON_SIZE_MEDIUM to "Medium",
    Space.ICON_SIZE_LARGE to "Large"
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
  ) {
    // Theme Palette
    Text(
      text = "App Theme Palette",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
      themePresets.forEach { (themeKey, name) ->
        FilterChip(
          selected = selectedTheme == themeKey,
          onClick = {
            selectedTheme = themeKey
            onUpdateTheme(themeKey, selectedColumns, selectedIconSize, showLabels)
          },
          label = { Text(name) },
          shape = ShapeRoundMd,
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
          ),
          modifier = Modifier.testTag("theme_chip_$themeKey")
        )
      }
    }

    // Grid Columns
    Text(
      text = "Grid Density",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
      columnsList.forEach { cols ->
        FilterChip(
          selected = selectedColumns == cols,
          onClick = {
            selectedColumns = cols
            onUpdateTheme(selectedTheme, cols, selectedIconSize, showLabels)
          },
          label = { Text("${cols} Columns") },
          shape = ShapeRoundMd,
          modifier = Modifier
            .weight(1f)
            .testTag("grid_col_$cols"),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
          )
        )
      }
    }

    // Icon Size
    Text(
      text = "Icon Size",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
    ) {
      iconSizes.forEach { (sizeKey, label) ->
        FilterChip(
          selected = selectedIconSize == sizeKey,
          onClick = {
            selectedIconSize = sizeKey
            onUpdateTheme(selectedTheme, selectedColumns, sizeKey, showLabels)
          },
          label = { Text(label) },
          shape = ShapeRoundMd,
          modifier = Modifier
            .weight(1f)
            .testTag("icon_size_$sizeKey"),
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
          )
        )
      }
    }

    // App Labels Toggle
    Card(
      shape = ShapeRoundMd,
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
      ),
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
          Icon(
            imageVector = if (showLabels) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Column {
            Text(
              text = "Show Icon Labels",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Display app name text under icons",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Switch(
          checked = showLabels,
          onCheckedChange = {
            showLabels = it
            onUpdateTheme(selectedTheme, selectedColumns, selectedIconSize, it)
          },
          modifier = Modifier.testTag("switch_show_labels")
        )
      }
    }
  }
}

// --- PAGE CONTROL SUB-SCREEN ---

@Composable
private fun PageControlSubscreen(
  space: Space,
  placements: List<SpaceItemPlacement>,
  currentPage: Int,
  totalPageCount: Int,
  onAddPage: () -> Unit,
  onDeletePage: (pageIndex: Int) -> Unit,
  onScrollToPage: (pageIndex: Int) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Pages ($totalPageCount)",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Button(
        onClick = onAddPage,
        shape = ShapeRoundMd,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = AppDimens.Spacing12, vertical = 6.dp),
        modifier = Modifier
          .height(36.dp)
          .testTag("btn_add_page")
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Page", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
      }
    }

    for (p in 0 until totalPageCount) {
      val isPage1 = p == 0
      val isCurrent = p == currentPage
      val itemsOnPage = placements.count { it.pageIndex == p }

      Card(
        shape = ShapeRoundMd,
        colors = CardDefaults.cardColors(
          containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = androidx.compose.foundation.BorderStroke(
          if (isCurrent) 1.5.dp else 1.dp,
          if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("page_card_$p")
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimens.Spacing16),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "${p + 1}",
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing6)
              ) {
                Text(
                  text = "Page ${p + 1}",
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (isPage1) {
                  Box(
                    modifier = Modifier
                      .clip(CircleShape)
                      .background(AmberPulse.copy(alpha = 0.18f))
                      .padding(horizontal = AppDimens.Spacing6, vertical = 1.dp)
                  ) {
                    Text(
                      text = "Main / Permanent",
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = AmberPulse,
                      fontSize = 10.sp
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "$itemsOnPage items placed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
              )
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
          ) {
            FilledTonalButton(
              onClick = { onScrollToPage(p) },
              shape = ShapeRoundSm,
              contentPadding = PaddingValues(horizontal = AppDimens.Spacing10, vertical = 4.dp),
              modifier = Modifier
                .height(32.dp)
                .testTag("btn_jump_page_$p")
            ) {
              Text(
                text = if (isCurrent) "Current" else "Jump",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
              )
            }

            if (!isPage1) {
              IconButton(
                onClick = { onDeletePage(p) },
                modifier = Modifier
                  .size(32.dp)
                  .testTag("btn_delete_page_$p")
              ) {
                Icon(
                  imageVector = Icons.Default.Delete,
                  contentDescription = "Delete Page",
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(18.dp)
                )
              }
            } else {
              Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Page 1 is permanent and immutable",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}
