package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.Space
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_preferences")

class LauncherPreferences(private val context: Context) {

  companion object {
    private val KEY_ACTIVE_SPACE_ID = stringPreferencesKey("active_space_id")
  }

  val activeSpaceIdFlow: Flow<String?> = context.dataStore.data.map { preferences ->
    preferences[KEY_ACTIVE_SPACE_ID] ?: Space.DEFAULT_SPACE_ID
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
