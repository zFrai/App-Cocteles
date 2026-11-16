package com.barcraft.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.LocalBar
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val RECIPES = "recipes"
    const val COMMUNITY = "community"
    const val CHATS = "chats"
    const val ADMIN = "admin"
    const val PROFILE = "profile"

    const val COCKTAIL = "cocktail/{id}"
    fun cocktail(id: Long) = "cocktail/$id"

    const val EDITOR = "editor/{id}"
    fun editor(id: Long = 0) = "editor/$id"

    const val POST = "post/{id}"
    fun post(id: String) = "post/$id"

    const val COMPOSE = "compose?cocktailId={cocktailId}"
    fun compose(cocktailId: Long = 0) = "compose?cocktailId=$cocktailId"

    const val CHAT = "chat/{roomId}"
    fun chat(roomId: String) = "chat/$roomId"

    const val PEOPLE = "people"
}

/** Pestañas de la barra inferior. */
enum class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    RECIPES(Routes.RECIPES, "Recetas", Icons.Outlined.LocalBar, Icons.Filled.LocalBar),
    COMMUNITY(Routes.COMMUNITY, "Comunidad", Icons.Outlined.Public, Icons.Filled.Public),
    CHATS(Routes.CHATS, "Chats", Icons.Outlined.Forum, Icons.Filled.Forum),
    ADMIN(Routes.ADMIN, "Admin", Icons.Outlined.AdminPanelSettings, Icons.Filled.AdminPanelSettings),
    PROFILE(Routes.PROFILE, "Perfil", Icons.Outlined.Person, Icons.Filled.Person)
}
