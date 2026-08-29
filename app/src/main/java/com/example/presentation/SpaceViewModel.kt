package com.example.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.LauncherDatabase
import com.example.data.preferences.LauncherPreferences
import com.example.data.repository.RoomSpaceRepository
import com.example.diagnostics.AppLogger
import com.example.domain.model.DiscoveredApp
import com.example.domain.model.Space
import com.example.domain.model.SpaceMembership
import com.example.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SpaceUiState(
  val spaces: List<Space> = emptyList(),
  val activeSpace: Space? = null,
  val activeSpaceMemberships: List<SpaceMembership> = emptyList(),
  val isLoading: Boolean = false,
  val errorMessage: String? = null
)

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
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val activeSpace: StateFlow<Space?> = spaceRepository.activeSpaceFlow
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = null
    )

  private val _activeMemberships = MutableStateFlow<List<SpaceMembership>>(emptyList())
  val activeMemberships: StateFlow<List<SpaceMembership>> = _activeMemberships.asStateFlow()

  init {
    AppLogger.i(AppLogger.Category.LAUNCHER, "SpaceViewModel initialized: ensuring default Space state")
    viewModelScope.launch {
      spaceRepository.ensureDefaultSpaceInitialized()
    }
    observeActiveSpaceMemberships()
  }

  private fun observeActiveSpaceMemberships() {
    viewModelScope.launch {
      spaceRepository.activeSpaceFlow.collect { space ->
        if (space != null) {
          spaceRepository.getMembershipsForSpaceFlow(space.id).collect { memberships ->
            _activeMemberships.value = memberships
          }
        } else {
          _activeMemberships.value = emptyList()
        }
      }
    }
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

  fun toggleAppMembership(spaceId: String, app: DiscoveredApp) {
    viewModelScope.launch {
      val isMember = spaceRepository.isAppInSpace(spaceId, app)
      val result = if (isMember) {
        spaceRepository.removeAppFromSpace(spaceId, app)
      } else {
        spaceRepository.addAppToSpace(spaceId, app)
      }
      result.fold(
        onSuccess = {
          val action = if (isMember) "removed from" else "added to"
          _userFeedback.tryEmit("${app.label} $action Space.")
        },
        onFailure = { error ->
          _userFeedback.tryEmit("Error updating membership: ${error.message}")
        }
      )
    }
  }
}
