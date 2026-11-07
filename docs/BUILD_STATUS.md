# Build and test evidence — 2026-11-07

The complete source is in [Disha-Galal/dishafile](https://github.com/Disha-Galal/dishafile), branch `main`. The APK is built on GitHub-hosted Ubuntu x86_64, not Android ARM64.

The final workflow [42340529607](https://github.com/Disha-Galal/dishafile/actions/runs/42340529607) **succeeded**, including APK assembly, lint and all 35 automated tests. Android 13 instrumentation ran after the connectivity service confirmed airplane mode was enabled and Wi-Fi/mobile data were disabled. [Instrumentation output](validation/android-13-instrumentation.txt) and [airplane-mode confirmation](validation/airplane-mode.txt) are included. The APK from the final run is available as [Disha-FileKit-debug-APK](https://github.com/Disha-Galal/dishafile/actions/runs/42340529607/artifacts/13560778718).

- Tested source commit: `bf403b9a24c660502d94e67913a2610ca0fab379`.
- APK size: 2,884,105 bytes (about 2.75 MiB).
- APK SHA-256: `d10bf8412f36fb882c7cfcb0d8750398a7090a905e4eb4311b4e62fd2a0fd8d7`.
- Local install copy: `/root/storage/Download/Disha-FileKit-1.0.0-debug-bf403b9.apk`.
- Artifacts are retained for 30 days; source and workflow remain in the repository for rebuilding.

| Check | Result |
|---|---|
| Kotlin codec unit tests | 17 passed |
| JSON / JSONL unit tests | 6 passed |
| Chromium browser interaction tests | 7 passed |
| Android 13 emulator instrumentation tests | 5 passed, airplane mode verified |
| Android lint | 0 errors, 19 warnings |
| Debug APK assembly | Passed |
| APK signature | Verified APK Signature Scheme v2 |

Android tests exercise native ACTION_VIEW content URIs, explicit grants from an independent DocumentsProvider, read-only metadata, read/write/reopen and truncation, original backups and conflict detection, offline editor launch, Arabic text, inert HTML, editing/undo, incoming text shares and disk-backed recovery drafts. They do not drive the system picker UI or simulate Telegram's real provider.

Codec tests cover Arabic UTF-8, BOM, UTF-16 LE/BE, Windows-1256 with explicit decoding, lossy-encoding rejection, LF/CRLF/CR, mixed-line-ending read-only protection, gzip round trips checked with the standard GZIPInputStream implementation, corrupt/truncated/concatenated gzip, short provider reads, bounded decompression including the actual 32 MiB limit, binary/TAR rejection and large read-only previews. Sample gzip files also passed independent Python `gzip.decompress` checks.

Browser tests cover selection/copy, editing/undo/redo, Save As message contents, find/replace, themes/language, JSON formatting without numeric or duplicate-key changes, invalid JSON preservation, inert HTML/no remote requests, and read-only preview controls. [Phone-sized browser rendering](editor-phone.png) is from an actual test, not a mockup or physical-device screenshot.

## Local verification

The workspace is `/root/disha-filekit` on ARM64 Ubuntu PRoot. Official JDK 17 and Gradle 8.11.1 were used to generate the wrapper and run the JVM tests. Native Kotlin compiled locally with the SDK resource-processing task excluded; this is a compile check, not an APK build. Chromium cannot launch reliably in this PRoot environment, so browser execution and Android packaging were validated on GitHub's Ubuntu runner. No physical ADB device was connected.

## Non-blocking warnings and limits

- Lint warns about deliberately pinned SDK/dependency versions, KTX/style suggestions and the default filename. It also reports URI-taint `UnsafeIntentLaunch` paths ending at `checkUriPermission`; review confirmed that this path checks a typed content URI and does not launch or forward an incoming Intent. The sharing Intent is constructed from an app-owned export URI.
- `allowBackup=false` disables legacy backup; Android 12+ cloud/device transfer rules exclude all app data. Lint still suggests an explicit legacy `fullBackupContent` attribute. The packaged APK has no INTERNET or broad storage permissions; AndroidX adds only an app-scoped signature permission for internal receivers.
- OPPO CPH2203/real Downloads/Telegram integration, native picker interaction, revoked grants, disk-full/provider failure injection, physical keyboard/IME behavior and Android 8 hardware remain manual checks. See [acceptance checklist](ACCEPTANCE.md).
- No provider can be assumed to support atomic replacement. Draft-before-write, original backup, external-change detection and readback verification reduce the risk; recovery still requires the user to Save As after a failed write.
- v1 intentionally excludes archives, binary editing and execution/previews. Editing is capped at 8 MiB; larger files have a read-only preview. Mixed line endings are read-only. Gzip decompression is capped at 32 MiB.
- Drafts debounce 700 ms and can miss the latest keystrokes on abrupt process death. Recovery opens from Home; drafts/backups remain in app storage and are lost on uninstall.
- GitHub generates temporary debug signing keys. Export drafts before uninstalling if a later debug build has a different signature.
- The owner still needs to confirm the original code's distribution license; `LICENSE` explicitly records that TODO. Third-party licenses are bundled.
