package com.disha.filekit.core

import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

// شرائح البايتات تمنع نسخ الأجسام أثناء فهرسة الملف؛ لا تُكتب أي بيانات إلى الأصل.
internal data class FlowBytes(val source: ByteArray, val start: Int, val size: Int) {
    fun bytes() = source.copyOfRange(start, start + size)
    fun text(charset: String = "UTF-8"): String = Charset.forName(charset).newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(source, start, size)).toString()
}
internal data class FlowDict(val entries: List<Pair<String, Any?>>) {
    operator fun get(key: String): Any? = entries.firstOrNull { it.first == key }?.second
}
internal fun Any?.flowText(): String = when (this) {
    null -> "null"
    is FlowBytes -> try { text() } catch (_: Exception) { "Binary, $size bytes · Hex: " + (start until minOf(start + size, start + 32)).joinToString(" ") { "%02x".format(source[it]) } }
    else -> toString()
}

data class FlowSummary(val index: Int, val type: String, val method: String, val host: String, val path: String,
    val status: String, val requestBytes: Int, val responseBytes: Int, val timestamp: String)
class FlowDocument internal constructor(internal val records: List<FlowDict>, val sourceSize: Int) {
    val summaries: List<FlowSummary> = records.mapIndexed { index, record ->
        val request = record["request"] as? FlowDict
        val response = record["response"] as? FlowDict
        FlowSummary(index, record["type"].flowText(), request?.get("method")?.flowText() ?: "",
            request?.get("host")?.flowText() ?: "", request?.get("path")?.flowText() ?: "",
            response?.get("status_code")?.flowText() ?: "—", (request?.get("content") as? FlowBytes)?.size ?: 0,
            (response?.get("content") as? FlowBytes)?.size ?: 0, request?.get("timestamp_start")?.flowText() ?: "")
    }
    fun detail(index: Int): FlowDetail = FlowValues.detail(records[index], index)
}

object FlowTnetstringParser {
    const val FILE_LIMIT = 40 * 1024 * 1024
    const val RECORD_LIMIT = 16 * 1024 * 1024
    const val NODE_LIMIT = 200_000
    const val FLOW_LIMIT = 4096
    fun candidate(prefix: ByteArray, name: String): Boolean {
        val lower = name.lowercase()
        if (lower.substringAfterLast('.') in setOf("flow", "mitm", "mitmproxy") || lower.matches(Regex("flows?( \\(\\d+\\))?"))) return true
        val colon = prefix.indexOf(':'.code.toByte())
        return colon in 1..10 && prefix.take(colon).all { it in 48..57 }
    }
    fun parse(bytes: ByteArray): FlowDocument {
        if (bytes.size > FILE_LIMIT) throw FileProblem("flowLimit")
        val reader = Reader(bytes)
        val records = mutableListOf<FlowDict>()
        while (reader.pos < bytes.size) {
            if (records.size >= FLOW_LIMIT) throw FileProblem("flowLimit")
            val record = reader.read(bytes.size, 0) as? FlowDict ?: throw FileProblem("flowInvalid")
            val type = record["type"] as? FlowBytes ?: throw FileProblem("flowInvalid")
            val typeText = try { type.text() } catch (_: Exception) { throw FileProblem("flowInvalid") }
            if (typeText.isBlank() || typeText.length > 64) throw FileProblem("flowInvalid")
            if (typeText == "http") {
                val request = record["request"] as? FlowDict ?: throw FileProblem("flowInvalid")
                if (listOf("method", "host", "path").any { request[it] !is FlowBytes }) throw FileProblem("flowInvalid")
                if (record["response"] != null && record["response"] !is FlowDict) throw FileProblem("flowInvalid")
            }
            records.add(record)
        }
        if (records.isEmpty()) throw FileProblem("flowInvalid")
        return FlowDocument(records, bytes.size)
    }
    private class Reader(val bytes: ByteArray) {
        var pos = 0
        var nodes = 0
        fun read(end: Int, depth: Int): Any? {
            if (Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException()
            if (depth > 48 || ++nodes > NODE_LIMIT) throw FileProblem("flowLimit")
            var length = 0L
            val begin = pos
            while (pos < end && bytes[pos] in 48..57) {
                if (pos - begin >= 10) throw FileProblem("flowInvalid")
                length = length * 10 + bytes[pos++] - 48
                if (length > RECORD_LIMIT) throw FileProblem("flowLimit")
            }
            if (pos == begin || pos >= end || bytes[pos++].toInt() != 58 || (pos - begin > 2 && bytes[begin].toInt() == 48)) throw FileProblem("flowInvalid")
            if (length >= end.toLong() - pos) throw FileProblem("flowInvalid")
            val stop = pos + length.toInt()
            val tag = bytes[stop].toInt().toChar()
            val slice = FlowBytes(bytes, pos, length.toInt())
            val value: Any? = when (tag) {
                ',', ';' -> { if (tag == ';') try { slice.text() } catch (_: Exception) { throw FileProblem("flowInvalid") }; pos = stop; slice }
                '#', '^', '!', '~' -> {
                    val text = try { slice.text() } catch (_: Exception) { throw FileProblem("flowInvalid") }
                    pos = stop
                    when (tag) {
                        '#' -> { if (!text.matches(Regex("-?(0|[1-9][0-9]*)"))) throw FileProblem("flowInvalid"); text.toLongOrNull() ?: throw FileProblem("flowInvalid") }
                        '^' -> text.toDoubleOrNull()?.takeIf { it.isFinite() } ?: throw FileProblem("flowInvalid")
                        '!' -> when (text) { "true" -> true; "false" -> false; else -> throw FileProblem("flowInvalid") }
                        else -> { if (length != 0L) throw FileProblem("flowInvalid"); null }
                    }
                }
                ']' -> buildList { while (pos < stop) add(read(stop, depth + 1)) }
                '}' -> FlowDict(buildList {
                    while (pos < stop) {
                        val key = read(stop, depth + 1) as? FlowBytes ?: throw FileProblem("flowInvalid")
                        val text = try { key.text() } catch (_: Exception) { throw FileProblem("flowInvalid") }
                        if (key.size > 8192) throw FileProblem("flowLimit")
                        add(text to read(stop, depth + 1))
                    }
                })
                else -> throw FileProblem("flowInvalid")
            }
            if (pos != stop) throw FileProblem("flowInvalid")
            pos++
            return value
        }
    }
}
