package com.barcraft.app.ui.navigation

import androidx.compose.ui.res.stringResource
import com.barcraft.app.R
import com.barcraft.app.ui.components.FitText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.barcraft.app.ui.admin.AdminScreen
import com.barcraft.app.ui.auth.AuthFlow
import com.barcraft.app.ui.auth.BannedScreen
import com.barcraft.app.ui.auth.SessionLoadingScreen
import com.barcraft.app.ui.chat.ChatListScreen
import com.barcraft.app.ui.chat.ChatScreen
import com.barcraft.app.ui.chat.PeopleScreen
import com.barcraft.app.ui.components.AccountRequiredState
import com.barcraft.app.ui.cocktails.CocktailDetailScreen
import com.barcraft.app.ui.cocktails.CocktailEditorScreen
import com.barcraft.app.ui.cocktails.CocktailListScreen
import com.barcraft.app.ui.lock.LockGate
import com.barcraft.app.ui.profile.MyPostsScreen
import com.barcraft.app.ui.profile.ProfileScreen
import com.barcraft.app.ui.profile.SavedPostsScreen
import com.barcraft.app.ui.social.ComposePostScreen
import com.barcraft.app.ui.social.FeedScreen
import com.barcraft.app.ui.social.PostDetailScreen
import com.barcraft.app.ui.social.SharedRecipeScreen
import com.barcraft.app.utils.appViewModel

/** Punto de entrada de la interfaz: decide entre login, bloqueo o la app. */
@Composable
fun BarCraftRoot() {
    val sessionVm = appViewModel { SessionViewModel(it) }
    val state by sessionVm.state.collectAsStateWithLifecycle()

    when (val s = state) {
        SessionState.Loading -> SessionLoadingScreen(onLogout = sessionVm::logout)
        SessionState.SignedOut -> AuthFlow(onContinueAsGuest = sessionVm::continueAsGuest)
        is SessionState.Banned -> BannedScreen(s.profile, onLogout = sessionVm::logout)
        is SessionState.Local -> MainScaffold(
            session = AppSession(profile = null, firebaseEnabled = s.firebaseEnabled),
            onLogin = sessionVm::goToLogin,
            onLogout = sessionVm::logout
        )
        is SessionState.SignedIn -> LockGate(s.profile, onLogout = sessionVm::logout) {
            MainScaffold(
                session = AppSession(profile = s.profile, firebaseEnabled = true),
                onLogin = sessionVm::goToLogin,
                onLogout = sessionVm::logout
            )
        }
    }
}

