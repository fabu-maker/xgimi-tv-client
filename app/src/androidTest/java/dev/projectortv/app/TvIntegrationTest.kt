package dev.projectortv.app

import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Offline HTML fixtures only. Passing these tests says nothing about real streams or accounts. */
@RunWith(AndroidJUnit4::class)
class TvIntegrationTest {
    private val instrument = InstrumentationRegistry.getInstrumentation()
    private fun launch() = ActivityScenario.launch<OfflineTvActivity>(
        Intent(instrument.targetContext,OfflineTvActivity::class.java))

    private fun eval(scenario: ActivityScenario<OfflineTvActivity>,script: String): String {
        val done=CountDownLatch(1)
        var result=""
        scenario.onActivity {activity -> activity.findViewById<WebView>(R.id.site_webview).evaluateJavascript(script) {result=it;done.countDown()}}
        assertTrue("JavaScript callback timeout",done.await(8,TimeUnit.SECONDS))
        return result
    }
    private fun load(scenario: ActivityScenario<OfflineTvActivity>,html: String) {
        scenario.onActivity {activity -> activity.findViewById<WebView>(R.id.site_webview)
            .loadDataWithBaseURL("https://bingebang.st/",html,"text/html","UTF-8","https://bingebang.st/")}
        val deadline=SystemClock.elapsedRealtime()+10000
        while(SystemClock.elapsedRealtime()<deadline) {
            if(eval(scenario,"!!window.__projectorTV")=="true") {instrument.waitForIdleSync();return}
            SystemClock.sleep(100)
        }
        fail("Website adapter did not initialize")
    }
    private fun views(view: View): List<View> = listOf(view) +
        if(view is ViewGroup) (0 until view.childCount).flatMap {views(view.getChildAt(it))} else emptyList()
    private fun awaitView(scenario: ActivityScenario<OfflineTvActivity>,condition: (View)->Boolean) {
        val deadline=SystemClock.elapsedRealtime()+10000
        while(SystemClock.elapsedRealtime()<deadline) {
            var found=false
            scenario.onActivity {found=views(it.window.decorView).any(condition)}
            if(found) {instrument.waitForIdleSync();return}
            SystemClock.sleep(100)
        }
        fail("Expected visible UI state was not reached")
    }
    private fun click(scenario: ActivityScenario<OfflineTvActivity>,label: String) {
        scenario.onActivity {activity ->
            val target=views(activity.window.decorView).filterIsInstance<Button>().first {it.text.toString()==label}
            target.performClick()
        }
        instrument.waitForIdleSync()
    }
    private fun screenshot(name: String) {
        val path=File(instrument.targetContext.getExternalFilesDir(null),"screenshots").apply {mkdirs()}
        instrument.uiAutomation.takeScreenshot().useBitmap {bitmap ->
            File(path,"$name.png").outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        }
    }
    private inline fun Bitmap.useBitmap(block: (Bitmap)->Unit) {try {block(this)} finally {recycle()}}

    @Test fun extractsOnlySiteTitlesWithoutReadingFormValues() {
        launch().use {scenario ->
            load(scenario,"""<html><head><meta name="viewport" content="width=device-width"></head><body>
              <section><h2>Offline fixture</h2>
              <a aria-label="Fixture A" href="/movie/watch/fixture-a">A</a>
              <a aria-label="Fixture B" href="/tv/watch/fixture-b">B</a>
              <a href="/movie/watch/untitled"></a>
              <a aria-label="Foreign" href="https://example.org/movie/watch/foreign">Foreign</a>
              </section><input type="password" value="NEVER_EXPORT_THIS_FIXTURE_VALUE">
              </body></html>""")
            val raw=eval(scenario,"window.__projectorTV.snapshot()")
            assertFalse(raw.contains("NEVER_EXPORT"))
            val snapshot=JSONObject(JSONTokener(raw).nextValue() as String)
            assertEquals(2,snapshot.getJSONArray("titles").length())
            assertEquals("Offline fixture",snapshot.getJSONArray("titles").getJSONObject(0).getString("group"))
            assertFalse(snapshot.getBoolean("blocked"))
        }
    }

    @Test fun originalSearchOpensHiddenInputThenDpadMovesButtons() {
        launch().use {scenario ->
            load(scenario,"""<html><head><meta name="viewport" content="width=device-width"></head><body>
              <button aria-label="Search" onclick="document.getElementById('search').hidden=false">Search</button>
              <div id="search" hidden><input placeholder="Search movies and TV shows…"></div>
              <div style="display:flex;gap:60px;margin-top:60px"><button id="a">First</button><button id="b">Second</button></div>
              </body></html>""")
            click(scenario,"Suche")
            assertEquals("true",eval(scenario,"document.activeElement.tagName==='INPUT' && !document.getElementById('search').hidden"))
            eval(scenario,"document.getElementById('a').focus()")
            instrument.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            assertEquals("\"b\"",eval(scenario,"document.activeElement.id"))
            screenshot("web-focus-offline")
        }
    }

    @Test fun browserCheckShowsOriginalPageAndNoSyntheticCatalogue() {
        launch().use {scenario ->
            load(scenario,"""<html><head><title>Quick check · BingeBang</title></head><body>
              <h1>Quick check before you watch</h1><p>You seem to be using a VPN/proxy</p>
              <p>Offline regression fixture, no network request.</p></body></html>""")
            val snapshot=JSONObject(JSONTokener(eval(scenario,"window.__projectorTV.snapshot()")).nextValue() as String)
            assertTrue(snapshot.getBoolean("blocked"))
            assertEquals(0,snapshot.getJSONArray("titles").length())
            awaitView(scenario) {it is TextView && it.isShown && it.text.contains("Browserprüfung")}
            scenario.onActivity {activity ->
                assertEquals(View.VISIBLE,activity.findViewById<WebView>(R.id.site_webview).visibility)
                assertTrue(views(activity.window.decorView).filterIsInstance<TextView>()
                    .any {it.text.contains("Browserprüfung")})
            }
            screenshot("gate-offline")
        }
    }

    @Test fun nativeCardsAcceptRemoteNavigationAndBack() {
        launch().use {scenario ->
            load(scenario,"""<html><body><section><h2>Offline fixture</h2>
              <a aria-label="Fixture A" href="/movie/watch/fixture-a">Fixture A</a>
              <a aria-label="Fixture B" href="/movie/watch/fixture-b">Fixture B</a>
              </section></body></html>""")
            awaitView(scenario) {it is TextView && it.isShown && it.text.toString()=="Fixture B"}
            scenario.onActivity {it.findViewById<View>(R.id.catalog_rows).requestFocus()}
            instrument.waitForIdleSync()
            instrument.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_RIGHT)
            instrument.waitForIdleSync()
            instrument.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
            instrument.waitForIdleSync()
            awaitView(scenario) {it is Button && it.isShown && it.text.toString()=="Ansehen"}
            scenario.onActivity {activity -> assertTrue(views(activity.window.decorView).filterIsInstance<Button>()
                .any {it.isShown && it.text.toString()=="Ansehen"})}
            instrument.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            instrument.waitForIdleSync()
            scenario.onActivity {assertEquals(View.VISIBLE,it.findViewById<View>(R.id.catalog_rows).visibility)}
            screenshot("native-cards-offline")
            instrument.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
            awaitView(scenario) {it is Button && it.isShown && it.text.toString()=="Ansehen"}
            scenario.onActivity {activity -> assertTrue(views(activity.window.decorView).filterIsInstance<TextView>()
                .any {it.isShown && it.text.toString()=="Fixture B"})}
        }
    }
}
