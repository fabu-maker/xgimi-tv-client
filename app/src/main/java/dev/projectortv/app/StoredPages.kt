package dev.projectortv.app

import android.content.Context
import dev.projectortv.core.Favorites
import dev.projectortv.core.UrlPolicy
import org.json.JSONArray

/** Only first-party URLs are stored. No passwords, cookies, or video links. */
class StoredPages(context: Context) {
    private val prefs = context.getSharedPreferences("local_pages_v1", Context.MODE_PRIVATE)

    fun favorites(): List<String> = read("favorites")
    fun recent(): List<String> = read("recent")

    fun toggleFavorite(url: String): Boolean {
        val clean = UrlPolicy.bookmarkUrl(url) ?: return false
        val current = favorites()
        val added = clean !in current
        write("favorites", if (added) Favorites.add(current, clean) else Favorites.remove(current, clean))
        return added
    }

    fun visit(url: String) {
        if (UrlPolicy.isAllowedUrl(url)) write("recent", Favorites.add(recent(), url).take(12))
    }

    private fun read(key: String): List<String> = try {
        val array = JSONArray(prefs.getString(key, "[]") ?: "[]")
        (0 until array.length()).mapNotNull { UrlPolicy.bookmarkUrl(array.optString(it)) }.distinct()
            .take(Favorites.MAX_ITEMS)
    } catch (_: Exception) { emptyList() }

    private fun write(key: String, items: List<String>) {
        prefs.edit().putString(key, JSONArray(items).toString()).apply()
    }
}
