package com.barcraft.app.model

/** Valores fijos que usan las recetas. */
object CocktailCatalog {
    val categories = listOf(
        "Clásico", "Tropical", "Sour", "Tiki", "Creamy",
        "Highball", "Martini", "Signature", "Sin alcohol", "Otro"
    )

    /** Tipos de ingrediente: alcoholes, mezclas, garnish y extras. */
    val ingredientTypes = listOf("Alcohol", "Mezcla", "Garnish", "Extra")

    const val MAX_STARS = 6
    const val MAX_DIFFICULTY = 5
}
