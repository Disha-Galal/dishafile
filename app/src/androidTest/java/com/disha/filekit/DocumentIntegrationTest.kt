package com.disha.filekit

import android.content.Intent
import android.provider.DocumentsContract
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.disha.filekit.core.FileProblem
import com.disha.filekit.core.TextCodec
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class DocumentIntegrationTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun uri(id: String) = DocumentsContract.buildDocumentUri("com.disha.filekit.test.documents", id).also {
        instrumentation.context.grantUriPermission(context.packageName,it,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }
    @Test fun contentUriRoundTripTruncatesAndReadOnlyProviderIsDetected() {
        val store=DocumentStore(context)
        val source=uri("sample.json")
        val root=uri("root")
        val created=DocumentsContract.createDocument(instrumentation.context.contentResolver,root,"application/json","copy.json")!!
        instrumentation.context.grantUriPermission(context.packageName,created,Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val original=store.read(source,"sample.json")
        assertEquals("CRLF",original.newline)
        store.write(created,TextCodec.encode(original))
        assertEquals(original.text,store.read(created,"copy.json").text)
        store.write(created,"{}".toByteArray())
        assertEquals("{}",store.read(created,"copy.json").text)
        val readOnly=uri("readonly.txt")
        assertFalse(store.meta(readOnly).writable)
        assertEquals("read-only عربي",store.read(readOnly,"readonly.txt").text)
    }
    @Test fun backupBeforeOverwriteAndConflictDetection() {
        val store=DocumentStore(context)
        val source=uri("sample.json")
        val hash=store.digest(source)
        store.backup(source,"sample.json",hash,"UTF-8")
        assertTrue(store.recoveryDir.listFiles()!!.any { it.extension=="bin" })
        try { store.backup(source,"sample.json","wrong-hash","UTF-8"); fail("Expected conflict") }
        catch(e:FileProblem){assertEquals("conflict",e.kind)}
        assertEquals(hash,store.digest(source))
    }
    @Test fun actionViewLoadsContentUriThroughNativeLayer() {
        val source=uri("sample.json")
        val intent=Intent(context,MainActivity::class.java).setAction(Intent.ACTION_VIEW).setDataAndType(source,"application/json")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            val end=System.currentTimeMillis()+20000
            var loaded=false
            while(System.currentTimeMillis()<end){
                val latch=CountDownLatch(1)
                scenario.onActivity { activity -> activity.web.evaluateJavascript("Boolean(window.Disha && Disha.snapshot() && Disha.snapshot().text.includes('أهلا'))") { result -> loaded=result=="true";latch.countDown() } }
                assertTrue(latch.await(5,TimeUnit.SECONDS))
                if(loaded) break
                Thread.sleep(200)
            }
            assertTrue("ACTION_VIEW content URI must load Arabic text",loaded)
        }
    }
}
