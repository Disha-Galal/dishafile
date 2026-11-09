package com.disha.filekit.core

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.zip.Inflater

// كل صف يحتفظ بالمصدر والقيمة الحقيقية؛ الإخفاء يخص العرض فقط.
data class FlowRow(val section: String, val group: String, val key: String, val value: String, val source: String,
    val sensitive: Boolean = false, val type: String = "value")
data class FlowDetail(val index: Int, val rows: List<FlowRow>, val extracted: List<FlowRow>, val notices: List<String>)

object FlowValues {
    const val BODY_LIMIT = 1024 * 1024
    const val PREVIEW_LIMIT = 64 * 1024
    const val ROW_LIMIT = 5000
    const val MATCH_LIMIT = 1000
    private val secret = Regex("password|passwd|pwd|token|authorization|cookie|api[_-]?key|session", RegexOption.IGNORE_CASE)
    private val email = Regex("[A-Za-z0-9.!#\u0024%&'*+/=?^_`{|}~-]{1,64}@[A-Za-z0-9-]{1,63}(?:\\.[A-Za-z0-9-]{1,63})+")
    private val phone = Regex("(?<![\\w+])(?:\\+\\d[\\d ()-]{7,23}\\d|(?:\\+?20)?01[0125][0-9]{8})(?!\\w)")
    private val url = Regex("https?://[^\\s<>\"']{1,2048}")

