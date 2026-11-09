# Changelog

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
