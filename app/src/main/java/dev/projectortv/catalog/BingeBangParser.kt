package dev.projectortv.catalog

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI

/** Parses publicly visible catalogue HTML; does not invoke private APIs or extract media URLs. */
object BingeBangParser {
    const val SITE = "https://bingebang.st"
    private const val MAX_ITEMS = 80
    private val year = Regex("\\b(?:19|20)\\d{2}\\b")

    fun isDetailUrl(value: String?): Boolean {
        val uri = validSiteUri(value) ?: return false
        val path = uri.path.orEmpty()
        return path.startsWith("/movie/watch/") || path.startsWith("/tv/watch/")
    }

    fun validSiteUri(value: String?): URI? = try {
        val uri = URI(value ?: "")
        if (uri.scheme != "https" || !uri.host.equals("bingebang.st", true) ||
            uri.port !in listOf(-1, 443) || uri.userInfo != null || uri.query != null ||
            uri.fragment != null || uri.toString().length > 1500
        ) null else uri
    } catch (_: Exception) { null }

    fun parseShelf(html: String, base: String = SITE): List<MediaItem> {
        val doc = Jsoup.parse(html, base)
        val results = linkedMapOf<String, MediaItem>()
        for (link in doc.select("a[href]")) {
            val uri = link.absUrl("href")
            if (!isDetailUrl(uri)) continue
            val type = if (URI(uri).path.startsWith("/movie/")) MediaKind.MOVIE else MediaKind.SERIES
            val title = extractTitle(link) ?: continue
            val poster = posterUrl(link.selectFirst("img"))
            // An entry's URL uniquely identifies its title; repeated cards are deduplicated.
            val item = MediaItem(uri, title, type, poster, year.find(title)?.value)
            val old = results[uri]
            if (old == null || (old.posterUrl == null && poster != null)) results[uri] = item
            if (results.size >= MAX_ITEMS) break
        }
        return results.values.toList()
    }

    fun parseDetail(html: String, url: String): MediaItem? {
        if (!isDetailUrl(url)) return null
        val doc = Jsoup.parse(html, SITE)
        val heading = doc.selectFirst("h1")?.text()?.trim()?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.takeIf { it.isNotBlank() }
            ?: return null
        val description = doc.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: doc.selectFirst("main p")?.text()?.trim()?.takeIf { it.isNotBlank() }
        val poster = posterUrl(doc.select("img[alt]").firstOrNull { it.attr("alt") == heading })
            ?: posterUrl(doc.selectFirst("meta[property=og:image]"))
        return MediaItem(url, heading.take(150),
            if (URI(url).path.startsWith("/movie/")) MediaKind.MOVIE else MediaKind.SERIES,
            poster, year.find(doc.title())?.value, description?.take(1500))
    }

    private fun extractTitle(anchor: Element): String? {
        val fromImage = anchor.selectFirst("img[alt]")?.attr("alt")
        val candidates = listOf(anchor.attr("aria-label"), anchor.attr("title"), fromImage, anchor.text())
        return candidates.firstOrNull { !it.isNullOrBlank() && it.length in 2..150 }
            ?.trim()?.replace(Regex("\\s+"), " ")
    }

    fun posterUrl(element: Element?): String? {
        if (element == null) return null
        val candidates = listOf(element.attr("data-src"), element.attr("src"), element.attr("content"))
        return candidates.firstNotNullOfOrNull { posterUrlFromRaw(it) }
    }

    fun posterUrlFromRaw(raw: String): String? {
        val candidate = if (raw.startsWith("//")) "https:$raw" else raw
        return try {
            val uri = URI(candidate)
            if (uri.scheme == "https" && uri.host in setOf("image.tmdb.org", "bingebang.st") &&
                uri.userInfo == null && uri.port in listOf(-1, 443) && uri.toString().length < 1800
            ) uri.toString() else null
        } catch (_: Exception) { null }
    }
}
