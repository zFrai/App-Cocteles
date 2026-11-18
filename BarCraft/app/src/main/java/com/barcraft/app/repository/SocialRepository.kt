package com.barcraft.app.repository

import com.barcraft.app.data.remote.Collections
import com.barcraft.app.data.remote.snapshotFlow
import com.barcraft.app.data.remote.snapshotFlowOrFail
import com.barcraft.app.data.remote.toComment
import com.barcraft.app.data.remote.toPost
import com.barcraft.app.data.remote.toSavedPost
import com.barcraft.app.model.Comment
import com.barcraft.app.model.Post
import com.barcraft.app.model.SavedPost
import com.barcraft.app.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** Muro de la comunidad: publicaciones, "me gusta" y comentarios. */
class SocialRepository(private val db: FirebaseFirestore) {

    private val posts get() = db.collection(Collections.POSTS)

    fun observeFeed(limit: Long = 100): Flow<List<Post>> =
        posts.orderBy("createdAt", Query.Direction.DESCENDING).limit(limit)
            .snapshotFlowOrFail()
            .map { snap -> snap.documents.map { it.toPost() } }

    fun observePost(postId: String): Flow<Post?> =
        posts.document(postId).snapshotFlow().map { if (it.exists()) it.toPost() else null }

    fun observeComments(postId: String): Flow<List<Comment>> =
        posts.document(postId).collection(Collections.COMMENTS)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshotFlow()
            .map { snap -> snap.documents.map { it.toComment() } }

    suspend fun createPost(
        me: UserProfile,
        text: String,
        imageUrl: String,
        mediaId: String,
        cocktailName: String,
        cocktailSummary: String
    ) {
        posts.add(
            mapOf(
                "authorId" to me.uid,
                "authorName" to me.name,
                "authorRole" to me.role.id,
                "authorTag" to me.tag,
                "text" to text.trim(),
                "imageUrl" to imageUrl.trim(),
                "mediaId" to mediaId,
                "cocktailName" to cocktailName,
                "cocktailSummary" to cocktailSummary,
                "likes" to emptyList<String>(),
                "commentCount" to 0,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Mis publicaciones (sin orderBy para no necesitar índice compuesto; se ordenan aquí). */
    fun observeByAuthor(uid: String): Flow<List<Post>> =
        posts.whereEqualTo("authorId", uid).snapshotFlow()
            .map { snap -> snap.documents.map { it.toPost() }.sortedByDescending { it.createdAt } }

    /** Solo el autor (lo exigen las reglas). Marca editedAt para avisar a quien la tenga guardada. */
    suspend fun editPost(postId: String, text: String) {
        posts.document(postId).update(
            mapOf("text" to text.trim(), "editedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    // ── Guardados (privados de cada uno) ──────────────────────────────
    private fun saved(uid: String) =
        db.collection(Collections.USERS).document(uid).collection(Collections.SAVED)

    fun observeSaved(uid: String): Flow<List<SavedPost>> =
        saved(uid).snapshotFlow().map { snap -> snap.documents.map { it.toSavedPost() }.sortedByDescending { it.savedAt } }

    suspend fun savePost(uid: String, post: Post) {
        saved(uid).document(post.id).set(
            mapOf("savedAt" to FieldValue.serverTimestamp(), "seenEditAt" to post.editedAt)
        ).await()
    }

    suspend fun unsavePost(uid: String, postId: String) {
        saved(uid).document(postId).delete().await()
    }

    /** Ya he visto la última edición: quita el aviso "ha cambiado su post". */
    suspend fun markEditSeen(uid: String, post: Post) {
        saved(uid).document(post.id).set(mapOf("seenEditAt" to post.editedAt), SetOptions.merge()).await()
    }

    suspend fun setLiked(postId: String, uid: String, liked: Boolean) {
        val change = if (liked) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
        posts.document(postId).update("likes", change).await()
    }

    /** El autor o un Admin. */
    suspend fun deletePost(postId: String) {
        posts.document(postId).delete().await()
    }

    suspend fun addComment(postId: String, me: UserProfile, text: String) {
        val post = posts.document(postId)
        val comment = post.collection(Collections.COMMENTS).document()
        db.batch()
            .set(
                comment,
                mapOf(
                    "authorId" to me.uid,
                    "authorName" to me.name,
                    "text" to text.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            .update(post, "commentCount", FieldValue.increment(1))
            .commit()
            .await()
    }

    /** El autor del comentario o un Admin. */
    suspend fun deleteComment(postId: String, commentId: String) {
        val post = posts.document(postId)
        db.batch()
            .delete(post.collection(Collections.COMMENTS).document(commentId))
            .update(post, "commentCount", FieldValue.increment(-1))
            .commit()
            .await()
    }
}
