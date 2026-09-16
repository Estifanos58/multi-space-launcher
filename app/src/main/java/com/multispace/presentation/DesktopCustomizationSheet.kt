package com.multispace.presentation

import android.appwidget.AppWidgetManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
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
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import com.multispace.presentation.widget.BatteryStatusWidget
import com.multispace.presentation.widget.CalendarWidget
import com.multispace.presentation.widget.ClockDateWidget
import com.multispace.presentation.widget.DesktopWidgetView
import com.multispace.presentation.widget.QuickNotesWidget
import com.multispace.presentation.widget.QuickSearchWidget
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.R
import com.multispace.domain.model.DiscoveredApp
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
  PAGE_CONTROL,
  CUSTOMIZE_SPACE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopCustomizationSheet(
  space: Space,
  placements: List<SpaceItemPlacement>,
  currentPage: Int,
  totalPageCount: Int,
  spaceApps: List<DiscoveredApp> = emptyList(),
  onDismiss: () -> Unit,
  onSelectWallpaperColor: (Long) -> Unit,
  onSelectWallpaperPreset: (String) -> Unit,
  onSelectWallpaperUri: (Uri) -> Unit,
  onOpenWallpaperEditor: () -> Unit = {},
  onUpdateSpaceCustomization: ((
    backgroundType: String,
    backgroundColor: Long?,
    backgroundImageUri: String?,
    gridColumns: Int,
    iconSize: String,
    labelVisibility: Boolean
  ) -> Unit)? = null,
  onReorderApp: ((DiscoveredApp, Int) -> Unit)? = null,
  onSortAlphabetically: (() -> Unit)? = null,
  onAddWidget: (pageIndex: Int, widgetType: String, spanX: Int, spanY: Int, appWidgetId: Int, pkg: String?, comp: String?) -> Unit,
  onUpdateTheme: (appTheme: String, cols: Int, iconSize: String, showLabels: Boolean) -> Unit,
  onAddPage: () -> Unit,
  onDeletePage: (pageIndex: Int) -> Unit,
  onScrollToPage: (pageIndex: Int) -> Unit,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  var currentSection by remember { mutableStateOf(CustomizationSection.ROOT) }

  if (currentSection == CustomizationSection.ROOT) {
    DesktopCustomizationOverlay(
      onNavigate = { currentSection = it },
      onDismiss = onDismiss
    )
  } else {
    ModalBottomSheet(
      onDismissRequest = {
        currentSection = CustomizationSection.ROOT
      },
      sheetState = sheetState,
      containerColor = Color(0xEB12141F),
      scrimColor = Color.Black.copy(alpha = 0.55f),
      shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("desktop_customization_sheet")
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.82f)
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
              IconButton(
                onClick = {
                  if (currentSection == CustomizationSection.CUSTOMIZE_SPACE) {
                    currentSection = CustomizationSection.WALLPAPERS
                  } else {
                    currentSection = CustomizationSection.ROOT
                  }
                },
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
              Text(
                text = when (currentSection) {
                  CustomizationSection.ROOT -> "Desktop Customization"
                  CustomizationSection.WALLPAPERS -> "Wallpapers"
                  CustomizationSection.WIDGETS -> "Widgets"
                  CustomizationSection.THEME -> "Theme & Layout"
                  CustomizationSection.PAGE_CONTROL -> "Page Control"
                  CustomizationSection.CUSTOMIZE_SPACE -> "Customize Space"
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

          when (currentSection) {
            CustomizationSection.WALLPAPERS -> {
              WallpapersSubscreen(
                space = space,
                onSelectColor = onSelectWallpaperColor,
                onSelectPreset = onSelectWallpaperPreset,
                onSelectUri = onSelectWallpaperUri,
                onOpenCustomizeSpace = { currentSection = CustomizationSection.CUSTOMIZE_SPACE }
              )
            }
            CustomizationSection.CUSTOMIZE_SPACE -> {
              CustomizeSpaceSubscreen(
                space = space,
                spaceApps = spaceApps,
                onSaveCustomization = { bgType, bgColor, bgUri, cols, size, showLabels ->
                  onUpdateSpaceCustomization?.invoke(bgType, bgColor, bgUri, cols, size, showLabels)
                },
                onSelectColor = onSelectWallpaperColor,
                onSelectPreset = onSelectWallpaperPreset,
                onSelectUri = onSelectWallpaperUri,
                onReorderApp = onReorderApp,
                onSortAlphabetically = onSortAlphabetically
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
            CustomizationSection.ROOT -> {}
          }
        }
      }
    }
  }
}

@Composable
private fun DesktopCustomizationOverlay(
  onNavigate: (CustomizationSection) -> Unit,
  onDismiss: () -> Unit
) {
  BackHandler(onBack = onDismiss)

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.45f))
      .clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onDismiss
      )
      .testTag("desktop_customization_overlay")
  ) {
    // Floating horizontal dock at the bottom of the screen
    Surface(
      shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
      color = Color(0xD9101218),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        Color.White.copy(alpha = 0.12f)
      ),
      tonalElevation = 8.dp,
      shadowElevation = 12.dp,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = AppDimens.Spacing32)
        .padding(horizontal = AppDimens.Spacing16)
        .clickable(
          interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
          indication = null,
          onClick = { /* Intercept click so it does not dismiss */ }
        )
        .testTag("desktop_customization_horizontal_bar")
    ) {
      Row(
        modifier = Modifier
          .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically
      ) {
        HorizontalCustomizationItem(
          title = "Wallpapers",
          icon = Icons.Default.Wallpaper,
          iconTint = QuantumViolet,
          onClick = { onNavigate(CustomizationSection.WALLPAPERS) },
          testTag = "tile_customization_wallpapers"
        )

        HorizontalCustomizationItem(
          title = "Widgets",
          icon = Icons.Default.Widgets,
          iconTint = CyberCyan,
          onClick = { onNavigate(CustomizationSection.WIDGETS) },
          testTag = "tile_customization_widgets"
        )

        HorizontalCustomizationItem(
          title = "Theme",
          icon = Icons.Default.Palette,
          iconTint = EmeraldCore,
          onClick = { onNavigate(CustomizationSection.THEME) },
          testTag = "tile_customization_theme"
        )

        HorizontalCustomizationItem(
          title = "Page Control",
          icon = Icons.Default.Layers,
          iconTint = AmberPulse,
          onClick = { onNavigate(CustomizationSection.PAGE_CONTROL) },
          testTag = "tile_customization_page_control"
        )
      }
    }
  }
}

