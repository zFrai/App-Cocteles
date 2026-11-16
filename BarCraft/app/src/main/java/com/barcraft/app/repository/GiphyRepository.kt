package com.barcraft.app.repository

import com.barcraft.app.model.GiphyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * GIFs y stickers de GIPHY. La clave se lee de `secrets.properties` (GIPHY_API_KEY).
 * GIPHY pide mostrar "Powered by GIPHY" donde se usan sus resultados.
 */
class GiphyRepository(private val apiKey: String) {

    enum class Kind(val path: String) { GIFS("gifs"), STICKERS("stickers") }

    val isConfigured: Boolean get() = apiKey.isNotBlank()

    suspend fun search(kind: Kind, query: String, limit: Int = 24): Result<List<GiphyItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(isConfigured) { "Falta la clave de GIPHY en secrets.properties" }
                val q = query.trim()
                val endpoint = if (q.isBlank()) "trending" else "search"
                val url = buildString {
                    append("https://api.giphy.com/v1/${kind.path}/$endpoint")
                    append("?api_key=$apiKey&limit=$limit&rating=pg-13&lang=es")
                    if (q.isNotBlank()) append("&q=").append(URLEncoder.encode(q, "UTF-8"))
                }
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }
                try {
                    check(conn.responseCode == 200) { "GIPHY respondió ${conn.responseCode}" }
                    parse(conn.inputStream.bufferedReader().use { it.readText() })
                } finally {
                    conn.disconnect()
                }
            }
        }

    private fun parse(json: String): List<GiphyItem> {
        val data = JSONObject(json).optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).mapNotNull { i ->
            val item = data.getJSONObject(i)
            val images = item.optJSONObject("images") ?: return@mapNotNull null
            val preview = images.optJSONObject("fixed_width_small")?.optString("url").orEmpty()
                .ifBlank { images.optJSONObject("fixed_width")?.optString("url").orEmpty() }
            val full = images.optJSONObject("fixed_width")?.optString("url").orEmpty().ifBlank { preview }
            if (preview.isBlank()) null
            else GiphyItem(item.optString("id"), item.optString("title"), preview, full)
        }
    }
}
