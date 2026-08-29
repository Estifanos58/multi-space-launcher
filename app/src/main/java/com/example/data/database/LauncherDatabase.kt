package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.SpaceDao
import com.example.data.dao.SpaceMembershipDao
import com.example.data.entity.SpaceEntity
import com.example.data.entity.SpaceMembershipEntity

@Database(
  entities = [
    SpaceEntity::class,
    SpaceMembershipEntity::class
  ],
  version = 1,
  exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
  abstract fun spaceDao(): SpaceDao
  abstract fun spaceMembershipDao(): SpaceMembershipDao

  companion object {
    @Volatile
    private var INSTANCE: LauncherDatabase? = null

    fun getInstance(context: Context): LauncherDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          LauncherDatabase::class.java,
          "multispace_launcher.db"
        )
        .fallbackToDestructiveMigration() // Initial Version 1
        .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
