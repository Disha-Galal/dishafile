# DISHA FILEKIT — Codex Execution Plan

**المهمة:** تطوير تطبيق Disha FileKit Android Native لإتاحة قراءة ملفات mitmproxy `flows` وعرض كل `Key = Value` مباشرة أثناء الاستعراض، وإضافة استخراج محلي سريع للإيميلات وأرقام الهواتف. **نفّذ التعديل فعلًا، واختبره، وابنِ APK تجريبيًا عبر GitHub Actions إن أمكن.**

**المستودع:** `https://github.com/Disha-Galal/dishafile`  
**البيئة:** Android / Kotlin + WebView/CodeMirror، تطوير من Ubuntu PRoot على ARM64 داخل Termux، Android APK Build على GitHub Actions x86_64.  
**فرع التنفيذ:** `feature/flow-viewer-inline-values`؛ لا تدمج في `main` تلقائيًا.  
**المبدأ:** بدون API أو LLM داخل التطبيق، بدون إنترنت، بدون Base44، بدون اشتراكات أو خدمات مدفوعة. احتفظ بكل وظائف FileKit الحالية.

---

## 0) تعليمات تشغيل ملزمة للوكيل (اقرأ قبل أي تعديل)

أنت Codex Coding Agent. اقرأ `AGENTS.md` ثم `README.md` ثم `docs/ACCEPTANCE.md`، وافحص الشجرة والحالة الحالية بـ`git status`. **المطلوب تنفيذ المشروع، وليس إرجاع خطة أخرى فقط.** لا تطلب موافقة على التفاصيل الهندسية الصغيرة؛ اتخذ القرار الأنسب واختبره. استفسر فقط إذا كان هناك تغيير مدمّر أو وصول غير متاح لا يمكن تجاوزه.

### سياسة توفير الـCredits/Usage

1. استخدم **`gpt-6.1-sol` على `high`** للتصميم والدمج واكتشاف عيوب الـParser. **لا تستخدم Max/Ultra افتراضيًا**، ولا تنتقل إلى Astra إلا عند وجود عطل معماري أو أمني غير محلول بعد محاولتين مبنيتين على نتائج اختبارات فعلية.
2. إذا كانت أداة الـCLI تدعم subagents، استخدم `gpt-6-luna` أو Sol Low للمهام المحددة جدًا فقط (صياغة README، مراجعة تنسيق، اختبار صغير). لا تنشئ subagents لو تكلفتها أعلى من تنفيذ المهمة محليًا.
3. نفّذ `git`, `grep`, `sed`, `find`, `npm`, `node`, `gradle`, `python` كأوامر Shell مباشرة، وليس عن طريق تكليف LLM بتنفيذ عملية يمكن للـShell تنفيذها.
4. اقرأ **الملفات المعنية فقط**، واستخدم مقتطفات من الأسطر بدل طباعة المشروع بالكامل أو الملفات الثنائية أو الـbundles الضخمة. لا تعِد تحميل السياق من الصفر بعد كل اختبار.
5. أصلح أخطاء الاختبار من نص الخطأ المحدد؛ بحد أقصى محاولتين للإصلاح الموضعي قبل التوقف وتحليل السبب الجذري. لا تدخل في إعادة تصميم شاملة بلا دليل.
6. لا تعمل أبحاثًا على الإنترنت إلا لتوثيق نقطة تقنية غير محسومة. لا تستخدم API Keys أو Agents مدفوعة أو خدمات Cloud خارج GitHub Actions المتاحة أصلًا.
7. حافظ على ردودك مختصرة: `تم / لم يتم / الاختبار / الخطوة التالية`. لا تطبع HTTP bodies ولا passwords أو cookies في الطرفية أو مخرجات الـCI.

### خط أحمر لحماية المشروع

