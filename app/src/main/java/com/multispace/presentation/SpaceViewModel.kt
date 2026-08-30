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
    preferences = preferences
  )

  private val _userFeedback = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val userFeedback: SharedFlow<String> = _userFeedback.asSharedFlow()

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

  init {
    AppLogger.i(AppLogger.Category.LAUNCHER, "SpaceViewModel initialized: ensuring default Space state")
    ensureDefaultSpaceInitialized()
  }

  fun ensureDefaultSpaceInitialized() {
    viewModelScope.launch {
      spaceRepository.ensureDefaultSpaceInitialized()
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
}
