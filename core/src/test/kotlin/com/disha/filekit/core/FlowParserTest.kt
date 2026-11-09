package com.disha.filekit.core

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

class FlowParserTest {
    private fun tn(value: Any?): ByteArray {
        val (payload, tag) = when (value) {
            null -> byteArrayOf() to '~'
            is ByteArray -> value to ','
            is String -> value.toByteArray() to ';'
            is Boolean -> value.toString().toByteArray() to '!'
            is Int, is Long -> value.toString().toByteArray() to '#'
            is Double -> value.toString().toByteArray() to '^'
            is List<*> -> ByteArrayOutputStream().apply { value.forEach { write(tn(it)) } }.toByteArray() to ']'
            is Map<*, *> -> ByteArrayOutputStream().apply { value.entries.forEach { write(tn(it.key)); write(tn(it.value)) } }.toByteArray() to '}'
            else -> error("fixture")
        }
        return "${payload.size}:".toByteArray() + payload + tag.code.toByte()
    }
    private fun flow(content: ByteArray = "".toByteArray(), type: String = "text/plain", encoding: String = "identity"): ByteArray = tn(mapOf(
        "type" to "http", "version" to 17, "request" to mapOf("method" to "POST", "host" to "example.test", "path" to "/?bad=%GG",
            "headers" to listOf(listOf("content-type", type), listOf("content-encoding", encoding)), "content" to content), "response" to null))
    private fun reject(bytes: ByteArray, kind: String? = null) {
        try { FlowTnetstringParser.parse(bytes); fail("Expected rejection") } catch (e: FileProblem) { if (kind != null) assertEquals(kind, e.kind) }
    }
    @Test fun goldenFourRecordsShowActualInlineValuesWithoutChangingSource() {
        val bytes = javaClass.getResourceAsStream("/four-flows.tnet")!!.readBytes()
        val original = bytes.copyOf()
        assertTrue(FlowTnetstringParser.candidate(bytes.take(16).toByteArray(), "no-extension"))
        val doc = FlowTnetstringParser.parse(bytes)
        assertEquals(4, doc.summaries.size)
        val detail = doc.detail(0)
        fun row(group: String, key: String) = detail.rows.first { it.group == group && it.key == key }.value
        assertEquals("demo@example.test", row("Form", "username"))
        assertEquals("sample123", row("Form", "password"))
        assertEquals("", row("Form", "empty"))
        assertEquals("عربي", row("Query", "search"))
        assertEquals("+ ok", row("Query", "plus"))
        assertEquals("", row("Query", "empty"))
        assertEquals(listOf("a=1", "b=2"), detail.rows.filter { it.key == "set-cookie" }.map { it.value })
        assertEquals("0", row("JSON", "items[0].id")); assertEquals("false", row("JSON", "ok")); assertEquals("null", row("JSON", "nil"))
        assertEquals(listOf("900719925474099312345", "2"), detail.rows.filter { it.key == "n" }.map { it.value })
        assertTrue(detail.extracted.any { it.type == "email" && it.value == "demo@example.test" && it.source.contains("Form > username") })
        assertTrue(detail.extracted.any { it.type == "phone" && it.value == "+201012345678" })
        assertArrayEquals(original, bytes)
    }
    @Test fun malformedLengthsTagsScalarsAndContainersAreRejected() {
        for (source in listOf("", "1:x", "1:x?", "-1:x,", "01:x,", "999999999999999999999999:", "1:1!", "1:x~", "1:x#", "1:x^", "4:1:a}", "2:\u00c3;")) reject(source.toByteArray())
        val valid = flow()
        reject(valid.copyOf(valid.size - 1)); reject(valid + "1:x,".toByteArray())
        reject("${FlowTnetstringParser.RECORD_LIMIT + 1}:".toByteArray(), "flowLimit")
        reject(ByteArray(FlowTnetstringParser.FILE_LIMIT + 1), "flowLimit")
    }
    @Test fun depthNodesAndFlowCountAreBounded() {
        var nested: Any? = null
        repeat(55) { nested = listOf(nested) }
        reject(tn(nested), "flowLimit")
        val record = tn(mapOf("type" to "tcp", "metadata" to List(FlowTnetstringParser.NODE_LIMIT) { null }))
        reject(record, "flowLimit")
        val tiny = tn(mapOf("type" to "tcp"))
        reject(ByteArrayOutputStream().apply { repeat(FlowTnetstringParser.FLOW_LIMIT + 1) { write(tiny) } }.toByteArray(), "flowLimit")
    }
    @Test fun bytesAreLosslessWhileUtf8StringTagAndKeysAreStrict() {
        val detail = FlowTnetstringParser.parse(flow(byteArrayOf(0xc3.toByte(), 0x28))).detail(0)
        assertTrue(detail.rows.any { it.type == "binary" && it.value.contains("c3 28") })
        assertFalse(detail.rows.any { it.value.contains('\uFFFD') })
        reject("1:\u00ff;".toByteArray(Charsets.ISO_8859_1))
        reject(tn(mapOf(byteArrayOf(0xff.toByte()) to "x")))
    }
    @Test fun unsupportedTypeMissingResponseAndBadQueryDoNotCrash() {
        val generic = FlowTnetstringParser.parse(tn(mapOf("type" to "tcp", "active" to false, "n" to null))).detail(0)
        assertTrue(generic.notices.any { it.contains("Unsupported") })
        val detail = FlowTnetstringParser.parse(flow()).detail(0)
        assertTrue(detail.notices.any { it.contains("query") })
        assertFalse(detail.rows.any { it.section == "Response" })
    }
    @Test fun gzipDeflateAndBombsStayBoundedAndPreserveHeaders() {
        val text = "{\"message\":\"أهلا\\r\\n\",\"empty\":\"\"}"
        val gzip = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(text.toByteArray()) } }.toByteArray()
        val deflate = ByteArrayOutputStream().also { out -> DeflaterOutputStream(out).use { it.write(text.toByteArray()) } }.toByteArray()
        for ((bytes, encoding) in listOf(gzip to "gzip", deflate to "deflate")) {
            val detail = FlowTnetstringParser.parse(flow(bytes, "application/json", encoding)).detail(0)
            assertEquals("أهلا\r\n", detail.rows.first { it.key == "message" }.value)
        }
        val bomb = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(ByteArray(FlowValues.BODY_LIMIT + 1)) } }.toByteArray()
        for ((bytes, encoding) in listOf(bomb to "gzip", byteArrayOf(1, 2, 3) to "gzip", deflate.copyOf(3) to "deflate", gzip to "br")) {
            val detail = FlowTnetstringParser.parse(flow(bytes, "text/plain", encoding)).detail(0)
            assertTrue(detail.rows.any { it.group == "Headers" }); assertTrue(detail.rows.any { it.type == "binary" })
        }
    }
    @Test fun bomCharsetCrLfAndMalformedJsonRemainExplicit() {
        val detail = FlowTnetstringParser.parse(flow(byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()) + "أهلا\r\n".toByteArray())).detail(0)
        assertEquals("أهلا\r\n", detail.rows.first { it.type == "text" }.value)
        val legacy = FlowTnetstringParser.parse(flow("عربي".toByteArray(charset("windows-1256")), "text/plain; charset=windows-1256")).detail(0)
        assertEquals("عربي", legacy.rows.first { it.type == "text" }.value)
        for (json in listOf("{\"x\":1,}", "{\"x\":1} trailing", "{\"x\":\"\\ud800\"}", "[01]", "{\"x\":\"\\u-001\"}")) {
            val bad = FlowTnetstringParser.parse(flow(json.toByteArray(), "application/json")).detail(0)
            assertFalse(bad.rows.any { it.group == "JSON" }); assertTrue(bad.notices.any { it.contains("flowJson") })
        }
    }
    @Test fun multipartFieldsAndFileSizesDoNotExposeFilePayload() {
        val body = "--abc\r\nContent-Disposition: form-data; name=\"username\"\r\n\r\nعربي\r\n--abc\r\nContent-Disposition: form-data; name=\"upload\"; filename=\"fake.bin\"\r\n\r\nSECRETFILE\r\n--abc--\r\n"
        val detail = FlowTnetstringParser.parse(flow(body.toByteArray(), "multipart/form-data; boundary=abc")).detail(0)
        assertEquals("عربي", detail.rows.first { it.key == "username" }.value)
        assertEquals("filename=fake.bin; 10 bytes", detail.rows.first { it.key == "upload" }.value)
        assertFalse(detail.rows.any { it.value.contains("SECRETFILE") })
    }
    @Test fun invalidPercentEncodingDoesNotInventAValue() {
        for (value in listOf("%-1", "%GG", "%C3%28", "%")) {
            val detail = FlowTnetstringParser.parse(flow("key=$value".toByteArray(), "application/x-www-form-urlencoded")).detail(0)
            assertFalse(detail.rows.any { it.group == "Form" })
            assertTrue(detail.rows.any { it.type == "binary" })
        }
    }
    @Test fun largePreviewScannerAndMaliciousHeadersAreLimited() {
        val detail = FlowTnetstringParser.parse(flow("a".repeat(200000).toByteArray())).detail(0)
        assertEquals(FlowValues.PREVIEW_LIMIT, detail.rows.first { it.type == "text" }.value.length)
        val large = FlowTnetstringParser.parse(flow(ByteArray(FlowValues.BODY_LIMIT + 1))).detail(0)
        assertTrue(large.rows.any { it.type == "binary" })
        val hostile = tn(mapOf("type" to "http", "request" to mapOf("method" to "GET", "path" to "/", "host" to "example.test", "headers" to listOf(listOf("x", "a".repeat(65537))))))
        try { FlowTnetstringParser.parse(hostile).detail(0); fail() } catch (e: FileProblem) { assertEquals("flowLimit", e.kind) }
    }
}