    internal fun detail(record: FlowDict, index: Int): FlowDetail {
        val rows = mutableListOf<FlowRow>()
        val notes = mutableListOf<String>()
        fun add(section: String, group: String, key: String, value: String, type: String = "value", inheritedSecret: Boolean = false) {
            if (rows.size < ROW_LIMIT) rows.add(FlowRow(section, group, key, value, "Flow #${index + 1} > $section > $group > $key", inheritedSecret || secret.containsMatchIn(key) || (key == "path" && secret.containsMatchIn(value)), type))
        }
        for ((key, value) in record.entries) if (key !in setOf("request", "response")) {
            if (value !is FlowDict && value !is List<*>) add("Overview", "Metadata", key, value.flowText())
        }
        if (record["websocket"] != null) notes.add("بيانات WebSocket غير مدعومة / WebSocket data is not decoded")
        if (record["type"].flowText() != "http") notes.add("نوع غير مدعوم / Unsupported flow type: ${record["type"].flowText()}")
        for (section in listOf("Request", "Response")) {
            val message = record[section.lowercase()] as? FlowDict ?: continue
            for ((key, value) in message.entries) if (key !in setOf("headers", "content", "trailers")) {
                if (value !is FlowDict && value !is List<*>) add(section, "Metadata", key, value.flowText())
            }
            val headers = mutableListOf<Pair<String, String>>()
            for (group in listOf("headers", "trailers")) {
                val list = message[group] as? List<*> ?: continue
                if (list.size > ROW_LIMIT) throw FileProblem("flowLimit")
                for (item in list) {
                    val pair = item as? List<*> ?: throw FileProblem("flowInvalid")
                    if (pair.size != 2 || pair.any { it !is FlowBytes }) throw FileProblem("flowInvalid")
                    val key = pair[0].flowText()
                    val value = pair[1].flowText()
                    if (key.length > 8192 || value.length > 64 * 1024) throw FileProblem("flowLimit")
                    add(section, if (group == "headers") "Headers" else "Trailers", key, value)
                    if (group == "headers") headers.add(key to value)
                }
            }
            if (section == "Request") {
                val path = message["path"].flowText()
                if (path.contains('?')) try {
                    pairs(path.substringAfter('?').substringBefore('#'), "UTF-8").forEach { (key, value) -> add(section, "Query", key, value) }
                } catch (_: Exception) { notes.add("ترميز Query غير صالح / Invalid query encoding") }
            }
            val body = message["content"] as? FlowBytes ?: continue
            if (body.size == 0) { add(section, "Body", "content", ""); continue }
            fun header(key: String) = headers.firstOrNull { it.first.equals(key, true) }?.second ?: ""
            val contentType = header("content-type")
            val media = contentType.substringBefore(';').trim().lowercase()
            val encoding = header("content-encoding").trim().lowercase()
            try {
                if (body.size > BODY_LIMIT) throw FileProblem("flowBodyLimit")
                val decodedBytes = when (encoding) {
                    "", "identity" -> body.bytes()
                    "gzip" -> TextCodec.inflateSingle(body.bytes(), BODY_LIMIT)
                    "deflate" -> inflate(body.bytes())
                    else -> throw FileProblem("flowEncoding")
                }
                if (media == "multipart/form-data") {
                    multipart(decodedBytes, contentType) { key, value -> add(section, "Multipart", key, value) }
                    continue
                }
                val charset = parameter(contentType, "charset") ?: "UTF-8"
                val text = decode(decodedBytes, charset).removePrefix("\uFEFF")
                if (text.any { it == '\u0000' || (it.code < 32 && it !in "\t\r\n\u000c") }) throw FileProblem("binary")
                if (media == "application/x-www-form-urlencoded") {
                    pairs(text, charset).forEach { (key, value) -> add(section, "Form", key, value) }
                } else if (media == "application/json" || media.endsWith("+json")) {
                    // التحليل المؤقت يمنع عرض صفوف جزئية من JSON تالف.
                    JsonRows(text).parse().forEach { (key, value) -> add(section, "JSON", key, value) }
                }
                add(section, "Body", "text preview", text.take(PREVIEW_LIMIT), "text", true)
                if (text.length > PREVIEW_LIMIT) notes.add("معاينة النص محدودة / Text preview limited to 64 Ki characters")
            } catch (e: Exception) {
                notes.add("$section: تعذر فك الجسم بأمان / Body unavailable (${(e as? FileProblem)?.kind ?: "encoding"}); raw hex preview")
                val sample = FlowBytes(body.source, body.start, minOf(body.size, 64))
                val hex = (sample.start until sample.start + sample.size).joinToString(" ") { "%02x".format(sample.source[it]) }
                add(section, "Body", "binary", "Binary, ${body.size} bytes · Hex preview: $hex", "binary")
            }
        }
        if (rows.size >= ROW_LIMIT) notes.add("تم بلوغ حد الصفوف / Row limit reached: $ROW_LIMIT")
        val extracted = mutableListOf<FlowRow>()
        val seen = mutableSetOf<String>()
        var matches = 0
        for (row in rows) {
            if (Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException()
            if (row.type != "text" && row.type != "binary") {
                if (extracted.size < ROW_LIMIT) extracted.add(row)
            }
            if (row.type == "binary") continue
            for ((type, pattern) in listOf("email" to email, "phone" to phone, "url" to url)) {
                for (match in pattern.findAll(row.value.take(PREVIEW_LIMIT))) {
                    if (matches >= MATCH_LIMIT || extracted.size >= ROW_LIMIT) break
                    val value = match.value
                    val digits = value.count { it.isDigit() }
                    if (type == "phone" && digits !in 10..15) continue
                    if (!seen.add("${row.source}|$type|$value")) continue
                    matches++
                    extracted.add(row.copy(key = type, value = value, type = type))
                }
            }
        }
        if (extracted.size >= ROW_LIMIT || matches >= MATCH_LIMIT) notes.add("نتائج الاستخراج محدودة / Extraction limit reached")
        return FlowDetail(index, rows, extracted, notes)
    }
    private fun parameter(header: String, key: String): String? = Regex("(?:^|;)\\s*${Regex.escape(key)}\\s*=\\s*(?:\"([^\"]*)\"|([^;\\s]*))", RegexOption.IGNORE_CASE)
        .find(header)?.let { it.groups[1]?.value ?: it.groups[2]?.value }
    private fun decode(bytes: ByteArray, charset: String): String = Charset.forName(charset).newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
    private fun component(text: String, charset: String): String {
        val output = ByteArrayOutputStream()
        var pos = 0
        while (pos < text.length) {
            when (text[pos]) {
                '%' -> { if (pos + 2 >= text.length) throw FileProblem("flowInvalid"); val hex = text.substring(pos + 1, pos + 3); if (!hex.all { it in "0123456789abcdefABCDEF" }) throw FileProblem("flowInvalid"); val n = hex.toInt(16); output.write(n); pos += 3 }
                '+' -> { output.write(32); pos++ }
                else -> {
                    val end = text.indexOfAny(charArrayOf('%', '+'), pos).let { if (it < 0) text.length else it }
                    val encoder = Charset.forName(charset).newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    val encoded = encoder.encode(java.nio.CharBuffer.wrap(text.substring(pos, end)))
                    while (encoded.hasRemaining()) output.write(encoded.get().toInt())
                    pos = end
                }
            }
        }
        return decode(output.toByteArray(), charset)
    }
    private fun pairs(text: String, charset: String): List<Pair<String, String>> {
        if (text.isEmpty()) return emptyList()
        val parts = text.split('&', limit = ROW_LIMIT + 2)
        if (parts.size > ROW_LIMIT) throw FileProblem("flowLimit")
        return parts.map { component(it.substringBefore('='), charset) to component(if (it.contains('=')) it.substringAfter('=') else "", charset) }
    }
    private fun inflate(bytes: ByteArray): ByteArray {
        val inflater = Inflater()
        try {
            inflater.setInput(bytes)
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                if (output.size() + count > BODY_LIMIT) throw FileProblem("flowBodyLimit")
                if (count == 0 && !inflater.finished()) throw FileProblem("flowEncoding")
                output.write(buffer, 0, count)
            }
            if (inflater.remaining != 0) throw FileProblem("flowEncoding")
            return output.toByteArray()
        } finally { inflater.end() }
    }
    private fun multipart(bytes: ByteArray, contentType: String, add: (String, String) -> Unit) {
        val boundary = parameter(contentType, "boundary")?.takeIf { it.isNotEmpty() && it.length <= 200 && !it.contains('\r') && !it.contains('\n') } ?: throw FileProblem("flowInvalid")
        val raw = String(bytes, Charsets.ISO_8859_1)
        val marker = "--$boundary"
        var pos = raw.indexOf(marker)
        var count = 0
        if (pos != 0) throw FileProblem("flowInvalid")
        val result = mutableListOf<Pair<String, String>>()
        while (true) {
            pos += marker.length
            if (raw.startsWith("--", pos)) break
            if (++count > ROW_LIMIT || !raw.startsWith("\r\n", pos)) throw FileProblem("flowInvalid")
            val headerEnd = raw.indexOf("\r\n\r\n", pos + 2)
            if (headerEnd < 0 || headerEnd - pos > 16 * 1024) throw FileProblem("flowInvalid")
            val headers = decode(bytes.copyOfRange(pos + 2, headerEnd), "UTF-8")
            val disposition = headers.lineSequence().firstOrNull { it.startsWith("content-disposition:", true) } ?: throw FileProblem("flowInvalid")
            val key = parameter(disposition, "name") ?: throw FileProblem("flowInvalid")
            val filename = parameter(disposition, "filename")
            val begin = headerEnd + 4
            val next = raw.indexOf("\r\n$marker", begin)
            if (next < 0) throw FileProblem("flowInvalid")
            if (filename != null) result.add(key to "filename=$filename; ${next - begin} bytes")
            else {
                val charset = parameter(headers.lineSequence().firstOrNull { it.startsWith("content-type:", true) } ?: "", "charset") ?: "UTF-8"
                result.add(key to decode(bytes.copyOfRange(begin, next), charset))
            }
            pos = next + 2
        }
        result.forEach { (key, value) -> add(key, value) }
    }
}

