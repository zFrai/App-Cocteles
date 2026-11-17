package com.barcraft.app.model

import java.net.URLEncoder

/**
 * "Dónde encontrarme": redes y local de una persona (campo `links` de `users/{uid}`).
 * Todo es opcional; no todos los usuarios son bartenders. Va separado de la descripción.
 */
data class ProfileLinks(
    /** Usuario de Instagram, sin @. */
    val instagram: String = "",
    /** Usuario de TikTok, sin @. */
    val tiktok: String = "",
    /** Usuario de X, sin @. */
    val x: String = "",
    /** Web propia (con o sin https://). */
    val web: String = "",
    /** Nombre del local donde trabaja o al que va. */
    val placeName: String = "",
    /** Dirección o ciudad del local, para abrirlo en Maps. */
    val placeAddress: String = ""
) {
    val isEmpty: Boolean
        get() = listOf(instagram, tiktok, x, web, placeName, placeAddress).all { it.isBlank() }

    val instagramUrl: String? get() = instagram.takeIf { it.isNotBlank() }?.let { "https://instagram.com/$it" }
    val tiktokUrl: String? get() = tiktok.takeIf { it.isNotBlank() }?.let { "https://www.tiktok.com/@$it" }
    val xUrl: String? get() = x.takeIf { it.isNotBlank() }?.let { "https://x.com/$it" }
    val webUrl: String?
        get() = web.takeIf { it.isNotBlank() }?.let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }

    /** Texto del local: "Bar Pepe · Calle Mayor 3, Madrid". */
    val placeLabel: String get() = listOf(placeName, placeAddress).filter { it.isNotBlank() }.joinToString(" · ")

    val mapsUrl: String?
        get() = placeLabel.takeIf { it.isNotBlank() }?.let {
            val q = URLEncoder.encode(listOf(placeName, placeAddress).filter { s -> s.isNotBlank() }.joinToString(", "), "UTF-8")
            "https://www.google.com/maps/search/?api=1&query=$q"
        }

    /** Limpia espacios, quita la @ de los usuarios y recorta cada campo. */
    fun cleaned(): ProfileLinks = ProfileLinks(
        instagram = handle(instagram),
        tiktok = handle(tiktok),
        x = handle(x),
        web = web.trim().take(MAX_FIELD),
        placeName = placeName.trim().take(MAX_FIELD),
        placeAddress = placeAddress.trim().take(MAX_FIELD)
    )

    fun toMap(): Map<String, String> = mapOf(
        "instagram" to instagram,
        "tiktok" to tiktok,
        "x" to x,
        "web" to web,
        "placeName" to placeName,
        "placeAddress" to placeAddress
    )

    companion object {
        const val MAX_FIELD = 100

        fun fromMap(map: Map<String, String>): ProfileLinks = ProfileLinks(
            instagram = map["instagram"].orEmpty(),
            tiktok = map["tiktok"].orEmpty(),
            x = map["x"].orEmpty(),
            web = map["web"].orEmpty(),
            placeName = map["placeName"].orEmpty(),
            placeAddress = map["placeAddress"].orEmpty()
        )

        private fun handle(s: String): String =
            s.trim().removePrefix("@").replace(" ", "").take(MAX_FIELD)
    }
}
