package dev.projectortv.account

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import java.net.URI
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.app.AlertDialog
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Only first-party official pages may be used for account entry and navigation. */
object AccountRoutes {
    const val HOME = "https://bingebang.st/my-space"
    const val SIGN_UP = "https://bingebang.st/signup"
    const val RESET = "https://bingebang.st/reset"
    fun allowedMainFrame(url: String?): Boolean = try {
        val uri = URI(url ?: "")
        uri.scheme.equals("https", true) &&
            uri.host.equals("bingebang.st", true) &&
            (uri.port == -1 || uri.port == 443) &&
            uri.userInfo == null && uri.toString().length < 2048
    } catch (_: Exception) { false }
}

/**
 * First-party site-controlled login/registration.
 * No credentials are read, recorded or transmitted by application code.
 * Session cookies stay in Android WebView's standard CookieManager and are
 * available to the separately displayed first-party video playback page.
 */
class AccountSession(
    private val activity: Activity,
    private val host: FrameLayout,
    private val onExit: () -> Unit
) {
    private var web: WebView? = null
    private var title: TextView? = null
    private var active = false

    @SuppressLint("SetJavaScriptEnabled")
    fun start(initialUrl: String = AccountRoutes.HOME) {
        require(AccountRoutes.allowedMainFrame(initialUrl))
        close()
        active = true
        host.removeAllViews()
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(9, 15, 25))
        }
        host.addView(layout, FrameLayout.LayoutParams(-1, -1))
        val bar = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 8, 10, 8)
        }
        fun control(text: String, weight: Float = 1f, click: () -> Unit): Button =
            Button(activity).apply {
                this.text = text
                isAllCaps = false
                textSize = 14f
                minHeight = 55
                setOnClickListener { click() }
                bar.addView(this, LinearLayout.LayoutParams(0, 60, weight))
            }
        control("◀ TV-App", 1f) { onExit() }
        control("Mein Konto", 1.2f) { web?.loadUrl(AccountRoutes.HOME) }
        control("Registrieren", 1.2f) { web?.loadUrl(AccountRoutes.SIGN_UP) }
        control("Passwort vergessen", 1.4f) { web?.loadUrl(AccountRoutes.RESET) }
        control("↻", .45f) { web?.reload() }
        control("Abmelden", 1.0f) {
            AlertDialog.Builder(activity)
                .setTitle("Anmeldesitzung auf diesem Gerät löschen?")
                .setMessage("Dies entfernt die lokale Anmeldung. Andere Geräte bleiben angemeldet.")
                .setNegativeButton("Abbrechen", null)
                .setPositiveButton("Abmelden") { _, _ ->
                    logOutBrowserSession { web?.loadUrl(AccountRoutes.HOME) }
                }.show()
        }
        control("Seite bedienen", 1.2f) { web?.requestFocus() }
        layout.addView(bar)
        title = TextView(activity).apply {
            text = "BingeBang-Konto · offizielle Website bingebang.st"
            setTextColor(Color.LTGRAY)
            textSize = 14f
            setPadding(16, 4, 16, 8)
        }
        layout.addView(title)
        val browser = WebView(activity).apply {
            setBackgroundColor(Color.rgb(13, 20, 29))
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                safeBrowsingEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    return !AccountRoutes.allowedMainFrame(request.url.toString())
                }
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
                    !AccountRoutes.allowedMainFrame(url)

                override fun onPageFinished(view: WebView, url: String) {
                    if (active) title?.text = "BingeBang-Konto · bingebang.st · " + (view.title ?: "Mein Konto")
                    CookieManager.getInstance().flush()
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (active && request.isForMainFrame) title?.text = "Ladefehler: " + error.description
                }
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    (view.parent as? ViewGroup)?.removeView(view)
                    view.destroy()
                    if (web === view) web = null
                    if (active) title?.text = "Anmeldeansicht wurde beendet. Bitte zurück zur App."
                    return true
                }
            }
            webChromeClient = WebChromeClient()
        }
        web = browser
        layout.addView(browser, LinearLayout.LayoutParams(-1, 0, 1f))
        bar.getChildAt(0)?.requestFocus()
        browser.loadUrl(initialUrl)
    }

    fun back(): Boolean {
        val current = web ?: return false
        if (!current.canGoBack()) return false
        current.goBack()
        return true
    }

    fun onPause() {
        web?.onPause()
        CookieManager.getInstance().flush()
    }
    fun onResume() { web?.onResume() }

    fun close() {
        active = false
        val current = web ?: return
        web = null
        current.stopLoading()
        current.onPause()
        (current.parent as? ViewGroup)?.removeView(current)
        current.loadUrl("about:blank")
        current.destroy()
        title = null
        CookieManager.getInstance().flush()
    }

    fun logOutBrowserSession(onFinished: () -> Unit) {
        // Clearing local cookies is not equivalent to server-side revocation.
        CookieManager.getInstance().removeAllCookies {
            WebStorage.getInstance().deleteAllData()
            CookieManager.getInstance().flush()
            onFinished()
        }
    }
}
