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

}
