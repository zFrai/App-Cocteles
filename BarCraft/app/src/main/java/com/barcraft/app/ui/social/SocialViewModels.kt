package com.barcraft.app.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.data.local.entity.CocktailWithDetails
import com.barcraft.app.model.Comment
import com.barcraft.app.model.Permissions
import com.barcraft.app.model.Post
import com.barcraft.app.model.UserProfile
import android.net.Uri
import com.barcraft.app.AppContainer
import com.barcraft.app.repository.SocialRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Ejecuta una acción remota y avisa si falla (por ejemplo, sin permisos o sin red). */
internal fun ViewModel.launchSafely(
    messages: MutableSharedFlow<String>,
    errorText: String,
    block: suspend () -> Unit
) {
    viewModelScope.launch {
        runCatching { block() }.onFailure { messages.emit(errorText) }
    }
}

class FeedViewModel(
    private val container: AppContainer,
    me: UserProfile
) : ViewModel() {

    private val social = container.socialRepository
    private val chat = container.chatRepository

    /** Publicaciones, sin las de personas que he bloqueado. */
    val posts: StateFlow<List<Post>?> =
        combine(social.observeFeed(), container.friendsRepository.observeSettings(me.uid)) { posts, settings ->
            posts.filter { it.authorId !in settings.blocked }
        }.map<List<Post>, List<Post>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun toggleLike(post: Post, uid: String) =
        launchSafely(_messages, "No se pudo guardar el me gusta") { social.setLiked(post.id, uid, !post.isLikedBy(uid)) }

    fun delete(post: Post) =
        launchSafely(_messages, "No se pudo borrar la publicación") {
            social.deletePost(post.id)
            if (post.mediaId.isNotBlank()) container.mediaRepository.delete(post.mediaId)
            _messages.emit("Publicación eliminada")
        }

    fun messageAuthor(me: UserProfile, post: Post, onReady: (String) -> Unit) =
        // Si su cuenta es privada y no os seguís los dos, las reglas no dejan crear la conversación.
        launchSafely(_messages, "No se pudo abrir el chat: si su cuenta es privada, tenéis que seguiros los dos") {
            onReady(chat.openRoom(me, post.authorId, post.authorName))
        }
}

class PostDetailViewModel(
    private val social: SocialRepository,
    private val postId: String
) : ViewModel() {

    val post: StateFlow<Post?> =
        social.observePost(postId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val comments: StateFlow<List<Comment>> =
        social.observeComments(postId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun toggleLike(uid: String) {
        val p = post.value ?: return
        launchSafely(_messages, "No se pudo guardar el me gusta") { social.setLiked(p.id, uid, !p.isLikedBy(uid)) }
    }

    fun comment(me: UserProfile, text: String) {
        if (text.isBlank()) return
        launchSafely(_messages, "No se pudo enviar el comentario") { social.addComment(postId, me, text) }
    }

    fun deleteComment(comment: Comment) =
        launchSafely(_messages, "No se pudo borrar el comentario") { social.deleteComment(postId, comment.id) }

    fun deletePost(onDone: () -> Unit) =
        launchSafely(_messages, "No se pudo borrar la publicación") {
            social.deletePost(postId)
            onDone()
        }
}

class ComposePostViewModel(
    private val container: AppContainer,
    initialCocktailId: Long
) : ViewModel() {

    private val social = container.socialRepository
    private val cocktails = container.cocktailRepository

    /** Foto elegida con la cámara o la galería (se sube al publicar). */
    private val _photo = MutableStateFlow<Uri?>(null)
    val photo: StateFlow<Uri?> = _photo.asStateFlow()

    fun setPhoto(uri: Uri?) { _photo.value = uri }

    val myCocktails: StateFlow<List<CocktailWithDetails>> =
        cocktails.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _attachedId = MutableStateFlow(initialCocktailId)
    val attachedId: StateFlow<Long> = _attachedId.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun attach(id: Long) { _attachedId.value = id }

    fun publish(me: UserProfile, text: String, imageUrl: String, onDone: () -> Unit) {
        val cocktail = myCocktails.value.firstOrNull { it.cocktail.id == _attachedId.value }
        if (!Permissions.canPublishPosts(me.role)) {
            viewModelScope.launch { _messages.emit("Solo los barmans pueden publicar") }
            return
        }
        if (text.isBlank() && cocktail == null && _photo.value == null) {
            viewModelScope.launch { _messages.emit("Escribe algo o adjunta un cóctel") }
            return
        }
        viewModelScope.launch {
            _sending.value = true
            val result = runCatching {
                // Foto: la elegida, o la de la receta adjunta (si está en el móvil se sube; si es URL se enlaza).
                val recipeImage = cocktail?.cocktail?.imageUri.orEmpty()
                val localSource: Uri? = _photo.value
                    ?: recipeImage.takeIf { imageUrl.isBlank() && it.startsWith("file://") }?.let { Uri.parse(it) }
                val mediaId = localSource?.let { container.mediaRepository.uploadImage(me.uid, it) }.orEmpty()
                val url = if (mediaId.isNotBlank()) "" else imageUrl.trim().ifBlank { recipeImage.takeIf { it.startsWith("http") }.orEmpty() }
                social.createPost(
                    me = me,
                    text = text,
                    imageUrl = url,
                    mediaId = mediaId,
                    cocktailName = cocktail?.cocktail?.name.orEmpty(),
                    cocktailSummary = cocktail?.let { summary(it) }.orEmpty()
                )
            }
            _sending.value = false
            if (result.isSuccess) onDone() else _messages.emit("No se pudo publicar. Revisa tu conexión.")
        }
    }

    /** Resumen de la receta que viaja con la publicación. */
    private fun summary(c: CocktailWithDetails): String = buildString {
        append(c.cocktail.category)
        append(" · ${c.cocktail.prepTimeMinutes} min")
        if (c.cocktail.hasSmoke) append(" · Humo")
        appendLine()
        c.sortedIngredients.forEach { appendLine("• ${it.amount} ${it.name}".trimEnd()) }
        if (c.cocktail.hasGarnish && c.cocktail.garnishDescription.isNotBlank()) {
            appendLine("Garnish: ${c.cocktail.garnishDescription}")
        }
        if (c.steps.isNotEmpty()) {
            appendLine()
            c.sortedSteps.forEachIndexed { i, s -> appendLine("${i + 1}. ${s.instruction}") }
        }
    }.trim()
}
