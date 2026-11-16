package com.barcraft.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class CocktailWithDetails(
    @Embedded val cocktail: CocktailEntity,
    @Relation(parentColumn = "id", entityColumn = "cocktailId")
    val ingredients: List<IngredientEntity>,
    @Relation(parentColumn = "id", entityColumn = "cocktailId")
    val steps: List<StepEntity>,
    @Relation(parentColumn = "id", entityColumn = "cocktailId")
    val ratings: List<RatingEntity>
) {
    val sortedIngredients: List<IngredientEntity> get() = ingredients.sortedBy { it.position }
    val sortedSteps: List<StepEntity> get() = steps.sortedBy { it.position }
    val sortedRatings: List<RatingEntity> get() = ratings.sortedByDescending { it.createdAt }
}
