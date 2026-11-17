package com.barcraft.app.model

/** Publicación de la comunidad (`posts/{id}`). */
data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorRole: Role,
    /** Tag del autor sin @ (vacío en publicaciones antiguas). */
    val authorTag: String = "",
    val text: String,
    val imageUrl: String = "",
    /** Foto subida desde el móvil (colección `media`). */
    val mediaId: String = "",
    val cocktailName: String = "",
    val cocktailSummary: String = "",
    val likes: List<String> = emptyList(),
    val commentCount: Int = 0,
    val createdAt: Long = 0L,
    /** Última vez que su autor la editó (0 = nunca). */
    val editedAt: Long = 0L
) {
    fun isLikedBy(uid: String?): Boolean = uid != null && uid in likes

    /** "@laura #4821", o el nombre si es una publicación antigua sin tag. */
    val authorHandle: String
        get() = UserProfile(uid = authorId, name = authorName, tag = authorTag).handle.ifBlank { authorName }
}

/** Publicación guardada por mí (`users/{uid}/saved/{postId}`). */
data class SavedPost(
    val postId: String,
    val savedAt: Long,
    /** editedAt de la publicación la última vez que la vi; si cambia, su autor la ha editado. */
    val seenEditAt: Long
)

/** Comentario de una publicación (`posts/{id}/comments/{id}`). */
data class Comment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val createdAt: Long
)
