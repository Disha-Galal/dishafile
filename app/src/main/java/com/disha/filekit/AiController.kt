package com.disha.filekit

import android.app.Activity
import android.app.AlertDialog
import android.text.InputType
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.disha.filekit.core.AiFallback
import org.json.JSONObject
import java.util.concurrent.Executors

// إدارة مستقلة للذكاء الاصطناعي حتى لا يعطل انتظار النموذج حفظ المسودات.
class AiController(private val activity: Activity, private val emit: (JSONObject) -> Unit, private val english: () -> Boolean) {
    private val settings = AiSettings(activity)
    private val worker = Executors.newSingleThreadExecutor()
    private var client: AiClient? = null
    private var generation = 0
    private fun tr(ar: String, en: String) = if (english()) en else ar
    private fun send(value: JSONObject) { activity.runOnUiThread { if (!activity.isDestroyed) emit(value) } }
    fun cancel() { generation++; client?.cancel(); client = null }
    fun destroy() { cancel(); worker.shutdownNow() }
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
        val secret = field(tr("مفتاح Gemini جديد (اتركه فارغًا للاحتفاظ بالمحفوظ)", "New Gemini key (blank keeps saved key)"), "", true)
        val remove = CheckBox(activity).apply { text = tr("حذف مفتاح Gemini المحفوظ", "Delete saved Gemini key"); layout.addView(this) }
        val dialog = AlertDialog.Builder(activity).setTitle(tr("إعداد الذكاء الاصطناعي", "AI settings"))
            .setView(ScrollView(activity).apply { addView(layout) }).setNegativeButton(tr("إلغاء", "Cancel"), null)
            .setPositiveButton(tr("حفظ", "Save"), null).create()
        dialog.setOnShowListener { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            try {
                settings.save(AiClient.Config(endpoint.text.toString(), primary.text.toString(), secondary.text.toString(), gemini.text.toString(), ""), secret.text.toString(), remove.isChecked)
                secret.text.clear(); dialog.dismiss()
            } catch (_: Exception) { endpoint.error = tr("راجع العنوان وأسماء النماذج؛ استخدم عنوان نفس الهاتف فقط.", "Check endpoint and model names; use this phone's loopback address.") }
        } }
        dialog.show()
    }
    fun request(message: JSONObject) {
        if (client != null) return
        val id = message.optString("requestId").take(100)
        val text = message.optString("content")
        val instruction = message.optString("instruction")
        if (id.isBlank() || text.length > 16000 || instruction.isBlank() || instruction.length > 2000 || (instruction + text).toByteArray(Charsets.UTF_8).size > 6000) return
        val cloud = message.optBoolean("allowCloud", false)
        val config = try { settings.config() } catch (_: Exception) { fail(id, tr("تعذر قراءة إعدادات الذكاء الاصطناعي.", "Cannot read AI settings.")); return }
        val description = tr("إرسال ${text.length} حرفًا مع التعليمات إلى ${config.endpoint}: ${config.primary} ثم ${config.secondary}.", "Send ${text.length} characters and instructions to ${config.endpoint}: ${config.primary}, then ${config.secondary}.") +
            if (cloud) tr(" عند فشلهما، يُرسل نفس النص إلى Google Gemini (${config.gemini}) باستخدام المفتاح المحفوظ. قد تُحتسب تكلفة.", " On failure, send the same text to Google Gemini (${config.gemini}) using the saved key. Charges may apply.") else ""
        AlertDialog.Builder(activity).setTitle(tr("تأكيد إرسال النص المحدد", "Confirm selected text transfer")).setMessage(description)
            .setNegativeButton(tr("إلغاء", "Cancel")) { _, _ -> fail(id, tr("تم الإلغاء.", "Cancelled.")) }
            .setOnCancelListener { fail(id, tr("تم الإلغاء.", "Cancelled.")) }
            .setPositiveButton(tr("إرسال", "Send")) { _, _ ->
                val task = AiClient(); client = task; val ticket = ++generation
                worker.execute {
                    val result = runCatching { task.generate(config, instruction, text, cloud) { model ->
                        send(JSONObject().put("type", "aiProgress").put("requestId", id).put("model", model))
                    } }
                    activity.runOnUiThread {
                        if (!activity.isDestroyed && ticket == generation) {
                            client = null
                            result.fold({ value -> emit(JSONObject().put("type", "aiResult").put("requestId", id).put("model", value.model).put("text", value.text).put("failures", org.json.JSONArray(value.failures))) }, { error ->
                                val names = (error as? AiFallback.Unavailable)?.failures?.joinToString(", ") ?: ""
                                fail(id, tr("تعذر الحصول على إجابة كاملة. راجع خدمة Ollama وأسماء النماذج والمفتاح. $names", "No complete response. Check Ollama service, model names and key. $names"))
                            })
                        }
                    }
                }
            }.show()
    }
    private fun fail(id: String, message: String) = send(JSONObject().put("type", "aiError").put("requestId", id).put("text", message))
}
