package dev.projectortv.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.projectortv.tv.PosterLoader
import java.util.concurrent.Executors
import android.os.Handler
import android.os.Looper

/**
 * Native TV facade for the current first-party BingeBang session.
 * No local accounts, watchlist, catalogue API, playback service or account passwords.
 */
class TvFacadeActivity : Activity() {
    private val background = Color.rgb(7, 13, 23)
    private val surface = Color.rgb(23, 37, 54)
    private val accent = Color.rgb(82, 208, 222)
    private val ink = Color.rgb(242, 247, 250)
    private val muted = Color.rgb(168, 187, 205)
    private val workers = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val posters = PosterLoader(workers) { task -> mainHandler.post(task) }
    private lateinit var root: FrameLayout
    private lateinit var native: LinearLayout
    private lateinit var page: SitePageBridge
    private var contentHost: LinearLayout? = null
    private var siteToolbar: LinearLayout? = null
    private var nativeMode = true
    private var activePath = "/"
    private var currentTitles: List<SitePageBridge.SiteTitle> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        root = FrameLayout(this).apply { setBackgroundColor(background) }
        setContentView(root)
        page = SitePageBridge(this,root,
            { titles -> if (nativeMode && titles.isNotEmpty()) { currentTitles=titles; renderTitles() } },
            { url -> if(nativeMode && SitePageBridge.allowed(url)) { /* single source owns location */ } },
            { reason -> if(nativeMode) showError(reason) })
        page.initialize()
        buildNative()
    }
    private fun d(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun lp(w:Int=-1,h:Int=-2) = LinearLayout.LayoutParams(w,h)
    private fun txt(value:String,size:Float=18f,bold:Boolean=false,color:Int=ink) =
        TextView(this).apply {
            text=value
            textSize=size
            setTextColor(color)
            if(bold)typeface=Typeface.DEFAULT_BOLD
            maxLines=2
        }
    private fun btn(title:String, action:()->Unit) = Button(this).apply {
        text=title
        isAllCaps=false
        setTextColor(ink)
        textSize=16f
        minHeight=d(58)
        isFocusable=true
        isFocusableInTouchMode=true
        background=bg(false)
        setOnFocusChangeListener { view, selected ->
            view.background=bg(selected)
            (view as Button).setTextColor(if(selected)this@TvFacadeActivity.background else ink)
            view.scaleX=if(selected)1.02f else 1f
            view.scaleY=if(selected)1.02f else 1f
        }
        setOnClickListener { action() }
    }
    private fun bg(focus:Boolean)=GradientDrawable().apply {
        cornerRadius=d(12).toFloat()
        setColor(if(focus)accent else surface)
        setStroke(d(2), if(focus)Color.WHITE else surface)
    }

    private fun buildNative() {
        nativeMode=true
        page.websiteVisible(false)
        siteToolbar?.visibility=View.GONE
        if (!::native.isInitialized) {
            native = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;setBackgroundColor(background) }
            root.addView(native, FrameLayout.LayoutParams(-1,-1))
        }
        native.visibility=View.VISIBLE
        native.removeAllViews()
        val sidebar=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(d(16),d(28),d(14),d(12))
            setBackgroundColor(Color.rgb(12, 23, 37))
        }
        native.addView(sidebar,lp(d(210),-1))
        sidebar.addView(txt("PROJEKTOR TV",23f,true,accent))
        sidebar.addView(txt("BingeBang · TV-Ansicht",12f,false,muted),lp().apply{bottomMargin=d(24)})
        val menu=listOf(
            "Start" to "/",
            "Filme" to "/movies",
            "Serien" to "/tv",
            "Entdecken" to "/explore",
            "Kalender" to "/tv/calendar",
            "Mein Bereich" to "/my-space"
        )
        var first:Button?=null
        for ((name,path) in menu) {
            val b=btn(name) { openSection(path) }
            if(first==null)first=b
            sidebar.addView(b,lp(-1,d(59)).apply{bottomMargin=d(8)})
        }
        sidebar.addView(btn("Suche") { openSearch() },lp(-1,d(59)).apply{bottomMargin=d(8)})
        sidebar.addView(btn("Website-Ansicht") { showWebsite() },lp(-1,d(59)))
        val scroll=ScrollView(this).apply{clipToPadding=false;isFillViewport=true}
        native.addView(scroll,lp(0,-1).apply{weight=1f})
        val area=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(d(32),d(25),d(26),d(35))
        }
        contentHost=area
        scroll.addView(area)
        renderTitles()
        first?.post{ if(nativeMode)first?.requestFocus() }
    }
    private fun openSection(path:String) {
        activePath=path
        if(path=="/my-space") { page.navigate(path);showWebsite();return }
        currentTitles=emptyList()
        buildNative()
        page.navigate(path)
    }
    private fun renderTitles() {
        val area=contentHost ?: return
        if(!nativeMode)return
        area.removeAllViews()
        val label=when(activePath) {
            "/movies" -> "Filme"
            "/tv" -> "Serien"
            "/explore" -> "Entdecken"
            "/tv/calendar" -> "Neue Episoden"
            else -> "Für deinen Filmabend"
        }
        area.addView(txt(label,33f,true),lp().apply{bottomMargin=d(12)})
        area.addView(txt("Echte Titel der geladenen BingeBang-Seite",16f,false,muted),
            lp().apply{bottomMargin=d(24)})
        if(currentTitles.isEmpty()) {
            area.addView(txt("Inhalte werden von der Website geladen …",20f,false,muted))
            area.addView(btn("Website direkt anzeigen") { showWebsite() },
                lp(d(285),d(58)).apply{topMargin=d(24)})
            return
        }
        val movies=currentTitles.filter{it.kind=="Film"}
        val series=currentTitles.filter{it.kind=="Serie"}
        if(activePath=="/"){
            row(area,"Filme",movies)
            row(area,"Serien",series)
        } else {
            row(area,label,currentTitles)
        }
        area.addView(btn("Weitere Inhalte und Filter der Website") { showWebsite() },
            lp(d(410),d(58)).apply{topMargin=d(20)})
    }
    private fun row(area:LinearLayout,title:String,entries:List<SitePageBridge.SiteTitle>) {
        if(entries.isEmpty())return
        area.addView(txt(title,24f,true),lp().apply{bottomMargin=d(12);topMargin=d(12)})
        val horizontal=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;clipChildren=false}
        val line=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;clipChildren=false}
        entries.take(70).forEach { item ->
            val tile=LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL
                isFocusable=true
                isFocusableInTouchMode=true
                setPadding(d(6),d(6),d(6),d(6))
                background=bg(false)
                contentDescription=item.kind+" "+item.name
                setOnFocusChangeListener{ v,yes->v.background=bg(yes);v.scaleX=if(yes)1.035f else 1f;v.scaleY=if(yes)1.035f else 1f}
                setOnClickListener{ showDetail(item) }
            }
            val image=ImageView(this).apply{
                setBackgroundColor(Color.rgb(43,60,78))
                scaleType=ImageView.ScaleType.CENTER_CROP
            }
            tile.addView(image,lp(-1,d(215)))
            posters.into(image,item.poster)
            tile.addView(txt(item.name.take(56),15f,true),lp(-1,d(56)))
            line.addView(tile,lp(d(165),d(286)).apply{rightMargin=d(13)})
        }
        horizontal.addView(line)
        area.addView(horizontal,lp(-1,d(300)))
    }

    private fun showDetail(item:SitePageBridge.SiteTitle) {
        nativeMode=true
        buildNative()
        val area=contentHost ?: return
        area.removeAllViews()
        area.addView(txt(item.name,32f,true),lp().apply{bottomMargin=d(14)})
        area.addView(txt(item.kind+" · BingeBang",17f,false,muted),lp().apply{bottomMargin=d(22)})
        val actions=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        actions.addView(btn("▶ Ansehen") { page.playTitle(item.url); showWebsite() },lp(d(350),d(66)).apply{bottomMargin=d(12)})
        actions.addView(btn("Website-Funktionen: Merkliste, Folgen und Details") {
            page.navigate(item.url); showWebsite()
        },lp(d(500),d(66)).apply{bottomMargin=d(12)})
        actions.addView(btn("← Zurück zur Übersicht") { openSection(activePath) },lp(d(350),d(62)))
        area.addView(actions)
        actions.getChildAt(0)?.requestFocus()
    }

    private fun showError(problem:String) {
        val area=contentHost ?: return
        if(!nativeMode)return
        area.addView(txt("Website: "+problem,16f,false,muted))
    }
    private fun showWebsite() {
        nativeMode=false
        native.visibility=View.GONE
        val toolbar=siteToolbar ?: LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            setPadding(d(5),d(3),d(5),d(3))
            setBackgroundColor(Color.rgb(12,23,37))
            root.addView(this,FrameLayout.LayoutParams(-1,d(66)))
            siteToolbar=this
        }
        toolbar.removeAllViews()
        val controls=listOf(
            "⌂ TV-Start" to { openSection("/") },
            "◀ Zurück" to { if(page.canGoBack())page.goBack() else openSection("/") },
            "Suche" to { openSearch() },
            "Mein Bereich" to { page.navigate("/my-space") },
            "↻" to { page.refresh() }
        )
        controls.forEach { (label,command)->
            toolbar.addView(btn(label,command),lp(0,d(58)).apply{weight=1f;rightMargin=d(8)})
        }
        toolbar.visibility=View.VISIBLE
        toolbar.bringToFront()
        page.websiteVisible(true,d(66))
        toolbar.getChildAt(0)?.requestFocus()
    }
    private fun openSearch() {
        // The provider's search box owns its results and filters.
        page.navigate("/")
        showWebsite()
        page.focusSiteSearch()
    }

    @Deprecated("Android TV 9 compatible back navigation")
    override fun onBackPressed() {
        if(page.back())return
        if(!nativeMode) { openSection("/");return }
        if(activePath!="/"){openSection("/");return}
        super.onBackPressed()
    }
    override fun onKeyDown(keyCode:Int,event:KeyEvent):Boolean {
        if(keyCode==KeyEvent.KEYCODE_BACK){onBackPressed();return true}
        return super.onKeyDown(keyCode,event)
    }
    override fun onPause() { page.onPause();super.onPause() }
    override fun onResume() { super.onResume();if(::page.isInitialized)page.onResume() }
    override fun onDestroy() { page.dispose();workers.shutdownNow();super.onDestroy() }
}