- لا تعمل `reset --hard` أو حذف ملفات غير متتبعة أو إعادة كتابة تاريخ Git؛ افحص تغييرات المستخدم أولًا ولا تدهسها.
- لا تغيّر التطبيق إلى Web App أو Base44. لا تضف `INTERNET` permission، ولا CDN، ولا معالجة الملفات على سيرفر.
- لا تُنفِّذ محتوى HTML/JS أو Scripts من ملفات المستخدم، ولا تسمح بأي HTTP requests ناتجة عنها. اعرضها كنص **escaped/inert** فقط.
- لا تكسر SAF أو النسخ الاحتياطية أو الاسترجاع أو JSON/JSONL أو GZIP أو الترميزات الحالية.
- لا تحفظ تفريغ الشبكة الحقيقي في Git أو رسائل الـcommit أو Logs أو لقطات الاختبار؛ استخدم fixtures **صناعية** مع بيانات وهمية.
- تعليقات الكود المكتوب يدويًا بالعربي، واحترم حقوق المشروع: `This project By Gogo & Disha ®` وتعليمات `AGENTS.md`.

## 1) بيانات مؤكدة عن المشروع والملف المستهدف

المشروع الحالي (افحصه مجددًا إذا تغيّر):

- `core/src/main/kotlin/com/disha/filekit/core/TextCodec.kt`: يقرر صيغ الملفات ويفك GZIP ويمنع binary؛ `EDIT_LIMIT=8 MiB`, `SOURCE_LIMIT=40 MiB`, `PREVIEW_LIMIT=256 KiB`.
- `app/src/main/java/com/disha/filekit/DocumentStore.kt`: فتح `content://` عبر SAF وإدارة القراءة والنسخ الاحتياطي.
- `app/src/main/java/com/disha/filekit/MainActivity.kt`: `loadUri`, أحداث WebView والـBridge، المعالجة في background executor.
- `editor/src/index.html`, `editor/src/main.js`, `editor/src/style.css`: الواجهة الحالية.
- `core/src/test/`, `editor/test/`, `editor/tests/` بحسب ما يظهر فعليًا: الاختبارات.
- `app/src/main/assets/editor/` نتيجة بناء المحرر؛ يجب تحديثها بعد تعديل السورس.

**ملف تحقق حقيقي يمتلكه المستخدم:** `flows (2)` (بدون امتداد)، حجمه **554,453 bytes**، بصيغة **mitmproxy Flow Dump / TNetstring**، يحتوي **4 HTTP flows**. نسخة الملف موجودة على موبايل المستخدم وقد لا تكون داخل جلسة Ubuntu. لا تتوقف إن لم تجدها؛ اصنع fixtures مشابهة صناعيًا أولًا واكتب خطوات اختبار الملف الحقيقي محليًا دون إضافته للمستودع.

للتأكد من بنية الملف: كل flow هو TNetstring record مستقل يبدأ بطول عشري ثم `:`، والـtop-level dict به `type="http"`, `version=17`, `request`, `response` وبيانات أخرى. الـ`request` فيه `method`, `path`, `scheme`, `host`, `port`, `headers`, `content`، والـ`response` فيه `status_code`, `headers`, `content`. الـHTTP bodies نص/HTML وForm وقد تحتوي معلومات حساسة. **HTTP 200 لا يدل وحده على نجاح تسجيل الدخول.**

مراجع البروتوكول عند الحاجة:
- https://github.com/mitmproxy/mitmproxy/blob/main/mitmproxy/io/io.py
- https://github.com/mitmproxy/mitmproxy/blob/main/mitmproxy/io/tnetstring.py
- https://docs.mitmproxy.org/stable/api/mitmproxy/http.html

## 2) تعريف النجاح — الشرط الأهم للمستخدم

> المستخدم يريد **القيم (`values`) ظاهرة فورًا بجانب المفاتيح أثناء عرض المحتوى داخل التطبيق**. لا يكفي استخراجها داخل شاشة مستقلة أو بعد الضغط على زر «Analyze». لا تعرض أسماء مفاتيح فقط بدون قيم.

**تجربة المستخدم المطلوبة:**

