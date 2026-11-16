package com.barcraft.app.repository

import com.barcraft.app.data.remote.Collections
import com.barcraft.app.data.remote.snapshotFlow
import com.barcraft.app.data.remote.toReport
import com.barcraft.app.model.ChatMessage
import com.barcraft.app.model.Report
import com.barcraft.app.model.ReportReason
import com.barcraft.app.model.ReportStatus
import com.barcraft.app.model.UserProfile
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Denuncias entre usuarios (`reports/{id}`).
 * Cualquiera puede denunciar a la otra persona de un chat suyo; solo el Admin las lee y las gestiona
 * (lo garantizan las reglas de Firestore).
 */
class ReportRepository(private val db: FirebaseFirestore) {

    private val reports get() = db.collection(Collections.REPORTS)

    suspend fun report(
        me: UserProfile,
        reportedId: String,
        reportedName: String,
        roomId: String,
        reason: ReportReason,
        details: String,
        message: ChatMessage?
    ) {
        reports.add(
            mapOf(
                "reporterId" to me.uid,
                "reporterName" to me.name,
                "reportedId" to reportedId,
                "reportedName" to reportedName,
                "roomId" to roomId,
                "reason" to reason.id,
                "details" to details.trim().take(500),
                "messageId" to (message?.id ?: ""),
                "messageText" to (message?.preview ?: "").take(2000),
                "status" to ReportStatus.OPEN.id,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Denuncias pendientes, las más recientes primero (solo Admin). */
    fun observeOpen(): Flow<List<Report>> =
        reports.whereEqualTo("status", ReportStatus.OPEN.id).snapshotFlow()
            .map { snap -> snap.documents.map { it.toReport() }.sortedByDescending { it.createdAt } }

    /** Marca la denuncia como revisada o descartada (solo Admin). */
    suspend fun close(reportId: String, adminUid: String, status: ReportStatus) {
        require(status != ReportStatus.OPEN)
        reports.document(reportId).update(
            mapOf(
                "status" to status.id,
                "resolvedBy" to adminUid,
                "resolvedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
