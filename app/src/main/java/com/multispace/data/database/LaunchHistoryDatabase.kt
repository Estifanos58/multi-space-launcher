package com.multispace.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.multispace.data.dao.LaunchHistoryDao
import com.multispace.data.entity.LaunchEventEntity
import com.multispace.domain.model.Space

/**
 * Dedicated Room database storing Space-specific application launch events and analytics.
 *
 * Fully decoupled from Space desktop layout tables.
 * Employs proper schema migrations to transition from legacy un-scoped launch history (v1)
 * to space-isolated launch events (v2).
 */
@Database(
  entities = [LaunchEventEntity::class],
  version = 2,
  exportSchema = true
)
abstract class LaunchHistoryDatabase : RoomDatabase() {
  abstract fun launchHistoryDao(): LaunchHistoryDao

  companion object {
    @Volatile
    private var INSTANCE: LaunchHistoryDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create the new launch_events table with space_id
        db.execSQL("""
          CREATE TABLE IF NOT EXISTS `launch_events` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `space_id` TEXT NOT NULL,
            `package_name` TEXT NOT NULL,
            `component_name` TEXT NOT NULL,
            `user_handle_id` INTEGER NOT NULL,
            `timestamp` INTEGER NOT NULL
          )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_launch_events_space_id_package_name_component_name_user_handle_id` ON `launch_events` (`space_id`, `package_name`, `component_name`, `user_handle_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_launch_events_space_id_timestamp` ON `launch_events` (`space_id`, `timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_launch_events_timestamp` ON `launch_events` (`timestamp`)")

        // 2. Safely migrate legacy global launch history from v1:
        // Legacy records in launch_history had no space context. To preserve existing data safely
        // without falsely polluting newly created custom spaces, old history is migrated to the
        // system default space (Space.DEFAULT_SPACE_ID = "space_default").
        val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='launch_history'")
        val tableExists = cursor.moveToFirst()
        cursor.close()

        if (tableExists) {
          db.execSQL("""
            INSERT INTO `launch_events` (`space_id`, `package_name`, `component_name`, `user_handle_id`, `timestamp`)
            SELECT '${Space.DEFAULT_SPACE_ID}', `package_name`, `component_name`, `user_handle_id`, `last_launched_at`
            FROM `launch_history`
          """.trimIndent())
          db.execSQL("DROP TABLE IF EXISTS `launch_history`")
        }
      }
    }

    fun getInstance(context: Context): LaunchHistoryDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          LaunchHistoryDatabase::class.java,
          "multispace_launch_history.db"
        )
        .addMigrations(MIGRATION_1_2)
        .build()
        .also { INSTANCE = it }
      }
    }
  }
}
