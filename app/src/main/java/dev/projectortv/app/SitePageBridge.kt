package dev.projectortv.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Bitmap
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
import android.widget.FrameLayout
import org.json.JSONArray
import org.json.JSONTokener
import java.net.URI

/** One persistent first-party web session powers TV catalog, account, settings and playback. */
class SitePageBridge(
    private val activity: Activity,
    private val host: FrameLayout,
    private val onCatalog: (List<SiteTitle>) -> Unit,
    private val onLocation: (String) -> Unit,
    private val onProblem: (String) -> Unit
) {
    companion object {
        const val ORIGIN = "https://bingebang.st"
        fun allowed(url: String?): Boolean = try {
            val uri = URI(url ?: "")
            uri.scheme.equals("https", true) &&
                uri.host.equals("bingebang.st", true) &&
                (uri.port == -1 || uri.port == 443) &&
                uri.userInfo == null && url!!.length < 4096
        } catch (_: Exception) { false }

        fun allowedTitle(url: String): Boolean = allowed(url) &&
            Regex("^/(movie|tv)/watch/[a-zA-Z0-9_-]+/?$").matches(URI(url).path)

        /** DOM content only. Never inspect forms, inputs, credentials or session tokens. */
        const val CATALOG_JS = """
(function() {
 var found=[], seen={};
 var links=document.querySelectorAll('a[href]');
 for(var i=0;i<links.length && found.length<96;i++){
   var a=links[i], href=a.href;
   try {
     var u=new URL(href);
     if(u.protocol!=='https:' || u.hostname!=='bingebang.st')continue;
     if(!/^\/(movie|tv)\/watch\/[a-zA-Z0-9_-]+\/?$/.test(u.pathname))continue;
     if(seen[u.pathname])continue; seen[u.pathname]=true;
     var img=a.querySelector('img');
     var title=(a.getAttribute('aria-label')||a.getAttribute('title')||
                (img&&img.alt)||a.innerText||a.textContent||'').trim().slice(0,130);
     if(!title)title=decodeURIComponent(u.pathname.split('/').pop()).replace(/[-_]/g,' ');
     var poster=img ? (img.currentSrc||img.src||img.getAttribute('data-src')||'') : '';
     if(poster && !/^https:\/\//.test(poster))poster='';
     found.push({title:title,url:u.origin+u.pathname,poster:poster,kind:u.pathname.startsWith('/movie/')?'Film':'Serie'});
   } catch(e){}
 }
 return JSON.stringify(found);
})()
        """
    }

    data class SiteTitle(val name: String, val url: String, val poster: String?, val kind: String)

    private var web: WebView? = null
    private var full: View? = null
    private var callback: WebChromeClient.CustomViewCallback? = null
    private var active = true

    @SuppressLint("SetJavaScriptEnabled")
    fun initialize() {
        val view = WebView(activity).apply {
            setBackgroundColor(Color.rgb(9, 15, 25))
            isFocusable = true
            isFocusableInTouchMode = true
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture = true
                useWideViewPort = true
                loadWithOverviewMode = true
                safeBrowsingEnabled = true
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object: WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                    request.isForMainFrame && !allowed(request.url.toString())
                @Suppress("DEPRECATION")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !allowed(url)
                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    if (active) onLocation(url)
                }
                override fun onPageFinished(view: WebView, url: String) {
                    if (!active || !allowed(url)) return
                    CookieManager.getInstance().flush()
                    onLocation(url)
                    installTvFocusStyling()
                    view.postDelayed({ if(active && web===view && view.url==url) extract() }, 650)
                    view.postDelayed({ if(active && web===view && view.url==url) extract() }, 2700)
                }
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame && active) onProblem(error.description.toString())
                }
                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    if (view === web) web = null
                    (view.parent as? ViewGroup)?.removeView(view)
                    view.destroy()
                    if (active) onProblem("Website-Prozess beendet. App erneut öffnen.")
                    return true
                }
            }
            webChromeClient = object: WebChromeClient() {
                override fun onShowCustomView(view: View, custom: CustomViewCallback) {
                    if (full != null) { custom.onCustomViewHidden(); return }
                    full = view
                    callback = custom
                    host.addView(view, FrameLayout.LayoutParams(-1,-1))
                    view.requestFocus()
                }
                override fun onHideCustomView() { exitFullscreen() }
            }
        }
        web = view
        host.addView(view, 0, FrameLayout.LayoutParams(-1, -1))
        view.visibility = View.INVISIBLE
        view.loadUrl(ORIGIN + "/")
    }

    fun navigate(path: String) {
        val url = if (path.startsWith("https://")) path else ORIGIN + path
        if (!allowed(url)) { onProblem("Externer Seitenwechsel blockiert"); return }
        onCatalog(emptyList())
        web?.loadUrl(url)
    }
    fun current(): String = web?.url ?: ORIGIN
    fun websiteVisible(show: Boolean, toolbarHeight: Int = 0) {
        val view = web ?: return
        val lp = FrameLayout.LayoutParams(-1, -1).apply { topMargin = if(show) toolbarHeight else 0 }
        view.layoutParams = lp
        view.visibility = if(show) View.VISIBLE else View.INVISIBLE
        if(show) view.requestFocus() else extract()
    }
    /** Opens only the site's own search UI; results stay tied to the website account. */
    fun focusSiteSearch() {
        web?.postDelayed({
            web?.evaluateJavascript("""
(function(){
 var q=document.querySelector('input[placeholder*="Search"],input[placeholder*="search"]');
 if(!q){
  var buttons=Array.from(document.querySelectorAll('button,[role=button]'));
  var trigger=buttons.find(function(e){
   var label=((e.getAttribute('aria-label')||'')+' '+(e.textContent||'')).trim();
   return /search/i.test(label) && e.getClientRects().length>0;
  });
  if(trigger)trigger.click();
  q=document.querySelector('input[placeholder*="Search"],input[placeholder*="search"]');
 }
 if(q){q.focus();return 'focused'}
 return 'unavailable';
})()
            """.trimIndent()) { result ->
                if (result.contains("unavailable")) onProblem("Bitte die Website-Suche über das Suchsymbol öffnen")
            }
        },1200)
    }

    fun refresh() { web?.reload() }
    fun onPause() { web?.onPause(); CookieManager.getInstance().flush() }
    fun onResume() { web?.onResume() }
    fun back(): Boolean {
        if (full != null) { exitFullscreen(); return true }
        return false
    }
    fun canGoBack() = web?.canGoBack() == true
    fun goBack() { web?.goBack() }
    fun dispose() {
        active=false
        exitFullscreen()
        val current=web
        web=null
        if (current!=null) {
            current.stopLoading()
            (current.parent as? ViewGroup)?.removeView(current)
            current.destroy()
        }
        CookieManager.getInstance().flush()
    }

    private fun exitFullscreen() {
        val shown=full ?: return
        full=null
        (shown.parent as? ViewGroup)?.removeView(shown)
        val done=callback
        callback=null
        done?.onCustomViewHidden()
    }

    fun extract() {
        web?.evaluateJavascript(CATALOG_JS) { raw ->
            if (!active) return@evaluateJavascript
            try {
                val json=JSONTokener(raw).nextValue() as? String ?: return@evaluateJavascript
                val values=JSONArray(json)
                val items=ArrayList<SiteTitle>()
                for (i in 0 until values.length()) {
                    val obj=values.optJSONObject(i) ?: continue
                    val url=obj.optString("url")
                    if (!allowedTitle(url)) continue
                    val title=obj.optString("title").trim()
                    if(title.isBlank())continue
                    items.add(SiteTitle(title,url,obj.optString("poster").takeIf { it.startsWith("https://") },
                        obj.optString("kind")))
                }
                onCatalog(items)
            } catch(e:Exception) { onProblem("Katalogdaten konnten nicht gelesen werden") }
        }
    }

    /** Change only focus appearance; do not hide forms, tabs or original player controls. */
    private fun installTvFocusStyling() {
        web?.evaluateJavascript("""
(function(){
 if(document.getElementById('projector-tv-focus'))return;
 var s=document.createElement('style');s.id='projector-tv-focus';
 s.textContent=':is(a,button,input,select,[role=button],[tabindex]):focus-visible{outline:4px solid #5be9f7!important;outline-offset:3px!important;box-shadow:0 0 0 5px #05111e!important}';
 document.head.appendChild(s);
})()
        """.trimIndent(), null)
    }
}
