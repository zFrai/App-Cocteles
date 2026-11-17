package com.barcraft.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.Public
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val RECIPES = "recipes"
    const val COMMUNITY = "community"
    const val CHATS = "chats"
    const val SEARCH = "search"
    const val ADMIN = "admin"
    const val PROFILE = "profile"
    const val MY_POSTS = "profile/posts"
    const val SAVED = "profile/saved"

    const val COCKTAIL = "cocktail/{id}"
    fun cocktail(id: Long) = "cocktail/$id"

    const val EDITOR = "editor/{id}"
    fun editor(id: Long = 0) = "editor/$id"

    /** Receta compartida en Comunidad: `users/{authorId}/recipes/{recipeId}`. */
    const val SHARED_RECIPE = "shared/{authorId}/{recipeId}"
    fun sharedRecipe(authorId: String, recipeId: String) = "shared/$authorId/$recipeId"

    const val POST = "post/{id}"
    fun post(id: String) = "post/$id"

    const val COMPOSE = "compose?cocktailId={cocktailId}"
    fun compose(cocktailId: Long = 0) = "compose?cocktailId=$cocktailId"

    const val CHAT = "chat/{roomId}"
    fun chat(roomId: String) = "chat/$roomId"
}

/** Pestañas de la barra inferior. El panel de Admin se abre desde Perfil. */
enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    RECIPES(Routes.RECIPES, "Recetas", Icons.Outlined.LocalBar, Icons.Filled.LocalBar),
    COMMUNITY(Routes.COMMUNITY, "Comunidad", Icons.Outlined.Public, Icons.Filled.Public),
    CHATS(Routes.CHATS, "Chats", Icons.Outlined.Forum, Icons.Filled.Forum),
    SEARCH(Routes.SEARCH, "Buscar", Icons.Outlined.PersonSearch, Icons.Filled.PersonSearch),
    PROFILE(Routes.PROFILE, "Perfil", Icons.Outlined.Person, Icons.Filled.Person)
}
