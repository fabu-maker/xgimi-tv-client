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

}
