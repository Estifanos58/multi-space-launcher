package com.multispace.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.multispace.data.dao.SpaceDao
import com.multispace.data.dao.SpaceLayoutDao
import com.multispace.data.dao.SpaceMembershipDao
import com.multispace.data.entity.SpaceDockItemEntity
import com.multispace.data.entity.SpaceEntity
import com.multispace.data.entity.SpaceFolderEntity
import com.multispace.data.entity.SpaceFolderItemEntity
import com.multispace.data.entity.SpaceItemPlacementEntity
import com.multispace.data.entity.SpaceMembershipEntity

@Database(
  entities = [
    SpaceEntity::class,
    SpaceMembershipEntity::class,
    SpaceItemPlacementEntity::class,
    SpaceFolderEntity::class,
    SpaceFolderItemEntity::class,
    SpaceDockItemEntity::class
  ],
  version = 6,
  exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
  abstract fun spaceDao(): SpaceDao
  abstract fun spaceMembershipDao(): SpaceMembershipDao
  abstract fun spaceLayoutDao(): SpaceLayoutDao

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

    private val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE spaces ADD COLUMN pattern_rows INTEGER NOT NULL DEFAULT 3")
        db.execSQL("ALTER TABLE spaces ADD COLUMN pattern_cols INTEGER NOT NULL DEFAULT 3")
        db.execSQL("ALTER TABLE spaces ADD COLUMN home_wallpaper_type TEXT NOT NULL DEFAULT 'DEFAULT'")
        db.execSQL("ALTER TABLE spaces ADD COLUMN home_wallpaper_color INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE spaces ADD COLUMN home_wallpaper_image_uri TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE spaces ADD COLUMN phone_lock_wallpaper_type TEXT NOT NULL DEFAULT 'DEFAULT'")
        db.execSQL("ALTER TABLE spaces ADD COLUMN phone_lock_wallpaper_color INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE spaces ADD COLUMN phone_lock_wallpaper_image_uri TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE spaces ADD COLUMN space_lock_wallpaper_type TEXT NOT NULL DEFAULT 'DEFAULT'")
        db.execSQL("ALTER TABLE spaces ADD COLUMN space_lock_wallpaper_color INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE spaces ADD COLUMN space_lock_wallpaper_image_uri TEXT DEFAULT NULL")
        db.execSQL("ALTER TABLE spaces ADD COLUMN app_theme TEXT NOT NULL DEFAULT 'DEFAULT'")
      }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE spaces ADD COLUMN layer1_display_mode TEXT NOT NULL DEFAULT 'PAGE'")
        db.execSQL("ALTER TABLE spaces ADD COLUMN layer2_display_mode TEXT NOT NULL DEFAULT 'SCROLL'")
        db.execSQL("ALTER TABLE spaces ADD COLUMN layer2_access_mode TEXT NOT NULL DEFAULT 'DOCK_BUTTON'")
        db.execSQL("ALTER TABLE spaces ADD COLUMN dock_capacity INTEGER NOT NULL DEFAULT 5")
        db.execSQL("ALTER TABLE spaces ADD COLUMN layout_preset TEXT NOT NULL DEFAULT 'DEFAULT'")

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS space_item_placements (
            id TEXT PRIMARY KEY NOT NULL,
            space_id TEXT NOT NULL,
            layer INTEGER NOT NULL DEFAULT 1,
            page_index INTEGER NOT NULL DEFAULT 0,
            position_index INTEGER NOT NULL DEFAULT 0,
            item_type TEXT NOT NULL DEFAULT 'APP',
            package_name TEXT,
            component_name TEXT,
            user_handle_id INTEGER NOT NULL DEFAULT 0,
            folder_id TEXT,
            FOREIGN KEY(space_id) REFERENCES spaces(id) ON DELETE CASCADE
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_space_item_placements_space_id ON space_item_placements(space_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_space_item_placements_space_id_layer_page_index ON space_item_placements(space_id, layer, page_index)")

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS space_folders (
            id TEXT PRIMARY KEY NOT NULL,
            space_id TEXT NOT NULL,
            name TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            updated_at INTEGER NOT NULL,
            FOREIGN KEY(space_id) REFERENCES spaces(id) ON DELETE CASCADE
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_space_folders_space_id ON space_folders(space_id)")

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS space_folder_items (
            id TEXT PRIMARY KEY NOT NULL,
            folder_id TEXT NOT NULL,
            package_name TEXT NOT NULL,
            component_name TEXT NOT NULL,
            user_handle_id INTEGER NOT NULL DEFAULT 0,
            order_index INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(folder_id) REFERENCES space_folders(id) ON DELETE CASCADE
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_space_folder_items_folder_id ON space_folder_items(folder_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_space_folder_items_folder_id_order_index ON space_folder_items(folder_id, order_index)")

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS space_dock_items (
            id TEXT PRIMARY KEY NOT NULL,
            space_id TEXT NOT NULL,
            order_index INTEGER NOT NULL DEFAULT 0,
            package_name TEXT NOT NULL,
            component_name TEXT NOT NULL,
            user_handle_id INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(space_id) REFERENCES spaces(id) ON DELETE CASCADE
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_space_dock_items_space_id ON space_dock_items(space_id)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_space_dock_items_space_id_order_index ON space_dock_items(space_id, order_index)")
      }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE spaces ADD COLUMN use_layer2 INTEGER NOT NULL DEFAULT 1")
      }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE spaces ADD COLUMN home_wallpaper_scale_mode TEXT NOT NULL DEFAULT 'crop'")
        db.execSQL("ALTER TABLE spaces ADD COLUMN home_wallpaper_zoom_level REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE spaces ADD COLUMN home_wallpaper_dim_level REAL NOT NULL DEFAULT 0.20")
        db.execSQL("ALTER TABLE spaces ADD COLUMN home_wallpaper_offset_x REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE spaces ADD COLUMN home_wallpaper_offset_y REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE spaces ADD COLUMN phone_lock_wallpaper_scale_mode TEXT NOT NULL DEFAULT 'crop'")
        db.execSQL("ALTER TABLE spaces ADD COLUMN phone_lock_wallpaper_zoom_level REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE spaces ADD COLUMN phone_lock_wallpaper_dim_level REAL NOT NULL DEFAULT 0.20")
        db.execSQL("ALTER TABLE spaces ADD COLUMN phone_lock_wallpaper_offset_x REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE spaces ADD COLUMN phone_lock_wallpaper_offset_y REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE spaces ADD COLUMN space_lock_wallpaper_scale_mode TEXT NOT NULL DEFAULT 'crop'")
        db.execSQL("ALTER TABLE spaces ADD COLUMN space_lock_wallpaper_zoom_level REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE spaces ADD COLUMN space_lock_wallpaper_dim_level REAL NOT NULL DEFAULT 0.20")
        db.execSQL("ALTER TABLE spaces ADD COLUMN space_lock_wallpaper_offset_x REAL NOT NULL DEFAULT 0.0")
        db.execSQL("ALTER TABLE spaces ADD COLUMN space_lock_wallpaper_offset_y REAL NOT NULL DEFAULT 0.0")
      }
    }

    fun getInstance(context: Context): LauncherDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          LauncherDatabase::class.java,
          "multispace_launcher.db"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .fallbackToDestructiveMigration()
        .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
