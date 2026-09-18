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
import com.multispace.diagnostics.AppLogger

@Database(
  entities = [
    SpaceEntity::class,
    SpaceMembershipEntity::class,
    SpaceItemPlacementEntity::class,
    SpaceFolderEntity::class,
    SpaceFolderItemEntity::class,
    SpaceDockItemEntity::class
  ],
  version = 11,
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

    private fun hasColumn(db: SupportSQLiteDatabase, tableName: String, columnName: String): Boolean {
      val cursor = db.query("PRAGMA table_info(`$tableName`)")
      cursor.use {
        val nameIndex = it.getColumnIndex("name")
        while (it.moveToNext()) {
          if (nameIndex != -1 && it.getString(nameIndex).equals(columnName, ignoreCase = true)) {
            return true
          }
        }
      }
      return false
    }

    private fun addColumnIfNotExists(
      db: SupportSQLiteDatabase,
      tableName: String,
      columnName: String,
      columnDefinition: String
    ) {
      if (!hasColumn(db, tableName, columnName)) {
        db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
      }
    }

    private fun recreateSpacesTable(db: SupportSQLiteDatabase) {
      db.execSQL("PRAGMA foreign_keys = OFF")
      db.execSQL("""
        CREATE TABLE IF NOT EXISTS `spaces_migration_temp` (
          `id` TEXT NOT NULL,
          `name` TEXT NOT NULL,
          `order_index` INTEGER NOT NULL,
          `created_at` INTEGER NOT NULL,
          `updated_at` INTEGER NOT NULL,
          `auth_policy` TEXT NOT NULL,
          `pin_salt` TEXT,
          `pin_hash` TEXT,
          `layout_type` TEXT NOT NULL,
          `pattern_rows` INTEGER NOT NULL,
          `pattern_cols` INTEGER NOT NULL,
          `background_type` TEXT NOT NULL,
          `background_color` INTEGER,
          `background_image_uri` TEXT,
          `home_wallpaper_type` TEXT NOT NULL,
          `home_wallpaper_color` INTEGER,
          `home_wallpaper_image_uri` TEXT,
          `phone_lock_wallpaper_type` TEXT NOT NULL,
          `phone_lock_wallpaper_color` INTEGER,
          `phone_lock_wallpaper_image_uri` TEXT,
          `space_lock_wallpaper_type` TEXT NOT NULL,
          `space_lock_wallpaper_color` INTEGER,
          `space_lock_wallpaper_image_uri` TEXT,
          `app_theme` TEXT NOT NULL,
          `grid_columns` INTEGER NOT NULL,
          `icon_size` TEXT NOT NULL,
          `label_visibility` INTEGER NOT NULL,
          `layer1_display_mode` TEXT NOT NULL,
          `layer2_display_mode` TEXT NOT NULL,
          `layer2_access_mode` TEXT NOT NULL,
          `dock_capacity` INTEGER NOT NULL,
          `layout_preset` TEXT NOT NULL,
          `use_layer2` INTEGER NOT NULL,
          `home_wallpaper_scale_mode` TEXT NOT NULL,
          `home_wallpaper_zoom_level` REAL NOT NULL,
          `home_wallpaper_dim_level` REAL NOT NULL,
          `home_wallpaper_offset_x` REAL NOT NULL,
          `home_wallpaper_offset_y` REAL NOT NULL,
          `phone_lock_wallpaper_scale_mode` TEXT NOT NULL,
          `phone_lock_wallpaper_zoom_level` REAL NOT NULL,
          `phone_lock_wallpaper_dim_level` REAL NOT NULL,
          `phone_lock_wallpaper_offset_x` REAL NOT NULL,
          `phone_lock_wallpaper_offset_y` REAL NOT NULL,
          `space_lock_wallpaper_scale_mode` TEXT NOT NULL,
          `space_lock_wallpaper_zoom_level` REAL NOT NULL,
          `space_lock_wallpaper_dim_level` REAL NOT NULL,
          `space_lock_wallpaper_offset_x` REAL NOT NULL,
          `space_lock_wallpaper_offset_y` REAL NOT NULL,
          `page_turn_effect` TEXT NOT NULL,
          `page_turn_duration_ms` INTEGER NOT NULL,
          `page_turn_intensity` REAL NOT NULL,
          `page_count` INTEGER NOT NULL,
          PRIMARY KEY(`id`)
        )
      """.trimIndent())

      val durationSelect = when {
        hasColumn(db, "spaces", "page_turn_duration_ms") -> "`page_turn_duration_ms`"
        hasColumn(db, "spaces", "page_turn_duration") -> "`page_turn_duration`"
        else -> "300"
      }
      val pageCountSelect = if (hasColumn(db, "spaces", "page_count")) "`page_count`" else "1"

      db.execSQL("""
        INSERT INTO `spaces_migration_temp` (
          `id`, `name`, `order_index`, `created_at`, `updated_at`, `auth_policy`, `pin_salt`, `pin_hash`,
          `layout_type`, `pattern_rows`, `pattern_cols`, `background_type`, `background_color`,
          `background_image_uri`, `home_wallpaper_type`, `home_wallpaper_color`, `home_wallpaper_image_uri`,
          `phone_lock_wallpaper_type`, `phone_lock_wallpaper_color`, `phone_lock_wallpaper_image_uri`,
          `space_lock_wallpaper_type`, `space_lock_wallpaper_color`, `space_lock_wallpaper_image_uri`,
          `app_theme`, `grid_columns`, `icon_size`, `label_visibility`, `layer1_display_mode`,
          `layer2_display_mode`, `layer2_access_mode`, `dock_capacity`, `layout_preset`, `use_layer2`,
          `home_wallpaper_scale_mode`, `home_wallpaper_zoom_level`, `home_wallpaper_dim_level`,
          `home_wallpaper_offset_x`, `home_wallpaper_offset_y`, `phone_lock_wallpaper_scale_mode`,
          `phone_lock_wallpaper_zoom_level`, `phone_lock_wallpaper_dim_level`,
          `phone_lock_wallpaper_offset_x`, `phone_lock_wallpaper_offset_y`,
          `space_lock_wallpaper_scale_mode`, `space_lock_wallpaper_zoom_level`,
          `space_lock_wallpaper_dim_level`, `space_lock_wallpaper_offset_x`,
          `space_lock_wallpaper_offset_y`, `page_turn_effect`, `page_turn_duration_ms`,
          `page_turn_intensity`, `page_count`
        )
        SELECT
          `id`, `name`, `order_index`, `created_at`, `updated_at`, `auth_policy`, `pin_salt`, `pin_hash`,
          `layout_type`, `pattern_rows`, `pattern_cols`, `background_type`, `background_color`,
          `background_image_uri`, `home_wallpaper_type`, `home_wallpaper_color`, `home_wallpaper_image_uri`,
          `phone_lock_wallpaper_type`, `phone_lock_wallpaper_color`, `phone_lock_wallpaper_image_uri`,
          `space_lock_wallpaper_type`, `space_lock_wallpaper_color`, `space_lock_wallpaper_image_uri`,
          `app_theme`, `grid_columns`, `icon_size`, `label_visibility`, `layer1_display_mode`,
          `layer2_display_mode`, `layer2_access_mode`, `dock_capacity`, `layout_preset`, `use_layer2`,
          `home_wallpaper_scale_mode`, `home_wallpaper_zoom_level`, `home_wallpaper_dim_level`,
          `home_wallpaper_offset_x`, `home_wallpaper_offset_y`, `phone_lock_wallpaper_scale_mode`,
          `phone_lock_wallpaper_zoom_level`, `phone_lock_wallpaper_dim_level`,
          `phone_lock_wallpaper_offset_x`, `phone_lock_wallpaper_offset_y`,
          `space_lock_wallpaper_scale_mode`, `space_lock_wallpaper_zoom_level`,
          `space_lock_wallpaper_dim_level`, `space_lock_wallpaper_offset_x`,
          `space_lock_wallpaper_offset_y`, `page_turn_effect`, $durationSelect,
          `page_turn_intensity`, $pageCountSelect
        FROM `spaces`
      """.trimIndent())

      db.execSQL("DROP TABLE `spaces`")
      db.execSQL("ALTER TABLE `spaces_migration_temp` RENAME TO `spaces`")
      db.execSQL("PRAGMA foreign_keys = ON")
    }

    private fun migrateToVersion8Or9(db: SupportSQLiteDatabase) {
      addColumnIfNotExists(db, "space_item_placements", "span_x", "INTEGER NOT NULL DEFAULT 1")
      addColumnIfNotExists(db, "space_item_placements", "span_y", "INTEGER NOT NULL DEFAULT 1")
      addColumnIfNotExists(db, "space_item_placements", "app_widget_id", "INTEGER NOT NULL DEFAULT -1")
      addColumnIfNotExists(db, "space_item_placements", "custom_widget_type", "TEXT DEFAULT NULL")

      addColumnIfNotExists(db, "spaces", "page_count", "INTEGER NOT NULL DEFAULT 1")
      addColumnIfNotExists(db, "spaces", "page_turn_effect", "TEXT NOT NULL DEFAULT 'NORMAL'")
      addColumnIfNotExists(db, "spaces", "page_turn_intensity", "REAL NOT NULL DEFAULT 1.0")

      val hasOldDuration = hasColumn(db, "spaces", "page_turn_duration")
      val hasNewDuration = hasColumn(db, "spaces", "page_turn_duration_ms")

      if (hasOldDuration && !hasNewDuration) {
        try {
          db.execSQL("ALTER TABLE `spaces` RENAME COLUMN `page_turn_duration` TO `page_turn_duration_ms`")
        } catch (_: Exception) {
          recreateSpacesTable(db)
        }
      } else if (hasOldDuration && hasNewDuration) {
        try {
          db.execSQL("ALTER TABLE `spaces` DROP COLUMN `page_turn_duration`")
        } catch (_: Exception) {
          recreateSpacesTable(db)
        }
      } else if (!hasNewDuration) {
        db.execSQL("ALTER TABLE `spaces` ADD COLUMN `page_turn_duration_ms` INTEGER NOT NULL DEFAULT 300")
      }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        addColumnIfNotExists(db, "spaces", "page_turn_effect", "TEXT NOT NULL DEFAULT 'NORMAL'")
        addColumnIfNotExists(db, "spaces", "page_turn_duration_ms", "INTEGER NOT NULL DEFAULT 300")
        addColumnIfNotExists(db, "spaces", "page_turn_intensity", "REAL NOT NULL DEFAULT 1.0")
      }
    }

    internal val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
        migrateToVersion8Or9(db)
      }
    }

    internal val MIGRATION_8_9 = object : Migration(8, 9) {
      override fun migrate(db: SupportSQLiteDatabase) {
        migrateToVersion8Or9(db)
      }
    }

    internal val MIGRATION_7_9 = object : Migration(7, 9) {
      override fun migrate(db: SupportSQLiteDatabase) {
        migrateToVersion8Or9(db)
      }
    }

    internal val MIGRATION_9_10 = object : Migration(9, 10) {
      override fun migrate(db: SupportSQLiteDatabase) {
        try {
          db.execSQL("""
            DELETE FROM space_item_placements 
            WHERE item_type = 'APP' 
              AND package_name IS NOT NULL 
              AND id NOT IN (
                SELECT MIN(id) 
                FROM space_item_placements 
                WHERE item_type = 'APP' AND package_name IS NOT NULL 
                GROUP BY space_id, layer, package_name, COALESCE(component_name, ''), user_handle_id
              )
          """.trimIndent())
        } catch (_: Exception) {
          // Fallback if table schema or temporary state conflicts
        }
      }
    }

    internal val MIGRATION_10_11 = object : Migration(10, 11) {
      override fun migrate(db: SupportSQLiteDatabase) {
        try {
          // 1. Deduplicate dock items by full canonical identity before creating unique index
          db.execSQL("""
            DELETE FROM space_dock_items 
            WHERE id NOT IN (
              SELECT MIN(id) 
              FROM space_dock_items 
              GROUP BY space_id, package_name, component_name, user_handle_id
            )
          """.trimIndent())
          db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS `index_space_dock_items_space_id_package_name_component_name_user_handle_id` 
            ON `space_dock_items` (`space_id`, `package_name`, `component_name`, `user_handle_id`)
          """.trimIndent())

          // 2. Deduplicate folder items by full canonical identity before creating unique index
          db.execSQL("""
            DELETE FROM space_folder_items 
            WHERE id NOT IN (
              SELECT MIN(id) 
              FROM space_folder_items 
              GROUP BY folder_id, package_name, component_name, user_handle_id
            )
          """.trimIndent())
          db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS `index_space_folder_items_folder_id_package_name_component_name_user_handle_id` 
            ON `space_folder_items` (`folder_id`, `package_name`, `component_name`, `user_handle_id`)
          """.trimIndent())

          // 3. Deduplicate space item placements by full canonical identity
          db.execSQL("""
            DELETE FROM space_item_placements 
            WHERE item_type = 'APP' 
              AND package_name IS NOT NULL 
              AND id NOT IN (
                SELECT MIN(id) 
                FROM space_item_placements 
                WHERE item_type = 'APP' AND package_name IS NOT NULL 
                GROUP BY space_id, layer, package_name, COALESCE(component_name, ''), user_handle_id
              )
          """.trimIndent())
          db.execSQL("""
            CREATE INDEX IF NOT EXISTS `index_space_item_placements_space_id_layer_package_name_component_name_user_handle_id` 
            ON `space_item_placements` (`space_id`, `layer`, `package_name`, `component_name`, `user_handle_id`)
          """.trimIndent())

          // 4. Resolve position collisions between distinct placements instead of deleting them
          val collidingCursor = db.query("""
            SELECT p1.id, p1.space_id, p1.layer, p1.page_index 
            FROM space_item_placements p1 
            INNER JOIN space_item_placements p2 
              ON p1.space_id = p2.space_id 
             AND p1.layer = p2.layer 
             AND p1.page_index = p2.page_index 
             AND p1.position_index = p2.position_index 
             AND p1.id > p2.id
          """.trimIndent())
          collidingCursor.use { cursor ->
            while (cursor.moveToNext()) {
              val id = cursor.getString(0)
              val spaceId = cursor.getString(1)
              val layer = cursor.getInt(2)
              val pageIndex = cursor.getInt(3)
              val maxCursor = db.query(
                "SELECT COALESCE(MAX(position_index), 0) + 1 FROM space_item_placements WHERE space_id = ? AND layer = ? AND page_index = ?",
                arrayOf(spaceId, layer, pageIndex)
              )
              val newPos = maxCursor.use { mc ->
                if (mc.moveToNext()) mc.getInt(0) else 0
              }
              db.execSQL("UPDATE space_item_placements SET position_index = ? WHERE id = ?", arrayOf(newPos, id))
            }
          }
        } catch (e: Exception) {
          AppLogger.e(AppLogger.Category.LAUNCHER, "Migration 10->11 failed", e)
        }
      }
    }

    fun getInstance(context: Context): LauncherDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          LauncherDatabase::class.java,
          "multispace_launcher.db"
        )
        .addMigrations(
          MIGRATION_1_2,
          MIGRATION_2_3,
          MIGRATION_3_4,
          MIGRATION_4_5,
          MIGRATION_5_6,
          MIGRATION_6_7,
          MIGRATION_7_8,
          MIGRATION_8_9,
          MIGRATION_7_9,
          MIGRATION_9_10,
          MIGRATION_10_11
        )
        .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
