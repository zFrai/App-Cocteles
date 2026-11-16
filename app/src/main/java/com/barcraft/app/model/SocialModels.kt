package com.barcraft.app.model

/** Publicación de la comunidad (`posts/{id}`). */
data class Post(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorRole: Role,
    val text: String,
    val imageUrl: String = "",
    /** Foto subida desde el móvil (colección `media`). */
    val mediaId: String = "",
    val cocktailName: String = "",
    val cocktailSummary: String = "",
    val likes: List<String> = emptyList(),
    val commentCount: Int = 0,
    val createdAt: Long = 0L
) {
    fun isLikedBy(uid: String?): Boolean = uid != null && uid in likes
}

/** Comentario de una publicación (`posts/{id}/comments/{id}`). */
data class Comment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val createdAt: Long
)
