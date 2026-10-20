package dev.projectortv.catalog

/** Pure favourite-list and search rules, suitable for JVM tests. */
object NativeLibrary {
    fun toggle(items: List<String>, url: String, limit: Int = 100): List<String> {
        if (!BingeBangParser.isDetailUrl(url)) return items
        return if (url in items) items.filterNot { it == url }
        else (listOf(url) + items).distinct().take(limit)
    }

    fun search(all: List<MediaItem>, query: String): List<MediaItem> {
        val clean = query.trim()
        if (clean.isEmpty()) return all.distinctBy { it.url }
        return all.distinctBy { it.url }.filter { it.title.contains(clean, ignoreCase = true) }
    }
}
