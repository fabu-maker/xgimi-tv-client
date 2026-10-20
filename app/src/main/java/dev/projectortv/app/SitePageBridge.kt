package dev.projectortv.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
import android.widget.FrameLayout
import dev.projectortv.catalog.BingeBangParser
import dev.projectortv.core.UrlPolicy
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URI

/** One WebView owns the real site session, forms, history and original player. */
class SitePageBridge(
    private val activity: Activity,
    private val host: FrameLayout,
    private val onCatalog: (String, List<SiteTitle>) -> Unit,
    private val onLocation: (String) -> Unit,
    private val onProblem: (String) -> Unit,
    private val onBlocked: () -> Unit,
    private val onFullscreen: (Boolean) -> Unit
) {
    companion object {
        const val ORIGIN = UrlPolicy.ORIGIN
        fun allowed(url: String?) = UrlPolicy.isAllowedUrl(url)
        fun allowedTitle(url: String): Boolean = allowed(url) &&
            Regex("^/(movie|tv)/watch/[a-zA-Z0-9_-]+/?$").matches(URI(url).rawPath)
    }
    data class SiteTitle(val name: String, val url: String, val poster: String?, val kind: String, val group: String = "")
    private val handler = Handler(Looper.getMainLooper())
    private val script by lazy { activity.assets.open("tv-site.js").bufferedReader().use { it.readText() } }
    private var web: WebView? = null
    private var full: View? = null
    private var fullCallback: WebChromeClient.CustomViewCallback? = null
    private var active = true
    private var resumed = true
    private var generation = 0
    private var ready = false
    private var blocked = false
    private var pendingSearch = false
    private var pendingPlay: String? = null
    private var shown = false
    private var toolbarHeight = 0

    @SuppressLint("SetJavaScriptEnabled")
    fun initialize(loadHome: Boolean = true) {
        val view = WebView(activity).apply {
            id = R.id.site_webview
            setBackgroundColor(Color.rgb(9,15,25))
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = false
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = false
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture = true
                useWideViewPort = true
                loadWithOverviewMode = true
                safeBrowsingEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
            }
            CookieManager.getInstance().setAcceptCookie(true)
            // Embedded original players may use their own session; cookies stay inside WebView.
            CookieManager.getInstance().setAcceptThirdPartyCookies(this,true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val reject = request.isForMainFrame && !allowed(request.url.toString())
                    if (reject) onProblem("Externer Seitenwechsel blockiert")
                    return reject
                }
                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String) = !allowed(url)
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    generation++
                    handler.removeCallbacksAndMessages(null)
                    ready = false
                    blocked = false
                    if (active) onLocation(url)
                }
                override fun onPageFinished(view: WebView, url: String) {
                    if (!active || !allowed(url) || view.url != url) return
                    val version = generation
                    CookieManager.getInstance().flush()
                    view.evaluateJavascript(script) {
                        if (!isCurrent(version,url)) return@evaluateJavascript
                        ready = true
                        extract()
                        if (pendingSearch) performSearch()
                        if (pendingPlay == url) findPlayLink(url)
                        listOf(700L,2000L,5000L).forEach { delay ->
                            handler.postDelayed({if(isCurrent(version,url) && resumed && !blocked) extract()},delay)
                        }
                    }
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame && active) {
                        ready = false
                        onProblem("Website-Ladefehler (${error.errorCode}). Neu laden oder Website öffnen.")
                    }
                }
                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                    if (request.isForMainFrame && active) onProblem("Website antwortet mit HTTP ${response.statusCode}")
                }
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    generation++
                    ready = false
                    handler.removeCallbacksAndMessages(null)
                    exitFullscreen()
                    if (view === web) web = null
                    (view.parent as? ViewGroup)?.removeView(view)
                    view.destroy()
                    if (active) onProblem("WebView wurde beendet. Mit Neu laden wieder öffnen.")
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    if (full != null) {callback.onCustomViewHidden();return}
                    full = view
                    fullCallback = callback
                    host.addView(view,FrameLayout.LayoutParams(-1,-1))
                    activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    onFullscreen(true)
                    view.requestFocus()
                }
                override fun onHideCustomView() {exitFullscreen()}
            }
        }
        web = view
        host.addView(view,0,FrameLayout.LayoutParams(-1,-1))
        view.visibility = View.INVISIBLE
        if (loadHome) view.loadUrl(ORIGIN + "/")
    }
    private fun isCurrent(version: Int,url: String) = active && generation == version && web?.url == url
    fun navigate(path: String) {
        val url = if(path.startsWith("https://")) path else ORIGIN + path
        if (!allowed(url)) {onProblem("Externer Seitenwechsel blockiert");return}
        exitFullscreen()
        generation++
        pendingPlay = null
        pendingSearch = false
        ready = false
        if (web == null) {initialize(false);websiteVisible(shown,toolbarHeight)}
        web?.loadUrl(url)
    }
    fun playTitle(url: String) {
        if (!allowedTitle(url)) {onProblem("Ungültige Titelseite");return}
        navigate(url)
        pendingPlay = url
    }
    private fun findPlayLink(url: String) {
        val version = generation
        web?.evaluateJavascript("window.__projectorTV && window.__projectorTV.playLink()") { raw ->
            if (!isCurrent(version,url) || blocked) return@evaluateJavascript
            pendingPlay = null
            val target = runCatching {JSONTokener(raw).nextValue() as? String}.getOrNull()
            if (target != null && allowed(target) && Regex("^/(movie|tv)/play/.*").matches(URI(target).path)) navigate(target)
            else onProblem("Play oder die gewünschte Episode auf der Website auswählen")
        }
    }
    fun current() = web?.url ?: ORIGIN + "/"
    fun websiteVisible(show: Boolean,toolbarHeight: Int = 0) {
        shown = show
        this.toolbarHeight = toolbarHeight
        web?.apply {
            layoutParams = FrameLayout.LayoutParams(-1,-1).apply {topMargin = if(show) toolbarHeight else 0}
            visibility = if(show) View.VISIBLE else View.INVISIBLE
            if(show && resumed) onResume() else if(!show) onPause()
        }
    }
    fun focusContent() {
        web?.requestFocus()
        if (ready && !blocked) web?.evaluateJavascript("window.__projectorTV && window.__projectorTV.focusContent()",null)
    }
    fun focusSiteSearch() {pendingSearch = true;if(ready) performSearch()}
    private fun performSearch() {
        if (!pendingSearch || blocked) return
        pendingSearch = false
        val version = generation
        val url = current()
        web?.requestFocus()
        web?.evaluateJavascript("window.__projectorTV && window.__projectorTV.openSearch()") { raw ->
            if(isCurrent(version,url) && raw != "\"focused\"") onProblem("Suchsymbol auf der Website auswählen")
        }
    }
    fun extract() {
        if (!ready || !active || !allowed(current())) return
        val version = generation
        val url = current()
        web?.evaluateJavascript("window.__projectorTV && window.__projectorTV.snapshot()") { raw ->
            if (!isCurrent(version,url)) return@evaluateJavascript
            try {
                val encoded = JSONTokener(raw).nextValue() as? String ?: return@evaluateJavascript
                val snapshot = JSONObject(encoded)
                if (snapshot.optString("url") != url) return@evaluateJavascript
                if (snapshot.optBoolean("blocked")) {
                    if(!blocked) {blocked=true;pendingPlay=null;pendingSearch=false;onBlocked()}
                    return@evaluateJavascript
                }
                val values = snapshot.optJSONArray("titles") ?: return@evaluateJavascript
                val titles = ArrayList<SiteTitle>()
                for(i in 0 until minOf(values.length(),160)) {
                    val obj = values.optJSONObject(i) ?: continue
                    val link = obj.optString("url")
                    val title = obj.optString("title").trim().take(150)
                    if(!allowedTitle(link) || title.isEmpty()) continue
                    titles.add(SiteTitle(title,link,BingeBangParser.posterUrlFromRaw(obj.optString("poster")),
                        if(URI(link).path.startsWith("/movie/")) "Film" else "Serie",obj.optString("group").take(100)))
                }
                onCatalog(url,titles)
            } catch(_: Exception) {onProblem("Katalogdaten konnten nicht gelesen werden")}
        }
    }
    fun refresh() {
        if(web == null) {initialize();websiteVisible(shown,toolbarHeight);return}
        web?.reload()
    }
    fun onPause() {resumed=false;handler.removeCallbacksAndMessages(null);web?.onPause();CookieManager.getInstance().flush()}
    fun onResume() {resumed=true;web?.onResume();if(ready) extract()}
    fun back(): Boolean {if(full != null) {exitFullscreen();return true};return false}
    fun canGoBack() = web?.canGoBack() == true
    fun goBack() {generation++;pendingPlay=null;pendingSearch=false;web?.goBack()}
    private fun exitFullscreen() {
        val shownView = full ?: return
        full = null
        (shownView.parent as? ViewGroup)?.removeView(shownView)
        val callback = fullCallback
        fullCallback = null
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        callback?.onCustomViewHidden()
        onFullscreen(false)
        web?.requestFocus()
    }
    fun dispose() {
        active=false;generation++;handler.removeCallbacksAndMessages(null);exitFullscreen()
        web?.let {view -> view.stopLoading();(view.parent as? ViewGroup)?.removeView(view);view.destroy()}
        web=null
        CookieManager.getInstance().flush()
    }
}
