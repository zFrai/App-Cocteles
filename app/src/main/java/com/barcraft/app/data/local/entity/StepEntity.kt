package com.barcraft.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "steps",
    foreignKeys = [ForeignKey(
        entity = CocktailEntity::class,
        parentColumns = ["id"],
        childColumns = ["cocktailId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cocktailId")]
)
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cocktailId: Long,
    val position: Int,
    val instruction: String
)