- فتح `flows`, `.flow`, `.mitm`, `.mitmproxy` أو ملف بلا امتداد يحتوي signature صالح يفتح **Flow Viewer** تلقائيًا، دون إعادة تسمية الملف، ودون خطأ `unsupported` أو `binary`.
- تظهر قائمة مختصرة بكل Flow: الرقم، method، host + path، HTTP status، توقيت/حجم إن توفر، ومؤشر محتوى request/response.
- عند فتح أي Flow تظهر أقسام واضحة `Overview | Request | Response | Extracted`. **داخل كل قسم** تظهر أزواج المفاتيح والقيم مباشرة، مثل:

  ```text
  Request form values
  username = [القيمة الفعلية من ملف المستخدم]
  password = [القيمة الفعلية من ملف المستخدم]

  Query parameters
  page = 2
  search = example

  Response headers
  content-type = text/html; charset=utf-8
  ```

- اجعل الـKey والـValue في **نفس السطر أو صف واضح متجاور**، مع Select/Copy للقيمة الفردية، والسياق (`Flow #`, `Request/Response`, `Header/Query/Form/JSON`). لا حاجة لأي زر استخراج لظهور الصفوف؛ الاستخراج يتم تلقائيًا عند فتح Flow أو قسمه، بشكل كسول وغير محجِّز للواجهة.
- اعرض **القيمة الحقيقية كما هي** بما فيها القيمة الفارغة وUnicode والإيميلات والأرقام والقيم المركبة. لا تجعل `key` يختفي إذا كانت `value` فارغة. عند تعذر فك قيمة ثنائية، اكتب `Binary, N bytes` مع `Hex preview` محدود ولا تختلق نصًا.
- قيم `password`, `token`, `authorization`, `cookie`, `set-cookie` يمكن عرضها **كقيم فعلية محليًا** تلبيةً لطلب المستخدم، مع خيار ظاهر `إخفاء البيانات الحساسة` يتعامل مع العرض فقط. لا تغيّر bytes الأصلية ولا تحول القيم في التحليل إلى placeholders. **لا تصدّر ولا تشارك ولا تسجّل القيم تلقائيًا**. لا تخزن نسخة غير لازمة في Preferences أو telemetry.
- دعم بحث فوري محلي باسم المفتاح أو القيمة، مع تمييز النتائج، بدون تنفيذ remote requests.
- الملف الأصلي في وضع **قراءة فقط**؛ لو المستخدم يريد نصًا قابلًا للتعديل يختار `Export decoded text/JSON` إلى ملف جديد بواسطة SAF، وليس الكتابة فوق Flow الأصلي. اسم الملف وبياناته لا تضيع.
- أخطاء الملفات التالفة واضحة بالعربي، بدون crash أو فتحها كنص متلخبط.

## 3) التنفيذ الهندسي المقترح (نفذه وفق الواقع الحالي للريبو)

### المرحلة A — Discovery وParser آمن (أساسية)

1. افحص مسار `loadUri()` في `MainActivity`، ومداخل `TextCodec.open()`، وبروتوكول الرسائل `Native` / `emit` بين Kotlin وWebView. جهّز فرع Git نظيفًا.
2. اعمل **Content Sniffing** من بايتات الملف في مسار الفتح **قبل** تمرير الملف إلى `TextCodec.open()` الذي يرفض الامتدادات غير المعروفة والملفات الثنائية. التعرف لا يعتمد على الامتداد وحده؛ افحص طول/تاج الـTNetstring وتحقق من قاموس أعلى المستوى و`type` وحقول HTTP الأساسية.
3. أضف parser منفصلًا داخل `core/`، مثل `FlowTnetstringParser.kt` أو تصميم أنسب (مع اختبارات JVM). لا تثبّت mitmproxy Python ولا تشغيل Python داخل APK. استخدم Kotlin/Java Stdlib دون dependencies ثقيلة.
4. ادعم TNetstring tags: `,` bytes، `;` UTF-8 string، `#` integer، `^` float، `!` boolean، `~` null، `]` list، `}` dict؛ تعامل مع keys من `String` أو `ByteArray` عند الحاجة واحتفظ بالـorder والـduplicate headers. لا تفترض وجود response أو إنه دائمًا HTTP.
5. Parsing آمن: فحص length قبل allocation، رفض integer overflow، EOF/truncation، invalid tag/length، recursion depth bounded، limits لكل record/body/مجموع الملف، وإظهار الخطأ بصياغة مفهومة. القراءة تتم على worker وليس UI Thread؛ لملفات كبيرة استخدم InputStream/streaming أو index offsets مع lazy body loading، وتجنب نسخ الـbody عدة مرات.
6. الأنواع غير المدعومة مثل TCP/UDP/WebSocket: اعرض `Unsupported flow type` أو `Generic Metadata` بوضوح بدل انهيار التطبيق. للـHTTP flow احفظ Request وResponse وملحقاتها وفق المتاح.
7. حافظ على **ترتيب وتكرار Headers**؛ `set-cookie` قد يتكرر، والقيم الثنائية تعالج بعناية. لا تقطع URLs أو قيم Query في data model (يمكن اختصار العرض فقط).
8. اعزل العرض بـ`FlowDocument` أو `FlowSummary/FlowDetail` view models؛ لا تعِد استخدام `TextFile` للتحرير كأنه ملف نصي عادي؛ لا تعرض Save overwrite لتنسيق غير قابل لإعادة الكتابة losslessly.

