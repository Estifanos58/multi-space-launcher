package com.multispace.data.dao

import androidx.room.*
import com.multispace.data.entity.SpaceDockItemEntity
import com.multispace.data.entity.SpaceFolderEntity
import com.multispace.data.entity.SpaceFolderItemEntity
import com.multispace.data.entity.SpaceItemPlacementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceLayoutDao {

  // --- Placements ---
  @Query("SELECT * FROM space_item_placements WHERE space_id = :spaceId AND layer = :layer ORDER BY page_index ASC, position_index ASC")
  fun getPlacementsForSpaceLayerFlow(spaceId: String, layer: Int): Flow<List<SpaceItemPlacementEntity>>

  @Query("SELECT * FROM space_item_placements WHERE space_id = :spaceId AND layer = :layer ORDER BY page_index ASC, position_index ASC")
  suspend fun getPlacementsForSpaceLayer(spaceId: String, layer: Int): List<SpaceItemPlacementEntity>

  @Query("SELECT * FROM space_item_placements WHERE space_id = :spaceId ORDER BY layer ASC, page_index ASC, position_index ASC")
  suspend fun getAllPlacementsForSpace(spaceId: String): List<SpaceItemPlacementEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlacement(placement: SpaceItemPlacementEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlacements(placements: List<SpaceItemPlacementEntity>)

  @Update
  suspend fun updatePlacement(placement: SpaceItemPlacementEntity)

  @Query("DELETE FROM space_item_placements WHERE id = :placementId")
  suspend fun deletePlacementById(placementId: String)

  @Query("DELETE FROM space_item_placements WHERE space_id = :spaceId AND layer = :layer")
  suspend fun deletePlacementsForSpaceLayer(spaceId: String, layer: Int)

  @Query("DELETE FROM space_item_placements WHERE space_id = :spaceId")
  suspend fun deleteAllPlacementsForSpace(spaceId: String)

  @Query("DELETE FROM space_item_placements WHERE folder_id = :folderId")
  suspend fun deletePlacementByFolderId(folderId: String)

  // --- Folders ---
  @Query("SELECT * FROM space_folders WHERE space_id = :spaceId ORDER BY created_at ASC")
  fun getFoldersForSpaceFlow(spaceId: String): Flow<List<SpaceFolderEntity>>

  @Query("SELECT * FROM space_folders WHERE space_id = :spaceId ORDER BY created_at ASC")
  suspend fun getFoldersForSpace(spaceId: String): List<SpaceFolderEntity>

  @Query("SELECT * FROM space_folders WHERE id = :folderId")
  suspend fun getFolderById(folderId: String): SpaceFolderEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFolder(folder: SpaceFolderEntity)

  @Update
  suspend fun updateFolder(folder: SpaceFolderEntity)

  @Query("DELETE FROM space_folders WHERE id = :folderId")
  suspend fun deleteFolderById(folderId: String)

  // --- Folder Items ---
  @Query("SELECT * FROM space_folder_items WHERE folder_id = :folderId ORDER BY order_index ASC")
  fun getFolderItemsFlow(folderId: String): Flow<List<SpaceFolderItemEntity>>

  @Query("SELECT * FROM space_folder_items WHERE folder_id = :folderId ORDER BY order_index ASC")
  suspend fun getFolderItems(folderId: String): List<SpaceFolderItemEntity>

  @Query("SELECT * FROM space_folder_items WHERE folder_id IN (SELECT id FROM space_folders WHERE space_id = :spaceId) ORDER BY order_index ASC")
  fun getAllFolderItemsForSpaceFlow(spaceId: String): Flow<List<SpaceFolderItemEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFolderItem(item: SpaceFolderItemEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFolderItems(items: List<SpaceFolderItemEntity>)

  @Query("DELETE FROM space_folder_items WHERE id = :itemId")
  suspend fun deleteFolderItemById(itemId: String)

  @Query("DELETE FROM space_folder_items WHERE folder_id = :folderId")
  suspend fun deleteFolderItemsForFolder(folderId: String)

  // --- Dock Items ---
  @Query("SELECT * FROM space_dock_items WHERE space_id = :spaceId ORDER BY order_index ASC")
  fun getDockItemsForSpaceFlow(spaceId: String): Flow<List<SpaceDockItemEntity>>

  @Query("SELECT * FROM space_dock_items WHERE space_id = :spaceId ORDER BY order_index ASC")
  suspend fun getDockItemsForSpace(spaceId: String): List<SpaceDockItemEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDockItem(item: SpaceDockItemEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDockItems(items: List<SpaceDockItemEntity>)

  @Query("DELETE FROM space_dock_items WHERE id = :itemId")
  suspend fun deleteDockItemById(itemId: String)

  @Query("DELETE FROM space_dock_items WHERE space_id = :spaceId")
  suspend fun deleteAllDockItemsForSpace(spaceId: String)

  @Query("UPDATE space_item_placements SET span_x = :spanX, span_y = :spanY WHERE id = :placementId")
  suspend fun updateWidgetSpan(placementId: String, spanX: Int, spanY: Int)

  @Query("UPDATE space_item_placements SET span_x = :spanX, span_y = :spanY, position_index = :positionIndex WHERE id = :placementId")
  suspend fun updateWidgetSpanAndPosition(placementId: String, spanX: Int, spanY: Int, positionIndex: Int)

  // --- Cleanup on App Uninstall ---
  @Query("DELETE FROM space_item_placements WHERE package_name = :packageName")
  suspend fun deletePlacementsForPackage(packageName: String)

  @Query("DELETE FROM space_folder_items WHERE package_name = :packageName")
  suspend fun deleteFolderItemsForPackage(packageName: String)

  @Query("DELETE FROM space_dock_items WHERE package_name = :packageName")
  suspend fun deleteDockItemsForPackage(packageName: String)
}
