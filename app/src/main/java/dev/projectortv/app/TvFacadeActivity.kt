package dev.projectortv.app

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.widget.*
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.OnItemViewClickedListener
import dev.projectortv.tv.PosterLoader
import dev.projectortv.tv.TvCardPresenter
import dev.projectortv.tv.TvListRowPresenter
import java.net.URI
import java.util.concurrent.Executors

/** Native TV catalogue with a single original website session. */
open class TvFacadeActivity : FragmentActivity() {
    protected open val loadWebsiteOnStart = true
    private val backgroundColor = Color.rgb(7,13,23)
    private val ink = Color.rgb(242,247,250)
    private val muted = Color.rgb(168,187,205)
    private val accent = Color.rgb(82,208,222)
    private val workers = Executors.newFixedThreadPool(2)
    private val handler = Handler(Looper.getMainLooper())
    private val posters = PosterLoader(workers) {task -> handler.post(task)}
    private lateinit var root: FrameLayout
    private lateinit var native: LinearLayout
    private lateinit var heading: TextView
    private lateinit var status: TextView
    private lateinit var rowsHost: FrameLayout
    private lateinit var details: LinearLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var webStatus: TextView
    private lateinit var page: SitePageBridge
    private lateinit var rows: RowsSupportFragment
    private lateinit var rowAdapter: ArrayObjectAdapter
    private var nativeMode = true
    private var activePath = "/"
    private var titles: List<SitePageBridge.SiteTitle> = emptyList()
    private var detail: SitePageBridge.SiteTitle? = null
    private var returning = false
    private var firstMenu: Button? = null
    private var selectedRow = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        root = FrameLayout(this).apply {setBackgroundColor(backgroundColor)}
        setContentView(root)
        buildUi()
        page = SitePageBridge(this,root,
            {url,items ->
                if(nativeMode && URI(url).path == activePath) {
                    status.text = if(items.isEmpty()) "Keine Titel aus dieser Seite gelesen. Website öffnen oder neu laden." else ""
                    if(items != titles) {titles=items;renderRows()}
                }
            },
            {url ->
                webStatus.text = if(SitePageBridge.allowed(url)) "bingebang.st" else "Seitenwechsel"
                if(returning && SitePageBridge.allowed(url) && URI(url).path == activePath) {
                    returning=false;showNative()
                }
            },
            {reason -> status.text=reason;webStatus.text=reason},
            {
                showWebsite()
                webStatus.text="BingeBang verlangt eine Browserprüfung. Wiedergabe wurde noch nicht gestartet."
            },
            {full -> toolbar.visibility=if(full || nativeMode) View.GONE else View.VISIBLE}
        )
        page.initialize(loadWebsiteOnStart)
        firstMenu?.requestFocus()
    }
    private fun d(value: Int) = (value*resources.displayMetrics.density).toInt()
    private fun lp(w: Int=-1,h: Int=-2) = LinearLayout.LayoutParams(w,h)
    private fun text(value: String,size: Float=18f,bold: Boolean=false,color: Int=ink) = TextView(this).apply {
        text=value;textSize=size;setTextColor(color)
        if(bold) typeface=Typeface.DEFAULT_BOLD
    }
    private fun button(label: String,action: () -> Unit) = Button(this).apply {
        text=label;isAllCaps=false;textSize=15f;setTextColor(ink)
        isFocusable=true;isFocusableInTouchMode=true
        background=background(false)
        setOnFocusChangeListener {view,focused ->
            view.background=background(focused)
            (view as Button).setTextColor(if(focused) backgroundColor else ink)
        }
        setOnClickListener {action()}
    }
    private fun background(focused: Boolean) = GradientDrawable().apply {
        cornerRadius=d(8).toFloat()
        setColor(if(focused) accent else Color.rgb(23,37,54))
        setStroke(d(2),if(focused) Color.WHITE else Color.rgb(23,37,54))
    }
    private fun buildUi() {
        native=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL;setBackgroundColor(backgroundColor)}
        root.addView(native,FrameLayout.LayoutParams(-1,-1))
        val sidebarScroll=ScrollView(this).apply {isVerticalScrollBarEnabled=false;isFillViewport=true}
        native.addView(sidebarScroll,lp(d(180),-1))
        val sidebar=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(d(12),d(24),d(10),d(20))}
        sidebarScroll.addView(sidebar)
        sidebar.addView(text("PROJEKTOR TV",19f,true,accent),lp().apply {bottomMargin=d(20)})
        listOf("Start" to "/","Filme" to "/movies","Serien" to "/tv").forEach {(name,path) ->
            val b=button(name) {openSection(path,name)}
            if(firstMenu==null) firstMenu=b
            sidebar.addView(b,lp(-1,d(48)).apply {bottomMargin=d(6)})
        }
        listOf("Entdecken" to "/explore","Kalender" to "/tv/calendar","Mein Bereich" to "/my-space").forEach {(name,path) ->
            sidebar.addView(button(name) {showWebsite();page.navigate(path)},lp(-1,d(48)).apply {bottomMargin=d(6)})
        }
        sidebar.addView(button("Suche",::openSearch),lp(-1,d(48)).apply {bottomMargin=d(6)})
        sidebar.addView(button("Website") {showWebsite();page.focusContent()},lp(-1,d(48)).apply {bottomMargin=d(6)})
        sidebar.addView(button("Neu laden") {status.text="Lade BingeBang …";page.navigate(activePath)},lp(-1,d(48)).apply {bottomMargin=d(6)})
        sidebar.addView(button("Info / Lizenzen",::showInfo),lp(-1,d(48)))

        val content=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;setPadding(d(24),d(24),d(20),d(12))}
        native.addView(content,lp(0,-1).apply {weight=1f})
        heading=text("BingeBang",30f,true)
        status=text("Lade BingeBang …",15f,false,muted)
        content.addView(heading)
        content.addView(status,lp().apply {topMargin=d(8);bottomMargin=d(14)})
        val body=FrameLayout(this)
        content.addView(body,lp(-1,0).apply {weight=1f})
        rowsHost=FrameLayout(this).apply {id=R.id.catalog_rows}
        body.addView(rowsHost,FrameLayout.LayoutParams(-1,-1))
        details=LinearLayout(this).apply {orientation=LinearLayout.VERTICAL;visibility=View.GONE}
        body.addView(details,FrameLayout.LayoutParams(-1,-1))
        rows=(supportFragmentManager.findFragmentByTag("catalog") as? RowsSupportFragment) ?: RowsSupportFragment().also {
            supportFragmentManager.beginTransaction().replace(R.id.catalog_rows,it,"catalog").commitNow()
        }
        rowAdapter=ArrayObjectAdapter(TvListRowPresenter())
        rows.adapter=rowAdapter
        rows.onItemViewClickedListener=OnItemViewClickedListener {_,item,_,_ ->
            if(item is SitePageBridge.SiteTitle) {selectedRow=rows.selectedPosition;showDetail(item)}
        }

        toolbar=LinearLayout(this).apply {
            id=R.id.website_toolbar;orientation=LinearLayout.VERTICAL;visibility=View.GONE
            setBackgroundColor(Color.rgb(12,23,37));setPadding(d(8),d(3),d(8),d(2))
        }
        val commands=LinearLayout(this)
        listOf<Pair<String,()->Unit>>(
            "TV-Übersicht" to {showNative()},"Zurück" to {backFromWebsite()},
            "Inhalt bedienen" to {page.focusContent()},"Suche" to {openSearch()},
            "Konto" to {page.navigate("/my-space")},"Neu laden" to {page.refresh()}
        ).forEach {(name,action) -> commands.addView(button(name,action),lp(0,d(44)).apply {weight=1f;rightMargin=d(5)})}
        toolbar.addView(commands)
        webStatus=text("bingebang.st",12f,false,muted).apply {maxLines=2}
        toolbar.addView(webStatus)
        root.addView(toolbar,FrameLayout.LayoutParams(-1,d(80)))
    }
    private fun openSection(path: String,name: String) {
        activePath=path;heading.text=name;titles=emptyList();detail=null;selectedRow=0
        rowAdapter.clear();status.text="Lade BingeBang …";showNative();page.navigate(path)
    }
    private fun renderRows() {
        if(detail!=null) return
        val hadFocus=rowsHost.hasFocus()
        rowAdapter.clear()
        titles.groupBy {it.group.ifBlank {if(it.kind=="Film") "Filme" else "Serien"}}.entries.forEachIndexed {i,group ->
            val cards=ArrayObjectAdapter(TvCardPresenter(posters))
            group.value.forEach {cards.add(it)}
            rowAdapter.add(ListRow(HeaderItem(i.toLong(),group.key),cards))
        }
        if(rowAdapter.size()>0) rows.setSelectedPosition(selectedRow.coerceIn(0,rowAdapter.size()-1),false)
        if(hadFocus) rowsHost.requestFocus()
    }
    private fun showDetail(item: SitePageBridge.SiteTitle) {
        detail=item;rowsHost.visibility=View.GONE;details.visibility=View.VISIBLE;details.removeAllViews()
        details.addView(text(item.name,28f,true),lp().apply {bottomMargin=d(18)})
        details.addView(text(item.kind,17f,false,muted),lp().apply {bottomMargin=d(18)})
        val play=button("Ansehen") {showWebsite();page.playTitle(item.url)}
        details.addView(play,lp(d(310),d(60)).apply {bottomMargin=d(10)})
        details.addView(button("Details, Folgen und Merkliste") {showWebsite();page.navigate(item.url)},lp(d(380),d(60)).apply {bottomMargin=d(10)})
        details.addView(button("Zur Übersicht") {detail=null;details.visibility=View.GONE;rowsHost.visibility=View.VISIBLE;renderRows();rowsHost.requestFocus()},lp(d(310),d(60)))
        play.requestFocus()
    }
    private fun showNative() {
        nativeMode=true
        if(::page.isInitialized) {page.back();page.websiteVisible(false)}
        toolbar.visibility=View.GONE;native.visibility=View.VISIBLE
        if(detail!=null) showDetail(detail!!) else {
            details.visibility=View.GONE;rowsHost.visibility=View.VISIBLE
            if(titles.isNotEmpty()) rowsHost.requestFocus() else firstMenu?.requestFocus()
        }
    }
    private fun showWebsite() {
        nativeMode=false;native.visibility=View.GONE;toolbar.visibility=View.VISIBLE;toolbar.bringToFront()
        page.websiteVisible(true,d(80));toolbar.getChildAt(0).requestFocus()
    }
    private fun openSearch() {
        showWebsite()
        // Run against the current original document; no timed click into a different page.
        page.focusSiteSearch()
    }
    private fun backFromWebsite() {
        if(page.back()) return
        if(page.canGoBack()) {returning=true;page.goBack()} else showNative()
    }
    @Deprecated("Android TV 9 compatible back navigation")
    override fun onBackPressed() {
        if(page.back()) return
        if(!nativeMode) {backFromWebsite();return}
        if(detail!=null) {detail=null;showNative();renderRows();return}
        if(activePath!="/") {openSection("/","BingeBang");return}
        super.onBackPressed()
    }
    override fun onKeyDown(keyCode: Int,event: KeyEvent): Boolean {
        if(keyCode==KeyEvent.KEYCODE_MENU && !nativeMode) {toolbar.visibility=View.VISIBLE;toolbar.requestFocus();return true}
        return super.onKeyDown(keyCode,event)
    }
    private fun showInfo() {
        val body=text("Projektor TV 0.6.0\n\nEntwicklungsstand für Geräteprüfung. BingeBang-Anmeldung, Film- und Serienwiedergabe auf Xgimi sind noch nicht nachgewiesen. Konto, Merkliste und Fortschritt verwaltet die Website.\n\n" +
            assets.open("licenses/NOTICE.txt").bufferedReader().use {it.readText()} + "\n\n" +
            assets.open("licenses/DEPENDENCIES.txt").bufferedReader().use {it.readText()},15f).apply {setPadding(d(24),d(16),d(24),d(16))}
        val scroll=ScrollView(this).apply {addView(body)}
        AlertDialog.Builder(this).setTitle("Status und Open-Source-Lizenzen").setView(scroll).setPositiveButton("Schließen",null).show()
    }
    override fun onPause() {if(::page.isInitialized) page.onPause();super.onPause()}
    override fun onResume() {super.onResume();if(::page.isInitialized) page.onResume()}
    override fun onDestroy() {if(::page.isInitialized) page.dispose();posters.close();workers.shutdownNow();handler.removeCallbacksAndMessages(null);super.onDestroy()}
}
