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
