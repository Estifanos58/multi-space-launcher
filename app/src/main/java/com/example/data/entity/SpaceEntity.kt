package com.example.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Space

@Entity(tableName = "spaces")
data class SpaceEntity(
  @PrimaryKey
  @ColumnInfo(name = "id")
  val id: String,

  @ColumnInfo(name = "name")
  val name: String,

  @ColumnInfo(name = "order_index")
  val orderIndex: Int = 0,

  @ColumnInfo(name = "created_at")
  val createdAt: Long = System.currentTimeMillis(),

  @ColumnInfo(name = "updated_at")
  val updatedAt: Long = System.currentTimeMillis(),

  @ColumnInfo(name = "auth_policy")
  val authPolicy: String = "NONE",

  @ColumnInfo(name = "layout_type")
  val layoutType: String = "GRID_4"
) {
  fun toDomain(): Space = Space(
    id = id,
    name = name,
    orderIndex = orderIndex,
    createdAt = createdAt,
    updatedAt = updatedAt,
    authPolicy = authPolicy,
    layoutType = layoutType
  )

  companion object {
    fun fromDomain(domain: Space): SpaceEntity = SpaceEntity(
      id = domain.id,
      name = domain.name,
      orderIndex = domain.orderIndex,
      createdAt = domain.createdAt,
      updatedAt = domain.updatedAt,
      authPolicy = domain.authPolicy,
      layoutType = domain.layoutType
    )
  }
}
