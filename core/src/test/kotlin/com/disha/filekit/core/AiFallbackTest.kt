package com.disha.filekit.core

import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class AiFallbackTest {
    private val targets = listOf(AiFallback.Target("llama"), AiFallback.Target("qwen"), AiFallback.Target("gemini", true))
    @Test fun localFailureSwitchesToSecondModel() {
        val calls = mutableListOf<String>()
        val result = AiFallback.generate(targets, false) { calls.add(it.model); if (it.model == "llama") throw IOException() else "عربي" }
        assertEquals(listOf("llama", "qwen"), calls)
        assertEquals("عربي", result.text); assertEquals(listOf("llama"), result.failures)
    }
    @Test fun cloudNeverReceivesTextWithoutPerRequestConsent() {
        val calls = mutableListOf<String>()
        try { AiFallback.generate(targets, false) { calls.add(it.model); throw IOException() }; fail() }
        catch (e: AiFallback.Unavailable) { assertEquals(listOf("llama", "qwen"), e.failures) }
        assertEquals(listOf("llama", "qwen"), calls)
    }
    @Test fun blankResponseFailsOverAndCloudIsLast() {
        val result = AiFallback.generate(targets, true) { if (it.cloud) "answer" else " " }
        assertEquals("gemini", result.model); assertEquals(listOf("llama", "qwen"), result.failures)
    }
    @Test fun cancellationNeverFallsThroughToCloud() {
        var calls = 0
        try { AiFallback.generate(targets, true) { calls++; throw InterruptedException() }; fail() }
        catch (_: InterruptedException) { assertEquals(1, calls) }
    }
    @Test fun endpointsCannotSendLocalDocumentsToRemoteHosts() {
        for (good in listOf("http://127.0.0.1:11434", "http://localhost:1234", "http://[::1]:11434")) AiFallback.localEndpoint(good)
        for (bad in listOf("https://evil.invalid", "http://127.0.0.1.evil.invalid", "http://localhost@evil.invalid", "http://localhost/path", "http://localhost?remote=true", "file:///tmp/x", "http://localhost:99999")) {
            try { AiFallback.localEndpoint(bad); fail(bad) } catch (_: IllegalArgumentException) { }
        }
    }
}