@Composable
private fun MainScaffold(session: AppSession, onLogin: () -> Unit, onLogout: () -> Unit) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination

    val tabs = TopDestination.entries
    val showBottomBar = tabs.any { tab -> current?.hierarchy?.any { it.route == tab.route } == true }

    CompositionLocalProvider(LocalAppSession provides session) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            val selected = current?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { nav.navigateToTab(tab.route) },
                                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                                label = { FitText(stringResource(tab.labelRes)) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            // Solo se aplica el hueco inferior; cada pantalla dibuja su propia barra superior.
            val bottom = PaddingValues(bottom = padding.calculateBottomPadding())
            Box(Modifier.fillMaxSize().padding(bottom).consumeWindowInsets(bottom)) {
                AppNavHost(nav, session, onLogin, onLogout)
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppNavHost(
    nav: NavHostController,
    session: AppSession,
    onLogin: () -> Unit,
    onLogout: () -> Unit
) {
    NavHost(navController = nav, startDestination = Routes.RECIPES) {
        // ── Recetas (Room, funciona sin conexión) ─────────────────────
        composable(Routes.RECIPES) {
            CocktailListScreen(
                onOpen = { nav.navigate(Routes.cocktail(it)) },
                onEdit = { nav.navigate(Routes.editor(it)) },
                onCreate = { nav.navigate(Routes.editor()) }
            )
        }
        composable(
            Routes.COCKTAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            CocktailDetailScreen(
                cocktailId = entry.arguments?.getLong("id") ?: 0L,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.editor(it)) },
                onShare = { nav.navigate(Routes.compose(it)) }
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            CocktailEditorScreen(
                cocktailId = entry.arguments?.getLong("id") ?: 0L,
                onDone = { nav.popBackStack() }
            )
        }

        // ── Comunidad (red social) ────────────────────────────────────
        composable(Routes.COMMUNITY) {
            FeedScreen(
                onLogin = onLogin,
                onOpenPost = { nav.navigate(Routes.post(it)) },
                onCompose = { nav.navigate(Routes.compose()) },
                onMessageUser = { roomId -> nav.navigate(Routes.chat(roomId)) },
                onOpenRecipe = { authorId, recipeId -> nav.navigate(Routes.sharedRecipe(authorId, recipeId)) }
            )
        }
        composable(
            Routes.SHARED_RECIPE,
            arguments = listOf(
                navArgument("authorId") { type = NavType.StringType },
                navArgument("recipeId") { type = NavType.StringType }
            )
        ) { entry ->
            SharedRecipeScreen(
                authorId = entry.arguments?.getString("authorId").orEmpty(),
                recipeId = entry.arguments?.getString("recipeId").orEmpty(),
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.POST, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            PostDetailScreen(
                postId = entry.arguments?.getString("id").orEmpty(),
                onBack = { nav.popBackStack() }
            )
        }
        composable(
            Routes.COMPOSE,
            arguments = listOf(navArgument("cocktailId") { type = NavType.LongType; defaultValue = 0L })
        ) { entry ->
            ComposePostScreen(
                initialCocktailId = entry.arguments?.getLong("cocktailId") ?: 0L,
                onDone = {
                    nav.popBackStack()
                    nav.navigateToTab(Routes.COMMUNITY)
                },
                onBack = { nav.popBackStack() }
            )
        }

        // ── Chat entre usuarios ───────────────────────────────────────
        composable(Routes.CHATS) {
            ChatListScreen(
                onLogin = onLogin,
                onOpenRoom = { nav.navigate(Routes.chat(it)) },
                onFindPeople = { nav.navigateToTab(Routes.SEARCH) }
            )
        }
        composable(Routes.CHAT, arguments = listOf(navArgument("roomId") { type = NavType.StringType })) { entry ->
            ChatScreen(
                roomId = entry.arguments?.getString("roomId").orEmpty(),
                onBack = { nav.popBackStack() }
            )
        }

        // ── Buscar personas ───────────────────────────────────────────
        composable(Routes.SEARCH) {
            if (session.profile == null) {
                AccountRequiredState(session, stringResource(R.string.feature_search_people), onLogin)
            } else {
                PeopleScreen(
                    onBack = null,
                    onRoomReady = { roomId -> nav.navigate(Routes.chat(roomId)) }
                )
            }
        }

        // ── Perfil (el panel de Admin cuelga de aquí) ─────────────────
        composable(Routes.PROFILE) {
            ProfileScreen(
                onLogin = onLogin,
                onLogout = onLogout,
                onOpenAdmin = { nav.navigate(Routes.ADMIN) },
                onOpenMyPosts = { nav.navigate(Routes.MY_POSTS) },
                onOpenSaved = { nav.navigate(Routes.SAVED) }
            )
        }
        composable(Routes.MY_POSTS) {
            MyPostsScreen(onBack = { nav.popBackStack() }, onOpenPost = { nav.navigate(Routes.post(it)) })
        }
        composable(Routes.SAVED) {
            SavedPostsScreen(onBack = { nav.popBackStack() }, onOpenPost = { nav.navigate(Routes.post(it)) })
        }
        composable(Routes.ADMIN) {
            if (session.isAdmin) AdminScreen(
                onOpenPost = { nav.navigate(Routes.post(it)) },
                onBack = { nav.popBackStack() }
            )
        }
    }
}
