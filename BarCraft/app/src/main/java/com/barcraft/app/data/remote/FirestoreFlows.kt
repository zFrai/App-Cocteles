package com.barcraft.app.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

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
