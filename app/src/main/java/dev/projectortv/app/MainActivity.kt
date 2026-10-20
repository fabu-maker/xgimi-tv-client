package dev.projectortv.app

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
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
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import dev.projectortv.core.Section
import dev.projectortv.core.UrlPolicy
import org.json.JSONObject

/**
 * Native, remote-operated Android TV shell. Website DOM and video are isolated in WebView.
 * No media URL extraction, DRM bypass or unofficial API calls.
 */
class MainActivity : Activity() {
    private val surface = Color.rgb(13, 20, 29)
    private val tile = Color.rgb(29, 44, 59)
    private val accent = Color.rgb(122, 213, 219)
    private val white = Color.rgb(242, 248, 249)
    private val muted = Color.rgb(170, 185, 195)

    private lateinit var root: FrameLayout
    private lateinit var pages: StoredPages
    private var browser: WebView? = null
    private var toolbarTitle: TextView? = null
    private var status: TextView? = null
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
    private var isBrowser = false
    private var pendingSearch: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pages = StoredPages(this)
        root = FrameLayout(this).apply { setBackgroundColor(surface) }
        setContentView(root)
        showHome()
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()
    private fun layout(width: Int = ViewGroup.LayoutParams.MATCH_PARENT, height: Int = ViewGroup.LayoutParams.WRAP_CONTENT) =
        LinearLayout.LayoutParams(width, height)

    private fun text(content: String, sp: Float, color: Int = white, heavy: Boolean = false): TextView =
        TextView(this).apply {
            text = content
            textSize = sp
            setTextColor(color)
            if (heavy) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 17f
        setTextColor(white)
        isFocusable = true
        isFocusableInTouchMode = true
        minHeight = dp(56)
        setPadding(dp(18), dp(12), dp(18), dp(12))
        background = buttonBackground(false)
        setOnFocusChangeListener { view, focused ->
            view.background = buttonBackground(focused)
            (view as Button).setTextColor(if (focused) surface else white)
            view.elevation = if (focused) dp(10).toFloat() else 0f
        }
        setOnClickListener { action() }
        contentDescription = label
    }

    private fun buttonBackground(focused: Boolean) = GradientDrawable().apply {
        cornerRadius = dp(12).toFloat()
        setColor(if (focused) accent else tile)
        setStroke(dp(2), if (focused) white else tile)
    }

