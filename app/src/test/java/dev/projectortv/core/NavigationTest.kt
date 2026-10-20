package dev.projectortv.core

import org.junit.Assert.*
import org.junit.Test

class NavigationTest {
    @Test fun acceptsOnlyExactHttpsOrigin() {
        assertTrue(UrlPolicy.isAllowedUrl("https://bingebang.st/"))
        assertTrue(UrlPolicy.isAllowedUrl("https://bingebang.st/movie/watch/test-123?a=1"))
        assertTrue(UrlPolicy.isAllowedUrl("https://bingebang.st:443/tv"))
        listOf(
            "http://bingebang.st/", "https://evil.test/", "https://bingebang.st.evil.test/",
            "https://evil.test@bingebang.st.evil.test/", "https://bingebang.st:8443/",
            "javascript:alert(1)", "data:text/html,hello", "file:///etc/passwd",
            "https://bingebang.st@evil.test/", "//bingebang.st/", "not a url", ""
        ).forEach { assertFalse("Unexpected acceptance: $it", UrlPolicy.isAllowedUrl(it)) }
    }

    @Test fun bookmarksStripQueriesAndFragments() {
        assertEquals("https://bingebang.st/movie/watch/abc", UrlPolicy.bookmarkUrl("https://bingebang.st/movie/watch/abc?token=private#position"))
        assertNull(UrlPolicy.bookmarkUrl("https://evil.test/item?token=sensitive"))
    }

    @Test fun sectionUrlsRemainAllowed() = Section.entries.forEach {
        assertTrue(it.name, UrlPolicy.isAllowedUrl(it.url))
    }

    @Test fun favoritesAreUniqueOrderedAndCapped() {
        val first = Favorites.add(emptyList(), "https://bingebang.st/one")
        assertEquals(listOf("https://bingebang.st/one"), first)
        assertEquals(first, Favorites.add(first, "https://bingebang.st/one"))
        assertEquals(first, Favorites.add(first, "https://evil.test/"))
        val many = (1..60).fold(emptyList<String>()) { acc, i ->
            Favorites.add(acc, "https://bingebang.st/item-$i")
        }
        assertEquals(40, many.size)
        assertEquals("https://bingebang.st/item-60", many.first())
        assertEquals("https://bingebang.st/item-21", many.last())
        assertFalse(Favorites.remove(many, many.first()).contains(many.first()))
    }
}
