package dev.projectortv.catalog

/** A public catalogue entry, with no assertion of stream availability or distribution rights. */
data class MediaItem(
    val url: String,
    val title: String,
    val kind: MediaKind,
    val posterUrl: String? = null,
    val year: String? = null,
    val overview: String? = null
)

enum class MediaKind { MOVIE, SERIES }

enum class CatalogShelf(val title: String, val path: String) {
    HOME("Entdecken", "/"),
    MOVIES("Filme", "/movies"),
    SERIES("Serien", "/tv")
}