@Composable
private fun HorizontalCustomizationItem(
  title: String,
  icon: ImageVector,
  iconTint: Color,
  onClick: () -> Unit,
  testTag: String
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier
      .clip(ShapeRoundMd)
      .clickable(onClick = onClick)
      .padding(horizontal = AppDimens.Spacing6, vertical = AppDimens.Spacing4)
      .testTag(testTag)
  ) {
    Surface(
      shape = CircleShape,
      color = iconTint.copy(alpha = 0.15f),
      border = androidx.compose.foundation.BorderStroke(
        1.dp,
        iconTint.copy(alpha = 0.35f)
      ),
      modifier = Modifier.size(50.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = iconTint,
          modifier = Modifier.size(24.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(AppDimens.Spacing6))

    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

// --- WALLPAPERS SUB-SCREEN ---

data class DefaultWallpaperItem(
  val id: String,
  val name: String,
  val category: String,
  @DrawableRes val resId: Int,
  val isDefault: Boolean = false
)

@Composable
private fun WallpapersSubscreen(
  space: Space,
  onSelectColor: (Long) -> Unit,
  onSelectPreset: (String) -> Unit,
  onSelectUri: (Uri) -> Unit,
  onOpenCustomizeSpace: () -> Unit
) {
  val context = LocalContext.current
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      onSelectUri(uri)
    }
  }

  val defaultWallpapers = remember(context.packageName) {
    listOf(
      DefaultWallpaperItem("aurora", "Aurora Borealis", "Cosmic Glow", R.drawable.img_wallpaper_aurora, isDefault = true),
      DefaultWallpaperItem("cyber", "Cyberpunk Glow", "Neon Night", R.drawable.img_wallpaper_cyber),
      DefaultWallpaperItem("mountain", "Mountain Mist", "Alpine Serenity", R.drawable.img_wallpaper_mountain),
      DefaultWallpaperItem("nature", "Emerald Nature", "Forest Wonder", R.drawable.img_wallpaper_nature)
    )
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

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
  ) {
    // Action Row: Photo Picker & Continuation to Customize Space
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
        onClick = onOpenCustomizeSpace,
        shape = ShapeRoundMd,
        modifier = Modifier
          .weight(1f)
          .height(44.dp)
          .testTag("btn_open_wallpaper_editor")
      ) {
        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(AppDimens.Spacing8))
        Text("Customize Space", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
      }
    }

    // Default Wallpapers Gallery
    Text(
      text = "Default Wallpapers",
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = MaterialTheme.colorScheme.onSurface
    )

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
    ) {
      defaultWallpapers.forEach { wallpaper ->
        val uriStr = "android.resource://${context.packageName}/${wallpaper.resId}"
        val isSelected = if (wallpaper.isDefault) {
          space.homeWallpaperType == Space.BACKGROUND_DEFAULT ||
            (space.homeWallpaperType == Space.BACKGROUND_IMAGE && (space.homeWallpaperImageUri == uriStr || space.homeWallpaperImageUri.isNullOrEmpty() || space.homeWallpaperImageUri == "DEFAULT"))
        } else {
          space.homeWallpaperType == Space.BACKGROUND_IMAGE && space.homeWallpaperImageUri == uriStr
        }

        Card(
          onClick = { onSelectPreset(uriStr) },
          shape = ShapeRoundMd,
          modifier = Modifier
            .width(135.dp)
            .height(195.dp)
            .testTag("default_wallpaper_${wallpaper.id}"),
          border = androidx.compose.foundation.BorderStroke(
            if (isSelected) 3.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            Image(
              painter = painterResource(id = wallpaper.resId),
              contentDescription = wallpaper.name,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )

            // Scrim overlay for text
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  Brush.verticalGradient(
                    colors = listOf(
                      Color.Transparent,
                      Color.Black.copy(alpha = 0.3f),
                      Color.Black.copy(alpha = 0.85f)
                    ),
                    startY = 140f
                  )
                )
            )

            if (isSelected) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(AppDimens.Spacing8)
                  .size(26.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = "Selected",
                  tint = Color.White,
                  modifier = Modifier.size(16.dp)
                )
              }
            }

            Column(
              modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(AppDimens.Spacing8)
            ) {
              if (wallpaper.isDefault) {
                Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                  modifier = Modifier.padding(bottom = 2.dp)
                ) {
                  Text(
                    text = "DEFAULT",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Text(
                text = wallpaper.name,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = wallpaper.category,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(AppDimens.Spacing4))

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
  }
}

// --- CUSTOMIZE SPACE SUB-SCREEN (CONTINUATION OF BOTTOM SHEET) ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomizeSpaceSubscreen(
  space: Space,
  spaceApps: List<DiscoveredApp>,
  onSaveCustomization: (backgroundType: String, backgroundColor: Long?, backgroundImageUri: String?, gridColumns: Int, iconSize: String, labelVisibility: Boolean) -> Unit,
  onSelectColor: (Long) -> Unit,
  onSelectPreset: (String) -> Unit,
  onSelectUri: (Uri) -> Unit,
  onReorderApp: ((DiscoveredApp, Int) -> Unit)?,
  onSortAlphabetically: (() -> Unit)?
) {
  val context = LocalContext.current

  val defaultWallpapers = remember(context.packageName) {
    listOf(
      DefaultWallpaperItem("aurora", "Aurora Borealis", "Cosmic Glow", R.drawable.img_wallpaper_aurora, isDefault = true),
      DefaultWallpaperItem("cyber", "Cyberpunk Glow", "Neon Night", R.drawable.img_wallpaper_cyber),
      DefaultWallpaperItem("mountain", "Mountain Mist", "Alpine Serenity", R.drawable.img_wallpaper_mountain),
      DefaultWallpaperItem("nature", "Emerald Nature", "Forest Wonder", R.drawable.img_wallpaper_nature)
    )
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

  var selectedBgType by remember(space.id) { mutableStateOf(space.backgroundType) }
  var selectedBgColor by remember(space.id) { mutableStateOf(space.backgroundColor ?: presetColors.first().first) }
  var selectedImageUri by remember(space.id) { mutableStateOf(space.backgroundImageUri) }
  var selectedGridColumns by remember(space.id) { mutableIntStateOf(space.gridColumns.coerceIn(Space.MIN_GRID_COLUMNS, Space.MAX_GRID_COLUMNS)) }
  var selectedIconSize by remember(space.id) { mutableStateOf(space.iconSize) }
  var selectedLabelVisibility by remember(space.id) { mutableStateOf(space.labelVisibility) }

  var activeTab by remember { mutableIntStateOf(0) } // 0: Wallpaper, 1: Grid & Layout, 2: App Ordering

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      selectedImageUri = uri.toString()
      selectedBgType = Space.BACKGROUND_IMAGE
      onSelectUri(uri)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(bottom = AppDimens.Spacing8),
    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
  ) {
    // Space Identity Banner
    Surface(
      shape = ShapeRoundMd,
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text(
            text = space.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${selectedGridColumns} Cols • ${selectedIconSize.replaceFirstChar { it.uppercase() }} Icons",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(36.dp)
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }

    // Segmented Navigation Tabs
    SecondaryTabRow(
      selectedTabIndex = activeTab,
      containerColor = Color.Transparent,
      modifier = Modifier.fillMaxWidth()
    ) {
      Tab(
        selected = activeTab == 0,
        onClick = { activeTab = 0 },
        text = { Text("Wallpapers", fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal) },
        icon = { Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp)) }
      )
      Tab(
        selected = activeTab == 1,
        onClick = { activeTab = 1 },
        text = { Text("Grid & Icons", fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal) },
        icon = { Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(18.dp)) }
      )
      Tab(
        selected = activeTab == 2,
        onClick = { activeTab = 2 },
        text = { Text("App Ordering", fontWeight = if (activeTab == 2) FontWeight.Bold else FontWeight.Normal) },
        icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(18.dp)) }
      )
    }

    // Tab Body
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      when (activeTab) {
        0 -> {
          // Wallpaper & Color Tab
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
          ) {
            // Default Wallpapers
            Text(
              text = "Default Wallpapers",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
            ) {
              defaultWallpapers.forEach { wp ->
                val uriStr = "android.resource://${context.packageName}/${wp.resId}"
                val isSelected = if (wp.isDefault) {
                  selectedBgType == Space.BACKGROUND_DEFAULT || (selectedBgType == Space.BACKGROUND_IMAGE && (selectedImageUri == uriStr || selectedImageUri.isNullOrEmpty() || selectedImageUri == "DEFAULT"))
                } else {
                  selectedBgType == Space.BACKGROUND_IMAGE && selectedImageUri == uriStr
                }

                Card(
                  onClick = {
                    selectedBgType = Space.BACKGROUND_IMAGE
                    selectedImageUri = uriStr
                    onSelectPreset(uriStr)
                  },
                  shape = ShapeRoundMd,
                  modifier = Modifier
                    .width(120.dp)
                    .height(170.dp),
                  border = androidx.compose.foundation.BorderStroke(
                    if (isSelected) 3.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                  )
                ) {
                  Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                      painter = painterResource(id = wp.resId),
                      contentDescription = wp.name,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )

                    Box(
                      modifier = Modifier
                        .fillMaxSize()
                        .background(
                          Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                            startY = 120f
                          )
                        )
                    )

                    if (isSelected) {
                      Box(
                        modifier = Modifier
                          .align(Alignment.TopEnd)
                          .padding(6.dp)
                          .size(22.dp)
                          .clip(CircleShape)
                          .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                      ) {
                        Icon(
                          imageVector = Icons.Default.Check,
                          contentDescription = "Selected",
                          tint = Color.White,
                          modifier = Modifier.size(14.dp)
                        )
                      }
                    }

                    Column(
                      modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(6.dp)
                    ) {
                      if (wp.isDefault) {
                        Surface(
                          shape = CircleShape,
                          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                          modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                          Text(
                            text = "DEFAULT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                          )
                        }
                      }
                      Text(
                        text = wp.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }
                  }
                }
              }
            }

            // Solid Colors
            Text(
              text = "Solid Colors",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
            ) {
              presetColors.forEach { (colorVal, name) ->
                val isSelected = selectedBgType == Space.BACKGROUND_COLOR && selectedBgColor == colorVal
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.clickable {
                    selectedBgType = Space.BACKGROUND_COLOR
                    selectedBgColor = colorVal
                    onSelectColor(colorVal)
                  }
                ) {
                  Box(
                    modifier = Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .background(Color(colorVal))
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
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    maxLines = 1
                  )
                }
              }
            }

            // Custom Photo
            OutlinedButton(
              onClick = {
                photoPickerLauncher.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              shape = ShapeRoundMd,
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
            ) {
              Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Choose Photo from Gallery", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
          }
        }
        1 -> {
          // Grid & Layout Tab
          Column(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16)
          ) {
            Text(
              text = "Desktop Grid Columns",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              (3..6).forEach { cols ->
                val isSelected = selectedGridColumns == cols
                FilterChip(
                  selected = isSelected,
                  onClick = { selectedGridColumns = cols },
                  label = {
                    Text(
                      text = "$cols Columns",
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                      )
                    )
                  },
                  modifier = Modifier.weight(1f)
                )
              }
            }

            Text(
              text = "App Icon Size",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              listOf(
                Space.ICON_SIZE_SMALL to "Compact",
                Space.ICON_SIZE_MEDIUM to "Standard",
                Space.ICON_SIZE_LARGE to "Spacious"
              ).forEach { (sizeKey, label) ->
                val isSelected = selectedIconSize == sizeKey
                FilterChip(
                  selected = isSelected,
                  onClick = { selectedIconSize = sizeKey },
                  label = {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                      )
                    )
                  },
                  modifier = Modifier.weight(1f)
                )
              }
            }

            Surface(
              shape = ShapeRoundMd,
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(AppDimens.Spacing16),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Show App Labels",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "Display app titles under icons on the desktop",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                Switch(
                  checked = selectedLabelVisibility,
                  onCheckedChange = { selectedLabelVisibility = it }
                )
              }
            }
          }
        }
        2 -> {
          // App Ordering Tab
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
          ) {
            FilledTonalButton(
              onClick = { onSortAlphabetically?.invoke() },
              shape = ShapeRoundMd,
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Sort Apps Alphabetically (A-Z)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }

            if (spaceApps.isEmpty()) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "No apps in this space yet",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            } else {
              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                itemsIndexed(spaceApps) { index, app ->
                  Surface(
                    shape = ShapeRoundSm,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                      Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                      )

                      Row {
                        IconButton(
                          onClick = { onReorderApp?.invoke(app, -1) },
                          enabled = index > 0,
                          modifier = Modifier.size(32.dp)
                        ) {
                          Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                          onClick = { onReorderApp?.invoke(app, 1) },
                          enabled = index < spaceApps.size - 1,
                          modifier = Modifier.size(32.dp)
                        ) {
                          Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // Save & Apply Button
    Button(
      onClick = {
        onSaveCustomization(
          selectedBgType,
          if (selectedBgType == Space.BACKGROUND_COLOR) selectedBgColor else null,
          if (selectedBgType == Space.BACKGROUND_IMAGE) selectedImageUri else null,
          selectedGridColumns,
          selectedIconSize,
          selectedLabelVisibility
        )
      },
      shape = ShapeRoundMd,
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("btn_apply_space_customization")
    ) {
      Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text("Apply Customization", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
    }
  }
}

// --- WIDGETS SUB-SCREEN ---

@Composable
private fun CompanionWidgetPreview(
  type: String,
  spanX: Int,
  spanY: Int,
  space: Space,
  modifier: Modifier = Modifier
) {
  BoxWithConstraints(
    modifier = modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
  ) {
    val cols = space.gridColumns.coerceIn(3, 8)
    val appSpacing = 8.dp
    val iconDp = when (space.iconSize) {
      Space.ICON_SIZE_SMALL -> 44.dp
      Space.ICON_SIZE_LARGE -> 62.dp
      else -> 52.dp
    }
    val labelHeight = if (space.labelVisibility) 20.dp else 0.dp
    val cellHeight = iconDp + labelHeight + 16.dp

    val totalGaps = appSpacing * (cols - 1)
    val cellWidth = (maxWidth - totalGaps) / cols

    val widthDp = (cellWidth * spanX + appSpacing * (spanX - 1)).coerceAtMost(maxWidth)
    val heightDp = cellHeight * spanY + appSpacing * (spanY - 1)

    val dummyPlacement = remember(type, spanX, spanY, space.id) {
      SpaceItemPlacement(
        id = "preview_$type",
        spaceId = space.id,
        itemType = SpaceItemPlacement.ITEM_TYPE_WIDGET,
        spanX = spanX,
        spanY = spanY,
        customWidgetType = type
      )
    }

    Box(
      modifier = Modifier.size(width = widthDp, height = heightDp)
    ) {
      DesktopWidgetView(
        placement = dummyPlacement,
        space = space,
        onRemove = null,
        appWidgetHost = null,
        isResizeMode = false
      )

      // Invisible touch interceptor so preview interactions don't launch external activities
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
              }
            }
          }
      )
    }
  }
}

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
      "Large elegant time and current date display"
    ) to (2 to 1),
    Triple(
      SpaceItemPlacement.WIDGET_QUICK_SEARCH,
      "Quick Search Bar",
      "Web and app launcher search pill"
    ) to (4 to 1),
    Triple(
      SpaceItemPlacement.WIDGET_CALENDAR,
      "Calendar Card",
      "Date, day of week, and month indicator"
    ) to (2 to 2),
    Triple(
      SpaceItemPlacement.WIDGET_BATTERY_STATUS,
      "Battery Status",
      "Live battery percentage and charging status"
    ) to (2 to 1),
    Triple(
      SpaceItemPlacement.WIDGET_QUICK_NOTES,
      "Quick Notes",
      "Compact notepad for desktop reminders"
    ) to (4 to 2),
    Triple(
      SpaceItemPlacement.WIDGET_USAGE_STATS,
      "Usage Statistics",
      "Live launch metrics and top app for active Space"
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
        shape = ShapeRoundLg,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("widget_item_$type")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(AppDimens.Spacing16),
          verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
        ) {
          // --- TOP: Widget Title, Icon, Description & Dimension Badge ---
          Row(
            modifier = Modifier.fillMaxWidth(),
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
                  .size(38.dp)
                  .clip(ShapeRoundSm)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = when (type) {
                    SpaceItemPlacement.WIDGET_CLOCK_DATE -> Icons.Default.Schedule
                    SpaceItemPlacement.WIDGET_QUICK_SEARCH -> Icons.Default.Search
                    SpaceItemPlacement.WIDGET_CALENDAR -> Icons.Default.CalendarToday
                    SpaceItemPlacement.WIDGET_BATTERY_STATUS -> Icons.Default.AutoAwesome
                    SpaceItemPlacement.WIDGET_USAGE_STATS -> Icons.Default.Insights
                    else -> Icons.Default.Notes
                  },
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(20.dp)
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = title,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = description,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.5.sp
                )
              }
            }

            Surface(
              shape = ShapeRoundSm,
              color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
              modifier = Modifier.padding(start = AppDimens.Spacing8)
            ) {
              Text(
                text = "${spanX}x$spanY",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = 4.dp)
              )
            }
          }

          // --- MIDDLE: Widget Preview ---
          CompanionWidgetPreview(
            type = type,
            spanX = spanX,
            spanY = spanY,
            space = space
          )

          // --- BOTTOM: Add Button ---
          Button(
            onClick = {
              onAddWidget(selectedTargetPage, type, spanX, spanY, -1, null, null)
            },
            shape = ShapeRoundSm,
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            contentPadding = PaddingValues(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing8),
            modifier = Modifier
              .fillMaxWidth()
              .height(42.dp)
              .testTag("btn_add_widget_$type")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(AppDimens.Spacing8))
            Text(
              text = "Add to Page ${selectedTargetPage + 1}",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
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
        val providerPackage = provider.provider.packageName
        val providerClass = provider.provider.className
        val label = provider.loadLabel(context.packageManager)

        Card(
          shape = ShapeRoundLg,
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
          ),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(AppDimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
          ) {
            // --- TOP: System Widget Title, Icon, Package & Badge ---
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
              ) {
                val appIcon = remember(provider) {
                  try {
                    provider.loadIcon(context, 0)
                  } catch (e: Exception) {
                    null
                  }
                }
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .clip(ShapeRoundSm)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                  contentAlignment = Alignment.Center
                ) {
                  if (appIcon != null) {
                    val iconBitmap = remember(appIcon) { appIcon.toBitmap(72, 72).asImageBitmap() }
                    androidx.compose.foundation.Image(
                      bitmap = iconBitmap,
                      contentDescription = null,
                      modifier = Modifier.size(24.dp)
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Default.Widgets,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onPrimaryContainer,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = providerPackage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp,
                    maxLines = 1
                  )
                }
              }

              Surface(
                shape = ShapeRoundSm,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = AppDimens.Spacing8)
              ) {
                Text(
                  text = "System",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = AppDimens.Spacing8, vertical = 4.dp)
                )
              }
            }

            // --- MIDDLE: System Widget Preview ---
            val previewDrawable = remember(provider) {
              try {
                provider.loadPreviewImage(context, 0)
              } catch (e: Exception) {
                null
              }
            }

            val sysSpanX = 2
            val sysSpanY = 2
            BoxWithConstraints(
              modifier = Modifier.fillMaxWidth(),
              contentAlignment = Alignment.Center
            ) {
              val cols = space.gridColumns.coerceIn(3, 8)
              val appSpacing = 8.dp
              val iconDp = when (space.iconSize) {
                Space.ICON_SIZE_SMALL -> 44.dp
                Space.ICON_SIZE_LARGE -> 62.dp
                else -> 52.dp
              }
              val labelHeight = if (space.labelVisibility) 20.dp else 0.dp
              val cellHeight = iconDp + labelHeight + 16.dp

              val totalGaps = appSpacing * (cols - 1)
              val cellWidth = (maxWidth - totalGaps) / cols

              val widthDp = (cellWidth * sysSpanX + appSpacing * (sysSpanX - 1)).coerceAtMost(maxWidth)
              val heightDp = cellHeight * sysSpanY + appSpacing * (sysSpanY - 1)

              if (previewDrawable != null) {
                val previewBitmap = remember(previewDrawable) {
                  try {
                    previewDrawable.toBitmap(400, 250).asImageBitmap()
                  } catch (e: Exception) {
                    null
                  }
                }
                if (previewBitmap != null) {
                  Card(
                    shape = ShapeRoundLg,
                    colors = CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.size(width = widthDp, height = heightDp)
                  ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                      androidx.compose.foundation.Image(
                        bitmap = previewBitmap,
                        contentDescription = "Widget Preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                          .fillMaxSize()
                          .padding(AppDimens.Spacing8)
                      )
                    }
                  }
                }
              } else {
                Card(
                  shape = ShapeRoundLg,
                  colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                  ),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                  modifier = Modifier.size(width = widthDp, height = heightDp)
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(AppDimens.Spacing16),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
                  ) {
                    Icon(
                      imageVector = Icons.Default.Widgets,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(28.dp)
                    )
                    Column {
                      Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                      )
                      Text(
                        text = "Android System Widget (2x2)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }
                }
              }
            }

            // --- BOTTOM: Add Button ---
            Button(
              onClick = {
                onAddWidget(
                  selectedTargetPage,
                  "SYSTEM_WIDGET",
                  2,
                  2,
                  -1,
                  providerPackage,
                  providerClass
                )
              },
              shape = ShapeRoundSm,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ),
              contentPadding = PaddingValues(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing8),
              modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(AppDimens.Spacing8))
              Text(
                text = "Add to Page ${selectedTargetPage + 1}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
              )
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
  var selectedTheme by remember(space.appTheme) { mutableStateOf(space.appTheme) }
  var selectedColumns by remember(space.gridColumns) { mutableIntStateOf(space.gridColumns) }
  var selectedIconSize by remember(space.iconSize) { mutableStateOf(space.iconSize) }
  var showLabels by remember(space.labelVisibility) { mutableStateOf(space.labelVisibility) }

  val activePalette = remember(selectedTheme) { AppThemeHelper.getPalette(selectedTheme) }

  val columnsList = listOf(3, 4, 5, 6)
  val iconSizes = listOf(
    Space.ICON_SIZE_SMALL to ("Small" to "44dp"),
    Space.ICON_SIZE_MEDIUM to ("Standard" to "52dp"),
    Space.ICON_SIZE_LARGE to ("Large" to "62dp")
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing20)
  ) {
    // 1. LIVE DESKTOP PREVIEW CARD
    Card(
      shape = ShapeRoundLg,
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
      ),
      border = BorderStroke(1.dp, activePalette.primaryColor.copy(alpha = 0.35f)),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("theme_live_preview_card")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(AppDimens.Spacing16),
        verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
          ) {
            Icon(
              imageVector = Icons.Default.Visibility,
              contentDescription = null,
              tint = activePalette.primaryColor,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "Live Desktop Preview",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Surface(
            shape = CircleShape,
            color = activePalette.primaryColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, activePalette.primaryColor.copy(alpha = 0.4f))
          ) {
            Text(
              text = activePalette.name,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = activePalette.primaryColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
        }

        // Preview app row
        val previewApps = listOf(
          Triple("Phone", Icons.Default.Phone, activePalette.primaryColor),
          Triple("Mail", Icons.Default.Email, activePalette.secondaryColor),
          Triple("Search", Icons.Default.Search, activePalette.primaryColor),
          Triple("Apps", Icons.Default.Apps, activePalette.secondaryColor)
        )

        val previewIconDp = when (selectedIconSize) {
          Space.ICON_SIZE_SMALL -> 40.dp
          Space.ICON_SIZE_LARGE -> 54.dp
          else -> 46.dp
        }

        Surface(
          shape = ShapeRoundMd,
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
          border = BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = AppDimens.Spacing12, vertical = AppDimens.Spacing16),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            val isThemed = !selectedTheme.equals(Space.THEME_DEFAULT, ignoreCase = true)
            previewApps.take(selectedColumns.coerceAtMost(4)).forEach { (name, icon, tint) ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
              ) {
                Box(
                  modifier = Modifier
                    .size(previewIconDp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isThemed) activePalette.iconBackgroundColor else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .border(
                      width = if (isThemed) 1.5.dp else 1.dp,
                      color = if (isThemed) activePalette.primaryColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant,
                      shape = RoundedCornerShape(14.dp)
                    ),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = icon,
                    contentDescription = name,
                    tint = if (isThemed) activePalette.primaryColor else tint,
                    modifier = Modifier.size(previewIconDp * 0.55f)
                  )
                }

                if (showLabels) {
                  Spacer(modifier = Modifier.height(AppDimens.Spacing4))
                  Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }
            }
          }
        }
      }
    }

    // 2. APP THEME PALETTE SECTION
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "App Theme Palette",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Color accent & icon styling applied to desktop apps",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Icon(
          imageVector = Icons.Default.Palette,
          contentDescription = null,
          tint = QuantumViolet
        )
      }

      // 2-column Grid of Themes
      Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
        AppThemeHelper.PALETTES.chunked(2).forEach { rowPalettes ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
          ) {
            rowPalettes.forEach { palette ->
              val isSelected = selectedTheme.equals(palette.id, ignoreCase = true)
              Surface(
                shape = ShapeRoundMd,
                color = if (isSelected) QuantumViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                border = if (isSelected) BorderStroke(2.dp, QuantumViolet) else BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    selectedTheme = palette.id
                    onUpdateTheme(palette.id, selectedColumns, selectedIconSize, showLabels)
                  }
                  .testTag("theme_card_${palette.id.lowercase()}")
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimens.Spacing12),
                  verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    // Mini preview icon chip
                    Box(
                      modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.iconBackgroundColor)
                        .border(1.2.dp, palette.primaryColor.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = palette.primaryColor,
                        modifier = Modifier.size(18.dp)
                      )
                    }

                    // Color dots & selection indicator
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(10.dp)
                          .clip(CircleShape)
                          .background(palette.primaryColor)
                      )
                      Box(
                        modifier = Modifier
                          .size(10.dp)
                          .clip(CircleShape)
                          .background(palette.secondaryColor)
                      )
                      if (isSelected) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                          imageVector = Icons.Default.CheckCircle,
                          contentDescription = "Selected",
                          tint = QuantumViolet,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }
                  }

                  Column {
                    Text(
                      text = palette.name,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurface,
                      maxLines = 1
                    )
                    Text(
                      text = palette.description,
                      style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1
                    )
                  }
                }
              }
            }
            if (rowPalettes.size == 1) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }

    // 3. GRID DENSITY SECTION
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Grid Density",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Number of columns for desktop applications",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Icon(
          imageVector = Icons.Default.GridView,
          contentDescription = null,
          tint = QuantumViolet
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
      ) {
        columnsList.forEach { cols ->
          val isSelected = selectedColumns == cols
          val label = when (cols) {
            3 -> "Spacious"
            4 -> "Standard"
            5 -> "Compact"
            6 -> "Dense"
            else -> "$cols Cols"
          }

          Surface(
            shape = ShapeRoundMd,
            color = if (isSelected) QuantumViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (isSelected) BorderStroke(2.dp, QuantumViolet) else BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
              .weight(1f)
              .clickable {
                selectedColumns = cols
                onUpdateTheme(selectedTheme, cols, selectedIconSize, showLabels)
              }
              .testTag("grid_col_$cols")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.Spacing12, horizontal = AppDimens.Spacing6),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing4)
            ) {
              // Mini column indicator dots
              Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                repeat(cols) {
                  Box(
                    modifier = Modifier
                      .size(4.dp)
                      .clip(CircleShape)
                      .background(if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                  )
                }
              }

              Text(
                text = "${cols}x",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
              )
            }
          }
        }
      }
    }

    // 4. ICON SIZE SECTION
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Icon Size",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Visual dimension of application icons",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Icon(
          imageVector = Icons.Default.Tune,
          contentDescription = null,
          tint = QuantumViolet
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
      ) {
        iconSizes.forEach { (sizeKey, sizeData) ->
          val (sizeTitle, sizeDp) = sizeData
          val isSelected = selectedIconSize == sizeKey
          val previewBoxSize = when (sizeKey) {
            Space.ICON_SIZE_SMALL -> 18.dp
            Space.ICON_SIZE_LARGE -> 28.dp
            else -> 23.dp
          }

          Surface(
            shape = ShapeRoundMd,
            color = if (isSelected) QuantumViolet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainerHigh,
            border = if (isSelected) BorderStroke(2.dp, QuantumViolet) else BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
              .weight(1f)
              .clickable {
                selectedIconSize = sizeKey
                onUpdateTheme(selectedTheme, selectedColumns, sizeKey, showLabels)
              }
              .testTag("icon_size_$sizeKey")
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimens.Spacing12, horizontal = AppDimens.Spacing8),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing6)
            ) {
              Box(
                modifier = Modifier
                  .size(previewBoxSize)
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isSelected) QuantumViolet else MaterialTheme.colorScheme.surfaceContainerHighest)
                  .border(1.dp, if (isSelected) QuantumViolet else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp))
              )

              Text(
                text = sizeTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurface
              )

              Text(
                text = sizeDp,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = if (isSelected) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }

    // 5. APP LABELS TOGGLE
    Card(
      shape = ShapeRoundMd,
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
      ),
      border = BorderStroke(AppDimens.BorderThin, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
          horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing12),
          modifier = Modifier.weight(1f)
        ) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(ShapeRoundSm)
              .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (showLabels) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = null,
              tint = if (showLabels) QuantumViolet else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp)
            )
          }

          Column {
            Text(
              text = "Show Icon Labels",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Display app name text under icons on desktop",
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

    // 6. APPLY TO SPACE BUTTON
    Button(
      onClick = {
        onUpdateTheme(selectedTheme, selectedColumns, selectedIconSize, showLabels)
      },
      colors = ButtonDefaults.buttonColors(containerColor = QuantumViolet),
      shape = ShapeRoundMd,
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("btn_apply_theme_and_layout")
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = Color.White
        )
        Text(
          text = "Apply Theme & Layout",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          color = Color.White
        )
      }
    }

    Spacer(modifier = Modifier.height(AppDimens.Spacing8))
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
