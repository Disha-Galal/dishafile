# Acceptance and remaining device checks

Automated test results and actual workflow URLs are recorded in `docs/BUILD_STATUS.md`. This checklist is for an Android 13 physical device, especially OPPO CPH2203. An emulator result does not prove a vendor provider's behavior.

1. In airplane mode, Open file → Downloads → sample.json. Select/copy a line, edit a key, Save As example-edited.json. Reopen and Validate.
2. From Downloads choose Open with Disha FileKit when offered. From Telegram share a compatible content URI. `application/octet-stream` is deliberately not claimed: use Open file if not offered.
3. Open malformed.json. Format must refuse, preserve content and show line/column.
4. Repeat editing, selection, Find/Replace, undo/redo and Save As with .md, .sql, .html, .txt. HTML must remain inert text.
5. Open config.json.gz and backup.sql.gz, edit, Save As .gz. Verify independently with `gzip -t` and `gzip -dc`. Save to original must explicitly confirm recompression.
6. Reject fake/corrupt/concatenated gzip and >32 MiB decompressed payloads. Reject tar.gz/tgz and binary data. Original stays unchanged on open failure.
7. Preserve Arabic, UTF-8 BOM and CRLF in arabic-crlf.txt; test UTF-16 BOM and Windows-1256. Unrepresentable added characters must refuse save and offer explicit UTF-8 Save As.
8. A provider with no write flag or grant must offer Save As. Revoke a recent URI grant and verify Open file guidance. Simulate disk-full/provider write failure; recover both draft and original backup.
9. Change a file externally after opening. Save must refuse overwriting and request Save As.
10. >8 MiB text: read-only 256 KiB preview, disabled export/save. Mixed line endings are read-only to avoid silent normalization.
11. Background/rotate/relaunch the app, Recover draft; verify content and Save As. Recovery intentionally has no original URI and cannot silently overwrite it.
12. Install the APK from Actions on Android 8+ and Android 13. Confirm no network/storage permissions and usable keyboard, insets, long-press selection and touch targets.

Known v1 limits: one document at a time; no archives, binary editing, execution, rendered document previews or folders; bounded read-only Flow/hex viewing is supported. Drafts debounce 700 ms; immediate process death may lose the latest keystrokes. Recovery opens from Home after process death/rotation. Backups/drafts are app-private and unencrypted at application level, not backed up to cloud; uninstall deletes them. Recovery entries are retained (no automatic pruning). Share exports live in app cache and Android can clear them. Providers do not guarantee atomic writes. Mixed newline files are read-only. Encoding selection is explicit for Windows-1256; no probabilistic autodetection. Large gzip is fully verified (bounded 32 MiB) before preview, so decompression is not instantaneous. Android 8 requires an updated compatible WebView. New debug signing keys on different GitHub runners may require uninstalling an earlier debug build; export drafts before uninstalling.


## قبول Flow Viewer — Android 13 وOPPO

هذه خطوات يدوية؛ لا تعني أنها نُفذت على هاتف حقيقي.

1. خذ نسخة خاصة من `flows (2)`، ولا تضفه إلى Git أو سجلات الاختبار. في وضع الطيران افتحه من Downloads عبر SAF بلا إعادة تسمية. المتوقع ظهور 4 تدفقات للملف المذكور في الخطة.
2. اختار POST الخاص بتسجيل الدخول: افتح الطلب وتأكد أن `username` و`password` وقيمهما الحقيقية ظاهرة فورًا بجوار بعض، دون Analyze أو تصدير. اختبر نسخ قيمة واحدة ثم `key=value` والمسار والقيم الفارغة والعربية.
3. ابحث بالمفتاح ثم بقيمة فعلية، وراجع تمييز النتائج. فعّل إخفاء البيانات الحساسة؛ القيم الحساسة ومعاينات النص الخام تُحجب ويُعطل نسخها. ألغِ الإخفاء وتأكد أن القيم الأصلية رجعت.
4. افتح الاستجابة: راجع الحالة وHeaders، وتكرار `set-cookie`، ومعاينة HTML كنص فقط بلا صور أو تنفيذ. HTTP 200 وحده ليس إثبات نجاح تسجيل الدخول.
5. راجع المستخرج ومصادر الإيميلات والهواتف والروابط والعدد. جرّب تحميل المزيد، التبديل السريع بين التدفقات واللغة، قائمة التدفقات، ثم الرئيسية والرجوع بأندرويد.
6. صدّر التدفق المحدد إلى JSON **جديد**. التصدير يحتوي القيم الحقيقية حتى مع إخفاء العرض، ويقتصر على الصفوف والمعاينات المحدودة؛ ليس تفريغ mitmproxy كاملًا. افتح JSON في المحرر وتحقق من العربية والقيم. قارن hash الأصل قبل وبعد الفتح والإغلاق والتصدير خارجيًا.
7. لا يظهر حفظ فوق الأصل داخل Flow Viewer. افتح من مزود قراءة فقط، وجرّب الإلغاء في منتقي التصدير. راجع grants المنتهية وأخطاء المزود والمساحة؛ لم تُحاكَ كل المزودات الحقيقية آليًا.
8. جرّب ملفًا ناقصًا وامتداد `.mitm` غير صالح؛ يجب ظهور خطأ عربي دون محارف بديلة أو crash. جرّب TCP/UDP وغياب Response وجسمًا ثنائيًا وجسمًا أكبر من 1 MiB وضغطًا غير مدعوم؛ تبقى الترويسات متاحة أو يظهر حد واضح.
9. بعد الخروج افتح TXT وJSON وJSONL وMD وSQL وHTML وGZ وأكمل خطوات حفظ المسودات والنسخ الاحتياطية السابقة. جرّب اللمس والتحديد ولوحة المفاتيح وRTL على OPPO؛ اختبارات المتصفح والمحاكي لا تثبت ذلك.
