package com.barcraft.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ratings",
    foreignKeys = [ForeignKey(
        entity = CocktailEntity::class,
        parentColumns = ["id"],
        childColumns = ["cocktailId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cocktailId")]
)
data class RatingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cocktailId: Long,
    val voterName: String,
    /** De 1 a 6 estrellas. */
    val stars: Int,
    val comment: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
