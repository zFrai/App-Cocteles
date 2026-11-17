package com.barcraft.app.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.barcraft.app.AppContainer
import com.barcraft.app.R
import com.barcraft.app.model.ChatMessage
import com.barcraft.app.model.ChatRoom
import com.barcraft.app.model.Follow
import com.barcraft.app.model.FollowState
import com.barcraft.app.model.GiphyItem
import com.barcraft.app.model.MessageType
import com.barcraft.app.model.PrivateSettings
import com.barcraft.app.model.ReportReason
import com.barcraft.app.model.UserProfile
import com.barcraft.app.repository.GiphyRepository
import com.barcraft.app.repository.OutgoingMessage
import com.barcraft.app.ui.social.launchSafely
import com.barcraft.app.utils.AudioRecorder
import com.barcraft.app.utils.Txt
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
//  Personas: seguidores, seguidos, solicitudes, favoritos y bloqueados
// ─────────────────────────────────────────────────────────────────────

/** Otra persona vista desde mi cuenta. */
data class Person(
    val profile: UserProfile,
    /** Si yo la sigo (o se lo he pedido). */
    val iFollow: FollowState,
    /** Si ella me sigue (o me lo ha pedido). */
    val followsMe: FollowState,
    val favorite: Boolean,
    val blocked: Boolean
) {
    val mutual: Boolean get() = iFollow == FollowState.FOLLOWING && followsMe == FollowState.FOLLOWING

    /** Se puede chatear si os seguís mutuamente o si su cuenta es pública. Debe coincidir con firestore.rules. */
    val canChat: Boolean get() = !blocked && (mutual || !profile.privateAccount)
}

data class PeopleState(
    val loaded: Boolean = false,
    val everyone: List<Person> = emptyList(),
    val settings: PrivateSettings = PrivateSettings()
) {
    private val visible get() = everyone.filter { !it.blocked }
    private val byFavThenName = compareByDescending<Person> { it.favorite }.thenBy { it.profile.name.lowercase() }

    val followers get() = visible.filter { it.followsMe == FollowState.FOLLOWING }.sortedWith(byFavThenName)
    val following get() = visible.filter { it.iFollow == FollowState.FOLLOWING }.sortedWith(byFavThenName)
    /** Quieren seguirme (mi cuenta es privada). */
    val incoming get() = visible.filter { it.followsMe == FollowState.REQUESTED }
    /** He pedido seguir a cuentas privadas. */
    val outgoing get() = visible.filter { it.iFollow == FollowState.REQUESTED }
    val blocked get() = everyone.filter { it.blocked }
    fun byUid(uid: String) = everyone.firstOrNull { it.profile.uid == uid }
}

private fun Follow?.state(): FollowState = when {
    this == null -> FollowState.NONE
    accepted -> FollowState.FOLLOWING
    else -> FollowState.REQUESTED
}

/** Compartido por las pantallas de chat: lista, personas y conversación. */
class PeopleViewModel(private val container: AppContainer, private val me: UserProfile) : ViewModel() {

    private val friends = container.friendsRepository
    private var migrationStarted = false

