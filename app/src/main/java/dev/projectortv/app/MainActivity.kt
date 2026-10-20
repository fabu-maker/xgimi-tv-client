package dev.projectortv.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import dev.projectortv.catalog.BingeBangParser
import dev.projectortv.catalog.CatalogRepository
import dev.projectortv.catalog.CatalogShelf
import dev.projectortv.catalog.MediaItem
import dev.projectortv.catalog.MediaKind
import dev.projectortv.catalog.NativeLibrary
import dev.projectortv.player.NativeVideo
import dev.projectortv.player.PlaybackPolicy
import dev.projectortv.player.SitePlaybackSession
import dev.projectortv.tv.PosterLoader
import java.util.concurrent.Executors

/**
 * Native Android-TV catalogue and controls. Embedded browser technology is confined
 * to the on-demand video player, for which the site does not expose a supported native API.
 */
class MainActivity : Activity() {
    private val surfaceColor = Color.rgb(9, 15, 25)
    private val panel = Color.rgb(23, 34, 50)
    private val focused = Color.rgb(100, 203, 218)
    private val ink = Color.rgb(240, 247, 250)
    private val muted = Color.rgb(172, 186, 202)
    private val pool = Executors.newFixedThreadPool(4)
    private val handler = Handler(Looper.getMainLooper())
    private val catalog = CatalogRepository()
    private val posters = PosterLoader(pool) { job -> handler.post { job() } }
    private lateinit var root: FrameLayout
    private lateinit var state: NativeState
    private val cachedItems = linkedMapOf<String, MediaItem>()
    private val player by lazy { NativeVideo(this) }
    private var screen = "home"
    private var serial = 0
    private var isDemoPlaying = false
    private var sitePlayer: SitePlaybackSession? = null
    private var returnToItem: MediaItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        state = NativeState(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        root = FrameLayout(this).apply { setBackgroundColor(surfaceColor) }
        setContentView(root)
        showShelf(CatalogShelf.HOME)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun lp(w: Int = -1, h: Int = -2) = LinearLayout.LayoutParams(w, h)
    private fun label(value: String, size: Float = 19f, tint: Int = ink, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(tint)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun button(title: String, click: () -> Unit): Button = Button(this).apply {
        text = title
        isAllCaps = false
        textSize = 16f
        setTextColor(ink)
        minHeight = dp(54)
        isFocusable = true
        isFocusableInTouchMode = true
        background = bg(false)
        setPadding(dp(16), dp(10), dp(16), dp(10))
        setOnFocusChangeListener { view, hasFocus ->
            view.background = bg(hasFocus)
            (view as Button).setTextColor(if (hasFocus) surfaceColor else ink)
            view.elevation = if (hasFocus) dp(8).toFloat() else 0f
        }
        setOnClickListener { click() }
    }

    private fun bg(hasFocus: Boolean) = GradientDrawable().apply {
        cornerRadius = dp(10).toFloat()
        setColor(if (hasFocus) focused else panel)
        setStroke(dp(2), if (hasFocus) Color.WHITE else panel)
    }

    private fun begin(id: String, heading: String, description: String): LinearLayout {
        screen = id
        serial++
        if (isDemoPlaying) closeDemo(savePosition = true)
        sitePlayer?.close()
        sitePlayer = null
        root.removeAllViews()
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(surfaceColor)
        }
        root.addView(outer, FrameLayout.LayoutParams(-1, -1))
        val rail = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(14, 23, 35))
            setPadding(dp(16), dp(25), dp(16), dp(20))
        }
        outer.addView(rail, lp(dp(218), -1))
        rail.addView(label("PROJEKTOR", 24f, focused, true))
        rail.addView(label("NATIVE TV", 12f, muted), lp().apply { bottomMargin = dp(20) })
        val menu = listOf(
            "Startseite" to { showShelf(CatalogShelf.HOME) },
            "Filme" to { showShelf(CatalogShelf.MOVIES) },
            "Serien" to { showShelf(CatalogShelf.SERIES) },
            "Suche" to { showSearch() },
            "Merkliste" to { showLocalList("Favoriten", state.favorites()) },
            "Verlauf" to { showLocalList("Zuletzt geöffnet", state.history()) },
            "Info & Test" to { showInfo() }
        )
        var first: Button? = null
        for ((title, action) in menu) {
            val control = button(title, action)
            if (first == null) first = control
            rail.addView(control, lp().apply { bottomMargin = dp(9) })
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(dp(28), dp(16), dp(20), dp(40))
        }
        outer.addView(scroll, lp(0, -1).apply { weight = 1f })
        val feed = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(feed)
        feed.addView(label(heading, 31f, ink, true))
        feed.addView(label(description, 15f, muted), lp().apply { bottomMargin = dp(24); topMargin = dp(5) })
        first?.post { if (screen == id) first?.requestFocus() }
        return feed
    }

    private fun showShelf(shelf: CatalogShelf, refresh: Boolean = false) {
        val feed = begin("shelf:${shelf.name}", shelf.title,
            "Eigenständiger Android-TV-Katalog · Pfeiltasten und OK")
        val current = serial
        val loading = label("Katalog wird geladen …", 18f, muted)
        feed.addView(loading)
        pool.execute {
            val result = runCatching { catalog.shelf(shelf, refresh) }
            handler.post {
                if (serial != current) return@post
                feed.removeView(loading)
                result.onSuccess { all ->
                    all.forEach { cachedItems[it.url] = it }
                    val movies = all.filter { it.kind == MediaKind.MOVIE }
                    val series = all.filter { it.kind == MediaKind.SERIES }
                    if (shelf == CatalogShelf.HOME) {
                        mediaRow(feed, "Filme", movies.take(24))
                        mediaRow(feed, "Serien", series.take(24))
                        mediaRow(feed, "Aus deiner Merkliste", lookup(state.favorites()).take(20))
                    } else {
                        mediaRow(feed, shelf.title, all.take(80))
                    }
                    feed.addView(label("Wiedergabe nutzt bei BingeBang-Titeln den Original-Webplayer; die Katalogoberfläche bleibt nativ.", 14f, muted))
                }.onFailure { e ->
                    feed.addView(label("Katalog konnte nicht geladen werden: ${e.message}", 18f))
                    feed.addView(button("Erneut versuchen") { showShelf(shelf, true) })
                }
            }
        }
    }

    private fun lookup(urls: List<String>) = urls.mapNotNull { cachedItems[it] }

    private fun mediaRow(feed: LinearLayout, heading: String, items: List<MediaItem>) {
        feed.addView(label("$heading (${items.size})", 22f, ink, true), lp().apply {
            bottomMargin = dp(10); topMargin = dp(12)
        })
        if (items.isEmpty()) {
            feed.addView(label("Noch keine Einträge", 15f, muted))
            return
        }
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            isFocusable = false
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
        }
        items.forEach { item -> row.addView(tile(item), lp(dp(164), dp(275)).apply { rightMargin = dp(13) }) }
        scroll.addView(row)
        feed.addView(scroll, lp(-1, dp(285)).apply { bottomMargin = dp(14) })
    }

    private fun tile(item: MediaItem): View {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), dp(5), dp(5), dp(5))
            isFocusable = true
            isFocusableInTouchMode = true
            background = bg(false)
            contentDescription = "${if (item.kind == MediaKind.MOVIE) "Film" else "Serie"}: ${item.title}"
            setOnFocusChangeListener { v, hasFocus ->
                v.background = bg(hasFocus)
                v.scaleX = if (hasFocus) 1.04f else 1f
                v.scaleY = if (hasFocus) 1.04f else 1f
            }
            setOnClickListener { showDetail(item) }
        }
        val image = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.rgb(46, 65, 81))
        }
        wrapper.addView(image, lp(-1, dp(207)))
        posters.into(image, item.posterUrl)
        wrapper.addView(label(item.title.take(35), 14f, ink, true), lp(-1, dp(51)))
        return wrapper
    }

    private fun showDetail(item: MediaItem) {
        state.opened(item.url)
        val feed = begin("detail:${item.url}", item.title,
            if (item.kind == MediaKind.MOVIE) "Film" else "Serie")
        val current = serial
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val favourite = button(if (state.isFavorite(item.url)) "✓ Von Merkliste entfernen" else "+ Zur Merkliste") {
            state.toggle(item.url)
            showDetail(item)
        }
        actions.addView(favourite, lp(dp(250), dp(58)).apply { rightMargin = dp(12) })
        actions.addView(button("▶ Film / Episode abspielen") {
            openSitePlayer(item)
        }, lp(dp(260), dp(58)))
        feed.addView(actions, lp().apply { bottomMargin = dp(20) })
        val info = label(item.overview ?: "Filminformationen werden geladen …", 18f)
        feed.addView(info)
        pool.execute {
            val result = runCatching { catalog.detail(item) }
            handler.post {
                if (serial != current) return@post
                result.onSuccess { detail ->
                    cachedItems[detail.url] = detail
                    info.text = detail.overview ?: "Keine Inhaltsbeschreibung verfügbar."
                }.onFailure { info.text = "Detailinformationen vorübergehend nicht verfügbar." }
            }
        }
        favourite.post { if (serial == current) favourite.requestFocus() }
    }

    private fun openSitePlayer(item: MediaItem) {
        if (!BingeBangParser.isDetailUrl(item.url)) {
            AlertDialog.Builder(this).setMessage("Ungültige Filmadresse").setPositiveButton("OK", null).show()
            return
        }
        if (isDemoPlaying) closeDemo(true)
        sitePlayer?.close()
        sitePlayer = null
        screen = "watch"
        serial++
        returnToItem = item
        val session = SitePlaybackSession(this, root, item.url) {
            closeSitePlayer()
            showDetail(item)
        }
        sitePlayer = session
        session.start()
    }

    private fun closeSitePlayer() {
        sitePlayer?.close()
        sitePlayer = null
        returnToItem = null
    }

    private fun showSearch() {
        val feed = begin("search", "Suche", "Durchsuche die öffentlich geladenen Film- und Serienkataloge")
        val search = EditText(this).apply {
            hint = "Filmtitel oder Serie eingeben"
            isSingleLine = true
            setSingleLine()
            textSize = 20f
            setTextColor(ink)
            setHintTextColor(muted)
            setPadding(dp(18), dp(13), dp(18), dp(13))
            background = bg(false)
        }
        feed.addView(search, lp(-1, dp(65)))
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        feed.addView(button("Suchen") {
            val query = search.text.toString()
            val current = serial
            results.removeAllViews()
            results.addView(label("Suche läuft …", 18f, muted))
            pool.execute {
                val source = mutableListOf<MediaItem>()
                val errors = mutableListOf<String>()
                for (shelf in listOf(CatalogShelf.MOVIES, CatalogShelf.SERIES)) {
                    runCatching { catalog.shelf(shelf) }.onSuccess { source.addAll(it) }
                        .onFailure { errors.add(shelf.title) }
                }
                val found = NativeLibrary.search(source, query)
                handler.post {
                    if (serial != current) return@post
                    source.forEach { cachedItems[it.url] = it }
                    results.removeAllViews()
                    mediaRow(results, "Suchergebnisse", found)
                    if (errors.isNotEmpty()) results.addView(label("Nicht verfügbar: ${errors.joinToString()}", 15f, muted))
                    results.addView(label("Suche umfasst die geladenen Katalogseiten, keinen vollständigen Website-Suchindex.", 15f, muted))
                }
            }
        }, lp(dp(200), dp(58)).apply { topMargin = dp(16); bottomMargin = dp(20) })
        feed.addView(results)
    }

    private fun showLocalList(title: String, urls: List<String>) {
        val feed = begin("library:$title", title, "Lokal auf diesem Xgimi gespeichert")
        val known = lookup(urls)
        mediaRow(feed, title, known)
        val missing = urls.filterNot { it in cachedItems }
        if (missing.isNotEmpty()) {
            feed.addView(label("${missing.size} Titel werden nach Laden der Katalogseiten angezeigt.", 15f, muted))
            feed.addView(button("Katalog laden") { showShelf(CatalogShelf.HOME) })
        }
    }

    private fun showInfo() {
        val feed = begin("info", "Informationen", "Native Vorschau · Version 0.2.0")
        feed.addView(label("Die gesamte Navigation und der Katalog bestehen aus nativen TV-Komponenten.", 18f))
        feed.addView(label("Bei BingeBang-Filmen öffnet sich ausschließlich für die Wiedergabe der Original-Webplayer. Das Verhalten hängt von WebView, Anbieter-Servern und verfügbaren Filmen ab.", 18f, muted),
            lp().apply { topMargin = dp(16); bottomMargin = dp(16) })
        feed.addView(button("Native Videowiedergabe mit Testvideo prüfen") { startDemo() }, lp(dp(480), dp(62)))
        feed.addView(label("Testvideo: Big Buck Bunny (Blender Foundation), öffentliches Demo-Material.", 14f, muted),
            lp().apply { topMargin = dp(14) })
    }

    private fun startDemo() {
        if (!PlaybackPolicy.canPlay(PlaybackPolicy.DEMO_URL)) return
        screen = "demo"
        serial++
        isDemoPlaying = true
        root.removeAllViews()
        player.start(root, PlaybackPolicy.DEMO_URL, state.demoPosition())
    }

    private fun closeDemo(savePosition: Boolean) {
        if (!isDemoPlaying) return
        if (savePosition) state.setDemoPosition(player.position())
        player.stop()
        isDemoPlaying = false
    }

    @Deprecated("Legacy Android TV back handling")
    override fun onBackPressed() {
        when {
            sitePlayer != null && sitePlayer?.back() == true -> Unit
            sitePlayer != null -> {
                val item = returnToItem
                closeSitePlayer()
                if (item != null) showDetail(item) else showShelf(CatalogShelf.HOME)
            }
            isDemoPlaying -> { closeDemo(true); showInfo() }
            screen.startsWith("detail") -> showShelf(CatalogShelf.HOME)
            screen != "shelf:HOME" -> showShelf(CatalogShelf.HOME)
            else -> super.onBackPressed()
        }
    }
    override fun onPause() {
        sitePlayer?.onPause()
        if (isDemoPlaying) { state.setDemoPosition(player.position()); player.pause() }
        super.onPause()
    }
    override fun onResume() {
        super.onResume()
        sitePlayer?.onResume()
    }
    override fun onDestroy() {
        closeSitePlayer()
        closeDemo(true)
        pool.shutdownNow()
        super.onDestroy()
    }
}
