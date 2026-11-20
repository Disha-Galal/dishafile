# Disha FileKit

This project By Gogo & Disha ®

## Android update 1.4.0

- Premium dark workspace with charcoal surfaces, mint controls and warm gold accents.
- Open and edit PHP source (`.php`, `.phtml`, `.phps`, PHP 3/4/5/7/8 extensions), including gzip copies, with local syntax highlighting. PHP and HTML are displayed as inert source.
- Open standalone, unencrypted SQLite 3 databases (`.db`, `.db3`, `.sqlite`, `.sqlite3`, or detected by header). Browse ordinary tables in read-only pages; encrypted databases and other DB formats are unsupported.
- Hold **A− / A+** to zoom continuously between 11 and 32 px. Hold line numbers or text lists to open the size slider. Normal long-press text selection stays available inside the editor.
- Tap the small chevron beside the filename to hide or restore the top tools. Save and the menu remain accessible. Text size and tool visibility survive app restarts.
- Temporary database snapshots are deleted when closed. Startup clears abandoned snapshots and share exports older than 24 hours. Drafts, backups and source documents are retained.

New editor modules: `editor/src/reading-controls.js` owns touch/size controls, `editor/src/database-viewer.js` renders database pages, and `app/.../DatabaseStore.kt` manages bounded, temporary read-only SQLite snapshots. Bundled assets remain in `app/src/main/assets/editor`; no CDN is used.

```text
app/src/main/java/com/disha/filekit/
  DatabaseStore.kt       SQLite snapshots, limits and paging
  DocumentStore.kt       SAF, recovery and temporary-file cleanup
  MainActivity.kt        Allowlisted native bridge and sessions
editor/src/
  reading-controls.js    Long press, zoom and toolbar visibility
  database-viewer.js     Read-only database tables and cells
  languages.js           Local syntax modes, including PHP
  style.css              Shared Android dark and light themes
```

Database limits: 40 MiB, 256 ordinary tables, 64 columns, 50 rows per page, 256 KiB cell text per page, and browsing through row 10,050. Text cells preview up to 4096 bytes; BLOB cells preview 32 bytes as hex, with visible truncation labels. Views and virtual tables are excluded. Export a standalone database after closing its source app; external WAL/journal files are not imported. Invalid text encoding is reported without replacement characters.

[دليل الاستخدام الكامل / User Guide](docs/USER_GUIDE.md) — إعداد Ollama وGemini وتنزيل APK واستخدام المحرر.

محرر ملفات كود ونصوص خفيف لأندرويد، عربي من الأول وفيه تحويل للإنجليزي. بيفتح من Downloads واختيار الملفات ومشاركة Telegram لو التطبيق بيدي صلاحية للملف. تحرير الملفات شغال من غير نت أو حساب. صلاحية INTERNET مخصصة لطلب AI يختاره المستخدم؛ مفيش وصول شامل للتخزين، وصفحات WebView نفسها محظورة عن الشبكة.

## تاخد الـ APK من موبايلك

