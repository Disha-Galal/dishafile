# حالة العمل الحالية — 1.2.0 — 2026-11-16

This project By Gogo & Disha ®

هذه نتائج التعديلات المحلية لإضافة AI وتحسين المحرر. نتائج 1.1.0 الموجودة أسفل الصفحة تاريخية، ولا تثبت بناء APK للتعديلات الجديدة.

| الاختبار | نتيجة الجولة الحالية |
|---|---|
| JVM / codec / Flow / تحويل AI | 34 ناجحًا، صفر فشل |
| JavaScript / JSON / JSONL | 6 ناجحة |
| Chromium بحجم هاتف | 20 ناجحًا في المجموعة الكاملة، ثم 6 ناجحة في إعادة الاختبارات المرتبطة بـ AI، تشمل حالة إضافية لرفض سياق عربي يتجاوز الميزانية |
| بناء أصول المحرر المحلية | ناجح، HTML وCSS مطابقان للسورس، بلا CDN أو تبعيات جديدة |
| Android lint / Debug APK / test APK | لم تُنفذ المراحل: توقف Gradle بسبب غياب Android SDK |
| محاكي API 33 وهاتف فعلي | لم يُشغلا لهذه التعديلات؛ لا جهاز متصل في adb |
| اتصال النماذج الفعلية وGemini | لم يُختبر؛ يلزم عنوان الخدمة وأسماء النماذج الفعلية ومفتاح داخل إعدادات الهاتف |

الأوامر المنفذة باستخدام Node 22 وJDK 17:

```bash
cd editor
npm ci
npm run build
npm test
npx playwright install chromium
npm run test:browser -- --workers=1
cd ..
./gradlew -PcoreOnly :core:test
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

احتاج Chromium إلى الإعداد العادي؛ تشغيل `DISHA_PROOT=1` بوضع single-process سبب توقف المتصفح عند إعادة إنشاء السياق. اجتاز التشغيل العادي الاختبارات العشرين. واجه Kotlin daemon خطأ جلسة في بيئة PRoot واستخدم Gradle التجميع البديل؛ انتهت اختبارات JVM بنجاح.

أضيفت اختبارات أندرويد لاتصال HTTP loopback ببديل تجريبي، والتحويل بعد 503، وتشفير/حذف المفتاح، والإلغاء، والامتدادات الجديدة من مزود للقراءة فقط، ورفض استبدال مستند موجود ضمن مسار الإنشاء. هذه الاختبارات لم تُشغّل محليًا. يجب أن تنجح GitHub Actions، بما فيها محاكي API 33، قبل إصدار APK.

[صورة المحرر من Chromium](editor-phone.png) و[معاينة واجهة AI](ai-phone.png). نص الإجابة في صورة AI مثال توضيحي موسوم كمعاينة، وليس ردًا من نموذج فعلي.

حذف أرشيف تثبيت JDK المستخرج وفّر 192,336,814 بايت محليًا؛ بقي JDK مثبتًا. لم تُحذف ملفات المستخدم أو أدلة الاختبارات السابقة. لم تُضف أوزان نماذج، ولم يُكتب مفتاح API في السورس أو Git. صلاحية INTERNET الجديدة مخصصة لاتصال أندرويد؛ CSP وحظر الشبكة والإطار الرئيسي في WebView باقية.

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
