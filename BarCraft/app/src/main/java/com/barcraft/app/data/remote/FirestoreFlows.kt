package com.barcraft.app.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch

/** Escucha en tiempo real una consulta de Firestore como Flow. */
fun Query.snapshotFlow(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.w("BarCraft", "Firestore: ${error.message}")
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/**
 * Como [snapshotFlow], pero si Firestore rechaza la consulta (falta un índice, las reglas no la
 * permiten...) el Flow termina con ese error en vez de quedarse esperando para siempre.
 * Úsalo con [orOnProblem] para enseñar el error en pantalla.
 */
fun Query.snapshotFlowOrFail(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            // En Logcat (filtro BarCraft) sale el mensaje completo, con el enlace para crear el índice si falta.
            Log.e("BarCraft", "Firestore: ${error.code}: ${error.message}")
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/** Por qué no se ha podido cargar algo de Firestore. */
data class LoadProblem(val kind: Kind, val detail: String, val indexUrl: String? = null) {
    enum class Kind { MISSING_INDEX, PERMISSION_DENIED, OTHER }

    companion object {
        private val CONSOLE_URL = Regex("""https://console\.firebase\.google\.com\S+""")

        fun from(error: Throwable): LoadProblem {
            val message = error.message.orEmpty()
            val code = (error as? FirebaseFirestoreException)?.code
            return when (code) {
                FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                    LoadProblem(Kind.MISSING_INDEX, message, CONSOLE_URL.find(message)?.value?.trimEnd('.', ')'))
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> LoadProblem(Kind.PERMISSION_DENIED, message)
                else -> LoadProblem(Kind.OTHER, message)
            }
        }
    }
}

/** Si el Flow falla, avisa con [onProblem] y emite [fallback] para que la pantalla deje de cargar. */
fun <T> Flow<T>.orOnProblem(fallback: T, onProblem: (LoadProblem) -> Unit): Flow<T> = catch { e ->
    onProblem(LoadProblem.from(e))
    emit(fallback)
}

/** Escucha en tiempo real un documento de Firestore como Flow. */
fun DocumentReference.snapshotFlow(): Flow<DocumentSnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            Log.w("BarCraft", "Firestore: ${error.message}")
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registration.remove() }
}

/** Emite el uid del usuario con sesión iniciada, o null. */
fun FirebaseAuth.uidFlow(): Flow<String?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}
