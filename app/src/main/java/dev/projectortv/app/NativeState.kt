package dev.projectortv.app

import android.content.Context
import dev.projectortv.catalog.BingeBangParser
import dev.projectortv.catalog.NativeLibrary

/** Local state is isolated from BingeBang accounts and cookies. */
class NativeState(context: Context) {
    private val prefs = context.getSharedPreferences("native_library_v2", Context.MODE_PRIVATE)

    private fun list(key: String) = prefs.getString(key, "").orEmpty().split('\n')
        .filter { BingeBangParser.isDetailUrl(it) }.distinct()

    private fun put(key: String, values: List<String>) {
        prefs.edit().putString(key, values.joinToString("\n")).apply()
    }

    fun favorites() = list("favorites")
    fun history() = list("recent")
    fun isFavorite(url: String) = url in favorites()
    fun toggle(url: String) = put("favorites", NativeLibrary.toggle(favorites(), url))
    fun opened(url: String) {
        if (BingeBangParser.isDetailUrl(url)) put("recent", (listOf(url) + history()).distinct().take(40))
    }
    fun demoPosition() = prefs.getLong("demo_position", 0L)
    fun setDemoPosition(ms: Long) = prefs.edit().putLong("demo_position", ms.coerceAtLeast(0L)).apply()
}
