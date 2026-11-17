package com.barcraft.app.ui.social

import com.barcraft.app.R
import com.barcraft.app.utils.Txt
import kotlinx.coroutines.flow.distinctUntilChanged
import com.barcraft.app.model.SharedRecipe
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
) = viewModelScope.launch {
    runCatching { block() }.onFailure { messages.emit(errorText) }
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

    /** Recetas compartidas: públicas + "Seguidores" de quien sigo, sin las de bloqueados (null = cargando). */
    val recipes: StateFlow<List<SharedRecipe>?> =
        combine(
            container.recipeShareRepository.observeCommunity(
                me.uid,
                container.friendsRepository.observeFollowing(me.uid)
                    .map { list -> list.filter { it.accepted }.map { it.followedId }.sorted() }
                    .distinctUntilChanged()
            ),
            container.friendsRepository.observeSettings(me.uid)
        ) { recipes, settings -> recipes?.filter { it.authorId !in settings.blocked } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val myUid = me.uid

    /** Ids de las publicaciones que tengo en Guardados. */
    val savedIds: StateFlow<Set<String>> =
        social.observeSaved(me.uid).map { list -> list.map { it.postId }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun toggleSave(post: Post) = launchSafely(_messages, Txt.get(R.string.msg_save_failed)) {
        if (post.id in savedIds.value) {
            social.unsavePost(myUid, post.id)
            _messages.emit(Txt.get(R.string.msg_post_unsaved))
        } else {
            social.savePost(myUid, post)
            _messages.emit(Txt.get(R.string.msg_post_saved))
        }
    }

    fun edit(post: Post, text: String) = launchSafely(_messages, Txt.get(R.string.msg_post_edit_failed)) {
        social.editPost(post.id, text)
        _messages.emit(Txt.get(R.string.msg_post_updated))
    }

    fun toggleLike(post: Post, uid: String) =
        launchSafely(_messages, Txt.get(R.string.msg_like_failed)) { social.setLiked(post.id, uid, !post.isLikedBy(uid)) }

    fun delete(post: Post) =
        launchSafely(_messages, Txt.get(R.string.msg_post_delete_failed)) {
            social.deletePost(post.id)
            if (post.mediaId.isNotBlank()) container.mediaRepository.delete(post.mediaId)
            _messages.emit(Txt.get(R.string.msg_post_deleted))
        }

    fun messageAuthor(me: UserProfile, post: Post, onReady: (String) -> Unit) =
        // Si su cuenta es privada y no os seguís los dos, las reglas no dejan crear la conversación.
        launchSafely(_messages, Txt.get(R.string.msg_chat_open_failed)) {
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
        launchSafely(_messages, Txt.get(R.string.msg_like_failed)) { social.setLiked(p.id, uid, !p.isLikedBy(uid)) }
    }

    fun comment(me: UserProfile, text: String) {
        if (text.isBlank()) return
        launchSafely(_messages, Txt.get(R.string.msg_comment_failed)) { social.addComment(postId, me, text) }
    }

    fun deleteComment(comment: Comment) =
        launchSafely(_messages, Txt.get(R.string.msg_comment_delete_failed)) { social.deleteComment(postId, comment.id) }

    fun deletePost(onDone: () -> Unit) =
        launchSafely(_messages, Txt.get(R.string.msg_post_delete_failed)) {
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
            viewModelScope.launch { _messages.emit(Txt.get(R.string.msg_only_barmans_publish)) }
            return
        }
        if (text.isBlank() && cocktail == null && _photo.value == null) {
            viewModelScope.launch { _messages.emit(Txt.get(R.string.msg_post_empty)) }
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
            if (result.isSuccess) onDone() else _messages.emit(Txt.get(R.string.msg_publish_failed))
        }
    }

    /** Resumen de la receta que viaja con la publicación. */
    private fun summary(c: CocktailWithDetails): String = buildString {
        append(c.cocktail.category)
        append(Txt.get(R.string.compose_summary_minutes, c.cocktail.prepTimeMinutes))
        if (c.cocktail.hasSmoke) append(Txt.get(R.string.compose_summary_smoke))
        appendLine()
        c.sortedIngredients.forEach { appendLine(Txt.get(R.string.cocktail_ingredient_bullet, it.amount, it.name).trimEnd()) }
        if (c.cocktail.hasGarnish && c.cocktail.garnishDescription.isNotBlank()) {
            appendLine(Txt.get(R.string.compose_summary_garnish, c.cocktail.garnishDescription))
        }
        if (c.steps.isNotEmpty()) {
            appendLine()
            c.sortedSteps.forEachIndexed { i, s -> appendLine(Txt.get(R.string.shared_step, i + 1, s.instruction)) }
        }
    }.trim()
}