    private fun header(name: String, description: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(44), dp(29), dp(44), dp(8))
        addView(text(name, 30f, white, true))
        addView(text(description, 16f, muted), layout().apply { topMargin = dp(6) })
    }

    private fun rowOfButtons(parent: LinearLayout, actions: List<Pair<String, () -> Unit>>, autoFocus: Boolean = false) {
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(dp(44), dp(7), dp(44), dp(7))
            clipToPadding = false
        }
        val list = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        var first: Button? = null
        actions.forEach { (label, action) ->
            val control = button(label, action)
            if (first == null) first = control
            list.addView(control, layout().apply {
                rightMargin = dp(12)
                width = dp(178)
            })
        }
        scroll.addView(list)
        parent.addView(scroll)
        if (autoFocus) first?.post { first?.requestFocus() }
    }

    private fun showHome() {
        isBrowser = false
        pendingSearch = null
        clearFullScreen()
        disposeBrowser()
        root.removeAllViews()
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(surface)
        }
        root.addView(main, FrameLayout.LayoutParams(-1, -1))
        main.addView(header("PROJEKTOR TV", "TV-Fernbedienung: Pfeiltasten, OK und Zurück"))
        rowOfButtons(main, Section.entries.take(5).map { section ->
            section.label to { openWebsite(section.url, section.label) }
        } + listOf("Suche" to { showSearch() }, "Hilfe" to { openWebsite(Section.HELP.url, "Hilfe") }), autoFocus = true)

        val scroll = ScrollView(this).apply { isFillViewport = true }
        val feed = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(44), dp(14), dp(44), dp(38))
        }
        feed.addView(text("Direkteinstieg", 25f, white, true))
        feed.addView(text("Native Bedienoberfläche. Die Website wird innerhalb der App geöffnet.", 16f, muted))
        val choices = listOf(
            Section.MOVIES to "Filme ansehen und filtern",
            Section.SERIES to "Serien durchsuchen",
            Section.EXPLORE to "Genres, Bewertungen und Jahrgänge",
            Section.CALENDAR to "Serienkalender"
        )
        rowOfButtons(feed, choices.map { (section, description) ->
            "$description  ›" to { openWebsite(section.url, section.label) }
        })
        addSavedRow(feed, "Favoriten (nur auf diesem Gerät)", pages.favorites())
        addSavedRow(feed, "Zuletzt geöffnet", pages.recent())
        feed.addView(text("Hinweis: Abspielen und die Suche innerhalb der Website sind auf diesem Xgimi noch nicht geprüft.", 15f, muted))
        scroll.addView(feed)
        main.addView(scroll, layout(-1, 0).apply { weight = 1f })
    }

    private fun addSavedRow(parent: LinearLayout, title: String, urls: List<String>) {
        val heading = text(title, 23f, white, true)
        parent.addView(heading, layout().apply { topMargin = dp(25); bottomMargin = dp(10) })
        if (urls.isEmpty()) {
            parent.addView(text("Noch keine Einträge", 15f, muted))
            return
        }
        rowOfButtons(parent, urls.take(10).map { url ->
            val uri = Uri.parse(url)
            (uri.lastPathSegment ?: "Startseite").replace('-', ' ').take(25) to
                { openWebsite(url, "Gespeicherte Seite") }
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun openWebsite(url: String, title: String) {
        if (!UrlPolicy.isAllowedUrl(url)) { message("Unzulässige Adresse blockiert"); return }
        clearFullScreen()
        disposeBrowser()
        isBrowser = true
        root.removeAllViews()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(surface)
        }
        root.addView(container, FrameLayout.LayoutParams(-1, -1))
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
        }
        val home = button("⌂ Start", ::showHome)
        toolbar.addView(home, layout().apply { rightMargin = dp(8) })
        toolbar.addView(button("◀ Zurück") { goBack() }, layout().apply { rightMargin = dp(8) })
        toolbar.addView(button("☆ Merken") { saveCurrentPage() }, layout().apply { rightMargin = dp(8) })
        toolbar.addView(button("⌕ Suche") { showSearch() }, layout().apply { rightMargin = dp(8) })
        toolbar.addView(button("↻") { browser?.reload() }, layout().apply { rightMargin = dp(8) })
        toolbarTitle = text(title, 20f, white, true).apply { maxLines = 1 }
        toolbar.addView(toolbarTitle, layout(0).apply { weight = 1f; leftMargin = dp(12) })
        container.addView(toolbar)
        status = text("Lade Website …", 14f, muted)
        container.addView(status, layout(-1, dp(24)).apply { leftMargin = dp(30) })

        val web = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = true // The website requires JavaScript for player/search.
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture = true
                cacheMode = WebSettings.LOAD_DEFAULT
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                builtInZoomControls = false
            }
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
            setWebViewClient(SiteClient())
            webChromeClient = PlayerChromeClient()
            setOnLongClickListener { true }
        }
        browser = web
        container.addView(web, layout(-1, 0).apply { weight = 1f })
        home.requestFocus()
        web.loadUrl(url)
    }

    private inner class SiteClient : WebViewClient() {
        private var httpError: Int? = null
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            if (UrlPolicy.isAllowedUrl(request.url.toString())) return false
            message("Externer Seitenwechsel aus Sicherheitsgründen blockiert")
            return true
        }

        @Suppress("DEPRECATION")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (UrlPolicy.isAllowedUrl(url)) return false
            message("Externer Seitenwechsel blockiert")
            return true
        }

        override fun onPageFinished(view: WebView, url: String) {
            if (UrlPolicy.isAllowedUrl(url)) pages.visit(url)
            if (httpError == null) {
                status?.text = "Geladen · Website-Navigation per D-Pad vom Zielgerät abhängig."
            }
            toolbarTitle?.text = view.title?.take(50) ?: "Website"
            pendingSearch?.let { search ->
                pendingSearch = null
                runWebsiteSearch(view, search)
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) {
                httpError = error.errorCode
                status?.text = "Ladefehler: ${error.description} · Erneut laden mit ↻"
            }
        }

        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: android.webkit.WebResourceResponse) {
            if (request.isForMainFrame) {
                httpError = response.statusCode
                status?.text = "HTTP ${response.statusCode} · Erneut laden mit ↻"
            }
        }

        override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
            httpError = null
            status?.text = "Lade Website …"
        }

        override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
            // Important: never reuse a dead renderer instance.
            message("Webanzeige abgestürzt. Die Startseite wird geöffnet.")
            showHome()
            return true
        }
    }

    private inner class PlayerChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            if (newProgress < 100) status?.text = "Lade Website … $newProgress %"
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            if (fullscreenView != null) { callback.onCustomViewHidden(); return }
            fullscreenView = view
            fullscreenCallback = callback
            root.addView(view, FrameLayout.LayoutParams(-1, -1))
            view.requestFocus()
        }

        override fun onHideCustomView() = clearFullScreen()
    }

    private fun clearFullScreen() {
        val shown = fullscreenView ?: return
        fullscreenView = null
        root.removeView(shown)
        val callback = fullscreenCallback
        fullscreenCallback = null
        callback?.onCustomViewHidden()
        browser?.requestFocus()
    }

    /** Best-effort search adapter; relies on the site's first-party search DOM. */
    private fun runWebsiteSearch(view: WebView, query: String) {
        val safe = JSONObject.quote(query)
        val script = """
            (function() {
                var input = document.querySelector('input[placeholder*="Search"], input[placeholder*="search"]');
                if (!input) return 'missing';
                input.focus();
                var setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set;
                setter.call(input, $safe);
                input.dispatchEvent(new Event('input', {bubbles:true}));
                input.dispatchEvent(new Event('change', {bubbles:true}));
                return 'sent';
            })();
        """.trimIndent()
        view.evaluateJavascript(script) { result ->
            status?.text = when (result) {
                "\"sent\"" -> "Suchbegriff an Website übergeben · Suchergebnis von Website abhängig"
                else -> "Suchfeld nicht erkannt · Suche innerhalb der Website öffnen"
            }
        }
    }

    private fun showSearch() {
        val input = EditText(this).apply {
            hint = "Filmtitel oder Serie"
            isSingleLine = true
            setPadding(dp(22), dp(16), dp(22), dp(16))
        }
        AlertDialog.Builder(this)
            .setTitle("Website-Suche (experimentell)")
            .setView(input)
            .setNegativeButton("Abbrechen", null)
            .setPositiveButton("Suchen") { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotBlank()) {
                    openWebsite(Section.START.url, "Suche")
                    pendingSearch = query.take(120)
                }
            }
            .show()
    }

    private fun saveCurrentPage() {
        val url = browser?.url ?: return
        if (!UrlPolicy.isAllowedUrl(url)) { message("Diese Seite kann nicht gespeichert werden"); return }
        message(if (pages.toggleFavorite(url)) "Seite gespeichert" else "Favorit entfernt")
    }

    private fun message(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    private fun goBack() {
        if (fullscreenView != null) { clearFullScreen(); return }
        val web = browser
        if (isBrowser && web?.canGoBack() == true) web.goBack()
        else showHome()
    }

    @Deprecated("Legacy back handling supports Android TV 9 and later")
    override fun onBackPressed() = goBack()

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && fullscreenView == null && !isBrowser) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPause() {
        browser?.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        browser?.onResume()
    }

    private fun disposeBrowser() {
        val current = browser ?: return
        browser = null
        current.stopLoading()
        current.onPause()
        (current.parent as? ViewGroup)?.removeView(current)
        current.loadUrl("about:blank")
        current.destroy()
        status = null
        toolbarTitle = null
    }

    override fun onDestroy() {
        clearFullScreen()
        disposeBrowser()
        super.onDestroy()
    }
}
