package com.disha.filekit

import com.disha.filekit.core.AiFallback
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class AiClientTest {
    private val config = AiClient.Config("http://127.0.0.1:11434", "llama3.2:3b", "qwen2.5:0.5b", "gemini-3.1-flash-lite", "private-test-key")
    private class Response(url: URL, private val status: Int, private val body: String) : HttpURLConnection(url) {
        val sent = ByteArrayOutputStream()
        var closed = false
        override fun connect() { }
        override fun disconnect() { closed = true }
        override fun usingProxy() = false
        override fun getOutputStream() = sent
        override fun getInputStream() = ByteArrayInputStream(body.toByteArray(Charsets.UTF_8))
        override fun getErrorStream() = inputStream
        override fun getResponseCode() = status
    }
    private fun gemini(text: String = "مرحبًا\r\nبالعالم", finish: String = "STOP"): String = JSONObject()
        .put("candidates", org.json.JSONArray().put(JSONObject().put("finishReason", finish)
            .put("content", JSONObject().put("parts", org.json.JSONArray()
                .put(JSONObject().put("thought", true).put("text", "hidden reasoning"))
                .put(JSONObject().put("text", text)))))).toString()

    @Test fun directGeminiSkipsOllamaAndKeepsKeyInHeaderOnly() {
        val connections = mutableListOf<Response>()
        val client = AiClient { url -> Response(url, 200, gemini()).also { connections.add(it) } }
        val result = client.generate(config.copy(endpoint = "invalid-local-address"), "ترجم للعربية", "Hello world", true, "gemini") { }
        assertEquals("مرحبًا\r\nبالعالم", result.text)
        assertEquals(1, connections.size)
        val request = connections.single()
        assertEquals("https", request.url.protocol)
        assertEquals("generativelanguage.googleapis.com", request.url.host)
        assertEquals("/v1beta/models/gemini-3.1-flash-lite:generateContent", request.url.path)
        assertNull(request.url.query)
        assertEquals(config.key, request.getRequestProperty("x-goog-api-key"))
        assertFalse(request.sent.toString("UTF-8").contains(config.key))
        assertTrue(request.sent.toString("UTF-8").contains("ترجم للعربية"))
        assertEquals(8192, JSONObject(request.sent.toString("UTF-8")).getJSONObject("generationConfig").getInt("maxOutputTokens"))
        assertFalse(request.instanceFollowRedirects)
        assertTrue(request.closed)
    }
    @Test fun runtimeFailureFallsBackLocallyAndNeverSendsCloudKey() {
        val connections = mutableListOf<Response>()
        val client = AiClient { url ->
            val first = connections.isEmpty()
            Response(url, if (first) 500 else 200, if (first) "{\"error\":\"llama-server binary not found\"}" else "{\"done\":true,\"response\":\"عربي\",\"done_reason\":\"stop\"}").also { connections.add(it) }
        }
        val result = client.generate(config, "ترجم", "Hello", false) { }
        assertEquals("qwen2.5:0.5b", result.model)
        assertEquals(listOf(AiFallback.Failure("llama3.2:3b", AiFallback.Reason.RUNTIME)), result.details)
        assertEquals(listOf("llama3.2:3b", "qwen2.5:0.5b"), connections.map { JSONObject(it.sent.toString("UTF-8")).getString("model") })
        assertTrue(connections.all { it.url.host == "127.0.0.1" && it.getRequestProperty("x-goog-api-key") == null && it.closed })
    }
    @Test fun geminiHttpFailuresAreActionableWithoutEchoingProviderSecrets() {
        for ((status, reason) in listOf(400 to AiFallback.Reason.AUTH, 401 to AiFallback.Reason.AUTH, 403 to AiFallback.Reason.AUTH, 404 to AiFallback.Reason.MODEL_NOT_FOUND, 429 to AiFallback.Reason.QUOTA, 500 to AiFallback.Reason.SERVER, 302 to AiFallback.Reason.SERVER)) {
            val client = AiClient { url -> Response(url, status, "{\"error\":\"API_KEY_INVALID private-test-key document contents\"}") }
            try { client.generate(config, "ترجم", "Hello", true, "gemini") { }; fail() }
            catch (error: AiFallback.Unavailable) {
                assertEquals(reason, error.details.single().reason)
                assertFalse(error.toString().contains(config.key))
                assertFalse(error.details.toString().contains("document contents"))
            }
        }
    }
    @Test fun malformedEmptyBlockedAndTruncatedAnswersCannotBecomeReplacements() {
        for ((body, reason) in listOf("not JSON" to AiFallback.Reason.INVALID, gemini("") to AiFallback.Reason.EMPTY,
            gemini("partial", "MAX_TOKENS") to AiFallback.Reason.INCOMPLETE,
            gemini("", "SAFETY") to AiFallback.Reason.BLOCKED,
            "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}" to AiFallback.Reason.BLOCKED)) {
            val client = AiClient { url -> Response(url, 200, body) }
            try { client.generate(config, "ترجم", "Hello", true, "gemini") { }; fail() }
            catch (error: AiFallback.Unavailable) { assertEquals(reason, error.details.single().reason) }
        }
    }
    @Test fun cloudModelDiscoveryUsesSavedKeyAndFiltersUnsupportedMethods() {
        val client = AiClient { url ->
            assertEquals("generativelanguage.googleapis.com", url.host)
            Response(url, 200, "{\"models\":[{\"name\":\"models/gemini-3.1-flash-lite\",\"supportedGenerationMethods\":[\"generateContent\"]},{\"name\":\"models/embedding\",\"supportedGenerationMethods\":[\"embedContent\"]}]}")
        }
        assertEquals(listOf("gemini-3.1-flash-lite"), client.models(config, true))
    }
    @Test fun cancelledRequestsDoNotOpenConnections() {
        val client = AiClient { fail("Cancelled request opened a connection"); throw AssertionError() }
        client.cancel()
        try { client.generate(config, "ترجم", "Hello", true, "gemini") { }; fail() }
        catch (_: InterruptedException) { }
    }
    @Test fun localRedirectNeverReceivesKeyOrFollowsRemoteLocation() {
        val client = AiClient { url -> assertEquals("127.0.0.1", url.host); Response(url, 302, "") }
        try { client.generate(config, "ترجم", "Hello", false) { }; fail() }
        catch (error: AiFallback.Unavailable) { assertEquals(2, error.details.size) }
    }
}
