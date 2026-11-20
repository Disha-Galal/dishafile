// This project By Gogo & Disha ®
package com.disha.filekit

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.net.Uri
import com.disha.filekit.core.FileProblem
import org.json.JSONArray
import org.json.JSONObject
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

class DatabaseStore(private val context: Context) {
    class Snapshot internal constructor(private val directory: File, private val database: SQLiteDatabase, val tables: List<String>) : Closeable {
        fun page(table: String, offset: Int): JSONObject {
            if (table !in tables || offset !in 0..MAX_OFFSET || offset % PAGE_SIZE != 0) throw FileProblem("databaseInvalid")
            val encoding = database.rawQuery("PRAGMA encoding", null).use { cursor -> cursor.moveToFirst(); cursor.getString(0) }
            val columns = mutableListOf<String>()
            database.rawQuery("PRAGMA table_info(${quote(table)})", null).use { cursor ->
                while (cursor.moveToNext()) {
                    if (columns.size == COLUMN_LIMIT) throw FileProblem("databaseLimit")
                    val column = cursor.getString(1)
                    if (column.length > 256) throw FileProblem("databaseLimit")
                    columns.add(column)
                }
            }
            if (columns.isEmpty()) throw FileProblem("databaseInvalid")
            val projection = columns.joinToString(",") { column ->
                val c = quote(column)
                "typeof($c),length(CAST($c AS BLOB)),CASE WHEN typeof($c)='blob' THEN hex(substr($c,1,32)) WHEN typeof($c)='text' THEN hex(substr(CAST($c AS BLOB),1,4096)) ELSE CAST($c AS TEXT) END"
            }
            val rows = JSONArray()
            var more = false
            var textBudget = 0
            try {
                database.rawQuery("SELECT $projection FROM ${quote(table)} LIMIT ${PAGE_SIZE + 1} OFFSET $offset", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        if (rows.length() == PAGE_SIZE) { more = true; break }
                        val row = JSONArray()
                        columns.indices.forEach { i ->
                            val type = cursor.getString(i * 3)
                            val length = cursor.getLong(i * 3 + 1)
                            val raw = cursor.getString(i * 3 + 2)
                            val value = if (type == "text") decodePreview(raw, length > 4096, encoding) else raw
                            textBudget += value?.length ?: 0
                            if (textBudget > 256 * 1024) throw FileProblem("databaseLimit")
                            row.put(JSONObject().put("type", type).put("value", if (type == "null") JSONObject.NULL else value)
                                .put("truncated", if (type == "blob") length > 32 else type == "text" && length > 4096))
                        }
                        rows.put(row)
                    }
                }
            } catch (_: SQLiteException) { throw FileProblem("databaseInvalid") }
            return JSONObject().put("table", table).put("offset", offset).put("columns", JSONArray(columns)).put("rows", rows)
                .put("hasMore", more && offset < MAX_OFFSET).put("limited", more && offset == MAX_OFFSET)
        }
        private fun decodePreview(hex: String, truncated: Boolean, encoding: String): String {
            val bytes = ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
            for (cut in 0..(if (truncated) 3 else 0)) {
                try {
                    return Charset.forName(encoding).newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes, 0, bytes.size - cut)).toString()
                } catch (_: java.nio.charset.CharacterCodingException) { }
            }
            throw FileProblem("databaseEncoding")
        }
        override fun close() { try { database.close() } finally { directory.deleteRecursively() } }
    }

    fun open(uri: Uri, name: String): Snapshot? {
        val stream = (context.contentResolver.openInputStream(uri) ?: throw FileProblem("permission")).buffered()
        stream.use { input ->
            input.mark(32)
            val prefix = ByteArray(16)
            var count = 0
            while (count < prefix.size) { val n = input.read(prefix, count, prefix.size - count); if (n < 0) break; if (n > 0) count += n }
            input.reset()
            val candidate = name.substringAfterLast('.', "").lowercase() in setOf("db", "db3", "sqlite", "sqlite3")
            val magic = prefix.contentEquals("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))
            if (!magic) { if (candidate) throw FileProblem("databaseUnsupported"); return null }
            val directory = File.createTempFile("sqlite-", "", context.cacheDir).apply { delete(); mkdir() }
            val file = File(directory, "snapshot.db")
            var database: SQLiteDatabase? = null
            try {
                file.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        total += n
                        if (total > FILE_LIMIT) throw FileProblem("databaseLimit")
                        if (n > 0) output.write(buffer, 0, n)
                    }
                }
                database = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS) { }
                database.execSQL("PRAGMA trusted_schema=OFF")
                val tables = mutableListOf<String>()
                database.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND rootpage>0 AND name NOT LIKE 'sqlite_%' ORDER BY name LIMIT 257", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val table = cursor.getString(0)
                        if (tables.size == 256 || table.length > 256) throw FileProblem("databaseLimit")
                        tables.add(table)
                    }
                }
                return Snapshot(directory, database, tables)
            } catch (e: Exception) {
                database?.close()
                directory.deleteRecursively()
                if (e is FileProblem) throw e
                throw FileProblem("databaseInvalid")
            }
        }
    }

    companion object {
        const val FILE_LIMIT = 40 * 1024 * 1024
        const val PAGE_SIZE = 50
        const val MAX_OFFSET = 10000
        const val COLUMN_LIMIT = 64
        private fun quote(identifier: String) = "\"${identifier.replace("\"", "\"\"")}\""
    }
}
