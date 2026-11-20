# FileKit 1.4: Android controls and new formats

This project By Gogo & Disha ®

Open a `.php` or `.phtml` file with **Open file** to edit its source with syntax highlighting. PHP is never executed. The existing confirmed-save, backup, conflict detection and readback verification steps apply.

Open a standalone SQLite 3 `.db`, `.sqlite`, `.sqlite3` or `.db3` file with **Open file**. Select a table and use **Previous / Next** for 50-row pages. The viewer is read-only. Long text and BLOBs have clearly marked previews. Encrypted databases, other DB formats, views, virtual tables and external WAL files are unsupported. Export a closed database from the source app first. The source file is never written by the viewer; its private temporary copy is removed when closed.

Hold **A− / A+** to keep zooming, or hold the line-number gutter or a text list to open **Text size**. The slider spans 11–32 px and also scales recent filenames, flow rows and database values. Long-press inside the text editor still selects text normally. **Options → Text size** is available in every viewer.

Tap the chevron beside the filename to collapse or restore the upper tools and metadata. Save and the three-dot menu remain visible. Size and collapsed state are saved on Android. The compact layout also works in landscape and with the software keyboard.

To save space, FileKit clears abandoned database snapshots and share copies older than one day on launch. Drafts and original backups remain available through **Recover draft / backup**.

The earlier guide below still describes file saving, encoding, AI setup and recovery.

---

# Disha FileKit 1.2 — User Guide

This project By Gogo & Disha ®

Disha FileKit is an Android editor for text and source files. It keeps the dark interface, Arabic and English controls, local recovery drafts, encoding checks, and protected saves. The AI assistant can explain or rewrite selected text using your existing Ollama installation, with direct Gemini support or optional cloud fallback.

## 1. Download and install the update

