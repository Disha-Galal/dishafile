# حالة التحقق — 1.3.0 — 2026-11-18

This project By Gogo & Disha ®

إصلاح مزودي AI: اختيار Gemini المباشر، تجهيز طلب الترجمة، اختيار نموذج Gemini واختبار ترجمة المزودين من إعدادات أندرويد، وأسباب أعطال واضحة دون عرض نصوص أخطاء الخادم. اسم Gemini الافتراضي الجديد `gemini-3.1-flash-lite`؛ الإعدادات المحفوظة سابقًا لا تتغير دون اختيار المستخدم.

- نجحت اختبارات JVM: 35 في core و7 في عميل ai المستقل عن Android SDK.
- نجحت اختبارات JSON الستة و23 اختبار Chromium، بما فيها ترجمة Gemini المباشرة والمراجعة والتراجع وأمان النص.
- نجح `scripts/setup-ubuntu.sh` محليًا مع JDK 17.0.21 وNode 22.22.1 وChromium. ملف البيئة المحلي `.tools/env.sh` لا يحتوي مفاتيح.
- نجح فحص ترجمة Gemini فعليًا لعبارة Hello world باستخدام `gemini-3.1-flash-lite`، وأعاد نصًا عربيًا. النموذج القديم `gemini-2.5-flash` أعاد HTTP 404 في نفس الجولة، رغم ظهوره في قائمة المزود.
- ظهر النموذجان المحليان في قائمة Ollama، لكن التثبيت القديم أعاد HTTP 500 لغياب `llama-server`. ثُبتت حزمة Ollama v0.40.16 الرسمية بعد التحقق من SHA-256، مع الاحتفاظ بالأوزان، وأعيد تشغيل الخدمة على `127.0.0.1:11434`. نجح عميل التطبيق الفعلي في توليد رد Qwen؛ جودة ترجمته العربية لم تكن موثوقة، وتجربة Llama تجاوزت مهلة 180 ثانية. Gemini هو المسار الموصى به للترجمة العربية في هذه البيئة؛ لا يوجد ادعاء بنجاح ترجمة Llama.
- محاولة بناء أندرويد محليًا توقفت لغياب Android SDK؛ نجح البناء الرسمي على GitHub Ubuntu x86_64 في [التشغيل 44382522377](https://github.com/Disha-Galal/dishafile/actions/runs/44382522377). نجحت اختبارات API 33 العشرة في وضع الطيران: [سجل الاختبار](validation/android-13-ai-1.3-instrumentation.txt) و[وضع الطيران](validation/ai-1.3-airplane-mode.txt). المجموع: 81 اختبارًا ناجحًا. Lint: صفر أخطاء و20 تحذيرًا.
- لم يُختبر التطبيق المحدّث على هاتف فعلي. [خطوات القبول](ACCEPTANCE.md) و[إعداد وتشخيص AI](AI_SETUP.md).

مصدر APK: `7d61b98bd512b97080b7ec7ff93010e0bbba6291`، على فرع `build/ai-providers-1.3.0`، دون دمج تلقائي في main. التوثيق اللاحق لا يغير APK المبني.

- [تنزيل APK إصدار 1.3.0](https://github.com/Disha-Galal/dishafile/actions/runs/44382522377/artifacts/14463609236) — فك ZIP ثم ثبّت app-debug.apk، versionCode 4، الحجم 2,976,220 بايت، الاحتفاظ حتى 2026-12-18.
- [دليل Markdown من البناء](https://github.com/Disha-Galal/dishafile/actions/runs/44382522377/artifacts/14463334241).
- [تقارير JVM وlint](https://github.com/Disha-Galal/dishafile/actions/runs/44382522377/artifacts/14463879061) و[أدلة محاكي API 33](https://github.com/Disha-Galal/dishafile/actions/runs/44382522377/artifacts/14463694479).

SHA-256 للـ APK: `f344864b33d85b8873d9349499b377adbb8bd3ceaa5cd7fb42bc71d27cdf7224`.

فُحص Manifest الثنائي: minSdk 26 وtargetSdk 35؛ INTERNET وصلاحية AndroidX الخاصة فقط، دون صلاحيات تخزين شامل. أصول المحرر داخل APK مطابقة للمصدر المتتبع. نجحت ترجمة Gemini أيضًا باستخدام عميل AiClient الفعلي المترجم إلى JVM، لا فحص Python وحده.


---

# حالة البناء والاختبارات — 1.2.0 — 2026-11-16

This project By Gogo & Disha ®

نجح [تشغيل GitHub Actions رقم 43864769118](https://github.com/Disha-Galal/dishafile/actions/runs/43864769118) بالكامل، بما فيه بناء APK وlint ومحاكي Android 13 في وضع الطيران. الفرع المنشور `build/local-ai-1.2.0`؛ لم يُدمج تلقائيًا في `main`.

مصدر APK المختبَر: `a6ead671cd6d59b592a1aca12d32160fc5509f7a`. أي تعديل توثيق بعد هذا المصدر لا يغيّر التطبيق المبني. البناء على GitHub Ubuntu x86_64، بالإصدارات المثبتة في AGENTS.md. لم يُبنَ APK محليًا على ARM64.

| الاختبار | النتيجة الفعلية |
|---|---|
| Kotlin/JVM: codec وFlow وتحويل AI | 34 ناجحًا |
| JavaScript: JSON وJSONL | 6 ناجحة |
| Chromium بحجم هاتف | 21 ناجحًا |
| Android 13 / API 33، وضع الطيران | 9 ناجحة |
| الإجمالي | 70 اختبارًا ناجحًا |
| Android lint | صفر أخطاء؛ 20 تحذيرًا |
| Debug APK وAPK الاختبارات | ناجح |
| تطابق أصول المحرر المبنية مع السورس | ناجح |
| تصدير دليل Markdown | ناجح؛ النسخة المنزلة مطابقة لملف المستودع |

## التنزيل

- [APK: Disha-FileKit-debug-APK](https://github.com/Disha-Galal/dishafile/actions/runs/43864769118/artifacts/14224813915) — فك ZIP ثم ثبّت `app-debug.apk`.
- [دليل الاستخدام بصيغة Markdown](USER_GUIDE.md)، و[نسخة ZIP مصدرة من نفس البناء](https://github.com/Disha-Galal/dishafile/actions/runs/43864769118/artifacts/14224824046).
- [تقارير JVM وlint](https://github.com/Disha-Galal/dishafile/actions/runs/43864769118/artifacts/14225156253).
- [نتائج محاكي Android 13 وسجل وضع الطيران](https://github.com/Disha-Galal/dishafile/actions/runs/43864769118/artifacts/14225610517).

تنزيل Artifacts قد يتطلب دخول GitHub. صلاحية APK ودليل ZIP حتى 2026-12-16 بحسب بيانات التشغيل؛ ملف الدليل داخل المستودع يبقى متاحًا بعدها. النسخة debug للتجربة، وليست إصدار متجر.

APK: الإصدار `1.2.0`، versionCode `3`، حجمه `2959402` بايت.

SHA-256 للـ APK بعد فك ZIP:

```text
c9f94f84e60a6f0eb709c511cb800445bc3015d9b06f5d773503b2468ceb695d
```

فُحص Manifest الثنائي داخل APK المنزّل: الحزمة `com.disha.filekit`، minSdk 26، targetSdk 35. الصلاحيات هي INTERNET وصلاحية AndroidX الخاصة بالمستقبل غير المصدّر؛ لا صلاحيات تخزين شامل. HTTP مسموح فقط لأسماء loopback المحددة مع `includeSubdomains=false`؛ بقية الوجهات تحتاج HTTPS. جسر WebView يظل محصورًا بالأصل المحلي والإطار الرئيسي، وCSP يحظر طلبات الشبكة من المحرر.

## نطاق التحقق

اختبارات Android التسعة تشمل حفظ/قراءة SAF، كشف التعارض، رفض الاستبدال الضمني في مسار الإنشاء، الامتدادات الجديدة مع مزود للقراءة فقط، Flow، التحرير والمسودات، تشفير/حذف مفتاح Gemini، الإلغاء، واتصال HTTP حقيقي بخادم تجريبي محلي يحاكي فشل النموذج الأول ونجاح البديل. النتيجة في سجل المحاكي `OK (9 tests)`، ووضع الطيران `enabled`.

المستخدم أكد اسمي النموذجين `llama3.2:3b` و`qwen2.5:0.5b`، وهما الافتراضيان داخل التطبيق. لم تُختبر جودة هذين النموذجين أو الاتصال الفعلي بهما على هاتف المستخدم، ولم تُستخدم بياناته أو مفتاحه لطلب Gemini. اختبارات الهاتف الفعلي في ACCEPTANCE.md ما زالت مطلوبة.

أول بناء لهذه الجولة توقف عند lint بسبب غياب `includeSubdomains` في نطاقات loopback. أضيفت القيمة `false` لكل نطاق، ثم نجح البناء الحالي دون تعطيل lint أو توسيع الشبكة.

حذف أرشيف تثبيت JDK المستخرج وفّر 192,336,814 بايت محليًا، مع الاحتفاظ بالـ JDK. لا أوزان نماذج أو CDN أو تبعيات إضافية؛ المفتاح غائب عن Git والدليل ويُدخل من إعدادات أندرويد ويُحفظ مشفرًا.

---

# حالة البناء والاختبارات — 2026-11-09

This project By Gogo & Disha ®

نجح [تشغيل 42540671614](https://github.com/Disha-Galal/dishafile/actions/runs/42540671614) كاملًا، بما يشمل اختبارات Android 13 في وضع الطيران. الفرع `feature/flow-viewer-inline-values`، و[PR #1](https://github.com/Disha-Galal/dishafile/pull/1) مفتوح دون دمج تلقائي. ظل `main` عند `ce0b2aa9823e5af293cb4f4541e7b15678f369ec` وقت التسليم.

مصدر APK المختبَر: `a65ccd72417afcebcf06ab860d1358e5c335073b`. التعديلات بعده توثيق وأدلة فقط. البناء على GitHub Ubuntu x86_64 باستخدام JDK 17 وGradle 8.11.1 وAGP 8.9.2 وKotlin 2.2.10 وSDK 35 وNode 22؛ لا بناء APK أو محاكي محلي على ARM64.

| الاختبار | النتيجة |
|---|---|
| Kotlin/JVM، بما يشمل Flow وcodec | 28 ناجحًا |
| JavaScript / JSON / JSONL | 6 ناجحة |
| Chromium بحجم هاتف | 11 ناجحًا |
| Android 13 / API 33 | 6 ناجحة، وضع الطيران مؤكَّد |
| المجموع | 51 ناجحًا |
| Lint | 0 أخطاء، 19 تحذيرًا |
| Debug APK وAPK الاختبارات | ناجح |
| تطابق الأصول المبنية مع الملفات المتتبعة | ناجح |

[APK للتنزيل](https://github.com/Disha-Galal/dishafile/actions/runs/42540671614/artifacts/13641371684): حزمة ZIP باسم `Disha-FileKit-debug-APK`، بداخلها `app-debug.apk` إصدار 1.1.0، حجمه 2,918,625 bytes. الاحتفاظ 30 يومًا، ويتطلب التنزيل دخول GitHub.

SHA-256: `5bbf185d1b5a91bd01b0963efb462389e2685e8dc88af5f9c15b1b20261e659c`.

الأدلة: [تقارير البناء](https://github.com/Disha-Galal/dishafile/actions/runs/42540671614/artifacts/13641476442)، [نتيجة Android 13](validation/android-13-flow-instrumentation.txt)، [وضع الطيران](validation/flow-airplane-mode.txt)، [صورة Flow من Chromium](flow-phone.png). فُحص Manifest الثنائي في APK المُنزّل: minSdk 26 وtargetSdk 35، ولا INTERNET أو صلاحيات تخزين شاملة.

نجحت اختبارات JVM وJavaScript وChromium محليًا أيضًا على ARM64 PRoot بعد تنزيل JDK 17 وChromium المطابق لـPlaywright. اختبار المتصفح قبل اكتمال تنزيله فشل بسبب غياب الملف التنفيذي ثم نجحت الاختبارات بعد التثبيت. لا يوجد ادعاء باختبار OPPO أو ملف المستخدم الحقيقي `flows (2)`؛ جميع fixtures صناعية.

تحذيرات Lint غير المانعة مرتبطة بإصدارات الأدوات المثبتة واقتراحات KTX وسياسات النسخ الاحتياطي ومسار فحص URI القائم. لم تُمنح صلاحيات شبكة أو تخزين عامة ولم يتغير تقييد الجسر للأصل المحلي والإطار الرئيسي أو CSP وحظر الطلبات البعيدة. مزود SAF لا يضمن استبدالًا ذريًا؛ يستمر مسار النص في إنشاء المسودة والنسخة الأصلية وفحص التعارض والتحقق بعد الكتابة، وFlow للقراءة فقط مع تصدير JSON جديد.

التفاصيل وما لم يُختبر وطريقة التثبيت والتحذير من اختلاف توقيع Debug في [تقرير التنفيذ العربي](FLOW_IMPLEMENTATION_REPORT_AR.md)، وخطوات الهاتف في [القبول](ACCEPTANCE.md). حالة إصدار 1.0.0 السابقة محفوظة في تاريخ Git؛ هذا الملف يخص إصدار 1.1.0.
