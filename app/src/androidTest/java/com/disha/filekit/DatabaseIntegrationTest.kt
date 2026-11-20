// This project By Gogo & Disha ®
package com.disha.filekit

import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.disha.filekit.core.FileProblem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DatabaseIntegrationTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun uri(name: String): Uri {
        val uri = DocumentsContract.buildDocumentUri("com.disha.filekit.test.documents", name)
        context.startActivity(Intent().setClassName("com.disha.filekit.test", "com.disha.filekit.GrantTestUriActivity")
            .putExtra("uri", uri.toString()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val end = System.currentTimeMillis() + 10000
        while (System.currentTimeMillis() < end) {
            if (context.checkUriPermission(uri, android.os.Process.myPid(), android.os.Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) == android.content.pm.PackageManager.PERMISSION_GRANTED) return uri
            Thread.sleep(100)
        }
        throw AssertionError("Provider grant timed out")
    }
    private fun problem(kind: String, action: () -> Unit) {
        try { action(); fail("Expected $kind") } catch (e: FileProblem) { assertEquals(kind, e.kind) }
    }
    @Test fun readOnlyDatabasePagesArabicLargeIntegersAndQuotedIdentifiersWithoutChangingSource() {
        val source = uri("sample.db")
        val store = DocumentStore(context)
        val hash = store.digest(source)
        assertFalse(store.meta(source).writable)
        val before = context.cacheDir.listFiles()!!.count { it.name.startsWith("sqlite-") }
        DatabaseStore(context).open(source, "sample.db")!!.use { snapshot ->
            assertFalse(snapshot.tables.contains("excluded_view"))
            assertFalse(snapshot.tables.contains("android_metadata"))
            val first = snapshot.page("people", 0)
            assertEquals(50, first.getJSONArray("rows").length())
            assertTrue(first.getBoolean("hasMore"))
            val row = first.getJSONArray("rows").getJSONArray(0)
            assertEquals(Long.MAX_VALUE.toString(), row.getJSONObject(0).getString("value"))
            assertEquals("عربي 0", row.getJSONObject(1).getString("value"))
            assertTrue(row.getJSONObject(2).getBoolean("truncated"))
            val last = snapshot.page("people", 50)
            assertEquals(5, last.getJSONArray("rows").length())
            assertFalse(last.getBoolean("hasMore"))
            assertTrue(snapshot.page("z_odd\"table", 0).toString().contains("window.pwned"))
            problem("databaseInvalid") { snapshot.page("people; DROP TABLE people", 0) }
            problem("databaseInvalid") { snapshot.page("people", -1) }
            problem("databaseEncoding") { snapshot.page("z_invalid_text", 0) }
        }
        assertEquals(before, context.cacheDir.listFiles()!!.count { it.name.startsWith("sqlite-") })
        assertEquals(hash, store.digest(source))
        DatabaseStore(context).open(uri("utf16.db"), "utf16.db")!!.use { snapshot ->
            assertEquals("عربي 😀", snapshot.page("texts", 0).getJSONArray("rows").getJSONArray(0).getJSONObject(0).getString("value"))
        }
    }
    @Test fun malformedAndUnsupportedDatabasesRefuseAndCleanTemporaryCopies() {
        val source = uri("invalid.db")
        val before = context.cacheDir.listFiles()!!.count { it.name.startsWith("sqlite-") }
        problem("databaseInvalid") { DatabaseStore(context).open(source, "invalid.db") }
        problem("databaseUnsupported") { DatabaseStore(context).open(uri("encrypted.db"), "encrypted.db") }
        problem("databaseLimit") { DatabaseStore(context).open(uri("oversized.db"), "oversized.db") }
        assertEquals(before, context.cacheDir.listFiles()!!.count { it.name.startsWith("sqlite-") })
    }
    @Test fun cleanupRemovesOnlyExpiredExportsAndOrphanedDatabaseSnapshots() {
        val store = DocumentStore(context)
        val recovery = File(store.recoveryDir, "cleanup-keep.json").apply { writeText("draft") }
        val orphan = File(context.cacheDir, "sqlite-cleanup-test").apply { mkdirs(); File(this,"snapshot.db").writeText("temp") }
        val old = File(context.cacheDir, "exports/cleanup-old").apply { mkdirs(); File(this,"old.txt").writeText("old"); setLastModified(1000) }
        val recent = File(context.cacheDir, "exports/cleanup-recent").apply { mkdirs(); File(this,"new.txt").writeText("new") }
        store.cleanTemporaryFiles()
        assertFalse(old.exists()); assertFalse(orphan.exists()); assertTrue(recent.exists()); assertEquals("draft", recovery.readText())
        recovery.delete(); recent.deleteRecursively()
    }
    private fun waitFor(scenario: ActivityScenario<MainActivity>, script: String) {
        val end = System.currentTimeMillis() + 20000
        while (System.currentTimeMillis() < end) {
            val latch = CountDownLatch(1)
            var answer = false
            scenario.onActivity { it.web.evaluateJavascript(script) { value -> answer = value == "true"; latch.countDown() } }
            assertTrue(latch.await(5, TimeUnit.SECONDS))
            if (answer) return
            Thread.sleep(150)
        }
        fail("Condition timed out: $script")
    }
    @Test fun actionViewDatabaseKeepsSavePathUnavailableAndCleansUpOnHome() {
        val source = uri("sample.db")
        val store = DocumentStore(context)
        val hash = store.digest(source)
        val intent = Intent(context, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setDataAndType(source, "application/vnd.sqlite3")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            waitFor(scenario, "Boolean(window.Disha && !document.querySelector('#databaseViewer').hidden && document.querySelector('#databaseRows').textContent.includes('عربي'))")
            waitFor(scenario, "Boolean(Disha.snapshot()===null && document.querySelector('#workspace').hidden)")
            scenario.onActivity { it.web.evaluateJavascript("Native.postMessage(JSON.stringify({action:'save'}));Native.postMessage(JSON.stringify({action:'saveAs'}));Native.postMessage(JSON.stringify({action:'home'}));", null) }
            waitFor(scenario, "Boolean(!document.querySelector('#home').hidden && document.querySelector('#databaseRows').textContent==='')")
        }
        assertEquals(hash, store.digest(source))
    }
}