### المرحلة B — Inline Values وواجهة Flow Viewer (أساسية)

1. أضف مسارًا آمنًا من Kotlin إلى WebView للـFlow Viewer: events allowlisted، main frame/local origin فقط، واتبع حماية الـBridge الموجودة في المشروع. لا تضف `eval` على محتوى الملف أو HTML dynamic injection.
2. أضف Flow List UI مع lazy detail، وتبويبات `Overview`, `Request`, `Response`, `Extracted`، واعرض `Key = Value` **ضمن Request/Response** وليس داخل Extracted فقط.
3. اعرض هكذا، بشكل منظم:
   - Request method/scheme/host/path/port/status/timestamps؛
   - Query params من URL، مع `URLDecoder` صحيح لجزء Query فقط؛ التعامل مع `%` و`+` بشكل صحيح؛
   - Request/Response headers مثل `authorization`, `cookie`, `set-cookie`, `content-type`؛
   - `application/x-www-form-urlencoded` (values تلقائيًا)؛
   - JSON bodies: شجرة أو rows flattened مثل `user.email = ...`, `items[0].id = ...` مع الحفاظ على null/false/0 والقيم المكررة إن كان المحلل يسمح بذلك؛
   - `multipart/form-data`: أسماء الحقول وقيم النص وأسماء الملفات وأحجامها فقط، مع عدم تحميل binary كامل في الواجهة؛
   - HTML/text bodies: text preview escaped (بدون Render/JS) مع بحث؛ لا تفسر كل HTML كنموذج Key/Value عشوائي؛
   - Response status والـheaders والـbody preview؛ decoded JSON أو Form إن كان مناسبًا.
4. التعامل مع `Content-Encoding` (identity, gzip, deflate) داخل حدود واضحة؛ فشل فك الضغط لا يكسر عرض الـheaders ولا الـraw preview. لا تضف Brotli/Zstd dependency ثقيلة إلا إذا لزم وكان تنفيذها خفيفًا. `Content-Type` وcharset يُؤخذان في الحسبان، وفشل decode يعرض بيانًا صريحًا ولا يستبدل الأحرف بصمت.
5. تحت كل Value: Copy value، Copy `key=value`، Copy path عند الحاجة؛ **لا تنسخ شيئًا تلقائيًا**. عند خروج المستخدم من Flow Viewer ارجع للقائمة/الرئيسية بشكل صحيح.
6. RTL لعناصر الواجهة، وLTR مناسب لعرض IDs والـURLs والكود والأرقام؛ دعم عربي وإنجليزي دون كسر CodeMirror الحالي.

### المرحلة C — Smart Extract محلي وخفيف (ضمن نفس التحديث بعد نجاح A+B)

**لا تستخدم نموذج AI أو قواعد بيانات أو REST API؛ المطلوب Rule-Based Scanner فقط.**

