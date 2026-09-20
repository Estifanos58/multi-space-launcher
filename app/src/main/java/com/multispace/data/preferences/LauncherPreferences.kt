package com.multispace.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.multispace.domain.model.Space
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_preferences")

class LauncherPreferences(private val context: Context) {

  companion object {
    private val KEY_ACTIVE_SPACE_ID = stringPreferencesKey("active_space_id")
    private val KEY_INITIALIZED_SPACE_IDS = stringSetPreferencesKey("initialized_space_ids")

    @Volatile
    private var INSTANCE: LauncherPreferences? = null

    fun getInstance(context: Context): LauncherPreferences {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: LauncherPreferences(context.applicationContext).also { INSTANCE = it }
      }
    }
  }

  val activeSpaceIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
    preferences[KEY_ACTIVE_SPACE_ID] ?: Space.DEFAULT_SPACE_ID
  }

  val initializedSpaceIdsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
    preferences[KEY_INITIALIZED_SPACE_IDS] ?: emptySet()
  }

  suspend fun isSpaceInitialized(spaceId: String): Boolean {
    return context.dataStore.data.map { preferences ->
      preferences[KEY_INITIALIZED_SPACE_IDS]?.contains(spaceId) ?: false
    }.first()
  }

  suspend fun markSpaceInitialized(spaceId: String) {
    context.dataStore.edit { preferences ->
      val current = preferences[KEY_INITIALIZED_SPACE_IDS] ?: emptySet()
      preferences[KEY_INITIALIZED_SPACE_IDS] = current + spaceId
    }
  }

  suspend fun setActiveSpaceId(spaceId: String) {
    context.dataStore.edit { preferences ->
      preferences[KEY_ACTIVE_SPACE_ID] = spaceId
    }
  }

  suspend fun clearActiveSpaceId() {
    context.dataStore.edit { preferences ->
      preferences.remove(KEY_ACTIVE_SPACE_ID)
    }
  }
}
