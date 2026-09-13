package com.multispace.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyRowItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dock
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.platform.AppUsageTracker
import com.multispace.presentation.components.AlphabetFastScroll
import com.multispace.ui.components.ModernCard
import com.multispace.ui.components.ModernDialogContainer
import com.multispace.ui.components.ModernEmptyState
import com.multispace.ui.components.ModernSearchField
import com.multispace.ui.theme.AppDimens
import com.multispace.ui.theme.QuantumViolet
import com.multispace.ui.theme.ShapeRoundLg
import com.multispace.ui.theme.ShapeRoundMd
import com.multispace.ui.theme.ShapeRoundSm
import kotlinx.coroutines.launch

/**
 * Pre-cached collections for Layer 2 to avoid re-sorting, re-grouping,
 * and rebuilding alphabet indexes during continuous layer gestures.
 */
data class Layer2CachedCatalog(
  val sortedApps: List<DiscoveredApp>,
  val groupedApps: Map<Char, List<DiscoveredApp>>,
  val letterToSectionIndex: Map<Char, Int>,
  val letterToFirstIndex: Map<Char, Int>,
  val activeLetters: Set<Char>
)

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
  onUninstallApp: (DiscoveredApp) -> Unit = {},
  onForceStopApp: (DiscoveredApp) -> Unit = {},
  onCloseLayer2: () -> Unit,
  mostUsedApps: List<DiscoveredApp>? = null,
  cachedCatalog: Layer2CachedCatalog? = null,
  gridState: LazyGridState = rememberLazyGridState(),
  sectionListState: LazyListState = rememberLazyListState(),
  isSectionedAlphabeticalView: Boolean = false,
  onToggleSectionedAlphabeticalView: () -> Unit = {},
  topBarModifier: Modifier = Modifier,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedAppForMenu by remember { mutableStateOf<DiscoveredApp?>(null) }

  val context = LocalContext.current
  val usageTracker = remember(context) { AppUsageTracker.getInstance(context) }

  // 1. Most Used Apps Section (bounded strictly to the number of columns in Layer 2)
  val maxMostUsedApps = space.gridColumns.coerceIn(2, 8)
  val resolvedMostUsedApps = remember(spaceApps, searchQuery, mostUsedApps, maxMostUsedApps) {
    val baseList = if (mostUsedApps != null) {
      if (searchQuery.isBlank()) {
        mostUsedApps
      } else {
        val q = searchQuery.trim().lowercase()
        mostUsedApps.filter {
          it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
      }
    } else {
      val pool = if (searchQuery.isBlank()) {
        spaceApps
      } else {
        val q = searchQuery.trim().lowercase()
        spaceApps.filter {
          it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
      }
      usageTracker.getMostUsedApps(pool, limit = maxMostUsedApps)
    }
    baseList.take(maxMostUsedApps)
  }

  // 2. Alphabetical Apps Section (maintains normal alphabetical ordering by app name)
  val isSearchBlank = searchQuery.isBlank()
  val alphabeticalApps = remember(spaceApps, searchQuery, cachedCatalog) {
    if (isSearchBlank && cachedCatalog != null) {
      cachedCatalog.sortedApps
    } else {
      val baseList = if (isSearchBlank) {
        spaceApps
      } else {
        val q = searchQuery.trim().lowercase()
        spaceApps.filter {
          it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
      }
      baseList.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
  }

  // Grouped by first letter for the sectioned alphabetical view
  val groupedAlphabeticalApps = remember(alphabeticalApps, isSearchBlank, cachedCatalog) {
    if (isSearchBlank && cachedCatalog != null) {
      cachedCatalog.groupedApps
    } else {
      val map = linkedMapOf<Char, MutableList<DiscoveredApp>>()
      alphabeticalApps.forEach { app ->
        val cleanLabel = app.label.trim().trim('"', '\'', '(', '[', '{')
        val firstChar = cleanLabel.firstOrNull()?.uppercaseChar() ?: '#'
        val groupKey = if (firstChar in 'A'..'Z') firstChar else '#'
        map.getOrPut(groupKey) { mutableListOf() }.add(app)
      }
      map
    }
  }

  val letterToSectionIndex = remember(groupedAlphabeticalApps, isSearchBlank, cachedCatalog) {
    if (isSearchBlank && cachedCatalog != null) {
      cachedCatalog.letterToSectionIndex
    } else {
      val map = mutableMapOf<Char, Int>()
      groupedAlphabeticalApps.keys.forEachIndexed { index, char ->
        map[char] = index
      }
      map
    }
  }

  val isVerticalMode = space.layer2DisplayMode != Space.DISPLAY_MODE_PAGE
  val showAlphabetIndex = isVerticalMode && alphabeticalApps.isNotEmpty()

  val coroutineScope = rememberCoroutineScope()

  val alphabet = remember { ('A'..'Z').toList() }

  // Map each letter A-Z to the index of the first matching app in alphabeticalApps.
  // This operates ONLY on the alphabetical list, not the Most Used section.
  val letterToFirstIndex = remember(alphabeticalApps, isSearchBlank, cachedCatalog) {
    if (isSearchBlank && cachedCatalog != null) {
      cachedCatalog.letterToFirstIndex
    } else {
      val map = mutableMapOf<Char, Int>()
      alphabeticalApps.forEachIndexed { index, app ->
        val cleanLabel = app.label.trim().trim('"', '\'', '(', '[', '{')
        val firstChar = cleanLabel.firstOrNull()?.uppercaseChar()
        if (firstChar != null && firstChar in 'A'..'Z') {
          if (!map.containsKey(firstChar)) {
            map[firstChar] = index
          }
        }
      }
      map
    }
  }
  val activeLetters = remember(letterToFirstIndex, isSearchBlank, cachedCatalog) {
    if (isSearchBlank && cachedCatalog != null) {
      cachedCatalog.activeLetters
    } else {
      letterToFirstIndex.keys
    }
  }

  LaunchedEffect(searchQuery) {
    if (alphabeticalApps.isNotEmpty()) {
      gridState.scrollToItem(0)
      sectionListState.scrollToItem(0)
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
      .background(MaterialTheme.colorScheme.background.copy(alpha = 0.80f))
      .statusBarsPadding()
      .padding(top = AppDimens.Spacing8)
      .testTag("layer2_library_screen")
  ) {
    // Header section (Top Bar + Most Used Row + Divider): accepts topBarModifier for smooth downward drag
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .then(topBarModifier)
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
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

      Spacer(modifier = Modifier.height(AppDimens.Spacing8))

      // 1. Most Used Apps Row (at the very top, ordered by usage/frequency)
      if (resolvedMostUsedApps.isNotEmpty()) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("layer2_most_used_section")
        ) {
          Text(
            text = "Most Used",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.SemiBold,
              letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = AppDimens.Spacing20, vertical = AppDimens.Spacing2)
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(
                start = AppDimens.Spacing16,
                end = if (showAlphabetIndex) 28.dp else AppDimens.Spacing16,
                top = AppDimens.Spacing4,
                bottom = AppDimens.Spacing4
              )
              .testTag("layer2_most_used_row"),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
          ) {
            resolvedMostUsedApps.forEach { app ->
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                  .weight(1f)
                  .clip(ShapeRoundMd)
                  .combinedClickable(
                    onClick = { onLaunchApp(app) },
                    onLongClick = { selectedAppForMenu = app }
                  )
                .padding(vertical = AppDimens.Spacing4)
                .testTag("layer2_most_used_app_${app.packageName}")
            ) {
              val bitmap = remember(app.id) { getBitmap(app) }
              ThemedAppIcon(
                app = app,
                bitmap = bitmap,
                appTheme = space.appTheme,
                modifier = iconSizeModifier,
                fallbackText = app.label.take(1).uppercase()
              )

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

          // Pad empty slots if available apps are fewer than gridColumns to maintain consistent column width
          val emptySlots = space.gridColumns - resolvedMostUsedApps.size
          if (emptySlots > 0) {
            repeat(emptySlots) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }

        // Clear vertical padding and subtle/dim horizontal divider line separating Most Used from Alphabetical list
        Spacer(modifier = Modifier.height(AppDimens.Spacing10))
        HorizontalDivider(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppDimens.Spacing20)
            .testTag("layer2_divider"),
          thickness = 0.8.dp,
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
        Spacer(modifier = Modifier.height(AppDimens.Spacing10))
      }
    }
  }

    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        // 2. Alphabetical section (below Most Used row, normal alphabetical ordering)
        if (alphabeticalApps.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f)
              .navigationBarsPadding()
              .then(topBarModifier),
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
        } else if (isSectionedAlphabeticalView) {
          // Sectioned Alphabetical View: Alphabet letter on the leftmost side, and rows of apps with that letter listed
          val sectionCols = (space.gridColumns - 1).coerceAtLeast(2)

          LazyColumn(
            state = sectionListState,
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(
                start = AppDimens.Spacing16,
                end = if (showAlphabetIndex) 28.dp else AppDimens.Spacing16
              )
              .testTag("layer2_alphabet_sections_list"),
            contentPadding = PaddingValues(
              bottom = AppDimens.Spacing24 + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
          ) {
            groupedAlphabeticalApps.forEach { (letter, appsForLetter) ->
              item(key = "section_$letter") {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.Spacing6)
                    .testTag("layer2_section_$letter")
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                  ) {
                    // Leftmost side: The Alphabet Letter Badge
                    Box(
                      modifier = Modifier
                        .width(42.dp)
                        .padding(top = 4.dp),
                      contentAlignment = Alignment.TopCenter
                    ) {
                      Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(
                          1.dp,
                          MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
                        ),
                        modifier = Modifier.size(36.dp)
                      ) {
                        Box(
                          contentAlignment = Alignment.Center,
                          modifier = Modifier.fillMaxSize()
                        ) {
                          Text(
                            text = letter.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                              fontWeight = FontWeight.Black,
                              fontSize = 17.sp
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                          )
                        }
                      }
                    }

                    Spacer(modifier = Modifier.width(AppDimens.Spacing8))

                    // Rows of apps with that alphabet listed
                    val appChunks = remember(appsForLetter, sectionCols) {
                      appsForLetter.chunked(sectionCols)
                    }

                    Column(
                      modifier = Modifier.weight(1f),
                      verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing12)
                    ) {
                      appChunks.forEach { rowApps ->
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8)
                        ) {
                          rowApps.forEach { app ->
                            Column(
                              horizontalAlignment = Alignment.CenterHorizontally,
                              modifier = Modifier
                                .weight(1f)
                                .clip(ShapeRoundMd)
                                .combinedClickable(
                                  onClick = { onLaunchApp(app) },
                                  onLongClick = { selectedAppForMenu = app }
                                )
                                .padding(AppDimens.Spacing4)
                                .testTag("layer2_app_${app.packageName}")
                            ) {
                              val bitmap = remember(app.id) { getBitmap(app) }
                              ThemedAppIcon(
                                app = app,
                                bitmap = bitmap,
                                appTheme = space.appTheme,
                                modifier = iconSizeModifier,
                                fallbackText = app.label.take(1).uppercase()
                              )

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

                          // Empty slot spacers so apps align with column widths
                          val emptySlots = sectionCols - rowApps.size
                          repeat(emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                          }
                        }
                      }
                    }
                  }

                  // Subtle horizontal divider between letter sections
                  Spacer(modifier = Modifier.height(AppDimens.Spacing8))
                  HorizontalDivider(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(start = 50.dp, end = AppDimens.Spacing8),
                    thickness = 0.6.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.20f)
                  )
                }
              }
            }
          }
        } else {
          LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(space.gridColumns),
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .padding(
                start = AppDimens.Spacing16,
                end = if (showAlphabetIndex) 28.dp else AppDimens.Spacing16
              )
              .testTag("layer2_apps_grid"),
            horizontalArrangement = Arrangement.spacedBy(AppDimens.Spacing8),
            verticalArrangement = Arrangement.spacedBy(AppDimens.Spacing16),
            contentPadding = PaddingValues(
              bottom = AppDimens.Spacing24 + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
          ) {
            items(alphabeticalApps, key = { "${it.packageName}/${it.activityName}" }) { app ->
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
                val bitmap = remember(app.id) { getBitmap(app) }
                ThemedAppIcon(
                  app = app,
                  bitmap = bitmap,
                  appTheme = space.appTheme,
                  modifier = iconSizeModifier,
                  fallbackText = app.label.take(1).uppercase()
                )

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

      // 3. Alphabet fast-scroll indicator with toggle icon above it
      if (showAlphabetIndex) {
        Column(
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 2.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Small toggle icon button above AlphabetScroller
          Surface(
            shape = CircleShape,
            color = if (isSectionedAlphabeticalView) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
            },
            border = BorderStroke(
              0.8.dp,
              if (isSectionedAlphabeticalView) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            ),
            shadowElevation = if (isSectionedAlphabeticalView) 2.dp else 0.dp,
            modifier = Modifier.size(24.dp)
          ) {
            IconButton(
              onClick = onToggleSectionedAlphabeticalView,
              modifier = Modifier
                .fillMaxSize()
                .testTag("layer2_alphabet_section_toggle")
            ) {
              Icon(
                imageVector = Icons.Default.SortByAlpha,
                contentDescription = if (isSectionedAlphabeticalView) {
                  "Switch to Standard Grid View"
                } else {
                  "Switch to Alphabetical Section View"
                },
                tint = if (isSectionedAlphabeticalView) {
                  MaterialTheme.colorScheme.onPrimary
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(14.dp)
              )
            }
          }

          AlphabetFastScroll(
            alphabet = alphabet,
            activeLetters = activeLetters,
            onLetterSelected = { letter ->
              if (isSectionedAlphabeticalView) {
                val targetSectionIdx = letterToSectionIndex[letter] ?: run {
                  groupedAlphabeticalApps.keys.filter { it >= letter }.minOrNull()?.let { letterToSectionIndex[it] }
                }
                targetSectionIdx?.let { idx ->
                  coroutineScope.launch {
                    sectionListState.scrollToItem(idx)
                  }
                }
              } else {
                letterToFirstIndex[letter]?.let { targetIndex ->
                  coroutineScope.launch {
                    gridState.scrollToItem(targetIndex)
                  }
                }
              }
            }
          )
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

        val isUninstallable = remember(app) {
          com.multispace.platform.PackageActionHelper.isPackageUninstallable(context, app)
        }

        if (isUninstallable) {
          ModernCard(
            onClick = {
              onUninstallApp(app)
              selectedAppForMenu = null
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_app_uninstall_action"),
            shape = ShapeRoundMd
          ) {
            Row(
              modifier = Modifier.padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.DeleteOutline, contentDescription = "Uninstall App", tint = MaterialTheme.colorScheme.error)
              Spacer(modifier = Modifier.width(AppDimens.Spacing12))
              Text(
                "Uninstall App",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.error
              )
            }
          }
        } else {
          ModernCard(
            onClick = {
              onForceStopApp(app)
              selectedAppForMenu = null
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("btn_app_force_stop_action"),
            shape = ShapeRoundMd
          ) {
            Row(
              modifier = Modifier.padding(horizontal = AppDimens.Spacing16, vertical = AppDimens.Spacing12),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Close, contentDescription = "Force Stop", tint = MaterialTheme.colorScheme.error)
              Spacer(modifier = Modifier.width(AppDimens.Spacing12))
              Text(
                "Force Stop",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.error
              )
            }
          }
        }
      }
    }
  }
}
