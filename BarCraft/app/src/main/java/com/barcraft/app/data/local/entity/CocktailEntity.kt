package com.barcraft.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cocktails")
data class CocktailEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    /** URL http(s) o ruta `file://` de una imagen copiada al almacenamiento de la app. */
    val imageUri: String = "",
    val category: String = "Clásico",
    val difficulty: Int = 2,
    val prepTimeMinutes: Int = 5,
    val hasSmoke: Boolean = false,
    val hasGarnish: Boolean = false,
    val garnishDescription: String = "",
    val isFavorite: Boolean = false,
    val averageRating: Float = 0f,
    val ratingCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    /** "private", "followers" o "public" (ver RecipeVisibility). Añadida en la versión 2 de la base de datos. */
    @ColumnInfo(defaultValue = "private") val visibility: String = "private"
)