1. الوحدة تستخرج عند فتح القسم ذي الصلة (أو background idle) بلا ضغط زر: Emails؛ Phone Numbers (مصر + دولي مبدئيًا، محافظًا على صيغة المصدر مع normalized اختياري)؛ URLs؛ Key/Value من JSON/Form/Query/Headers؛ وأسماء مفاتيح مهمة مثل `password`, `passwd`, `pwd`, `token`, `access_token`, `api_key`, `session`, `username` — **من غير ما تكتفي بالأسماء؛ أظهر القيم**.
2. الأولوية للمحللات المنظمة: JSON/form/query/headers قبل regex العام. Regex للنصوص الحرة فقط مع حدود طول وحجم وعدد matches لمنع التجميد والـfalse positives.
3. `Extracted` يعرض النتائج الفعلية مرفقة بمصدرها `flow 2 > Request > form > username`، مع type وCount؛ لا تعرض كل كلمة تشبه رقم هاتف باعتبارها رقمًا صحيحًا.
4. ميزة `Live Key Filter`: اكتب اسم مفتاح مثل `password` أو `session` فتعرض rows المطابقة بـvalues فورًا من نتائج parsing الموجودة، لا إعادة scanning لكل حرف.
5. قفل CPU/memory: maximum displayed rows و`Load more`؛ debounce للبحث؛ lazy parse لعمليات body الثقيلة، keep caches بحجم محدود، cancellation عند فتح ملف جديد. لا تُعطّل فتح txt/json التقليدي.
6. من المفيد مستقبلًا دعم HAR عبر Parser مختلف، لكن **ليس شرطًا لإنهاء هذه المهمة**؛ لا توسع النطاق لما بعد نجاح Flow Viewer.

## 4) الاختبارات الإلزامية — معايير القبول

### Kotlin/JVM parser

- Valid TNetstring with 1 and multiple records, mixed bytes/string keys، version 17، 4 records؛ response موجود/مفقود.
- Truncated input؛ invalid length/tag؛ length overflow؛ deep nesting؛ record/total limits؛ malicious header sizes؛ malformed UTF-8.
- Header order, duplicate `set-cookie` values؛ query percent-encoding؛ form `+`/`%` encoding؛ JSON nested/array/boolean/null/zero؛ empty values؛ binary payload.
- Content-Encoding gzip/deflate limits and malformed compressed body؛ never execute HTML؛ no network.
- **Synthetic golden fixture** على مثال أربعة flows، تتضمن `username=demo@example.test&password=sample123` داخل request body وهمي، وتتحقق الاختبارات أن النتائج تظهر بـ`demo@example.test` و`sample123` كـvalues كاملة عند الاستعراض، مش أسماء فقط.
- إثبات عدم تعديل الملف الأصلي byte-for-byte عند الإغلاق، وعدم ظهور overwrite/save عليه.

### JS/Browser/UI

- Open `Flow Viewer` → select POST Flow → `Request` → تظهر key/value fields **فورًا**؛ لا تضغط Analyze.
- Search by key and by value، copy فردي، إخفاء/إظهار البيانات الحساسة، تغيير اللغة، رجوع للشاشة الرئيسية.
- Large body preview doesn't hang; `Load more` works; HTML stays inert; no network requests from content.
- Regression لملفات `txt`, `md`, `json`, `jsonl`, `sql`, `html`, `gz` وSAF، مع عدم تراجع الأمان وميزات الحفظ.

### أوامر التحقق (عدّل حسب سورس المشروع الفعلي)

```bash
# افحص شجرة المشروع وتعليماته أولًا
pwd
git status --short
cat AGENTS.md

# اختبارات الـCore
./gradlew -PcoreOnly :core:test

# بناء واختبارات الواجهة
cd editor
npm ci
npm run build
npm test
# عند توافر Chromium؛ خلاف ذلك GitHub Actions
npm run test:browser
cd ..

# عند توافر Android SDK/JDK المناسبين على CI
./gradlew :core:test :app:lintDebug :app:assembleDebug :app:assembleDebugAndroidTest
```

**مهم:** البناء الرسمي للـAPK يتم على GitHub-hosted Ubuntu x86_64؛ لا تستنزف الوقت في محاولة محاكي Android/Chromium لا يعمل داخل ARM64 PRoot. قدّم أدلة الاختبارات الناجحة فقط، وميّز بوضوح ما لم يتم اختباره على جهاز OPPO فعلي.

