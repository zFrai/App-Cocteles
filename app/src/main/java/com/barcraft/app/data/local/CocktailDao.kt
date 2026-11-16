package com.barcraft.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.barcraft.app.data.local.entity.CocktailEntity
import com.barcraft.app.data.local.entity.CocktailWithDetails
import com.barcraft.app.data.local.entity.IngredientEntity
import com.barcraft.app.data.local.entity.RatingEntity
import com.barcraft.app.data.local.entity.StepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CocktailDao {

    @Transaction
    @Query("SELECT * FROM cocktails ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CocktailWithDetails>>

    @Transaction
    @Query("SELECT * FROM cocktails WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<CocktailWithDetails?>

    @Transaction
    @Query("SELECT * FROM cocktails WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CocktailWithDetails?

    @Query("SELECT * FROM cocktails WHERE id = :id LIMIT 1")
    suspend fun getEntity(id: Long): CocktailEntity?

    @Insert
    suspend fun insertCocktail(cocktail: CocktailEntity): Long

    @Update
    suspend fun updateCocktail(cocktail: CocktailEntity)

    @Delete
    suspend fun deleteCocktail(cocktail: CocktailEntity)

    @Query("UPDATE cocktails SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Insert
    suspend fun insertIngredients(list: List<IngredientEntity>)

    @Query("DELETE FROM ingredients WHERE cocktailId = :cocktailId")
    suspend fun deleteIngredients(cocktailId: Long)

    @Insert
    suspend fun insertSteps(list: List<StepEntity>)

    @Query("DELETE FROM steps WHERE cocktailId = :cocktailId")
    suspend fun deleteSteps(cocktailId: Long)

    @Insert
    suspend fun insertRating(rating: RatingEntity)

    @Query("DELETE FROM ratings WHERE id = :ratingId")
    suspend fun deleteRating(ratingId: Long)

    @Query(
        """
        UPDATE cocktails SET
            averageRating = COALESCE((SELECT AVG(stars) FROM ratings WHERE cocktailId = :id), 0),
            ratingCount   = (SELECT COUNT(*) FROM ratings WHERE cocktailId = :id)
        WHERE id = :id
        """
    )
    suspend fun refreshRatingStats(id: Long)
}
