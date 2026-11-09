package com.disha.filekit;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// مزود حقيقي داخل APK الاختبارات؛ يستخدم واجهات أندرويد وجافا فقط في عمليته المنفصلة.
public final class TestDocumentsProvider extends DocumentsProvider {
    private File directory() {
        File dir = new File(getContext().getFilesDir(), "provider");
        dir.mkdirs();
        return dir;
    }
    private void write(File file, String text) {
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) { throw new IllegalStateException(error); }
    }
    @Override public boolean onCreate() {
        write(new File(directory(), "readonly.txt"), "read-only عربي");
        write(new File(directory(), "sample.json"), "{\"message\":\"أهلا\"}\r\n");
        try (java.io.InputStream input = getContext().getAssets().open("four-flows.tnet");
             FileOutputStream output = new FileOutputStream(new File(directory(), "flows (2)"))) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        } catch (IOException error) { throw new IllegalStateException(error); }
        return true;
    }
    @Override public Cursor queryRoots(String[] projection) {
        MatrixCursor cursor = new MatrixCursor(new String[]{Root.COLUMN_ROOT_ID, Root.COLUMN_DOCUMENT_ID, Root.COLUMN_TITLE, Root.COLUMN_FLAGS, Root.COLUMN_MIME_TYPES});
        cursor.addRow(new Object[]{"root", "root", "Disha test documents", Root.FLAG_SUPPORTS_CREATE, "*/*"});
        return cursor;
    }
    private MatrixCursor cursor() {
        return new MatrixCursor(new String[]{Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS, Document.COLUMN_SIZE});
    }
    private File file(String id) throws FileNotFoundException {
        if (id.contains("/") || id.contains("..")) throw new FileNotFoundException();
        return new File(directory(), id);
    }
    private void addDocument(MatrixCursor cursor, String id) throws FileNotFoundException {
        if (id.equals("root")) cursor.addRow(new Object[]{id, "Test files", Document.MIME_TYPE_DIR, Document.FLAG_DIR_SUPPORTS_CREATE, 0});
        else cursor.addRow(new Object[]{id, id, id.endsWith("json") ? "application/json" : "text/plain", (id.equals("readonly.txt") || id.equals("flows (2)")) ? 0 : Document.FLAG_SUPPORTS_WRITE, file(id).length()});
    }
    @Override public Cursor queryDocument(String id, String[] projection) throws FileNotFoundException {
        MatrixCursor cursor = cursor(); addDocument(cursor, id); return cursor;
    }
    @Override public Cursor queryChildDocuments(String parentId, String[] projection, String sortOrder) throws FileNotFoundException {
        MatrixCursor cursor = cursor();
        File[] files = directory().listFiles();
        if (files != null) for (File file : files) addDocument(cursor, file.getName());
        return cursor;
    }
    @Override public ParcelFileDescriptor openDocument(String id, String mode, CancellationSignal signal) throws FileNotFoundException {
        if ((id.equals("readonly.txt") || id.equals("flows (2)")) && !mode.equals("r")) throw new FileNotFoundException("Read-only provider");
        return ParcelFileDescriptor.open(file(id), ParcelFileDescriptor.parseMode(mode));
    }
    @Override public String createDocument(String parentId, String mimeType, String displayName) throws FileNotFoundException {
        String id = System.nanoTime() + "-" + displayName.replace('/', '_');
        write(file(id), ""); return id;
    }
}
