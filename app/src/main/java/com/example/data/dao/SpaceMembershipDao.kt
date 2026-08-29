package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entity.SpaceMembershipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SpaceMembershipDao {
  @Query("SELECT * FROM space_memberships WHERE space_id = :spaceId ORDER BY order_index ASC, added_at ASC")
  fun getMembershipsForSpaceFlow(spaceId: String): Flow<List<SpaceMembershipEntity>>

  @Query("SELECT * FROM space_memberships WHERE space_id = :spaceId ORDER BY order_index ASC, added_at ASC")
  suspend fun getMembershipsForSpace(spaceId: String): List<SpaceMembershipEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMembership(membership: SpaceMembershipEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMemberships(memberships: List<SpaceMembershipEntity>)

  @Query("UPDATE space_memberships SET order_index = :newOrderIndex WHERE space_id = :spaceId AND package_name = :packageName AND component_name = :componentName AND user_handle_id = :userHandleId")
  suspend fun updateMembershipOrder(
    spaceId: String,
    packageName: String,
    componentName: String,
    userHandleId: Long,
    newOrderIndex: Int
  ): Int

  @Query("DELETE FROM space_memberships WHERE space_id = :spaceId AND package_name = :packageName AND component_name = :componentName AND user_handle_id = :userHandleId")
  suspend fun deleteMembership(
    spaceId: String,
    packageName: String,
    componentName: String,
    userHandleId: Long
  ): Int

  @Query("DELETE FROM space_memberships WHERE space_id = :spaceId AND package_name = :packageName")
  suspend fun deleteMembershipByPackage(
    spaceId: String,
    packageName: String
  ): Int

  @Query("DELETE FROM space_memberships WHERE space_id = :spaceId")
  suspend fun deleteMembershipsForSpace(spaceId: String): Int

  @Query("SELECT COUNT(*) FROM space_memberships WHERE space_id = :spaceId")
  suspend fun getMembershipCountForSpace(spaceId: String): Int
}
