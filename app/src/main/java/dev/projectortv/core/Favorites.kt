package dev.projectortv.core

/** Deterministic favorite list with bounded storage and strict main-frame URL filtering. */
object Favorites {
    const val MAX_ITEMS = 40

    fun add(existing: List<String>, candidate: String): List<String> {
        val clean = UrlPolicy.bookmarkUrl(candidate) ?: return existing.mapNotNull(UrlPolicy::bookmarkUrl).distinct().take(MAX_ITEMS)
        return (listOf(clean) + existing.mapNotNull(UrlPolicy::bookmarkUrl).filter { it != clean })
            .distinct().take(MAX_ITEMS)
    }

    fun remove(existing: List<String>, candidate: String): List<String> =
        existing.mapNotNull(UrlPolicy::bookmarkUrl)
            .filter { it != UrlPolicy.bookmarkUrl(candidate) }.distinct().take(MAX_ITEMS)
}
