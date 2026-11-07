package com.disha.filekit.core

import java.io.*
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.util.zip.CRC32
import java.util.zip.GZIPOutputStream
import java.util.zip.Inflater

class FileProblem(val kind: String) : IOException(kind)

data class TextFile(
    val text: String, val encoding: String = "UTF-8", val bom: Boolean = false,
    val newline: String = "LF", val gzip: Boolean = false, val preview: Boolean = false
)

object TextCodec {
    const val EDIT_LIMIT = 8 * 1024 * 1024
    const val INFLATE_LIMIT = 32 * 1024 * 1024
    const val SOURCE_LIMIT = 40 * 1024 * 1024
    const val PREVIEW_LIMIT = 256 * 1024
    private val extensions = setOf("txt", "md", "markdown", "json", "jsonl", "sql", "html", "htm", "css", "js", "xml", "yaml", "yml", "log", "csv", "ini", "conf", "sh", "py")

    fun readBounded(input: InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream(minOf(limit, 8192))
        val buffer = ByteArray(8192)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            if (n == 0) continue
            if (out.size().toLong() + n > limit) throw FileProblem("tooLarge")
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    fun open(input: InputStream, name: String, encoding: String? = null, preview: Boolean = false): TextFile {
        val lower = name.lowercase()
        if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz") || lower.endsWith(".tar")) throw FileProblem("archive")
        val source = PushbackInputStream(input, 2)
        val magic = ByteArray(2)
        var n = 0
        while (n < 2) {
            val value = source.read()
            if (value < 0) break
            magic[n++] = value.toByte()
        }
        if (n > 0) source.unread(magic, 0, n)
        val gzip = n == 2 && magic[0] == 0x1f.toByte() && magic[1] == 0x8b.toByte()
        if (lower.endsWith(".gz") && !gzip) throw FileProblem("gzip")
        val inner = if (lower.endsWith(".gz")) lower.removeSuffix(".gz") else lower
        if (inner.substringAfterLast('.', "txt") !in extensions) throw FileProblem("unsupported")
        // فحص الجزيب بالكامل حتى في المعاينة للتأكد من سلامة CRC وعدم وجود أعضاء إضافية.
        val payload = if (gzip) inflateSingle(readBounded(source, SOURCE_LIMIT))
        else if (preview) source.readNBytesCompat(PREVIEW_LIMIT + 4) else readBounded(source, EDIT_LIMIT)
        if (payload.size > EDIT_LIMIT && !preview) throw FileProblem("largeText")
        if (payload.size >= 262 && String(payload, 257, 5, Charsets.US_ASCII) == "ustar") throw FileProblem("archive")
        var bytes = if (preview && payload.size > PREVIEW_LIMIT) payload.copyOf(PREVIEW_LIMIT) else payload
        var charset = encoding ?: "UTF-8"
        var bom = false
        var skip = 0
        if (bytes.size >= 4 && ((bytes[0] == 0.toByte() && bytes[1] == 0.toByte() && bytes[2] == 0xfe.toByte() && bytes[3] == 0xff.toByte()) || (bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte() && bytes[2] == 0.toByte() && bytes[3] == 0.toByte()))) throw FileProblem("encoding")
        if (bytes.size >= 3 && bytes.take(3) == listOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte())) { charset = "UTF-8"; bom = true; skip = 3 }
        else if (bytes.size >= 2 && bytes[0] == 0xff.toByte() && bytes[1] == 0xfe.toByte()) { charset = "UTF-16LE"; bom = true; skip = 2 }
        else if (bytes.size >= 2 && bytes[0] == 0xfe.toByte() && bytes[1] == 0xff.toByte()) { charset = "UTF-16BE"; bom = true; skip = 2 }
        if (charset !in setOf("UTF-8", "UTF-16LE", "UTF-16BE", "windows-1256")) throw FileProblem("encoding")
        // ممكن المعاينة تقطع آخر حرف متعدد البايتات؛ بنشيل نهاية غير مكتملة فقط.
        var decoded: String? = null
        val attempts = if (preview && payload.size > PREVIEW_LIMIT) 4 else 1
        for (cut in 0 until attempts) {
            if (bytes.size - skip - cut < 0) break
            try {
                decoded = Charset.forName(charset).newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes, skip, bytes.size - skip - cut)).toString()
                break
            } catch (_: java.nio.charset.CharacterCodingException) { }
        }
        val text = decoded ?: throw FileProblem("encoding")
        if (text.any { it == '\u0000' || (it.code < 32 && it !in "\n\r\t\u000c") || it in '\u007f'..'\u009f' }) throw FileProblem("binary")
        val crlf = text.contains("\r\n")
        val remaining = text.replace("\r\n", "")
        val lf = remaining.contains('\n')
        val cr = remaining.contains('\r')
        val mixed = listOf(crlf, lf, cr).count { it } > 1
        val newline = when { mixed -> "MIXED"; crlf -> "CRLF"; cr -> "CR"; else -> "LF" }
        return TextFile(text.replace("\r\n", "\n").replace('\r', '\n'), charset, bom, newline, gzip, preview || mixed)
    }

    fun encode(file: TextFile, text: String = file.text): ByteArray {
        if (file.preview) throw FileProblem("preview")
        val newline = when (file.newline) { "CRLF" -> "\r\n"; "CR" -> "\r"; else -> "\n" }
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n').replace("\n", newline)
        val raw = try {
            val buffer = Charset.forName(file.encoding).newEncoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(normalized))
            ByteArray(buffer.remaining()).also { buffer.get(it) }
        } catch (_: java.nio.charset.CharacterCodingException) { throw FileProblem("lossy") }
        if (raw.size > EDIT_LIMIT) throw FileProblem("tooLarge")
        val prefix = if (!file.bom) byteArrayOf() else when (file.encoding) {
            "UTF-8" -> byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte())
            "UTF-16LE" -> byteArrayOf(0xff.toByte(), 0xfe.toByte())
            "UTF-16BE" -> byteArrayOf(0xfe.toByte(), 0xff.toByte())
            else -> byteArrayOf()
        }
        val bytes = prefix + raw
        return if (file.gzip) ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(bytes) } }.toByteArray() else bytes
    }

    // محلل عضو واحد: يمنع الجزيب المتسلسل والذيل الخفي ويفحص الحجم وCRC.
    fun inflateSingle(bytes: ByteArray, limit: Int = INFLATE_LIMIT): ByteArray {
        try {
            var pos = 0
            fun u8(): Int { if (pos >= bytes.size) throw FileProblem("gzip"); return bytes[pos++].toInt() and 255 }
            fun u16(): Int = u8() or (u8() shl 8)
            if (u8() != 31 || u8() != 139 || u8() != 8) throw FileProblem("gzip")
            val flags = u8()
            if (flags and 224 != 0) throw FileProblem("gzip")
            repeat(6) { u8() }
            if (flags and 4 != 0) { val len = u16(); repeat(len) { u8() } }
            if (flags and 8 != 0) { while (u8() != 0) { } }
            if (flags and 16 != 0) { while (u8() != 0) { } }
            if (flags and 2 != 0) {
                val crc = CRC32().apply { update(bytes, 0, pos) }.value.toInt() and 65535
                if (u16() != crc) throw FileProblem("gzip")
            }
            val inflater = Inflater(true)
            val output = ByteArrayOutputStream()
            val trailer: Int
            try {
                inflater.setInput(bytes, pos, bytes.size - pos)
                val buffer = ByteArray(8192)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buffer)
                    if (output.size().toLong() + count > limit) throw FileProblem("bomb")
                    if (count > 0) output.write(buffer, 0, count)
                    else if (!inflater.finished()) throw FileProblem("gzip")
                }
                trailer = bytes.size - inflater.remaining
            } finally { inflater.end() }
            if (trailer + 8 != bytes.size) throw FileProblem("gzip")
            fun u32(offset: Int): Long = (0..3).fold(0L) { acc, index -> acc or ((bytes[offset + index].toLong() and 255) shl (8 * index)) }
            val result = output.toByteArray()
            if (u32(trailer) != CRC32().apply { update(result) }.value || u32(trailer + 4) != result.size.toLong()) throw FileProblem("gzip")
            return result
        } catch (e: FileProblem) { throw e } catch (_: Exception) { throw FileProblem("gzip") }
    }

    private fun InputStream.readNBytesCompat(count: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (output.size() < count) {
            val n = read(buffer, 0, minOf(buffer.size, count - output.size()))
            if (n < 0) break
            if (n > 0) output.write(buffer, 0, n)
        }
        return output.toByteArray()
    }
}