1. افتح [الريبو](https://github.com/Disha-Galal/dishafile)، وبعدها [Actions → Android APK](https://github.com/Disha-Galal/dishafile/actions/workflows/android-apk.yml).
2. افتح آخر تشغيل أخضر مطابق لآخر تعديل في كود التطبيق؛ تعديلات التوثيق لوحدها مش بتعيد البناء. لو محتاج تشغيل جديد اختار **Run workflow**.
3. تحت **Artifacts** نزّل **Disha-FileKit-debug-APK** وفك ملف ZIP.
4. افتح `app-debug.apk`، واسمح بالتثبيت من المتصفح أو مدير الملفات لما أندرويد يطلب.
5. محتاج Android 8 أو أحدث وAndroid System WebView حديث. مش محتاج كمبيوتر ولا Android Studio ولا Termux لتشغيل التطبيق.

دي نسخة debug للتجربة، مش إصدار متجر. مفاتيح التوقيع المؤقتة ممكن تختلف بين تشغيلات Actions؛ لو التحديث رفض التثبيت صدّر المسودات الأول قبل إزالة النسخة القديمة. الحالة الفعلية للبناء والاختبارات موجودة في [BUILD_STATUS](docs/BUILD_STATUS.md)؛ وجود workflow لوحده مش دليل إن APK اتبنى.

## الاستخدام

- **افتح ملف**: اختار ملفك من SAF. لو مدير الملفات ما بيعرضش التطبيق مع Open with، افتحه من جوه التطبيق.
- **ملف جديد**: اكتب الاسم بالامتداد، زي `notes.md` أو `config.json.gz`.
- المحرر فيه أرقام سطور وتلوين، تحديد ونسخ ولصق، تراجع وإعادة، بحث واستبدال، لف السطور وحجم خط. الكود LTR والعربي بيظهر Unicode طبيعي.
- **حفظ**: استبدال الأصل بتأكيد واضح، مع مسودة ونسخة أمان وفحص إن الملف ما اتغيرش من تطبيق تاني. المزود لو للقراءة بس هيوجّهك لحفظ باسم.
- **حفظ باسم**: بيعمل مستند جديد؛ مش بيفترض إنه يكتب فوق اسم قديم. **مشاركة** بتصدّر نسخة.
- **استرجاع** من الرئيسية: افتح المسودات أو نسخ الأصل واحفظ باسم جديد. المسودات بتتخزن محليًا بعد 700 ms من التعديل؛ آخر ضغطة ممكن تضيع لو النظام قفل العملية فجأة.
- JSON: فحص بمكان الخطأ، تنسيق بمسافتين أو أربع، وضغط. التنسيق بيحافظ على الأرقام الكبيرة والمفاتيح المتكررة. JSONL له فحص مستقل سطر بسطر، من غير تنسيق جماعي.
- GZIP: بيتعرف من البايتات مش الاسم بس. فك ضغط آمن، تعديل النص الداخلي وإعادة ضغطه. `tar.gz` والأرشيفات متعددة الملفات مش مدعومة.
- الترميز: UTF-8، BOM لـ UTF-8/UTF-16، وWindows-1256 باختيار صريح. نهايات LF/CRLF/CR بتفضل زي الأصل. لو أضفت حرف مش مدعوم، الحفظ بيرفض بدل ما يبوظه؛ اختار **ترميز الحفظ → UTF-8** لنسخة جديدة.

الأنواع السابقة ومعها TS/TSX/JSX/MJS/CJS، Kotlin وJava وC/C++ وC# وGo وRust وRuby وTOML وProperties وGradle وBash/Zsh وPowerShell وBAT/CMD وTSV وNDJSON وSVG وSRT/VTT وENV/CFG/CONFIG وDockerfile وGitignore وEditorconfig، والنصوص دي داخل GZ. تلوين لغوي محلي للغات البرمجة المدعومة؛ TSV وCSV والترجمات وBAT/CMD تفتح كنصوص. NDJSON يتفحص سجل بسجل مثل JSONL. مفيش تشغيل سكربتات أو SVG/HTML ولا معاينة صفحات. PDF وOffice والصور والأرشيفات متعددة الملفات مش مدعومة للتحرير.

## الذكاء الاصطناعي المحلي وGemini

من **الاختيارات → إعداد AI** اضبط عنوان Ollama على نفس الهاتف (الافتراضي `http://127.0.0.1:11434`) وأسماء النماذج كما تظهر في `ollama list`. القيم الأولية `llama3.2:3b` ثم `qwen2.5:0.5b` قابلة للتغيير؛ وصف الحجم 2 GB أو 400 MB وحده لا يحدد اسم النموذج. التطبيق يتصل بخدمة Ollama المثبتة بالفعل، ولا يضم أو ينزل أوزان النماذج ولا يشغل GGUF مباشرة. الخدمة لازم تكون شغالة على الهاتف.

أدخل مفتاح Gemini من نافذة أندرويد؛ يُحفظ بتشفير AES-GCM ومفتاح Android Keystore، ولا يصل إلى JavaScript أو Git أو النسخ الاحتياطي. اترك الحقل فارغًا للاحتفاظ به، أو اختر حذف المفتاح. غيّر اسم نموذج Gemini عند الحاجة؛ الافتراضي `gemini-3.1-flash-lite`.

من إعداد AI تقدر تختبر ترجمة Ollama وGemini وتختار نموذج Gemini من قائمة المزود. التثبيت القديم يحتفظ باسم النموذج المحفوظ؛ لو ظهر «النموذج غير متاح» غيّره واختبره. متغير `GEMINI_API_KEY` في Ubuntu لا ينتقل لتطبيق أندرويد. [إعداد Ubuntu وتشخيص الخدمة](docs/AI_SETUP.md).

حدد النص باللمس أو **تحديد السطور** ثم افتح **✦ AI** واختار Ollama أو Gemini المباشر. للترجمة اختار العربية أو الإنجليزية واضغط **جهّز طلب الترجمة**، أو اكتب طلبك. بدون تحديد يُرسل الملف كله ضمن الميزانية. إجمالي النص والطلب حتى 6000 بايت UTF-8، والطلب حتى 2000 حرف؛ النص الأطول يُرفض مع رسالة لتحديد جزء أصغر. الميزانية محافظة لتلائم سياق المحليين 8192 رمزًا دون قص صامت. نافذة أندرويد تعرض الوجهة وعدد الحروف قبل الإرسال. يتبدل بين المحليين عند فشل الاتصال أو انتهاء المهلة أو الرد الفارغ/التالف/غير المكتمل. السماح بالسحابة اختياري لكل طلب؛ عند اختياره مع Ollama يُجرب Gemini بعد فشل المحليين؛ اختيار Gemini المباشر يرسل إلى Google فور تأكيدك وقد تُحتسب تكلفة API. حدود المحاولة: اتصال 4 ثوانٍ ومدة إجمالية 180 ثانية للمحلي و90 ثانية لـGemini، ورد حتى 1 MiB و1024 رمز إخراج محليًا أو 8192 في Gemini (بما فيها ميزانية التفكير بحسب النموذج). يتم تفريغ النموذج المحلي بعد الطلب لتخفيف الذاكرة.

النتائج نصوص خام غير قابلة للتنفيذ. راجع النتيجة ثم اضغط **استبدال النطاق بعد المراجعة**؛ التغيير قابل للتراجع، ويرفض التطبيق اقتراحًا لملف تغيّر أثناء الانتظار. مفيش حفظ تلقائي للاقتراح فوق الأصل؛ استخدم مسار الحفظ المعتاد. المعاينات للقراءة فقط تسمح بالشرح والنسخ ولا تسمح بالاستبدال. إغلاق النافذة يلغي الطلب. عارض Flow يظل مستقلاً ولا يرسل تدفقاته تلقائيًا إلى AI.

مراجع البروتوكول: [Ollama generate](https://docs.ollama.com/api/generate) و[Gemini generateContent](https://ai.google.dev/api/generate-content).

## تحكم الهاتف

الواجهة الداكنة محفوظة مع أزرار لمس كبيرة وشريط قابل للتمرير. **تحديد السطور** يحدد نطاقًا دقيقًا؛ شريط الأسهم ينقل المؤشر، وزر ⇥ يضبط إزاحة السطور المحددة. يظهر السطر والعمود وعدد الحروف المحددة. حقوق `This project By Gogo & Disha ®` ظاهرة أسفل الواجهة.

## عرض mitmproxy والقيم التلقائية

افتح `flows` أو `flows (2)` أو `.flow` أو `.mitm` أو `.mitmproxy` من **افتح ملف**. التطبيق يفحص البايتات قبل مسار النص، ويتعرف على TNetstring الصحيح حتى لو الاسم بلا امتداد. تظهر قائمة التدفقات، ويفتح أول تدفق على تبويب **الطلب** تلقائيًا. اختار أي تدفق لعرض **نظرة عامة / الطلب / الاستجابة / المستخرج**.

كل مفتاح وقيمته ظاهرين معًا فورًا داخل الطلب والاستجابة، بما فيهم Query وForm وJSON وHeaders وحقول Multipart النصية. القيم الفارغة والمفاتيح المتكررة والأرقام الكبيرة محفوظة. تقدر تنسخ القيمة أو `key=value` أو مسار المصدر، وتبحث باسم المفتاح أو القيمة. **إخفاء البيانات الحساسة** يخفي العرض والنسخ فقط؛ لا يغيّر الأصل. معاينات الأجسام النصية تختفي أيضًا عند تفعيل الإخفاء منعًا لتسريب قيمة سرية داخل النص الخام.

**المستخرج** يعرض القيم بمصدرها، مع الإيميلات والأرقام المصرية والدولية والروابط بقواعد محلية محدودة؛ لا يوجد AI أو API أو اتصال شبكة. `HTTP 200` وحده لا يثبت نجاح تسجيل الدخول. HTML يظل نصًا غير قابل للتشغيل.

ملف Flow للقراءة فقط، ولا يدخل مسار الحفظ فوق الأصل. زر **تصدير التدفق المحدد إلى JSON جديد** ينشئ مستند SAF جديدًا ويتحقق من قراءته بعد الكتابة. التصدير يحتوي صفوف العرض والمعاينات المحدودة للتدفق المحدد، وقد يتضمن أسرارًا حقيقية حتى لو فعلت إخفاء العرض؛ ليس نسخة كاملة قابلة للاستيراد إلى mitmproxy. لا يتم التصدير أو النسخ أو المشاركة تلقائيًا.

حدود Flow: الملف 40 MiB، السجل 16 MiB، 4096 تدفقًا، عمق 48، وإجمالي 200 ألف عقدة. فك الجسم لحد 1 MiB، ومعاينة النص 64 ألف حرف؛ الأجسام الأكبر أو غير القابلة للفك تعرض الحجم ومعاينة Hex محدودة. يدعم identity وgzip وdeflate؛ Brotli وZstd والأجسام المشفرة غير مدعومة. تُحفظ Headers عند فشل فك الجسم، ويظل TCP/UDP/WebSocket ببيان واضح بدل تفسيره كـHTTP. العرض 100 صف في الدفعة بحد 5000 صف وميزانية مليوني حرف للعرض والاستخراج لكل تدفق و1000 تطابق نصي؛ **تحميل المزيد** يعرض الدفعة التالية. البحث يرشح الصفوف المحللة للتدفق المحدد، ولا يبحث في بايتات الأجسام التي تجاوزت الحدود. التفاصيل تعمل في الخلفية مع إلغاء الطلب السابق وذاكرة مؤقتة لتدفقين.

الملف الحقيقي `flows (2)` مش مرفق بالمستودع ولم يُختبر هنا. خطوات تجربته على الهاتف في [اختبارات القبول](docs/ACCEPTANCE.md)، ونتائج النسخة الجديدة في [تقرير التنفيذ](docs/FLOW_IMPLEMENTATION_REPORT_AR.md).

## حدود مهمة

التعديل لحد 8 MiB. الملفات الأكبر ليها معاينة أول 256 KiB للقراءة بس. حد فك gzip هو 32 MiB، والملفات المضغوطة المصدر حدها 40 MiB. الملفات ذات نهايات سطور مختلطة بتفتح للقراءة بس عشان ما يحصلش تغيير خفي.

المسودات والنسخ الأصلية بتفضل في مساحة التطبيق، وبتتمسح لو أزلت التطبيق. مزود SAF ممكن يفشل في منتصف الكتابة؛ فيه نسخة أصل ومسودة للاسترجاع لكن ما نقدرش نضمن كتابة ذرية عند كل مزود. باقي القيود وخطوات تجربة OPPO في [اختبارات القبول](docs/ACCEPTANCE.md).

## تطوير وبناء من الموبايل

على **Ubuntu داخل Termux PRoot ARM64** تقدر تعدّل السورس وتشغّل اختبارات Java/JS. بناء APK الرسمي في GitHub Actions على Ubuntu x86_64، مش على ARM64. المطلوب للاختبارات المحلية: JDK 17 وNode 22 وPython 3 لسيرفر اختبار المتصفح فقط.

```bash
git clone https://github.com/Disha-Galal/dishafile.git disha-filekit
cd disha-filekit/editor
npm ci
npm run build
npm test
npx playwright install chromium
npm run test:browser
cd ..
./gradlew -PcoreOnly :core:test :ai:test
```

اختبارات Chromium الحالية نجحت على ARM64 PRoot باستخدام إعداد التشغيل العادي و`--workers=1` بعد تثبيت الإصدار المطابق لـPlaywright. وضع `DISHA_PROOT=1` مع single-process فشل في هذه الجولة. GitHub Actions يشغل الاختبارات أيضًا، ونتيجة الإصدار الحالي موضحة في BUILD_STATUS. لو واجهت مشكلة تشغيل المتصفح في بيئتك، استخدم Actions بدل الاعتماد على محاكي جوه الموبايل. `JAVA_HOME` لازم يشاور على JDK 17. مفيش Python أو Node أو سيرفر داخل APK.

أمر البناء على GitHub Ubuntu بعد تثبيت SDK:

```bash
./gradlew :core:test :ai:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

الإصدارات متوافقة ومثبتة: AGP 8.9.2، Kotlin 2.2.10، Gradle 8.11.1، JDK 17، SDK 35، minSdk 26. [مصادر التوافق والتفاصيل](docs/TOOLCHAIN.md).

## هيكل المشروع

```text
app/                  تطبيق Kotlin وSAF وWebView والاختبارات على أندرويد
  src/main/assets/    المحرر المبني محليًا، من غير CDN
core/                 فك/ضغط GZIP والترميز وحماية البيانات + اختبارات JVM
ai/                   عميل Ollama وGemini واختبارات البروتوكول دون Android SDK
editor/               سورس CodeMirror والاختبارات وpackage-lock.json
samples/              أمثلة عربية وJSON وSQL وMarkdown وGZIP
.github/workflows/    بناء APK واختبار Android 13 في وضع الطيران
scripts/              تشغيل اختبار APK على المحاكي
docs/                 حالة البناء، اختبار القبول، التوافق والتراخيص
gradle/wrapper/       Wrapper أصلي متولد بأداة Gradle الرسمية
```

راجع [AGENTS.md](AGENTS.md) قبل التعديل. ترخيص السورس الأصلي محتاج تأكيد المالك قبل نشر عام؛ مكتبات الطرف التالت ليها تراخيصها الموجودة في المشروع.
