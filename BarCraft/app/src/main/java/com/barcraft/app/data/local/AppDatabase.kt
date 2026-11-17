package com.barcraft.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.barcraft.app.data.local.entity.CocktailEntity
import com.barcraft.app.data.local.entity.IngredientEntity
import com.barcraft.app.data.local.entity.RatingEntity
import com.barcraft.app.data.local.entity.StepEntity

/** Base de datos local (Room): las recetas funcionan sin conexión. */
@Database(
    entities = [CocktailEntity::class, IngredientEntity::class, StepEntity::class, RatingEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun cocktailDao(): CocktailDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        /** v2: visibilidad de cada receta. Sin esta migración se borrarían las recetas al actualizar. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cocktails ADD COLUMN visibility TEXT NOT NULL DEFAULT 'private'")
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "barcraft.db"
                ).addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
