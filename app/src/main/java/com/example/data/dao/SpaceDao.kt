package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.SpaceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceDao {
  @Query("SELECT * FROM spaces ORDER BY order_index ASC, created_at ASC")
  fun getAllSpacesFlow(): Flow<List<SpaceEntity>>

  @Query("SELECT * FROM spaces ORDER BY order_index ASC, created_at ASC")
  suspend fun getAllSpaces(): List<SpaceEntity>

  @Query("SELECT * FROM spaces WHERE id = :id LIMIT 1")
  suspend fun getSpaceById(id: String): SpaceEntity?

  @Query("SELECT * FROM spaces WHERE id = :id LIMIT 1")
  fun getSpaceByIdFlow(id: String): Flow<SpaceEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSpace(space: SpaceEntity): Long

  @Update
  suspend fun updateSpace(space: SpaceEntity)

  @Query("DELETE FROM spaces WHERE id = :id")
  suspend fun deleteSpaceById(id: String): Int

  @Query("SELECT COUNT(*) FROM spaces")
  suspend fun getSpaceCount(): Int
}
