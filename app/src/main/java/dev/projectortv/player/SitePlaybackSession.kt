package dev.projectortv.player

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import dev.projectortv.catalog.BingeBangParser

/**
 * Video-only adapter for the site's own player. The catalogue and app controls remain native.
 * This is NOT ExoPlayer playback of BingeBang streams: the provider requires a browser session.
 * No stream URL extraction, fake credentials, DRM access, or bot-check bypass is performed.
 */
class SitePlaybackSession(
    private val activity: Activity,
    private val host: FrameLayout,
    private val source: String,
    private val onExit: () -> Unit
) {
    private var web: WebView? = null
    private var fullView: View? = null
    private var fullCallback: WebChromeClient.CustomViewCallback? = null
    private var status: TextView? = null
    private var active = false

    @SuppressLint("SetJavaScriptEnabled")
    fun start() {
        check(BingeBangParser.isDetailUrl(source)) { "Unsupported playback page" }
        close()
        active = true
        host.removeAllViews()
        val shell = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(9, 15, 25))
        }
        host.addView(shell, FrameLayout.LayoutParams(-1, -1))
        val controls = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 6, 12, 6)
        }
        val exit = Button(activity).apply {
            text = "← Zurück"
            isAllCaps = false
            setOnClickListener { onExit() }
        }
        controls.addView(exit, LinearLayout.LayoutParams(0, 58, 1f))
        val play = Button(activity).apply {
            text = "▶ Wiedergabe starten"
            isAllCaps = false
            setOnClickListener { triggerSitePlay() }
        }
        controls.addView(play, LinearLayout.LayoutParams(0, 58, 2f))
        val focus = Button(activity).apply {
            text = "Website steuern"
            isAllCaps = false
            setOnClickListener { web?.requestFocus() }
        }
        controls.addView(focus, LinearLayout.LayoutParams(0, 58, 1f))
        shell.addView(controls)
        status = TextView(activity).apply {
            setTextColor(Color.LTGRAY)
            textSize = 13f
            text = "Player wird geladen …"
        }
        shell.addView(status, LinearLayout.LayoutParams(-1, -2))
        val playerPage = WebView(activity).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setBackgroundColor(Color.BLACK)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
            }
            // Third-party frames can need their own short-lived browser cookies for playback.
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    if (!request.isForMainFrame) return false
                    val uri = request.url
                    return !(uri.scheme == "https" && uri.host.equals("bingebang.st", true))
                }
                override fun onPageFinished(view: WebView, url: String) {
                    if (active) status?.text = "Drücke ▶ Wiedergabe starten oder nutze die Player-Steuerung."
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (active && request.isForMainFrame) status?.text = "Website nicht erreichbar: ${error.description}"
                }
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    if (active) {
                        status?.text = "Player abgestürzt. Bitte zur Titelauswahl zurückkehren."
                        web = null
                        (view.parent as? ViewGroup)?.removeView(view)
                        view.destroy()
                    }
                    return true
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    if (fullView != null) {
                        callback.onCustomViewHidden()
                        return
                    }
                    fullView = view
                    fullCallback = callback
                    host.addView(view, FrameLayout.LayoutParams(-1, -1))
                    view.requestFocus()
                }
                override fun onHideCustomView() { hideFullScreen() }
            }
        }
        web = playerPage
        shell.addView(playerPage, LinearLayout.LayoutParams(-1, 0, 1f))
        exit.requestFocus()
        playerPage.loadUrl(source)
    }

    /** Sends a normal click to the site's visible Play control on direct user request. */
    private fun triggerSitePlay() {
        val script = """
          (function(){
            var elements = Array.from(document.querySelectorAll('button, a, [role="button"]'));
            var play = elements.find(function(e){
              var text = (e.innerText || e.textContent || '').trim();
              return /^play${'$'}/i.test(text) && e.getClientRects().length > 0;
            });
            if (play) { play.click(); return 'clicked'; }
            return 'manual';
          })();
        """.trimIndent()
        web?.evaluateJavascript(script) { result ->
            if (result != "\"clicked\"") {
                Toast.makeText(activity, "Bitte Play direkt auf der Website wählen.", Toast.LENGTH_LONG).show()
                web?.requestFocus()
            }
        }
    }

    fun onPause() { web?.onPause() }
    fun onResume() { web?.onResume() }
    fun back(): Boolean {
        if (fullView != null) {
            hideFullScreen()
            return true
        }
        return false
    }
    private fun hideFullScreen() {
        val old = fullView ?: return
        fullView = null
        (old.parent as? ViewGroup)?.removeView(old)
        val callback = fullCallback
        fullCallback = null
        callback?.onCustomViewHidden()
        web?.requestFocus()
    }
    fun close() {
        active = false
        hideFullScreen()
        val old = web ?: return
        web = null
        old.stopLoading()
        old.onPause()
        (old.parent as? ViewGroup)?.removeView(old)
        old.loadUrl("about:blank")
        old.destroy()
        status = null
    }
}