// محلل JSON صغير يحافظ على الأرقام حرفيا والمفاتيح المكررة دون مكتبة إضافية.
private class JsonRows(val text: String) {
    private var pos = 0
    private var nodes = 0
    private val rows = mutableListOf<Pair<String, String>>()
    fun parse(): List<Pair<String, String>> { value("$", 0); whitespace(); if (pos != text.length) fail(); return rows }
    private fun fail(): Nothing = throw FileProblem("flowJson")
    private fun whitespace() { while (pos < text.length && text[pos] in " \r\n\t") pos++ }
    private fun consume(c: Char): Boolean { whitespace(); if (pos < text.length && text[pos] == c) { pos++; return true }; return false }
    private fun string(): String {
        if (!consume('"')) fail()
        val out = StringBuilder()
        while (pos < text.length) {
            val c = text[pos++]
            if (c == '"') {
                val s = out.toString()
                var i = 0
                while (i < s.length) { if (s[i].isHighSurrogate()) { if (i + 1 >= s.length || !s[i + 1].isLowSurrogate()) fail(); i++ } else if (s[i].isLowSurrogate()) fail(); i++ }
                return s
            }
            if (c < ' ') fail()
            if (c != '\\') { out.append(c); continue }
            if (pos >= text.length) fail()
            when (val escape = text[pos++]) {
                '"', '\\', '/' -> out.append(escape)
                'b' -> out.append('\b'); 'f' -> out.append('\u000c'); 'n' -> out.append('\n'); 'r' -> out.append('\r'); 't' -> out.append('\t')
                'u' -> { if (pos + 4 > text.length) fail(); val hex = text.substring(pos, pos + 4); if (!hex.all { it in "0123456789abcdefABCDEF" }) fail(); val n = hex.toInt(16); out.append(n.toChar()); pos += 4 }
                else -> fail()
            }
        }
        fail()
    }
    private fun value(path: String, depth: Int) {
        if (Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException()
        if (depth > 48 || ++nodes > FlowValues.ROW_LIMIT) throw FileProblem("flowLimit")
        whitespace()
        if (pos >= text.length) fail()
        when (text[pos]) {
            '{' -> {
                pos++
                if (consume('}')) { rows.add(path to "{}"); return }
                do {
                    val key = string()
                    if (!consume(':')) fail()
                    val next = if (key.matches(Regex("[A-Za-z_][A-Za-z0-9_]*"))) { if (path == "$") key else "$path.$key" } else "$path[${quote(key)}]"
                    value(next, depth + 1)
                } while (consume(','))
                if (!consume('}')) fail()
            }
            '[' -> {
                pos++
                if (consume(']')) { rows.add(path to "[]"); return }
                var index = 0
                do { value("$path[${index++}]", depth + 1) } while (consume(','))
                if (!consume(']')) fail()
            }
            '"' -> rows.add(path to string())
            else -> {
                val start = pos
                while (pos < text.length && text[pos] !in " \r\n\t,]}") pos++
                val token = text.substring(start, pos)
                if (token !in listOf("null", "false", "true") && !token.matches(Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?"))) fail()
                rows.add(path to token)
            }
        }
    }
    private fun quote(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""
}
