package com.barcraft.app.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.AppContainer
import com.barcraft.app.model.ChatMessage
import com.barcraft.app.model.ChatRoom
import com.barcraft.app.model.FriendState
import com.barcraft.app.model.Friendship
import com.barcraft.app.model.GiphyItem
import com.barcraft.app.model.MessageType
import com.barcraft.app.model.PrivateSettings
import com.barcraft.app.model.UserProfile
import com.barcraft.app.repository.GiphyRepository
import com.barcraft.app.repository.OutgoingMessage
import com.barcraft.app.ui.social.launchSafely
import com.barcraft.app.utils.AudioRecorder
import kotlinx.coroutines.Job
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
import java.io.File

// ─────────────────────────────────────────────────────────────────────
//  Personas: amigos, solicitudes, favoritos y bloqueados
// ─────────────────────────────────────────────────────────────────────

/** Otra persona vista desde mi cuenta. */
data class Person(
    val profile: UserProfile,
    val state: FriendState,
    val friendshipId: String?,
    val favorite: Boolean,
    val blocked: Boolean
)

data class PeopleState(
    val loaded: Boolean = false,
    val everyone: List<Person> = emptyList(),
    val settings: PrivateSettings = PrivateSettings()
) {
    val friends get() = everyone.filter { it.state == FriendState.FRIENDS && !it.blocked }
        .sortedWith(compareByDescending<Person> { it.favorite }.thenBy { it.profile.name.lowercase() })
    val incoming get() = everyone.filter { it.state == FriendState.REQUEST_RECEIVED && !it.blocked }
    val outgoing get() = everyone.filter { it.state == FriendState.REQUEST_SENT && !it.blocked }
    val blocked get() = everyone.filter { it.blocked }
    fun byUid(uid: String) = everyone.firstOrNull { it.profile.uid == uid }
}

/** Compartido por las pantallas de chat: lista, personas y conversación. */
class PeopleViewModel(private val container: AppContainer, private val me: UserProfile) : ViewModel() {

    private val friends = container.friendsRepository