    val state: StateFlow<PeopleState> =
        combine(
            container.userRepository.observeAll(),
            friends.observeFollowing(me.uid),
            friends.observeFollowers(me.uid),
            friends.observeSettings(me.uid)
        ) { users, following, followers, settings ->
            if (!settings.followsMigrated) migrateOnce(users)
            val iFollow = following.associateBy { it.followedId }
            val followsMe = followers.associateBy { it.followerId }
            PeopleState(
                loaded = true,
                settings = settings,
                everyone = users.filter { it.uid != me.uid }.map { u ->
                    Person(
                        profile = u,
                        iFollow = iFollow[u.uid].state(),
                        followsMe = followsMe[u.uid].state(),
                        favorite = u.uid in settings.favorites,
                        blocked = u.uid in settings.blocked
                    )
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PeopleState())

    /** Las amistades del sistema anterior pasan a ser seguimientos (una vez por cuenta). */
    private fun migrateOnce(users: List<UserProfile>) {
        if (migrationStarted) return
        migrationStarted = true
        viewModelScope.launch {
            runCatching { friends.migrateFriendships(me, users.associateBy { it.uid }) }
                .onFailure { migrationStarted = false }
        }
    }

    val rooms: StateFlow<List<ChatRoom>?> =
        container.chatRepository.observeRooms(me.uid).map<List<ChatRoom>, List<ChatRoom>?> { it }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    fun follow(p: Person) = launchSafely(_messages, Txt.get(R.string.msg_follow_failed)) {
        friends.follow(me, p.profile)
        _messages.emit(
            if (p.profile.privateAccount) Txt.get(R.string.msg_request_sent, p.profile.name)
            else Txt.get(R.string.msg_now_following, p.profile.name)
        )
    }

    /** QR escaneado en Buscar: sigue a esa cuenta, o le envía solicitud si es privada. */
    fun followFromQr(uid: String) {
        val p = state.value.byUid(uid)
        val notice = when {
            uid == me.uid -> Txt.get(R.string.msg_qr_own)
            p == null -> Txt.get(R.string.msg_qr_not_found)
            p.blocked -> Txt.get(R.string.chat_you_blocked, p.profile.name)
            p.iFollow == FollowState.FOLLOWING -> Txt.get(R.string.msg_already_following, p.profile.name)
            p.iFollow == FollowState.REQUESTED -> Txt.get(R.string.msg_already_requested, p.profile.name)
            else -> null
        }
        if (notice != null) viewModelScope.launch { _messages.emit(notice) } else follow(p!!)
    }

    /** Dejar de seguir o cancelar la solicitud enviada. */
    fun unfollow(p: Person) = launchSafely(_messages, Txt.get(R.string.msg_action_failed)) {
        friends.unfollow(me.uid, p.profile.uid)
    }

    fun acceptFollower(p: Person) = launchSafely(_messages, Txt.get(R.string.msg_accept_failed)) {
        friends.acceptFollower(me.uid, p.profile.uid)
        _messages.emit(Txt.get(R.string.msg_now_follows_you, p.profile.name))
    }

    /** Rechazar una solicitud o quitar a alguien de mis seguidores. */
    fun removeFollower(p: Person) = launchSafely(_messages, Txt.get(R.string.msg_action_failed)) {
        friends.removeFollower(me.uid, p.profile.uid)
    }

    fun toggleFavorite(p: Person) = launchSafely(_messages, Txt.get(R.string.msg_favorite_failed)) {
        friends.setFavorite(me.uid, p.profile.uid, !p.favorite)
    }

    fun block(p: Person) = launchSafely(_messages, Txt.get(R.string.msg_block_failed)) {
        friends.block(me.uid, p.profile.uid)
        _messages.emit(Txt.get(R.string.msg_blocked, p.profile.name))
    }

    fun unblock(uid: String, name: String) = launchSafely(_messages, Txt.get(R.string.msg_unblock_failed)) {
        friends.unblock(me.uid, uid)
        _messages.emit(Txt.get(R.string.msg_unblocked, name))
    }

    /** Abre (o crea) la conversación. Solo con seguimiento mutuo o si su cuenta es pública. */
    fun openChat(p: Person, onReady: (String) -> Unit) {
        if (!p.canChat) {
            viewModelScope.launch {
                _messages.emit(Txt.get(R.string.msg_private_cannot_chat, p.profile.name))
            }
            return
        }
        launchSafely(_messages, Txt.get(R.string.msg_open_chat_failed)) {
            onReady(container.chatRepository.openRoom(me, p.profile.uid, p.profile.name))
        }
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

    fun sendImage(uri: Uri) = upload(Txt.get(R.string.msg_send_photo_failed)) {
        val id = container.mediaRepository.uploadImage(me.uid, uri)
        chat.send(roomId, me, OutgoingMessage(MessageType.IMAGE, mediaId = id))
    }

    fun sendAudio(file: File, durationMs: Long) = upload(Txt.get(R.string.msg_send_voice_failed)) {
        val id = container.mediaRepository.uploadAudio(me.uid, file)
        file.delete()
        chat.send(roomId, me, OutgoingMessage(MessageType.AUDIO, mediaId = id, durationMs = durationMs))
    }

    private fun send(message: OutgoingMessage) =
        launchSafely(_errors, Txt.get(R.string.msg_send_failed)) { chat.send(roomId, me, message) }

    private fun upload(error: String, block: suspend () -> Unit): Job = viewModelScope.launch {
        _uploading.value = true
        runCatching { block() }.onFailure { _errors.emit(error) }
        _uploading.value = false
    }

    // ── Reacciones y borrado ─────────────────────────────────────────
    fun react(message: ChatMessage, emoji: String) {
        val current = message.reactions[me.uid]
        launchSafely(_errors, Txt.get(R.string.msg_react_failed)) {
            chat.react(roomId, message.id, me.uid, if (current == emoji) null else emoji)
        }
    }

    fun delete(message: ChatMessage) = launchSafely(_errors, Txt.get(R.string.msg_delete_message_failed)) {
        chat.deleteMessage(roomId, message.id)
        if (message.mediaId.isNotBlank()) container.mediaRepository.delete(message.mediaId)
    }

    // ── Denuncias ────────────────────────────────────────────────────
    /** Denuncia a la otra persona del chat. [message] es opcional: el mensaje concreto que se denuncia. */
    fun report(reason: ReportReason, details: String, message: ChatMessage?) =
        launchSafely(_errors, Txt.get(R.string.msg_report_failed)) {
            val r = room.value ?: error("Sala no cargada")
            val otherId = r.otherId(me.uid)
            require(otherId != me.uid)
            container.reportRepository.report(me, otherId, r.otherName(me.uid), roomId, reason, details, message)
            _errors.emit(Txt.get(R.string.msg_report_sent))
        }

    fun saveToGallery(mediaId: String) = viewModelScope.launch {
        _errors.emit(if (container.mediaRepository.saveToGallery(mediaId)) Txt.get(R.string.msg_photo_saved) else Txt.get(R.string.msg_photo_save_failed))
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
            viewModelScope.launch { _errors.emit(Txt.get(R.string.msg_mic_failed)) }
        }
    }

    fun recordingElapsed(): Long = recorder?.elapsedMs() ?: 0L

    fun stopAndSend() {
        val result = recorder?.stop()
        recorder = null
        _recording.value = false
        if (result == null) viewModelScope.launch { _errors.emit(Txt.get(R.string.msg_recording_too_short)) }
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
            _error.value = result.exceptionOrNull()?.let { Txt.get(R.string.giphy_error) }
            _loading.value = false
        }
    }
}
