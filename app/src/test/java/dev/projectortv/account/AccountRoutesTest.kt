package dev.projectortv.account

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRoutesTest {
    @Test fun acceptsOnlyRealFirstPartyHttps() {
        listOf(
            "https://bingebang.st/my-space",
            "https://bingebang.st/signup",
            "https://bingebang.st/reset?token=example",
            "https://bingebang.st:443/my-space#watchlist"
        ).forEach { assertTrue(it, AccountRoutes.allowedMainFrame(it)) }
        listOf(
            "http://bingebang.st/signup",
            "https://bingebang.st.evil.com/",
            "https://bingebang.st@evil.com/my-space",
            "https://evil.com@bingebang.st/my-space",
            "https://bingebang.st:444/my-space",
            "file:///data/user/0/app/",
            "javascript:alert(1)",
            ""
        ).forEach { assertFalse(it, AccountRoutes.allowedMainFrame(it)) }
    }
}