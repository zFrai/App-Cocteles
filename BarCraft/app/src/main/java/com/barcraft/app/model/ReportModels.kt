package com.barcraft.app.model

/** Motivo de una denuncia. Se guarda como texto en Firestore (campo `reason`). */
enum class ReportReason(val id: String, val label: String) {
    SPAM("spam", "Spam o publicidad"),
    HARASSMENT("harassment", "Acoso o insultos"),
    INAPPROPRIATE("inappropriate", "Contenido inapropiado"),
    IMPERSONATION("impersonation", "Se hace pasar por otra persona"),
    OTHER("other", "Otro motivo");

    companion object {
        fun from(id: String?): ReportReason = entries.firstOrNull { it.id == id } ?: OTHER
    }
}

enum class ReportStatus(val id: String) {
    OPEN("open"), RESOLVED("resolved"), DISMISSED("dismissed");

    companion object {
        fun from(id: String?): ReportStatus = entries.firstOrNull { it.id == id } ?: OPEN
    }
}

/**
 * Denuncia de un usuario a otro desde un chat (`reports/{id}`).
 * Solo la ve el Admin, en la pestaña "Denuncias" de su panel.
 */
data class Report(
    val id: String,
    val reporterId: String,
    val reporterName: String,
    val reportedId: String,
    val reportedName: String,
    val roomId: String,
    val reason: ReportReason,
    val details: String = "",
    /** Mensaje concreto denunciado (opcional). Se guarda una copia por si el autor lo borra. */
    val messageId: String = "",
    val messageText: String = "",
    val status: ReportStatus = ReportStatus.OPEN,
    val createdAt: Long = 0L
)
