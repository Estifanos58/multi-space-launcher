package com.multispace.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.multispace.data.dao.SpaceDao
import com.multispace.data.dao.SpaceMembershipDao
import com.multispace.data.entity.SpaceEntity
import com.multispace.data.entity.SpaceMembershipEntity

@Database(
  entities = [
    SpaceEntity::class,
    SpaceMembershipEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
  abstract fun spaceDao(): SpaceDao
  abstract fun spaceMembershipDao(): SpaceMembershipDao

  companion object {
    @Volatile
    private var INSTANCE: LauncherDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE spaces ADD COLUMN background_type TEXT NOT NULL DEFAULT 'DEFAULT'")
        db.execSQL("ALTER TABLE spaces ADD COLUMN background_color INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE spaces ADD COLUMN background_image_uri TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE spaces ADD COLUMN grid_columns INTEGER NOT NULL DEFAULT 4")
        db.execSQL("ALTER TABLE spaces ADD COLUMN icon_size TEXT NOT NULL DEFAULT 'MEDIUM'")
        db.execSQL("ALTER TABLE spaces ADD COLUMN label_visibility INTEGER NOT NULL DEFAULT 1")
      }
    }

    fun getInstance(context: Context): LauncherDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          LauncherDatabase::class.java,
          "multispace_launcher.db"
        )
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration()
        .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
