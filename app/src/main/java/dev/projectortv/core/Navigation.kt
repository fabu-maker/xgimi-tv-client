package dev.projectortv.core

import java.net.URI

/** Pure URL policy. Browser main frames must stay on the explicitly configured origin. */
object UrlPolicy {
    const val ORIGIN = "https://bingebang.st"

    fun isAllowedUrl(raw: String?): Boolean = try {
        if (raw.isNullOrBlank() || raw.length > 4096) false
        else {
            val uri = URI(raw)
            uri.scheme.equals("https", ignoreCase = true) &&
                uri.host.equals("bingebang.st", ignoreCase = true) &&
                uri.userInfo == null &&
                (uri.port == -1 || uri.port == 443) &&
                uri.fragment?.length?.let { it <= 1024 } != false
        }
    } catch (_: Exception) { false }

    /** Drop queries/fragments before persisting: URLs can contain credentials or session tokens. */
    fun bookmarkUrl(raw: String?): String? {
        if (!isAllowedUrl(raw)) return null
        return try {
            val path = URI(raw).rawPath?.takeIf { it.startsWith("/") && it.isNotEmpty() } ?: "/"
            ORIGIN + path
        } catch (_: Exception) { null }
    }
}

/** Routes verified by website inspection, not invented API endpoints. */
enum class Section(val label: String, val relativePath: String, val description: String) {
    START("Start", "/", "Startseite der Website"),
    MOVIES("Filme", "/movies", "Filmkatalog"),
    SERIES("Serien", "/tv", "Serienkatalog"),
    EXPLORE("Entdecken", "/explore", "Genres und Filter"),
    CALENDAR("Kalender", "/tv/calendar", "Neue Serienfolgen"),
    HELP("Hilfe", "/help", "Bedienung und Playerfunktionen");

    val url: String get() = UrlPolicy.ORIGIN + relativePath
}
