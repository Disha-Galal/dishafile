package com.disha.filekit

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import java.net.ServerSocket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class AiIntegrationTest {
    @Test fun directGeminiWithoutSavedKeyReportsSetupErrorWithoutSendingText() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("ai", android.content.Context.MODE_PRIVATE)
        val original = prefs.all.toMap()
        try {
            prefs.edit().clear().commit()
            androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val events = mutableListOf<JSONObject>()
                    val controller = AiController(activity, { events.add(it) }, { true })
                    try {
                        controller.request(JSONObject().put("requestId", "no-key").put("provider", "gemini")
                            .put("content", "Hello world").put("instruction", "Translate to Arabic"))
                        assertEquals("aiError", events.single().getString("type"))
                        assertTrue(events.single().getString("text").contains("No saved Gemini key"))
                    } finally { controller.destroy() }
                }
            }
        } finally {
            val edit = prefs.edit().clear()
            original.forEach { (key, value) -> if (value is String) edit.putString(key, value) }
            edit.commit()
        }
    }
    @Test fun realLoopbackTransportFallsBackAndPreservesArabicWithoutCloud() {
        val requests = mutableListOf<JSONObject>()
        ServerSocket(0, 2, java.net.InetAddress.getByName("127.0.0.1")).use { server ->
            server.soTimeout = 10000
            val executor = Executors.newSingleThreadExecutor()
            val fake = executor.submit {
                repeat(2) { index -> server.accept().use { socket ->
                    socket.soTimeout = 10000
                    val input = socket.getInputStream().bufferedReader(Charsets.UTF_8)
                    assertEquals("POST /api/generate HTTP/1.1", input.readLine())
                    var length = 0
                    while (true) {
                        val line = input.readLine()
                        if (line.isEmpty()) break
                        if (line.startsWith("Content-Length:", true)) length = line.substringAfter(':').trim().toInt()
                        assertFalse(line.startsWith("x-goog-api-key:", true))
                    }
                    // محتوى الطلب عربي؛ نقرأ عدد البايتات عبر عدد الحروف حتى يغلق الجسم المنطقي.
                    val body = StringBuilder()
                    while (body.toString().toByteArray(Charsets.UTF_8).size < length) body.append(input.read().toChar())
                    requests.add(JSONObject(body.toString()))
                    val payload = if (index == 0) "{}" else JSONObject().put("done", true).put("done_reason", "stop").put("response", "اقتراح عربي").toString()
                    val bytes = payload.toByteArray(Charsets.UTF_8)
                    val status = if (index == 0) "503 Unavailable" else "200 OK"
                    socket.getOutputStream().apply { write("HTTP/1.1 $status\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray()); write(bytes); flush() }
                } }
            }
            try {
                val config = AiClient.Config("http://127.0.0.1:${server.localPort}", "llama3.2:3b", "qwen2.5:0.5b", "gemini-2.5-flash", "unused-secret")
                val result = AiClient().generate(config, "اشرح", "نص عربي", false) { }
                fake.get(15, TimeUnit.SECONDS)
                assertEquals("qwen2.5:0.5b", result.model); assertEquals("اقتراح عربي", result.text)
                assertEquals(listOf("llama3.2:3b"), result.failures)
                assertEquals(2, requests.size)
                assertTrue(requests.all { !it.getBoolean("stream") && it.getString("prompt").contains("نص عربي") })
            } finally { executor.shutdownNow() }
        }
    }
    @Test fun geminiKeyIsEncryptedAndCanBeRemoved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("ai", android.content.Context.MODE_PRIVATE)
        val original = prefs.all.toMap()
        try {
            prefs.edit().clear().commit()
            val store = AiSettings(context)
            store.save(store.config(), "test-secret-عربي", false)
            assertEquals("test-secret-عربي", store.config().key)
            assertFalse(prefs.getString("secret", "")!!.contains("test-secret"))
            assertEquals("", store.config(false).key)
            prefs.edit().putString("secret", "invalid-ciphertext").commit()
            store.save(store.config(false), "", true)
            assertEquals("", store.config().key)
        } finally {
            val edit = prefs.edit().clear()
            original.forEach { (key, value) -> if (value is String) edit.putString(key, value) }
            edit.commit()
        }
    }
    @Test fun cancelledClientDoesNotSendOrFallback() {
        val client = AiClient(); client.cancel()
        try {
            client.generate(AiClient.Config("http://127.0.0.1:11434", "llama", "qwen", "gemini", ""), "اشرح", "عربي", false) { fail("Cancelled request must not send") }
            fail("Expected cancellation")
        } catch (_: InterruptedException) { }
    }
}
