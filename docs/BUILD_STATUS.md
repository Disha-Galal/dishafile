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
