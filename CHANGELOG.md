# Changelog

## 1.2.0 — 2026-11-16

- اتصال AI اختياري: Ollama المحلي الأساسي ثم البديل ثم Gemini بعد سماح السحابة لكل طلب، مع تأكيد وجهة إرسال النص.
- مفتاح Gemini مشفر في Android Keystore، وحدود طلب ورد ومهلة وإلغاء، ومراجعة الاقتراح قبل تطبيقه مع كشف التغييرات والتراجع.
- دعم امتدادات نصية إضافية وتلوين TypeScript/TSX وKotlin وJava وC/C++ وC# وGo وRust وRuby وTOML وPowerShell وDockerfile، وفحص NDJSON سجلًا بسجل.
- أدوات تحديد نطاق السطور وتحريك المؤشر وعرض موضعه، وتحسين الواجهة الداكنة مع حفظ حقوق Gogo & Disha.
- رفض الاستبدال الضمني عند إرجاع مزود SAF مستندًا موجودًا من مسار الإنشاء. فصل وحدات AI واختيار اللغة دون إضافة أوزان نماذج أو مكتبات جديدة.
- نتائج الاختبارات الحالية وحدود التحقق موثقة في docs/BUILD_STATUS.md.

## 1.1.0 — 2026-11-09

- عارض mitmproxy محلي للقراءة فقط، يتعرف على ملفات TNetstring بلا امتداد قبل فحص النص.
- إظهار Key = Value تلقائيًا داخل الطلب والاستجابة، مع حفظ القيم الفارغة وUnicode والمفاتيح والترويسات المتكررة.
- استخراج محلي للإيميلات والهواتف والروابط، بحث مؤجل، نسخ فردي وإخفاء عرض الأسرار وتصدير التدفق المحدد إلى JSON جديد.
- حدود صارمة للتحليل وفك gzip/deflate ومعاينة Hex، وإلغاء تفاصيل التدفق السابق وذاكرة مؤقتة محدودة.
- اختبارات JVM وChromium وSAF على بيانات صناعية فقط؛ راجع تقرير التنفيذ العربي لنتائج التشغيل الفعلية وروابط APK.

## 1.0.0 — 2026-11-07

- Arabic-first offline Android editor with English toggle, local CodeMirror 6 and native document access.
- Text, JSON/JSONL, code, strict single-member gzip, encoding/BOM/newline preservation.
- Explicit save confirmation, safe copies, conflict checks, drafts and original backups.
- JSON whitespace formatter preserves number tokens and duplicate keys.
- Automated codec, editor and Android 13 smoke tests; APK build workflow.
- v1 excludes archives, binary editing, execution and HTML/Markdown preview.
