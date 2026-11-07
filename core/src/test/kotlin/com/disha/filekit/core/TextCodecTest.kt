package com.disha.filekit.core

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.GZIPInputStream

class TextCodecTest {
    private fun gzip(bytes: ByteArray) = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(bytes) } }.toByteArray()
    private fun open(bytes: ByteArray, name: String = "sample.txt", encoding: String? = null, preview: Boolean = false) = TextCodec.open(bytes.inputStream(), name, encoding, preview)
    private fun fails(kind: String, action: () -> Unit) {
        try { action(); fail("Expected $kind") } catch (e: FileProblem) { assertEquals(kind, e.kind) }
    }
    @Test fun arabicUtf8CrLfBomRoundTrip() {
        val bytes = byteArrayOf(0xef.toByte(), 0xbb.toByte(), 0xbf.toByte()) + "أهلا يا ديشا\r\nالسطر التاني\r\n".toByteArray()
        val file = open(bytes)
        assertEquals("CRLF", file.newline); assertTrue(file.bom)
        assertArrayEquals(bytes, TextCodec.encode(file))
    }
    @Test fun utf16BothByteOrders() {
        for ((enc, bom) in listOf("UTF-16LE" to byteArrayOf(-1,-2), "UTF-16BE" to byteArrayOf(-2,-1))) {
            val bytes = bom + "عربي 😀\r\n".toByteArray(charset(enc))
            assertArrayEquals(bytes, TextCodec.encode(open(bytes)))
        }
    }
    @Test fun windows1256RequiresExplicitChoiceAndRoundTrips() {
        val bytes = "ملف عربي\r\n".toByteArray(charset("windows-1256"))
        fails("encoding") { open(bytes) }
        val file = open(bytes, encoding="windows-1256")
        assertArrayEquals(bytes, TextCodec.encode(file))
        fails("lossy") { TextCodec.encode(file, file.text + "😀") }
    }
    @Test fun gzipDetectedByMagicAndIndependentDecoderAcceptsExport() {
        val bytes = "{\"message\":\"أهلا\"}\r\n".toByteArray()
        val file = open(gzip(bytes), "config.json.gz")
        assertTrue(file.gzip)
        val output = TextCodec.encode(file, file.text.replace("أهلا", "ديشا"))
        assertArrayEquals(String(bytes).replace("أهلا", "ديشا").toByteArray(), GZIPInputStream(output.inputStream()).readBytes())
        assertTrue(open(gzip(bytes), "config.json").gzip)
    }
    @Test fun plainTextNamedGzipIsRejected() { fails("gzip") { open("hello".toByteArray(), "bad.gz") } }
    @Test fun gzipCorruptTruncatedConcatenatedAndTrailingAreRejected() {
        val valid = gzip("select 1;".toByteArray())
        fails("gzip") { open(valid.copyOf(valid.size-1), "backup.sql.gz") }
        fails("gzip") { open(valid + valid, "backup.sql.gz") }
        fails("gzip") { open(valid + byteArrayOf(0), "backup.sql.gz") }
        val corrupt = valid.copyOf(); corrupt[corrupt.size-8] = (corrupt[corrupt.size-8].toInt() xor 1).toByte()
        fails("gzip") { open(corrupt, "backup.sql.gz") }
    }
    @Test fun decompressionIsBounded() { fails("bomb") { TextCodec.inflateSingle(gzip(ByteArray(4096) { 65 }), 2048) } }
    @Test fun productionBombLimitIsEnforced() { fails("bomb") { open(gzip(ByteArray(TextCodec.INFLATE_LIMIT+1) { 65 }), "large.txt.gz") } }
    @Test fun tarNamesAndMagicRejected() {
        fails("archive") { open(gzip("hello".toByteArray()), "backup.tar.gz") }
        val tar = ByteArray(512); "ustar".toByteArray().copyInto(tar, 257)
        fails("archive") { open(gzip(tar), "backup.txt.gz") }
    }
    @Test fun binaryInvalidUtf8AndUtf32Rejected() {
        fails("binary") { open(byteArrayOf(0, 1, 2)) }
        fails("encoding") { open(byteArrayOf(0xc3.toByte(), 0x28)) }
        fails("encoding") { open(byteArrayOf(-1,-2,0,0,65,0,0,0)) }
    }
    @Test fun largeFileOnlyHasReadOnlyPreview() {
        val bytes = ByteArray(TextCodec.EDIT_LIMIT+1) { 65 }
        fails("tooLarge") { open(bytes) }
        val preview = open(bytes, preview=true)
        assertTrue(preview.preview); assertEquals(TextCodec.PREVIEW_LIMIT, preview.text.length)
        fails("preview") { TextCodec.encode(preview) }
    }
    @Test fun previewDoesNotSplitArabicCharacters() {
        val bytes = ("a" + "ع".repeat(TextCodec.PREVIEW_LIMIT)).toByteArray()
        val preview = open(bytes, preview=true)
        assertFalse(preview.text.contains('�')); assertTrue(preview.text.startsWith("aع"))
    }
    @Test fun mixedNewlinesCannotBeSilentlyNormalizedOnSave() {
        val file = open("a\r\nb\nc\r".toByteArray())
        assertEquals("MIXED", file.newline); assertTrue(file.preview)
        fails("preview") { TextCodec.encode(file) }
    }
    @Test fun crOnlyAndNoFinalNewlineRoundTrip() {
        for (text in listOf("a\rb", "a\nb", "", "مرحبا")) assertArrayEquals(text.toByteArray(), TextCodec.encode(open(text.toByteArray())))
    }
    @Test fun unsupportedExtensionsDoNotBecomeEditable() { fails("unsupported") { open("text".toByteArray(), "app.apk") } }
    @Test fun boundedReadRejectsBeforeReturningOversizedData() { fails("tooLarge") { TextCodec.readBounded(ByteArray(100).inputStream(), 99) } }
}