1. Open [Android APK builds](https://github.com/Disha-Galal/dishafile/actions/workflows/android-apk.yml).
2. Choose the successful build for the `build/ai-providers-1.3.0` branch. Check that both `build` and `android-13-smoke` are green.
3. Under **Artifacts**, download **Disha-FileKit-debug-APK**. Sign in to GitHub if it asks.
4. Extract the ZIP and open `app-debug.apk` on your phone. Allow installation from the browser or file manager when Android asks.
5. Download **Disha-FileKit-User-Guide** for a separate ZIP containing this Markdown guide.

Android 8 / API 26 or later and a current Android System WebView are required. You do not need a PC or Android Studio. This is a debug APK for testing. If Android refuses an update because its signing certificate differs from the installed copy, export your unsaved drafts before uninstalling: uninstalling removes app-private drafts, backups, and the saved API key. The guide does not itself confirm that a build passed; see [current build status](https://github.com/Disha-Galal/dishafile/blob/build/ai-providers-1.3.0/docs/BUILD_STATUS.md).

## 2. Start the Ollama service on your phone

Your confirmed model names are already the application defaults:

| Setting | Value |
|---|---|
| Ollama endpoint | `http://127.0.0.1:11434` |
| Primary model | `llama3.2:3b` |
| Fallback model | `qwen2.5:0.5b` |
| Optional Gemini model | `gemini-3.1-flash-lite` |

In the terminal environment on your phone where Ollama is already installed, check:

```bash
ollama list
```

Both names should appear exactly as shown above. If the service is not running, start it there:

```bash
ollama serve
```

Keep that terminal session running. If Ollama reports that port 11434 is already in use, an existing service may already be running; check it before starting another. You do not need to download the models again when they are already listed.

The service must run on the **same physical phone** as FileKit. `127.0.0.1` refers to the phone, not your PC. FileKit accepts local loopback endpoints only. It connects to Ollama's API; it does not load a GGUF file directly, bundle model weights, or start Ollama for you. A model stored in another chat application is usable only if that application actually exposes the compatible Ollama API.

## 3. Set up AI inside FileKit

1. Open **☰ → إعداد AI / AI settings**.
2. Keep the endpoint and model names from the table, or set the loopback port used by your service.
3. To enable optional cloud fallback, enter your Gemini API key in this native Android settings window. The key is intentionally absent from the repository and this guide.
4. Existing installations keep their saved model: change an unavailable `gemini-2.5-flash` to `gemini-3.1-flash-lite`, or use **Available Gemini models** to choose another. Listing a model does not guarantee generation access.
5. Tap **Test Ollama translation** or **Test Gemini translation**. These tests send only the fixed phrase “Hello world”, never your open document.
6. Press **Save**.

Exporting `GEMINI_API_KEY` in an Ubuntu/Termux terminal configures terminal tools only. Android does not inherit that environment variable; enter the key in FileKit’s native **AI settings**.

The key is encrypted using Android Keystore and stays outside the editor's JavaScript bridge. A blank key field keeps the saved key. Select **Delete saved Gemini key** to remove it. The Gemini model name is editable because provider availability can change.

## 4. Ask AI to explain or edit a selection

1. Open a file using **Open file**.
2. Select text with the phone's text selection controls, or tap **تحديد السطور / Lines** and enter the first and last line numbers. Both endpoints are included.
3. Tap **✦ AI**.
4. Choose **Ollama — Local** or **Gemini — Direct cloud**. For translation, choose Arabic or English and tap **Prepare translation request**, then review the instructions. You can also write a request, such as:
   - `Explain this Kotlin function in Arabic.`
   - `Fix the error in this selection. Return only replacement code.`
   - `Rewrite this paragraph clearly. Return only the replacement text.`
5. With Ollama selected, leave cloud fallback unchecked for local models only, or check it to try Gemini after both local models fail. Direct Gemini sends the request straight to Google without waiting for Ollama. Missing saved keys produce a setup error before any transfer.
6. Tap **Send**, review the destination in Android's confirmation dialog, and confirm.
7. Review the response and the model name shown above it.
8. Use **Copy response** for explanations. For a replacement, inspect it first, then tap **Replace range after review**.
9. Use **Undo** if needed. Apply **Save** or **Save As** yourself when you want to write the result to a document.

Without a selection, the assistant sends the whole open text document if it fits the limits. Selected text plus your request must fit **6000 UTF-8 bytes**; the request is also limited to 2000 characters. Arabic and emoji can use more than one byte per character. For a large file, select a smaller function or paragraph. FileKit rejects oversized input instead of silently cutting it.

With Ollama selected, the attempt order is **llama3.2:3b → qwen2.5:0.5b**, with optional Gemini fallback after both fail. With Gemini selected, only Gemini is called. Cloud transfer requires a saved key and explicit provider selection or fallback permission plus Android’s transfer confirmation. The fallback checkbox resets after sending. Cloud requests require internet and may incur API charges. Local file editing and local Ollama requests can work without internet while the local service remains running.

Connection timeout is 4 seconds. Local generation has a 180-second deadline to allow cold loading on a phone; Gemini has a 90-second deadline. Local output is limited to 1024 tokens, Gemini to 8192 including any model reasoning budget, and the response body to 1 MiB. Failed connections, invalid responses, empty answers, or incomplete generation trigger the next permitted model. If every permitted model fails, FileKit reports the failure and does not change the document. Model quality and speed depend on the phone, model, and request; smaller selections usually work better.

Closing the AI dialog cancels the request. A response cannot replace a document that changed while it was being generated. Read-only previews allow explanation and copying, but not applying a replacement. AI responses are displayed as plain text and are never executed. The Flow Viewer does not send flows to AI automatically.

## 5. Edit and save files

- **Open file:** choose a document using Android's file picker. Use this when your file manager does not list FileKit under “Open with”.
- **New file:** give the file a name and extension, for example `notes.md`, `main.kt`, or `config.json.gz`.
- **Line selection:** select an inclusive range of whole lines. Standard touch selection can select a smaller part of a line.
- **Arrow buttons:** move the cursor without a physical keyboard. **⇥** indents selected lines. The status shows line, column, and selected character count.
- **Wrap / A− / A+:** adjust line wrapping and font size. Scroll the tools row sideways to reach additional controls.
- **Find / Replace:** search the open document. **Undo / Redo** also cover formatting and AI replacements.
- **Save:** confirm replacing the source. FileKit keeps a draft and an original backup, checks for outside changes, truncates writes, and verifies the result by reading it back.
- **Save As:** create a new document. Read-only providers require this route. FileKit rejects a provider that returns an existing non-empty document for an implicit replacement.
- **Recover draft / backup:** open a stored recovery copy, then save it under a new name.
- **Share / Export:** share a copy. It does not automatically replace the source.

Drafts are scheduled 700 ms after an edit. A sudden process kill can lose the last unsaved keystroke. SAF providers cannot guarantee atomic replacement; retain recovery copies if a write fails. App-private recovery files disappear when the app is uninstalled.

## 6. Supported formats and limits

Text and code support includes TXT, Markdown, JSON, JSONL/NDJSON, SQL, HTML, CSS, JavaScript/JSX/MJS/CJS, TypeScript/TSX, XML/SVG, YAML, Python, Kotlin/KTS, Java, C/C++, C#, Go, Rust, Ruby, shell, PowerShell, TOML, properties, INI/config, CSV/TSV, logs, SRT/VTT, BAT/CMD, `.env`, Dockerfile, `.gitignore`, and `.editorconfig`.

Supported programming languages have local syntax highlighting. CSV/TSV, subtitles, and BAT/CMD are plain text editing, not spreadsheets, subtitle playback, or script execution. HTML and SVG remain inert source text. PDF, Word/Excel documents, binary images, ZIP, TAR, and multi-file archives are not editable formats.

- Editable text: up to 8 MiB. Larger files can have a read-only preview of the first 256 KiB.
- GZIP: supported text inside a checked single-member stream; source limit 40 MiB and decompression limit 32 MiB.
- Encoding: strict UTF-8, UTF-8/UTF-16 BOM handling, or an explicit Windows-1256 choice. Unsupported characters cause a save error instead of replacement characters.
- LF, CRLF, and CR are preserved. Mixed line endings open read-only to avoid hidden normalization.
- JSON formatting preserves large numbers and duplicate keys. JSONL/NDJSON validation checks records individually.

The mitmproxy Flow Viewer is read-only. It recognizes valid TNetstring data, including extensionless flow files, shows request/response values, and can export a bounded selected-flow JSON copy. Its exported JSON is not a complete lossless mitmproxy dump. Sensitive-data hiding affects display and copy controls, not the original file or all export contents.

## 7. Troubleshooting

| Problem | What to check |
|---|---|
| AI cannot connect | Run Ollama on this phone, keep its service alive, and confirm the endpoint and port. |
| Primary model fails | Check exact names with `ollama list`. The app should try `qwen2.5:0.5b` next. |
| Long or incomplete answers fail | Select a smaller section and ask for a shorter answer. Incomplete output is not applied. |
| Gemini never starts | Enter the key in Android AI settings, select Gemini directly, and run its translation test. A terminal export does not configure Android. |
| Model unavailable / HTTP 404 | Choose a different Gemini model in settings and test generation; existing saved model names are preserved. |
| Ollama runner missing / HTTP 500 | The runtime installation is incomplete. Install the complete official distribution with its model runner, then restart the service. Seeing models in `ollama list` proves only that model metadata exists. |
| Key rejected / permission denied | Replace the key in Android settings and check its provider permissions. |
| Rate limit / quota exceeded | Check the project’s Gemini quota and billing, or retry later. |
| Context limit message | Reduce the selection or request to fit 6000 UTF-8 bytes combined. |
| Document changed during AI | Request a new suggestion for the current text. |
| Source changed outside FileKit | Use Save As or reopen it; do not overwrite another application's change. |
| File is read-only or has mixed line endings | Use a new copy where available; partial previews cannot replace the source. |
| Android refuses the APK update | Export recovery drafts first, then check the signing-certificate note in section 1. |

## بداية سريعة بالمصري

1. شغّل `ollama serve` في بيئة Ollama الموجودة على نفس الموبايل، وتأكد من `ollama list` إن عندك `llama3.2:3b` و`qwen2.5:0.5b`.
2. من FileKit افتح **☰ → إعداد AI**. العنوان الافتراضي `http://127.0.0.1:11434` وأسماء النموذجين جاهزين.
3. لو هتستخدم Gemini، أدخل المفتاح من إعدادات التطبيق فقط؛ أمر export في Ubuntu لا ينقله للتطبيق. غيّر النموذج القديم غير المتاح إلى `gemini-3.1-flash-lite` واضغط **اختبار ترجمة Gemini** ثم احفظ.
4. افتح الملف وحدد الجزء المطلوب، واضغط **✦ AI**. اختار Ollama أو Gemini المباشر، وحدد لغة الترجمة واضغط **جهّز طلب الترجمة** ثم أرسل. اختيار Gemini المباشر لا ينتظر المحلي.
5. راجع نافذة الإرسال والإجابة قبل **استبدال النطاق بعد المراجعة**. الشرح تقدر تنسخه من غير ما تستبدل الكود. بعدها احفظ بنفسك، وتقدر تتراجع عن التعديل.
6. لو النص كبير، حدد جزءًا أصغر: النص والطلب معًا حتى 6000 بايت UTF-8. التطبيق لا يقص النص بصمت.

## References and validation

- [Ollama CLI](https://docs.ollama.com/cli)
- [Ollama FAQ](https://docs.ollama.com/faq)
- [Ollama generation API](https://docs.ollama.com/api/generate)
- [Gemini generation API](https://ai.google.dev/api/generate-content)
- [Build status and actual test results](https://github.com/Disha-Galal/dishafile/blob/build/ai-providers-1.3.0/docs/BUILD_STATUS.md)
- [Physical phone acceptance checks](https://github.com/Disha-Galal/dishafile/blob/build/ai-providers-1.3.0/docs/ACCEPTANCE.md)

This guide describes the implemented behavior. Automated emulator checks and local test results do not prove model quality or a successful live connection to your phone's models or Gemini key.

Ubuntu setup and runtime diagnostics: [AI_SETUP.md](AI_SETUP.md).
