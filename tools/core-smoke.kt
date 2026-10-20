package localtests

import dev.projectortv.core.UrlPolicy
import dev.projectortv.core.Favorites
import dev.projectortv.core.Section

fun main() {
    val blocked = listOf(
        "http://bingebang.st", "https://bingebang.st.attack.test/path",
        "https://bingebang.st@evil.test", "javascript:alert(1)",
        "file:///data", "https://bingebang.st:1234", "https://evil.test",
        "https://bingebang.st\\@evil.test", "https://bingebang.st" + "/".repeat(4100)
    )
    val allowed = listOf("https://bingebang.st/", "https://bingebang.st/tv", "https://bingebang.st:443/movies")
    allowed.forEach { check(UrlPolicy.isAllowedUrl(it)) { "Reject valid URL: $it" } }
    blocked.forEach { check(!UrlPolicy.isAllowedUrl(it)) { "Accept unsafe URL: $it" } }
    Section.entries.forEach { check(UrlPolicy.isAllowedUrl(it.url)) }
    check(UrlPolicy.bookmarkUrl("https://bingebang.st/movie/foo?session=hidden#secret") == "https://bingebang.st/movie/foo")
    val list = (1..90).fold(emptyList<String>()) { acc, i -> Favorites.add(acc, "https://bingebang.st/show-$i") }
    check(list.size == 40)
    check(list.first().endsWith("show-90"))
    check(list.last().endsWith("show-51"))
    check(Favorites.add(list, "https://attacker.test") == list)
    check(Favorites.remove(list, list.first()).size == 39)
    println("PASS: 3 allowed URLs, ${blocked.size} blocked URLs, ${Section.entries.size} section URLs, query/fragment stripping, favorites add/remove/bounds")
}