### اختبار يدوي أساسي على الهاتف

1. افتح `flows (2)` مباشرة من Downloads بواسطة SAF، حتى لو الاسم بلا امتداد.
2. يجب عرض 4 flows؛ افتح `POST /login` ثم Request Form وتأكد أن `username` و`password` لهما قيم ظاهرة فعليًا، مع إمكانية نسخ قيمة واحدة، **دون تصدير أو تحليل يدوي**.
3. افتح Response Headers وBody Preview، وجرب بحث key/value والإيميلات/الأرقام؛ وافتح Flow آخر ثم ارجع.
4. اخرج من التطبيق وافتح ملف JSON عادي ثم GZ؛ تحقق أن الوظائف القديمة سليمة وأن ملف flows الأصلي لم يُستبدل.
5. افحص وضع الطيران؛ العرض والتحليل يجب أن يعملا بالكامل أوفلاين.

## 5) استراتيجية التنفيذ لتقليل المخاطر

- **Checkpoint 1:** Parser + tests، قبل أي تعديل في الواجهة. إذا فشل اختبار، أصلح الـCore أولًا.
- **Checkpoint 2:** Integration بـKotlin + WebView + UI (inline values)، أضف browser tests.
- **Checkpoint 3:** Smart Extract المحلي + الأداء + security/performance tests.
- **Checkpoint 4:** documentation + build assets + tests + APK artifact + changelog.

استخدم commits صغيرة مع رسائل مثل `feat(flow): parse mitmproxy tnetstrings`, `feat(ui): show inline request and response values`, `feat(extract): local value extraction`, `test(flow): cover malformed and binary records`. لا تعمل rewrite لكل الـarchitecture. افتح Pull Request من فرع الميزة إن التوثيق والتصاريح متاحة، لكن **لا تدمج تلقائيًا في main**.

إن احتجت الملف الحقيقي أثناء التطوير، ابحث عنه خارج الريبو داخل Downloads أو أنشئ نسخة خاصة مؤقتة خارج الشجرة المتتبعة، ولا ترفعها. لا تفترض توفر نفس مسار `/mnt/data` في Ubuntu على هاتف المستخدم.

## 6) التسليم النهائي المطلوب من Codex

- [ ] كود Flow Parser مكتوب ومرتبط بالـAndroid UI ويعمل فعلًا.
- [ ] Inline Key/Value تلقائي في Request وResponse؛ Values فعلية قابلة للنسخ بدون زر Analyze.
- [ ] استخراج محلي Emails/Phones/Key/Value بعرض واضح للمصدر.
- [ ] لا Internet permission ولا تشغيل محتوى الملفات ولا حفظ بيانات حساسة في Logs.
- [ ] لا تعديل أو overwrite للأصل؛ Export فقط لنسخة جديدة.
- [ ] الاختبارات الجديدة + regression tests ونتائجها موثقة.
- [ ] تحديث `README.md`, `CHANGELOG.md`, `docs/ACCEPTANCE.md` وربما `docs/BUILD_STATUS.md` مع الأدلة الفعلية؛ تحديث Assets المجمعة.
- [ ] `git status` نظيف بعد commit، واسم الفرع والـcommit SHA واضحان؛ PR إن أمكن.
- [ ] رابط GitHub Actions run وAPK Artifact صالح للتنزيل إذا نجح البناء؛ **لا تدّع النجاح دون نتيجة CI مؤكدة**.
- [ ] تقرير قصير بالعربي: ماذا تم؟ ما الذي اختُبر؟ ما الذي لم يُختبر؟ كيفية تثبيت النسخة الجديدة على الهاتف (والتنبيه لاختلاف debug signing keys/نسخ المسودات قبل uninstall).

**التوجيه الأخير:** الأولوية ليست إضافة عشرات الصيغ؛ هي تجربة فعلية ممتازة لملف flows بدون امتداد مع **ظهور القيم مباشرة داخل عرض الملف، بسرعة عالية وبدون تكلفة تشغيل**. لا تنهي المهمة قبل التحقق من هذا الشرط باختبار.
