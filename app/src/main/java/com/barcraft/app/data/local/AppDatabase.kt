package com.barcraft.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.barcraft.app.data.local.entity.CocktailEntity
import com.barcraft.app.data.local.entity.IngredientEntity
import com.barcraft.app.data.local.entity.RatingEntity
import com.barcraft.app.data.local.entity.StepEntity

/** Base de datos local (Room): las recetas funcionan sin conexión. */
@Database(
    entities = [CocktailEntity::class, IngredientEntity::class, StepEntity::class, RatingEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cocktailDao(): CocktailDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "barcraft.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
