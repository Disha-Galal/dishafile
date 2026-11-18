package com.disha.filekit

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Button
import com.disha.filekit.core.AiFallback
import org.json.JSONObject
import java.util.concurrent.Executors

// إدارة مستقلة للذكاء الاصطناعي حتى لا يعطل انتظار النموذج حفظ المسودات.
class AiController(private val activity: Activity, private val emit: (JSONObject) -> Unit, private val english: () -> Boolean) {
    private val settings = AiSettings(activity)
    private val worker = Executors.newSingleThreadExecutor()
    private var client: AiClient? = null
    private var generation = 0
    private var confirmation: AlertDialog? = null
    private var diagnostic: AiClient? = null
    private fun tr(ar: String, en: String) = if (english()) en else ar
    private fun send(value: JSONObject) { activity.runOnUiThread { if (!activity.isDestroyed) emit(value) } }
    fun cancel() { generation++; confirmation?.dismiss(); confirmation = null; client?.cancel(); client = null }
    fun destroy() { cancel(); diagnostic?.cancel(); worker.shutdownNow() }
    fun configure() {
        val config = try { settings.config(false) } catch (_: Exception) {
            send(JSONObject().put("type", "notice").put("text", tr("تعذر قراءة الإعدادات.", "Cannot read settings."))); return
        }
        val layout = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 8) }
        fun field(label: String, value: String, secret: Boolean = false): EditText {
            layout.addView(TextView(activity).apply { text = label })
            return EditText(activity).apply {
                setText(value); isSingleLine = true
                inputType = if (secret) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                layout.addView(this)
            }
        }
        val endpoint = field(tr("خدمة Ollama على نفس الهاتف (loopback فقط)", "Ollama on this phone (loopback only)"), config.endpoint)
        val primary = field(tr("النموذج الأساسي — الاسم من ollama list", "Primary model — name from ollama list"), config.primary)
        val secondary = field(tr("النموذج البديل", "Fallback model"), config.secondary)
        val gemini = field("Gemini model", config.gemini)
        layout.addView(TextView(activity).apply { text = if (settings.hasKey()) tr("مفتاح Gemini محفوظ ومشفر.", "Gemini key is saved and encrypted.") else tr("لا يوجد مفتاح Gemini محفوظ.", "No Gemini key saved.") })
        val secret = field(tr("مفتاح Gemini جديد (اتركه فارغًا للاحتفاظ بالمحفوظ)", "New Gemini key (blank keeps saved key)"), "", true)
        layout.addView(TextView(activity).apply { text = tr("تصدير GEMINI_API_KEY في Ubuntu لا يضبط مفتاح تطبيق أندرويد؛ أدخله هنا.", "Exporting GEMINI_API_KEY in Ubuntu does not configure the Android app; enter it here.") })
        val remove = CheckBox(activity).apply { text = tr("حذف مفتاح Gemini المحفوظ", "Delete saved Gemini key"); layout.addView(this) }
        val diagnostics = TextView(activity).apply { layout.addView(this) }
        val dialog = AlertDialog.Builder(activity).setTitle(tr("إعداد الذكاء الاصطناعي", "AI settings"))
            .setView(ScrollView(activity).apply { addView(layout) }).setNegativeButton(tr("إلغاء", "Cancel"), null)
            .setPositiveButton(tr("حفظ", "Save"), null).create()
        dialog.setOnDismissListener { diagnostic?.cancel(); diagnostic = null }
        fun currentConfig(cloud: Boolean): AiClient.Config = AiClient.Config(endpoint.text.toString().trim(), primary.text.toString().trim(), secondary.text.toString().trim(), gemini.text.toString().trim(),
            if (!cloud || remove.isChecked) "" else secret.text.toString().trim().ifBlank { settings.config().key })
        fun check(label: String, cloud: Boolean, listModels: Boolean = false) {
            val button = Button(activity).apply { text = label; layout.addView(this) }
            button.setOnClickListener {
                if (diagnostic != null) return@setOnClickListener
                val selected = try { currentConfig(cloud) } catch (_: Exception) { diagnostics.text = tr("تعذر قراءة المفتاح؛ استبدله أو احذفه.", "Cannot read saved key; replace or remove it."); return@setOnClickListener }
                if (cloud && selected.key.isBlank()) { diagnostics.text = tr("أدخل مفتاح Gemini أولًا.", "Enter a Gemini key first."); return@setOnClickListener }
                button.isEnabled = false
                diagnostics.text = tr("جاري الفحص؛ لا يتم إرسال أي مستند…", "Checking; no document is sent…")
                val task = AiClient(); diagnostic = task
                worker.execute {
                    val result = runCatching {
                        if (listModels) task.models(selected, true)
                        else listOf(task.generate(selected, "Translate to Arabic. Return only the translation.", "Hello world", cloud, if (cloud) "gemini" else "ollama") { }.let { "${it.model}: ${it.text}" })
                    }
                    activity.runOnUiThread {
                        if (diagnostic === task) diagnostic = null
                        if (!activity.isDestroyed && dialog.isShowing) {
                            button.isEnabled = true
                            result.fold({ values ->
                                if (listModels) {
                                    diagnostics.text = tr("اختار النموذج واختبر ترجمته ثم احفظ الإعدادات.", "Choose a model, test its translation, then save settings.")
                                    val names = values.sortedWith(compareBy<String> { !it.contains("flash") }.thenBy { it })
                                    if (names.isEmpty()) diagnostics.text = tr("لا توجد نماذج توليد متاحة لهذا المفتاح.", "No generation models are listed for this key.")
                                    else AlertDialog.Builder(activity).setTitle(tr("اختر نموذج Gemini ثم اختبره", "Choose a Gemini model, then test it")).setItems(names.toTypedArray()) { _, index -> gemini.setText(names[index]) }.show()
                                } else diagnostics.text = values.joinToString("\n")
                            }, { error -> diagnostics.text = failureText(error) })
                        }
                    }
                }
            }
        }
        check(tr("اختبار ترجمة Ollama", "Test Ollama translation"), false)
        check(tr("نماذج Gemini المتاحة", "Available Gemini models"), true, true)
        check(tr("اختبار ترجمة Gemini", "Test Gemini translation"), true)
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            try {
                settings.save(AiClient.Config(endpoint.text.toString(), primary.text.toString(), secondary.text.toString(), gemini.text.toString(), ""), secret.text.toString(), remove.isChecked)
                secret.text.clear(); dialog.dismiss()
            } catch (_: Exception) { endpoint.error = tr("راجع العنوان وأسماء النماذج؛ استخدم عنوان نفس الهاتف فقط.", "Check endpoint and model names; use this phone's loopback address.") }
        } }
        dialog.show()
    }
    fun request(message: JSONObject) {
        if (client != null || confirmation != null) return
        val id = message.optString("requestId").take(100)
        val text = message.optString("content")
        val instruction = message.optString("instruction")
        if (id.isBlank() || text.length > 16000 || instruction.isBlank() || instruction.length > 2000 || (instruction + text).toByteArray(Charsets.UTF_8).size > 6000) return
        val provider = message.optString("provider", "ollama")
        if (provider !in setOf("ollama", "gemini")) { fail(id, tr("اختار Ollama أو Gemini.", "Choose Ollama or Gemini.")); return }
        val cloud = provider == "gemini" || message.optBoolean("allowCloud", false)
        val config = try { settings.config(cloud) } catch (_: Exception) { fail(id, tr("تعذر قراءة المفتاح المحفوظ. استبدله أو احذفه من إعداد AI.", "Cannot read saved key. Replace or remove it in AI settings.")); return }
        if (cloud && config.key.isBlank()) { fail(id, tr("لا يوجد مفتاح Gemini محفوظ. افتح إعداد AI وأدخل المفتاح؛ متغير Ubuntu لا ينتقل إلى أندرويد.", "No saved Gemini key. Open AI settings and enter it; Ubuntu environment variables do not transfer to Android.")); return }
        val description = if (provider == "gemini") tr("إرسال ${text.length} حرفًا مع التعليمات مباشرة إلى Google Gemini (${config.gemini}). قد تُحتسب تكلفة.", "Send ${text.length} characters and instructions directly to Google Gemini (${config.gemini}). Charges may apply.")
            else tr("إرسال ${text.length} حرفًا مع التعليمات إلى ${config.endpoint}: ${config.primary} ثم ${config.secondary}.", "Send ${text.length} characters and instructions to ${config.endpoint}: ${config.primary}, then ${config.secondary}.") +
            if (cloud) tr(" عند فشلهما، يُرسل نفس النص إلى Google Gemini (${config.gemini}) باستخدام المفتاح المحفوظ. قد تُحتسب تكلفة.", " On failure, send the same text to Google Gemini (${config.gemini}) using the saved key. Charges may apply.") else ""
        confirmation = AlertDialog.Builder(activity).setTitle(tr("تأكيد إرسال النص المحدد", "Confirm selected text transfer")).setMessage(description)
            .setNegativeButton(tr("إلغاء", "Cancel")) { _, _ -> fail(id, tr("تم الإلغاء.", "Cancelled.")) }
            .setOnCancelListener { fail(id, tr("تم الإلغاء.", "Cancelled.")) }
            .setPositiveButton(tr("إرسال", "Send")) { _, _ ->
                confirmation = null
                val task = AiClient(); client = task; val ticket = ++generation
                worker.execute {
                    val result = runCatching { task.generate(config, instruction, text, cloud, provider) { model ->
                        send(JSONObject().put("type", "aiProgress").put("requestId", id).put("model", model))
                    } }
                    activity.runOnUiThread {
                        if (!activity.isDestroyed && ticket == generation) {
                            client = null
                            result.fold({ value -> emit(JSONObject().put("type", "aiResult").put("requestId", id).put("model", value.model).put("text", value.text).put("failures", org.json.JSONArray(value.failures))) }, { error ->
                                fail(id, failureText(error))
                            })
                        }
                    }
                }
            }.create().also { dialog -> dialog.setOnDismissListener { if (confirmation === dialog) confirmation = null }; dialog.show() }
    }
    private fun failureText(error: Throwable): String {
        val details = (error as? AiFallback.Unavailable)?.details
        return details?.joinToString("\n") { "${it.model}: ${reasonText(it.reason)}" }
            ?: reasonText(if (error is Exception) AiFallback.reason(error) else AiFallback.Reason.UNKNOWN)
    }
    private fun reasonText(reason: AiFallback.Reason): String = when (reason) {
        AiFallback.Reason.CONNECTION -> tr("لا يوجد اتصال. شغّل Ollama على نفس الهاتف أو راجع الإنترنت لـGemini.", "Cannot connect. Start Ollama on this phone or check internet for Gemini.")
        AiFallback.Reason.TIMEOUT -> tr("انتهت المهلة. جرّب qwen2.5:0.5b أو نصًا أصغر.", "Timed out. Try qwen2.5:0.5b or a smaller selection.")
        AiFallback.Reason.AUTH -> tr("المفتاح مرفوض أو لا يملك صلاحية. استبدله من إعداد AI.", "Key rejected or permission denied. Replace it in AI settings.")
        AiFallback.Reason.QUOTA -> tr("تجاوز حد الطلبات أو الحصة؛ راجع حصة Gemini والفوترة.", "Rate limit or quota exceeded; check Gemini quota and billing.")
        AiFallback.Reason.MODEL_NOT_FOUND -> tr("النموذج غير متاح. راجع ollama list أو اختر نموذج Gemini من الإعدادات واختبره.", "Model unavailable. Check ollama list or choose and test a Gemini model in settings.")
        AiFallback.Reason.RUNTIME -> tr("تثبيت Ollama ناقص: ملف تشغيل النموذج مفقود. أصلح تثبيت الخدمة؛ ظهور ollama list لا يكفي.", "Ollama installation is incomplete: model runner is missing. Repair the service; ollama list alone is insufficient.")
        AiFallback.Reason.MEMORY -> tr("ذاكرة غير كافية للنموذج. اجعل qwen2.5:0.5b الأساسي وأغلق التطبيقات الثقيلة.", "Insufficient memory. Set qwen2.5:0.5b as primary and close heavy apps.")
        AiFallback.Reason.INCOMPLETE -> tr("الرد غير مكتمل؛ حدد جزءًا أصغر واطلب ترجمة مختصرة.", "Incomplete response; select less text and request a shorter translation.")
        AiFallback.Reason.BLOCKED -> tr("رفض المزود هذا الطلب. غيّر النص أو التعليمات.", "Provider blocked this request. Revise the text or instructions.")
        AiFallback.Reason.EMPTY -> tr("النموذج أعاد ردًا فارغًا. جرّب نموذجًا آخر.", "Model returned an empty answer. Try another model.")
        AiFallback.Reason.INVALID -> tr("رد المزود غير صالح. راجع النموذج والخدمة.", "Invalid provider response. Check model and service.")
        AiFallback.Reason.SERVER -> tr("فشل خادم الذكاء الاصطناعي. اختبر الترجمة من إعداد AI وافحص خدمة المزود.", "AI server failed. Test translation in AI settings and check the provider service.")
        AiFallback.Reason.UNKNOWN -> tr("تعذر إكمال الطلب. راجع إعداد AI واختبر الاتصال.", "Request failed. Open AI settings and test the connection.")
    }
    private fun fail(id: String, message: String) = send(JSONObject().put("type", "aiError").put("requestId", id).put("text", message))
}