    val state: StateFlow<PeopleState> =
        combine(
            container.userRepository.observeAll(),
            friends.observeFriendships(me.uid),
            friends.observeSettings(me.uid)
        ) { users, friendships, settings ->
            val byOther: Map<String, Friendship> = friendships.associateBy { it.otherId(me.uid) }
            PeopleState(
                loaded = true,
                settings = settings,
                everyone = users.filter { it.uid != me.uid }.map { u ->
                    val f = byOther[u.uid]
                    Person(
                        profile = u,
                        state = when {
                            f == null -> FriendState.NONE
                            f.accepted -> FriendState.FRIENDS
                            f.requesterId == me.uid -> FriendState.REQUEST_SENT
                            else -> FriendState.REQUEST_RECEIVED
                        },
                        friendshipId = f?.id,
                        favorite = u.uid in settings.favorites,
                        blocked = u.uid in settings.blocked
                    )
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleState())

    val rooms: StateFlow<List<ChatRoom>?> =
        container.chatRepository.observeRooms(me.uid).map<List<ChatRoom>, List<ChatRoom>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun sendRequest(p: Person) = launchSafely(_messages, "No se pudo enviar la solicitud") {
        friends.sendRequest(me, p.profile)
        _messages.emit("Solicitud enviada a ${p.profile.name}")
    }

    fun accept(p: Person) = launchSafely(_messages, "No se pudo aceptar") {
        p.friendshipId?.let { friends.accept(it) }
        _messages.emit("${p.profile.name} y tú ya sois amigos")
    }

    /** Rechazar, cancelar o eliminar amigo. */
    fun removeFriend(p: Person) = launchSafely(_messages, "No se pudo completar") {
        p.friendshipId?.let { friends.remove(it) }
    }

    fun toggleFavorite(p: Person) = launchSafely(_messages, "No se pudo guardar el favorito") {
        friends.setFavorite(me.uid, p.profile.uid, !p.favorite)
    }

    fun block(p: Person) = launchSafely(_messages, "No se pudo bloquear") {
        friends.block(me.uid, p.profile.uid)
        _messages.emit("${p.profile.name} bloqueado")
    }

    fun unblock(uid: String, name: String) = launchSafely(_messages, "No se pudo desbloquear") {
        friends.unblock(me.uid, uid)
        _messages.emit("$name desbloqueado")
    }

    fun openChat(uid: String, name: String, onReady: (String) -> Unit) =
        launchSafely(_messages, "No se pudo abrir el chat") {
            onReady(container.chatRepository.openRoom(me, uid, name))
        }
}

// ─────────────────────────────────────────────────────────────────────
//  Conversación
// ─────────────────────────────────────────────────────────────────────

class ChatRoomViewModel(
    private val container: AppContainer,
    private val roomId: String,
    private val me: UserProfile
) : ViewModel() {

    private val chat = container.chatRepository

    val room: StateFlow<ChatRoom?> =
        chat.observeRoom(roomId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val messages: StateFlow<List<ChatMessage>?> =
        chat.observeMessages(roomId).map<List<ChatMessage>, List<ChatMessage>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _errors = MutableSharedFlow<String>()
    val errors: SharedFlow<String> = _errors

    /** Hay una foto o audio subiéndose. */
    private val _uploading = MutableStateFlow(false)
    val uploading: StateFlow<Boolean> = _uploading.asStateFlow()

    // ── Envío ─────────────────────────────────────────────────────────
    fun sendText(text: String) {
        if (text.isBlank()) return
        send(OutgoingMessage(MessageType.TEXT, text = text))
    }

    fun sendGiphy(item: GiphyItem, kind: GiphyRepository.Kind) = send(
        OutgoingMessage(
            type = if (kind == GiphyRepository.Kind.STICKERS) MessageType.STICKER else MessageType.GIF,
            text = item.title,
            gifUrl = item.url
        )
    )

    fun sendImage(uri: Uri) = upload("No se pudo enviar la foto") {
        val id = container.mediaRepository.uploadImage(me.uid, uri)
        chat.send(roomId, me, OutgoingMessage(MessageType.IMAGE, mediaId = id))
    }

    fun sendAudio(file: File, durationMs: Long) = upload("No se pudo enviar la nota de voz") {
        val id = container.mediaRepository.uploadAudio(me.uid, file)
        file.delete()
        chat.send(roomId, me, OutgoingMessage(MessageType.AUDIO, mediaId = id, durationMs = durationMs))
    }

    private fun send(message: OutgoingMessage) =
        launchSafely(_errors, "No se pudo enviar el mensaje") { chat.send(roomId, me, message) }

    private fun upload(error: String, block: suspend () -> Unit): Job = viewModelScope.launch {
        _uploading.value = true
        runCatching { block() }.onFailure { _errors.emit(error) }
        _uploading.value = false
    }

    // ── Reacciones y borrado ─────────────────────────────────────────
    fun react(message: ChatMessage, emoji: String) {
        val current = message.reactions[me.uid]
        launchSafely(_errors, "No se pudo reaccionar") {
            chat.react(roomId, message.id, me.uid, if (current == emoji) null else emoji)
        }
    }

    fun delete(message: ChatMessage) = launchSafely(_errors, "No se pudo borrar el mensaje") {
        chat.deleteMessage(roomId, message.id)
        if (message.mediaId.isNotBlank()) container.mediaRepository.delete(message.mediaId)
    }

    fun saveToGallery(mediaId: String) = viewModelScope.launch {
        _errors.emit(if (container.mediaRepository.saveToGallery(mediaId)) "Foto guardada en la galería" else "No se pudo guardar la foto")
    }

    // ── Notas de voz ─────────────────────────────────────────────────
    private var recorder: AudioRecorder? = null
    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    fun startRecording(context: Context) {
        val r = AudioRecorder(context.applicationContext)
        if (r.start()) {
            recorder = r
            _recording.value = true
        } else {
            viewModelScope.launch { _errors.emit("No se pudo usar el micrófono") }
        }
    }

    fun recordingElapsed(): Long = recorder?.elapsedMs() ?: 0L

    fun stopAndSend() {
        val result = recorder?.stop()
        recorder = null
        _recording.value = false
        if (result == null) viewModelScope.launch { _errors.emit("Mantén la grabación al menos un segundo") }
        else sendAudio(result.first, result.second)
    }

    fun cancelRecording() {
        recorder?.cancel()
        recorder = null
        _recording.value = false
    }

    override fun onCleared() {
        recorder?.cancel()
    }
}

// ─────────────────────────────────────────────────────────────────────
//  Buscador de GIPHY
// ─────────────────────────────────────────────────────────────────────

class GiphyViewModel(private val giphy: GiphyRepository) : ViewModel() {

    private val _results = MutableStateFlow<List<GiphyItem>>(emptyList())
    val results: StateFlow<List<GiphyItem>> = _results.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var job: Job? = null

    fun load(kind: GiphyRepository.Kind, query: String) {
        job?.cancel()
        job = viewModelScope.launch {
            if (query.isNotBlank()) kotlinx.coroutines.delay(350) // espera a que termine de escribir
            _loading.value = true
            val result = giphy.search(kind, query)
            _results.value = result.getOrDefault(emptyList())
            _error.value = result.exceptionOrNull()?.let { "No se pudo conectar con GIPHY" }
            _loading.value = false
        }
    }
}
