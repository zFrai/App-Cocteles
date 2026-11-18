package com.barcraft.app

import android.content.Context
import com.barcraft.app.data.local.AppDatabase
import com.barcraft.app.data.local.BiometricCredentialStore
import com.barcraft.app.data.local.LockStore
import com.barcraft.app.data.local.SettingsStore
import com.barcraft.app.data.remote.FirebaseProvider
import com.barcraft.app.repository.AuthRepository
import com.barcraft.app.repository.AvatarCache
import com.barcraft.app.repository.ChatRepository
import com.barcraft.app.repository.CocktailRepository
import com.barcraft.app.repository.FriendsRepository
import com.barcraft.app.repository.GiphyRepository
import com.barcraft.app.repository.MediaRepository
import com.barcraft.app.repository.ReportRepository
import com.barcraft.app.repository.RecipeShareRepository
import com.barcraft.app.repository.SocialRepository
import com.barcraft.app.repository.UserRepository
import com.barcraft.app.utils.AppLockController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Contenedor de dependencias (inyección manual, sin librerías extra).
 * Los repositorios de Firebase solo se crean si Firebase está configurado.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val firebaseEnabled: Boolean = FirebaseProvider.isConfigured(appContext)

    val settings = SettingsStore(appContext)
    val lockStore = LockStore(appContext)
    val biometricLogin = BiometricCredentialStore(appContext)
    val appLock = AppLockController()

    val cocktailRepository = CocktailRepository(AppDatabase.get(appContext))
    val giphyRepository = GiphyRepository(BuildConfig.GIPHY_API_KEY)

    private val firestore by lazy {
        FirebaseFirestore.getInstance().apply {
            // Caché local limitada a 50 MB: los chats se ven sin conexión sin llenar el móvil.
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(PersistentCacheSettings.newBuilder().setSizeBytes(50L * 1024 * 1024).build())
                .build()
        }
    }

    val authRepository by lazy { AuthRepository(FirebaseAuth.getInstance(), firestore) }
    val userRepository by lazy { UserRepository(firestore) }
    val chatRepository by lazy { ChatRepository(firestore) }
    val socialRepository by lazy { SocialRepository(firestore) }
    val recipeShareRepository by lazy { RecipeShareRepository(firestore) }
    val friendsRepository by lazy { FriendsRepository(firestore) }
    val mediaRepository by lazy { MediaRepository(firestore, appContext) }
    val reportRepository by lazy { ReportRepository(firestore) }
    val avatarCache by lazy { AvatarCache(userRepository) }
}
