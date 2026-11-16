package com.barcraft.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ingredients",
    foreignKeys = [ForeignKey(
        entity = CocktailEntity::class,
        parentColumns = ["id"],
        childColumns = ["cocktailId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cocktailId")]
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cocktailId: Long,
    val name: String,
    val amount: String,
    /** Alcohol, Mezcla, Garnish o Extra. */
    val type: String = "Alcohol",
    val position: Int = 0
)
