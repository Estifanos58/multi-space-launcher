package com.multispace.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.multispace.data.database.LauncherDatabase
import com.multispace.data.preferences.LauncherPreferences
import com.multispace.data.repository.RoomSpaceRepository
import com.multispace.diagnostics.AppLogger
import com.multispace.domain.model.DiscoveredApp
import com.multispace.domain.model.Space
import com.multispace.domain.model.SpaceMembership
import com.multispace.domain.repository.SpaceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SpaceUiState(
  val spaces: List<Space> = emptyList(),
  val activeSpace: Space? = null,
  val activeSpaceMemberships: List<SpaceMembership> = emptyList(),
  val isLoading: Boolean = false,
  val errorMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SpaceViewModel(application: Application) : AndroidViewModel(application) {

  private val database = LauncherDatabase.getInstance(application.applicationContext)
  private val preferences = LauncherPreferences(application.applicationContext)
  val spaceRepository: SpaceRepository = RoomSpaceRepository(
    spaceDao = database.spaceDao(),
    membershipDao = database.spaceMembershipDao(),
    layoutDao = database.spaceLayoutDao(),
    preferences = preferences
  )

  private val _userFeedback = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val userFeedback: SharedFlow<String> = _userFeedback.asSharedFlow()

  fun postFeedback(message: String) {
    _userFeedback.tryEmit(message)
  }

  // Layer State (1 = Layer 1 Curated Home, 2 = Layer 2 Space App Library)
  private val _activeLayerIndex = MutableStateFlow(1)
  val activeLayerIndex: StateFlow<Int> = _activeLayerIndex.asStateFlow()

  fun setLayer(layer: Int) {
    _activeLayerIndex.value = if (layer == 2) 2 else 1
  }

  fun toggleLayer() {
    _activeLayerIndex.value = if (_activeLayerIndex.value == 1) 2 else 1
  }

  val allSpaces: StateFlow<List<Space>> = spaceRepository.allSpacesFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = emptyList()
    )

  val activeSpace: StateFlow<Space?> = spaceRepository.activeSpaceFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = null
    )

  val activeMemberships: StateFlow<List<SpaceMembership>> = spaceRepository.activeSpaceFlow
    .flatMapLatest { space ->
      if (space != null) {
        spaceRepository.getMembershipsForSpaceFlow(space.id)
      } else {
        flowOf(emptyList())
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = emptyList()
    )

  val activePlacements: StateFlow<List<com.multispace.domain.model.SpaceItemPlacement>> = spaceRepository.activeSpaceFlow
    .flatMapLatest { space ->
      if (space != null) {
        spaceRepository.getPlacementsForSpaceLayerFlow(space.id, com.multispace.domain.model.SpaceItemPlacement.LAYER_HOME)
      } else {
        flowOf(emptyList())
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = emptyList()
    )

  val activeFolders: StateFlow<List<com.multispace.domain.model.SpaceFolder>> = spaceRepository.activeSpaceFlow
    .flatMapLatest { space ->
      if (space != null) {
        spaceRepository.getFoldersForSpaceFlow(space.id)
      } else {
        flowOf(emptyList())
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = emptyList()
    )

  val activeDockItems: StateFlow<List<com.multispace.domain.model.SpaceDockItem>> = spaceRepository.activeSpaceFlow
    .flatMapLatest { space ->
      if (space != null) {
        spaceRepository.getDockItemsForSpaceFlow(space.id)
      } else {
        flowOf(emptyList())
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.Eagerly,
      initialValue = emptyList()
    )

  init {
    AppLogger.i(AppLogger.Category.LAUNCHER, "SpaceViewModel initialized: ensuring default Space state")
    ensureDefaultSpaceInitialized()
  }

  fun ensureDefaultSpaceInitialized(apps: List<DiscoveredApp> = emptyList()) {
    viewModelScope.launch {
      val appList = if (apps.isNotEmpty()) {
        apps
      } else {
        try {
          com.multispace.platform.AppDiscoveryManager(getApplication<Application>().applicationContext).loadInstalledApps()
        } catch (e: Exception) {
          AppLogger.w(AppLogger.Category.LAUNCHER, "Failed to load installed apps for default space initialization: ${e.message}")
          emptyList()
        }
      }
      spaceRepository.ensureDefaultSpaceInitialized(appList)
    }
  }

  fun getMembershipsFlowForSpace(spaceId: String): Flow<List<SpaceMembership>> {
    return spaceRepository.getMembershipsForSpaceFlow(spaceId)
  }

  fun createSpace(name: String) {
    viewModelScope.launch {
      val result = spaceRepository.createSpace(name)
      result.fold(
        onSuccess = { created ->
          _userFeedback.tryEmit("Space '${created.name}' created successfully.")
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to create Space."
          _userFeedback.tryEmit("Error: $msg")
        }
      )
    }
  }

  fun createFullSpace(
    name: String,
    authPolicy: String = Space.AUTH_NONE,
    pinSalt: String? = null,
    pinHash: String? = null,
    patternRows: Int = Space.DEFAULT_PATTERN_ROWS,
    patternCols: Int = Space.DEFAULT_PATTERN_COLS,
    backgroundType: String = Space.BACKGROUND_DEFAULT,
    backgroundColor: Long? = null,
    backgroundImageUri: String? = null,
    homeWallpaperType: String = Space.BACKGROUND_DEFAULT,
    homeWallpaperColor: Long? = null,
    homeWallpaperImageUri: String? = null,
    phoneLockWallpaperType: String = Space.BACKGROUND_DEFAULT,
    phoneLockWallpaperColor: Long? = null,
    phoneLockWallpaperImageUri: String? = null,
    spaceLockWallpaperType: String = Space.BACKGROUND_DEFAULT,
    spaceLockWallpaperColor: Long? = null,
    spaceLockWallpaperImageUri: String? = null,
    appTheme: String = Space.THEME_DEFAULT,
    gridColumns: Int = Space.DEFAULT_GRID_COLUMNS,
    iconSize: String = Space.ICON_SIZE_MEDIUM,
    labelVisibility: Boolean = true,
    layer1DisplayMode: String = Space.DISPLAY_MODE_PAGE,
    layer2DisplayMode: String = Space.DISPLAY_MODE_SCROLL,
    layer2AccessMode: String = Space.ACCESS_MODE_DOCK_BUTTON,
    dockCapacity: Int = Space.DEFAULT_DOCK_CAPACITY,
    layoutPreset: String = Space.PRESET_DEFAULT,
    useLayer2: Boolean = true,
    homeWallpaperScaleMode: String = "crop",
    homeWallpaperZoomLevel: Float = 1.0f,
    homeWallpaperDimLevel: Float = 0.20f,
    homeWallpaperOffsetX: Float = 0.0f,
    homeWallpaperOffsetY: Float = 0.0f,
    phoneLockWallpaperScaleMode: String = "crop",
    phoneLockWallpaperZoomLevel: Float = 1.0f,
    phoneLockWallpaperDimLevel: Float = 0.20f,
    phoneLockWallpaperOffsetX: Float = 0.0f,
    phoneLockWallpaperOffsetY: Float = 0.0f,
    spaceLockWallpaperScaleMode: String = "crop",
    spaceLockWallpaperZoomLevel: Float = 1.0f,
    spaceLockWallpaperDimLevel: Float = 0.20f,
    spaceLockWallpaperOffsetX: Float = 0.0f,
    spaceLockWallpaperOffsetY: Float = 0.0f,
    initialApps: List<DiscoveredApp> = emptyList(),
    onResult: ((Boolean, String?) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val result = spaceRepository.createFullSpace(
        name = name,
        authPolicy = authPolicy,
        pinSalt = pinSalt,
        pinHash = pinHash,
        patternRows = patternRows,
        patternCols = patternCols,
        backgroundType = backgroundType,
        backgroundColor = backgroundColor,
        backgroundImageUri = backgroundImageUri,
        homeWallpaperType = homeWallpaperType,
        homeWallpaperColor = homeWallpaperColor,
        homeWallpaperImageUri = homeWallpaperImageUri,
        phoneLockWallpaperType = phoneLockWallpaperType,
        phoneLockWallpaperColor = phoneLockWallpaperColor,
        phoneLockWallpaperImageUri = phoneLockWallpaperImageUri,
        spaceLockWallpaperType = spaceLockWallpaperType,
        spaceLockWallpaperColor = spaceLockWallpaperColor,
        spaceLockWallpaperImageUri = spaceLockWallpaperImageUri,
        appTheme = appTheme,
        gridColumns = gridColumns,
        iconSize = iconSize,
        labelVisibility = labelVisibility,
        layer1DisplayMode = layer1DisplayMode,
        layer2DisplayMode = layer2DisplayMode,
        layer2AccessMode = layer2AccessMode,
        dockCapacity = dockCapacity,
        layoutPreset = layoutPreset,
        useLayer2 = useLayer2,
        homeWallpaperScaleMode = homeWallpaperScaleMode,
        homeWallpaperZoomLevel = homeWallpaperZoomLevel,
        homeWallpaperDimLevel = homeWallpaperDimLevel,
        homeWallpaperOffsetX = homeWallpaperOffsetX,
        homeWallpaperOffsetY = homeWallpaperOffsetY,
        phoneLockWallpaperScaleMode = phoneLockWallpaperScaleMode,
        phoneLockWallpaperZoomLevel = phoneLockWallpaperZoomLevel,
        phoneLockWallpaperDimLevel = phoneLockWallpaperDimLevel,
        phoneLockWallpaperOffsetX = phoneLockWallpaperOffsetX,
        phoneLockWallpaperOffsetY = phoneLockWallpaperOffsetY,
        spaceLockWallpaperScaleMode = spaceLockWallpaperScaleMode,
        spaceLockWallpaperZoomLevel = spaceLockWallpaperZoomLevel,
        spaceLockWallpaperDimLevel = spaceLockWallpaperDimLevel,
        spaceLockWallpaperOffsetX = spaceLockWallpaperOffsetX,
        spaceLockWallpaperOffsetY = spaceLockWallpaperOffsetY,
        initialApps = initialApps
      )
      result.fold(
        onSuccess = { created ->
          if (authPolicy != Space.AUTH_NONE) {
            unlockSpace(created.id)
          }
          _userFeedback.tryEmit("Space '${created.name}' created successfully.")
          onResult?.invoke(true, created.id)
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to create Space."
          _userFeedback.tryEmit("Error: $msg")
          onResult?.invoke(false, msg)
        }
      )
    }
  }

  fun updateFullSpace(
    spaceId: String,
    name: String,
    authPolicy: String = Space.AUTH_NONE,
    pinSalt: String? = null,
    pinHash: String? = null,
    keepExistingCredentials: Boolean = false,
    patternRows: Int = Space.DEFAULT_PATTERN_ROWS,
    patternCols: Int = Space.DEFAULT_PATTERN_COLS,
    backgroundType: String = Space.BACKGROUND_DEFAULT,
    backgroundColor: Long? = null,
    backgroundImageUri: String? = null,
    homeWallpaperType: String = Space.BACKGROUND_DEFAULT,
    homeWallpaperColor: Long? = null,
    homeWallpaperImageUri: String? = null,
    phoneLockWallpaperType: String = Space.BACKGROUND_DEFAULT,
    phoneLockWallpaperColor: Long? = null,
    phoneLockWallpaperImageUri: String? = null,
    spaceLockWallpaperType: String = Space.BACKGROUND_DEFAULT,
    spaceLockWallpaperColor: Long? = null,
    spaceLockWallpaperImageUri: String? = null,
    appTheme: String = Space.THEME_DEFAULT,
    gridColumns: Int = Space.DEFAULT_GRID_COLUMNS,
    iconSize: String = Space.ICON_SIZE_MEDIUM,
    labelVisibility: Boolean = true,
    layer1DisplayMode: String = Space.DISPLAY_MODE_PAGE,
    layer2DisplayMode: String = Space.DISPLAY_MODE_SCROLL,
    layer2AccessMode: String = Space.ACCESS_MODE_DOCK_BUTTON,
    dockCapacity: Int = Space.DEFAULT_DOCK_CAPACITY,
    layoutPreset: String = Space.PRESET_DEFAULT,
    useLayer2: Boolean = true,
    homeWallpaperScaleMode: String = "crop",
    homeWallpaperZoomLevel: Float = 1.0f,
    homeWallpaperDimLevel: Float = 0.20f,
    homeWallpaperOffsetX: Float = 0.0f,
    homeWallpaperOffsetY: Float = 0.0f,
    phoneLockWallpaperScaleMode: String = "crop",
    phoneLockWallpaperZoomLevel: Float = 1.0f,
    phoneLockWallpaperDimLevel: Float = 0.20f,
    phoneLockWallpaperOffsetX: Float = 0.0f,
    phoneLockWallpaperOffsetY: Float = 0.0f,
    spaceLockWallpaperScaleMode: String = "crop",
    spaceLockWallpaperZoomLevel: Float = 1.0f,
    spaceLockWallpaperDimLevel: Float = 0.20f,
    spaceLockWallpaperOffsetX: Float = 0.0f,
    spaceLockWallpaperOffsetY: Float = 0.0f,
    updatedApps: List<DiscoveredApp> = emptyList(),
    onResult: ((Boolean, String?) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val result = spaceRepository.updateFullSpace(
        spaceId = spaceId,
        name = name,
        authPolicy = authPolicy,
        pinSalt = pinSalt,
        pinHash = pinHash,
        keepExistingCredentials = keepExistingCredentials,
        patternRows = patternRows,
        patternCols = patternCols,
        backgroundType = backgroundType,
        backgroundColor = backgroundColor,
        backgroundImageUri = backgroundImageUri,
        homeWallpaperType = homeWallpaperType,
        homeWallpaperColor = homeWallpaperColor,
        homeWallpaperImageUri = homeWallpaperImageUri,
        phoneLockWallpaperType = phoneLockWallpaperType,
        phoneLockWallpaperColor = phoneLockWallpaperColor,
        phoneLockWallpaperImageUri = phoneLockWallpaperImageUri,
        spaceLockWallpaperType = spaceLockWallpaperType,
        spaceLockWallpaperColor = spaceLockWallpaperColor,
        spaceLockWallpaperImageUri = spaceLockWallpaperImageUri,
        appTheme = appTheme,
        gridColumns = gridColumns,
        iconSize = iconSize,
        labelVisibility = labelVisibility,
        layer1DisplayMode = layer1DisplayMode,
        layer2DisplayMode = layer2DisplayMode,
        layer2AccessMode = layer2AccessMode,
        dockCapacity = dockCapacity,
        layoutPreset = layoutPreset,
        useLayer2 = useLayer2,
        homeWallpaperScaleMode = homeWallpaperScaleMode,
        homeWallpaperZoomLevel = homeWallpaperZoomLevel,
        homeWallpaperDimLevel = homeWallpaperDimLevel,
        homeWallpaperOffsetX = homeWallpaperOffsetX,
        homeWallpaperOffsetY = homeWallpaperOffsetY,
        phoneLockWallpaperScaleMode = phoneLockWallpaperScaleMode,
        phoneLockWallpaperZoomLevel = phoneLockWallpaperZoomLevel,
        phoneLockWallpaperDimLevel = phoneLockWallpaperDimLevel,
        phoneLockWallpaperOffsetX = phoneLockWallpaperOffsetX,
        phoneLockWallpaperOffsetY = phoneLockWallpaperOffsetY,
        spaceLockWallpaperScaleMode = spaceLockWallpaperScaleMode,
        spaceLockWallpaperZoomLevel = spaceLockWallpaperZoomLevel,
        spaceLockWallpaperDimLevel = spaceLockWallpaperDimLevel,
        spaceLockWallpaperOffsetX = spaceLockWallpaperOffsetX,
        spaceLockWallpaperOffsetY = spaceLockWallpaperOffsetY,
        updatedApps = updatedApps
      )
      result.fold(
        onSuccess = { updated ->
          if (authPolicy != Space.AUTH_NONE) {
            unlockSpace(updated.id)
          }
          _userFeedback.tryEmit("Space '${updated.name}' updated successfully.")
          onResult?.invoke(true, null)
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to update Space."
          _userFeedback.tryEmit("Error: $msg")
          onResult?.invoke(false, msg)
        }
      )
    }
  }

  fun renameSpace(spaceId: String, newName: String) {
    viewModelScope.launch {
      val result = spaceRepository.renameSpace(spaceId, newName)
      result.fold(
        onSuccess = {
          _userFeedback.tryEmit("Space renamed to '$newName'.")
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to rename Space."
          _userFeedback.tryEmit("Error: $msg")
        }
      )
    }
  }

  fun deleteSpace(spaceId: String) {
    viewModelScope.launch {
      val result = spaceRepository.deleteSpace(spaceId)
      result.fold(
        onSuccess = {
          _userFeedback.tryEmit("Space deleted successfully.")
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to delete Space."
          _userFeedback.tryEmit("Error: $msg")
        }
      )
    }
  }

  fun selectActiveSpace(spaceId: String) {
    viewModelScope.launch {
      val result = spaceRepository.setActiveSpaceId(spaceId)
      result.fold(
        onSuccess = {
          _userFeedback.tryEmit("Switched active Space.")
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to select active Space."
          _userFeedback.tryEmit("Error: $msg")
        }
      )
    }
  }

  fun addAppToSpace(spaceId: String, app: DiscoveredApp) {
    viewModelScope.launch {
      val result = spaceRepository.addAppToSpace(spaceId, app)
      result.fold(
        onSuccess = {
          _userFeedback.tryEmit("${app.label} added to Space.")
        },
        onFailure = { error ->
          _userFeedback.tryEmit("Error adding app: ${error.message}")
        }
      )
    }
  }

  fun removeAppFromSpace(spaceId: String, app: DiscoveredApp) {
    viewModelScope.launch {
      val result = spaceRepository.removeAppFromSpace(spaceId, app)
      result.fold(
        onSuccess = {
          _userFeedback.tryEmit("${app.label} removed from Space.")
        },
        onFailure = { error ->
          _userFeedback.tryEmit("Error removing app: ${error.message}")
        }
      )
    }
  }

  private val _unlockedSpaceIds = MutableStateFlow<Set<String>>(emptySet())
  val unlockedSpaceIds: StateFlow<Set<String>> = _unlockedSpaceIds.asStateFlow()

  private val _isPhoneLocked = MutableStateFlow(false)
  val isPhoneLocked: StateFlow<Boolean> = _isPhoneLocked.asStateFlow()

  fun isSpaceUnlocked(space: Space?): Boolean {
    if (space == null) return true
    if (!space.isProtected) return true
    return _unlockedSpaceIds.value.contains(space.id)
  }

  fun unlockSpace(spaceId: String) {
    _unlockedSpaceIds.update { it + spaceId }
  }

  fun lockSpace(spaceId: String) {
    _unlockedSpaceIds.update { it - spaceId }
  }

  fun lockAllProtectedSpaces() {
    _unlockedSpaceIds.value = emptySet()
  }

  fun lockPhone() {
    lockAllProtectedSpaces()
    _isPhoneLocked.value = true
    _userFeedback.tryEmit("Multi-Space secured. Enter credential to unlock.")
  }

  fun unlockPhone() {
    _isPhoneLocked.value = false
  }

  suspend fun authenticateAndUnlockSpaceByCredential(credential: String): Space? {
    val matchedSpace = spaceRepository.findSpaceMatchingCredential(credential)
    if (matchedSpace != null) {
      unlockSpace(matchedSpace.id)
      selectActiveSpace(matchedSpace.id)
      _isPhoneLocked.value = false
      _userFeedback.tryEmit("Unlocked into '${matchedSpace.name}'")
      return matchedSpace
    }
    return null
  }

  fun authenticateAndUnlockWithBiometric(spaceId: String? = null): Space? {
    val targetSpace = if (spaceId != null) {
      allSpaces.value.firstOrNull { it.id == spaceId }
    } else {
      activeSpace.value ?: allSpaces.value.firstOrNull()
    }
    if (targetSpace != null) {
      unlockSpace(targetSpace.id)
      selectActiveSpace(targetSpace.id)
    }
    _isPhoneLocked.value = false
    _userFeedback.tryEmit(
      if (targetSpace != null) "Biometric unlocked into '${targetSpace.name}'"
      else "Device unlocked with biometrics"
    )
    return targetSpace
  }

  suspend fun verifyAndUnlockSpace(spaceId: String, pin: String): Boolean {
    val isValid = spaceRepository.verifySpacePin(spaceId, pin)
    if (isValid) {
      unlockSpace(spaceId)
    }
    return isValid
  }

  fun setSpacePin(spaceId: String, pin: String, onResult: ((Boolean, String?) -> Unit)? = null) {
    viewModelScope.launch {
      val result = spaceRepository.setSpacePin(spaceId, pin)
      result.fold(
        onSuccess = {
          unlockSpace(spaceId)
          _userFeedback.tryEmit("PIN protection enabled.")
          onResult?.invoke(true, null)
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to set PIN."
          _userFeedback.tryEmit("Error: $msg")
          onResult?.invoke(false, msg)
        }
      )
    }
  }

  fun changeSpacePin(
    spaceId: String,
    currentPin: String,
    newPin: String,
    onResult: ((Boolean, String?) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val result = spaceRepository.changeSpacePin(spaceId, currentPin, newPin)
      result.fold(
        onSuccess = {
          unlockSpace(spaceId)
          _userFeedback.tryEmit("PIN changed successfully.")
          onResult?.invoke(true, null)
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to change PIN."
          _userFeedback.tryEmit("Error: $msg")
          onResult?.invoke(false, msg)
        }
      )
    }
  }

  fun disableSpacePin(
    spaceId: String,
    currentPin: String,
    onResult: ((Boolean, String?) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val result = spaceRepository.disableSpacePin(spaceId, currentPin)
      result.fold(
        onSuccess = {
          lockSpace(spaceId)
          _userFeedback.tryEmit("PIN protection disabled.")
          onResult?.invoke(true, null)
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to disable PIN."
          _userFeedback.tryEmit("Error: $msg")
          onResult?.invoke(false, msg)
        }
      )
    }
  }

  fun setAppMembership(spaceId: String, app: DiscoveredApp, isIncluded: Boolean) {
    if (isIncluded) {
      addAppToSpace(spaceId, app)
    } else {
      removeAppFromSpace(spaceId, app)
    }
  }

  fun toggleAppMembership(spaceId: String, app: DiscoveredApp) {
    viewModelScope.launch {
      val isMember = spaceRepository.isAppInSpace(spaceId, app)
      if (isMember) {
        removeAppFromSpace(spaceId, app)
      } else {
        addAppToSpace(spaceId, app)
      }
    }
  }

  fun updateSpaceCustomization(
    spaceId: String,
    backgroundType: String,
    backgroundColor: Long?,
    backgroundImageUri: String?,
    gridColumns: Int,
    iconSize: String,
    labelVisibility: Boolean,
    onResult: ((Boolean, String?) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val result = spaceRepository.updateSpaceCustomization(
        spaceId = spaceId,
        backgroundType = backgroundType,
        backgroundColor = backgroundColor,
        backgroundImageUri = backgroundImageUri,
        gridColumns = gridColumns,
        iconSize = iconSize,
        labelVisibility = labelVisibility
      )
      result.fold(
        onSuccess = {
          _userFeedback.tryEmit("Space customization saved.")
          onResult?.invoke(true, null)
        },
        onFailure = { error ->
          val msg = error.message ?: "Failed to save customization."
          _userFeedback.tryEmit("Error: $msg")
          onResult?.invoke(false, msg)
        }
      )
    }
  }

  fun reorderSpaceApp(spaceId: String, app: DiscoveredApp, direction: Int) {
    viewModelScope.launch {
      val result = spaceRepository.reorderSpaceApp(spaceId, app, direction)
      result.fold(
        onSuccess = {},
        onFailure = { error ->
          _userFeedback.tryEmit("Error reordering: ${error.message}")
        }
      )
    }
  }

  fun sortSpaceAppsAlphabetically(spaceId: String, currentApps: List<DiscoveredApp>) {
    viewModelScope.launch {
      val sorted = currentApps.sortedBy { it.label.lowercase() }
      val result = spaceRepository.reorderSpaceApps(spaceId, sorted)
      result.fold(
        onSuccess = {
          _userFeedback.tryEmit("Apps sorted alphabetically.")
        },
        onFailure = { error ->
          _userFeedback.tryEmit("Error sorting apps: ${error.message}")
        }
      )
    }
  }

  // --- Layer 1 Placements & Pages ---

  fun loadLayer1Placements(spaceId: String) {
    viewModelScope.launch {
      spaceRepository.getPlacementsForSpaceLayer(spaceId, com.multispace.domain.model.SpaceItemPlacement.LAYER_HOME)
    }
  }

  fun removePlacement(placementId: String) {
    viewModelScope.launch {
      spaceRepository.removePlacement(placementId)
      _userFeedback.tryEmit("Item removed from Home.")
    }
  }

  fun moveAppToPage(spaceId: String, placementId: String, targetPage: Int, targetPosition: Int) {
    viewModelScope.launch {
      spaceRepository.moveAppToPage(spaceId, placementId, targetPage, targetPosition)
    }
  }

  fun addAppToHome(spaceId: String, app: DiscoveredApp, pageIndex: Int = 0) {
    viewModelScope.launch {
      val current = spaceRepository.getPlacementsForSpaceLayer(spaceId, com.multispace.domain.model.SpaceItemPlacement.LAYER_HOME)
      val pageItems = current.filter { it.pageIndex == pageIndex }
      val placement = com.multispace.domain.model.SpaceItemPlacement(
        id = "place_" + java.util.UUID.randomUUID().toString().replace("-", "").take(10),
        spaceId = spaceId,
        layer = com.multispace.domain.model.SpaceItemPlacement.LAYER_HOME,
        pageIndex = pageIndex,
        positionIndex = pageItems.size,
        itemType = com.multispace.domain.model.SpaceItemPlacement.ITEM_TYPE_APP,
        packageName = app.packageName,
        componentName = app.activityName,
        userHandleId = app.userHandleId
      )
      spaceRepository.addPlacement(placement)
      _userFeedback.tryEmit("Added '${app.label}' to Home page ${pageIndex + 1}.")
    }
  }

  // --- Folder Management ---

  fun createFolderFromApps(
    spaceId: String,
    pageIndex: Int,
    positionIndex: Int,
    folderName: String,
    sourceApp: DiscoveredApp,
    targetApp: DiscoveredApp,
    sourcePlacementId: String?,
    targetPlacementId: String?
  ) {
    viewModelScope.launch {
      val result = spaceRepository.createFolderFromApps(
        spaceId = spaceId,
        pageIndex = pageIndex,
        positionIndex = positionIndex,
        folderName = folderName,
        sourceApp = sourceApp,
        targetApp = targetApp,
        sourcePlacementId = sourcePlacementId,
        targetPlacementId = targetPlacementId
      )
      result.fold(
        onSuccess = { folder ->
          _userFeedback.tryEmit("Created folder '${folder.name}'.")
        },
        onFailure = { error ->
          _userFeedback.tryEmit("Error creating folder: ${error.message}")
        }
      )
    }
  }

  fun renameFolder(folderId: String, newName: String) {
    viewModelScope.launch {
      spaceRepository.renameFolder(folderId, newName)
      _userFeedback.tryEmit("Folder renamed.")
    }
  }

  fun addAppToFolder(folderId: String, app: DiscoveredApp) {
    viewModelScope.launch {
      spaceRepository.addAppToFolder(folderId, app)
      _userFeedback.tryEmit("Added '${app.label}' to folder.")
    }
  }

  fun removeAppFromFolder(folderId: String, folderItemId: String) {
    viewModelScope.launch {
      spaceRepository.removeAppFromFolder(folderId, folderItemId)
      _userFeedback.tryEmit("Item removed from folder.")
    }
  }

  fun deleteFolder(folderId: String) {
    viewModelScope.launch {
      spaceRepository.deleteFolder(folderId)
      _userFeedback.tryEmit("Folder deleted.")
    }
  }

  // --- Dock Management ---

  fun addAppToDock(spaceId: String, app: DiscoveredApp) {
    viewModelScope.launch {
      val result = spaceRepository.addAppToDock(spaceId, app)
      result.fold(
        onSuccess = {
          _userFeedback.tryEmit("Added '${app.label}' to Dock.")
        },
        onFailure = { error ->
          _userFeedback.tryEmit("Failed to add to Dock: ${error.message}")
        }
      )
    }
  }

  fun removeAppFromDock(spaceId: String, dockItemId: String) {
    viewModelScope.launch {
      spaceRepository.removeAppFromDock(spaceId, dockItemId)
      _userFeedback.tryEmit("App removed from Dock.")
    }
  }

  fun reorderDockItems(spaceId: String, items: List<com.multispace.domain.model.SpaceDockItem>) {
    viewModelScope.launch {
      spaceRepository.reorderDockItems(spaceId, items)
    }
  }

  // --- Layout Configuration & Presets ---

  fun updateSpaceLayoutSettings(
    spaceId: String,
    layer1DisplayMode: String,
    layer2DisplayMode: String,
    layer2AccessMode: String,
    dockCapacity: Int,
    gridColumns: Int,
    onSuccess: (() -> Unit)? = null
  ) {
    viewModelScope.launch {
      val result = spaceRepository.updateSpaceLayoutSettings(
        spaceId = spaceId,
        layer1DisplayMode = layer1DisplayMode,
        layer2DisplayMode = layer2DisplayMode,
        layer2AccessMode = layer2AccessMode,
        dockCapacity = dockCapacity,
        gridColumns = gridColumns
      )
      result.fold(
        onSuccess = {
          _userFeedback.tryEmit("Layout settings saved.")
          onSuccess?.invoke()
        },
        onFailure = { error ->
          _userFeedback.tryEmit("Error saving layout settings: ${error.message}")
        }
      )
    }
  }

  fun applyLayoutPreset(
    spaceId: String,
    preset: com.multispace.domain.model.LayoutPreset,
    apps: List<DiscoveredApp>,
    onComplete: (() -> Unit)? = null
  ) {
    viewModelScope.launch {
      val result = spaceRepository.applyLayoutPreset(spaceId, preset, apps)
      result.fold(
        onSuccess = {
          _userFeedback.tryEmit("Applied layout preset '${preset.name}'.")
          onComplete?.invoke()
        },
        onFailure = { error ->
          _userFeedback.tryEmit("Error applying preset: ${error.message}")
        }
      )
    }
  }

  fun importCurrentHomeLayout(
    spaceId: String,
    allInstalledApps: List<DiscoveredApp>,
    onReport: ((com.multispace.domain.model.ImportReport) -> Unit)? = null
  ) {
    viewModelScope.launch {
      val result = spaceRepository.importCurrentHomeLayout(spaceId, allInstalledApps)
      result.fold(
        onSuccess = { report ->
          _userFeedback.tryEmit("Layout import finished: ${report.summary}")
          onReport?.invoke(report)
        },
        onFailure = { error ->
          _userFeedback.tryEmit("Layout import failed: ${error.message}")
        }
      )
    }
  }
}
