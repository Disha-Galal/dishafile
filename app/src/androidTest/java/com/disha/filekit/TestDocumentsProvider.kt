package com.disha.filekit

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import java.io.File
import java.io.FileNotFoundException

class TestDocumentsProvider : DocumentsProvider() {
    private val dir get() = File(context!!.filesDir,"provider").apply { mkdirs() }
    override fun onCreate(): Boolean {
        File(dir,"readonly.txt").writeText("read-only عربي")
        File(dir,"sample.json").writeText("{\"message\":\"أهلا\"}\r\n")
        return true
    }
    override fun queryRoots(projection: Array<out String>?): Cursor = MatrixCursor(arrayOf(Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_TITLE, Root.COLUMN_FLAGS, Root.COLUMN_MIME_TYPES)).apply {
        addRow(arrayOf("root","root","Disha test documents",Root.FLAG_SUPPORTS_CREATE,"*/*"))
    }
    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor = cursor().apply { addDocument(this,documentId) }
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<out String>?, sortOrder: String?): Cursor = cursor().apply {
        dir.listFiles()?.forEach { addDocument(this,it.name) }
    }
    override fun openDocument(documentId: String, mode: String, signal: CancellationSignal?): ParcelFileDescriptor {
        if (documentId == "readonly.txt" && mode != "r") throw FileNotFoundException("Read-only provider")
        return ParcelFileDescriptor.open(file(documentId), ParcelFileDescriptor.parseMode(mode))
    }
    override fun createDocument(parentDocumentId: String, mimeType: String, displayName: String): String {
        val id = "${System.nanoTime()}-${displayName.replace('/','_')}"
        file(id).writeText("")
        return id
    }
    private fun file(id: String): File {
        if(id.contains('/') || id.contains("..")) throw FileNotFoundException()
        return File(dir,id)
    }
    private fun cursor() = MatrixCursor(arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS, Document.COLUMN_SIZE))
    private fun addDocument(cursor: MatrixCursor,id: String) {
        if (id=="root") cursor.addRow(arrayOf(id,"Test files",Document.MIME_TYPE_DIR,Document.FLAG_DIR_SUPPORTS_CREATE,0))
        else {
            val file = file(id)
            cursor.addRow(arrayOf(id,id,if(id.endsWith("json")) "application/json" else "text/plain",if(id=="readonly.txt") 0 else Document.FLAG_SUPPORTS_WRITE,file.length()))
        }
    }
}
