package com.disha.filekit

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.json.JSONObject

@RunWith(AndroidJUnit4::class)
class EditorSmokeTest {
    private fun js(scenario: ActivityScenario<MainActivity>, code: String): String {
        val latch = CountDownLatch(1)
        var answer = ""
        scenario.onActivity { it.web.evaluateJavascript(code) { value -> answer = value; latch.countDown() } }
        assertTrue("WebView callback timed out", latch.await(10, TimeUnit.SECONDS))
        return answer
    }
    private fun waitFor(scenario: ActivityScenario<MainActivity>, code: String) {
        val end = System.currentTimeMillis() + 20000
        while (System.currentTimeMillis() < end) {
            if (js(scenario, code) == "true") return
            Thread.sleep(200)
        }
        fail("Condition timed out: $code")
    }
    @Test fun readingPreferencesSurviveActivityRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE)
        val oldSize = prefs.getInt("fontSize", 15)
        val oldCollapsed = prefs.getBoolean("controlsCollapsed", false)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                waitFor(scenario, "Boolean(window.Disha && window.Disha.ready)")
                js(scenario, "Native.postMessage(JSON.stringify({action:'settings',lang:'ar',dark:true,wrap:true,fontSize:23,controlsCollapsed:true}))")
                waitFor(scenario, "Boolean(window.Disha.ready)")
                scenario.recreate()
                waitFor(scenario, "Boolean(window.Disha && window.Disha.ready && document.querySelector('#workspace').classList.contains('collapsed'))")
                val data = JSONObject().put("type","load").put("docId","preferences").put("name","sample.php")
                    .put("text","<?php echo 'عربي';").put("encoding","UTF-8").put("newline","CRLF")
                js(scenario, "Disha.receive($data)")
                assertEquals("\"23px\"", js(scenario, "getComputedStyle(document.querySelector('.cm-editor')).fontSize"))
                assertEquals("\"false\"", js(scenario, "document.querySelector('#toggleControls').getAttribute('aria-expanded')"))
                assertEquals(23, prefs.getInt("fontSize", 15))
            }
        } finally { prefs.edit().putInt("fontSize",oldSize).putBoolean("controlsCollapsed",oldCollapsed).commit() }
    }
    @Test fun offlineLaunchEditUndoAndInertHtml() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor(scenario, "Boolean(window.Disha && window.Disha.ready)")
            assertEquals("\"ar\"", js(scenario, "document.documentElement.lang"))
            val data = JSONObject().put("type","load").put("docId","smoke").put("name","sample.html")
                .put("text","<script>window.pwned=true</script>\nأهلا يا ديشا").put("encoding","UTF-8").put("newline","CRLF").put("preview",false)
            js(scenario, "Disha.receive($data)")
            assertEquals("false", js(scenario,"Boolean(window.pwned)"))
            assertEquals("true", js(scenario,"Disha.snapshot().text.includes('أهلا')"))
            js(scenario,"document.querySelector('.cm-content').focus(); document.execCommand('selectAll'); document.execCommand('insertText',false,'edited عربي')")
            waitFor(scenario,"Disha.snapshot().text === 'edited عربي'")
            js(scenario,"document.getElementById('undo').click()")
            waitFor(scenario,"Disha.snapshot().text.includes('<script>')")
            assertEquals("true",js(scenario,"document.querySelector('.cm-lineNumbers') !== null"))
        }
    }
    @Test fun incomingTextShareUsesNativeBridgeAndPersistsDraft() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, "نص من المشاركة").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            waitFor(scenario,"Boolean(window.Disha && Disha.snapshot() && Disha.snapshot().text === 'نص من المشاركة')")
            js(scenario,"document.querySelector('.cm-content').focus();document.execCommand('selectAll');document.execCommand('insertText',false,'مسودة محفوظة')")
            waitFor(scenario,"Disha.snapshot().text === 'مسودة محفوظة'")
            js(scenario,"Disha.receive({type:'flush'})")
            val end=System.currentTimeMillis()+10000
            var found=false
            while(System.currentTimeMillis()<end){
                found=java.io.File(context.filesDir,"recovery").listFiles()?.filter { it.extension=="json" }?.any { it.readText().contains("مسودة محفوظة") } == true
                if(found) break
                Thread.sleep(200)
            }
            assertTrue("Recovery draft should be on disk",found)
        }
    }
}
