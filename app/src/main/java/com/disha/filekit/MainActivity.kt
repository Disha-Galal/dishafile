package com.disha.filekit

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.webkit.*
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.disha.filekit.core.*
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    lateinit var web: WebView
        private set
    private lateinit var store: DocumentStore
    private val io = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("ui", MODE_PRIVATE) }
    private var ready = false
    private var busy = false
    private var pendingIntent: Intent? = null
    private var current: Session? = null
    private var pendingSave: Session? = null
    private var lang = "ar"
    private data class Session(val id: String, var name: String, var file: TextFile, var uri: Uri? = null, var writable: Boolean = false, var hash: String = "", var savedText: String = "\u0000")
    private val openDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { store.persist(uri); loadUri(uri) }
    }
    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val session = pendingSave
        pendingSave = null
        if (uri != null && session != null) writeCopy(uri, session)
    }
    private fun tr(ar: String, en: String) = if (lang == "en") en else ar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = DocumentStore(this)
        lang = prefs.getString("lang", "ar") ?: "ar"
        val root = FrameLayout(this)
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        CookieManager.getInstance().setAcceptCookie(false)
        val loader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == HOST && uri.path?.startsWith("/assets/editor/") == true)
                    loader.shouldInterceptRequest(uri)?.let { return it }
                return WebResourceResponse("text/plain", "UTF-8", 403, "Blocked", emptyMap(), ByteArrayInputStream(byteArrayOf()))
            }
            override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                AlertDialog.Builder(this@MainActivity).setMessage(tr("المحرر اتوقف. افتح التطبيق تاني واسترجع آخر مسودة.", "Editor stopped. Reopen the app and recover the last draft.")).setPositiveButton("OK") { _, _ -> finish() }.show()
                return true
            }
        }
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            AlertDialog.Builder(this).setMessage(tr("حدّث Android System WebView لتشغيل المحرر الآمن.", "Update Android System WebView to use the secure editor.")).setPositiveButton("OK") { _, _ -> finish() }.setCancelable(false).show()
            return
        }
        // الجسر متاح للأصل المحلي والإطار الرئيسي فقط؛ نص الملف لا يدخل HTML أبدا.
        WebViewCompat.addWebMessageListener(web, "Native", setOf(ORIGIN)) { _, message, source, main, _ ->
            if (main && source.toString() == ORIGIN) {
                val data = message.data
                if (data != null && data.length <= 64 * 1024 * 1024) {
                    try { route(JSONObject(data)) } catch (e: Exception) { error(e) }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (busy) return
                web.evaluateJavascript("JSON.stringify(window.Disha.snapshot())") { value ->
                    try { val raw = JSONTokener(value).nextValue(); if (raw is String && raw != "null") updateSnapshot(JSONObject(raw)) } catch (_: Exception) { }
                    guard { if (current == null) finish() else showHome() }
                }
            }
        })
        pendingIntent = if (savedInstanceState == null) intent else null
        web.loadUrl("$ORIGIN/assets/editor/index.html")
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        if (!ready) { pendingIntent = intent; return }
        web.evaluateJavascript("JSON.stringify(window.Disha.snapshot())") { value ->
            try { val raw = JSONTokener(value).nextValue(); if (raw is String && raw != "null") updateSnapshot(JSONObject(raw)) } catch (_: Exception) { }
            if (!busy) handleIntent(intent) else notice(tr("استنى العملية الحالية تخلص وبعدين افتح الملف.", "Wait for the current operation, then open the file."))
        }
    }
    override fun onPause() { if (ready) emit(JSONObject().put("type", "flush")); super.onPause() }
    override fun onDestroy() { web.destroy(); io.shutdown(); super.onDestroy() }

    private fun emit(message: JSONObject) {
        if (isDestroyed || !ready) return
        // JSON.quote يحافظ على النص كبيانات حتى لو احتوى على </script> أو أوامر جافاسكربت.
        web.evaluateJavascript("window.Disha.receive(JSON.parse(${JSONObject.quote(message.toString())}))", null)
    }
    private fun notice(text: String) = emit(JSONObject().put("type", "notice").put("text", text))
    private fun setBusy(value: Boolean) { busy = value; emit(JSONObject().put("type", "busy").put("value", value)) }
    private fun <T> background(task: () -> T, done: (T) -> Unit) {
        setBusy(true)
        io.execute {
            val result = runCatching(task)
            runOnUiThread { if (!isDestroyed) { setBusy(false); result.fold(done, ::error) } }
        }
    }
    private fun route(message: JSONObject) {
        val action = message.optString("action")
        if (action == "ready") {
            ready = true
            emit(JSONObject().put("type", "settings").put("lang", lang).put("dark", prefs.getBoolean("dark", true)).put("wrap", prefs.getBoolean("wrap", true)).put("fontSize", prefs.getInt("fontSize", 15)))
            recentList()
            pendingIntent?.let { pendingIntent = null; handleIntent(it) }
            return
        }
        if (action == "settings") {
            lang = if (message.optString("lang") == "en") "en" else "ar"
            prefs.edit().putString("lang", lang).putBoolean("dark", message.optBoolean("dark", true)).putBoolean("wrap", message.optBoolean("wrap", true)).putInt("fontSize", message.optInt("fontSize", 15).coerceIn(11, 28)).apply()
            return
        }
        if (busy && action != "draft") return
        if (message.has("docId")) updateSnapshot(message)
        when (action) {
            "open" -> guard { openDocument.launch(arrayOf("*/*")) }
            "new" -> guard { newFile() }
            "home" -> guard { showHome() }
            "recent" -> guard { val uri = Uri.parse(message.getString("uri")); if (uri.scheme == "content") loadUri(uri) }
            "recover" -> guard { recover() }
            "draft" -> Unit
            "copy" -> (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Disha FileKit", message.getString("text")))
            "save" -> current?.takeUnless { it.file.preview }?.let { save(it.copy()) }
            "saveAs" -> current?.takeUnless { it.file.preview }?.let { saveAs(it.copy()) }
            "share" -> current?.takeUnless { it.file.preview }?.let { share(it.copy()) }
            "encoding" -> chooseEncoding()
        }
    }
    private fun updateSnapshot(message: JSONObject) {
        val session = current ?: return
        if (session.id != message.optString("docId") || session.file.preview || !message.has("text")) return
        val text = message.getString("text")
        if (text.length > TextCodec.EDIT_LIMIT) throw FileProblem("tooLarge")
        session.file = session.file.copy(text = text)
        if (text != session.savedText) {
            val copy = session.copy()
            io.execute { try { store.draft(copy.id, copy.name, copy.file) } catch (_: Exception) { runOnUiThread { notice(tr("المسودة ما اتحفظتش؛ راجع المساحة واحفظ نسخة.", "Draft failed; check free space and Save As.")) } } }
        }
    }
    private fun guard(next: () -> Unit) {
        val session = current
        if (session != null && !session.file.preview && session.file.text != session.savedText) {
            AlertDialog.Builder(this).setMessage(tr("في تعديلات مش محفوظة. تكمل وتسيبها كمسودة استرجاع؟", "Unsaved changes. Continue and keep a recovery draft?"))
                .setNegativeButton(tr("إلغاء", "Cancel"), null).setPositiveButton(tr("حفظ مسودة ومتابعة", "Keep draft & continue")) { _, _ ->
                    background({ store.draft(session.id, session.name, session.file) }) { next() }
                }.show()
        } else next()
    }
    private fun showHome() { current = null; emit(JSONObject().put("type", "home")); recentList() }
    private fun newFile() {
        val input = EditText(this).apply { setText("untitled.txt"); selectAll(); isSingleLine = true }
        AlertDialog.Builder(this).setTitle(tr("اسم الملف (مثال: notes.md)", "Filename (e.g. notes.md)" )).setView(input)
            .setNegativeButton(tr("إلغاء", "Cancel"), null).setPositiveButton(tr("إنشاء", "Create")) { _, _ ->
                val name = safeName(input.text.toString())
                val gzip = name.lowercase().endsWith(".gz")
                val test = if (gzip) TextCodec.encode(TextFile("", gzip = true)) else byteArrayOf()
                try { TextCodec.open(test.inputStream(), name); display(Session(UUID.randomUUID().toString(), name, TextFile("", gzip = gzip))) } catch (e: Exception) { error(e) }
            }.show()
    }
    private fun loadUri(uri: Uri, encoding: String? = null, preview: Boolean = false) {
        if (uri.scheme != "content") { error(FileProblem("permission")); return }
        setBusy(true)
        io.execute {
            val result = runCatching {
                val meta = store.meta(uri)
                if (!preview && meta.size > TextCodec.EDIT_LIMIT && !meta.name.endsWith(".gz", true)) throw FileProblem("tooLarge")
                val before = if (preview) "" else store.digest(uri)
                val file = store.read(uri, meta.name, encoding, preview)
                if (!file.preview && before != store.digest(uri)) throw FileProblem("conflict")
                Session(UUID.randomUUID().toString(), meta.name, file, uri, meta.writable, if (file.preview) "" else before, file.text)
            }
            runOnUiThread {
                if (isDestroyed) return@runOnUiThread
                setBusy(false)
                result.fold({ display(it); addRecent(uri, it.name) }, { e ->
                    when ((e as? FileProblem)?.kind) {
                        "encoding" -> AlertDialog.Builder(this).setMessage(tr("الترميز مش UTF-8 صالح. تفتحه صراحة بـ Windows-1256؟ راجع الحروف قبل الحفظ.", "Not valid UTF-8. Explicitly decode as Windows-1256? Review the characters before saving."))
                            .setNegativeButton(tr("إلغاء", "Cancel"), null).setPositiveButton("Windows-1256") { _, _ -> if (encoding == null) loadUri(uri, "windows-1256", preview) else error(e) }.show()
                        "tooLarge", "largeText" -> AlertDialog.Builder(this).setMessage(tr("الملف أكبر من 8 MiB. افتح أول 256 KiB للقراءة فقط؟ الجزيب بيتفحص بالكامل بحد 32 MiB.", "File exceeds 8 MiB. Open a read-only 256 KiB preview? Gzip is fully checked with a 32 MiB cap."))
                            .setNegativeButton(tr("إلغاء", "Cancel"), null).setPositiveButton(tr("معاينة", "Preview")) { _, _ -> if (!preview) loadUri(uri, encoding, true) else error(e) }.show()
                        else -> error(e)
                    }
                })
            }
        }
    }
    private fun display(session: Session) {
        current = session
        val file = session.file
        emit(JSONObject().put("type", "load").put("docId", session.id).put("name", session.name).put("text", file.text).put("encoding", file.encoding)
            .put("newline", file.newline).put("bom", file.bom).put("gzip", file.gzip).put("preview", file.preview).put("dirty", file.text != session.savedText))
        if (file.newline == "MIXED") notice(tr("نهايات السطور مختلطة؛ الملف للقراءة فقط عشان نحافظ عليه.", "Mixed line endings: read-only to preserve the original bytes."))
    }
    private fun save(session: Session) {
        if (session.uri == null) { saveAs(session); return }
        if (!session.writable) {
            AlertDialog.Builder(this).setMessage(tr("المصدر للقراءة فقط. احفظ نسخة جديدة.", "This provider is read-only. Save a new copy."))
                .setNegativeButton(tr("إلغاء", "Cancel"), null).setPositiveButton(tr("حفظ باسم", "Save As")) { _, _ -> saveAs(session) }.show()
            return
        }
        AlertDialog.Builder(this).setMessage(tr("تأكيد استبدال ${session.name}${if (session.file.gzip) " وإعادة ضغط GZIP" else ""}؟ هنحتفظ بالمسودة ونسخة من الأصل. مزود الملفات ممكن ما يدعمش الحفظ الذري.", "Replace ${session.name}${if (session.file.gzip) " and recompress GZIP" else ""}? A draft and original backup will be kept. Providers may not support atomic writes."))
            .setNegativeButton(tr("إلغاء", "Cancel"), null).setPositiveButton(tr("استبدال", "Replace")) { _, _ ->
                background({
                    val bytes = TextCodec.encode(session.file)
                    store.draft(session.id, session.name, session.file)
                    store.backup(session.uri!!, session.name, session.hash, session.file.encoding)
                    store.write(session.uri!!, bytes)
                    store.hash(bytes)
                }) { hash -> saved(session, session.uri!!, session.name, hash, true) }
            }.show()
    }
    private fun saveAs(session: Session) {
        background({ TextCodec.encode(session.file) }) {
            pendingSave = session
            createDocument.launch(if (session.file.gzip && !session.name.endsWith(".gz", true)) "${session.name}.gz" else session.name)
        }
    }
    private fun writeCopy(uri: Uri, session: Session) {
        background({
            store.draft(session.id, session.name, session.file)
            val bytes = TextCodec.encode(session.file)
            store.write(uri, bytes)
            store.persist(uri)
            store.meta(uri) to store.hash(bytes)
        }) { (meta, hash) -> saved(session, uri, meta.name, hash, meta.writable) }
    }
    private fun saved(session: Session, uri: Uri, name: String, hash: String, writable: Boolean) {
        current?.takeIf { it.id == session.id }?.let {
            it.uri = uri; it.name = name; it.hash = hash; it.writable = writable; it.savedText = session.file.text; it.file = session.file.copy(text = it.file.text)
        }
        emit(JSONObject().put("type", "saved").put("docId", session.id).put("name", name).put("text", session.file.text).put("encoding", session.file.encoding).put("bom", session.file.bom).put("newline", session.file.newline).put("gzip", session.file.gzip))
        notice(tr("اتحفظ واتراجع. نسخة الاسترجاع لسه موجودة.", "Saved and verified. Recovery copy retained."))
        addRecent(uri, name)
    }
    private fun share(session: Session) {
        background({
            store.draft(session.id, session.name, session.file)
            val dir = File(cacheDir, "exports/${UUID.randomUUID()}").apply { mkdirs() }
            val file = File(dir, safeName(session.name))
            file.writeBytes(TextCodec.encode(session.file))
            FileProvider.getUriForFile(this, "$packageName.exports", file)
        }) { uri ->
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = if (session.file.gzip) "application/gzip" else "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri); clipData = ClipData.newRawUri("File", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, tr("مشاركة نسخة", "Share a copy")))
        }
    }
    private fun chooseEncoding() {
        val session = current?.takeUnless { it.file.preview } ?: return
        val options = arrayOf("UTF-8", "windows-1256", "UTF-16LE", "UTF-16BE")
        AlertDialog.Builder(this).setTitle(tr("ترميز الحفظ — التغيير محتاج حفظ باسم", "Save encoding — creates a new copy"))
            .setItems(options) { _, index ->
                val file = session.file.copy(encoding = options[index], bom = options[index].startsWith("UTF-16"))
                saveAs(session.copy(file = file, uri = null, writable = false))
            }.show()
    }
    private fun recover() {
        background({ store.recoveryDir.listFiles()?.filter { it.extension in setOf("json", "meta") }?.sortedByDescending { it.lastModified() }?.map { file ->
            file to runCatching { JSONObject(file.readText()).optString("name") }.getOrDefault(file.name)
        } ?: emptyList() }) { entries ->
            val files = entries.map { it.first }
            if (files.isEmpty()) { notice(tr("مفيش مسودات أو نسخ أمان.", "No recovery drafts or backups.")); return@background }
            val labels = entries.map { (file, name) ->
                "${if (file.extension == "meta") tr("الأصل", "Original") else tr("مسودة", "Draft")}: $name · ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(file.lastModified()))}"
            }.toTypedArray()
            AlertDialog.Builder(this).setTitle(tr("استرجاع — الحفظ باسم جديد", "Recover — save to a new file")).setItems(labels) { _, index ->
                val file = files[index]
                background({
                    if (file.extension == "json") store.readDraft(file)
                    else {
                        val name = JSONObject(file.readText()).getString("name")
                        name to File(store.recoveryDir, "${file.nameWithoutExtension}.bin").inputStream().use { TextCodec.open(it, name, JSONObject(file.readText()).optString("encoding", "UTF-8")) }
                    }
                }) { (name, data) -> display(Session(UUID.randomUUID().toString(), name, data)) }
            }.setNegativeButton(tr("إلغاء", "Cancel"), null).show()
        }
    }
    private fun addRecent(uri: Uri, name: String) {
        val old = JSONArray(prefs.getString("recent", "[]"))
        val next = JSONArray().put(JSONObject().put("uri", uri.toString()).put("name", name))
        for (i in 0 until old.length()) if (old.getJSONObject(i).getString("uri") != uri.toString() && next.length() < 12) next.put(old.getJSONObject(i))
        prefs.edit().putString("recent", next.toString()).apply(); recentList()
    }
    private fun recentList() = emit(JSONObject().put("type", "recents").put("items", JSONArray(prefs.getString("recent", "[]"))))
    @Suppress("DEPRECATION")
    private fun handleIntent(intent: Intent) {
        val uri: Uri? = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            else -> null
        }
        if (uri != null) { guard { loadUri(uri) }; return }
        if (intent.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
            if (text.length <= TextCodec.EDIT_LIMIT) guard { display(Session(UUID.randomUUID().toString(), "shared.txt", TextFile(text))) }
            else error(FileProblem("tooLarge"))
        }
    }
    private fun safeName(value: String): String = value.replace(Regex("[\\\\/\\p{Cntrl}]"), "_").take(160).ifBlank { "untitled.txt" }
    private fun error(error: Throwable) {
        val kind = (error as? FileProblem)?.kind ?: when (error) { is SecurityException -> "permission"; else -> "io" }
        val messages = mapOf(
            "gzip" to ("ملف GZIP تالف أو متعدد الأعضاء. الأصل ما اتغيرش." to "Invalid, truncated or multi-member GZIP. Original unchanged."),
            "bomb" to ("فك الضغط تجاوز حد الأمان 32 MiB." to "Decompression exceeds the 32 MiB safety limit."),
            "archive" to ("أرشيف TAR متعدد الملفات مش مدعوم في النسخة دي." to "Multi-file TAR archives are not supported in v1."),
            "binary" to ("ده ملف ثنائي مش نص قابل للتعديل." to "This is binary data, not editable text."),
            "unsupported" to ("نوع الملف مش مدعوم." to "Unsupported file type."),
            "encoding" to ("مش قادر نفك الترميز من غير فقد بيانات." to "Cannot decode losslessly."),
            "lossy" to ("الترميز الحالي مش بيدعم بعض الحروف. اختار UTF-8 من ترميز الحفظ." to "Some characters cannot be saved in this encoding. Choose UTF-8 in Save encoding."),
            "permission" to ("صلاحية الملف انتهت. افتحه تاني من اختيار الملفات." to "Access was revoked. Reopen using Open file."),
            "readOnly" to ("المزود رفض الكتابة. استخدم حفظ باسم." to "Provider refused writing. Use Save As."),
            "conflict" to ("الملف اتغير بره التطبيق. استخدم حفظ باسم عشان ما نكتبش فوق التغيير." to "Source changed outside the app. Use Save As to avoid overwriting it."),
            "verify" to ("فشل التحقق من الحفظ. استرجع النسخة الأصلية أو المسودة واحفظ باسم جديد." to "Save verification failed. Recover the original or draft and Save As."),
            "tooLarge" to ("الملف أكبر من حد التعديل 8 MiB." to "File exceeds the 8 MiB edit limit."),
            "preview" to ("المعاينة للقراءة فقط؛ مش مسموح بحفظ جزء فوق الأصل." to "Preview is read-only; partial content cannot replace the original."),
            "io" to ("فشل الوصول أو الحفظ. راجع مساحة الجهاز وصلاحيات المزود. لو الكتابة بدأت، استرجع نسخة الأمان واحفظ باسم." to "File access or save failed. Check disk space and provider permissions. If writing began, recover the backup and Save As.")
        )
        val pair = messages[kind] ?: messages.getValue("io")
        notice(tr(pair.first, pair.second))
    }
    companion object { const val HOST = "appassets.androidplatform.net"; const val ORIGIN = "https://$HOST" }
}
