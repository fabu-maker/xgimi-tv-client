package dev.projectortv.catalog

import org.jsoup.Jsoup
import java.io.IOException
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/** Bounded one-page requests. No account handling, no token reuse, no stream extraction. */
class CatalogRepository {
    private val cache = ConcurrentHashMap<CatalogShelf, List<MediaItem>>()

    fun shelf(section: CatalogShelf, refresh: Boolean = false): List<MediaItem> {
        if (!refresh) cache[section]?.let { return it }
        val items = BingeBangParser.parseShelf(fetch(BingeBangParser.SITE + section.path))
        if (items.isEmpty()) throw IOException("Keine Katalogeinträge im öffentlichen HTML gefunden. Seitenstruktur geändert?")
        cache[section] = items
        return items
    }

    fun detail(item: MediaItem): MediaItem {
        if (!BingeBangParser.isDetailUrl(item.url)) throw IOException("Ungültiger Katalogeintrag")
        val updated = BingeBangParser.parseDetail(fetch(item.url), item.url)
        return if (updated != null) updated.copy(posterUrl = updated.posterUrl ?: item.posterUrl,
            year = updated.year ?: item.year) else item
    }

    private fun fetch(url: String): String {
        val uri = BingeBangParser.validSiteUri(url) ?: throw IOException("Ungültige Website-Adresse")
        if (uri.path == null || !(uri.path == "/" || uri.path == "/movies" || uri.path == "/tv" ||
                    BingeBangParser.isDetailUrl(url))) throw IOException("Unbekannter Katalogpfad")
        return Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (compatible; ProjektorTV-Native/0.2)")
            .header("Accept", "text/html")
            .timeout(15000)
            .maxBodySize(1_800_000)
            .followRedirects(false)
            .get().html()
    }
}
