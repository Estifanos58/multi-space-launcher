package com.multispace.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.multispace.data.dao.LaunchHistoryDao
import com.multispace.data.entity.LaunchHistoryEntity

/**
 * Dedicated Room database storing launcher-owned launch history.
 *
 * Keeps history decoupled and isolated from Space layout semantics,
 * ensuring no modifications or migrations are required on LauncherDatabase.
 */
@Database(
  entities = [LaunchHistoryEntity::class],
  version = 1,
  exportSchema = false
)
abstract class LaunchHistoryDatabase : RoomDatabase() {
  abstract fun launchHistoryDao(): LaunchHistoryDao

  companion object {
    @Volatile
    private var INSTANCE: LaunchHistoryDatabase? = null

    fun getInstance(context: Context): LaunchHistoryDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          LaunchHistoryDatabase::class.java,
          "multispace_launch_history.db"
        )
        .fallbackToDestructiveMigration(true)
        .build()
        .also { INSTANCE = it }
      }
    }
  }
}
