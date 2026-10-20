package dev.projectortv.catalog

import org.junit.Assert.*
import org.junit.Test

class BingeBangParserTest {
    private val sample = """
       <html><body><main>
       <a href="/movie/watch/example-2026-x1"><img alt="Example Film (2026)" src="https://image.tmdb.org/t/p/w500/example.jpg"></a>
       <a href="/movie/watch/example-2026-x1">Example Film (2026)</a>
       <a href="/tv/watch/an-example-show-a1"><img alt="An Example Show" src="https://image.tmdb.org/t/p/w500/show.jpg"></a>
       <a href="https://fake-bingebang.st/movie/watch/fake"><img alt="Fake" src="https://evil.example/x.png"></a>
       <a href="/explore">Explore</a>
       </main></body></html>
    """.trimIndent()

    @Test fun nativeCatalogOnlyCollectsActualWatchLinks() {
        val items = BingeBangParser.parseShelf(sample)
        assertEquals(2, items.size)
        assertEquals(MediaKind.MOVIE, items[0].kind)
        assertEquals(MediaKind.SERIES, items[1].kind)
        assertEquals("Example Film (2026)", items[0].title)
        assertEquals("2026", items[0].year)
        assertNotNull(items[0].posterUrl)
    }

    @Test fun rejectsRedirectsAndHostSpoofing() {
        val invalid = listOf("http://bingebang.st/movie/watch/x", "https://evil.com/movie/watch/x",
            "https://bingebang.st.evil.com/tv/watch/x", "https://bingebang.st@evil.com/tv/watch/x",
            "https://bingebang.st/movie/watch/a?token=secret")
        invalid.forEach { assertFalse(it, BingeBangParser.isDetailUrl(it)) }
    }

    @Test fun detailTitleAndDescription() {
        val result = BingeBangParser.parseDetail("<html><head><meta property='og:description' content='A story.'></head><body><h1>Example</h1></body></html>", "https://bingebang.st/movie/watch/example-1")
        assertNotNull(result)
        assertEquals("Example", result?.title)
        assertEquals("A story.", result?.overview)
    }

    @Test fun searchAndFavouritesAreDeterministic() {
        val entries = BingeBangParser.parseShelf(sample)
        assertEquals(1, NativeLibrary.search(entries, "example film").size)
        val url = entries[0].url
        assertEquals(listOf(url), NativeLibrary.toggle(emptyList(), url))
        assertTrue(NativeLibrary.toggle(listOf(url), url).isEmpty())
    }

    @Test fun rejectOffOriginPosterAndScriptUrls() {
        assertNull(BingeBangParser.posterUrlFromRaw("javascript:alert(1)"))
        assertNull(BingeBangParser.posterUrlFromRaw("https://evil.example/image.jpg"))
        assertNotNull(BingeBangParser.posterUrlFromRaw("https://image.tmdb.org/t/p/w500/a.jpg"))
    }
}
