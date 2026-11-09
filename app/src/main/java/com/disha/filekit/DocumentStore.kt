package com.disha.filekit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.AtomicFile
import com.disha.filekit.core.*
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class DocumentStore(private val context: Context) {
    data class Meta(val name: String, val size: Long, val writable: Boolean)
    private val resolver get() = context.contentResolver
    val recoveryDir = File(context.filesDir, "recovery").apply { mkdirs() }
    fun meta(uri: Uri): Meta {
        var name = "document.txt"
        var size = -1L
        var supportsWrite = false
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it) ?: name }
                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }?.let { size = cursor.getLong(it) }
                cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS).takeIf { it >= 0 }?.let {
                    supportsWrite = cursor.getLong(it) and DocumentsContract.Document.FLAG_SUPPORTS_WRITE.toLong() != 0L
                }
            }
        }
        val permission = context.checkUriPermission(uri, android.os.Process.myPid(), android.os.Process.myUid(), Intent.FLAG_GRANT_WRITE_URI_PERMISSION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return Meta(name, size, supportsWrite && permission)
    }
    fun persist(uri: Uri) {
        try { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        catch (_: SecurityException) { try { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { } }
    }
    fun read(uri: Uri, name: String, encoding: String? = null, preview: Boolean = false): TextFile =
        (resolver.openInputStream(uri) ?: throw FileProblem("permission")).use { TextCodec.open(it, name, encoding, preview) }
    fun readFlow(uri: Uri, name: String): FlowDocument? =
        (resolver.openInputStream(uri) ?: throw FileProblem("permission")).buffered().use { input ->
            input.mark(32)
            val prefix = ByteArray(16)
            var count = 0
            while (count < prefix.size) { val n = input.read(prefix, count, prefix.size - count); if (n < 0) break; if (n > 0) count += n }
            input.reset()
            if (!FlowTnetstringParser.candidate(prefix.copyOf(count), name)) null
            else FlowTnetstringParser.parse(TextCodec.readBounded(input, FlowTnetstringParser.FILE_LIMIT))
        }
    fun digest(uri: Uri): String = (resolver.openInputStream(uri) ?: throw FileProblem("permission")).use {
        hash(TextCodec.readBounded(it, TextCodec.SOURCE_LIMIT))
    }
    fun hash(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    fun write(uri: Uri, bytes: ByteArray) {
        // وضع wt مطلوب لعدم ترك ذيل من الملف القديم؛ الفشل لا يتحول تلقائيا لوضع أضعف.
        (resolver.openOutputStream(uri, "wt") ?: throw FileProblem("readOnly")).use { it.write(bytes); it.flush() }
        if (digest(uri) != hash(bytes)) throw FileProblem("verify")
    }
    fun backup(uri: Uri, name: String, expectedHash: String, encoding: String) {
        val bytes = (resolver.openInputStream(uri) ?: throw FileProblem("permission")).use { TextCodec.readBounded(it, TextCodec.SOURCE_LIMIT) }
        if (hash(bytes) != expectedHash) throw FileProblem("conflict")
        val id = "original-${System.currentTimeMillis()}"
        atomic(File(recoveryDir, "$id.bin"), bytes)
        atomic(File(recoveryDir, "$id.meta"), JSONObject().put("name", name).put("encoding", encoding).toString().toByteArray())
    }
    fun draft(id: String, name: String, file: TextFile) {
        val json = JSONObject().put("name", name).put("text", file.text).put("encoding", file.encoding)
            .put("bom", file.bom).put("newline", file.newline).put("gzip", file.gzip)
        atomic(File(recoveryDir, "$id.json"), json.toString().toByteArray(Charsets.UTF_8))
    }
    fun readDraft(file: File): Pair<String, TextFile> {
        val json = JSONObject(file.readText(Charsets.UTF_8))
        return json.getString("name") to TextFile(json.getString("text"), json.getString("encoding"), json.getBoolean("bom"), json.getString("newline"), json.getBoolean("gzip"))
    }
    private fun atomic(file: File, bytes: ByteArray) {
        val atomic = AtomicFile(file)
        val stream = atomic.startWrite()
        try { stream.write(bytes); atomic.finishWrite(stream) }
        catch (e: Exception) { atomic.failWrite(stream); throw e }
    }
}
